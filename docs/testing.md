# Testing

Run the test suite with:

```powershell
.\gradlew.bat test
```

Test tiers, honestly stated: the automated tier is JUnit unit tests over the
Minecraft-free decision logic plus the Gradle `verifySourceStyle` and
`verifyClientOnlyArchitecture` checks; Minecraft API shapes are verified with
`javap` against the mapped 26.2 jars (see `version-porting.md`).

A Fabric client-gametest tier runs a real 26.2 client in-engine:

```powershell
.\gradlew.bat runClientGameTest
```

It creates a singleplayer world, enables every registered module at once,
soaks them through real tick/render traffic, cycles EntityESP through all
four modes, verifies no module was auto-disabled by the failure handler,
captures screenshots under `build/run/clientGameTest/screenshots/`, and
verifies a clean disable and world close. The gametest mod
(`helikon_gametest`, `src/gametest/`) is a separate dev-only mod and is never
included in release JARs. CI runs it under a virtual display.

No general static-analysis tool beyond the two custom verify tasks is
configured. The gametest tier proves crash-free operation, not visual
correctness: the manual live-client smoke checklists below remain required
before a final release. A checklist item in this document describes how to
verify a feature — it is not a record that the check has been performed.

The automated tests cover module lifecycle behavior, failure isolation,
setting validation/JSON recovery, configuration round-tripping (including
keybinds), malformed JSON recovery, ClickGUI search/filtering
(`ClickGuiStateTest`), number-field edit rules (`NumberSettingTextTest`),
command parsing and every built-in command (`CommandDispatcherTest`,
`BuiltinCommandsTest`), keybind edge/hold/suppression behavior
(`KeybindManagerTest`), and Active Modules selection, drag clamping, and HUD
configuration recovery (`ActiveModulesTest`, `HudEditorStateTest`,
`HudConfigurationManagerTest`). `ClickGuiStateTest` also covers keyboard
category/module wrapping and empty-search selection handling. Profile save,
load, atomic backup, malformed-file recovery, safe profile names, and local
command wiring are covered by `ProfileManagerTest` and `ProfileCommandTest`.
`EventBusTest` covers typed dispatch, subscription removal, listener-failure
isolation, event-catalog coverage, and malformed event-payload rejection.
`ClientEventAccessTest` covers verified keyboard, mouse, scroll, chunk,
resource-reload, render, interaction, and packet-metadata bridge normalization
without requiring a running game client. Run the live-client smoke checklist
before release to verify the version-sensitive mixin boundaries remain active.
`SeedSlimeMathTest` compares SeedCracker's exact Minecraft 26.2 slime predicate
with a `java.util.Random` reference across positive, negative, and
overflow-heavy coordinates and verifies the 48-bit structure-seed boundary.
`SeedCrackerTest` covers defaults, lifecycle, distinct-entity confirmation,
bounded incremental search, exact integrated-world results, malformed ranges,
manual correction, disable/re-enable behavior, and world-session cleanup.
`SeedCrackerCommandTest` covers every local evidence/search command. The
Fabric client GameTest additionally enables SeedCracker in an integrated
world, verifies its exact local seed, submits loaded-chunk evidence through
the real HUD/world render path, and captures `helikon-seed-cracker-solved`.
It also injects an unnamed HOME entry into Baritone's live collection, renders
the Waypoints HUD for multiple frames, and captures
`helikon-unnamed-home-waypoint`; repository tests cover blank, unsupported,
overlong, and structurally invalid external waypoint data.
`PlayerStateEventTrackerTest` covers lifecycle, movement, rotation, inventory
revision, and world-absence baseline transitions without Minecraft classes.
`SaturationHudTest` covers finite saturation formatting and invalid-fact
masking; its in-client visibility and panic behavior are covered by the HUD
smoke checklist.
`BetterNametagTextTest` covers deterministic configured fact composition; use
the world-render smoke checklist to verify local frustum/range behavior.
Fullbright's gamma restoration, setting-driven Night Vision lifecycle, and
identity-safe effect restoration are covered by `FullbrightGammaControllerTest`,
`FullbrightTest`, and `ClientEffectOverrideStateTest`.
AntiBlind, NoFireOverlay, and AntiTotemAnimation enable-state gating, BetterCrosshair geometry,
and strict ARGB color settings are covered by `AntiBlindTest`, `NoFireOverlayTest`,
`AntiTotemAnimationTest`, `BetterCrosshairTest`,
`CrosshairGeometryTest`, and `SettingTest`.
Dinnerbone's selected-category enable-state policy and RainbowEnchant's
configured/rainbow color policy are covered by `DinnerboneTest`,
`RainbowEnchantTest`, and `GlintColorTest`; their version-specific render hooks
need the visual smoke test below.
`SettingTest` also covers integral and range bounds, keybind JSON recovery,
immutable bounded string/identifier lists, item/block/entity selector tokens,
stable multi-select enum JSON, safe-regex rejection, visibility predicates,
and validated change listeners. `SettingTextTest` covers the compact ClickGUI
syntax for integer, list/selector, multi-enum, range, regex, and keybind
settings, including rejection that retains the last valid value.
`ColorPickerValueTest` covers ARGB channel replacement and picker endpoint
mapping without Minecraft UI dependencies.
AutoSprint's hunger, collision, direction, and reversible ownership decisions,
plus AutoWalk's GUI and steering input policy, are covered by
`AutoSprintTest` and `AutoWalkTest`.
AutoSneak's Toggle, bound-key Hold, Edge-only, and screen-suppression policies,
plus enum-setting recovery and command validation, are covered by
`AutoSneakTest`, `SettingTest`, and `BuiltinCommandsTest`.
Twerk's local pulse interval, physical-sneak preservation, and screen/disable
reset rules are covered by `TwerkTest`. Annoy's enabled/player/screen gates and
minimum swing interval are covered by `AnnoyTest`. SkinBlinker's local-layer
alternation, ownership-aware restoration, and world-exit cleanup are covered
by `SkinBlinkerTest`; `OneClickFriendsTest` covers its explicit module gate.
`InventoryPreviewLayoutTest` covers bounded storage/hotbar selection and grid
geometry; `DurabilityWarningsTest` covers the inclusive local threshold; and
`CoordinateTrackerTest` covers enabled-only session death/logout snapshots.
`LocalCapeTexturePatternTest` covers the bounded opaque procedural cape
pattern and ARGB-to-ABGR conversion, while `LocalAuraGeometryTest` covers its
closed bounded ring and malformed-fact rejection. Their render integrations
require the Phase K smoke checks below.
`ModuleTimingMetricsTest` covers disabled collection, tick/scan/render
classification, stable zero rows, invalid duration rejection, and the guarded
registry bridge. `DebugOverlayTest` covers opt-in timing lifecycle, while
`DebugOverlayLinesTest` covers local paging/formatting and all required
diagnostic rows; `ConfigurationManagerTest` covers in-memory global-save
status. Their HUD integration requires the diagnostics smoke check below.
AutoParkour's safe shallow-ledge gate and malformed-fact rejection,
InventoryWalk's inventory/text-focus and Shift-preservation policy, and
AntiAFK's interval, local-activity reset, and grounded-jump policy are covered
by `AutoParkourTest`, `InventoryWalkTest`, and `AntiAfkTest`. Their client input
bridges additionally require the movement smoke checklist below.
AutoTool's correct-tool scoring, durability guard, ownership-aware slot restore,
and safe no-selection behavior are covered by `AutoToolTest`.
FastPlace's held-use gate, item filtering, safe delay floor, and invalid input
rejection are covered by `FastPlaceTest`.
FastBreak's held-target/filter/cooldown restoration rules and Nuker's explicit
whitelist, range, visibility, action cap, temporary tool ownership, and local
rotation are covered by `FastBreakTest` and `NukerTest`. Embedded Baritone's
command bridge is covered by `BaritoneCommandTest`; the Fabric client game test
runs a real goal, opens the player inventory while it is active, captures its
path rendering, cancels it, and checks for guarded module failures.
AntiCactus's disabled default, non-collision preservation, vertical-movement
preservation, and deterministic safe-axis slide are covered by
`AntiCactusTest`. BlockSelection's bounded local distance-label and render-style
decisions are covered by `BlockSelectionTest`; their 26.2 mixin/Gizmo wiring
also requires the smoke check below.
AutoEat's deterministic food choice, avoid list, local combat/manual-use
interruption, owned Use-key cleanup, and slot safety are covered by
`AutoEatTest`, including always-edible foods at full hunger and physical-key
handoff. Bounded text-setting validation and local command support are
covered by `SettingTest` and `BuiltinCommandsTest`.
ChatPrefix/ChatSuffix composition, per-server entries, deterministic random
selection, and protected-command preservation are covered by
`OutgoingChatFormatterTest`, including the vanilla 256-character send limit.
Incoming category recognition, duplicate hiding, custom text rules, player
rules, and safe-regex rejection are covered by `IncomingChatPolicyTest`.
ChatSpammer's command rejection, sequential/random selection, minimum-delay
countdown (including toggles), screen pause, session cap, cancellation stop,
and disconnect stop are covered by
`ChatSpammerTest`.
ChatHistory's opt-in per-server persistence, atomic backup, corrupt-file
recovery, retention pruning, safe scope validation, local command search,
clipboard selection, and unsent-draft reopening decisions are covered by
`ChatHistoryManagerTest` and `ChatHistoryCommandTest`.
Announcer's per-trigger default-off policy, safe template/cooldown/screen
gates, and local distance/health/dimension threshold observations are covered
by `AnnouncerTest` and `AnnouncerObservationTrackerTest`. Its Minecraft hook
adapters require the smoke check below.
LocalTranslator's bounded local-glossary parsing and disabled, overlay, and
oversized-input guards are covered by `LocalGlossaryTest` and
`LocalTranslatorTest`.
Entity category/friend/range gating, local block-ID parsing, incremental cube
scan order, cache eviction, and Breadcrumb sampling/age bounds are covered by
`EntityRenderFilterTest`, `BlockEspPolicyTest`, and `BreadcrumbTrailTest`.
Trajectory drag/gravity ordering, collision stopping, radar projection/clipping,
and ARGB transparency are covered by `TrajectorySimulatorTest`,
`RadarProjectionTest`, and `RenderColorTest`.
XRay target filtering, opacity validation, reversible renderer-invalidation
decisions, and StorageESP target-family/enabled-state decisions are covered by
`XRayRenderStateTest`, `XRayTest`, `StorageEspTargetsTest`, and
`StorageEspTest`.
MiniPlayer's fixed geometry and DamageIndicators' confirmed health-loss,
bounded-label, fade, and rise decisions are covered by `MiniPlayerLayoutTest`
and `DamageIndicatorTrackerTest`.
Container click plans, armor/totem/eject/manager safeguards, and ChestSteal's
filter/priority decisions are covered by `ContainerClickSequenceTest`,
`InventoryAutomationPolicyTest`, and `ChestStealTest`. AutoFish's cast/bite/
recast state and durability reserve are covered by `AutoFishTest`; AutoReconnect's
countdown, cancellation, visible-screen guard, and bounded retries are covered
by `AutoReconnectTest`. Builder plan bounds/geometry and Use/repeat/delay gates
are covered by `BuilderPlanTest` and `BuilderAssistTest`.
Advanced Movement's NoSlow gates, bounded ladder/step/velocity rules,
ability-only flight/no-fall behavior, Elytra pitch/status, scaffold target and
hotbar selection, and safe timer multiplier are covered by
`AdvancedMovementPolicyTest`. Mixin/HUD/freecam wiring is additionally covered
by the live-client checklist below because it relies on verified 26.2 client
hooks.

