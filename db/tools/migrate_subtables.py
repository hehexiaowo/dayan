#!/usr/bin/env python3
"""
Migrate room_type, food_type, display_block(live_env/catering/payment),
and UPDATE basic info fields from wkb_yl to dayan.

Sources:
  - wkbyl_house.tsv → park_room_type (name, price, images)
  - wkbyl_ext.tsv   → park_food_type (catering_desc/images)
                      park_display_block (live_env + catering + payment_way)
                      park_info UPDATE (brand_introduction, operation_subject)
  - wkbyl_basic.tsv → park_info UPDATE (org_composition, area, greening_rate, etc.)
"""

import csv
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.environ.get("TEMP", "/tmp")
IMG_BASE_URL = "https://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_PREFIX = "park/migration/2026/08/08"

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


def clean(val):
    if val is None or val in ("\\N", "NULL", ""):
        return None
    return val.strip() if isinstance(val, str) else val


def sql_text(val):
    if val is None:
        return "NULL"
    val = val.replace("\\", "\\\\").replace("'", "''")
    return f"'{val}'"


def split_csv(val):
    if not val:
        return []
    return [x.strip() for x in val.split(",") if x.strip()]


def mk_key(park_code, prefix, idx, ext=".png"):
    return f"{MINIO_PREFIX}/{park_code.lower()}_{prefix}_{idx:03d}{ext}"


def read_tsv(path, columns):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter="\t")
        for row in reader:
            if len(row) < len(columns):
                row.extend([""] * (len(columns) - len(row)))
            rows.append(dict(zip(columns, row)))
    return rows


