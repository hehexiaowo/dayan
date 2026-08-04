#!/bin/bash
# 99_seed.sh - 在 DDL（01-17）执行完毕后，按顺序导入种子数据。
# MySQL 官方镜像的 docker-entrypoint 会按字典序执行 initdb.d 下的 .sql/.sh，
# 本脚本以 99_ 前缀确保排在所有 DDL 之后执行。
set -e

echo "=== 开始导入种子数据 ==="
for f in /docker-entrypoint-initdb.d/seed/*.sql; do
    if [ -f "$f" ]; then
        echo "  执行: $(basename "$f")"
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "$f"
    fi
done
echo "=== 种子数据导入完成 ==="