`AntiWaterPushTest` covers its disabled default and reversible water-current
policy. `GlideTest` covers its bounded descent, opt-out states, setting change,
and invalid velocity rejection. `FishTest` covers bounded underwater
horizontal/vertical velocity, idle drift, and every exclusion state.
`WaterJumpTest` independently covers its disabled default and every required
Minecraft-free water-edge fact. Its 26.2 input adapter is covered by the
advanced-movement smoke check below.
Combat filtering, deterministic selection, bounded bow smoothing, legitimate
critical gates, restorative-potion slot ownership, anti-bot heuristics, and
session HUD tracking are covered by `CombatPolicyTest`. `AutoLeaveTest` covers
its disabled default, independent danger triggers, and invalid local facts. Minecraft combat and
HUD wiring additionally have the manual checklist below because they depend on
verified 26.2 local game APIs.
Configuration migration coverage verifies that legacy Fullbright state loads
under the production module ID and that the next atomic save removes the legacy
key (`ConfigurationManagerTest`).

## Manual HUD editor smoke test

1. Run `./gradlew.bat runClient`, join a world, and send
   `.toggle fullbright`. Verify **Fullbright** appears in a small
   top-left HUD panel; send the command again and verify the panel disappears.
2. Open the ClickGUI with Right Shift and click **HUD** in its header. Verify a
   dimmed drag-only editor opens with a header bar, a **HUD settings** button,
   the Active Modules preview (showing either enabled modules or
   `No modules enabled`), and one labelled handle per enabled HUD element.
3. Drag the Active Modules preview to each screen edge and verify it stays
   fully visible. Drag close to, but not exactly on, an edge and then the
   viewport centre; verify the preview snaps there. Drag several element
   handles to distinct positions and verify each moves independently, stays
   fully on screen, and gains the accent outline when clicked.
