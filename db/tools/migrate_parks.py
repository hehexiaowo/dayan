#!/usr/bin/env python3
"""
Migrate 20 nursing home institutions from wkb_yl to dayan.

Pipeline:
1. Read exported wkb_yl park data (TSV)
2. Group by brand → create suppliers (SP00004+)
3. Generate SQL: INSERT supplier_info, park_info
4. Download all head_images from yl-web proxy URL
5. Output mc upload commands + park_asset INSERT SQL

Outputs:
  - /tmp/migrate_suppliers.sql
  - /tmp/migrate_parks.sql
  - /tmp/migrate_assets.sql
  - /tmp/download_images.sh  (curl commands)
  - /tmp/upload_images.sh    (mc commands)
"""

import csv
import os
import sys
import re
from datetime import datetime

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_FILE = os.path.join(SCRIPT_DIR, "wkbyl_parks.tsv")
OUTPUT_DIR = os.environ.get("TEMP", "/tmp")
IMG_BASE_URL = "https://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_BUCKET = "dayan-public"
MINIO_PREFIX = "park/migration/2026/08/08"
MIGRATION_DATE = "2026-08-08"

# CSV column order (matches SELECT)
COLUMNS = [
    "id", "code", "name", "short_name", "brand",
    "province", "province_code", "city", "city_code", "area", "area_code", "address",
    "longitude", "latitude",
    "mechanism_type", "mechanism_nature", "mechanism_characteristic",
    "mechanism_desc",
    "grade", "init_price", "min_price", "max_price", "price_unit",
    "bed_num", "phone", "is_hot", "characteristic_tag", "room_types", "opening_time",
    "head_images", "entertainment_life_images"
]

def sql_escape(val):
    """Escape a string for SQL INSERT. Returns NULL for empty/NULL/backslash-N."""
    if val is None or val == "" or val in ("\\N", "NULL"):
        return "NULL"
    val = val.replace("\\", "\\\\").replace("'", "''")
    return f"'{val}'"

def sql_int(val):
    if val is None or val == "" or val in ("\\N", "NULL"):
        return "NULL"
    try:
        return str(int(float(val)))
    except (ValueError, TypeError):
        return "NULL"

def sql_decimal(val):
    if val is None or val == "" or val in ("\\N", "NULL"):
        return "NULL"
    try:
        return str(float(val))
    except (ValueError, TypeError):
        return "NULL"

def sql_datetime(val):
    if val is None or val == "" or val in ("\\N", "NULL"):
        return "NULL"
    return f"'{val}'"

def clean_val(val):
    """Return None for NULL/\\N/empty, otherwise the stripped value."""
    if val is None or val in ("\\N", "NULL", ""):
        return None
    return val.strip() if isinstance(val, str) else val

def normalize_brand(brand, name, short_name=None, park_id=None):
    """Extract a clean brand name for supplier grouping.

    Uses an explicit mapping for the 20 migrated parks for reliability.
    Park ID 9560 and 9770 share the "悦年华" brand.
    """
    # Explicit mapping: park_id → brand (grouped supplier)
    BRAND_MAP = {
        9526: "椿萱茂",
        9533: "九华山庄",
        9555: "永春堂",
        9560: "悦年华",
        9578: "新华家园",
        9590: "康宁津园",
        9602: "首善人家",
        9720: "大爱城",
        9741: "仁帝山",
        9770: "悦年华",  # shares brand with 9560
        9947: "鑫颐太湖椿",
        10457: "东城海阳",
        10487: "银城康养",
        10593: "香树湾",
        10599: "侨辉天颐",
        10662: "星辰家",
        10728: "逸仙养老",
        10808: "鸿泰乐尔",
        10835: "荣上耆乐园",
        10838: "太保家园",
    }

    if park_id is not None:
        pid = int(float(park_id))
        if pid in BRAND_MAP:
            return BRAND_MAP[pid]

    # Fallback: use name as-is
    return name.strip() if name else "未知名"

