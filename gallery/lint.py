#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Lint the generated Functional Storage gallery without starting Minecraft."""

from __future__ import annotations

from collections import Counter
import json
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True
import generate


ROOT = Path(__file__).resolve().parent
EXPECTED_CHECKS = 29
DRAWER_HOSTS = frozenset(generate.ROUTED_HOSTS[:8]) | {
    "functionalstorage:oak_1"
}
CONTROLLER_HOSTS = frozenset(generate.ROUTED_HOSTS[8:])
DIRECTIONS = frozenset(("down", "up", "north", "south", "west", "east"))
LEGAL_DRAWER_ORIENTATIONS = (
    {(facing, "down") for facing in DIRECTIONS}
    | {(facing, "up") for facing in DIRECTIONS}
    | {("down", subfacing) for subfacing in ("north", "south", "west", "east")}
)


def fail(message: str) -> None:
    raise ValueError(message)


def parse_properties(raw: str) -> dict[str, str]:
    properties: dict[str, str] = {}
    for entry in raw.split(","):
        key, separator, value = entry.partition("=")
        if separator != "=" or not key or not value or key in properties:
            fail(f"malformed block property list: {raw}")
        properties[key] = value
    return properties


def main() -> int:
    expected = generate.generated_files()
    for relative, payload in expected.items():
        path = ROOT / relative
        if not path.is_file() or path.read_bytes() != payload:
            fail(f"generated file differs: {relative}")

    expected_datapack_files = {
        relative for relative in expected if relative.parts[0] == "datapack"
    }
    actual_datapack_files = {
        path.relative_to(ROOT)
        for path in (ROOT / "datapack").rglob("*")
        if path.is_file()
    }
    if actual_datapack_files != expected_datapack_files:
        fail("datapack contains unexpected or missing files")

    json.loads((ROOT / "datapack/pack.mcmeta").read_text(encoding="utf-8"))
    json.loads(
        (ROOT / "datapack/data/minecraft/tags/function/load.json").read_text(
            encoding="utf-8"
        )
    )

    function_root = ROOT / f"datapack/data/{generate.NAMESPACE}/function"
    build = (function_root / "build.mcfunction").read_text(encoding="utf-8")
    build_once = (function_root / "build_once.mcfunction").read_text(
        encoding="utf-8"
    )
    clear = (function_root / "clear.mcfunction").read_text(encoding="utf-8")
    load = (function_root / "load.mcfunction").read_text(encoding="utf-8")
    release = (function_root / "release.mcfunction").read_text(encoding="utf-8")
    verify = (function_root / "verify.mcfunction").read_text(encoding="utf-8")
    all_functions = "\n".join(
        path.read_text(encoding="utf-8")
        for path in sorted(function_root.glob("*.mcfunction"))
    )

    if len(generate.PLACEMENTS) != 12:
        fail("gallery must define exactly 12 anchors")
    if tuple(placement.block for placement in generate.PLACEMENTS[:10]) != (
        generate.ROUTED_HOSTS
    ):
        fail("first ten anchors must cover the routed host roster exactly once")
    mode_census = Counter(
        placement.material_mode for placement in generate.PLACEMENTS
    )
    if mode_census != Counter({"styled": 10, "unstyled": 1, "stock": 1}):
        fail(f"unexpected material-mode census: {mode_census}")
    if generate.PLACEMENTS[10].block != "functionalstorage:framed_1":
        fail("unstyled control must be functionalstorage:framed_1")
    if generate.PLACEMENTS[11].block != "functionalstorage:oak_1":
        fail("stock control must be functionalstorage:oak_1")

    coordinates = [
        (placement.x, placement.y, placement.z)
        for placement in generate.PLACEMENTS
    ]
    if len(set(coordinates)) != len(coordinates):
        fail("anchor coordinates must be unique")
    envelope = generate.ENVELOPE
    for x, y, z in coordinates:
        if not (
            envelope["min_x"] <= x <= envelope["max_x"]
            and envelope["min_y"] <= y <= envelope["max_y"]
            and envelope["min_z"] <= z <= envelope["max_z"]
        ):
            fail(f"anchor escaped safe envelope: {(x, y, z)}")

    locked_count = 0
    observed_facing: set[str] = set()
    observed_subfacing: set[str] = set()
    for placement in generate.PLACEMENTS:
        properties = parse_properties(placement.properties)
        if placement.block in DRAWER_HOSTS:
            if set(properties) != {"facing", "subfacing", "locked"}:
                fail(f"drawer has unexpected properties: {placement.cell}")
            orientation = (properties["facing"], properties["subfacing"])
            if orientation not in LEGAL_DRAWER_ORIENTATIONS:
                fail(f"illegal exact-profile orientation: {placement.cell}")
            if properties["locked"] not in {"true", "false"}:
                fail(f"invalid locked property: {placement.cell}")
            if properties["locked"] == "true":
                locked_count += 1
            observed_facing.add(properties["facing"])
            observed_subfacing.add(properties["subfacing"])
        elif placement.block in CONTROLLER_HOSTS:
            if set(properties) != {"subfacing"} or properties["subfacing"] not in {
                "north", "south", "west", "east"
            }:
                fail(f"controller has illegal exact-profile state: {placement.cell}")
            observed_subfacing.add(properties["subfacing"])
        else:
            fail(f"unexpected host block: {placement.block}")
    if locked_count != 4:
        fail("gallery must contain exactly four representative locked drawers")
    if observed_facing != DIRECTIONS:
        fail(f"mixed drawer facings are incomplete: {observed_facing}")
    if observed_subfacing != DIRECTIONS:
        fail(f"mixed subfacings are incomplete: {observed_subfacing}")

    native_census = Counter(
        placement.native_child
        for placement in generate.PLACEMENTS
        if placement.native_child != "-"
    )
    if native_census != Counter({"tank": 3, "display": 2}):
        fail(f"native child coverage changed: {native_census}")
    if sum(placement.divider_visible for placement in generate.PLACEMENTS) != 6:
        fail("exactly six installed shells must expose front_divider")

    for placement in generate.PLACEMENTS:
        if placement.material_mode == "styled":
            if placement.nbt != generate.STYLED_MODEL_NBT:
                fail(f"noncanonical styled model data: {placement.cell}")
        elif placement.material_mode == "unstyled":
            if placement.nbt != generate.UNSTYLED_MODEL_NBT:
                fail("unstyled control must use the canonical empty compound")
        elif placement.nbt is not None:
            fail("stock control must not inject block-entity NBT")

    if len(re.findall(r"^setblock ", build_once, re.MULTILINE)) != 12:
        fail("build_once must contain exactly 12 setblock anchors")
    if len(re.findall(r"^data merge block ", build_once, re.MULTILINE)) != 11:
        fail("build_once must contain ten styled and one unstyled NBT merge")
    if build_once.count(generate.STYLED_MODEL_NBT) != 10:
        fail("styled model data must appear exactly ten times")
    if build_once.count(generate.UNSTYLED_MODEL_NBT) != 1:
        fail("empty unstyled model data must appear exactly once")
    if len(
        re.findall(
            rf"^scoreboard players add #checked {generate.OBJECTIVE} 1$",
            verify,
            re.MULTILINE,
        )
    ) != EXPECTED_CHECKS:
        fail(f"verify must contain exactly {EXPECTED_CHECKS} retained checks")

    guard_call = (
        f"execute if score #builds {generate.OBJECTIVE} matches 0 run function "
        f"{generate.NAMESPACE}:build_once"
    )
    if build.count(guard_call) != 1 or "matches 1.. run tellraw @a" not in build:
        fail("build wrapper must enforce and announce the build-once guard")
    if "setblock " in build or "data merge " in build or "fill " in build:
        fail("guarded build wrapper must not mutate the gallery directly")
    increment = f"scoreboard players add #builds {generate.OBJECTIVE} 1"
    if build_once.count(increment) != 1 or increment in build:
        fail("build counter must increment exactly once inside build_once")

    expected_clear = "fill 160 99 160 191 108 191 minecraft:air"
    if set(re.findall(r"^fill .* minecraft:air$", clear, re.MULTILINE)) != {
        expected_clear
    }:
        fail("clear must cover the complete bounded envelope exactly once")
    if f"forceload add {generate.FORCELOAD}" not in build_once:
        fail("build_once must add the exact bounded forceload ticket")
    if f"forceload remove {generate.FORCELOAD}" not in release:
        fail("release must remove the exact bounded forceload ticket")
    if "forceload" in load:
        fail("datapack load must not create a forceload ticket")

    for phase, delay in (("20t", "20t"), ("100t", "100t")):
        schedule = (
            f"schedule function {generate.NAMESPACE}:verify_{phase} "
            f"{delay} replace"
        )
        if build_once.count(schedule) != 1:
            fail(f"missing exact {phase} retained-state schedule")
        clear_schedule = f"schedule clear {generate.NAMESPACE}:verify_{phase}"
        if clear.count(clear_schedule) != 1 or release.count(clear_schedule) != 1:
            fail(f"clear/release must cancel the {phase} retained check")

    forbidden_commands = ("summon ", "particle ", "item replace ", "loot ", "give ")
    for line in all_functions.lower().splitlines():
        command = line.lstrip()
        for token in forbidden_commands:
            if command.startswith(token) or f" run {token}" in command:
                fail(f"forbidden gallery operation present: {token}")
    forbidden_dynamic_nbt = (
        "amount:", "fluidstack", "upgrades:", "items:", "count:",
        "controllerpos", "linked", "range:", "frequency:", "active:"
    )
    for placement in generate.PLACEMENTS:
        lowered_nbt = (placement.nbt or "").lower()
        for token in forbidden_dynamic_nbt:
            if token in lowered_nbt:
                fail(f"dynamic NBT leaked into {placement.cell}: {token}")

    print(
        "Functional Storage gallery lint passed: "
        f"12 anchors, {EXPECTED_CHECKS} checks/phase, 3 tank + 2 display controls"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ValueError as error:
        print(f"lint failed: {error}", file=sys.stderr)
        raise SystemExit(1)
