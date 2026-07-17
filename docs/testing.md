# Testing

Run the test suite with:

```powershell
.\gradlew.bat test
```

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
AntiBlind setting gating, BetterCrosshair geometry, and strict ARGB color
settings are covered by `AntiBlindTest`, `BetterCrosshairTest`,
`CrosshairGeometryTest`, and `SettingTest`.
`SettingTest` also covers integral and range bounds, keybind JSON recovery,
immutable bounded string/identifier lists, item/block/entity selector tokens,
stable multi-select enum JSON, safe-regex rejection, visibility predicates,
and validated change listeners.
AutoSprint's hunger, collision, direction, and reversible ownership decisions,
plus AutoWalk's GUI and steering input policy, are covered by
`AutoSprintTest` and `AutoWalkTest`.
AutoSneak's Toggle, bound-key Hold, Edge-only, and screen-suppression policies,
plus enum-setting recovery and command validation, are covered by
`AutoSneakTest`, `SettingTest`, and `BuiltinCommandsTest`.
AutoTool's correct-tool scoring, durability guard, ownership-aware slot restore,
and safe no-selection behavior are covered by `AutoToolTest`.
FastPlace's held-use gate, item filtering, safe delay floor, and invalid input
rejection are covered by `FastPlaceTest`.
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
Entity category/friend/range gating, local block-ID parsing, incremental cube
scan order, cache eviction, and Breadcrumb sampling/age bounds are covered by
`EntityRenderFilterTest`, `BlockEspPolicyTest`, and `BreadcrumbTrailTest`.
Trajectory drag/gravity ordering, collision stopping, radar projection/clipping,
and ARGB transparency are covered by `TrajectorySimulatorTest`,
`RadarProjectionTest`, and `RenderColorTest`.
XRay target filtering, opacity validation, reversible renderer-invalidation
decisions, and StorageESP target-family parsing are covered by
`XRayRenderStateTest`, `XRayTest`, and `StorageEspTargetsTest`.
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
Combat filtering, deterministic selection, bounded bow smoothing, legitimate
critical gates, restorative-potion slot ownership, anti-bot heuristics, and
session HUD tracking are covered by `CombatPolicyTest`. Minecraft combat and
HUD wiring additionally have the manual checklist below because they depend on
verified 26.2 local game APIs.
Configuration migration coverage verifies that legacy Fullbright state loads
under the production module ID and that the next atomic save removes the legacy
key (`ConfigurationManagerTest`).

## Manual Active Modules HUD smoke test

1. Run `./gradlew.bat runClient`, join a world, and send
   `.toggle fullbright`. Verify **Fullbright** appears in a small
   top-left HUD panel; send the command again and verify the panel disappears.
2. Open the ClickGUI with Right Shift and click **HUD** in its header. Verify a
   dimmed editor opens, with an Active modules checkbox and a preview (showing
   either enabled modules or `No modules enabled`).
3. Drag the preview to each screen edge and verify it stays fully visible;
   turn its checkbox off and verify the normal HUD no longer renders after
   leaving the editor. Turn it back on and move it to a distinct position.
4. Press Escape and verify the ClickGUI returns. Close it, relaunch the
   client, and verify the enabled state and position are restored from
   `config/helikon/hud.json`.
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
14. Without clicking a text field, use Left/Right to change categories and
    Up/Down to select module rows. Verify selection wraps and scrolls into
    view, then use Enter and Space to toggle the selected module. Click the
    search or a number field and verify its normal keyboard editing continues
    to take priority.
15. Select **BetterCrosshair**, enable it, and verify its custom crosshair
    replaces the vanilla one. Change its size, gap, thickness, outline, and
    `#AARRGGBB` color text field; while moving, verify the optional dynamic gap
    responds locally. Disable it and verify the vanilla crosshair returns.
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
    hidden locally. Enter a malformed or nested-quantifier regex and verify it
    does not freeze the client or hide unrelated messages.
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
33. In a permitted local/test world, enable **EntityESP** and **Tracers**.
    Verify selected nearby player/hostile categories draw local boxes or lines,
    locally saved friends use the friend color, and entities outside the range
    do not render. Enable **BlockESP** with a harmless known block ID and
    verify its box appears after a bounded scan pass; move, change the ID list,
    or break/place a target and verify the cache eventually refreshes without
    a frame hitch. Enable **Breadcrumbs**, walk a short path, and verify a
    local line follows sampled positions, respects its point/age limits, and
    clears after disable or a world change. Verify none of these changes are
    visible to another player or affect normal interaction.
    Enable **Better Nametags** and verify each health, distance, armor, held
    item, and friend-status toggle affects only its corresponding local text.
    Verify friend color/status disappears when friend status is off, players
    outside range or the camera frustum do not render, and invisible or
    solid-block-occluded players receive no name-tag billboard.
34. In a permitted local/test world, enable **Trajectories** while arrows,
    tridents, snowballs, eggs, ender pearls, or splash potions are in flight.
    Verify each configured type draws a local path that ends at its first block
    impact marker, and that disabling it immediately removes the preview.
    Use an invisible entity in a controlled world to verify **TrueSight** draws
    only its configured translucent local box, then toggle player/hostile/
    passive filters and transparency. Verify a friendly-team entity that is
    visible to the local player does not gain a box. Turn away from a candidate
    to verify its trajectory/TrueSight overlay is frustum-culled. Enable
    **Radar**, verify circle/square,
    rotation, zoom, local friend color, and category filters, and confirm no
    unloaded or out-of-range entities appear. None of these results should be
    visible to another player or change a normal projectile/entity interaction.