def generate_minio_key(park_code, img_filename, idx):
    """Generate MinIO object key for an image."""
    ext = os.path.splitext(img_filename)[1].lower()
    if not ext:
        ext = ".png"
    # Use a clean hash-like name to avoid collisions
    safe_name = f"{park_code.lower()}_{idx:03d}{ext}"
    return f"{MINIO_PREFIX}/{safe_name}"

def main():
    # Read TSV
    parks = []
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter="\t")
        for row in reader:
            if len(row) < len(COLUMNS):
                row.extend([""] * (len(COLUMNS) - len(row)))
            park = dict(zip(COLUMNS, row))
            parks.append(park)

    print(f"Loaded {len(parks)} parks from wkb_yl")

    # Step 1: Group by brand → suppliers
    brand_groups = {}
    for p in parks:
        brand = normalize_brand(p["brand"], p["name"], p["short_name"], p["id"])
        if brand not in brand_groups:
            brand_groups[brand] = []
        brand_groups[brand].append(p)

    print(f"Grouped into {len(brand_groups)} suppliers by brand:")
    for brand, plist in brand_groups.items():
        print(f"  {brand}: {len(plist)} parks")

    # Assign supplier codes starting from SP00004
    supplier_map = {}  # brand → supplier_code
    supplier_start = 4
    for idx, brand in enumerate(sorted(brand_groups.keys())):
        supplier_code = f"SP{supplier_start + idx:05d}"
        supplier_map[brand] = supplier_code

    # Assign park codes starting from PK00003
    park_start = 3

    # Step 2: Generate supplier SQL
    supplier_sql_lines = [
        "-- Migration: Create suppliers (grouped by brand from wkb_yl)",
        f"-- Generated: {datetime.now().isoformat()}",
        "",
    ]
    for brand, supplier_code in supplier_map.items():
        plist = brand_groups[brand]
        first_park = plist[0]
        province_code = clean_val(first_park["province_code"])
        contact_phone = clean_val(first_park["phone"]) or "4000000000"

        supplier_sql_lines.append(
            f"INSERT INTO supplier_info "
            f"(supplier_code, full_name, short_name, supplier_type, "
            f"province_code, contact_phone, park_count, status, audit_status, sort_order, created_at, updated_at) "
            f"VALUES "
            f"({sql_escape(supplier_code)}, {sql_escape(brand + '（运营管理有限公司）')}, "
            f"{sql_escape(brand)}, 1, "
            f"{sql_escape(province_code)}, {sql_escape(contact_phone)}, "
            f"{len(plist)}, 1, 1, {supplier_start + list(supplier_map.keys()).index(brand) - supplier_start + 1}, "
            f"NOW(), NOW());"
        )
    supplier_sql_lines.append("")

    suppliers_sql = os.path.join(OUTPUT_DIR, "migrate_suppliers.sql")
    with open(suppliers_sql, "w", encoding="utf-8") as f:
        f.write("\n".join(supplier_sql_lines))
    print(f"Wrote {suppliers_sql} ({len(supplier_map)} suppliers)")

    # Step 3: Generate park SQL + collect image download/upload commands
    park_sql_lines = [
        "-- Migration: Create parks (from wkb_yl yl_park)",
        f"-- Generated: {datetime.now().isoformat()}",
        "",
    ]
    asset_sql_lines = [
        "-- Migration: Register images in park_asset (after upload to MinIO)",
        f"-- Generated: {datetime.now().isoformat()}",
        "",
    ]
    download_lines = ["#!/bin/bash", "# Download images from wkb_yl yl-web proxy", "set -e", ""]
    upload_lines = [
        "#!/bin/bash",
        "# Upload images to MinIO via mc (run inside dayan-minio container context)",
        "set -e",
        "",
    ]

    # Track all images to download: (remote_url, local_path, minio_key)
    image_tasks = []

    for pidx, park in enumerate(parks):
        park_code = f"PK{park_start + pidx:05d}"
        brand = normalize_brand(park["brand"], park["name"], park["short_name"], park["id"])
        supplier_code = supplier_map[brand]

        # Map fields
        full_name = park["name"]
        short_name = clean_val(park["short_name"])

        # Province/city/district
        province = clean_val(park["province"])
        province_code = clean_val(park["province_code"])
        city = clean_val(park["city"])
        city_code = clean_val(park["city_code"])
        district = clean_val(park["area"])
        district_code = clean_val(park["area_code"])
        address = clean_val(park["address"])

        # Geo
        longitude = sql_decimal(park["longitude"])
        latitude = sql_decimal(park["latitude"])

        # Classification
        ability_type = sql_int(park["mechanism_type"])
        nature_type = sql_int(park["mechanism_nature"])
        specialty_desc = clean_val(park["mechanism_characteristic"])
        base_desc = clean_val(park["mechanism_desc"])
        dayan_level = sql_int(park["grade"])

        # Pricing (remote stores in yuan, not fen — min_price/max_price look like yuan values)
        min_price = sql_int(park["min_price"])
        max_price = sql_int(park["max_price"])
        price_unit = clean_val(park["price_unit"])

        # Operations
        total_beds = sql_int(park["bed_num"])
        phone = clean_val(park["phone"])
        is_hot = sql_int(park["is_hot"]) if clean_val(park["is_hot"]) else "1"
        opening_time = sql_datetime(park["opening_time"])

        park_sql_lines.append(
            f"INSERT INTO park_info "
            f"(park_code, full_name, short_name, supplier_code, brand, "
            f"ability_type, nature_type, dayan_level, "
            f"province, province_code, city, city_code, district, district_code, address, "
            f"longitude, latitude, service_hotline, "
            f"base_description, specialty_description, "
            f"total_beds, min_price_display, max_price_display, price_unit, "
            f"is_hot, operate_status, is_published, opening_time, "
            f"created_at, updated_at) "
            f"VALUES "
            f"({sql_escape(park_code)}, {sql_escape(full_name)}, {sql_escape(short_name)}, "
            f"{sql_escape(supplier_code)}, {sql_escape(brand)}, "
            f"{ability_type}, {nature_type}, {dayan_level}, "
            f"{sql_escape(province)}, {sql_escape(province_code)}, "
            f"{sql_escape(city)}, {sql_escape(city_code)}, "
            f"{sql_escape(district)}, {sql_escape(district_code)}, {sql_escape(address)}, "
            f"{longitude}, {latitude}, {sql_escape(phone)}, "
            f"{sql_escape(base_desc)}, {sql_escape(specialty_desc)}, "
            f"{total_beds}, {min_price}, {max_price}, {sql_escape(price_unit)}, "
            f"{is_hot}, 1, 1, {opening_time}, "
            f"NOW(), NOW());"
        )

        # Process images: head_images (type 100 = cover) + entertainment_life_images
        head_imgs_raw = clean_val(park["head_images"]) or ""
        ent_imgs_raw = clean_val(park["entertainment_life_images"]) or ""

        # head_images → park_asset type=1 (image), first one is cover
        head_imgs = [img.strip() for img in head_imgs_raw.split(",") if img.strip()] if head_imgs_raw else []

        for iidx, img_filename in enumerate(head_imgs):
            remote_url = f"{IMG_BASE_URL}{img_filename}"
            minio_key = generate_minio_key(park_code, img_filename, iidx)
            is_cover = 1 if iidx == 0 else 0

            asset_sql_lines.append(
                f"INSERT INTO park_asset "
                f"(park_code, asset_type, asset_url, asset_name, is_cover, "
                f"source_type, sort_order, status, created_at, updated_at) "
                f"VALUES "
                f"({sql_escape(park_code)}, 1, "
                f"{sql_escape(minio_key)}, {sql_escape(img_filename)}, {is_cover}, "
                f"'data_migration', {iidx + 1}, 1, NOW(), NOW());"
            )

            # Download + upload tasks
            local_path = f"/tmp/migration_imgs/{minio_key.replace('/', '_')}"
            image_tasks.append((remote_url, local_path, minio_key, img_filename))

        # entertainment images → park_asset type=1, sort continuing
        ent_imgs = [img.strip() for img in ent_imgs_raw.split(",") if img.strip()] if ent_imgs_raw else []
        for iidx, img_filename in enumerate(ent_imgs):
            remote_url = f"{IMG_BASE_URL}{img_filename}"
            minio_key = generate_minio_key(park_code, img_filename, len(head_imgs) + iidx)

            asset_sql_lines.append(
                f"INSERT INTO park_asset "
                f"(park_code, asset_type, asset_url, asset_name, "
                f"source_type, sort_order, status, created_at, updated_at) "
                f"VALUES "
                f"({sql_escape(park_code)}, 1, "
                f"{sql_escape(minio_key)}, {sql_escape(img_filename)}, "
                f"'data_migration', {len(head_imgs) + iidx + 1}, 1, NOW(), NOW());"
            )

            local_path = f"/tmp/migration_imgs/{minio_key.replace('/', '_')}"
            image_tasks.append((remote_url, local_path, minio_key, img_filename))

    park_sql_lines.append("")
    asset_sql_lines.append("")

    parks_sql = os.path.join(OUTPUT_DIR, "migrate_parks.sql")
    with open(parks_sql, "w", encoding="utf-8") as f:
        f.write("\n".join(park_sql_lines))
    print(f"Wrote {parks_sql} ({len(parks)} parks)")

    assets_sql = os.path.join(OUTPUT_DIR, "migrate_assets.sql")
    with open(assets_sql, "w", encoding="utf-8") as f:
        f.write("\n".join(asset_sql_lines))
    print(f"Wrote {assets_sql} ({len(image_tasks)} image assets)")

    # Generate download/upload scripts
    download_lines.append("mkdir -p /tmp/migration_imgs")
    download_lines.append("")
    for remote_url, local_path, minio_key, orig_name in image_tasks:
        download_lines.append(f'curl -sS -o "{local_path}" "{remote_url}"')
    download_lines.append("")
    download_lines.append('echo "Downloaded $(ls /tmp/migration_imgs | wc -l) images"')

    download_sh = os.path.join(OUTPUT_DIR, "download_images.sh")
    with open(download_sh, "w", encoding="utf-8") as f:
        f.write("\n".join(download_lines))
    print(f"Wrote {download_sh} ({len(image_tasks)} downloads)")

    upload_lines.append("mkdir -p /tmp/migration_imgs")
    upload_lines.append("")
    for remote_url, local_path, minio_key, orig_name in image_tasks:
        # mc cp local_path local/dayan-public/minio_key
        # local_path is on host; mc is inside container. We need to handle this.
        # We'll copy files into the container then upload
        upload_lines.append(f'mc cp "/tmp/migration_imgs/{os.path.basename(local_path)}" local/{MINIO_BUCKET}/{minio_key} 2>/dev/null || echo "FAIL: {minio_key}"')
    upload_lines.append("")
    upload_lines.append(f'echo "Uploaded $(mc find local/{MINIO_BUCKET}/{MINIO_PREFIX}/ | wc -l) images"')

    upload_sh = os.path.join(OUTPUT_DIR, "upload_images.sh")
    with open(upload_sh, "w", encoding="utf-8") as f:
        f.write("\n".join(upload_lines))
    print(f"Wrote {upload_sh} ({len(image_tasks)} uploads)")

    # Summary
    print(f"\n=== MIGRATION SUMMARY ===")
    print(f"Suppliers: {len(supplier_map)}")
    print(f"Parks: {len(parks)}")
    print(f"Images: {len(image_tasks)}")
    print(f"\nNext steps:")
    print(f"1. bash /tmp/download_images.sh")
    print(f"2. docker cp + upload to MinIO")
    print(f"3. mysql < /tmp/migrate_suppliers.sql")
    print(f"4. mysql < /tmp/migrate_parks.sql")
    print(f"5. mysql < /tmp/migrate_assets.sql")

if __name__ == "__main__":
    main()
