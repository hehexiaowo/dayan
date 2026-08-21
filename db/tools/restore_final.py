#!/usr/bin/env python3
"""
Restore park images to MinIO — definitive version.

Single source of truth: system_asset table in dayan DB.
Each record has:
  - asset_url: MinIO key (e.g. park/migration/2026/08/08/pk00003_000.png)
  - asset_name: original filename from wkb_yl (UUID or descriptive)
  - ref_type2: data_migration / display_block / room_type / food_type

For data_migration records: asset_name IS the download filename.
For others: look up original filename from TSV files using the MinIO key.

Local cache is NEVER deleted.

Usage:
  python restore_final.py                # download + upload
  python restore_final.py --upload-only  # only upload cached files
  python restore_final.py --delay 0.5    # custom delay between requests
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

# ============================================================
# TSV lookup: minio_key -> original_filename
# ============================================================

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


def build_tsv_lookup():
    """Build minio_key -> original_filename from all TSV files."""
    lookup = {}

    # 1. wkbyl_parks.tsv: head + ent (continuous counter)
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
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for pidx, row in enumerate(csv.reader(f, delimiter="\t")):
                if len(row) < len(COLS):
                    row.extend([""] * (len(COLS) - len(row)))
                p = dict(zip(COLS, row))
                pc = PARK_CODES[pidx].lower()
                all_imgs = split_csv(clean(p.get("head_images", ""))) + \
                           split_csv(clean(p.get("entertainment_life_images", "")))
                for i, fn in enumerate(all_imgs):
                    ext = os.path.splitext(fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{pc}_{i:03d}{ext}"] = fn

    # 2. wkbyl_extra.tsv: ent_, health_, adv_
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
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for row in csv.reader(f, delimiter="\t"):
                if len(row) < len(COLS):
                    row.extend([""] * (len(COLS) - len(row)))
                p = dict(zip(COLS, row))
                pc = PARK_ID_MAP.get(int(float(p["id"])))
                if not pc:
                    continue
                pcc = pc.lower()
                for prefix, field in [("ent", "entertainment_life_images"),
                                       ("health", "health_status_images"),
                                       ("adv", "adviser_images")]:
                    for i, fn in enumerate(split_csv(clean(p.get(field, "")))):
                        ext = os.path.splitext(fn)[1].lower() or ".png"
                        lookup[f"park/migration/2026/08/08/{pcc}_{prefix}_{i:03d}{ext}"] = fn

    # 3. wkbyl_ext.tsv: env_, food_, cat_, cert_
    COLS = [
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
                if len(row) < len(COLS):
                    row.extend([""] * (len(COLS) - len(row)))
                e = dict(zip(COLS, row))
                pc = CODE_MAP.get(clean(e.get("code", "")))
                if not pc:
                    continue
                p = pc.lower()
                for fn in split_csv(clean(e.get("live_env_images", ""))):
                    env_c[pc] = env_c.get(pc, 300) + 1
                    ext = os.path.splitext(fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{p}_env_{env_c[pc]}{ext}"] = fn
                for fn in split_csv(clean(e.get("catering_images", ""))):
                    ext = os.path.splitext(fn)[1].lower() or ".png"
                    food_c[pc] = food_c.get(pc, 300) + 1
                    lookup[f"park/migration/2026/08/08/{p}_food_{food_c[pc]}{ext}"] = fn
                    cat_c[pc] = cat_c.get(pc, 310) + 1
                    lookup[f"park/migration/2026/08/08/{p}_cat_{cat_c[pc]}{ext}"] = fn
                for i, fn in enumerate(split_csv(clean(e.get("org_cert_images", "")))):
                    ext = os.path.splitext(fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{p}_cert_{i:03d}{ext}"] = fn

    # 4. wkbyl_house.tsv: room_
    COLS = [
        "code", "name", "house_type", "house_type_desc", "intro",
        "base_institutions", "smart_institutions", "month_price",
        "images", "orientaion", "person_num", "sort_rank"
    ]
    path = os.path.join(SCRIPT_DIR, "wkbyl_house.tsv")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            room_c = 100
            for row in csv.reader(f, delimiter="\t"):
                if len(row) < len(COLS):
                    row.extend([""] * (len(COLS) - len(row)))
                h = dict(zip(COLS, row))
                pc = CODE_MAP.get(clean(h.get("code", "")))
                if not pc:
                    continue
                p = pc.lower()
                for fn in split_csv(clean(h.get("images", ""))):
                    room_c += 1
                    ext = os.path.splitext(fn)[1].lower() or ".png"
                    lookup[f"park/migration/2026/08/08/{p}_room_{room_c}{ext}"] = fn

    return lookup


def get_db_records():
    """Read all park migration records from system_asset."""
    result = subprocess.run(
        ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
         "-e", "USE dayan; SELECT asset_url, asset_name, ref_type2 "
               "FROM system_asset WHERE ref_type1='park' AND asset_url LIKE 'park/migration%';"],
        capture_output=True, text=True, timeout=30
    )
    records = []
    for line in result.stdout.strip().split("\n")[1:]:
        parts = line.split("\t")
        if len(parts) >= 3:
            records.append({
                "minio_key": parts[0].strip(),
                "asset_name": parts[1].strip(),
                "ref_type2": parts[2].strip(),
            })
    return records


def resolve_orig_filename(rec, tsv_lookup):
    """Determine the original download filename for a system_asset record."""
    name = rec["asset_name"]
    key = rec["minio_key"]

    # data_migration: asset_name IS the original filename
    if rec["ref_type2"] == "data_migration" and name:
        return name

    # Others: look up from TSV
    return tsv_lookup.get(key)


def download(orig_fn, local_path):
    """Download image from proxy. Returns True on success."""
    url = f"{IMG_BASE_URL}{orig_fn}"
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
    parser.add_argument("--delay", type=float, default=0.25, help="Seconds between requests")
    args = parser.parse_args()

    print("=" * 60)
    print("Park Image Restore (final)")
    print("=" * 60)

    # 1. DB records
    print("\n[1/4] Reading system_asset...")
    records = get_db_records()
    print(f"  Total records: {len(records)}")

    # 2. TSV lookup
    print("\n[2/4] Building TSV lookup...")
    tsv_lookup = build_tsv_lookup()
    print(f"  TSV entries: {len(tsv_lookup)}")

    # 3. Resolve download tasks
    tasks = []  # (minio_key, orig_filename)
    unresolved = []
    for rec in records:
        orig = resolve_orig_filename(rec, tsv_lookup)
        if orig:
            tasks.append((rec["minio_key"], orig))
        else:
            unresolved.append(rec)

    print(f"  Resolved: {len(tasks)}")
    print(f"  Unresolved: {len(unresolved)}")
    if unresolved:
        print(f"  Unresolved samples:")
        for r in unresolved[:5]:
            print(f"    {r['minio_key']} ({r['asset_name']}, {r['ref_type2']})")

    # 4. Download phase
    os.makedirs(CACHE_DIR, exist_ok=True)

    if not args.upload_only:
        print(f"\n[3/4] Downloading {len(tasks)} images (delay={args.delay}s)...")
        dl_ok = dl_cache = dl_fail = 0
        for i, (key, orig) in enumerate(tasks):
            if (i + 1) % 100 == 0:
                print(f"  {i+1}/{len(tasks)}: ok={dl_ok} cached={dl_cache} fail={dl_fail}")

            cache_file = os.path.join(CACHE_DIR, key.replace("/", "_"))
            if os.path.exists(cache_file) and os.path.getsize(cache_file) > 100:
                dl_cache += 1
                continue

            if download(orig, cache_file):
                dl_ok += 1
            else:
                dl_fail += 1

            time.sleep(args.delay)

        print(f"  Download: ok={dl_ok} cached={dl_cache} fail={dl_fail}")

    # 5. Upload phase
    print(f"\n[4/4] Uploading to MinIO...")
    mc = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
               secret_key=MINIO_SECRET_KEY, secure=False)
    existing = set()
    for obj in mc.list_objects(MINIO_BUCKET, prefix="park/migration/", recursive=True):
        existing.add(obj.object_name)

    up_ok = up_skip = up_fail = 0
    for key, orig in tasks:
        if key in existing:
            up_skip += 1
            continue

        cache_file = os.path.join(CACHE_DIR, key.replace("/", "_"))
        if not os.path.exists(cache_file) or os.path.getsize(cache_file) < 100:
            up_fail += 1
            continue

        ext = os.path.splitext(key)[1].lower()
        ct = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
        try:
            mc.fput_object(MINIO_BUCKET, key, cache_file, content_type=ct)
            up_ok += 1
        except S3Error:
            up_fail += 1

    final = len(list(mc.list_objects(MINIO_BUCKET, prefix="park/migration/", recursive=True)))
    print(f"  Upload: ok={up_ok} already={up_skip} fail={up_fail}")
    print(f"  MinIO total: {final}/{len(records)}")
    print(f"  Cache dir: {CACHE_DIR}")


if __name__ == "__main__":
    main()
