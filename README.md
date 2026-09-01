# BlueMap Functional Storage Add-on

[![CI](https://github.com/jan-guenter/bluemap-functional-storage-addon/actions/workflows/ci.yml/badge.svg)](https://github.com/jan-guenter/bluemap-functional-storage-addon/actions/workflows/ci.yml)

Experimental support for the static installed-material shells of Functional
Storage's ten framed blocks on the exact BlueMap 5.23 feature backport in All
the Mons 1.2.0.

Version `0.1.0-alpha.2` is the unpublished BlueMap 5.23 migration candidate.
Its production JAR is 85,323 bytes with SHA-256
`0bc4c0ed0195093487967260fc8ec2dbb4c9f389635098deb1bcc9e7839c1cfc`.
It preserves the alpha.1 renderer and gallery contract; alpha.1's staging
gallery passed all 29 assertions with zero failures at the immediate, 20-tick
and 100-tick phases on 2026-08-21. Its deterministic gallery ZIP remains
4,697 bytes with SHA-256
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
the exact BlueMap feature backport
`5.22-feature.backport-5.23-stateless-java-web-server-46` at
`7e07f4e74ec1e92a6ead9aa1e66054af3e133aac`, with API commit
`285c9a60eff3ac2b0cab308ce1058d1565be0971`.

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

Clone with `--recurse-submodules`, or initialize the exact toolkit and Adapter
API source modules:

```bash
git submodule update --init --recursive -- \
  tooling/bluemap-addon-toolkit modules/bluemap-addon-adapter-api
```

The settings preflight accepts only toolkit commit
`6cd34a8368cc4ee8628fbe830a90ec5b14960629` and Adapter API commit
`e81f08bc4bfbf02d810ec8949a019130e2e61634` with source tree
`2f974c9bb2ba13888d69682f86f30f58922d30eb`. It compiles exactly four shared
helpers as source and never installs, bundles, or nests the module JAR.
Install the corresponding toolkit and verify the repository contract before
Gradle:

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
  -PbluemapSourcePath=/absolute/path/to/BlueMap-at-7e07f4e7 \
  -PfunctionalStorageJar=/absolute/path/functionalstorage-1.21.1-1.5.8.jar \
  -PtitaniumJar=/absolute/path/titanium-1.21-4.0.45.jar \
  -PreleaseTag=v0.1.0-alpha.2 \
  clean check build generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication verifyPublicationArtifacts \
  verifyReleaseCandidate
```

The produced JAR is under `build/libs/`. Publication does not deploy it to a
Minecraft server.

## Release

The intended immutable tag is `v0.1.0-alpha.2`, and the Maven coordinate is
`io.github.jan-guenter:bluemap-functional-storage-addon:0.1.0-alpha.2`.
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
