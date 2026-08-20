# Functional Storage framed-shell staging gallery

This directory defines a tiny deterministic datapack for the exact Functional
Storage `1.21.1-1.5.8` framed-shell prototype. It is confined to inclusive x
`160..191`, y `99..108`, z `160..191` in the disposable staging world. It
does not touch production or cluster state.

The gallery has exactly twelve anchors:

| Cell | Position | Host and exact state | Static fixture intent |
| --- | --- | --- | --- |
| A1 | `164 100 164` | `framed_1[facing=north,subfacing=down,locked=false]` | styled one-drawer shell |
| A2 | `170 100 164` | `framed_2[facing=east,subfacing=down,locked=true]` | styled two-drawer shell plus native lock overlay |
| A3 | `176 100 164` | `framed_4[facing=south,subfacing=up,locked=false]` | styled four-drawer shell |
| A4 | `182 100 164` | `compacting_framed_drawer[facing=west,subfacing=up,locked=true]` | styled compacting shell plus native lock overlay |
| B1 | `164 100 170` | `framed_simple_compacting_drawer[facing=down,subfacing=north,locked=false]` | styled simple-compacting shell |
| B2 | `170 100 170` | `framed_fluid_1[facing=up,subfacing=down,locked=true]` | empty styled fluid shell, native tank and lock retained |
| B3 | `176 100 170` | `framed_fluid_2[facing=down,subfacing=east,locked=false]` | empty styled fluid shell and native tank retained |
| B4 | `182 100 170` | `framed_fluid_4[facing=down,subfacing=south,locked=true]` | empty styled fluid shell, native tank and lock retained |
| C1 | `164 100 176` | `framed_storage_controller[subfacing=west]` | styled controller with native display child retained |
| C2 | `170 100 176` | `framed_controller_extension[subfacing=east]` | styled extension with native display child retained |
| C3 | `176 100 176` | `framed_1[facing=down,subfacing=west,locked=false]` | canonical empty model-data/native-shell control |
| C4 | `182 100 176` | `oak_1[facing=north,subfacing=down,locked=false]` | ordinary stock-resource control |

All ten routed anchors use the exact persisted compound:

```text
framedDrawerModelData:{particle:"minecraft:oak_planks",side:"minecraft:oak_planks",front:"minecraft:bricks",front_divider:"minecraft:gold_block"}
```

Oak planks therefore drive the particle and side material, bricks drive the
front, and gold drives `front_divider` on the six installed shells that have
that child. The unstyled control uses the canonical empty
`framedDrawerModelData:{}` map. The ordinary oak drawer receives no custom
NBT. The exact mixed orientations come only from the legal installed 1.5.8
blockstate combinations.

No item stacks, counts, fluid amount, upgrades, activity, controller
link/range/status, particle commands, or animation state are injected. The
three fluid block entities are left empty. `tank`, `display`, and the ordinary
lock multipart remain installed-resource visual checks, as recorded in
`placements.tsv`.

## Generate, lint, and package

Run from the repository root:

```text
PYTHONDONTWRITEBYTECODE=1 python3 gallery/generate.py --check
PYTHONDONTWRITEBYTECODE=1 python3 gallery/lint.py
bash gallery/package.sh /tmp/bluemap-functional-storage-gallery.zip
```

Running `gallery/generate.py` without `--check` rewrites only the generated
ledger, datapack files, and `SHA256SUMS`. Packaging uses sorted paths, fixed
file modes, stripped ZIP metadata, and a fixed DOS epoch. It bundles no
Functional Storage, Titanium, Minecraft, or BlueMap resource.

## Staging functions and retained checks

```text
/function functionalstorage_gallery:build
/function functionalstorage_gallery:verify
/function functionalstorage_gallery:clear
/function functionalstorage_gallery:release
```

`build` is a strong build-once guard. Only `#builds = 0` may call the internal
`build_once` mutator; later invocations retain the existing anchors and report
that the guard fired. For a deliberate fresh disposable run, call `clear`, set
`#builds` in objective `fs_gallery` back to zero, then call `build` again.

The verifier runs immediately and at 20 and 100 ticks. Every phase performs
29 assertions: twelve exact host states, ten exact styled compounds, presence
plus four-key absence for the empty unstyled compound, absence of model data
on the stock control, and the one-build counter. Require:

```text
#immediate_checked = 29   #immediate_failures = 0
#20t_checked       = 29   #20t_failures       = 0
#100t_checked      = 29   #100t_failures      = 0
```

`release` cancels delayed checks and removes only this gallery's bounded
forceload ticket. It deliberately retains all twelve anchors for BlueMap and
client comparison.
