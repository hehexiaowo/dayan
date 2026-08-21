#!/usr/bin/env python3
"""
Download ALL park images from wkb_yl proxy.
Stores files by original filename in local cache.
Then maps to MinIO keys using system_asset records.

Phase 1: Download all unique images from TSV to cache (by original filename)
Phase 2: Upload to MinIO using system_asset mapping

Usage:
  python download_all.py [--delay 0.3]
  python download_all.py --upload-only
"""

import csv
import os
import subprocess
import sys
import argparse
import time
from minio import Minio
from minio.error import S3Error

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
IMG_BASE_URL = "http://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_BUCKET = "dayan-public"
CACHE_DIR = os.path.join(os.environ.get("TEMP", "/tmp"), "park_img_cache")
MINIO_ENDPOINT = "localhost:9000"
MINIO_ACCESS_KEY = "dayan"
MINIO_SECRET_KEY = "dayan12345"

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


def collect_all_orig_filenames():
    """Collect ALL unique original filenames from all TSV files."""
    filenames = set()

    # wkbyl_parks.tsv: head_images + entertainment_life_images
    COLS = [
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
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            p = dict(zip(COLS, row))
            for fn in split_csv(clean(p.get("head_images", ""))):
                filenames.add(fn)
            for fn in split_csv(clean(p.get("entertainment_life_images", ""))):
                filenames.add(fn)

    # wkbyl_extra.tsv
    COLS = [
        "id", "entertainment_life_desc", "entertainment_life_images",
        "health_status_desc", "health_status_images", "life_service_desc",
        "periphery_traffic_names", "periphery_traffic_addresss",
        "periphery_medical_names", "periphery_medical_addresss",
        "periphery_scenic_spot_names", "periphery_scenic_spot_addresss",
        "periphery_shop_names", "periphery_shop_addresss",
        "adviser_names", "adviser_titles", "adviser_images", "adviser_content",
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_extra.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            p = dict(zip(COLS, row))
            for field in ["entertainment_life_images", "health_status_images", "adviser_images"]:
                for fn in split_csv(clean(p.get(field, ""))):
                    filenames.add(fn)

    # wkbyl_ext.tsv
    COLS = [
        "code", "require_health_type", "support_lived", "live_case",
        "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
        "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
        "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
        "org_cert_desc", "org_cert_images"
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            e = dict(zip(COLS, row))
            for field in ["live_env_images", "catering_images", "org_cert_images"]:
                for fn in split_csv(clean(e.get(field, ""))):
                    filenames.add(fn)

    # wkbyl_house.tsv
    COLS = [
        "code", "name", "house_type", "house_type_desc", "intro",
        "base_institutions", "smart_institutions", "month_price",
        "images", "orientaion", "person_num", "sort_rank"
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_house.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            h = dict(zip(COLS, row))
            for fn in split_csv(clean(h.get("images", ""))):
                filenames.add(fn)

    return filenames


def build_minio_mapping():
    """Build orig_filename -> [minio_key] mapping from system_asset + TSV."""
    # Get DB records with UUID/ys- names
    result = subprocess.run(
        ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
         "-e", "USE dayan; SELECT asset_url, asset_name FROM system_asset "
               "WHERE ref_type1='park' AND asset_url LIKE 'park/migration%' "
               "AND asset_name IS NOT NULL AND asset_name != '' "
               "AND (asset_name REGEXP '^[0-9a-f]{8}-' OR asset_name LIKE 'ys-%');"],
        capture_output=True, text=True, timeout=30
    )
    # orig_filename -> [minio_key, ...]
    mapping = {}
    for line in result.stdout.strip().split("\n")[1:]:
        parts = line.split("\t")
        if len(parts) >= 2:
            key = parts[0].strip()
            orig = parts[1].strip()
            if orig not in mapping:
                mapping[orig] = []
            mapping[orig].append(key)
    return mapping


def download(fn, local_path):
    url = f"{IMG_BASE_URL}{fn}"
    try:
        r = subprocess.run(
            ["curl", "-sS", "--fail", "--connect-timeout", "10", "--max-time", "60",
             "-o", local_path, url],
            capture_output=True, text=True, timeout=90
        )
        if r.returncode == 0 and os.path.exists(local_path) and os.path.getsize(local_path) > 100:
            return True
        if os.path.exists(local_path):
            os.remove(local_path)
        return False
    except Exception:
        return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--upload-only", action="store_true")
    parser.add_argument("--delay", type=float, default=0.3)
    args = parser.parse_args()

    print("=" * 60)
    print("Park Image Download & Upload")
    print("=" * 60)

    os.makedirs(CACHE_DIR, exist_ok=True)

    # Phase 1: Download
    if not args.upload_only:
        print("\n[Phase 1] Collecting filenames from TSV...")
        all_fns = collect_all_orig_filenames()
        print(f"  Unique filenames: {len(all_fns)}")

        # Also add UUID/ys- names from DB
        mapping = build_minio_mapping()
        for orig in mapping:
            all_fns.add(orig)
        print(f"  After DB merge: {len(all_fns)}")

        print(f"\n  Downloading (delay={args.delay}s)...")
        ok = skip = fail = 0
        fns_list = sorted(all_fns)
        for i, fn in enumerate(fns_list):
            if (i + 1) % 100 == 0:
                print(f"    {i+1}/{len(fns_list)}: ok={ok} cached={skip} fail={fail}")

            cache_path = os.path.join(CACHE_DIR, fn)
            if os.path.exists(cache_path) and os.path.getsize(cache_path) > 100:
                skip += 1
                continue

            if download(fn, cache_path):
                ok += 1
            else:
                fail += 1

            time.sleep(args.delay)

        print(f"\n  Done: ok={ok} cached={skip} fail={fail}")

    # Phase 2: Upload
    print("\n[Phase 2] Upload to MinIO...")
    mc = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
               secret_key=MINIO_SECRET_KEY, secure=False)

    # Get DB mapping: orig_filename -> [minio_keys]
    mapping = build_minio_mapping()
    print(f"  DB mappings: {len(mapping)}")

    existing = set()
    for obj in mc.list_objects(MINIO_BUCKET, prefix="park/migration/", recursive=True):
        existing.add(obj.object_name)

    up_ok = up_skip = up_fail = 0
    for orig, keys in mapping.items():
        cache_path = os.path.join(CACHE_DIR, orig)
        if not os.path.exists(cache_path) or os.path.getsize(cache_path) < 100:
            up_fail += len(keys)
            continue

        for key in keys:
            if key in existing:
                up_skip += 1
                continue

            ext = os.path.splitext(key)[1].lower()
            ct = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
            try:
                mc.fput_object(MINIO_BUCKET, key, cache_path, content_type=ct)
                up_ok += 1
            except S3Error:
                up_fail += 1

    final = len(list(mc.list_objects(MINIO_BUCKET, prefix="park/migration/", recursive=True)))
    print(f"  Upload: ok={up_ok} already={up_skip} fail={up_fail}")
    print(f"  MinIO total: {final}")


if __name__ == "__main__":
    main()
