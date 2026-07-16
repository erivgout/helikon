# Testing

Run the test suite with:

```powershell
.\gradlew.bat test
```

The automated tests cover module lifecycle behavior, failure isolation,
setting validation/JSON recovery, configuration round-tripping, malformed JSON
recovery, ClickGUI search/filtering (`ClickGuiStateTest`), and number-field
edit rules (`NumberSettingFieldTest`).

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

## Bootstrap smoke test

1. Run `./gradlew.bat runClient` using Java 25.
2. Confirm the client starts without a configuration directory.
3. Press Right Shift and verify the ClickGUI opens.
4. Close the client and verify `config/helikon/global.json` is created.
5. Replace `global.json` with invalid JSON, start again, and verify a
   `global.corrupt-<timestamp>.json` backup is created.
