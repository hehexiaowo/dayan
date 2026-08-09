#!/usr/bin/env python3
"""
migrate_gaps.py — 第二轮补充迁移：填充 park_info 剩余空字段 + park_pricing 结构化价格

数据源：
  - wkbyl_park_gaps.tsv: yl_park 主表（price_unit, greening_rate, occupancy_rate, check_in_age 等）
  - wkbyl_fees.tsv: yl_park_fee_management 结构化费用（9 个机构 107 行）

产出 SQL：
  - migrate_park_info_gaps.sql: UPDATE park_info 填充空字段
  - migrate_pricing.sql: INSERT park_pricing + park_pricing_item 结构化价格

字段映射（仅填充当前为 NULL 的字段，不覆盖已有数据）：
  price_unit        ← yl_park.price_unit（远程全空 → 填默认 "元/月"）
  green_area_rate   ← yl_park.greening_rate（文本→取数值）
  occupancy_rate    ← yl_park.occupancy_rate（百分比文本→数值）
  check_in_age_min  ← yl_park.check_in_age_min
  check_in_age_max  ← yl_park.check_in_age_max
  available_beds    ← yl_park.bed_num × (1 - occupancy_rate/100)（估算）

park_pricing 映射规则：
  master_item=床位费/月费型 → charge_type=1 (room), ref_type=room_type
  master_item=护理费       → charge_type=2 (care),  ref_type=care_type
  master_item=餐费         → charge_type=3 (food),  ref_type=food_type
  master_item=押金/保证金  → charge_type=4 (deposit), ref_type=other
  其他                     → charge_type=5 (other), ref_type=other
  amt_combine 中 amt/amtMin/amtMax 以分(cents)为单位 → /100 转元
"""

import csv
import json
import os
import re
from datetime import datetime

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.environ.get("TEMP", "/tmp")

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
    "OZ31307499": "PK00018",
    "OZ31164689": "PK00019",
    "OZ12968100": "PK00020",
    "OZ31137757": "PK00021",
    "OZ31542804": "PK00022",
}


def clean(val):
    """Return None for NULL/empty, otherwise stripped string."""
    if val is None or val in ("\\N", "NULL", ""):
        return None
    return val.strip() if isinstance(val, str) else val


def sql_str(val):
    """Escape string for SQL."""
    if val is None:
        return "NULL"
    escaped = str(val).replace("\\", "\\\\").replace("'", "''")
    return f"'{escaped}'"


def parse_numeric(text):
    """Extract first number from text like '30%' -> 30, '约10.6万' -> None (complex)."""
    if text is None:
        return None
    m = re.search(r"^-?(\d+\.?\d*)", str(text).strip())
    if m:
        try:
            val = float(m.group(1))
            return val if val == int(val) else val
        except ValueError:
            return None
    return None


def parse_area(text):
    """
    Parse area text to square meters (numeric string preserved).
    '48800' -> '48800'
    '1.2万平方' -> '12000'
    '约10.6万平方米' -> '106000'
    '200亩' -> None (can't convert without unit context)
    '11.65万方' -> '116500'
    """
    if text is None:
        return None
    s = str(text).strip()
    # Check for 万 multiplier
    if "万" in s:
        m = re.search(r"(\d+\.?\d*)\s*万", s)
        if m:
            base = float(m.group(1))
            val = base * 10000
            return str(int(val)) if val == int(val) else str(val)
        return None
    # Plain number
    m = re.search(r"^(\d+\.?\d*)", s)
    if m:
        return m.group(1)
    return None


def parse_amt_combine(amt_json):
    """
    Parse amt_combine JSON to extract price range in yuan.
    Returns (min_price, max_price, fixed_price) — all in yuan or None.
    amt is in cents (fen), divide by 100.
    """
    if not amt_json:
        return None, None, None

    try:
        items = json.loads(amt_json)
    except (json.JSONDecodeError, TypeError):
        return None, None, None

    if not items or not isinstance(items, list):
        return None, None, None

    # Use personNum=1 entry (first person pricing)
    entry = items[0]

    amt_min = entry.get("amtMin")
    amt_max = entry.get("amtMax")
    amt = entry.get("amt")

    min_yuan = None
    max_yuan = None
    fixed_yuan = None

    # amtMin/amtMax in cents
    if amt_min and str(amt_min).strip() and str(amt_min) != "0":
        try:
            min_yuan = float(amt_min) / 100
        except (ValueError, TypeError):
            pass
    if amt_max and str(amt_max).strip() and str(amt_max) != "0":
        try:
            max_yuan = float(amt_max) / 100
        except (ValueError, TypeError):
            pass
    if amt and str(amt).strip() and str(amt) != "0":
        try:
            fixed_yuan = float(amt) / 100
        except (ValueError, TypeError):
            pass

    return min_yuan, max_yuan, fixed_yuan