def main():
    image_tasks = []  # (remote_url, minio_key, orig_fn)
    room_sql = ["-- park_room_type from yl_park_house_info", ""]
    food_sql = ["-- park_food_type from yl_park_ext.catering", ""]
    block_sql = ["-- park_display_block (live_env + catering + payment_way) from yl_park_ext", ""]
    update_sql = ["-- park_info UPDATE basic fields from yl_park + yl_park_ext", ""]

    # ===== 1. park_room_type from house_info =====
    HOUSE_COLS = ["code", "name", "house_type", "house_type_desc", "intro",
                  "base_institutions", "smart_institutions", "month_price",
                  "images", "orientaion", "person_num", "sort_rank"]
    houses = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_house.tsv"), HOUSE_COLS)
    print(f"Loaded {len(houses)} house_info rows")

    # Track room type counter per park for code generation
    room_counter = {}

    for h in houses:
        code = clean(h["code"])
        park_code = CODE_MAP.get(code)
        if not park_code:
            continue

        room_counter[park_code] = room_counter.get(park_code, 0) + 1
        seq = room_counter[park_code]
        rt_code = f"{park_code}RT{seq:02d}"

        name = clean(h["name"])
        if not name:
            continue

        intro = clean(h["intro"])
        price = clean(h["month_price"])
        images_raw = clean(h["images"])
        orientation = clean(h["orientaion"])
        person_num = clean(h["person_num"])

        # Parse person_num to bed_count (extract first number)
        bed_count = 1
        if person_num:
            import re
            nums = re.findall(r"\d+", person_num)
            if nums:
                bed_count = int(nums[0])

        # Download images
        img_keys = []
        if images_raw:
            for i, img_fn in enumerate(split_csv(images_raw)):
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                mk = mk_key(park_code, "room", len(image_tasks))
                img_keys.append(mk)
                image_tasks.append((f"{IMG_BASE_URL}{img_fn}", mk, img_fn))

        room_sql.append(
            f"INSERT INTO park_room_type "
            f"(park_code, room_type_code, room_type_name, stay_type, room_category, "
            f"bed_count, orientation, description, cover_image, images, "
            f"sort_order, status, created_at, updated_at) "
            f"VALUES ({sql_text(park_code)}, {sql_text(rt_code)}, {sql_text(name)}, "
            f"1, 1, {bed_count}, {sql_text(orientation)}, {sql_text(intro)}, "
            f"{sql_text(img_keys[0] if img_keys else None)}, "
            f"{sql_text(json.dumps(img_keys) if img_keys else None)}, "
            f"{seq}, 1, NOW(), NOW());"
        )

    # ===== 2. park_food_type + display blocks from ext =====
    EXT_COLS = ["code", "require_health_type", "support_lived", "live_case",
                "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
                "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
                "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
                "org_cert_desc", "org_cert_images"]
    exts = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv"), EXT_COLS)
    print(f"Loaded {len(exts)} ext rows")

    food_counter = {}
    block_counter = {}

    for e in exts:
        code = clean(e["code"])
        park_code = CODE_MAP.get(code)
        if not park_code:
            continue

        # food_type from catering_desc
        catering_desc = clean(e["catering_desc"])
        catering_imgs = clean(e["catering_images"])
        if catering_desc:
            food_counter[park_code] = food_counter.get(park_code, 0) + 1
            seq = food_counter[park_code]
            ft_code = f"{park_code}FT{seq:02d}"

            img_keys = []
            if catering_imgs:
                for img_fn in split_csv(catering_imgs):
                    mk = mk_key(park_code, "food", len(image_tasks))
                    img_keys.append(mk)
                    image_tasks.append((f"{IMG_BASE_URL}{img_fn}", mk, img_fn))

            food_sql.append(
                f"INSERT INTO park_food_type "
                f"(park_code, food_type_code, food_type_name, meal_plan, "
                f"description, cover_image, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, {sql_text(ft_code)}, '营养膳食', 1, "
                f"{sql_text(catering_desc)}, {sql_text(img_keys[0] if img_keys else None)}, "
                f"{seq}, 1, NOW(), NOW());"
            )

        # display_block: live_env
        live_env_desc = clean(e["live_env_desc"])
        live_env_imgs = clean(e["live_env_images"])
        if live_env_desc:
            block_counter[park_code] = block_counter.get(park_code, 0) + 1
            img_keys = []
            if live_env_imgs:
                for img_fn in split_csv(live_env_imgs):
                    mk = mk_key(park_code, "env", len(image_tasks))
                    img_keys.append(mk)
                    image_tasks.append((f"{IMG_BASE_URL}{img_fn}", mk, img_fn))

            imgs_json = json.dumps(img_keys) if img_keys else None
            block_sql.append(
                f"INSERT INTO park_display_block "
                f"(park_code, block_type, block_title, content, images, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, 'live_env', '居住环境', {sql_text(live_env_desc)}, "
                f"{sql_text(imgs_json)}, 4, 1, NOW(), NOW());"
            )

        # display_block: catering (if catering_desc not already used for food_type, use here too)
        if catering_desc:
            img_keys = []
            if catering_imgs:
                for img_fn in split_csv(catering_imgs):
                    mk = mk_key(park_code, "cat", len(image_tasks))
                    img_keys.append(mk)
                    image_tasks.append((f"{IMG_BASE_URL}{img_fn}", mk, img_fn))

            imgs_json = json.dumps(img_keys) if img_keys else None
            block_sql.append(
                f"INSERT INTO park_display_block "
                f"(park_code, block_type, block_title, content, images, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, 'catering', '餐饮服务', {sql_text(catering_desc)}, "
                f"{sql_text(imgs_json)}, 5, 1, NOW(), NOW());"
            )

        # display_block: payment_way
        payment = clean(e["payment_way"])
        if payment:
            block_sql.append(
                f"INSERT INTO park_display_block "
                f"(park_code, block_type, block_title, content, images, sort_order, status, created_at, updated_at) "
                f"VALUES ({sql_text(park_code)}, 'payment_way', '缴费方式', {sql_text(payment)}, "
                f"NULL, 6, 1, NOW(), NOW());"
            )

    # ===== 3. park_info UPDATE from basic fields =====
    BASIC_COLS = ["id", "code", "organization_composition", "nursing_unit",
                  "area_covered", "floor_area", "greening_rate",
                  "business_license_no", "business_bd", "occupancy_rate",
                  "brand_introduction", "mechanism_introduction",
                  "house_design", "house_design_desc", "house_design_images"]
    basics = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_basic.tsv"), BASIC_COLS)
    print(f"Loaded {len(basics)} basic rows")

    # Build ext lookup for brand/operation fields
    ext_lookup = {}
    for e in exts:
        code = clean(e["code"])
        ext_lookup[code] = e

    for b in basics:
        code = clean(b["code"])
        park_code = CODE_MAP.get(code)
        if not park_code:
            continue

        sets = []

        org_comp = clean(b["organization_composition"])
        if org_comp:
            sets.append(f"operation_subject={sql_text(org_comp)}")

        area_cov = clean(b["area_covered"])
        if area_cov:
            sets.append(f"total_area={sql_text(area_cov)}")

        floor_a = clean(b["floor_area"])
        if floor_a:
            sets.append(f"building_area={sql_text(floor_a)}")

        green = clean(b["greening_rate"])
        if green:
            sets.append(f"green_area_rate={sql_text(green)}")

        occ = clean(b["occupancy_rate"])
        if occ:
            sets.append(f"occupancy_rate={sql_text(occ)}")

        license_no = clean(b["business_license_no"])
        if license_no:
            sets.append(f"business_license_no={sql_text(license_no)}")

        bd = clean(b["business_bd"])
        if bd:
            sets.append(f"business_bd={sql_text(bd)}")

        brand_intro = clean(b["brand_introduction"])
        if brand_intro:
            sets.append(f"brand_introduction={sql_text(brand_intro)}")

        mech_intro = clean(b["mechanism_introduction"])
        if mech_intro:
            sets.append(f"base_description={sql_text(mech_intro)}")

        nursing = clean(b["nursing_unit"])
        if nursing:
            sets.append(f"nurse_patient_ratio={sql_text(nursing)}")

        # From ext: brand info
        ext_data = ext_lookup.get(code, {})
        subj_brand = clean(ext_data.get("subject_brand"))
        if subj_brand:
            sets.append(f"operation_subject_description={sql_text(subj_brand)}")

        shareholders = clean(ext_data.get("shareholders_desc"))
        if shareholders:
            sets.append(f"important_shareholders={sql_text(shareholders)}")

        if sets:
            update_sql.append(
                f"UPDATE park_info SET {', '.join(sets)} WHERE park_code={sql_text(park_code)};"
            )

    # Write all SQL files
    files = {
        "migrate_room_types.sql": room_sql,
        "migrate_food_types.sql": food_sql,
        "migrate_display_blocks2.sql": block_sql,
        "migrate_basic_update.sql": update_sql,
    }
    for fname, lines in files.items():
        path = os.path.join(OUTPUT_DIR, fname)
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
        insert_count = len([l for l in lines if l.startswith(("INSERT", "UPDATE"))])
        print(f"Wrote {path} ({insert_count} statements)")

    # Download script
    dl_lines = ["#!/bin/bash", "# Download room/food/env images", "set -e", "mkdir -p /tmp/migration_imgs_sub"]
    for url, mk, orig in image_tasks:
        local_name = mk.replace("/", "_")
        dl_lines.append(f'curl -sS --fail --connect-timeout 10 --max-time 30 -o "/tmp/migration_imgs_sub/{local_name}" "{url}"')
    dl_lines.append('echo "Downloaded $(ls /tmp/migration_imgs_sub | wc -l) images"')
    dl_path = os.path.join(OUTPUT_DIR, "download_sub_images.sh")
    with open(dl_path, "w", encoding="utf-8") as f:
        f.write("\n".join(dl_lines))

    # Deduplicate image tasks (same image may be referenced multiple times)
    unique_urls = set()
    for url, mk, orig in image_tasks:
        unique_urls.add(url)

    print(f"\n=== SUMMARY ===")
    print(f"Room types: {len([l for l in room_sql if l.startswith('INSERT')])}")
    print(f"Food types: {len([l for l in food_sql if l.startswith('INSERT')])}")
    print(f"Display blocks (live_env + catering + payment): {len([l for l in block_sql if l.startswith('INSERT')])}")
    print(f"Basic info UPDATEs: {len([l for l in update_sql if l.startswith('UPDATE')])}")
    print(f"Image download tasks: {len(image_tasks)} ({len(unique_urls)} unique URLs)")


if __name__ == "__main__":
    main()
