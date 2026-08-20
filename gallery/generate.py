#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Generate the bounded Functional Storage framed-shell staging gallery."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parent
NAMESPACE = "functionalstorage_gallery"
OBJECTIVE = "fs_gallery"
ENVELOPE = {
    "min_x": 160,
    "max_x": 191,
    "min_y": 99,
    "max_y": 108,
    "min_z": 160,
    "max_z": 191,
}
FORCELOAD = "160 160 191 191"
FLOOR = "160 99 160 191 99 191"

STYLED_MODEL_NBT = (
    '{framedDrawerModelData:{particle:"minecraft:oak_planks",'
    'side:"minecraft:oak_planks",front:"minecraft:bricks",'
    'front_divider:"minecraft:gold_block"}}'
)
UNSTYLED_MODEL_NBT = "{framedDrawerModelData:{}}"
MODEL_KEYS = ("particle", "side", "front", "front_divider")


@dataclass(frozen=True)
class Placement:
    cell: str
    label: str
    x: int
    y: int
    z: int
    block: str
    properties: str
    material_mode: str
    nbt: str | None
    divider_visible: bool
    native_child: str

    @property
    def block_spec(self) -> str:
        return f"{self.block}[{self.properties}]"


ROUTED_HOSTS = (
    "functionalstorage:framed_1",
    "functionalstorage:framed_2",
    "functionalstorage:framed_4",
    "functionalstorage:compacting_framed_drawer",
    "functionalstorage:framed_simple_compacting_drawer",
    "functionalstorage:framed_fluid_1",
    "functionalstorage:framed_fluid_2",
    "functionalstorage:framed_fluid_4",
    "functionalstorage:framed_storage_controller",
    "functionalstorage:framed_controller_extension",
)

# The drawer-family orientations below are drawn only from the 16 combinations
# represented by the exact 1.5.8 installed multipart blockstate resources.
PLACEMENTS = (
    Placement(
        "A1", "framed 1 north", 164, 100, 164,
        "functionalstorage:framed_1",
        "facing=north,subfacing=down,locked=false",
        "styled", STYLED_MODEL_NBT, False, "-",
    ),
    Placement(
        "A2", "framed 2 east locked", 170, 100, 164,
        "functionalstorage:framed_2",
        "facing=east,subfacing=down,locked=true",
        "styled", STYLED_MODEL_NBT, True, "-",
    ),
    Placement(
        "A3", "framed 4 south inverted", 176, 100, 164,
        "functionalstorage:framed_4",
        "facing=south,subfacing=up,locked=false",
        "styled", STYLED_MODEL_NBT, True, "-",
    ),
    Placement(
        "A4", "compacting west inverted locked", 182, 100, 164,
        "functionalstorage:compacting_framed_drawer",
        "facing=west,subfacing=up,locked=true",
        "styled", STYLED_MODEL_NBT, True, "-",
    ),
    Placement(
        "B1", "simple compacting floor north", 164, 100, 170,
        "functionalstorage:framed_simple_compacting_drawer",
        "facing=down,subfacing=north,locked=false",
        "styled", STYLED_MODEL_NBT, True, "-",
    ),
    Placement(
        "B2", "fluid 1 ceiling locked empty", 170, 100, 170,
        "functionalstorage:framed_fluid_1",
        "facing=up,subfacing=down,locked=true",
        "styled", STYLED_MODEL_NBT, False, "tank",
    ),
    Placement(
        "B3", "fluid 2 floor east empty", 176, 100, 170,
        "functionalstorage:framed_fluid_2",
        "facing=down,subfacing=east,locked=false",
        "styled", STYLED_MODEL_NBT, True, "tank",
    ),
    Placement(
        "B4", "fluid 4 floor south locked empty", 182, 100, 170,
        "functionalstorage:framed_fluid_4",
        "facing=down,subfacing=south,locked=true",
        "styled", STYLED_MODEL_NBT, True, "tank",
    ),
    Placement(
        "C1", "storage controller west", 164, 100, 176,
        "functionalstorage:framed_storage_controller",
        "subfacing=west",
        "styled", STYLED_MODEL_NBT, False, "display",
    ),
    Placement(
        "C2", "controller extension east", 170, 100, 176,
        "functionalstorage:framed_controller_extension",
        "subfacing=east",
        "styled", STYLED_MODEL_NBT, False, "display",
    ),
    Placement(
        "C3", "unstyled framed 1 native shell", 176, 100, 176,
        "functionalstorage:framed_1",
        "facing=down,subfacing=west,locked=false",
        "unstyled", UNSTYLED_MODEL_NBT, False, "-",
    ),
    Placement(
        "C4", "ordinary oak 1 stock control", 182, 100, 176,
        "functionalstorage:oak_1",
        "facing=north,subfacing=down,locked=false",
        "stock", None, False, "-",
    ),
)