4. Click **HUD settings** and verify the settings screen opens over the same
   previews. Toggle the Active modules checkbox off and verify the normal HUD
   no longer renders after leaving the screens; turn it back on. Cycle Sort,
   Alignment, and Color, toggle Background, Text shadow, and Animation, and
   adjust Scale/Padding; verify the live previews reflect every choice and
   remain entirely on screen. Press Escape and verify the editor returns;
   press Escape again and verify the ClickGUI returns. Relaunch the client
   and verify the enabled state, positions, and presentation choices are
   restored from `config/helikon/hud.json`.
   In HUD settings, cycle the **HUD element** selector through Waypoints,
   Coordinates, Saturation, Elytra, Target HUD, Reach, Inventory Preview,
   Durability warnings, Radar, MiniPlayer, Debug Overlay, and Better
   Crosshair. Toggle one off and verify its editor handle renders grey only
   while it is the selected element. Drag another handle to a distinct
   position, close/reopen the editor, and verify both local settings persist
   and affect only that HUD element. For each selected element, cycle
   Alignment and Color, toggle Background, Text shadow, and Rainbow, and
   adjust Scale/Padding; verify the styled preview changes and its rendered HUD
   remains fully on screen. Drag Radar, MiniPlayer,
   Inventory Preview, and Debug Overlay to each viewport edge and verify their
   actual rendered content remains fully on screen. For Better Crosshair,
   verify its placement changes the arm centre and toggling it restores the
   vanilla crosshair when its module is otherwise enabled.
   Continue through Direction, FPS, Ping, TPS estimate, Speed, Armor
   durability, Held-item durability, Potion effects, Clock, Biome, Server
   address, and Totem count. Enable a few, verify they render only local facts
   at their saved placements, then disable them again; TPS must be labelled
   local and must not be presented as server information.
5. Replace `hud.json` with invalid JSON, relaunch, and verify Helikon creates
   `hud.corrupt-<timestamp>.json` and returns the element to its default
   position.

## Manual Saturation Display smoke test

1. Run `./gradlew.bat runClient`, join a local/test world, and enable
   **Saturation Display**. Verify a `Saturation <value>` readout appears above
   the bottom-left edge at both the normal GUI scale and a scaled height of 240
   or less; it must remain fully on screen.
2. Eat ordinary food in the test world and verify the displayed local value
   changes after Minecraft updates hunger. Confirm that it does not alter food,
   saturation, packets, or server-visible state.
3. Run `.panic` and verify the readout hides with the other custom HUD; run
   `.panic restorehud` and verify it returns while the module remains enabled.

## Manual AntiTotemAnimation smoke test

1. Run `./gradlew.bat runClient`, join a local/test world, and arrange a safe
   totem-pop test. With **AntiTotemAnimation** disabled, verify Minecraft's
   normal centered death-protection item activation overlay appears.
2. Enable **AntiTotemAnimation** and repeat the same test. Verify only the
   centered item activation overlay is absent: the normal totem protection,
   particles, sound, effects, inventory change, and server-visible outcome
   still occur.
3. Disable the module and repeat once more. Verify Minecraft's normal item
   activation overlay returns immediately without reconnecting.

## Manual Phase C movement-control smoke test

1. In a local/test world, enable **AutoParkour**, walk forward at normal speed
   toward a shallow one- or two-block loaded drop with clear local space and a
   solid landing, and verify it requests only Minecraft's ordinary jump. Test
   a lava landing, a three-block-or-greater drop, an obstructed landing, a
   screen-open state, and low forward speed; none may request a jump. Verify
   no chunk loads, interaction, or packet-specific behavior is claimed.
2. Open the vanilla player inventory and enable **InventoryWalk**. Hold the
   configured keyboard movement and jump bindings to verify local movement,
   then hold Shift while quick-moving an item to verify it is not converted to
   sneak. Focus the recipe-book search or another inventory widget and verify
   movement pauses while typing. Open chat, the ClickGUI, a chest, or a screen
   with a mouse/scancode movement bind and verify InventoryWalk stays inactive.
3. Enable **AntiAFK** in a local/test world with a short permitted interval and
   one action setting at a time. Verify a turn is at most 15 degrees, Jump is
   requested only while grounded, and Short movement lasts one input tick.
   Move manually or open a screen before the interval elapses and verify the
   timer restarts. Disable it and verify no further local action occurs. In a
   permitted multiplayer check, confirm these are ordinary client inputs only;
   server movement rules remain authoritative.

## Manual Dinnerbone and RainbowEnchant smoke test

1. Run `./gradlew.bat runClient` and join a local/test world with a player, a
   hostile `Monster`, and a passive living entity in view. Enable
   **Dinnerbone**. Verify its default player and hostile categories flip only
   those local models, then toggle **Passive** to flip the other living entity.
   Disable the module and verify all selected models return immediately to
   their normal presentation. A normally named Dinnerbone/Grumm entity must
   keep Minecraft's own upside-down behavior throughout.
2. Hold or open the inventory containing an enchanted item. With
   **RainbowEnchant** disabled, note the normal glint. Enable it, set a solid
   local `#AARRGGBB` color, and verify only this client's item-stack glint is
   tinted. Enable **Rainbow** and vary **Rainbow speed**; verify the tint cycles
   locally and returns to vanilla immediately when disabled. Worn armor-layer
   glint intentionally remains vanilla in this initial implementation.
3. In a permitted multiplayer check, confirm another player sees neither the
   entity transforms nor the item-glint tint, and that no interaction, item
   data, or server message changes.

## Manual ClickGUI smoke test

The screen itself renders inside Minecraft, so the following checks are
manual. Run `./gradlew.bat runClient` using Java 25, then:

1. Press Right Shift in a world and verify the ClickGUI opens; press Escape
   and verify it closes.
2. Click the **Render** category and verify `Fullbright` is listed.
3. Click the square at the right of the module row and verify it fills with
   the accent color (enabled); click again to disable.
4. Click the module name and verify the right panel shows its name, category,
   ID, description, an **Enabled** row, the **Gamma mode** and **Night Vision
   mode** checkboxes, and the
   **Brightness** number field with its range.
5. Toggle **Gamma mode** and type a new **Brightness** value. Out-of-range or
   non-numeric text must turn red and leave the stored value unchanged.
6. Type into the search box and verify matches by name or ID (`full`),
   and description text, across all categories; verify a nonsense query shows
   "No matching modules" and that clearing the query restores the category
   list.
7. While the search box is focused, verify typed letters go into the box and
   do not trigger game actions.
8. Close the GUI, quit the client, and verify `config/helikon/global.json`
   contains the edited values; relaunch and verify the GUI shows them again.
9. In the selected module panel, change **Gamma mode** and **Brightness**,
   click the appropriate small **R** controls, and verify each returns only
   that setting to its default. Change both again, click **Reset module**, and
   verify both defaults are restored.
10. Click **Bind**, press `R`, close the GUI, and verify R toggles the module.
    Reopen it and click **Bind** again: Escape must leave the R bind unchanged;
    Backspace/Delete must remove it. Verify Right Shift is rejected as the
    reserved GUI key and does not replace the module bind. Start capture after
    focusing Search or the Brightness field and verify the captured printable
    key does not enter or change that field.
    Repeat capture with Ctrl held and `R`, then with Mouse 4; after closing the
    GUI verify only the complete Ctrl+R or Mouse 4 input activates the module.
    Give a second module the same bind and verify the ClickGUI reports the
    local collision without altering either module.
    Rebind **Open GUI** to Mouse 4 in Minecraft Controls and verify both
    ClickGUI capture and `.bind ... mouse4` reject it as reserved.
