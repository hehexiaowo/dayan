#!/bin/bash
# 99_seed.sh - 在编号迁移（01-36）执行完毕后，按字典序导入 seed/ 目录种子数据。
# MySQL 官方镜像的 docker-entrypoint 会按字典序执行 initdb.d 下的 .sql/.sh，
# 本脚本以 99_ 前缀确保排在所有编号 SQL 之后执行。
#
# 注意：seed/rbac_demo_seed.sql（演示账号 operator/op123）被有意排除，
# 仅供本地开发手动执行：mysql -uroot -p dayan < seed/rbac_demo_seed.sql
set -e

echo "=== 开始导入种子数据 ==="
for f in /docker-entrypoint-initdb.d/seed/*.sql; do
    if [ -f "$f" ]; then
        if [ "$(basename "$f")" = "rbac_demo_seed.sql" ]; then
            echo "  跳过（演示账号，仅手动执行）: $(basename "$f")"
            continue
        fi
        echo "  执行: $(basename "$f")"
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "$f"
    fi
done
echo "=== 种子数据导入完成 ==="