35. In a disposable local/test world, enable **XRay** and verify only its
    configured locally loaded block models remain visible after the geometry
    rebuild; change the block list and opacity, wait for the local rebuild, and
    verify the display follows it. Disable XRay and verify normal world geometry
    fully returns. Enable **StorageESP** near known selected storage, then
    verify its box appears after a bounded scan pass, is culled when offscreen,
    honors category/custom-ID settings, and never opens or changes a container.
36. In a local/test world, enable **MiniPlayer** and verify the local player
    model appears in its fixed HUD panel, responds to rotation/scale/background
    settings, and armor on/off changes only the panel. Enable **DamageIndicators**
    near an eligible mob/player, observe a normal local health loss, and verify
    one amount rises and fades with the configured duration/color. Verify a
    target outside range or behind the camera has no label, no damage changes
    occur without a local health decrease, and neither feature affects combat.
37. In a disposable local/test world, open the player's normal inventory with
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
38. In a disposable chest containing harmless items, enable **ChestSteal** and
    verify one normal quick move occurs per configured delay, whitelist and
    blacklist filters leave the correct items, priority changes the transfer
    order, and close-after-completion closes only after eligible items are
    exhausted. Keep a nonempty cursor or leave the chest screen and verify no
    action occurs.
39. In a permitted fishing test world, select a player-provided rod and enable
    **AutoFish**. Verify it casts once, waits for a visible bite and the reel
    delay, then reels and waits for the recast delay before casting again.
    Turn on open-water-only near non-open water and verify it does not reel;
    set the durability reserve above the remaining rod durability and verify it
    stops. Open a screen and verify it does nothing.
40. On a disposable multiplayer target, disconnect unexpectedly with
    **AutoReconnect** enabled. Verify the local countdown and Cancel button,
    cancel once to verify no connection is made, then retry and verify no more
    than the configured attempts use Minecraft's normal connect screen. Leave
    explicitly or use a local world and verify it never reconnects.
41. In a local/test world with ordinary blocks, enable **BuilderAssist**, hold
    Use with a player-provided block, and target a replaceable face. Verify the
    bounded local preview follows each single/line/floor/wall mode and length,
    width, height, and color settings. Verify repeat placement honors its delay
    and stops when Use is released, the held item is not a block, a screen is
    open, a target is unloaded/occupied, or normal vanilla placement fails.
42. In a disposable local/test world, enable each Advanced Movement module
    separately. With **NoSlow**, verify each enabled food/block/bow/sneak/
    soul-sand/honey/cobweb category changes only local responsiveness and no
    other entity is affected. Verify **FastLadders** changes only normal
    climbable movement, **Step** honors its 1.5-block cap through normal
    collisions, and **Speed**/**BunnyHop** remain within their configured caps
    and do nothing in screens. On a creative/spectator or otherwise permitted
    test environment, verify **Flight** enables normal permitted flight then
    restores its own speed/state; enable its Freecam view, move and look around,
    confirm the player does not move, then disable it and confirm the camera
    returns. Verify **NoFall** does nothing without `mayfly`. While gliding,
    verify **ExtraElytra**'s gradual pitch/near-ground adjustment, speed HUD,
    durability warning, and panic hide. With player-provided hotbar blocks,
    hold Use for **Scaffold** and verify one normal supported placement per
    delay, hotbar selection, below/ahead mode, optional local rotation/tower/
    edge-safety requests, and no action for unloaded/occupied targets or open
    screens. Finally set **Timer** within its safe range, verify it disables on
    disconnect/world leave, and confirm no module claims server-side movement
    or tick-rate changes.
43. In a disposable local/test world, enable each **Combat** module separately.
    Verify **TriggerBot** acts only for a visible crosshair target with normal
    cooldown and, with its option enabled, a conventional melee item. Verify
    **BowAimAssist** moves the local view gradually only while Use is held with
    a bow, draws one local target outline, never releases the bow, and clears
    the outline on disable/world leave. While falling normally with Attack
    held, verify **CriticalAssist** requests no attack when grounded, in water,
    climbing, or fall-flying. Verify **KillAura** respects its range/FOV,
    friend/bot exclusions (including the configured invisible heuristic), delay, bounded rotation speed, priority, and single/switch mode, never
    selects a target behind a solid block, and makes no more than one ordinary
    Helikon attack request in a tick alongside the other combat modules. Put a
    healing splash/drink potion and a non-healing potion in the hotbar; verify
    **AutoPotion** selects only the configured healing potion below its health
    threshold, uses Minecraft's normal item path, then restores its owned slot.
    Verify **TargetHUD** shows only local crosshair/attack facts and
    **ReachDisplay** reports only a measured Helikon attack request distance.
    Finally toggle **AntiBot** options with a test player/list state and verify
    they merely exclude local targets and create no network/service request.
44. Before a release candidate, start from a fresh Minecraft profile and run
    the relevant module smoke checks above. Run `./gradlew.bat check
    releaseBundle`, confirm the bundle contains the remapped non-dev JAR,
    SHA-256 checksum, and dependency report, then inspect the source-style and
    client-only architecture checks. Confirm that no release workflow publishes
    an untagged build and record any manual checks not performed.

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
    Verify the current-dimension entry reports distance and a compass direction
    and appears in the small Waypoints HUD. Add manual coordinates with
    `.waypoint add mine 10 70 -5`, use `color`, `icon`, and `toggle`, then
    verify disabled entries are absent from the HUD but identified by `list`.
    Rename and remove an entry, relaunch to verify persistence and backup
    creation, then replace `waypoints.json` with malformed or duplicate data;
    it must become `waypoints.corrupt-<timestamp>.json` and show no entries.
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
