#!/usr/bin/env python3
"""
Restore park images to MinIO — Final version.

Uses system_asset DB records as the single source of truth.
For each record:
  - If asset_name is a UUID/ys- filename: download from proxy
  - If asset_name is descriptive: look up original filename from TSV

Usage:
  python restore_images_final.py [--dry-run] [--retry-failed]
"""

import csv
import os
import subprocess
import sys
import argparse
from minio import Minio
from minio.error import S3Error

IMG_BASE_URL = "http://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_BUCKET = "dayan-public"
DOWNLOAD_DIR = os.environ.get("TEMP", "/tmp") + "/restore_final"
MINIO_ENDPOINT = "localhost:9000"
MINIO_ACCESS_KEY = "dayan"
MINIO_SECRET_KEY = "dayan12345"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))


def build_tsv_lookup():
    """Build mapping: minio_key -> original_filename from TSV files."""
    lookup = {}

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
    PARK_CODES = [f"PK{i:05d}" for i in range(3, 23)]

    def split_csv(val):
        return [x.strip() for x in val.split(",") if x.strip()] if val else []

    def clean(val):
        if val is None or val in ("\\N", "NULL", ""):
            return None
        return val.strip() if isinstance(val, str) else val

    # wkbyl_parks.tsv: continuous counter for head + ent
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
    path = os.path.join(SCRIPT_DIR, "wkbyl_parks.tsv")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for pidx, row in enumerate(csv.reader(f, delimiter="\t")):
                if len(row) < len(PARKS_COLS):
                    row.extend([""] * (len(PARKS_COLS) - len(row)))
                park = dict(zip(PARKS_COLS, row))
                pc = PARK_CODES[pidx].lower()
                all_imgs = split_csv(clean(park.get("head_images", ""))) + \
                           split_csv(clean(park.get("entertainment_life_images", "")))
                for i, img_fn in enumerate(all_imgs):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{pc}_{i:03d}{ext}"] = img_fn

    # wkbyl_extra.tsv: ent_, health_, adv_
    EXTRA_COLS = [
        "id", "entertainment_life_desc", "entertainment_life_images",
        "health_status_desc", "health_status_images", "life_service_desc",
        "periphery_traffic_names", "periphery_traffic_addresss",
        "periphery_medical_names", "periphery_medical_addresss",
        "periphery_scenic_spot_names", "periphery_scenic_spot_addresss",
        "periphery_shop_names", "periphery_shop_addresss",
        "adviser_names", "adviser_titles", "adviser_images", "adviser_content",
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_extra.tsv")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for row in csv.reader(f, delimiter="\t"):
                if len(row) < len(EXTRA_COLS):
                    row.extend([""] * (len(EXTRA_COLS) - len(row)))
                park = dict(zip(EXTRA_COLS, row))
                pc = PARK_ID_MAP.get(int(float(park["id"])))
                if not pc:
                    continue
                pc = pc.lower()
                for prefix, field in [("ent", "entertainment_life_images"),
                                       ("health", "health_status_images"),
                                       ("adv", "adviser_images")]:
                    for i, img_fn in enumerate(split_csv(clean(park.get(field, "")))):
                        ext = os.path.splitext(img_fn)[1].lower() or ".png"
                        lookup[f"park/migration/2026/08/08/{pc}_{prefix}_{i:03d}{ext}"] = img_fn

    # wkbyl_ext.tsv: env_, food_, cat_, cert_ (counters from original scripts)
    EXT_COLS = [
        "code", "require_health_type", "support_lived", "live_case",
        "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
        "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
        "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
        "org_cert_desc", "org_cert_images"
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            food_c, env_c, cat_c = {}, {}, {}
            for row in csv.reader(f, delimiter="\t"):
                if len(row) < len(EXT_COLS):
                    row.extend([""] * (len(EXT_COLS) - len(row)))
                e = dict(zip(EXT_COLS, row))
                pc = CODE_MAP.get(clean(e.get("code", "")))
                if not pc:
                    continue
                p = pc.lower()
                for img_fn in split_csv(clean(e.get("live_env_images", ""))):
                    env_c[pc] = env_c.get(pc, 300) + 1
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{p}_env_{env_c[pc]}{ext}"] = img_fn
                for img_fn in split_csv(clean(e.get("catering_images", ""))):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    food_c[pc] = food_c.get(pc, 300) + 1
                    lookup[f"park/migration/2026/08/08/{p}_food_{food_c[pc]}{ext}"] = img_fn
                    cat_c[pc] = cat_c.get(pc, 310) + 1
                    lookup[f"park/migration/2026/08/08/{p}_cat_{cat_c[pc]}{ext}"] = img_fn
                for i, img_fn in enumerate(split_csv(clean(e.get("org_cert_images", "")))):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{p}_cert_{i:03d}{ext}"] = img_fn

    # wkbyl_house.tsv: room_ (counter from original: 101+)
    HOUSE_COLS = [
        "code", "name", "house_type", "house_type_desc", "intro",
        "base_institutions", "smart_institutions", "month_price",
        "images", "orientaion", "person_num", "sort_rank"
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_house.tsv")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            room_c = 100
            for row in csv.reader(f, delimiter="\t"):
                if len(row) < len(HOUSE_COLS):
                    row.extend([""] * (len(HOUSE_COLS) - len(row)))
                h = dict(zip(HOUSE_COLS, row))
                pc = CODE_MAP.get(clean(h.get("code", "")))
                if not pc:
                    continue
                p = pc.lower()
                for img_fn in split_csv(clean(h.get("images", ""))):
                    room_c += 1
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{p}_room_{room_c}{ext}"] = img_fn

    return lookup


def is_uuid_or_ys(name):
    """Check if asset_name looks like a UUID or ys- filename."""
    if not name:
        return False
    if name.startswith('ys-'):
        return True
    # UUID pattern: 8-4-4-4-12 hex
    parts = name.split('.')[0].split('-')
    if len(parts) == 5:
        lengths = [len(p) for p in parts]
        if lengths == [8, 4, 4, 4, 12]:
            return True
    return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--retry-failed", action="store_true", help="Only retry previously failed images")
    args = parser.parse_args()

    print("=" * 60)
    print("Park Image Restoration — Final Version")
    print("=" * 60)

    # Get existing MinIO objects
    print("\n[1/5] Checking MinIO...")
    mc = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
               secret_key=MINIO_SECRET_KEY, secure=False)
    existing = set()
    for obj in mc.list_objects(MINIO_BUCKET, prefix="park/", recursive=True):
        existing.add(obj.object_name)
    print(f"  Existing objects: {len(existing)}")

    # Get DB records
    print("\n[2/5] Reading system_asset from DB...")
    result = subprocess.run(
        ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
         "-e", "USE dayan; SELECT asset_url, asset_name FROM system_asset WHERE ref_type1='park';"],
        capture_output=True, text=True, timeout=30
    )
    records = []
    for line in result.stdout.strip().split("\n")[1:]:
        parts = line.split("\t")
        if len(parts) >= 2:
            records.append((parts[0].strip(), parts[1].strip()))
    print(f"  Total records: {len(records)}")

    # Filter to missing
    missing = [(url, name) for url, name in records if url not in existing]
    print(f"  Missing from MinIO: {len(missing)}")

    if not missing:
        print("\nAll images present!")
        return

    # Build TSV lookup
    print("\n[3/5] Building TSV lookup...")
    tsv_lookup = build_tsv_lookup()
    print(f"  TSV entries: {len(tsv_lookup)}")

    # Prepare
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)
    failed_log = os.path.join(DOWNLOAD_DIR, "failed.txt")

    # If retry-failed, load previous failures
    if args.retry_failed and os.path.exists(failed_log):
        with open(failed_log, "r") as f:
            retry_keys = set(line.split("\t")[0] for line in f)
        missing = [(url, name) for url, name in missing if url in retry_keys]
        print(f"  Retrying {len(missing)} previously failed images")

    # Process
    print(f"\n[4/5] Processing {len(missing)} images...")
    success = 0
    failed = 0
    failed_list = []

    for i, (minio_key, asset_name) in enumerate(missing):
        if (i + 1) % 50 == 0:
            print(f"  Progress: {i+1}/{len(missing)} (ok={success}, fail={failed})")

        # Determine original filename
        if is_uuid_or_ys(asset_name):
            orig_fn = asset_name
        else:
            orig_fn = tsv_lookup.get(minio_key)
            if not orig_fn:
                failed += 1
                failed_list.append((minio_key, asset_name, "no mapping"))
                continue

        # Download
        ext = os.path.splitext(orig_fn)[1].lower() or ".png"
        local = os.path.join(DOWNLOAD_DIR, f"img_{i:06d}{ext}")
        try:
            r = subprocess.run(
                ["curl", "-sS", "--fail", "--connect-timeout", "10", "--max-time", "60",
                 "-o", local, f"{IMG_BASE_URL}{orig_fn}"],
                capture_output=True, text=True, timeout=90
            )
            if r.returncode != 0 or not os.path.exists(local) or os.path.getsize(local) < 100:
                failed += 1
                failed_list.append((minio_key, asset_name, "403/download failed"))
                if os.path.exists(local):
                    os.remove(local)
                continue
        except Exception as e:
            failed += 1
            failed_list.append((minio_key, asset_name, str(e)))
            continue

        # Upload
        try:
            ct = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
            mc.fput_object(MINIO_BUCKET, minio_key, local, content_type=ct)
            success += 1
        except S3Error as e:
            failed += 1
            failed_list.append((minio_key, asset_name, f"S3: {e}"))

        if os.path.exists(local):
            os.remove(local)

    # Final count
    final_count = len(list(mc.list_objects(MINIO_BUCKET, prefix="park/", recursive=True)))

    print(f"\n[5/5] Done!")
    print(f"  New uploads:  {success}")
    print(f"  Failed:       {failed}")
    print(f"  Total in MinIO: {final_count}")
    print(f"  DB records:   {len(records)}")
    print(f"  Coverage:     {(len(records) - len(missing) + success) * 100 // len(records)}%")

    if failed_list:
        with open(failed_log, "w", encoding="utf-8") as f:
            for key, name, reason in failed_list:
                f.write(f"{key}\t{name}\t{reason}\n")
        print(f"\n  Failed list: {failed_log}")
        print(f"  Sample failures (first 10):")
        for key, name, reason in failed_list[:10]:
            print(f"    {key} ({name}) — {reason}")


if __name__ == "__main__":
    main()