11. Drag an unused portion of the header to every edge. Verify the complete
    window and its search/number fields move together and remain visible;
    close and reopen the GUI to verify the saved position is restored.
12. Drag the bottom-right handle inward and outward. Verify the top-left corner
    remains fixed, the window cannot shrink below its usable minimum or grow
    beyond the screen, and its restored size remains valid after a relaunch.
13. Select **Theme** and choose each palette. Verify the ClickGUI changes
    immediately, High Contrast remains legible, Escape returns to the ClickGUI,
    and the chosen theme is restored after relaunch. Replace the stored theme
    with an invalid value and verify Helikon safely returns to Midnight.
    Cycle the Theme editor's **GUI scale** through 0.75x, 1.0x, 1.25x, and
    1.50x; verify the resolved ClickGUI panel size changes, stays on-screen,
    and persists after relaunch. Toggle **Reduced animation** and verify the
    preference persists locally.
14. Without clicking a text field, use Left/Right to change categories and
    Up/Down to select module rows. Verify selection wraps and scrolls into
    view, then use Enter and Space to toggle the selected module. Click the
    search or a number field and verify its normal keyboard editing continues
    to take priority.
15. Enable two modules from different categories, click **Active** above the
    category list, and verify it shows only those modules. Toggle one there and
    verify it immediately disappears while the other remains. Select a module
    with enough settings to exceed the right-hand panel, then hover that panel
    and scroll: its scrollbar, rows, and editable fields must move together
    without drawing outside the panel.
15. Select **BetterCrosshair**, enable it, and verify its custom crosshair
    replaces the vanilla one. Change its size, gap, thickness, outline, and
    `#AARRGGBB` color text field; drag each alpha/red/green/blue picker track
    and verify it changes only that color channel and synchronizes the text
    value. While moving, verify the optional dynamic gap responds locally.
    Disable it and verify the vanilla crosshair returns.
16. Select **AntiBlind** and independently toggle each setting. Verify Nausea,
    carved-pumpkin, and powder-snow overlays hide only when configured; verify
    disabling the module restores vanilla visuals. Blindness and Darkness fog
    checks require the matching effect in a controlled local test world; verify
    both the fog-color darkening and fog-distance changes are absent.
17. Enable **AutoWalk** and verify W-style forward movement continues after
    releasing the physical forward key. Verify side/back steering remains
    available by default; disable **Allow steering** to verify only forward
    movement remains. Open chat or the ClickGUI and verify movement stops with
    **Stop on GUI** enabled, then disable the module and verify normal input
    resumes immediately.
18. Enable **AutoSprint** with its defaults and verify it requests normal
    sprinting only while moving forward with enough hunger and no horizontal
    collision. Verify the hunger and collision settings gate the request,
    `forward_only` permits other movement directions when off, and disabling
    the module releases only sprinting it enabled.
19. Select **AutoSneak** and cycle **Mode**. In Toggle mode, verify the local
    player sneaks while it is enabled and resumes normal movement immediately
    when disabled. Enable the module through the GUI or a local command, bind
    it, select Hold, and verify it sneaks only while that bound key is
    physically held without toggling the module. Select Edge-only and verify it
    applies local sneak while moving so vanilla careful movement stops at a
    ledge. Open chat or the ClickGUI in each mode and verify it never forces
    sneak while the screen is open.
20. Put multiple tools in the hotbar, begin manually mining a block, and enable
    **AutoTool**. Verify it selects the fastest correct tool with at least the
    configured remaining durability, then restores the previous slot when
    mining ends if that setting is enabled. Change slots manually while mining
    and verify AutoTool does not overwrite that later user selection. Verify it
    is inactive while a screen is open or the attack key is not held.
21. Enable **FastPlace** with its default Blocks filter, hold Use while placing
    ordinary blocks, and verify repeated placement only follows normal
    Minecraft interactions. Set a nonzero safe minimum delay and verify it
    remains a floor. Change the filter to Non-blocks and verify held block use
    is no longer affected. Open any screen and verify no cooldown change is
    applied while it is open.
22. Put two food items in the hotbar, lower hunger below **AutoEat**'s threshold,
    and enable it. Verify it selects the configured preferred, non-avoided food
    and uses the ordinary eating animation. Verify it pauses while attack is
    held (with the default rule), never takes over while manually using another
    item or while a screen is open, and restores its original slot only when
    the user has not changed slots. Disable it mid-eat and verify Use releases
    immediately unless the player is physically holding the configured Use
    binding, in which case that physical hold continues normally. At full
    hunger with low health, verify only a normally always-edible food is chosen.
23. Enable **ChatPrefix** and **ChatSuffix**, send an ordinary harmless chat
    message, and verify the configured prefix/suffix appears once through the
    normal server chat route. Send `.help`, `/login <test value>`, and a common
    private-message command; verify each is unchanged and the local command is
    still cancelled before any server send. Configure one local per-server
    suffix entry and a random list, then verify the matching entry wins.
24. Enable **ChatMute** with one category at a time and verify only the local
    chat HUD hides the chosen message type. Enable **ChatFilter** with a simple
    keyword, player name, and safe regex; verify matching incoming messages are
    hidden locally. Turn off Hide and turn on Highlight, Sound, and HUD
    notification; verify a matching line remains local-only, highlights, sounds,
    and posts one Helikon notice. Enter a malformed or nested-quantifier regex
    and verify it does not freeze the client or hide unrelated messages.
25. With a server or test environment where you are permitted to send chat,
    configure **ChatSpammer** with a harmless ordinary message and verify it
    sends no more frequently than its stated delay, pauses while chat or any
    GUI is open, and stops after the configured session cap or disconnect.
    Verify slash commands and Helikon local-command prefixes in its message
    list are ignored. Do not use it where server rules prohibit repetitive chat.
26. On a server where `/msg` and `/r` are permitted, send `.pm Alice hello`
    and verify only the configured normal server command is emitted (never the
    `.pm` text). Run `.pm history Alice` and verify `You: hello` is local
    feedback. Use a 16-character player name and a long message to verify an
    over-limit composed command is rejected before sending. Relaunch and
    verify history is gone while the configurable command-token settings remain
    local. Receive a supported `From Bob: hello`, `Bob whispers to you: hello`,
    or `Bob -> you: hello` line and verify it appears as an incoming history
    entry, uses only local notification/highlight/sound controls, and a standard
    player name suggests the configured normal PM command when clicked.
    in `global.json`. To message a player literally named `history`, use
    `.pm -- history hello`; the `--` escape also lets `.reply -- history`
    send that literal reply instead of opening local history.
27. Enable **MentionNotifier**, set a local term, and have another permitted
    player send it in ordinary chat. Verify one local Helikon notice appears,
    a repeated mention inside the configured cooldown is quiet, and a message
    from the local player's own name is ignored. Verify no sound, desktop, or
    taskbar notification is requested by this first slice.
