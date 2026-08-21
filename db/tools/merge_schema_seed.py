#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
merge_schema_seed.py - 将 Navicat 全量导出 dayan.sql 拆分为最终初始化产物：
    db/migration/schema.sql   最终数据库架构（全部 CREATE TABLE）
    db/migration/seed.sql     最终数据库种子（全部 INSERT IGNORE，幂等）

以导出文件为唯一基准：结构、种子数据（含演示账号）全量进入 seed.sql。

用法：python db/tools/merge_schema_seed.py <dayan.sql 路径>
"""
import re
import sys
from pathlib import Path

SRC = Path(sys.argv[1]).resolve()
OUT_DIR = Path(__file__).resolve().parent.parent / "migration"

CREATE_RE = re.compile(r"^CREATE TABLE `(\w+)`")


def main() -> None:
    tables: list[tuple[str, list[str], list[str]]] = []
    cur_name, cur_ddl, cur_inserts = None, [], []

    def flush() -> None:
        nonlocal cur_name, cur_ddl, cur_inserts
        if cur_name:
            tables.append((cur_name, cur_ddl, cur_inserts))
        cur_name, cur_ddl, cur_inserts = None, [], []

    for raw in SRC.read_text(encoding="utf-8-sig").splitlines():
        s = raw.strip()
        m = CREATE_RE.match(s)
        if m:
            flush()
            cur_name, cur_ddl, cur_inserts = m.group(1), [raw], []
            continue
        if cur_name is None:
            continue
        if s.startswith("INSERT INTO"):
            cur_inserts.append(raw)
        elif s.startswith("--") or s.startswith("DROP TABLE") or s.startswith("SET "):
            continue
        else:
            cur_ddl.append(raw)
    flush()

    # ---------- schema.sql ----------
    schema = [
        "SET NAMES utf8mb4;",
        "",
        "-- ================================================================",
        "-- 大雁养老 · 最终数据库架构",
        f"-- 由 {SRC.name}（Navicat 导出）合并生成，共 {len(tables)} 张表",
        "-- CREATE TABLE IF NOT EXISTS，重复执行安全；仅用于全新初始化",
        "-- 变更维护：直接编辑本文件（不再走编号迁移链），见 README.md",
        "-- ================================================================",
        "",
    ]
    for name, ddl, _ in tables:
        schema.append(ddl[0].replace("CREATE TABLE `", "CREATE TABLE IF NOT EXISTS `", 1))
        schema.extend(ddl[1:])
        schema.append("")

    # ---------- seed.sql ----------
    seed = [
        "SET NAMES utf8mb4;",
        "",
        "-- ================================================================",
        "-- 大雁养老 · 最终数据库种子",
        f"-- 由 {SRC.name}（Navicat 导出）合并生成，以导出文件为唯一基准",
        "-- INSERT IGNORE 保证幂等（重复执行不报错、不覆盖已存在行）",
        "-- ================================================================",
        "",
    ]
    total = 0
    for name, _, inserts in tables:
        if not inserts:
            continue
        seed.append(f"-- ----------------------------")
        seed.append(f"-- Records of {name}")
        seed.append(f"-- ----------------------------")
        for line in inserts:
            seed.append(line.replace("INSERT INTO", "INSERT IGNORE INTO", 1))
        seed.append("")
        total += len(inserts)

    out_schema = OUT_DIR / "schema.sql"
    out_seed = OUT_DIR / "seed.sql"
    out_schema.write_text("\n".join(schema), encoding="utf-8")
    out_seed.write_text("\n".join(seed), encoding="utf-8")
    print(f"tables={len(tables)}  inserts={total}")
    print(f"wrote {out_schema} ({out_schema.stat().st_size} bytes)")
    print(f"wrote {out_seed}   ({out_seed.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
