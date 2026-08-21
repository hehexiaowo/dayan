#!/usr/bin/env python3
"""
Restore park images to MinIO — with local cache.

Downloads images from yl-web proxy and keeps local copies.
Uploads to MinIO. Can be re-run safely (skips existing).

Usage:
  python restore_images_cached.py [--upload-only]
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
CACHE_DIR = os.path.join(os.environ.get("TEMP", "/tmp"), "park_image_cache")
MINIO_ENDPOINT = "localhost:9000"
MINIO_ACCESS_KEY = "dayan"
MINIO_SECRET_KEY = "dayan12345"


def build_tasks():
    """Build (orig_filename, minio_key) from TSV + system_asset DB."""
    tasks = []

    # Get existing DB records (asset_url + asset_name for UUID/ys- filenames)
    try:
        result = subprocess.run(
            ["docker", "exec", "dayan-mysql", "mysql", "-u", "root", "-proot123",
             "-e", "USE dayan; SELECT asset_url, asset_name FROM system_asset "
                   "WHERE ref_type1='park' AND asset_url LIKE 'park/migration%' "
                   "AND asset_name IS NOT NULL AND asset_name != '' "
                   "AND (asset_name REGEXP '^[0-9a-f]{8}-' OR asset_name LIKE 'ys-%');"],
            capture_output=True, text=True, timeout=30
        )
        for line in result.stdout.strip().split("\n")[1:]:
            parts = line.split("\t")
            if len(parts) >= 2:
                tasks.append((parts[1].strip(), parts[0].strip()))
    except Exception as e:
        print(f"Warning: DB read failed: {e}")

    # Deduplicate
    seen = set()
    unique = []
    for orig, key in tasks:
        if key not in seen:
            seen.add(key)
            unique.append((orig, key))
    return unique


def download(orig_fn, local_path):
    """Download image, return True if successful."""
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
    parser.add_argument("--upload-only", action="store_true", help="Only upload cached files, skip download")
    args = parser.parse_args()

    print("=" * 60)
    print("Park Image Restore (with local cache)")
    print("=" * 60)
    print(f"Cache dir: {CACHE_DIR}")

    tasks = build_tasks()
    print(f"Tasks from DB: {len(tasks)}")

    os.makedirs(CACHE_DIR, exist_ok=True)

    # Connect MinIO
    mc = Minio(MINIO_ENDPOINT, access_key=MINIO_ACCESS_KEY,
               secret_key=MINIO_SECRET_KEY, secure=False)
    existing = set()
    for obj in mc.list_objects(MINIO_BUCKET, prefix="park/", recursive=True):
        existing.add(obj.object_name)
    print(f"Already in MinIO: {len(existing)}")

    # Phase 1: Download (with cache)
    if not args.upload_only:
        print(f"\n[Download] Processing {len(tasks)} images...")
        dl_ok = 0
        dl_skip = 0
        dl_fail = 0
        for i, (orig_fn, minio_key) in enumerate(tasks):
            if (i + 1) % 50 == 0:
                print(f"  Progress: {i+1}/{len(tasks)} (ok={dl_ok}, cached={dl_skip}, fail={dl_fail})")

            ext = os.path.splitext(orig_fn)[1].lower() or ".png"
            cache_name = minio_key.replace("/", "_")
            local_path = os.path.join(CACHE_DIR, cache_name)

            # Skip if already cached
            if os.path.exists(local_path) and os.path.getsize(local_path) > 100:
                dl_skip += 1
                continue

            if download(orig_fn, local_path):
                dl_ok += 1
            else:
                dl_fail += 1

            # Rate limit: 0.2s between requests
            time.sleep(0.2)

        print(f"\n  Download done: ok={dl_ok}, cached={dl_skip}, fail={dl_fail}")

    # Phase 2: Upload cached files to MinIO
    print(f"\n[Upload] Uploading to MinIO...")
    up_ok = 0
    up_skip = 0
    up_fail = 0

    for i, (orig_fn, minio_key) in enumerate(tasks):
        if minio_key in existing:
            up_skip += 1
            continue

        ext = os.path.splitext(orig_fn)[1].lower() or ".png"
        cache_name = minio_key.replace("/", "_")
        local_path = os.path.join(CACHE_DIR, cache_name)

        if not os.path.exists(local_path) or os.path.getsize(local_path) < 100:
            up_fail += 1
            continue

        try:
            ct = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
            mc.fput_object(MINIO_BUCKET, minio_key, local_path, content_type=ct)
            up_ok += 1
        except S3Error as e:
            up_fail += 1

    # Also upload from TSV (non-UUID names that aren't in DB with UUID)
    # ... (handled by the main tasks list)

    final = len(list(mc.list_objects(MINIO_BUCKET, prefix="park/", recursive=True)))
    print(f"\n  Upload done: ok={up_ok}, already_in_minio={up_skip}, fail={up_fail}")
    print(f"  Total in MinIO: {final}")


if __name__ == "__main__":
    main()