28. In a permitted test environment, enable **AutoReply** with a harmless
    trigger and reply. Verify a matching chat message from an allowed player
    produces one ordinary chat reply, while a local-player message, blacklisted
    player, non-matching server, screen-open state, slash/dot reply, repeated
    sender inside cooldown, and per-minute cap do not send anything. Configure
    the reply text as the trigger and verify an echoed matching reply does not
    cause a response loop.
29. Enable **AntiSpam** and send or receive harmless repeated chat in a
    permitted test environment. Verify its configured repeat window hides only
    the local duplicate, a rapid single sender exceeds the local limit while
    other senders remain visible, and the configured whitelist keeps its
    message type visible. Enable join/leave collapsing and verify a same-type
    burst is hidden locally. The first slice intentionally does not display a
    duplicate counter yet.
30. Enable **ChatTimestamps** and receive a normal player message plus a
    server-system message. Verify the local bracketed timestamp precedes each
    line without altering the original content, then switch 12/24-hour time,
    seconds, color, and session-relative mode. Verify Helikon's own local
    command feedback is not timestamped and no timestamp is visible to another
    player or sent to the server.
31. Enable **ChatColor** and receive normal player chat, a server-system
    message, a mention of the local player, and a server format beginning with
    `From` or `To`. Verify each local fallback color, background-opacity
    multiplier applies to ordinary message-line backgrounds as well as focused
    prompt backgrounds, and text-shadow toggle changes only this client's
    display. In
    standard vanilla `<player> message` chat, verify the sender span uses
    **Player name color** while the message retains its normal color. Verify a
    custom server chat format remains legible and keeps any explicit server
    name styling. Combine it with **ChatTimestamps** and verify the timestamp
    uses ChatColor's timestamp color. Relaunch to verify settings persist, then
    disable ChatColor and verify vanilla display colors/opacity return without
    sending or changing any chat text.
32. Enable **BetterChat** and receive more than 100 harmless local/test chat
    lines. Verify its configured history limit is retained, then lower the
    setting or disable the module and verify only the current bounded local
    display remains. Send the same line twice consecutively and verify one line
    becomes `[x2]`; insert a different line between repeats and verify the
    counter restarts. Disable duplicate stacking while keeping counters enabled
    and verify repeat lines stay separate. In ordinary vanilla `<player>` chat,
    click a name and verify `/msg <name> ` is only suggested in the input, never
    sent; a server-provided click action must remain intact. Use `.chat history
    3`, `.chat search <text>`, and `.chat copy 1`, verify each is local and the
    selected newest line reaches only the local clipboard. Change visibility,
    fade, compact mode, and smooth scroll; verify unfocused chat timing,
    line-height, and multi-line scroll easing change locally. Relaunch and
    verify settings persist but no old chat lines do.
33. Enable **ChatHistory** in a local/test world and receive harmless chat,
   then send one ordinary message. Run `.history list`, `.history search`,
   `.history copy`, `.history player` on an entry with a normal player name,
   and `.history reopen` on the outgoing entry. Verify clipboard operations
   reach only the local clipboard and reopen creates an unsent chat draft.
   Send a `.` command and verify it neither reaches the server nor appears in
   history. With **Persistent logging** left off, quit and verify no
   `chat-history/` file exists. Enable it explicitly, relaunch on the same
   server, and verify bounded retained entries return; join another server and
   verify its history is separate. Replace one log with malformed JSON and
   verify a matching `.corrupt-<timestamp>.json` recovery file appears.
34. In a permitted local/test world, enable **Announcer** and enable one trigger
   at a time. Confirm no ordinary chat is sent for any unchecked trigger. Use a
   harmless non-command template, a generous cooldown/cap, and a permitted
   target to verify death, pickup, traveled distance, block mined, dimension,
   join, advancement, low-health, and totem triggers emit at most one normal
   chat line each. A direct melee kill must wait for a locally dead target
   unload; projectile or indirect kills intentionally do not claim a
   confirmation. Open a screen to verify **Pause in GUI**, try a `/` or `.`
   template to verify rejection, and disconnect to verify a local leave notice
   appears without a server-chat attempt.
35. In a permitted local/test world, enable **EntityESP** and **Tracers**.
    Verify selected nearby player/hostile categories draw local boxes or lines,
    locally saved friends use the friend color, and entities outside the range
    do not render. Check each EntityESP mode separately:
    - **Outline**: verify targets get an unfilled local wireframe only (no
      fill), with the configured line width and module/friend colors.
    - **Box**: verify the same wireframe plus the configured fill color.
    - **Glow**: verify targets gain Minecraft's genuine full-body outline
      (the same visual as the vanilla Glowing effect, including its
      post-processing halo) in the vanilla team-derived color — white when
      the entity has no team. Apply a real Glowing effect (e.g.
      `/effect give` a mob) with EntityESP disabled and verify it looks
      identical, then verify a genuinely glowing non-target entity still
      glows while EntityESP is enabled.
    - **Shader**: verify the same genuine native outline appears but uses the
      module color, and the friend color for locally saved friends.
    For Glow and Shader, verify the category/friend/range filters and the
    maximum-entity cap still choose targets; verify disabling the module,
    switching the mode back to Outline/Box, and changing worlds each remove
    every Helikon outline immediately while entities with a real server-given
    Glowing effect keep glowing. Verify no mode changes how other players see
    anything or alters interaction. Enable **BlockESP** with a harmless known block ID and
    verify its box appears after a bounded scan pass; move, change the ID list,
    or break/place a target and verify the cache eventually refreshes without
    a frame hitch. Enable **Breadcrumbs**, walk a short path, and verify a
    local line follows sampled positions, respects its point/age limits, and
    clears after disable or a world change. Verify none of these changes are
    visible to another player or affect normal interaction.
    Enable **Better Nametags** and verify each health, distance, armor, held
    item, and friend-status toggle adds or removes only its corresponding
    stacked local row, with health colored by its remaining fraction and no
    row for zero armor or an empty hand. Verify friend color/status disappears
    when friend status is off, players outside range or the camera frustum do
    not render, and invisible or solid-block-occluded players receive no
    name-tag billboard.
36. In a permitted local/test world, enable **Trajectories** while arrows,
    tridents, snowballs, eggs, ender pearls, or splash potions are in flight.
    Verify each configured type draws a local path that ends at its first block
    impact marker, and that disabling it immediately removes the preview.
    Use an invisible entity in a controlled world to verify **TrueSight** draws
    only its configured translucent local box, then toggle player/hostile/
    passive filters and transparency. Verify a friendly-team entity that is
    visible to the local player does not gain a box. Turn away from a candidate
    to verify its trajectory/TrueSight overlay is frustum-culled. Enable
    **Radar**, verify circle/square, toggle the terrain minimap on and off, then
    confirm the cached minimap no longer causes a major FPS drop while moving
    and turning while retaining one-pixel terrain detail. Verify terrain
    refreshes within one second, along with
    rotation, zoom, local friend color, and category filters. Confirm no
    unloaded terrain or out-of-range entities appear. None of these results should be
    visible to another player or change a normal projectile/entity interaction.
