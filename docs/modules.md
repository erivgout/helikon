# Modules

## Render modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `fullbright` | Render | Locally increases brightness with reversible gamma or Night Vision. | `use_gamma`, `night_vision`, `brightness` | Gamma is constrained to Minecraft's 0.0–1.0 option range; Night Vision is visual and client-local. |
| `anti_blind` | Render | Hides selected local impairment visuals. | `blindness`, `darkness`, `nausea`, `pumpkin_overlay`, `powder_snow_overlay` | Does not remove effects from the local player or change server state; it only filters their 26.2 render paths. |
| `better_crosshair` | Render | Draws a local four-arm HUD crosshair. | `size`, `gap`, `thickness`, `outline`, `color`, `dynamic_movement`, `hide_vanilla` | Dynamic movement uses local horizontal velocity; hit detection and server packets are unaffected. |

## Movement modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `auto_sprint` | Movement | Requests ordinary local sprinting while its conditions pass. | `always`, `forward_only`, `hunger_check`, `collision_check` | It sends no custom packets and cannot make a server accept sprinting or movement it rejects. It releases only sprint state it requested. |
| `auto_walk` | Movement | Applies continuous local forward input. | `continue_forward`, `stop_on_gui`, `allow_steering` | It changes only the freshly polled local input record. `stop_on_gui` is on by default; turning it off intentionally permits movement while screens are open. |
| `auto_sneak` | Movement | Applies a local sneaking policy. | `mode` (`toggle`, `hold`, `edge_only`) | Toggle holds sneak while enabled; Hold reserves its bound key as input-only after enabling the module through the GUI or a local command; Edge-only holds sneak while moving so vanilla careful movement guards ledges. It is inactive while a screen is open. |

## Player modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `auto_tool` | Player | Selects the best safe hotbar tool while the user is already mining. | `minimum_durability`, `restore_prior_slot` | It changes only the local selected hotbar slot, ignores bare-hand-equivalent items and guarded durability, and uses Minecraft's normal mining path. It never creates a mining or inventory packet. |
| `auto_eat` | Player | Uses existing safe hotbar food when local hunger or health is low. | `hunger_threshold`, `health_threshold`, `food_priority`, `avoided_foods`, `combat_rule` | It selects regular food below full hunger, or an eligible always-edible food at full hunger when health is low. It only holds the normal local Use key when no screen is open and the player is not already using an item manually; an overlapping physical hold is preserved. It restores only a slot it still owns and releases module-owned Use state immediately on disable or panic. |

## World modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `fast_place` | World | Lowers the ordinary local item-use cooldown while Use is held. | `use_delay`, `item_filter` (`all`, `blocks`, `non_blocks`), `safe_minimum_delay` | It only lowers a cooldown Minecraft has already created for a non-empty held item, and restores an unchanged module-owned cooldown immediately on disable or panic. It never generates uses, clicks, or packets; the server still controls all interaction rate limits. |

## Chat modules

| ID | Category | Description | Settings | Limitation |
| --- | --- | --- | --- | --- |
| `chat_prefix` | Chat | Adds configured text before safe ordinary outgoing chat. | `prefix`, `separator`, `exclude_commands`, `exclude_private_messages` | Helikon commands, slash commands, likely authentication commands, and private-message commands are preserved verbatim. Formatting is declined if it would exceed the vanilla normal-chat limit. |
| `chat_suffix` | Chat | Appends configured text to safe ordinary outgoing chat. | `suffix`, `separator`, `per_server_suffixes`, `random_suffixes`, `exclude_commands`, `exclude_private_messages` | A normalized local `server=suffix` entry takes priority; otherwise one random-list entry is chosen locally or the fallback suffix is used. Protected input and over-limit messages are preserved verbatim. |

Every production module will document its stable ID, category, settings,
limitations, acceptance criteria, and automated or manual test coverage here.

Module IDs are lowercase and stable; display names are not used as identifiers.

## ClickGUI

Modules appear in the ClickGUI (Right Shift) under their `ModuleCategory`.
Selecting a module shows its name, category, ID, description, an enabled
toggle, and controls for its `BooleanSetting`, `NumberSetting`, `ColorSetting`,
and finite `EnumSetting` values. Colors use strict `#AARRGGBB` tokens; click
an enum row to cycle its documented choices.
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

## Panic

`.panic` disables every enabled module through `ModuleRegistry`, so current and
future modules run their own `onDisable` restoration (for example brightness,
timer, FOV, or step changes). It closes any Helikon GUI screen, stops queued
macro work, and hides all custom HUD for the current session without deleting
or changing user configuration. Use `.panic restorehud` to show HUD again
without re-enabling modules. `.panic bind <key>` stores a separate local
keyboard-only panic key; chat and ordinary screens suppress it, while Helikon
screens permit it as an emergency close control.

## HUD

| Element | Behavior | Persistence | Limitation | Coverage |
| --- | --- | --- | --- | --- |
| Active Modules | Lists enabled Helikon modules in registry order. | `hud.json`: enabled state and top-left position. | No sorting options, styling controls, snapping, or animation yet. | `ActiveModulesTest`, `HudEditorStateTest`, `HudConfigurationManagerTest`, manual HUD checklist. |

The minimal HUD editor is opened through the **HUD** button in the ClickGUI
header. It shows a preview even when no module is enabled, supports toggling
the element, and clamps dragging so its entire current preview remains on
screen. It is client-only and sends no network traffic.
