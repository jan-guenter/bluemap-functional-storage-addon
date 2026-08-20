#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Verify the exact Functional Storage and Titanium runtime artifacts."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import sys
import zipfile


FUNCTIONAL_STORAGE_SIZE = 810_628
FUNCTIONAL_STORAGE_SHA256 = (
    "e3e7368c28a24e7de5b877988aa92cc94ca7417c8f60b7d28e7f584a94a51147"
)
FUNCTIONAL_STORAGE_REQUIRED = {
    "META-INF/neoforge.mods.toml",
    "assets/functionalstorage/blockstates/framed_1.json",
    "assets/functionalstorage/blockstates/framed_storage_controller.json",
    "com/buuz135/functionalstorage/block/tile/FramedDrawerTile.class",
    "com/buuz135/functionalstorage/block/tile/FramedFluidDrawerTile.class",
    "com/buuz135/functionalstorage/block/tile/StorageControllerTile.class",
}
TITANIUM_SIZE = 606_801
TITANIUM_SHA256 = (
    "d224a9bd5cfb9e921ba644b2a7a2ce1041f879a9945f80dacebd87d95888530c"
)
TITANIUM_REQUIRED = {
    "META-INF/neoforge.mods.toml",
    "com/hrznstudio/titanium/Titanium.class",
    "com/hrznstudio/titanium/block/tile/BasicTile.class",
}


def verify(
    path: Path,
    expected_size: int,
    expected_sha256: str,
    required: set[str],
    label: str,
) -> bool:
    payload = path.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    if len(payload) != expected_size or digest != expected_sha256:
        print(
            f"{label} mismatch: {len(payload)} bytes, SHA-256 {digest}",
            file=sys.stderr,
        )
        return False
    with zipfile.ZipFile(path) as archive:
        missing = sorted(required.difference(archive.namelist()))
    if missing:
        print(f"{label} missing entries: {missing}", file=sys.stderr)
        return False
    print(f"verified {label}: {len(payload)} bytes, SHA-256 {digest}")
    return True


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--functional-storage-jar", required=True, type=Path)
    parser.add_argument("--titanium-jar", required=True, type=Path)
    args = parser.parse_args()
    if not verify(
        args.functional_storage_jar,
        FUNCTIONAL_STORAGE_SIZE,
        FUNCTIONAL_STORAGE_SHA256,
        FUNCTIONAL_STORAGE_REQUIRED,
        "Functional Storage 1.5.8",
    ):
        return 1
    if not verify(
        args.titanium_jar,
        TITANIUM_SIZE,
        TITANIUM_SHA256,
        TITANIUM_REQUIRED,
        "Titanium 4.0.45",
    ):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
