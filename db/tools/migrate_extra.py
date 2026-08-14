#!/usr/bin/env python3
"""
Migrate display_block, periphery, and adviser data from wkb_yl to dayan.

Reads wkbyl_extra.tsv (exported from yl_park for the 20 migrated parks).
Generates SQL for park_display_block, park_periphery, park_adviser.
Downloads new images (health_status, entertainment) and generates upload tasks.
"""

import csv
import os
import re
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_FILE = os.path.join(SCRIPT_DIR, "wkbyl_extra.tsv")
OUTPUT_DIR = os.environ.get("TEMP", "/tmp")
IMG_BASE_URL = "https://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_PREFIX = "park/migration/2026/08/08"

# yl_park.id → dayan park_code (matching the migrate_parks.py order)
# The parks were ordered by id in the export, assigned PK00003..PK00022
PARK_ID_MAP = {
    9526: "PK00003",
    9533: "PK00004",
    9555: "PK00005",
    9560: "PK00006",
    9578: "PK00007",
    9590: "PK00008",
    9602: "PK00009",
    9720: "PK00010",
    9741: "PK00011",
    9770: "PK00012",
    9947: "PK00013",
    10457: "PK00014",
    10487: "PK00015",
    10593: "PK00016",
    10599: "PK00017",
    10662: "PK00018",
    10728: "PK00019",
    10808: "PK00020",
    10835: "PK00021",
    10838: "PK00022",
}

COLUMNS = [
    "id",
    "entertainment_life_desc", "entertainment_life_images",
    "health_status_desc", "health_status_images",
    "life_service_desc",
    "periphery_traffic_names", "periphery_traffic_addresss",
    "periphery_medical_names", "periphery_medical_addresss",
    "periphery_scenic_spot_names", "periphery_scenic_spot_addresss",
    "periphery_shop_names", "periphery_shop_addresss",
    "adviser_names", "adviser_titles", "adviser_images", "adviser_content",
]


def clean_val(val):
    if val is None or val in ("\\N", "NULL", ""):
        return None
    return val.strip() if isinstance(val, str) else val


def sql_text(val):
    """Escape for SQL TEXT/VARCHAR. Returns 'NULL' or quoted string."""
    if val is None:
        return "NULL"
    val = val.replace("\\", "\\\\").replace("'", "''")
    return f"'{val}'"


def split_csv(val):
    """Split comma-separated values, return list of stripped non-empty items."""
    if not val:
        return []
    return [item.strip() for item in val.split(",") if item.strip()]


def generate_minio_key(park_code, img_filename, block, idx):
    """Generate a MinIO object key for a display block image."""
    ext = os.path.splitext(img_filename)[1].lower()
    if not ext:
        ext = ".png"
    safe_name = f"{park_code.lower()}_{block}_{idx:03d}{ext}"
    return f"{MINIO_PREFIX}/{safe_name}"


