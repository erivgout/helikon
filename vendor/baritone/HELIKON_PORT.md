# Helikon Baritone Port

This directory contains the source for the Baritone component embedded in
Helikon distributions.

## Provenance

- Upstream project: https://github.com/cabaletta/baritone
- Latest stable baseline when this port was started: `v1.15.0`
- Stable baseline commit: `612a8a6dc31edd13fa35123aa422e0cdca5c3389`
- Upstream Minecraft 26.1.2 port scaffold: pull request `#4990`
- Scaffold commit imported here: `aa96d1e938fc98bc216e51d30dfb4457acff1d72`
- Helikon target: Minecraft `26.2`, Fabric Loader `0.19.3`, Java `25`

The 26.1.2 scaffold contains upstream changes made after `v1.15.0` as well as
the mapping and platform port. Helikon carries the small 26.2 delta directly
in this directory so the complete corresponding source remains available and
reviewable beside the distributed binary.

## Local behavior changes

- `randomLooking113` defaults to `0` instead of the upstream `2` degrees.
  The upstream value applies a fresh random yaw offset every tick and visibly
  shakes the camera when block interaction requires client-side aim. The
  setting remains configurable for users who intentionally want randomized
  rotation.

## License boundary

Baritone is licensed separately under LGPL-3.0-or-later. Its original
`LICENSE` and `LICENSE-Part-2.jpg` are retained in this directory. Helikon's
MIT license does not replace or narrow Baritone's license.

The build produces Baritone as a distinct Fabric JAR and embeds that JAR in the
Helikon Fabric JAR. This preserves a clear component boundary and permits users
to inspect, modify, rebuild, or replace the Baritone component.

## Building

From this directory:

```powershell
.\gradlew.bat :fabric:build
```

The Helikon root build invokes this automatically and embeds the remapped
Fabric JAR. Only the Fabric target is enabled for the Helikon port.
