# BlueMap Functional Storage Add-on

[![CI](https://github.com/jan-guenter/bluemap-functional-storage-addon/actions/workflows/ci.yml/badge.svg)](https://github.com/jan-guenter/bluemap-functional-storage-addon/actions/workflows/ci.yml)

Experimental BlueMap 5.22 support for the static installed-material shells of
Functional Storage's ten framed blocks in All the Mons 1.2.0.

Version `0.1.0-alpha.1` is the owner-accepted release candidate. Its final
production JAR is 82,274 bytes with SHA-256
`ffe80812802c2c8f68d50ee74f17dd492561f3b44a9e650545064f66e285b048`.
The accepted staging gallery passed all 29 assertions with zero failures at
the immediate, 20-tick and 100-tick phases on 2026-08-21. Its deterministic
gallery ZIP is 4,697 bytes with SHA-256
`51dd3845398b9c624631e227468bcce65e693dce15e2f764c67dfe8ba9fddd27`.

## Exact compatibility profile

The add-on activates only when both complete installed artifacts match:

| Artifact | Exact version | Size | SHA-256 |
| --- | --- | ---: | --- |
| Functional Storage | `1.21.1-1.5.8` | 810,628 B | `e3e7368c28a24e7de5b877988aa92cc94ca7417c8f60b7d28e7f584a94a51147` |
| Titanium | `1.21-4.0.45` | 606,801 B | `d224a9bd5cfb9e921ba644b2a7a2ce1041f879a9945f80dacebd87d95888530c` |

Exactly one distinct installed JAR declaring each target mod ID is required;
repeated paths or symlink aliases of that same real file are deduplicated, while
conflicting or byte-identical distinct copies disable the add-on. The runtime
profile is also pinned to Minecraft 1.21.1, NeoForge 21.1.248 and
the audited BlueMap backport
`5.22-agent.backport-5.22-mc1.21.1-2` at
`9be321df995a1103808621d529eb72773e719d4d`.

## Rendered scope

The profile owns exactly:

- `framed_1`, `framed_2`, `framed_4`;
- `compacting_framed_drawer`, `framed_simple_compacting_drawer`;
- `framed_fluid_1`, `framed_fluid_2`, `framed_fluid_4`;
- `framed_storage_controller`, `framed_controller_extension`.

All IDs are in the `functionalstorage` namespace. The renderer requires a
same-named block entity, a legal installed orientation, and strict
`framedDrawerModelData`. Controller hosts admit exactly four horizontal
`subfacing` values times the two canonical `locked` values. Both lock values
use the same installed shell without a lock overlay; other controller states
remain stock. Absent, null, or empty design data emits the complete native
installed shell. Any non-empty styled design must contain
exactly `particle`, `side`, `front`, and `front_divider`; all four values must
resolve to admitted BlockItem default-state materials. `particle` is
validation-only, and `front_divider` is validation-only on hosts without that
installed child.
Only `front`, `side`, and an installed `front_divider` are substituted. Native
`tank` and `display` children plus the ordinary lock overlay stay
resource-driven, including the exact native controller-display animation.

Effective child geometry must match structural signatures derived at runtime
from the exact Functional Storage artifact. Operator PNG replacements through
the same stable texture keys remain supported; model-geometry replacement
disables the custom route. Persisted target materials are limited to ordinary,
one-layer, non-animated opaque canonical full cubes.

Inventory icons/counts, fluid fill, upgrades/activity, controller link/range,
Ender Drawer frequency, armory contents, particles, target-material animation,
and every other Functional Storage block remain stock. Missing or unsupported
input falls back atomically to BlueMap's stock renderer; removing the add-on
and restarting restores stock behavior without world migration.

## Source and asset policy

The implementation is MIT-licensed and uses an installed-resource interpreter.
It adapts only identified MIT owner-code patterns recorded in `NOTICE.md` and
`provenance/upstreams.json`. No Functional Storage/Titanium/Minecraft assets,
classes, JARs, or source are bundled. Functional Storage source tag
`1.21-1.5.8b` at `cf64288607d1a40e0f45542e7378853b7346825c` is the strongest
release-time correlation available; the complete runtime JAR hash, not that
correlation, controls activation.

## Build

Clone with `--recurse-submodules`, or initialize an existing checkout with
`git submodule update --init --recursive -- tooling/bluemap-addon-toolkit`.
The settings preflight accepts only toolkit commit
`6cd34a8368cc4ee8628fbe830a90ec5b14960629` and rejects an uninitialized,
changed, or dirty checkout. Install the corresponding toolkit and verify the
repository contract before Gradle:

```bash
python -m pip install --disable-pip-version-check --no-deps \
  --require-hashes --only-binary=:all: \
  --requirement requirements/toolkit.txt
bluemap-addon-toolkit conventions check .
```

The requirement locks the 20,585-byte `v0.3.0-alpha.1` wheel at SHA-256
`82f1ec53603646849a7c2d4b58f3fb7000413fe83043a302bee88cc88daeb8f7`.

```bash
gradle --no-daemon \
  -PfunctionalStorageJar=/absolute/path/functionalstorage-1.21.1-1.5.8.jar \
  -PtitaniumJar=/absolute/path/titanium-1.21-4.0.45.jar \
  -PreleaseTag=v0.1.0-alpha.1 \
  clean check build generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication verifyPublicationArtifacts \
  verifyReleaseCandidate
```

The produced JAR is under `build/libs/`. Publication does not deploy it to a
Minecraft server.

## Release

The intended immutable tag is `v0.1.0-alpha.1`, and the Maven coordinate is
`io.github.jan-guenter:bluemap-functional-storage-addon:0.1.0-alpha.1`.
Publication is allowed only after the independently audited pull request and
its final-head CI pass. See [the release procedure](docs/RELEASING.md) and
[recorded candidate provenance](provenance/release.json).

## Disposable comparison gallery

`gallery/` contains the independently generated twelve-anchor staging
datapack: all ten routed hosts, one empty-data native-shell control, and one
ordinary oak-drawer control. It deliberately injects no inventories, fluid
amount, upgrades, controller state, particles, or animation. Its generator,
29 retained assertions per phase, checksums, and deterministic packaging are
documented in `gallery/README.md`.