def text_bytes(text: str) -> bytes:
    return (text.rstrip() + "\n").encode("utf-8")


def json_bytes(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def placements_tsv() -> bytes:
    lines = [
        "cell\tlabel\tx\ty\tz\tblock\tproperties\tmaterial_mode\t"
        "framedDrawerModelData\tdivider_visible\tnative_child\tdynamic_state"
    ]
    for placement in PLACEMENTS:
        if placement.material_mode == "styled":
            model_data = (
                "particle=minecraft:oak_planks;side=minecraft:oak_planks;"
                "front=minecraft:bricks;front_divider=minecraft:gold_block"
            )
        elif placement.material_mode == "unstyled":
            model_data = "{}"
        else:
            model_data = "-"
        dynamic_state = (
            "empty-fluid" if placement.native_child == "tank" else "none-injected"
        )
        lines.append(
            "\t".join(
                (
                    placement.cell,
                    placement.label,
                    str(placement.x),
                    str(placement.y),
                    str(placement.z),
                    placement.block,
                    placement.properties,
                    placement.material_mode,
                    model_data,
                    "yes" if placement.divider_visible else "no",
                    placement.native_child,
                    dynamic_state,
                )
            )
        )
    return text_bytes("\n".join(lines))


def build_wrapper_function() -> bytes:
    guard_message = json.dumps(
        [{"text": "Functional Storage gallery already built; guard retained it."}],
        separators=(",", ":"),
    )
    return text_bytes(
        "# Generated by gallery/generate.py; do not edit.\n"
        f"scoreboard objectives add {OBJECTIVE} dummy\n"
        f"scoreboard players add #builds {OBJECTIVE} 0\n"
        f"execute if score #builds {OBJECTIVE} matches 1.. run tellraw @a "
        f"{guard_message}\n"
        f"execute if score #builds {OBJECTIVE} matches 0 run function "
        f"{NAMESPACE}:build_once"
    )


def build_once_function() -> bytes:
    lines = [
        "# Generated by gallery/generate.py; do not edit.",
        f"scoreboard players add #builds {OBJECTIVE} 1",
        f"forceload add {FORCELOAD}",
        f"function {NAMESPACE}:clear",
        f"fill {FLOOR} minecraft:smooth_stone",
    ]
    for placement in PLACEMENTS:
        lines.append(f"# {placement.cell}: {placement.label}")
        lines.append(
            f"setblock {placement.x} {placement.y} {placement.z} "
            f"{placement.block_spec}"
        )
        if placement.nbt is not None:
            lines.append(
                f"data merge block {placement.x} {placement.y} {placement.z} "
                f"{placement.nbt}"
            )
    lines.extend(
        (
            f"function {NAMESPACE}:verify_immediate",
            f"schedule function {NAMESPACE}:verify_20t 20t replace",
            f"schedule function {NAMESPACE}:verify_100t 100t replace",
        )
    )
    return text_bytes("\n".join(lines))


def append_assertion(lines: list[str], command: str) -> None:
    lines.append(
        f"{command} run scoreboard players add #failures {OBJECTIVE} 1"
    )
    lines.append(f"scoreboard players add #checked {OBJECTIVE} 1")


def verify_function() -> bytes:
    lines = [
        "# Generated by gallery/generate.py; do not edit.",
        f"scoreboard players set #failures {OBJECTIVE} 0",
        f"scoreboard players set #checked {OBJECTIVE} 0",
    ]
    for placement in PLACEMENTS:
        position = f"{placement.x} {placement.y} {placement.z}"
        lines.append(f"# {placement.cell}: {placement.label}")
        append_assertion(
            lines,
            f"execute unless block {position} {placement.block_spec}",
        )
        if placement.material_mode == "styled":
            append_assertion(
                lines,
                f"execute unless data block {position} {STYLED_MODEL_NBT}",
            )
        elif placement.material_mode == "unstyled":
            append_assertion(
                lines,
                f"execute unless data block {position} framedDrawerModelData",
            )
            for key in MODEL_KEYS:
                append_assertion(
                    lines,
                    f"execute if data block {position} framedDrawerModelData.{key}",
                )
        else:
            append_assertion(
                lines,
                f"execute if data block {position} framedDrawerModelData",
            )
    append_assertion(
        lines,
        f"execute unless score #builds {OBJECTIVE} matches 1",
    )
    return text_bytes("\n".join(lines))


def phase_function(phase: str, announce: bool) -> bytes:
    lines = [
        "# Generated by gallery/generate.py; do not edit.",
        f"function {NAMESPACE}:verify",
        f"scoreboard players operation #{phase}_failures {OBJECTIVE} = "
        f"#failures {OBJECTIVE}",
        f"scoreboard players operation #{phase}_checked {OBJECTIVE} = "
        f"#checked {OBJECTIVE}",
    ]
    if announce:
        payload = [
            {"text": f"Functional Storage gallery {phase}: "},
            {"score": {"name": f"#{phase}_checked", "objective": OBJECTIVE}},
            {"text": " checks, "},
            {"score": {"name": f"#{phase}_failures", "objective": OBJECTIVE}},
            {"text": " failures"},
        ]
        lines.append("tellraw @a " + json.dumps(payload, separators=(",", ":")))
    return text_bytes("\n".join(lines))


def base_files() -> dict[Path, bytes]:
    function_root = Path(f"datapack/data/{NAMESPACE}/function")
    return {
        Path("placements.tsv"): placements_tsv(),
        Path("datapack/pack.mcmeta"): json_bytes(
            {
                "pack": {
                    "description": (
                        "ATM 1.2.0 Functional Storage framed-shell BlueMap gallery"
                    ),
                    "pack_format": 48,
                }
            }
        ),
        Path("datapack/data/minecraft/tags/function/load.json"): json_bytes(
            {"values": [f"{NAMESPACE}:load"]}
        ),
        function_root / "load.mcfunction": text_bytes(
            "# Generated by gallery/generate.py; do not edit.\n"
            f"scoreboard objectives add {OBJECTIVE} dummy"
        ),
        function_root / "build.mcfunction": build_wrapper_function(),
        function_root / "build_once.mcfunction": build_once_function(),
        function_root / "clear.mcfunction": text_bytes(
            "# Generated by gallery/generate.py; do not edit.\n"
            f"schedule clear {NAMESPACE}:verify_20t\n"
            f"schedule clear {NAMESPACE}:verify_100t\n"
            "fill 160 99 160 191 108 191 minecraft:air"
        ),
        function_root / "verify.mcfunction": verify_function(),
        function_root / "verify_immediate.mcfunction": phase_function(
            "immediate", True
        ),
        function_root / "verify_20t.mcfunction": phase_function("20t", False),
        function_root / "verify_100t.mcfunction": phase_function("100t", True),
        function_root / "release.mcfunction": text_bytes(
            "# Generated by gallery/generate.py; do not edit.\n"
            f"schedule clear {NAMESPACE}:verify_20t\n"
            f"schedule clear {NAMESPACE}:verify_100t\n"
            f"forceload remove {FORCELOAD}"
        ),
    }


def sha256(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def generated_files() -> dict[Path, bytes]:
    files = base_files()
    checksum = "".join(
        f"{sha256(files[path])}  {path.as_posix()}\n" for path in sorted(files)
    )
    files[Path("SHA256SUMS")] = checksum.encode("ascii")
    return files


def write_or_check(check: bool) -> int:
    failures: list[str] = []
    files = generated_files()
    for relative, payload in files.items():
        path = ROOT / relative
        if check:
            if not path.is_file():
                failures.append(f"missing generated file: {relative}")
            elif path.read_bytes() != payload:
                failures.append(f"generated file differs: {relative}")
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(payload)
    if failures:
        print("\n".join(failures), file=sys.stderr)
        return 1
    action = "checked" if check else "wrote"
    print(f"{action} {len(files)} deterministic gallery files")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    return write_or_check(args.check)


if __name__ == "__main__":
    raise SystemExit(main())
