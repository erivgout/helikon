# Modules

## Bootstrap modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `fullbright_stub` | Render | Exercises module registration, settings, and persistence. | `use_gamma`, `brightness` | Does not modify gamma or gameplay state. |

Every production module will document its stable ID, category, settings,
limitations, acceptance criteria, and automated or manual test coverage here.

Module IDs are lowercase and stable; display names are not used as identifiers.

## ClickGUI

Modules appear in the ClickGUI (Right Shift) under their `ModuleCategory`.
Selecting a module shows its name, category, ID, description, an enabled
toggle, and controls for its `BooleanSetting` and `NumberSetting` values.
Toggles are dispatched through `ModuleRegistry`, so a module that throws
during `onEnable`/`onDisable` is isolated and force-disabled instead of
crashing the client. Setting edits and enabled state persist to
`config/helikon/global.json` when the screen closes.

## Commands and keybinds

Modules can also be controlled through local chat commands (`.toggle`,
`.setting`, `.reset`, `.bind`, `.unbind` — see the README) and per-module
keybinds with `toggle`, `hold`, or `press_once` activation. Keybinds never
fire while a screen is open.
