#!/usr/bin/env python3
"""
Fix the mapping for descriptive-name records in system_asset.

Strategy:
1. Build fingerprint -> original_filename from cached files
2. For each DB record:
   - If asset_name is UUID/ys-: already mapped, use asset_name directly
   - If asset_name is descriptive: find original filename via TSV + fingerprint
3. Upload to MinIO using the correct mapping

Output: mapping_all.tsv (minio_key \t orig_filename)
"""

import csv
import os
import hashlib
import subprocess
import time
from minio import Minio
from minio.error import S3Error

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CACHE_DIR = os.path.join(os.environ.get("TEMP", "/tmp"), "park_img_cache")
IMG_BASE_URL = "http://yl-web.wkbins.com/yl-web/ylParkController/file/showPhoto?imgUrl="
MINIO_BUCKET = "dayan-public"
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


def split_csv(val):
    return [x.strip() for x in val.split(",") if x.strip()] if val else []


def clean(val):
    if val is None or val in ("\\N", "NULL", ""):
        return None
    return val.strip() if isinstance(val, str) else val


def file_fingerprint(path):
    """Return (size, md5_head) fingerprint."""
    size = os.path.getsize(path)
    with open(path, "rb") as f:
        head = f.read(4096)
    return (size, hashlib.md5(head).hexdigest())


def build_fingerprint_map():
    """Build fingerprint -> original_filename from cached files."""
    fp_map = {}
    for fn in os.listdir(CACHE_DIR):
        path = os.path.join(CACHE_DIR, fn)
        if not os.path.isfile(path) or os.path.getsize(path) < 100:
            continue
        # Original filenames are UUID patterns (not starting with 'park_')
        if not fn.startswith("park_"):
            fp = file_fingerprint(path)
            fp_map[fp] = fn
    return fp_map


