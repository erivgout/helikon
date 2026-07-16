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

## Manual Active Modules HUD smoke test

1. Run `./gradlew.bat runClient`, join a world, and send
   `.toggle fullbright_stub`. Verify **Fullbright (Stub)** appears in a small
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
2. Click the **Render** category and verify `Fullbright (Stub)` is listed.
3. Click the square at the right of the module row and verify it fills with
   the accent color (enabled); click again to disable.
4. Click the module name and verify the right panel shows its name, category,
   ID, description, an **Enabled** row, the **Gamma mode** checkbox, and the
   **Brightness** number field with its range.
5. Toggle **Gamma mode** and type a new **Brightness** value. Out-of-range or
   non-numeric text must turn red and leave the stored value unchanged.
6. Type into the search box and verify matches by name (`full`), ID (`stub`),
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

## Manual command and keybind smoke test

1. In a world, send `.help` in chat and verify the command list appears in
   chat (gray text with a gold `[Helikon]` prefix) and nothing is sent to the
   server (no "unknown command" from the server, not visible to other
   players).
2. Send `.toggle fullbright_stub`, then `.modules`, and verify the state
   changed. Send `.toggle nope` and verify a red error.
3. Send `.setting fullbright_stub brightness 5` and verify the ClickGUI shows
   5; send an out-of-range value and verify the red range error.
4. Send `.bind fullbright_stub r`, close chat, press R, and verify the module
   toggles. Open chat, type `r`, and verify the module does not toggle. Send
   `.bind fullbright_stub r hold`, hold R, and verify it enables only while
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

## Bootstrap smoke test

1. Run `./gradlew.bat runClient` using Java 25.
2. Confirm the client starts without a configuration directory.
3. Press Right Shift and verify the ClickGUI opens.
4. Close the client and verify `config/helikon/global.json` is created.
5. Replace `global.json` with invalid JSON, start again, and verify a
   `global.corrupt-<timestamp>.json` backup is created.
