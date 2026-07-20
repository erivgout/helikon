# Changelog

## Unreleased

## 1.3.5 - 2026-07-20

- Caps each projected waypoint-label panel at 5% of the current GUI height,
  including its padding, after applying distance compensation and the configured
  Scale multiplier.
- Reduces Radar minimap terrain samples and per-frame draw calls from as many as
  5,776 to at most 361, with one-second refresh caching plus four-block movement
  and fifteen-degree rotation thresholds. Entity markers remain frame-smooth.

## 1.3.4 - 2026-07-20

- Gives **Domain Expansion** the same target checkboxes as KillAura:
  **Players**, **Hostiles**, **Passive**, and **Exclude friends**. Loaded living
  mobs such as Wardens can now be selected when their category is enabled.
- Extends placement hitbox protection to every loaded living entity so a
  mob-targeted enclosure never deliberately places through its target or
  nearby living entities.
- Adds deterministic checks for player, friend, hostile, and passive target
  filtering.

## 1.3.3 - 2026-07-20

- Marks Baritone's vendored Gradle wrapper script executable on Unix runners,
  allowing Helikon's root build task to invoke it after the CI prebuild.

## 1.3.2 - 2026-07-20

- Restores clean Linux CI builds by including Baritone's pinned Gradle wrapper
  runtime alongside its already-vendored wrapper script and properties.

## 1.3.1 - 2026-07-20

- Fixes clean-checkout CI builds by compiling the embedded Baritone component
  with its pinned wrapper before Fabric Loom resolves Helikon's client runtime.

## 1.3.0 - 2026-07-20

- Adds **Domain Expansion**, a client-side Combat module that constructs one
  bounded rectangular arena around the local player and a nearby enemy using
  ordinary server-authoritative block placements. It includes manual and
  proximity activation, friend/game-mode filtering, padded dimension limits,
  escape-aware wall ordering, roof/floor plans, terrain reuse, placement
  confirmation and bounded retries, allowed-block inventory policy, reversible
  slot/rotation ownership, early target recalculation, optional self exits,
  themed plan rendering, and a completion HUD.
- Adds deterministic Domain Expansion checks for adjacent, separated, and
  diagonal players; existing terrain; missing support; insufficient resources;
  player escape and hitbox safety; friend filtering and proximity cooldown;
  manual cancellation; rejected placements; slot restoration; and dimension
  limits.
- Adds a default-on **Waypoints** Render module with Lunar-inspired projected
  name/distance panels, colored initials, and thin vertical world beams. It is
  independent from HUD Settings, keeps labels visible when their distant
  ground position is outside the camera projection, enlarges distant labels
  while keeping nearby labels compact, and does not change saved Baritone
  waypoint data.
- Upgrades Radar's optional minimap with native terrain colors, relief shading,
  compass/player heading, type colors, and entity altitude indicators.
- Adds a default-off **Hit through walls** KillAura setting. It removes the
  local line-of-sight gate only; server combat validation remains authoritative.
- Tightens BetterNametags row spacing while retaining compact whole-number
  health text and the duplicate vanilla-label suppression.
- Prevents Baritone's camera from shaking while it actively mines a block by
  disabling its legacy two-degree random yaw offset by default; users can
  still opt into the offset with Baritone's `randomLooking113` setting.

## 1.2.1 - 2026-07-19

- Adds a persistent **Baritone** section to the ClickGUI sidebar with
  permissions, visualization controls, destination and mining shortcuts,
  pause/resume/stop actions, and a field for running any Baritone command.
- Restores current, next, and in-progress route rendering on Minecraft 26.2
  and highlights blocks Baritone plans to break, place, or walk into.
- Renders composite mining goals and bounded Y-level/inverted goals so mining
  targets no longer disappear when represented as a goal set.
- Replaces Helikon's live waypoint store with Baritone's per-world,
  per-dimension collection and migrates existing `waypoints.json` entries.
- Adds tests for the Baritone sidebar, generic command action, composite-goal
  rendering, waypoint mutations/migration, and live renderable route data.

## 1.2.0 - 2026-07-19

- Vendors the latest stable Baritone 1.15.0 source and ports its 26.1
  development branch to Minecraft 26.2, Java 25, Mojang mappings, and the
  current client GUI/tick APIs.
- Embeds Baritone into the Helikon JAR with its LGPL license, provenance, and
  corresponding source; a separate Baritone installation is no longer needed.
- Adds a Baritone World module with pathing permissions, `#` command control,
  path/goal visuals, destination and mining fields, and GUI buttons for go,
  mine, pause, resume, and stop.
- Adds local `.baritone` status, goto, mine, and stop commands.
- Keeps Baritone pathing active while inventory/container and Helikon screens
  are open, and makes its worker pool shut down cleanly with Minecraft.
- Extends the Fabric client game test to execute and render a real Baritone
  goal, keep it alive with an inventory open, cancel it, and soak every module.
- Defers saved Fullbright gamma restoration/application until Minecraft options
  exist, preventing an early-startup failure while retaining the enabled state.

## 1.1.6 - 2026-07-19

- Persists the ClickGUI's selected category or special section, search query,
  selected module, module-list scroll, and settings scroll so reopening returns
  to the same working position.
- Keeps active utilities ticking in live-world container and Helikon screens
  instead of treating every open GUI as paused gameplay. InventoryWalk now
  supports crafting, furnace, chest, player-inventory, and Helikon screens while
  still suppressing movement when a text-capable widget has focus.
- Adds the opt-in Creative/Spectator Detector, which disables every other active
  module when a nearby creative or spectator player is observed and excludes
  locally saved friends by default.
- Fixes Speed Multiplier mode compounding already-boosted velocity every tick,
  which made different multiplier values rapidly collapse onto the same
  `maximum_speed` cap. It now scales ordinary movement speed to a stable target.
- Raises Speed's configurable `maximum_speed` ceiling from 3 to 100 blocks per
  tick and the multiplier ceiling from 10× to 1000× so that cap is reachable,
  while retaining the existing 0.90 and 3× defaults.

## 1.1.5 - 2026-07-18

- Makes StorageESP rebuild from loaded block entities every tick, so nearby
  storage appears and disappears immediately instead of waiting on a cube scan.
- Treats invalid or formatted server-supplied player names as non-friends during
  lookups, preventing WTap and other friend-aware features from crashing.
- Raises Speed's configurable multiplier ceiling from 3× to 10× and its
  horizontal velocity-cap ceiling from 0.90 to 3.0 blocks per tick.
- Stops Scaffold from locking the selected hotbar slot: automatic block
  selection now borrows a slot only for placement and immediately restores it.
- Makes Scaffold place automatically while enabled instead of requiring Use to
  be held; its existing bounded placement delay still controls interaction rate.
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