def map_master_item(master_item):
    """
    Map remote master_item to dayan charge_type and ref_type.
    Returns (charge_type, ref_type, ref_code_prefix).
    """
    mi = (master_item or "").strip()

    # Room/bed fees
    if mi in ("床位费", "月费型", "月费型产品", "消费型产品床位费",
              "权益型产品床位费", "房间费"):
        return 1, "room_type", "RT"
    # Care/nursing fees
    if mi in ("护理费", "护理服务费"):
        return 2, "care_type", "CT"
    # Food fees
    if mi in ("餐费", "膳食费"):
        return 3, "food_type", "FT"
    # Deposit
    if mi in ("押金", "保证金", "医疗备用金"):
        return 4, "other", "DP"
    # Everything else
    return 5, "other", "OT"


def charge_period_label(period):
    """Map charge_period code to billing_cycle."""
    mapping = {1: 1, 4: None, 8: None}  # 1=月, 4=一次性, 8=分期
    return mapping.get(period, 1)


# ============================================================
# Step 1: Generate park_info gap-fill UPDATEs
# ============================================================
def gen_park_info_gaps():
    tsv_path = os.path.join(SCRIPT_DIR, "wkbyl_park_gaps.tsv")
    parks = []
    with open(tsv_path, "r", encoding="utf-8") as f:
        # Skip warning line if present
        lines = [l for l in f if not l.startswith("mysql:")]
        reader = csv.reader(lines, delimiter="\t")
        for row in reader:
            if len(row) < 10:
                continue
            parks.append({
                "code": row[0],
                "name": row[1],
                "price_unit": clean(row[2]),
                "greening_rate": clean(row[3]),
                "occupancy_rate": clean(row[4]),
                "check_in_age_min": clean(row[5]),
                "check_in_age_max": clean(row[6]),
                "area_covered": clean(row[7]),
                "floor_area": clean(row[8]),
                "bed_num": clean(row[9]),
            })

    sql_lines = [
        "-- Migration: Fill remaining park_info gaps from wkb_yl",
        f"-- Generated: {datetime.now().isoformat()}",
        "-- Only updates fields that are currently NULL (does not overwrite existing data)",
        "",
    ]

    update_count = 0
    for park in parks:
        park_code = CODE_MAP.get(park["code"])
        if not park_code:
            continue

        sets = []

        # price_unit: remote is mostly NULL, fill default "元/月"
        pu = park["price_unit"]
        if pu and pu.strip():
            sets.append(f"price_unit = {sql_str(pu.strip())}")
        else:
            sets.append("price_unit = '元/月'")

        # green_area_rate: parse numeric from text
        green = parse_numeric(park["greening_rate"])
        if green is not None:
            sets.append(f"green_area_rate = {sql_str(str(int(green)) if green == int(green) else str(green))}")

        # occupancy_rate: parse numeric from percentage text
        occ = parse_numeric(park["occupancy_rate"])
        if occ is not None:
            occ_val = str(int(occ)) if occ == int(occ) else str(occ)
            sets.append(f"occupancy_rate = {sql_str(occ_val)}")

        # check_in_age
        age_min = parse_numeric(park["check_in_age_min"])
        if age_min is not None:
            sets.append(f"check_in_age_min = {int(age_min)}")
        age_max = parse_numeric(park["check_in_age_max"])
        if age_max is not None:
            sets.append(f"check_in_age_max = {int(age_max)}")

        # available_beds: estimate from bed_num × (1 - occupancy/100)
        beds = parse_numeric(park["bed_num"])
        if beds is not None and occ is not None and occ > 0:
            avail = int(beds * (1 - occ / 100))
            if avail > 0:
                sets.append(f"available_beds = {avail}")

        # total_area: only fill if currently NULL and we have a parseable value
        area = parse_area(park["area_covered"])
        if area:
            sets.append(f"total_area = {sql_str(area)}")

        # building_area: only fill if currently NULL
        bldg = parse_area(park["floor_area"])
        if bldg:
            sets.append(f"building_area = {sql_str(bldg)}")

        # Generate UPDATE — only for fields that are currently NULL
        # Use CASE WHEN ... IS NULL pattern
        conditional_sets = []
        for s in sets:
            col = s.split(" = ")[0]
            conditional_sets.append(f"{col} = CASE WHEN {col} IS NULL THEN {s.split(' = ', 1)[1]} ELSE {col} END")

        sql_lines.append(
            f"UPDATE park_info SET {', '.join(conditional_sets)}, updated_at = NOW() "
            f"WHERE park_code = {sql_str(park_code)};"
        )
        update_count += 1

    sql_lines.insert(3, f"-- Total: {update_count} UPDATEs")

    out_path = os.path.join(OUTPUT_DIR, "migrate_park_info_gaps.sql")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines) + "\n")

    print(f"[park_info gaps] {update_count} UPDATEs → {out_path}")
    return out_path


