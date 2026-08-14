#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
通用 COMMENT 双重编码乱码修复 SQL 生成器（只生成，不执行）。

用法
----
    python gen_fix_comment_mojibake.py <table> [<table> ...] [--out PATH]

默认输出 scripts/fix-<table1>-<table2>-...-comment-mojibake.sql（多表合并）。

根因
----
表/列 COMMENT 为 cp1252 双重编码乱码：正确 UTF-8 字节被按 latin1
（MySQL 的 latin1 在 0x80-0x9F 即 cp1252 + C1 控制符填充）解读成字符后、
又以 utf8mb4 存储。仅出现在被「手动以非 utf8mb4 连接执行的重构脚本」
DROP+CREATE 覆盖过的表上；initdb 自动建的表不受影响。

修复原理
--------
MySQL 的 latin1 在 0x80-0x9F 就是 cp1252，故用 SQL 公式逆向：
    CONVERT(CAST(CONVERT(x USING latin1) AS BINARY) USING utf8mb4)
注意：
 1) 对「正确」字符串有破坏性（非幂等），仅对乱码表使用。
 2) 不能用 Python 'cp1252' codec 替代——它在 0x81/0x80 等 cp1252 未定义位抛错，
    而 MySQL latin1 在这些位填了 C1 控制符。编码还原必须交给 MySQL。

