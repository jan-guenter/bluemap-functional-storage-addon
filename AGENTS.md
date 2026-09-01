# Agent guide for the Functional Storage BlueMap add-on

This is the standalone repository for the exact-gated Functional Storage
BlueMap renderer. Read this file and `README.md` before changing the project.

## Frozen release scope

- Target Java 21, Minecraft 1.21.1, NeoForge 21.1.248 and exact BlueMap
  feature-backport commit `7e07f4e74ec1e92a6ead9aa1e66054af3e133aac` with
  API commit `285c9a60eff3ac2b0cab308ce1058d1565be0971`.
- Compile the four Adapter API helpers from exact gitlink
  `e81f08bc4bfbf02d810ec8949a019130e2e61634`; never install, bundle, or nest
  its standalone JAR. Keep local adapter code under `adapter.bluemap523`.
- Activate only when both exact installed artifacts match the byte identities
  in `provenance/upstreams.json`: Functional Storage 1.5.8 and Titanium 4.0.45.
- Own exactly the ten block IDs in the packaged profile. Each host must have a
  same-named block-entity ID and a legal exact-profile orientation state.
  Controller hosts admit exactly four horizontal `subfacing` values times the
  two canonical `locked` values; both use the same shell without a lock overlay.
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

## Release gate

```bash
git submodule update --init --recursive -- \
  tooling/bluemap-addon-toolkit modules/bluemap-addon-adapter-api
gradle --no-daemon \
  -PbluemapSourcePath=/absolute/path/to/BlueMap-at-7e07f4e7 \
  -PfunctionalStorageJar=/absolute/path/functionalstorage-1.21.1-1.5.8.jar \
  -PtitaniumJar=/absolute/path/titanium-1.21-4.0.45.jar \
  -PreleaseTag=v0.1.0-alpha.2 \
  clean check build generatePomFileForAddonPublication \
  generateMetadataFileForAddonPublication verifyPublicationArtifacts \
  verifyReleaseCandidate
```

For a narrower exact-artifact test rerun, use:

```bash
gradle --no-daemon test \
  -PbluemapSourcePath=/absolute/path/to/BlueMap-at-7e07f4e7 \
  -PfunctionalStorageJar=/path/to/functionalstorage-1.21.1-1.5.8.jar \
  -PtitaniumJar=/path/to/titanium-1.21-4.0.45.jar
```

Report only checks actually observed. A loadable local JAR is a prototype, not
a production-deployment authorization. Publication does not deploy the add-on
to a Minecraft server.
