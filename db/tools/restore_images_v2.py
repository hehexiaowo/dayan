#!/usr/bin/env python3
"""
Restore park images to MinIO — Phase 2: fill remaining gaps.

Uses system_asset DB records as the authoritative source.
For records with UUID/ys- asset_name: downloads from proxy and uploads.
For records with descriptive names: looks up original filename from TSV.

Usage:
  python restore_images_v2.py [--dry-run]
"""

import csv
import os
import subprocess
import sys
import argparse
from minio import Minio
from minio.error import S3Error

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
IMG_BASE_URL = "http://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_BUCKET = "dayan-public"
DOWNLOAD_DIR = os.environ.get("TEMP", "/tmp") + "/restore_images_v2"

MINIO_ENDPOINT = "localhost:9000"
MINIO_ACCESS_KEY = "dayan"
MINIO_SECRET_KEY = "dayan12345"


def build_tsv_lookup():
    """Build a mapping from (park_code, image_type, index) -> original_filename
    by re-reading the TSV files exactly as the original migration scripts did."""

    lookup = {}  # minio_key_suffix -> original_filename

    # Park code assignments (same order as TSV)
    PARK_CODES = [f"PK{i:05d}" for i in range(3, 23)]

    CODE_MAP = {
        "PKe2e5457a-5346-4e8b-8759-197c316dc4f8": "PK00003",
        "PKe48cb28e-70f7-4842-bf75-1efdc22b1a61": "PK00004",
        "PK320dbd5b-d8f3-4a44-ac15-3caebfe67d7e": "PK00005",
        "PKb4521edb-bc1c-4ad3-9417-e833b71cc7e1": "PK00006",
        "PK8a5bd9a8-3203-40b0-9e91-faaa8ba21120": "PK00007",
        "PKd47e21d6-2f42-43f7-93b2-8b139bbe5ca7": "PK00008",
        "PK90e94fc3-360d-4c7b-9716-9fcf5e1cc15f": "PK00009",
        "PK049d1bfe-21f1-4342-b855-aca7e146f761": "PK00010",
        "PK611efa4d-9f3f-4117-a966-33b70cbae25d": "PK00011",
        "PKce56aeb5-bd6a-4db7-a8b6-f3641cd4cdd7": "PK00012",
        "PKb7c08a77-6ab8-484a-868c-b5ea03be5dbf": "PK00013",
        "PK794ce3e6-48a7-461e-92b7-d1a05640d011": "PK00014",
        "PKd564ae95-ab88-46ca-b81a-b949af0f9815": "PK00015",
        "PKdf662d45-0ab9-4136-91ca-18bd95f419cc": "PK00016",
        "PK824fe82f-51e1-4b40-85b5-7fffcc7a85b6": "PK00017",
        "OZ31542804": "PK00022", "OZ31307499": "PK00018",
        "OZ31164689": "PK00019", "OZ12968100": "PK00020", "OZ31137757": "PK00021",
    }

    PARK_ID_MAP = {
        9526: "PK00003", 9533: "PK00004", 9555: "PK00005", 9560: "PK00006",
        9578: "PK00007", 9590: "PK00008", 9602: "PK00009", 9720: "PK00010",
        9741: "PK00011", 9770: "PK00012", 9947: "PK00013", 10457: "PK00014",
        10487: "PK00015", 10593: "PK00016", 10599: "PK00017", 10662: "PK00018",
        10728: "PK00019", 10808: "PK00020", 10835: "PK00021", 10838: "PK00022",
    }

    def split_csv(val):
        if not val:
            return []
        return [x.strip() for x in val.split(",") if x.strip()]

    def clean(val):
        if val is None or val in ("\\N", "NULL", ""):
            return None
        return val.strip() if isinstance(val, str) else val

    # --- wkbyl_parks.tsv: head_images + entertainment_life_images ---
    # Original migrate_parks.py used a CONTINUOUS counter for both
    PARKS_COLS = [
        "id", "code", "name", "short_name", "brand",
        "province", "province_code", "city", "city_code", "area", "area_code", "address",
        "longitude", "latitude",
        "mechanism_type", "mechanism_nature", "mechanism_characteristic",
        "mechanism_desc",
        "grade", "init_price", "min_price", "max_price", "price_unit",
        "bed_num", "phone", "is_hot", "characteristic_tag", "room_types", "opening_time",
        "head_images", "entertainment_life_images"
    ]
    parks_path = os.path.join(SCRIPT_DIR, "wkbyl_parks.tsv")
    if os.path.exists(parks_path):
        with open(parks_path, "r", encoding="utf-8") as f:
            reader = csv.reader(f, delimiter="\t")
            for pidx, row in enumerate(reader):
                if len(row) < len(PARKS_COLS):
                    row.extend([""] * (len(PARKS_COLS) - len(row)))
                park = dict(zip(PARKS_COLS, row))
                park_code = PARK_CODES[pidx]
                pc = park_code.lower()

                head_imgs = split_csv(clean(park.get("head_images", "")))
                ent_imgs = split_csv(clean(park.get("entertainment_life_images", "")))

                # Continuous counter: head first, then ent
                all_imgs = head_imgs + ent_imgs
                for i, img_fn in enumerate(all_imgs):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    key = f"park/migration/2026/08/08/{pc}_{i:03d}{ext}"
                    lookup[key] = img_fn

    # --- wkbyl_extra.tsv: ent_, health_, adv_ images ---
    EXTRA_COLS = [
        "id", "entertainment_life_desc", "entertainment_life_images",
        "health_status_desc", "health_status_images", "life_service_desc",
        "periphery_traffic_names", "periphery_traffic_addresss",
        "periphery_medical_names", "periphery_medical_addresss",
        "periphery_scenic_spot_names", "periphery_scenic_spot_addresss",
        "periphery_shop_names", "periphery_shop_addresss",
        "adviser_names", "adviser_titles", "adviser_images", "adviser_content",
    ]
    extra_path = os.path.join(SCRIPT_DIR, "wkbyl_extra.tsv")
    if os.path.exists(extra_path):
        with open(extra_path, "r", encoding="utf-8") as f:
            reader = csv.reader(f, delimiter="\t")
            for row in reader:
                if len(row) < len(EXTRA_COLS):
                    row.extend([""] * (len(EXTRA_COLS) - len(row)))
                park = dict(zip(EXTRA_COLS, row))
                yl_id = int(float(park["id"]))
                park_code = PARK_ID_MAP.get(yl_id)
                if not park_code:
                    continue
                pc = park_code.lower()

                for prefix, field in [("ent", "entertainment_life_images"),
                                       ("health", "health_status_images"),
                                       ("adv", "adviser_images")]:
                    imgs = split_csv(clean(park.get(field, "")))
                    for i, img_fn in enumerate(imgs):
                        ext = os.path.splitext(img_fn)[1].lower() or ".png"
                        key = f"park/migration/2026/08/08/{pc}_{prefix}_{i:03d}{ext}"
                        lookup[key] = img_fn

    # --- wkbyl_ext.tsv: env_, food_, cat_, cert_ images ---
    EXT_COLS = [
        "code", "require_health_type", "support_lived", "live_case",
        "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
        "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
        "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
        "org_cert_desc", "org_cert_images"
    ]
    ext_path = os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv")
    if os.path.exists(ext_path):
        with open(ext_path, "r", encoding="utf-8") as f:
            reader = csv.reader(f, delimiter="\t")
            food_counter, env_counter, cat_counter = {}, {}, {}
            for row in reader:
                if len(row) < len(EXT_COLS):
                    row.extend([""] * (len(EXT_COLS) - len(row)))
                e = dict(zip(EXT_COLS, row))
                code = clean(e.get("code", ""))
                park_code = CODE_MAP.get(code)
                if not park_code:
                    continue
                pc = park_code.lower()

                for img_fn in split_csv(clean(e.get("live_env_images", ""))):
                    env_counter[park_code] = env_counter.get(park_code, 300) + 1
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    key = f"park/migration/2026/08/08/{pc}_env_{env_counter[park_code]}{ext}"
                    lookup[key] = img_fn

                for img_fn in split_csv(clean(e.get("catering_images", ""))):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    food_counter[park_code] = food_counter.get(park_code, 300) + 1
                    key = f"park/migration/2026/08/08/{pc}_food_{food_counter[park_code]}{ext}"
                    lookup[key] = img_fn
                    cat_counter[park_code] = cat_counter.get(park_code, 310) + 1
                    key = f"park/migration/2026/08/08/{pc}_cat_{cat_counter[park_code]}{ext}"
                    lookup[key] = img_fn

                for i, img_fn in enumerate(split_csv(clean(e.get("org_cert_images", "")))):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    key = f"park/migration/2026/08/08/{pc}_cert_{i:03d}{ext}"
                    lookup[key] = img_fn

    # --- wkbyl_house.tsv: room_ images ---
    HOUSE_COLS = [
        "code", "name", "house_type", "house_type_desc", "intro",
        "base_institutions", "smart_institutions", "month_price",
        "images", "orientaion", "person_num", "sort_rank"
    ]
    house_path = os.path.join(SCRIPT_DIR, "wkbyl_house.tsv")
    if os.path.exists(house_path):
        with open(house_path, "r", encoding="utf-8") as f:
            reader = csv.reader(f, delimiter="\t")
            room_counter = 100
            for row in reader:
                if len(row) < len(HOUSE_COLS):
                    row.extend([""] * (len(HOUSE_COLS) - len(row)))
                h = dict(zip(HOUSE_COLS, row))
                code = clean(h.get("code", ""))
                park_code = CODE_MAP.get(code)
                if not park_code:
                    continue
                pc = park_code.lower()
                for img_fn in split_csv(clean(h.get("images", ""))):
                    room_counter += 1
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    key = f"park/migration/2026/08/08/{pc}_room_{room_counter}{ext}"
                    lookup[key] = img_fn

    return lookup


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    print("=" * 60)
    print("Park Image Restoration — Phase 2 (gap fill)")
    print("=" * 60)

    # Get existing MinIO objects
    print("\n[1/4] Checking existing MinIO objects...")
    minio_client = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
                         secret_key=MINIO_SECRET_KEY, secure=False)
    existing = set()
    for obj in minio_client.list_objects(MINIO_BUCKET, prefix="park/migration/2026/08/08/", recursive=True):
        existing.add(obj.object_name)
    print(f"  Already in MinIO: {len(existing)}")

    # Get all system_asset records
    print("\n[2/4] Reading system_asset records from DB...")
    result = subprocess.run(
        ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
         "-e", "USE dayan; SELECT asset_url, asset_name FROM system_asset "
               "WHERE ref_type1='park' AND asset_url LIKE 'park/migration%';"],
        capture_output=True, text=True, timeout=30
    )
    db_records = []
    for line in result.stdout.strip().split("\n")[1:]:
        parts = line.split("\t")
        if len(parts) >= 2:
            db_records.append((parts[0].strip(), parts[1].strip()))
    print(f"  Total DB records: {len(db_records)}")

    # Find missing
    missing = [(url, name) for url, name in db_records if url not in existing]
    print(f"  Missing from MinIO: {len(missing)}")

    if not missing:
        print("\nAll images already in MinIO!")
        return

    # Build TSV lookup for descriptive names
    print("\n[3/4] Building TSV lookup for descriptive names...")
    tsv_lookup = build_tsv_lookup()

    # Prepare download dir
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)

    # Process missing images
    print(f"\n[4/4] Downloading and uploading {len(missing)} missing images...")
    success = 0
    failed = 0
    failed_list = []

    for i, (minio_key, asset_name) in enumerate(missing):
        if (i + 1) % 50 == 0:
            print(f"  Progress: {i+1}/{len(missing)} (ok={success}, fail={failed})")

        # Determine original filename
        if asset_name and (asset_name[0:8].replace('-', '').isalnum() or asset_name.startswith('ys-')):
            # UUID or ys- prefix: use directly
            orig_fn = asset_name
        else:
            # Descriptive name: look up in TSV
            orig_fn = tsv_lookup.get(minio_key)
            if not orig_fn:
                failed += 1
                failed_list.append((minio_key, asset_name, "no TSV mapping"))
                continue

        # Download
        ext = os.path.splitext(orig_fn)[1].lower() or ".png"
        local_path = os.path.join(DOWNLOAD_DIR, f"img_{i:05d}{ext}")
        url = f"{IMG_BASE_URL}{orig_fn}"

        try:
            result = subprocess.run(
                ["curl", "-sS", "--fail", "--connect-timeout", "10", "--max-time", "60",
                 "-o", local_path, url],
                capture_output=True, text=True, timeout=90
            )
            if result.returncode != 0 or not os.path.exists(local_path) or os.path.getsize(local_path) < 100:
                failed += 1
                failed_list.append((minio_key, asset_name, "download failed"))
                if os.path.exists(local_path):
                    os.remove(local_path)
                continue
        except Exception as e:
            failed += 1
            failed_list.append((minio_key, asset_name, str(e)))
            continue

        # Upload
        try:
            content_type = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
            minio_client.fput_object(MINIO_BUCKET, minio_key, local_path, content_type=content_type)
            success += 1
        except S3Error as e:
            failed += 1
            failed_list.append((minio_key, asset_name, f"upload error: {e}"))

        if os.path.exists(local_path):
            os.remove(local_path)

    print(f"\n{'=' * 60}")
    print(f"Phase 2 Results:")
    print(f"  Success: {success}")
    print(f"  Failed:  {failed}")
    print(f"  Total:   {len(missing)}")

    if failed_list:
        print(f"\n  Failed (first 20):")
        for key, name, reason in failed_list[:20]:
            print(f"    {key} ({name}) — {reason}")
        failed_path = os.path.join(DOWNLOAD_DIR, "failed_v2.txt")
        with open(failed_path, "w", encoding="utf-8") as f:
            for key, name, reason in failed_list:
                f.write(f"{key}\t{name}\t{reason}\n")
        print(f"\n  Full list: {failed_path}")


if __name__ == "__main__":
    main()
