# BlueMap Functional Storage Add-on

Experimental BlueMap 5.22 support for the static installed-material shells of
Functional Storage's ten framed blocks in All the Mons 1.2.0.

## Exact compatibility profile

The add-on activates only when both complete installed artifacts match:

| Artifact | Exact version | Size | SHA-256 |
| --- | --- | ---: | --- |
| Functional Storage | `1.21.1-1.5.8` | 810,628 B | `e3e7368c28a24e7de5b877988aa92cc94ca7417c8f60b7d28e7f584a94a51147` |
| Titanium | `1.21-4.0.45` | 606,801 B | `d224a9bd5cfb9e921ba644b2a7a2ce1041f879a9945f80dacebd87d95888530c` |

The runtime profile is also pinned to Minecraft 1.21.1, NeoForge 21.1.248 and
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
`framedDrawerModelData`. It interprets the exact installed
`functionalstorage:framedblock` child-shell JSON, validates `particle`, and
substitutes only `front`, `side`, and `front_divider` with admitted BlockItem
default-state materials. Native `tank` and `display` children plus the ordinary
lock overlay stay resource-driven.

Inventory icons/counts, fluid fill, upgrades/activity, controller link/range,
Ender Drawer frequency, armory contents, particles, animation, and every other
Functional Storage block remain stock. Missing or unsupported input falls back
atomically to BlueMap's stock renderer; removing the add-on and restarting
restores stock behavior without world migration.

## Source and asset policy

The implementation is MIT-licensed and uses an installed-resource interpreter.
It adapts only identified MIT owner-code patterns recorded in `NOTICE.md` and
`provenance/upstreams.json`. No Functional Storage/Titanium/Minecraft assets,
classes, JARs, or source are bundled. Functional Storage source tag
`1.21-1.5.8b` at `cf64288607d1a40e0f45542e7378853b7346825c` is the strongest
release-time correlation available; the complete runtime JAR hash, not that
correlation, controls activation.

## Build

```bash
gradle --no-daemon clean test jar verifyProductionJar
```

The produced JAR is under `build/libs/`. This prototype is not released and is
not authorized for production deployment.
