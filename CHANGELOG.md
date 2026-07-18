# Changelog

## Unreleased

## 1.1.1

- Fixes the Camera Distance mixin target for Minecraft 26.2 so the client can
  finish startup instead of crashing while transforming the camera class.

## 1.1.0

- Adds an **All blocks** checkbox to Nuker that targets every non-air block
  regardless of the whitelist (the blacklist still applies); the whitelist row
  is hidden while it is on.
- Redesigns Better Nametags from one concatenated line into stacked,
  color-coded rows: name (teal for friends), health colored by remaining
  fraction, armor, then distance and held item. Zero armor and an empty hand
  are hidden, and vanilla item IDs drop the `minecraft:` prefix.
- Splits the HUD editor into a drag-only layout canvas and a separate HUD
  settings screen. Every enabled element preview is now draggable directly,
  and the settings screen (reached from the editor's header button) owns all
  Active Modules and per-element presentation options, so previews no longer
  intercept clicks meant for setting rows.
- Adds a slider above every whole-number and decimal ClickGUI setting. Drag or
  click the track to set a value, or point at it and use the mouse wheel to
  increase or decrease one step. The paired text field still accepts exact
  entry, and integral sliders round while decimal sliders snap to a clean
  range-relative grid.

## 1.0.1

- Adds Jesus water-surface walking and Spider wall climbing as independent
  Movement modules. Jesus now holds the exact fluid surface without bobbing,
  while Jump and Sneak retain their expected escape and diving behavior.
- Corrects left/right movement in Flight, Boat Flight, Speed, and Freecam, and
  updates Speed's default multiplier and maximum to 3×.
- Adds KillAura multi-target mode with a configurable target cap, and fixes
  Nuker so a blank whitelist targets all non-air blocks and Survival mining
  continues the same block instead of repeatedly restarting.
- Adds an optional cached terrain minimap beneath Radar entity markers.
- Makes long ClickGUI settings panels scrollable and adds an Active category
  for quickly reviewing and disabling enabled utilities.
- Adds per-line animated rainbow colors to the Active Modules HUD and clarifies
  its editor control.

## 1.0.0

- Fixes XRay rendering under Fabric Indigo, keeps configured ores visible and
  full-bright, and reliably restores normal chunk geometry when disabled.
- Splits Boat Flight and Freecam into independently toggleable movement
  modules; regular Flight now controls only player flight.
- Fixes NoFall for ordinary Survival falls and adds an in-engine 30-block drop
  regression test.
- Removes MiniPlayer's duplicated panel background and outline so only the
  player model is rendered.
- Adds expanded Fabric client game tests for XRay, NoFall, MiniPlayer, and the
  complete registered-module smoke suite.

## 1.0.0-rc.1

- Adds the four planned EntityESP modes. Existing profiles without a saved
  `mode` now default to Outline (an unfilled wireframe); choose Box to restore
  the previous stroke-and-fill look. Glow and Shader reuse Minecraft's native
  entity-outline pass through a local, reversible target snapshot.
- Adds optional ChatSpammer counter and timestamp message suffixes.

- Completes the planned client-only module set, local configuration stores,
  ClickGUI/HUD foundations, and release-candidate checks.
- Adds deterministic release packaging with dependency reporting and SHA-256
  checksums.
- Adds source-style and client-only/no-external-network architecture checks to
  the normal Gradle `check` lifecycle and CI.
- Documents the performance, crash-isolation, migration, security, and manual
  release validation required before a final 1.0 tag.

This is a release candidate. Run the documented manual smoke checks before
publishing a final release and use only on servers whose rules permit client
modifications.