def build_tsv_candidates():
    """Build (park_code, type) -> [original_filenames] from TSV files."""
    candidates = {}

    def add(pc, typ, fn):
        key = (pc, typ)
        if key not in candidates:
            candidates[key] = []
        candidates[key].append(fn)

    # wkbyl_extra.tsv: ent, health, adv
    COLS = ["id", "entertainment_life_desc", "entertainment_life_images",
            "health_status_desc", "health_status_images", "life_service_desc",
            "periphery_traffic_names", "periphery_traffic_addresss",
            "periphery_medical_names", "periphery_medical_addresss",
            "periphery_scenic_spot_names", "periphery_scenic_spot_addresss",
            "periphery_shop_names", "periphery_shop_addresss",
            "adviser_names", "adviser_titles", "adviser_images", "adviser_content"]
    path = os.path.join(SCRIPT_DIR, "wkbyl_extra.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            p = dict(zip(COLS, row))
            pc = PARK_ID_MAP.get(int(float(p["id"])))
            if not pc:
                continue
            for prefix, field in [("ent", "entertainment_life_images"),
                                   ("health", "health_status_images"),
                                   ("adv", "adviser_images")]:
                for fn in split_csv(clean(p.get(field, ""))):
                    add(pc, prefix, fn)

    # wkbyl_ext.tsv: env, food, cat
    COLS = ["code", "require_health_type", "support_lived", "live_case",
            "support_panted", "pant_case", "subject_brand", "subject_brand_desc",
            "shareholders_desc", "payment_way", "live_env_desc", "live_env_images",
            "catering_desc", "catering_images", "have_clinic", "health_fixed_point",
            "org_cert_desc", "org_cert_images"]
    path = os.path.join(SCRIPT_DIR, "wkbyl_ext.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            e = dict(zip(COLS, row))
            pc = CODE_MAP.get(clean(e.get("code", "")))
            if not pc:
                continue
            for fn in split_csv(clean(e.get("live_env_images", ""))):
                add(pc, "env", fn)
            for fn in split_csv(clean(e.get("catering_images", ""))):
                add(pc, "food", fn)
                add(pc, "cat", fn)

    # wkbyl_house.tsv: room
    COLS = ["code", "name", "house_type", "house_type_desc", "intro",
            "base_institutions", "smart_institutions", "month_price",
            "images", "orientaion", "person_num", "sort_rank"]
    path = os.path.join(SCRIPT_DIR, "wkbyl_house.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for row in csv.reader(f, delimiter="\t"):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            h = dict(zip(COLS, row))
            pc = CODE_MAP.get(clean(h.get("code", "")))
            if not pc:
                continue
            for fn in split_csv(clean(h.get("images", ""))):
                add(pc, "room", fn)

    # wkbyl_parks.tsv: head (pk00003_000 to pk00003_007)
    COLS = ["id", "code", "name", "short_name", "brand",
            "province", "province_code", "city", "city_code", "area", "area_code", "address",
            "longitude", "latitude",
            "mechanism_type", "mechanism_nature", "mechanism_characteristic",
            "mechanism_desc",
            "grade", "init_price", "min_price", "max_price", "price_unit",
            "bed_num", "phone", "is_hot", "characteristic_tag", "room_types", "opening_time",
            "head_images", "entertainment_life_images"]
    PARK_CODES = [f"PK{i:05d}" for i in range(3, 23)]
    path = os.path.join(SCRIPT_DIR, "wkbyl_parks.tsv")
    with open(path, "r", encoding="utf-8") as f:
        for pidx, row in enumerate(csv.reader(f, delimiter="\t")):
            if len(row) < len(COLS):
                row.extend([""] * (len(COLS) - len(row)))
            park = dict(zip(COLS, row))
            pc = PARK_CODES[pidx]
            for fn in split_csv(clean(park.get("head_images", ""))):
                add(pc, "head", fn)
            for fn in split_csv(clean(park.get("entertainment_life_images", ""))):
                add(pc, "ent_park", fn)

    return candidates


def main():
    print("=" * 60)
    print("Fix mapping for descriptive-name records")
    print("=" * 60)

    # 1. Build fingerprint map
    print("\n[1/4] Building fingerprint map...")
    fp_map = build_fingerprint_map()
    print(f"  Fingerprints: {len(fp_map)}")

    # 2. Build TSV candidates
    print("\n[2/4] Building TSV candidates...")
    candidates = build_tsv_candidates()
    print(f"  Candidate groups: {len(candidates)}")

    # 3. Get DB records
    print("\n[3/4] Reading DB records...")
    result = subprocess.run(
        ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
         "-e", "USE dayan; SELECT asset_url, asset_name, ref_type2 FROM system_asset "
               "WHERE ref_type1='park' AND asset_url LIKE 'park/migration%';"],
        capture_output=True, text=True, timeout=30
    )
    records = []
    for line in result.stdout.strip().split("\n")[1:]:
        parts = line.split("\t")
        if len(parts) >= 3:
            records.append({"key": parts[0].strip(), "name": parts[1].strip(), "type": parts[2].strip()})
    print(f"  Total records: {len(records)}")

    # 4. Build mapping
    mapping = {}  # minio_key -> orig_filename
    unresolved = []

    for rec in records:
        key = rec["key"]
        name = rec["name"]

        # UUID/ys- names: use directly
        if name and (name[0:8].replace("-", "").isalnum() or name.startswith("ys-")):
            mapping[key] = name
            continue

        # Descriptive names: parse and match
        # Format: pk00003_ent_000 -> PK00003, ent, 0
        parts = name.split("_")
        if len(parts) >= 3:
            pc = "PK" + parts[0][2:].upper().zfill(5)
            typ = parts[1]
            idx = int(parts[2])

            # Get candidates for this park+type
            cands = candidates.get((pc, typ), [])
            if not cands:
                # Try alternative type names
                alt_types = {"cat": "food", "food": "cat"}
                cands = candidates.get((pc, alt_types.get(typ, typ)), [])

            if cands:
                # Try to match by fingerprint
                # First, check if any candidate is already cached and matches
                matched = False
                for cfn in cands:
                    cache_path = os.path.join(CACHE_DIR, cfn)
                    if os.path.exists(cache_path) and os.path.getsize(cache_path) > 100:
                        # Check if this fingerprint matches any migration key file
                        fp = file_fingerprint(cache_path)
                        if fp in fp_map:
                            mapping[key] = cfn
                            matched = True
                            break

                if not matched:
                    # Assign by index (order in TSV)
                    if idx < len(cands):
                        mapping[key] = cands[idx]
                    else:
                        unresolved.append(rec)
            else:
                unresolved.append(rec)
        else:
            unresolved.append(rec)

    print(f"\n  Mapped: {len(mapping)}")
    print(f"  Unresolved: {len(unresolved)}")

    if unresolved:
        print(f"\n  Unresolved samples:")
        for rec in unresolved[:10]:
            print(f"    {rec['key']} ({rec['name']})")

    # Write mapping
    out_path = os.path.join(SCRIPT_DIR, "mapping_all.tsv")
    with open(out_path, "w", encoding="utf-8") as f:
        for key, orig in sorted(mapping.items()):
            f.write(f"{key}\t{orig}\n")
    print(f"\n  Written to: {out_path}")

    # 5. Upload to MinIO
    print("\n[4/4] Uploading to MinIO...")
    mc = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
               secret_key=MINIO_SECRET_KEY, secure=False)
    existing = set()
    for obj in mc.list_objects(MINIO_BUCKET, prefix="park/migration/", recursive=True):
        existing.add(obj.object_name)

    up_ok = up_skip = up_fail = 0
    for key, orig in mapping.items():
        if key in existing:
            up_skip += 1
            continue

        cache_path = os.path.join(CACHE_DIR, orig)
        if not os.path.exists(cache_path) or os.path.getsize(cache_path) < 100:
            up_fail += 1
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
    print(f"  MinIO total: {final}/{len(records)}")


if __name__ == "__main__":
    main()
