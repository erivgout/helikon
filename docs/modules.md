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

The settings panel also has **Reset module** plus small **R** buttons for each
visible setting. Reset operations use the existing validated setting defaults.
Its **Bind** row captures one keyboard key locally; Escape cancels capture,
Backspace/Delete clears the bind, and the key that opens the Helikon GUI is
rejected. Existing activation mode (`toggle`, `hold`, or `press_once`) is
preserved when rebinding. Drag an unused portion of the header to move the
window, or its bottom-right handle to resize it. The clamped top-left position
and custom dimensions persist in `global.json`.

Select **Theme** from the ClickGUI header to open its local palette selector.
The initial themes are Midnight, High Contrast, and Ocean; selecting one
changes the panel immediately and persists it locally. Custom colors and the
color picker remain future work.

When no text field is focused, the ClickGUI supports keyboard navigation:
Left/Right changes the category (and exits search), Up/Down moves the selected
module row with wrapping and automatic list scrolling, and Enter or Space
toggles the selected module through `ModuleRegistry`.

## Commands and keybinds

Modules can also be controlled through local chat commands (`.toggle`,
`.setting`, `.reset`, `.bind`, `.unbind`, `.profile` — see the README) and
per-module keybinds with `toggle`, `hold`, or `press_once` activation. Profile
commands save/load/duplicate/rename the complete local module and ClickGUI
snapshot without transmitting it. Import/export uses Helikon's local
`imports/` and `exports/` folders rather than arbitrary paths. A profile can
also be marked as the persisted default. Keybinds never fire while a screen is
open. Server and singleplayer associations are also local profile preferences;
they neither inspect nor send server data beyond the user-provided identifier.

## Friends

Friends are local player-name entries, managed through `.friend list`,
`.friend add`, `.friend remove`, and `.friend color`. Middle-click a targeted
player in the game world to add or remove that name without opening chat.
Each entry stores a local ARGB render color in `friends.json`; no friend data is
sent to a server. Targeting modules will use the friend list for their default
friend-exclusion policy when those modules are introduced.

## Waypoints

`.waypoint add <name>` saves the current block position; supplying `x y z`
saves manual coordinates in the currently loaded server/world and dimension.
`.waypoint list` orders enabled local entries by distance and reports a compass
direction. Waypoints can be removed, renamed, toggled, recolored, or given a
small optional icon token with local commands. The minimal Waypoints HUD shows
up to three nearest enabled entries with distance and direction, and hides
entries from any other server/world or dimension.

Death and logout waypoints are deliberately not automatic yet. The first HUD
indicator has no separate HUD-editor position or world-space beacon; those
rendering and layout controls remain future work.

## Macros

Macros are configured through `.macro`. Create a global or currently connected
server-scoped macro, then append explicit `local`, `chat`, `command`, or
`delay` actions. A local action must start with `.` and stays inside the local
dispatcher; chat text cannot start with `.` or `/`; Minecraft command text
omits `/`; delay actions are bounded to 1-12,000 ticks. Run one macro at a time
with `.macro run <name>` and stop it with `.macro stop`. The runner pauses while
a screen is open, executes no more than one action per client tick, and stops a
server-scoped macro if the current server changes.

Macros are not a scripting engine: they cannot execute code, read files, or
download content. They do not bypass server authority, rate limits, or normal
chat/command packet formats.

## HUD

| Element | Behavior | Persistence | Limitation | Coverage |
| --- | --- | --- | --- | --- |
| Active Modules | Lists enabled Helikon modules in registry order. | `hud.json`: enabled state and top-left position. | No sorting options, styling controls, snapping, or animation yet. | `ActiveModulesTest`, `HudEditorStateTest`, `HudConfigurationManagerTest`, manual HUD checklist. |

The minimal HUD editor is opened through the **HUD** button in the ClickGUI
header. It shows a preview even when no module is enabled, supports toggling
the element, and clamps dragging so its entire current preview remains on
screen. It is client-only and sends no network traffic.
