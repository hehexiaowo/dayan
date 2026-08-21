#!/usr/bin/env python3
"""
Restore park images to MinIO after Docker rebuild.

Reads the original image filenames from wkb_yl TSV data and the existing
system_asset records in dayan DB, downloads from the yl-web proxy,
and uploads to MinIO.

Usage:
  python restore_images.py [--dry-run] [--batch-size N]

Requirements:
  - MinIO running at localhost:9000
  - pip install minio
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
MINIO_PREFIX = "park/migration/2026/08/08"
DOWNLOAD_DIR = os.environ.get("TEMP", "/tmp") + "/restore_images"

# MinIO connection
MINIO_ENDPOINT = "localhost:9000"
MINIO_ACCESS_KEY = "dayan"
MINIO_SECRET_KEY = "dayan12345"

# yl_park.code → dayan park_code
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
    "OZ31542804": "PK00022",
    "OZ31307499": "PK00018",
    "OZ31164689": "PK00019",
    "OZ12968100": "PK00020",
    "OZ31137757": "PK00021",
}

# yl_park.id → dayan park_code (for wkbyl_parks.tsv)
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


def read_tsv(path, columns):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter="\t")
        for row in reader:
            if len(row) < len(columns):
                row.extend([""] * (len(columns) - len(row)))
            rows.append(dict(zip(columns, row)))
    return rows


def collect_image_tasks():
    """Collect all image download/upload tasks from TSV files."""
    tasks = []  # (original_filename, minio_key)

    # 1. From wkbyl_parks.tsv: head_images + entertainment_life_images
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
        parks = read_tsv(parks_path, PARKS_COLS)
        for pidx, park in enumerate(parks):
            park_code = f"PK{3 + pidx:05d}"
            head_imgs = split_csv(clean(park.get("head_images", "")))
            for i, img_fn in enumerate(head_imgs):
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_{i:03d}{ext}"))
            ent_imgs = split_csv(clean(park.get("entertainment_life_images", "")))
            for i, img_fn in enumerate(ent_imgs):
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_ent_{i:03d}{ext}"))

    # 2. From wkbyl_extra.tsv: entertainment + health + adviser images
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
        extras = read_tsv(extra_path, EXTRA_COLS)
        for park in extras:
            yl_id = int(float(park["id"]))
            park_code = PARK_ID_MAP.get(yl_id)
            if not park_code:
                continue
            for prefix, field in [("ent", "entertainment_life_images"), ("health", "health_status_images"), ("adv", "adviser_images")]:
                imgs = split_csv(clean(park.get(field, "")))
                for i, img_fn in enumerate(imgs):
                    ext = os.path.splitext(img_fn)[1].lower() or ".png"
                    tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_{prefix}_{i:03d}{ext}"))

    # 3. From wkbyl_ext.tsv: live_env + catering + org_cert images
    EXT_COLS = [
        "code", "require_health_type", "support_lived", "live_case",
        "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
        "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
        "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
        "org_cert_desc", "org_cert_images"
    ]
    ext_path = os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv")
    if os.path.exists(ext_path):
        exts = read_tsv(ext_path, EXT_COLS)
        food_counter, env_counter, cat_counter = {}, {}, {}
        for e in exts:
            code = clean(e.get("code", ""))
            park_code = CODE_MAP.get(code)
            if not park_code:
                continue
            # live_env
            for img_fn in split_csv(clean(e.get("live_env_images", ""))):
                env_counter[park_code] = env_counter.get(park_code, 300) + 1
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_env_{env_counter[park_code]}{ext}"))
            # catering → food + cat
            for img_fn in split_csv(clean(e.get("catering_images", ""))):
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                food_counter[park_code] = food_counter.get(park_code, 300) + 1
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_food_{food_counter[park_code]}{ext}"))
                cat_counter[park_code] = cat_counter.get(park_code, 310) + 1
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_cat_{cat_counter[park_code]}{ext}"))
            # org_cert
            for i, img_fn in enumerate(split_csv(clean(e.get("org_cert_images", "")))):
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_cert_{i:03d}{ext}"))

    # 4. From wkbyl_house.tsv: room type images
    HOUSE_COLS = [
        "code", "name", "house_type", "house_type_desc", "intro",
        "base_institutions", "smart_institutions", "month_price",
        "images", "orientaion", "person_num", "sort_rank"
    ]
    house_path = os.path.join(SCRIPT_DIR, "wkbyl_house.tsv")
    if os.path.exists(house_path):
        houses = read_tsv(house_path, HOUSE_COLS)
        room_counter = 100
        for h in houses:
            code = clean(h.get("code", ""))
            park_code = CODE_MAP.get(code)
            if not park_code:
                continue
            for img_fn in split_csv(clean(h.get("images", ""))):
                room_counter += 1
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                tasks.append((img_fn, f"{MINIO_PREFIX}/{park_code.lower()}_room_{room_counter}{ext}"))

    return tasks


def collect_from_db():
    """Collect image tasks from existing system_asset records (UUID/ys- filenames only)."""
    tasks = []
    try:
        result = subprocess.run(
            ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
             "-e", "USE dayan; SELECT asset_url, asset_name FROM system_asset "
                   "WHERE ref_type1='park' AND asset_name IS NOT NULL AND asset_name != '' "
                   "AND (asset_name REGEXP '^[0-9a-f]{8}-' OR asset_name LIKE 'ys-%');"],
            capture_output=True, text=True, timeout=30
        )
        for line in result.stdout.strip().split("\n")[1:]:
            parts = line.split("\t")
            if len(parts) >= 2:
                tasks.append((parts[1].strip(), parts[0].strip()))
    except Exception as e:
        print(f"  Warning: Could not read from dayan DB: {e}")
    return tasks


def deduplicate_tasks(tasks):
    """Deduplicate by minio_key, keeping first occurrence."""
    seen = {}
    for orig_fn, minio_key in tasks:
        if minio_key not in seen:
            seen[minio_key] = orig_fn
    return [(fn, key) for key, fn in seen.items()]


def main():
    parser = argparse.ArgumentParser(description="Restore park images to MinIO")
    parser.add_argument("--dry-run", action="store_true", help="Only show what would be done")
    parser.add_argument("--batch-size", type=int, default=50, help="Batch size for progress reporting")
    args = parser.parse_args()

    print("=" * 60)
    print("Park Image Restoration Script")
    print("=" * 60)

    # Step 1: Collect all image tasks
    print("\n[1/5] Collecting image tasks from TSV files...")
    tsv_tasks = collect_image_tasks()
    print(f"  Found {len(tsv_tasks)} images from TSV files")

    print("\n[2/5] Collecting image tasks from system_asset DB records...")
    db_tasks = collect_from_db()
    print(f"  Found {len(db_tasks)} images from DB records")

    all_tasks = tsv_tasks + db_tasks
    tasks = deduplicate_tasks(all_tasks)
    print(f"\n  Total unique images to restore: {len(tasks)}")

    if args.dry_run:
        print("\n[DRY RUN] Would download and upload these images:")
        for i, (orig_fn, minio_key) in enumerate(tasks[:20]):
            print(f"  {i+1}. {orig_fn} -> {minio_key}")
        if len(tasks) > 20:
            print(f"  ... and {len(tasks) - 20} more")
        return

    # Step 2: Init MinIO client
    print("\n[3/5] Connecting to MinIO...")
    minio_client = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
                         secret_key=MINIO_SECRET_KEY, secure=False)
    if not minio_client.bucket_exists(MINIO_BUCKET):
        minio_client.make_bucket(MINIO_BUCKET)
        print(f"  Created bucket: {MINIO_BUCKET}")
    else:
        print(f"  Bucket exists: {MINIO_BUCKET}")

    # Step 3: Prepare download directory
    os.makedirs(DOWNLOAD_DIR, exist_ok=True)

    # Step 4: Download and upload
    print(f"\n[4/5] Downloading and uploading {len(tasks)} images...")
    success = 0
    failed = 0
    failed_list = []

    for i, (orig_fn, minio_key) in enumerate(tasks):
        if (i + 1) % args.batch_size == 0:
            print(f"  Progress: {i+1}/{len(tasks)} (ok={success}, fail={failed})")

        ext = os.path.splitext(orig_fn)[1].lower() or ".png"
        local_path = os.path.join(DOWNLOAD_DIR, f"img_{i:05d}{ext}")

        # Download
        url = f"{IMG_BASE_URL}{orig_fn}"
        try:
            result = subprocess.run(
                ["curl", "-sS", "--fail", "--connect-timeout", "10", "--max-time", "60", "-o", local_path, url],
                capture_output=True, text=True, timeout=90
            )
            if result.returncode != 0 or not os.path.exists(local_path) or os.path.getsize(local_path) < 100:
                failed += 1
                failed_list.append((orig_fn, minio_key, "download failed"))
                if os.path.exists(local_path):
                    os.remove(local_path)
                continue
        except Exception as e:
            failed += 1
            failed_list.append((orig_fn, minio_key, str(e)))
            continue

        # Upload to MinIO
        try:
            content_type = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
            minio_client.fput_object(MINIO_BUCKET, minio_key, local_path, content_type=content_type)
            success += 1
        except S3Error as e:
            failed += 1
            failed_list.append((orig_fn, minio_key, f"upload error: {e}"))

        # Cleanup local file
        if os.path.exists(local_path):
            os.remove(local_path)

    # Step 5: Summary
    print(f"\n[5/5] Done!")
    print(f"  Success: {success}")
    print(f"  Failed:  {failed}")
    print(f"  Total:   {len(tasks)}")

    if failed_list:
        print(f"\n  Failed images (first 20):")
        for orig_fn, minio_key, reason in failed_list[:20]:
            print(f"    {orig_fn} -> {minio_key} ({reason})")
        if len(failed_list) > 20:
            print(f"    ... and {len(failed_list) - 20} more")

        # Write full failed list to file
        failed_path = os.path.join(DOWNLOAD_DIR, "failed_images.txt")
        with open(failed_path, "w", encoding="utf-8") as f:
            for orig_fn, minio_key, reason in failed_list:
                f.write(f"{orig_fn}\t{minio_key}\t{reason}\n")
        print(f"\n  Full failed list written to: {failed_path}")


if __name__ == "__main__":
    main()
