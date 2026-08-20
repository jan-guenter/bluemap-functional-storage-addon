# Agent guide for the Functional Storage BlueMap add-on

This is the standalone repository for the exact-gated Functional Storage
BlueMap renderer. Read this file and `README.md` before changing the project.

## Frozen prototype scope

- Target Java 21, Minecraft 1.21.1, NeoForge 21.1.248 and the audited BlueMap
  5.22 Java-21 backport.
- Activate only when both exact installed artifacts match the byte identities
  in `provenance/upstreams.json`: Functional Storage 1.5.8 and Titanium 4.0.45.
- Own exactly the ten block IDs in the packaged profile. Each host must have a
  same-named block-entity ID and a legal exact-profile orientation state.
- Read only `framedDrawerModelData`. Absent, null, or empty data is the native
  installed shell; non-empty styled data must contain exactly `particle`,
  `front`, `side`, and `front_divider`. Validate all four materials and
  substitute only the three face roles that have an installed child.
- Preserve installed native `tank` and `display` children and the ordinary
  installed lock overlay, including exact native controller-display animation.
  Dynamic contents, fluids, counts, upgrades, status, range/link state,
  particles, and persisted target-material animation remain excluded.
- Material admission is an ordinary one-layer canonical full-cube BlockItem
  default state with non-animated opaque textures. Effective installed child
  geometry must match the exact artifact-derived structural signature; stable
  texture-key PNG overrides remain allowed. Any unknown artifact, host, block
  entity, state, NBT, item, model, texture, or capacity failure falls back
  atomically to stock rendering.
- Runtime code may interpret operator-installed assets but must not package
  Functional Storage, Titanium, Minecraft, or other upstream assets/classes.
  Production source is MIT. Do not copy Functional Storage source or the LGPL
  FramedBlocks implementation.

## Repository boundaries

- `gallery/**` is exclusively reserved for the independent gallery owner.
  Other agents must not create, edit, format, stage, or delete anything there.
- Keep third-party JARs, extracted resources, server state, maps, screenshots,
  and private fixtures untracked and outside this repository.
- Do not touch root coordination, staging infrastructure, remotes, tags, or
  releases from this repository task.
- Preserve unrelated changes and stage explicit paths only.

## Focused prototype gate

```bash
gradle --no-daemon clean test jar verifyProductionJar
```

When exact artifact fixtures are available, also run:

```bash
gradle --no-daemon test \
  -PfunctionalStorageJar=/path/to/functionalstorage-1.21.1-1.5.8.jar \
  -PtitaniumJar=/path/to/titanium-1.21-4.0.45.jar
```

Report only checks actually observed. A loadable local JAR is a prototype, not
a release or production-deployment authorization.