# ============================================================
# Step 2: Generate park_pricing + park_pricing_item INSERTs
# ============================================================
def gen_pricing():
    tsv_path = os.path.join(SCRIPT_DIR, "wkbyl_fees.tsv")
    fees = []
    with open(tsv_path, "r", encoding="utf-8") as f:
        lines = [l for l in f if not l.startswith("mysql:")]
        reader = csv.reader(lines, delimiter="\t")
        for row in reader:
            if len(row) < 6:
                continue
            fees.append({
                "code": row[0],
                "master_item": clean(row[1]),
                "sub_item": clean(row[2]),
                "amt_combine": clean(row[3]),
                "charge_period": clean(row[4]),
                "charge_mete": clean(row[5]),
                "remark": clean(row[6]) if len(row) > 6 else None,
            })

    sql_lines = [
        "-- Migration: park_pricing + park_pricing_item from yl_park_fee_management",
        f"-- Generated: {datetime.now().isoformat()}",
        "",
    ]

    pricing_count = 0
    # Track per-park charge_type sequence for ref_code generation
    park_charge_seq = {}  # (park_code, charge_type) -> counter

    for fee in fees:
        park_code = CODE_MAP.get(fee["code"])
        if not park_code:
            continue

        master = fee["master_item"] or "其他"
        sub = fee["sub_item"] or master

        charge_type, ref_type, ref_prefix = map_master_item(master)
        min_price, max_price, fixed_price = parse_amt_combine(fee["amt_combine"])

        # Skip if no pricing data at all
        if min_price is None and max_price is None and fixed_price is None:
            continue

        # Determine sale_price (prefer fixed, then min, then average)
        if fixed_price is not None:
            sale_price = fixed_price
            original_price = max_price if max_price else None
        elif min_price is not None and max_price is not None:
            sale_price = min_price
            original_price = max_price
        elif min_price is not None:
            sale_price = min_price
            original_price = None
        elif max_price is not None:
            sale_price = max_price
            original_price = None
        else:
            continue

        # Generate ref_code
        key = (park_code, charge_type)
        park_charge_seq[key] = park_charge_seq.get(key, 0) + 1
        seq = park_charge_seq[key]
        ref_code = f"{park_code}{ref_prefix}{seq:02d}"

        # price_description
        price_desc_parts = [master]
        if sub and sub != master:
            price_desc_parts.append(sub)
        if fee["remark"]:
            price_desc_parts.append(fee["remark"][:80])
        price_desc = " - ".join(price_desc_parts)[:500]

        # billing_cycle
        billing = charge_period_label(int(fee["charge_period"]) if fee["charge_period"] else 1)

        # Build price_unit based on charge_mete
        # charge_mete: 1=一次性, 2=元/月, 7=元/人/月, 3=元/人
        mete_map = {"1": "元/次", "2": "元/月", "7": "元/人/月", "3": "元/人"}
        price_unit_val = mete_map.get(fee["charge_mete"] or "", "元/月")

        sql_lines.append(
            f"INSERT INTO park_pricing "
            f"(park_code, charge_type, ref_type, ref_code, ref_name, "
            f"billing_cycle, price_unit, original_price, sale_price, "
            f"price_description, effective_date, is_current, status, "
            f"created_at, updated_at) "
            f"VALUES "
            f"({sql_str(park_code)}, {charge_type}, {sql_str(ref_type)}, {sql_str(ref_code)}, "
            f"{sql_str(sub)}, {sql_str(str(billing)) if billing else 'NULL'}, "
            f"{sql_str(price_unit_val)}, "
            f"{sql_str(str(original_price)) if original_price else 'NULL'}, "
            f"{sql_str(str(sale_price))}, "
            f"{sql_str(price_desc)}, '2026-01-01', 1, 1, NOW(), NOW());"
        )

        sql_lines.append(
            f"INSERT INTO park_pricing_item "
            f"(pricing_id, park_code, item_type, item_code, item_name, created_at, updated_at) "
            f"VALUES "
            f"(LAST_INSERT_ID(), {sql_str(park_code)}, {sql_str(ref_type)}, {sql_str(ref_code)}, "
            f"{sql_str(sub)}, NOW(), NOW());"
        )
        sql_lines.append("")
        pricing_count += 1

    sql_lines.insert(3, f"-- Total: {pricing_count} pricing items (9 parks)")

    out_path = os.path.join(OUTPUT_DIR, "migrate_pricing.sql")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines) + "\n")

    print(f"[park_pricing] {pricing_count} pricing items → {out_path}")
    return out_path


if __name__ == "__main__":
    print("=" * 60)
    print("Gap-fill migration: park_info gaps + park_pricing")
    print("=" * 60)

    gen_park_info_gaps()
    gen_pricing()

    print("\nDone! Execute the generated SQL files in order:")
    print(f"  1. {os.path.join(OUTPUT_DIR, 'migrate_park_info_gaps.sql')}")
    print(f"  2. {os.path.join(OUTPUT_DIR, 'migrate_pricing.sql')}")
