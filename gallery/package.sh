#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 /path/to/output.zip" >&2
  exit 2
fi

output_path="$(realpath -m -- "$1")"
output_parent="$(dirname -- "$output_path")"
if [[ ! -d "$output_parent" ]]; then
  echo "output directory does not exist: $output_parent" >&2
  exit 2
fi

gallery_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
archive_temp="$(mktemp -d /tmp/bluemap-functional-storage-gallery.XXXXXX)"
cleanup() { rm -rf -- "$archive_temp"; }
trap cleanup EXIT

PYTHONDONTWRITEBYTECODE=1 python3 "$gallery_root/generate.py" --check
PYTHONDONTWRITEBYTECODE=1 python3 "$gallery_root/lint.py"
(cd "$gallery_root" && sha256sum --check SHA256SUMS)

mkdir -p "$archive_temp/root"
cp -a "$gallery_root/datapack/." "$archive_temp/root/"
find "$archive_temp/root" -type d -exec chmod 0755 {} +
find "$archive_temp/root" -type f -exec chmod 0644 {} +
find "$archive_temp/root" -exec touch -h -t 198001010000.00 {} +
(
  cd "$archive_temp/root"
  LC_ALL=C find . -type f -printf '%P\n' | LC_ALL=C sort |
    zip -q -X -9 "$archive_temp/functional-storage-gallery.zip" -@
)
unzip -tq "$archive_temp/functional-storage-gallery.zip"
cp "$archive_temp/functional-storage-gallery.zip" "$output_path"
sha256sum "$output_path"