字节已丢失的列（latin1 还原后变 '?'）
------------------------------------
个别列经历了多次错误编码转换，中文字节已不可逆丢失，latin1 还原只会得到 '?'。
这些列的正确 COMMENT 必须从 db/migration 源 SQL 取，见下方 COMMENT_OVERRIDES。
"""
import argparse
import os
import re
import sys

import pymysql

DB = dict(host='127.0.0.1', port=3306, user='root', password='root123',
          database='dayan', charset='utf8mb4')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# comment 还原 SQL 片段（MySQL latin1 完整映射，含 C1 控制符）
FIX = "CONVERT(CAST(CONVERT(%s USING latin1) AS BINARY) USING utf8mb4)"

# latin1 还原失败的列（中文字节已不可逆丢失），用 db/migration 源文件的权威 COMMENT 覆盖。
#   agent_lead.visitor_token / visitor_source / wx_nickname / wx_avatar /
#   last_trace_time / trace_count  ← db/migration/32_lead_trace.sql
#   agent_lead.last_trace_type      ← db/migration/33_lead_last_trace_type.sql
COMMENT_OVERRIDES = {
    'agent_lead': {
        'visitor_token': '访客令牌（匿名唯一标识，UUID）',
        'visitor_source': '访客来源（wechat/browser/unknown）',
        'wx_nickname': '微信昵称',
        'wx_avatar': '微信头像URL',
        'last_trace_time': '最后互动时间',
        'trace_count': '互动总次数',
        'last_trace_type': '最后互动类型（1=内容 2=工具 3=海报）',
    },
}


def sql_quote(s):
    if s is None:
        return 'NULL'
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def render_default(default, nullable, extra):
    # 注意：生成列（STORED/VIRTUAL GENERATED）不会走到这里（main 已分流到
    # render_modify_generated），故无需在此排除；切勿用 'GENERATED' in extra
    # 判断——那会误伤 DEFAULT_GENERATED（timestamp 默认值），丢掉 DEFAULT
    # CURRENT_TIMESTAMP。
    if 'auto_increment' in extra:
        return ''
    if default is None:
        return ' DEFAULT NULL' if nullable else ''
    d = str(default)
    if d == 'CURRENT_TIMESTAMP':
        return ' DEFAULT CURRENT_TIMESTAMP'
    if re.match(r'^-?\d+(\.\d+)?$', d):
        return f' DEFAULT {d}'
    return ' DEFAULT ' + sql_quote(d)


def render_extra(extra):
    e = ''
    if 'auto_increment' in extra:
        e += ' AUTO_INCREMENT'
    if 'on update' in extra:
        e += ' ON UPDATE CURRENT_TIMESTAMP'
    return e


def render_modify(col, comment_fixed):
    name = col['COLUMN_NAME']
    ctype = col['COLUMN_TYPE']
    nullable = col['IS_NULLABLE'] == 'YES'
    extra = col['EXTRA'] or ''
    s = f'MODIFY COLUMN `{name}` {ctype} {"NULL" if nullable else "NOT NULL"}'
    s += render_default(col['COLUMN_DEFAULT'], nullable, extra)
    s += render_extra(extra)
    s += ' COMMENT ' + sql_quote(comment_fixed)
    return s


def render_modify_generated(col, comment_fixed):
    name = col['COLUMN_NAME']
    ctype = col['COLUMN_TYPE']
    expr = col['GENERATION_EXPRESSION']  # 形如 (case when ...)
    kind = 'STORED' if 'STORED' in (col['EXTRA'] or '') else 'VIRTUAL'
    return (f"MODIFY COLUMN `{name}` {ctype} GENERATED ALWAYS AS {expr} {kind}\n"
            f"  COMMENT {sql_quote(comment_fixed)}")


def main():
    parser = argparse.ArgumentParser(description='生成 COMMENT 乱码修复 SQL')
    parser.add_argument('tables', nargs='+', help='待修复的表名')
    parser.add_argument('--out', help='输出文件路径（默认 scripts/fix-<tables>-comment-mojibake.sql）')
    args = parser.parse_args()

    out_path = args.out or os.path.join(
        ROOT, 'scripts',
        f"fix-{'-'.join(args.tables)}-comment-mojibake.sql")

    conn = pymysql.connect(**DB)
    try:
        cur = conn.cursor(pymysql.cursors.DictCursor)

        header = [
            "-- =================================================================",
            f"-- fix-{'-'.join(args.tables)}-comment-mojibake.sql",
            f"-- 修复 {', '.join(args.tables)} 表 COMMENT 的 cp1252 双重编码乱码。",
            "-- ",
            "-- 生成器: scripts/gen_fix_comment_mojibake.py",
            "-- 修复原理: latin1(=cp1252) 双重编码逆向还原；字节丢失列用源文件值覆盖",
            "-- 安全性: 仅改 COMMENT；列定义取自 information_schema 现状，不动其它属性。",
            "--         执行前请先 review；本操作对正确 COMMENT 有破坏性，仅限乱码表。",
            "-- =================================================================",
            "SET NAMES utf8mb4;",
            "",
        ]

        review = []
        warnings = []

        for tbl in args.tables:
            overrides = COMMENT_OVERRIDES.get(tbl, {})

            cur.execute(f"""
                SELECT TABLE_COMMENT AS RAW,
                       {FIX % 'TABLE_COMMENT'} AS FIXED
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s
            """, (tbl,))
            trow = cur.fetchone()
            tbl_raw = trow['RAW'] if trow else ''
            tbl_good = trow['FIXED'] if trow else ''
            if '?' in tbl_good and tbl_raw and any(ord(c) > 127 for c in tbl_raw):
                warnings.append(f"{tbl}: 表级 COMMENT latin1 还原含 '?'，字节可能丢失，请人工核对")

            review.append(f"\n[{tbl}] 表级 COMMENT")
            review.append(f"  乱码: {tbl_raw}")
            review.append(f"  还原: {tbl_good}")

            cur.execute(f"""
                SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
                       EXTRA, GENERATION_EXPRESSION, ORDINAL_POSITION,
                       COLUMN_COMMENT AS RAW,
                       {FIX % 'COLUMN_COMMENT'} AS FIXED
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = %s
                ORDER BY ORDINAL_POSITION
            """, (tbl,))
            cols = cur.fetchall()

            mods = []
            for c in cols:
                raw = c['RAW'] or ''
                if c['COLUMN_NAME'] in overrides:
                    fixed = overrides[c['COLUMN_NAME']]
                    src = 'OVERRIDE(源文件)'
                else:
                    fixed = c['FIXED']
                    src = 'latin1'
                    if '?' in fixed and raw and any(ord(ch) > 127 for ch in raw):
                        warnings.append(
                            f"{tbl}.{c['COLUMN_NAME']}: latin1 还原含 '?' "
                            f"(raw={raw!r})，字节可能丢失，请补 COMMENT_OVERRIDES")

                review.append(f"  - {c['COLUMN_NAME']} [{src}]: {raw!r} -> {fixed}")

                if c['GENERATION_EXPRESSION']:
                    mods.append(render_modify_generated(c, fixed))
                else:
                    mods.append(render_modify(c, fixed))

            mods.append(f"COMMENT = {sql_quote(tbl_good)}")

            header.append(f"-- {tbl}")
            header.append(f"ALTER TABLE `{tbl}`")
            header.append("  " + ",\n  ".join(mods) + ";")
            header.append("")

        sql_text = "\n".join(header) + "\n"

        with open(out_path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(sql_text)

        print("===== 乱码→还原 对照表（review 用）=====", file=sys.stderr)
        print("\n".join(review), file=sys.stderr)
        if warnings:
            print("\n⚠️ 警告：", file=sys.stderr)
            for w in warnings:
                print(f"  {w}", file=sys.stderr)
        print(f"\n[OK] 已写入 {out_path}", file=sys.stderr)
    finally:
        conn.close()


if __name__ == '__main__':
    main()
