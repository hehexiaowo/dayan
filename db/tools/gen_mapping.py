#!/usr/bin/env python3
"""
Reproduce the exact image_tasks order from all 3 migration scripts
to generate correct orig_filename -> minio_key mapping.

Output: mapping.tsv (orig_filename \t minio_key)
"""

import csv
import os
import json

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
MINIO_PREFIX = "park/migration/2026/08/08"

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
    mapping = {}  # orig_filename -> minio_key (first occurrence wins)

    # ================================================================
    # Script 1: migrate_subtables.py
    # Order: house -> ext (food, cat, env)
    # Counter: len(image_tasks) per script run
    # ================================================================
    image_tasks_sub = []

    # 1a. wkbyl_house.tsv -> room_NNN
    HOUSE_COLS = ["code", "name", "house_type", "house_type_desc", "intro",
                  "base_institutions", "smart_institutions", "month_price",
                  "images", "orientaion", "person_num", "sort_rank"]
    houses = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_house.tsv"), HOUSE_COLS)
    for h in houses:
        code = clean(h.get("code", ""))
        pc = CODE_MAP.get(code)
        if not pc:
            continue
        for img_fn in split_csv(clean(h.get("images", ""))):
            ext = os.path.splitext(img_fn)[1].lower() or ".png"
            mk = f"{MINIO_PREFIX}/{pc.lower()}_room_{len(image_tasks_sub):03d}{ext}"
            image_tasks_sub.append((img_fn, mk))
            if img_fn not in mapping:
                mapping[img_fn] = mk

    # 1b. wkbyl_ext.tsv -> food_NNN, cat_NNN, env_NNN
    EXT_COLS = ["code", "require_health_type", "support_lived", "live_case",
                "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
                "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
                "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
                "org_cert_desc", "org_cert_images"]
    exts = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv"), EXT_COLS)
    for e in exts:
        code = clean(e.get("code", ""))
        pc = CODE_MAP.get(code)
        if not pc:
            continue
        p = pc.lower()
        catering_imgs = split_csv(clean(e.get("catering_images", "")))
        # food first
        for img_fn in catering_imgs:
            ext = os.path.splitext(img_fn)[1].lower() or ".png"
            mk = f"{MINIO_PREFIX}/{p}_food_{len(image_tasks_sub):03d}{ext}"
            image_tasks_sub.append((img_fn, mk))
            if img_fn not in mapping:
                mapping[img_fn] = mk
        # then cat
        for img_fn in catering_imgs:
            ext = os.path.splitext(img_fn)[1].lower() or ".png"
            mk = f"{MINIO_PREFIX}/{p}_cat_{len(image_tasks_sub):03d}{ext}"
            image_tasks_sub.append((img_fn, mk))
            if img_fn not in mapping:
                mapping[img_fn] = mk
        # then env
        for img_fn in split_csv(clean(e.get("live_env_images", ""))):
            ext = os.path.splitext(img_fn)[1].lower() or ".png"
            mk = f"{MINIO_PREFIX}/{p}_env_{len(image_tasks_sub):03d}{ext}"
            image_tasks_sub.append((img_fn, mk))
            if img_fn not in mapping:
                mapping[img_fn] = mk

    # ================================================================
    # Script 2: migrate_extra.py
    # Order: ent_NNN, health_NNN, adv_NNN
    # Counter: len(image_tasks) per script run (starts at 0)
    # ================================================================
    image_tasks_extra = []

    EXTRA_COLS = ["id", "entertainment_life_desc", "entertainment_life_images",
                  "health_status_desc", "health_status_images", "life_service_desc",
                  "periphery_traffic_names", "periphery_traffic_addresss",
                  "periphery_medical_names", "periphery_medical_addresss",
                  "periphery_scenic_spot_names", "periphery_scenic_spot_addresss",
                  "periphery_shop_names", "periphery_shop_addresss",
                  "adviser_names", "adviser_titles", "adviser_images", "adviser_content"]
    extras = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_extra.tsv"), EXTRA_COLS)
    for park in extras:
        yl_id = int(float(park["id"]))
        pc = PARK_ID_MAP.get(yl_id)
        if not pc:
            continue
        pcc = pc.lower()
        for prefix, field in [("ent", "entertainment_life_images"),
                               ("health", "health_status_images"),
                               ("adv", "adviser_images")]:
            for img_fn in split_csv(clean(park.get(field, ""))):
                ext = os.path.splitext(img_fn)[1].lower() or ".png"
                mk = f"{MINIO_PREFIX}/{pcc}_{prefix}_{len(image_tasks_extra):03d}{ext}"
                image_tasks_extra.append((img_fn, mk))
                if img_fn not in mapping:
                    mapping[img_fn] = mk

    # ================================================================
    # Script 3: migrate_parks.py
    # Order: head + ent (continuous counter per park)
    # Counter: per-park continuous (head then ent)
    # ================================================================
    PARKS_COLS = ["id", "code", "name", "short_name", "brand",
                  "province", "province_code", "city", "city_code", "area", "area_code", "address",
                  "longitude", "latitude",
                  "mechanism_type", "mechanism_nature", "mechanism_characteristic",
                  "mechanism_desc",
                  "grade", "init_price", "min_price", "max_price", "price_unit",
                  "bed_num", "phone", "is_hot", "characteristic_tag", "room_types", "opening_time",
                  "head_images", "entertainment_life_images"]
    parks = read_tsv(os.path.join(SCRIPT_DIR, "wkbyl_parks.tsv"), PARKS_COLS)
    for pidx, park in enumerate(parks):
        pc = PARK_CODES[pidx].lower()
        head_imgs = split_csv(clean(park.get("head_images", "")))
        ent_imgs = split_csv(clean(park.get("entertainment_life_images", "")))
        all_imgs = head_imgs + ent_imgs
        for i, img_fn in enumerate(all_imgs):
            ext = os.path.splitext(img_fn)[1].lower() or ".png"
            mk = f"{MINIO_PREFIX}/{pc}_{i:03d}{ext}"
            if img_fn not in mapping:
                mapping[img_fn] = mk

    # Write output
    out_path = os.path.join(SCRIPT_DIR, "mapping.tsv")
    with open(out_path, "w", encoding="utf-8") as f:
        for orig, mk in sorted(mapping.items(), key=lambda x: x[1]):
            f.write(f"{orig}\t{mk}\n")

    print(f"Mapping entries: {len(mapping)}")
    print(f"Written to: {out_path}")

    # Verify against DB
    import subprocess
    result = subprocess.run(
        ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
         "-e", "USE dayan; SELECT asset_url, asset_name FROM system_asset "
               "WHERE ref_type1='park' AND asset_url LIKE 'park/migration%';"],
        capture_output=True, text=True, timeout=30
    )
    db_records = {}
    for line in result.stdout.strip().split("\n")[1:]:
        parts = line.split("\t")
        if len(parts) >= 2:
            db_records[parts[0].strip()] = parts[1].strip()

    # Check coverage
    covered = 0
    uncovered = []
    for key, name in db_records.items():
        # Check if any orig maps to this key
        for orig, mk in mapping.items():
            if mk == key:
                covered += 1
                break
        else:
            uncovered.append((key, name))

    print(f"\nDB records: {len(db_records)}")
    print(f"Covered by mapping: {covered}")
    print(f"Uncovered: {len(uncovered)}")
    if uncovered:
        print(f"\nUncovered (first 20):")
        for key, name in uncovered[:20]:
            print(f"  {key} ({name})")


if __name__ == "__main__":
    main()