37. In a disposable local/test world, enable **XRay** and verify only its
    configured locally loaded block models remain visible after the geometry
    rebuild; change the block list and opacity, wait for the local rebuild, and
    verify the display follows it. Disable XRay and verify normal world geometry
    fully returns. Enable **StorageESP** near known selected storage, then
    verify its box appears on the next tick, disappears on the next tick after
    the storage is removed, is culled when offscreen, honors category/custom-ID
    settings, and never opens or changes a container.
    For **BlockESP**, set two configured IDs to distinct `block_colors` entries
    and verify each retained local box/tracer uses its own color; malformed
    entries must safely retain the shared fallback color.
38. In a local/test world, enable **MiniPlayer** and verify the local player
    model appears in its fixed HUD panel, responds to rotation/scale/background
    settings, and armor on/off changes only the panel. Enable **DamageIndicators**
    near an eligible mob/player, observe a normal local health loss, and verify
    one amount rises and fades with the configured duration/color. Verify a
    target outside range or behind the camera has no label, no damage changes
    occur without a local health decrease, and neither feature affects combat.
39. In a disposable local/test world, open the player's normal inventory with
    an empty cursor. Enable **AutoArmor**, confirm it equips only a strictly
    better piece after its delay, then equip a Binding Curse piece and verify
    protection leaves it in place. Configure **AutoEject** with a harmless
    test item and protected hotbar range; verify only an unprotected matching
    item is normally dropped. At a safe low-health/fall test threshold, verify
    **AutoTotem** moves an existing inventory totem into offhand and restores
    the recorded prior item only when its source remains unchanged. Enable
    **InventoryManager** with a harmless preferred hotbar entry and junk item;
    verify named, enchanted, and protected/durability-reserved items remain
    untouched and close the screen to verify every module stops immediately.
40. In a disposable chest containing harmless items, enable **ChestSteal** and
    verify one normal quick move occurs per configured delay, whitelist and
    blacklist filters leave the correct items, priority changes the transfer
    order, and close-after-completion closes only after eligible items are
    exhausted. Keep a nonempty cursor or leave the chest screen and verify no
    action occurs.
41. In a permitted fishing test world, select a player-provided rod and enable
    **AutoFish**. Verify it casts once, waits for a visible bite and the reel
    delay, then reels and waits for the recast delay before casting again.
    Turn on open-water-only near non-open water and verify it does not reel;
    set the durability reserve above the remaining rod durability and verify it
    stops. Open a screen and verify it does nothing.
42. On a disposable multiplayer target, disconnect unexpectedly with
    **AutoReconnect** enabled. Verify the local countdown and Cancel button,
    cancel once to verify no connection is made, then retry and verify no more
    than the configured attempts use Minecraft's normal connect screen. Leave
    explicitly or use a local world and verify it never reconnects.
43. In a local/test world with ordinary blocks, enable **BuilderAssist**, hold
    Use with a player-provided block, and target a replaceable face. Verify the
    bounded local preview follows each single/line/floor/wall mode and length,
    width, height, and color settings. Verify repeat placement honors its delay
    and stops when Use is released, the held item is not a block, a screen is
    open, a target is unloaded/occupied, or normal vanilla placement fails.
