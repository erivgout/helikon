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
`HudConfigurationManagerTest`).

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

## Bootstrap smoke test

1. Run `./gradlew.bat runClient` using Java 25.
2. Confirm the client starts without a configuration directory.
3. Press Right Shift and verify the ClickGUI opens.
4. Close the client and verify `config/helikon/global.json` is created.
5. Replace `global.json` with invalid JSON, start again, and verify a
   `global.corrupt-<timestamp>.json` backup is created.
