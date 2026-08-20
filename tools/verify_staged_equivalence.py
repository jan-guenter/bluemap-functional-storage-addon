#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Prove the release JAR's functional entries equal the accepted staged JAR."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import zipfile


IGNORED_ENTRY = "META-INF/MANIFEST.MF"


def read_expected(path: Path) -> dict[str, str]:
    expected: dict[str, str] = {}
    for number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        digest, separator, entry = line.partition("  ")
        if not separator or len(digest) != 64 or not entry:
            raise SystemExit(f"Malformed entry manifest line {number}: {raw_line!r}")
        if entry in expected:
            raise SystemExit(f"Duplicate entry in manifest: {entry}")
        expected[entry] = digest
    return expected


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--entries", type=Path, required=True)
    args = parser.parse_args()

    expected = read_expected(args.entries)
    with zipfile.ZipFile(args.jar) as archive:
        names = {
            info.filename
            for info in archive.infolist()
            if not info.is_dir() and info.filename != IGNORED_ENTRY
        }
        if names != set(expected):
            missing = sorted(set(expected) - names)
            extra = sorted(names - set(expected))
            raise SystemExit(f"Accepted entry set changed; missing={missing}, extra={extra}")
        changed = []
        for entry, expected_digest in sorted(expected.items()):
            actual_digest = hashlib.sha256(archive.read(entry)).hexdigest()
            if actual_digest != expected_digest:
                changed.append((entry, expected_digest, actual_digest))
        try:
            manifest = archive.read(IGNORED_ENTRY).decode("utf-8")
        except KeyError:
            raise SystemExit(f"Release JAR is missing {IGNORED_ENTRY}") from None

    if changed:
        detail = "\n".join(
            f"{entry}: {actual} != {expected}"
            for entry, expected, actual in changed
        )
        raise SystemExit(f"Accepted entry bytes changed:\n{detail}")
    if "Implementation-Version: 0.1.0-alpha.1\r\n" not in manifest:
        raise SystemExit("Final release manifest version is missing or incorrect")
    if "SNAPSHOT" in manifest:
        raise SystemExit("Final release manifest contains SNAPSHOT")
    print(
        f"Verified {len(expected)} non-manifest entries byte-for-byte against "
        "the owner-accepted staged JAR."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
