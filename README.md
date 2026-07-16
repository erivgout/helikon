# Helikon

Helikon is a clean-room, open-source Fabric utility client for Minecraft Java
Edition. This repository currently contains the **core framework**: the client
entrypoint, modular lifecycle API, basic settings, local JSON configuration,
an internal event bus, a functional Right Shift ClickGUI, local chat commands,
module keybinds, an Active Modules HUD, local profiles, and tests.

It is not affiliated with Aristois, and it does not contain copied code, assets,
branding, or configuration formats from that project.

## Status

The current milestones deliberately do **not** implement gameplay automation,
combat tools, packet manipulation, external networking, telemetry, a custom
backend, or a server-side component. `FullbrightStub` exists only to exercise
the registry and settings architecture; it does not change game brightness.

The ClickGUI currently provides:

- a category sidebar driven by `ModuleCategory`
- a scrollable module list with per-module toggles
- search across module names, IDs, and descriptions
- a settings panel with metadata plus editable boolean and number settings
- in-GUI keyboard keybind assignment (Backspace/Delete unbinds; Escape cancels)
- reset buttons for individual settings and whole modules
- a draggable, resizable, clamped window saved in `global.json`
- three local ClickGUI palettes, selected from the Theme editor and saved in `global.json`
- keyboard navigation: Left/Right changes categories, Up/Down selects module rows, and Enter/Space toggles the selected module
- persistence of module state and settings when the screen closes

The first HUD slice adds an **Active Modules** list. It renders enabled Helikon
modules in registry order and can be positioned through the small HUD editor.
Open the ClickGUI with Right Shift and select **HUD** in its header; drag the
preview, toggle it with the checkbox, and press Escape to return. The layout
is stored locally in `config/helikon/hud.json`.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.0+26.2
- Java 25

## Build

```powershell
.\gradlew.bat build
```

The remapped mod JAR is produced in `build/libs`. To start a development client:

```powershell
.\gradlew.bat runClient
```

Press Right Shift in the client to open the ClickGUI. Click a category to list
its modules, click a module to inspect and edit its settings, and click the
square at the right of a row to toggle it. The search box at the top filters
across every category. Changes are written to `global.json` when the screen
closes. Select **HUD** in the header to open the minimal HUD editor; its
changes are written to `hud.json` when that editor closes. Drag the ClickGUI
by an empty part of its header. In a selected module's settings panel, click
**Bind** and press a key; Backspace/Delete removes the bind and Escape cancels.
Drag its bottom-right handle to resize it; the position and dimensions are
restored locally when it is reopened.
Select **Theme** in the header to choose Midnight, High Contrast, or Ocean;
the client never downloads themes or contacts a service.
With no text field focused, use Left/Right to switch categories, Up/Down to
select a module, and Enter or Space to toggle it.

## Local commands

Chat messages that start with `.` are intercepted on the client and are never
sent to the server:

| Command | Effect |
| --- | --- |
| `.help` | Lists all local commands. |
| `.modules` | Lists registered modules and their state. |
| `.toggle <module>` | Enables or disables a module by ID. |
| `.search <text>` | Finds modules by name, ID, or description. |
| `.setting <module> <setting> <value>` | Changes a boolean or number setting. |
| `.reset <module>` | Resets a module's settings to defaults. |
| `.bind <module> <key> [toggle\|hold\|press_once]` | Binds a key to a module. |
| `.unbind <module>` | Removes a module's keybind. |
| `.gui` | Opens the ClickGUI. |
| `.profile list` | Lists saved local profiles. |
| `.profile save\|load\|delete <name>` | Saves, loads, or deletes a local module/ClickGUI snapshot. |
| `.panic` | Disables all modules immediately. |

Key names follow Minecraft's keyboard names, for example `r`, `f6`, or
`right.shift`. Module keybinds never fire while any screen is open, so typing
into chat or a search box cannot toggle modules. Tab completion is not
implemented yet. Profile names are local, lowercase file-safe tokens (letters,
digits, `_`, and `-`), and no profile data is synchronized or sent to a server.

## Local data and privacy

Helikon stores its initial global configuration locally at:

```text
.minecraft/config/helikon/global.json
```

No Helikon-operated backend, telemetry, account service, remote feature flags,
or cloud synchronization is used. See [networking.md](docs/networking.md) and
[privacy.md](docs/privacy.md).

## Multiplayer

Use only on servers whose rules permit client modifications. Minecraft servers
remain authoritative; future client-side features must not claim to guarantee
server-side behavior.

## Development

Read [architecture.md](docs/architecture.md), [configuration.md](docs/configuration.md),
and [testing.md](docs/testing.md) before changing core systems. The long-term
roadmap and policies live in [PLAN.md](PLAN.md). Contributions
should include tests and keep all behavior local-only unless an explicitly
documented optional integration is approved.

## License

Helikon is released under the [MIT License](LICENSE).
