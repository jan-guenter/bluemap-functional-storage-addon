# Notices

Copyright (c) 2026 Jan Guenter. Licensed under MIT.

This project reuses and adapts small implementation patterns from these
owner-controlled MIT projects:

- `bluemap-sophisticated-addon`: conservative block-material resolution,
  resource-model mesh emission, atomic stock fallback, and bounded exact-mod
  artifact identification;
- `bluemap-mekanism-addon`: installed-artifact JSON child-model parsing;
- `bluemap-glassential-addon`: reflection-only registered-default-state
  resolution at the Minecraft runtime boundary.

The adapted files retain SPDX identifiers and their modification purpose is
recorded in `provenance/upstreams.json`. The add-on compiles the four MIT
helpers from the exact `bluemap-addon-adapter-api` source pin into its own JAR;
the standalone module JAR is neither nested nor installed. The renderer also
adapts MIT-licensed resource-emission and UV-lock conventions from BlueMap's
5.23 feature backport. BlueMap's complete
MIT notice is retained in `LICENSE-BlueMap` and packaged as
`META-INF/LICENSE-BlueMap`. The adapter API license is packaged as
`META-INF/LICENSE-bluemap-addon-adapter-api`.

Functional Storage and Titanium are runtime evidence/resources only and are
not copied or redistributed. No LGPL FramedBlocks code or Functional Storage
source is used.