44. In a disposable local/test world, enable each Advanced Movement module
    separately. With **NoSlow**, verify each enabled food/block/bow/sneak/
    soul-sand/honey/cobweb category changes only local responsiveness and no
    other entity is affected. Enable **AntiWaterPush** in a flowing-water test
    area and verify only the local water-current velocity is suppressed; water
    contact, swimming, splash behavior, bubble columns, lava currents, and
    disabled behavior remain vanilla. Verify **FastLadders** changes only normal
    climbable movement. Enable **WaterJump** in shallow water and move forward
    into a loaded sturdy one-block bank with two clear blocks above it: verify
    it requests only normal Jump input. Test backward movement, air, an
    obstructed bank, an unloaded boundary, and every open screen; none may
    request a jump. In a submerged test area, enable **Fish** and verify only
    directional/Jump/Sneak input applies the configured bounded local swimming
    velocity; idle drift, screens, riding, ability flight, and Elytra flight
    remain unchanged. Verify **Jesus** holds a steady, non-bobbing water surface,
    Jump releases upward, and Sneak permits normal diving. Walk into a wall with
    **Spider** and verify forward/side
    movement climbs while Sneak, ladders, and open screens do not. Verify
    **Step** honors its configured cap through normal collisions, and
    **Speed** defaults to its 3.0× multiplier with a 0.90 horizontal cap;
    **Speed**/**BunnyHop** must remain within configured caps and do nothing in
    screens. On a creative/spectator or otherwise permitted test environment,
    verify **Flight** enables normal permitted flight then restores its own
    speed/state. For Flight, Boat Flight, Speed, and Freecam, face south and
    verify A moves east/left and D moves west/right rather than being inverted.
    Enable **Freecam**, move and look around, confirm the player does not move,
    then disable it and confirm the camera returns. In Survival, enable
    **NoFall**, drop far enough to take damage,
    and verify health is unchanged. With **Glide**, verify fast ordinary
    descent is capped at its configured local speed, while Sneak, ground,
    water, climbables, riding, permitted flight, Elytra flight, and open
    screens retain vanilla behavior. While gliding with Elytra,
    verify **ExtraElytra**'s gradual pitch/near-ground adjustment, speed HUD,
    durability warning, and panic hide. With player-provided hotbar blocks,
    enable **Scaffold** without holding Use and verify one normal supported
    placement per delay, temporary hotbar selection with immediate held-slot
    restoration, below/ahead mode, optional local rotation/tower/edge-safety
    requests, and no action for unloaded/occupied targets or open screens.
    Finally set **Timer** within its safe range, verify it disables on
    disconnect/world leave, and confirm no module claims server-side movement
    or tick-rate changes.
45. In a disposable local/test world, enable each **Combat** module separately.
    Verify **TriggerBot** acts only for a visible crosshair target with normal
    cooldown and, with its option enabled, a conventional melee item. Verify
    **BowAimAssist** moves the local view gradually only while Use is held with
    a bow, draws one local target outline, never releases the bow, and clears
    the outline on disable/world leave. While falling normally with Attack
    held, verify **CriticalAssist** requests no attack when grounded, in water,
    climbing, or fall-flying. Verify **KillAura** respects its range/FOV,
    friend/bot exclusions (including the configured invisible heuristic), delay,
    bounded rotation speed, priority, and single/switch/multi modes; Multi must
    never exceed its configured target limit. Confirm it never selects a target
    behind a solid block and that no other combat module begins an attack in the
    same tick. Put a
    healing splash/drink potion and a non-healing potion in the hotbar; verify
    **AutoPotion** selects only the configured healing potion below its health
    threshold, uses Minecraft's normal item path, then restores its owned slot.
    On a disposable multiplayer server, set **AutoLeave** to a safe test
    threshold and verify it uses Minecraft's ordinary leave flow only for an
    enabled low-health or dangerous-fall condition. Disable each condition in
    turn to verify it cannot leave, and verify it never leaves a local world or
    causes **AutoReconnect** to rejoin after its intentional leave.
    On a private or explicitly permitted server with a cooperating second
    player, give the local player ordinary allowed blocks and enable **Domain
    Expansion**. Verify Manual and Automatic Proximity honor the KillAura-style
    **Players**, **Hostiles**, **Passive**, and **Exclude friends** controls.
    Repeat with a Warden and one passive mob, confirming disabled categories
    report no target. Verify the module keeps the local player and selected
    target inside one padded bounded arena, prioritizes lower/escape-side walls,
    reuses existing solid terrain,
    confirm normal placements before advancing, close the roof, and restore the
    original slot/rotation after completion or cancellation. Repeat with
    insufficient blocks, an unloaded boundary, missing support, a rejected
    placement, a moving/escaping target, a local player beside the planned
    wall, and Manual Final Seal. Confirm no block intersects either player,
    the selected mob, or another nearby living entity,
    retries/cooldowns stay bounded, the plan/HUD states are distinct, and every
    result remains subject to normal server placement reach and authority.
    Verify **TargetHUD** shows only local crosshair/attack facts and
    **ReachDisplay** reports only a measured Helikon attack request distance.
    Finally toggle **AntiBot** options with a test player/list state and verify
    they merely exclude local targets and create no network/service request.
46. Before a release candidate, start from a fresh Minecraft profile and run
    the relevant module smoke checks above. Run `./gradlew.bat check
    releaseBundle`, confirm the bundle contains the remapped non-dev JAR,
    SHA-256 checksum, and dependency report, then inspect the source-style and
    client-only architecture checks. Confirm that no release workflow publishes
    an untagged build and record any manual checks not performed.

## Manual Phase F world-module smoke test

1. In a disposable singleplayer world, enable **SeedCracker**. Verify the HUD
   reports the same full seed as `/seed`, its HUD handle can be dragged/scaled
   in HUD Settings, and a manually added current loaded chunk renders a bounded
   world marker. Disable, panic, leave/rejoin, and change dimensions; verify no
   stale evidence/result or marker survives a world transition.
2. On a private or explicitly permitted multiplayer server, set a small known
   `Search start`/`Search count`, add four verified slime chunks with
   `.seedcracker addslime`, and start `.seedcracker search`. Verify progress is
   bounded by `Candidates per tick`, `.seedcracker candidates` labels results
   as lower-48-bit structure seeds, and clearing/removing evidence is local.
   Spawn or move a slime artificially and verify the documented provenance
   limitation is not presented as proof. Confirm no new chunk loads, packets,
   file, or external connection occur.
3. Run `./gradlew.bat runClient` in a disposable local/test world with a cactus
   beside a clear walking path. With **AntiCactus** disabled, verify normal
   vanilla movement and damage behavior. Enable it and walk diagonally toward
   the cactus: verify the local player slides only along a clear horizontal
   axis instead of entering its collision box. Use a vehicle or cross an
   unloaded boundary and verify the module does not claim to alter those
   movements. If contact occurs, cactus damage remains vanilla.
4. Enable **BlockSelection** and target ordinary nearby blocks. Verify exactly
   one local outline follows Minecraft's visible block target; toggle **Fill**,
   change outline color/line width, and toggle **Distance label** to verify
   each local rendering setting independently. Target air, an entity, or an
   unloaded boundary and verify no box/label appears. Disable the module and
   verify the extra rendering disappears immediately.
5. In a permitted multiplayer check, confirm neither behavior sends a custom
   packet, changes reach, edits a block, or becomes visible to another player.

## Manual Phase I world-automation smoke test

1. In a disposable local/test world, enable **FastBreak**, hold Attack on a
   normal loaded block, and verify only the normal local post-break cooldown is
   reduced. Set a specific `blocks` filter and verify a non-matching block is
   unchanged. Release Attack, open a screen, disable the module, and verify no
   new mining action is started and Minecraft's normal cooldown behavior
   returns.
2. Keep **Nuker** disabled and hold Attack near
   blocks: verify it does nothing. In a disposable local/test world, whitelist
   one harmless block ID, keep the default one-request safety limit, hold
   Attack, and verify only loaded, reachable matching blocks receive ordinary
   mining attempts. Test a screen-open state, blacklist, out-of-range target,
   obstructed line of sight, and a low-durability-only tool hotbar. Raise both
   caps only to two and verify no more than two normal requests occur in a
   tick. The server remains authoritative; do not treat local prediction as a
   successful break.
3. Enable **Baritone** and run `.baritone goto` toward a safe nearby coordinate.
   Verify route/goal visuals appear, pathing continues with inventory, crafting,
   furnace, and Helikon screens open, Pause/Resume work, and Stop or disabling
   the module releases movement. Run a harmless local-world mining command and
   confirm normal server-authoritative block actions. Confirm there is no
   separate Baritone JAR in `mods`, no runtime download, and no external
   request.

## Manual Phase K miscellaneous-control smoke test

1. In a disposable local/test world, enable **Twerk**. Verify local sneaking
   alternates at the configured half-cycle, physical Shift stays effective,
   opening chat or the ClickGUI stops the pulse immediately, and disable or
   panic leaves no forced sneak input.
2. Enable **OneClickFriends**, target a player, and middle-click once to add
   then again after release to remove that local friend. Disable the module and
   verify the same gesture changes nothing; enable it while holding middle
   click and verify it still waits for a release and a new click. Screens must
   never change the friend store.
3. Enable **SkinBlinker** in third-person view. Verify only the local skin
   layers alternate at the configured interval, disable or panic restores the
   previous layers, opening a screen pauses/restores them, and leaving the
   world restores them while the module stays enabled. Quit without changing
   vanilla skin options and verify it did not persist a blink state.
4. Enable **Annoy** only in a permitted disposable/local test. With no screen,
   verify it makes one ordinary visible main-hand swing no faster than the
   configured interval; with chat/ClickGUI open, no player, or the module
   disabled, it must not swing. Verify it never attacks, changes a target, or
   sends chat. Server policy remains authoritative for normal swings.
5. In third-person view, enable **Local Cape** and verify only the local player
   receives the generated cape. Change either local color setting and verify
   the cape updates; disable the module and verify the next render uses the
   normal local cape state. Join a multiplayer test server and verify another
   client never receives an asset or sees a changed cape.
6. Enable **Local Cosmetics** and verify one local aura ring follows the local
   player, the configured radius and 12–48 segment count take effect, and it
   disappears immediately when disabled or panicked. Verify it does not appear
   around other players and no new file/network activity occurs.
7. Enable **Inventory Preview** and verify a read-only grid shows the selected
   1–3 storage rows; enable its hotbar option and verify one extra final row.
   Move or consume an item normally and verify the preview updates without
   opening an inventory or affecting the item. Panic must hide it.
8. Damage a held tool or armor piece in a disposable world, enable
   **Durability Warnings**, and set its threshold above the observed percentage.
   Verify only configured held/armor items at or below the inclusive threshold
   appear, raising the threshold reveals an eligible item, and panic hides the
   warning. No item durability or inventory position may change.
9. Enable **Death Coordinates**, die in a disposable local/test world, and
   verify one local chat/HUD entry gives the observed block position and
   dimension without adding a waypoint. Enable **Logout Coordinates**, leave a
   world after moving, and verify the last observed position is reported. Both
   must do nothing while disabled, hide snapshots after joining a different
   local server/world, and reset after a client restart.

## Manual debug-overlay smoke test

1. In a disposable local/test world, enable **Debug Overlay**. Verify it shows
   a local page of module IDs with tick/render milliseconds, BlockESP and
   StorageESP cache counts, event subscriber count, and the global-save state.
2. Change `page` from 1 through a later valid page and verify different module
   rows appear; values for modules with no measured work should safely be
   `0.000ms` rather than omitted or throwing.
3. Close the ClickGUI after changing any module setting and verify global-save
   state becomes `saved`. Disable Debug Overlay or use panic and verify the HUD
   vanishes and later ordinary module work has no timing probe. Confirm no file
   other than normal configuration output, chat line, packet, or network
   request is created.

## Manual LocalTranslator smoke test

1. Run `./gradlew.bat runClient`, join a local/test world, enable
   **LocalTranslator**, and set its glossary to `hello=Bonjour`. Receive a
   harmless incoming `hello` line and verify Minecraft's original message stays
   unchanged while a local Helikon translation line appears. Receive a message
   with no exact entry and verify no translation line is added.
2. Open a screen, receive an overlay/action-bar message, then disable the
   module; verify none produces a translation. Set a malformed glossary entry
   and verify it safely produces no translation.
3. Confirm no network request, API-key prompt, outgoing message, or server-side
   visible formatting occurs. This module has no provider or API selection.

## Manual command and keybind smoke test

1. In a world, send `.help` in chat and verify the command list appears in
   chat (gray text with a gold `[Helikon]` prefix) and nothing is sent to the
   server (no "unknown command" from the server, not visible to other
   players).
2. Send `.toggle fullbright`, then `.modules`, and verify the state
   changed. Send `.toggle nope` and verify a red error.
3. Send `.setting fullbright brightness 0.7` and verify the ClickGUI shows
   0.7; send an out-of-range value and verify the red range error.
4. Send `.bind fullbright r`, close chat, press R, and verify the module
   toggles. Open chat, type `r`, and verify the module does not toggle. Send
   `.bind fullbright r hold`, hold R, and verify it enables only while
   held.
   Send `.bind fullbright ctrl+mouse4`, close chat, and verify Mouse 4 alone
   does nothing while Ctrl+Mouse 4 activates it. Bind another module to the
   same combination and verify the command emits a local conflict warning.
   In chat type `.tog` and press Tab; verify it completes `.toggle` locally,
   while Tab on `/` commands and after `.toggle ` remains vanilla.
5. Send `.gui` and verify the ClickGUI opens after chat closes.
6. Send `.panic` and verify all modules disable.
7. Quit and relaunch; verify the keybind still works (persisted in
   `global.json`).
8. Send `.profile save smoke`, change a setting or module state, then send
   `.profile load smoke` and verify the saved local state returns. Send
   `.profile list` and `.profile delete smoke`; verify the profile is listed
   then removed. Replace a profile file with invalid JSON and verify it is
   renamed to `smoke.corrupt-<timestamp>.json` without changing live state.
9. Save `smoke`, run `.profile duplicate smoke copy` and
   `.profile rename copy renamed`, then verify `renamed` appears in
   `.profile list`, loads correctly, and `copy` no longer exists.
10. Export `smoke` as `portable`, copy its file from `exports/` to `imports/`
    as `incoming.json`, and import it as `restored`. Verify it loads correctly;
    an unsupported-schema import must be rejected without creating a profile.
11. Set `smoke` as default, rename it, then delete it. Verify `profiles.json`
    follows the rename and its default entry is cleared after deletion.
12. Associate `smoke` with a server address and a world ID. Verify the
    associations survive a profile rename and are cleared when that profile is
    deleted; case changes in the server address should still resolve it.
13. Add a friend with `.friend add Alice_1`, set a color, list it, and remove
    it. Target a player and middle-click once to add them, hold the button to
    verify it does not repeatedly toggle, then release and middle-click again
    to remove them. Verify clicks while a screen is open do not alter friends.
    Relaunch after adding to verify `friends.json` persists; malformed or
    case-insensitively duplicate entries must become
    `friends.corrupt-<timestamp>.json`.
14. In a world, run `.waypoint add home`, walk away, and run `.waypoint list`.
    Verify the current-dimension Baritone entry reports distance and a compass
    direction and appears in the Waypoints HUD. Add manual coordinates with
    `.waypoint add mine 10 70 -5`, rename and remove entries, and relaunch to
    verify Baritone persistence. Exercise `#waypoint list` and confirm its
    user/home/death/bed entries also appear through `.waypoint list`. With a
    pre-existing valid `waypoints.json`, verify its current-context entries
    migrate after joining without replacing same-named Baritone entries. Set
    the Waypoints Scale to its minimum, default, and maximum while viewing a
    distant marker; verify its size changes at every step. At the default
    setting, the complete projected label panel remains no taller than 5% of
    the GUI. Add an unnamed HOME waypoint through Baritone and verify Helikon
    displays it as `Home` without crashing; rename or remove it through
    Helikon using that displayed name.
15. Create a macro with `.macro create smoke`, then append a local action, a
    delay, ordinary chat text, and a Minecraft command (without `/`). Verify
    `show` reports each action, `run` executes no more than one action per tick
    after chat closes, and `stop` cancels it. Create a server-scoped macro on a
    multiplayer server and change servers before its next action; it must stop.
    Verify chat actions beginning with `.` or `/`, command actions with `/`,
    and local `.macro` recursion are rejected. Relaunch to verify
    `macros.json` persistence and backup; malformed or duplicate data must
    become `macros.corrupt-<timestamp>.json`.
16. Bind panic with `.panic bind r`, enable a module, and press R outside a
    screen. Verify all modules disable, Helikon custom HUD hides, queued macros
    stop, and the persisted module/HUD layout files remain unchanged. Open the
    ClickGUI and press R to verify it closes; press R while typing in chat to
    verify it does not fire. Use `.panic restorehud`, relaunch to verify the
    key persists in `panic.json`, and replace that file with invalid JSON to
    verify `panic.corrupt-<timestamp>.json` recovery and an unbound fallback.

## Bootstrap smoke test

1. Run `./gradlew.bat runClient` using Java 25.
2. Confirm the client starts without a configuration directory.
3. Press Right Shift and verify the ClickGUI opens.
4. Close the client and verify `config/helikon/global.json` is created.
5. Replace `global.json` with invalid JSON, start again, and verify a
   `global.corrupt-<timestamp>.json` backup is created.