def main():
    # Read TSV
    parks = []
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter="\t")
        for row in reader:
            if len(row) < len(COLUMNS):
                row.extend([""] * (len(COLUMNS) - len(row)))
            parks.append(dict(zip(COLUMNS, row)))

    print(f"Loaded {len(parks)} parks from wkbyl_extra.tsv")

    display_sql = [
        "-- Migration: park_display_block (entertainment + health_status from wkb_yl)",
        "",
    ]
    periphery_sql = [
        "-- Migration: park_periphery (traffic + medical + scenic + shop from wkb_yl)",
        "",
    ]
    adviser_sql = [
        "-- Migration: park_adviser (adviser data from wkb_yl)",
        "",
    ]

    image_tasks = []  # (remote_url, local_basename, minio_key)

    # Periphery type mapping:
    # traffic_names/addresss → type 1 (交通-公交)
    # medical_names/addresss → type 5 (医疗)
    # scenic_spot_names/addresss → type 4 (景点)
    # shop_names/addresss → type 6 (购物)

    for park in parks:
        yl_id = int(float(park["id"]))
        park_code = PARK_ID_MAP.get(yl_id)
        if not park_code:
            print(f"WARNING: yl_park id {yl_id} has no park_code mapping, skipping")
            continue

        # ===== park_display_block =====
        # entertainment_life → block_type='entertainment'
        ent_desc = clean_val(park["entertainment_life_desc"])
        ent_imgs_raw = clean_val(park["entertainment_life_images"])
        if ent_desc:
            ent_imgs = split_csv(ent_imgs_raw) if ent_imgs_raw else []
            minio_keys = []
            for i, img_fn in enumerate(ent_imgs):
                mk = generate_minio_key(park_code, img_fn, "ent", i)
                minio_keys.append(mk)
                image_tasks.append((f"{IMG_BASE_URL}{img_fn}", os.path.basename(mk.replace("/", "_") + ".tmp"), mk, img_fn))

            display_sql.append(
                f"INSERT INTO park_display_block "
                f"(park_code, block_type, block_title, content, images, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, 'entertainment', '文娱生活', {sql_text(ent_desc)}, "
                f"{sql_text(json.dumps(minio_keys) if minio_keys else None)}, 1, 1, NOW(), NOW());"
            )

        # health_status → block_type='health_status'
        health_desc = clean_val(park["health_status_desc"])
        health_imgs_raw = clean_val(park["health_status_images"])
        if health_desc:
            health_imgs = split_csv(health_imgs_raw) if health_imgs_raw else []
            minio_keys = []
            for i, img_fn in enumerate(health_imgs):
                mk = generate_minio_key(park_code, img_fn, "health", i)
                minio_keys.append(mk)
                image_tasks.append((f"{IMG_BASE_URL}{img_fn}", os.path.basename(mk.replace("/", "_") + ".tmp"), mk, img_fn))

            display_sql.append(
                f"INSERT INTO park_display_block "
                f"(park_code, block_type, block_title, content, images, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, 'health_status', '康养状况', {sql_text(health_desc)}, "
                f"{sql_text(json.dumps(minio_keys) if minio_keys else None)}, 2, 1, NOW(), NOW());"
            )

        # life_service_desc → block_type='custom' (title='生活服务')
        life_desc = clean_val(park["life_service_desc"])
        if life_desc:
            display_sql.append(
                f"INSERT INTO park_display_block "
                f"(park_code, block_type, block_title, content, images, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, 'custom', '生活服务', {sql_text(life_desc)}, NULL, 3, 1, NOW(), NOW());"
            )

        # ===== park_periphery =====
        periphery_sources = [
            ("periphery_traffic_names", "periphery_traffic_addresss", 1),   # 交通
            ("periphery_medical_names", "periphery_medical_addresss", 5),   # 医疗
            ("periphery_scenic_spot_names", "periphery_scenic_spot_addresss", 4),  # 景点
            ("periphery_shop_names", "periphery_shop_addresss", 6),         # 购物
        ]
        peri_sort = 0
        for names_col, addrs_col, ptype in periphery_sources:
            names = split_csv(clean_val(park[names_col]))
            addrs = split_csv(clean_val(park[addrs_col]))
            for i, name in enumerate(names):
                addr = addrs[i] if i < len(addrs) else None
                peri_sort += 1
                periphery_sql.append(
                    f"INSERT INTO park_periphery "
                    f"(park_code, periphery_type, place_name, place_address, sort_order, status, created_at, updated_at) "
                    f"VALUES ({sql_text(park_code)}, {ptype}, {sql_text(name)}, {sql_text(addr)}, "
                    f"{peri_sort}, 1, NOW(), NOW());"
                )

        # ===== park_adviser =====
        adv_names = split_csv(clean_val(park["adviser_names"]))
        adv_titles = split_csv(clean_val(park["adviser_titles"]))
        adv_images = split_csv(clean_val(park["adviser_images"]))
        adv_contents = split_csv(clean_val(park["adviser_content"]))

        for i, name in enumerate(adv_names):
            title = adv_titles[i] if i < len(adv_titles) else None
            content = adv_contents[i] if i < len(adv_contents) else None
            img_fn = adv_images[i] if i < len(adv_images) else None
            minio_key = None
            if img_fn:
                mk = generate_minio_key(park_code, img_fn, "adv", i)
                minio_key = mk
                image_tasks.append((f"{IMG_BASE_URL}{img_fn}", os.path.basename(mk.replace("/", "_") + ".tmp"), mk, img_fn))

            adviser_sql.append(
                f"INSERT INTO park_adviser "
                f"(park_code, adviser_name, adviser_title, adviser_image, adviser_content, "
                f"is_primary, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, {sql_text(name)}, {sql_text(title)}, "
                f"{sql_text(minio_key)}, {sql_text(content)}, "
                f"{1 if i == 0 else 0}, {i + 1}, 1, NOW(), NOW());"
            )

    # Write SQL files
    display_file = os.path.join(OUTPUT_DIR, "migrate_display_blocks.sql")
    with open(display_file, "w", encoding="utf-8") as f:
        f.write("\n".join(display_sql))
    display_count = len([l for l in display_sql if l.startswith("INSERT")])
    print(f"Wrote {display_file} ({display_count} display blocks)")

    periphery_file = os.path.join(OUTPUT_DIR, "migrate_periphery.sql")
    with open(periphery_file, "w", encoding="utf-8") as f:
        f.write("\n".join(periphery_sql))
    peri_count = len([l for l in periphery_sql if l.startswith("INSERT")])
    print(f"Wrote {periphery_file} ({peri_count} periphery items)")

    adviser_file = os.path.join(OUTPUT_DIR, "migrate_adviser.sql")
    with open(adviser_file, "w", encoding="utf-8") as f:
        f.write("\n".join(adviser_sql))
    adv_count = len([l for l in adviser_sql if l.startswith("INSERT")])
    print(f"Wrote {adviser_file} ({adv_count} advisers)")

    # Generate image download script
    download_lines = ["#!/bin/bash", "# Download extra images (display block + adviser)", "set -e", ""]
    download_lines.append("mkdir -p /tmp/migration_imgs_extra")
    for remote_url, _, minio_key, orig in image_tasks:
        local_name = minio_key.replace("/", "_")
        download_lines.append(f'curl -sS --fail --connect-timeout 10 --max-time 30 -o "/tmp/migration_imgs_extra/{local_name}" "{remote_url}"')
    download_lines.append('')
    download_lines.append('echo "Downloaded $(ls /tmp/migration_imgs_extra | wc -l) images"')

    download_sh = os.path.join(OUTPUT_DIR, "download_extra_images.sh")
    with open(download_sh, "w", encoding="utf-8") as f:
        f.write("\n".join(download_lines))
    print(f"Wrote {download_sh} ({len(image_tasks)} image downloads)")

    print(f"\n=== SUMMARY ===")
    print(f"Display blocks: {display_count}")
    print(f"Periphery items: {peri_count}")
    print(f"Advisers: {adv_count}")
    print(f"New images: {len(image_tasks)}")


if __name__ == "__main__":
    main()
