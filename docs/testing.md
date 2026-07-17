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
Fullbright's gamma restoration, setting-driven Night Vision lifecycle, and
identity-safe effect restoration are covered by `FullbrightGammaControllerTest`,
`FullbrightTest`, and `ClientEffectOverrideStateTest`.
AntiBlind setting gating, BetterCrosshair geometry, and strict ARGB color
settings are covered by `AntiBlindTest`, `BetterCrosshairTest`,
`CrosshairGeometryTest`, and `SettingTest`.
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
