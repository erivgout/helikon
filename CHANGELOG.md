# Changelog

## Unreleased

- Fixes Scaffold remaining idle while Use is held by relying on its own bounded
  placement delay and clicking the actual support-block face.
- Fixes Air Place remaining idle while Use is held by relying on its own bounded
  placement delay instead of Minecraft's continually refreshed right-click cooldown.
- Wraps the ClickGUI's empty settings-panel instruction within the panel instead
  of clipping the single line at narrow window widths.
- Locks Better Crosshair to the exact viewport center, ignoring old saved drag
  offsets and generic HUD alignment offsets while keeping its style settings.

## 1.1.4 - 2026-07-18

- Fixes Speed's default Multiplier mode so held movement input can redirect
  horizontal velocity in midair, including while Jetpack supplies vertical thrust.
- Expands AutoFarm to handle nether wart, sweet berries, cocoa, sugar cane,
  bamboo, cactus, melons, and pumpkins with plant-specific harvest behavior.

## 1.1.3 - 2026-07-18

- Replaces Gojo's Infinity local movement constraint with a strictly client-side
  living-threat repel system using legal range checks, silent server rotation,
  optional sprint reset, attack charge scheduling, and ordinary vanilla attacks.
- Raises Enderman Aura's preferred sideways escape from 6 to 12 blocks and
  checks 9- and 6-block fallbacks on both sides when terrain blocks the full jump.
- Makes Enderman Aura use swept gravity-aware arrow prediction and consistently
  derive its sideways direction from the earliest incoming projectile.
- Fixes Freecam horizontal look and camera shake by synchronizing the detached
  ArmorStand camera's head, body, entity, and previous-frame rotations.
- Lets Air Jump repeat at a bounded interval while Space remains held and uses
  Minecraft's normal jump action for each accepted airborne jump.
- Prevents Zoom from crashing a render frame when Minecraft temporarily
  reports a zero FOV while initializing or changing camera state.
- Adds Air Jump, allowing one bounded airborne jump per fresh Jump-key press.
- Makes TP-Aura stay beside and advance around targets by default instead of
  teleporting away after a hit; returning to the origin is now optional.
- Fixes Nuker's All Blocks runtime behavior by measuring radius from the
  player's feet and skipping unbreakable targets; held Attack remains the
  default and can be made optional.
- Prevents Freecam mouse shake by synchronizing the detached camera's current
  and prior look rotations after each mouse turn.
- Clips ClickGUI's empty settings message to the settings panel.
- Resets fall state immediately before TpClick relocation when Cancel Velocity
  or NoFall applies, covering downward teleports after NoFall's tick check.
- Raises Timer's configurable maximum to 5x.
- Makes FastBreak accelerate active block-damage progress instead of changing
  only the short delay between completed blocks.
- Adds the missing bounded Regen module requested by issue #155.
- Makes the Active Modules HUD enable and size controls explicit while retaining
  its automatic empty-list hiding.
- Makes both ClickGUI scrollbars draggable, with track-click jumping and a
  wider mouse hit area.
- Lets Anime Aura target players, hostile mobs, and passive mobs independently,
  and prevents it from advancing attack stages without the shared attack slot.
- Lets TP-Aura directly attack nearby targets and enables passive-mob targeting
  by default.
- Prevents Announcer from failing when commands or mods raise health above 20.
- Makes Enderman Aura escape perpendicular to an incoming projectile's flight
  line instead of choosing a backward destination that the projectile may
  continue through.
- Applies Zoom's configured FOV directly in Minecraft 26.2's per-frame camera
  projection instead of indirectly rewriting the saved video option each tick.
- Reports a module failure in chat and the log only once while that module
  remains disabled; explicitly re-enabling it starts a new failure episode.
- Adds Timer's optional `digging_only` mode, which leaves general client time
  at 1x and applies values above 1x only to an active held block break.
- Makes TooManyHax retain the most recently enabled module in a conflict group,
  so enabling TP-Aura replaces an older KillAura instead of immediately
  disabling TP-Aura again.

## 1.1.2

- Keeps KillAura from moving the local camera or player head by leaving
  rotation untouched while requesting ordinary entity attacks.
- Keeps selected ClickGUI category labels, module names, favorite stars, and
  toggle boxes legible by drawing them in a neutral color chosen for contrast
  with the animated selection background.
- Extends Reach's shared maximum to 9 blocks and applies it to the local block
  ray used by ordinary mining and placement as well as melee requests.
- Adds an optional BetterCrosshair frame setting, defaulting off, so the
  crosshair arms can render without the generic square HUD border.

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
