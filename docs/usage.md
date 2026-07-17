# Usage

## ClickGUI

Press **Right Shift** in game to open the ClickGUI.

- Click a category to list its modules. **Active** at the top shows every
  enabled utility across categories; click a row's square there to turn it
  off. Click a module to inspect and edit its settings.
- The search box at the top filters across module names, IDs, and
  descriptions in every category.
- In a module's settings panel, click **Bind** and press a key to assign a
  keybind; Backspace/Delete removes the bind and Escape cancels. Held
  modifiers are captured.
- Every setting and module has a reset control.
- Hover the right-hand settings panel and use the mouse wheel to scroll long
  setting groups. Editable fields remain aligned with their setting as you
  scroll.
- Drag the window by an empty part of its header, and drag its bottom-right
  handle to resize. Position and size are clamped to the screen and restored
  when it is reopened.
- Select **Theme** in the header to choose Midnight, High Contrast, or Ocean,
  interface scale (0.75x–1.50x), and a reduced-animation preference. Themes
  are local; the client never downloads themes or contacts a service.
- With no text field focused, Left/Right switches categories, Up/Down selects
  a module row, and Enter or Space toggles the selected module.
- Module state and settings are written to `config/helikon/global.json` when
  the screen closes.

Text controls validate every supported compact setting type before changing
the stored value: semicolons separate text/identifier lists, commas separate
multi-enum selections, `minimum..maximum` expresses ranges, and
`keyboard|mouse:code:toggle|hold|press_once[:modifiers]` expresses standalone
keybind settings. Invalid input is shown in red and the last valid value is
retained. Color rows provide alpha/red/green/blue picker tracks below the
`#AARRGGBB` text value.

## HUD editor

Select **HUD** in the ClickGUI header. The editor is a drag-only canvas: drag
the Active Modules preview (it snaps to edges and centre) or any enabled
element preview directly — Waypoints, Coordinates, Saturation, Elytra, Target
HUD, Reach, Inventory Preview, Durability Warnings, Radar, MiniPlayer, Debug
Overlay, Better Crosshair, plus opt-in Direction, FPS, Ping, local TPS
estimate, Speed, Armor durability, Held-item durability, Potion effects,
Clock, Biome, Server address, and Totem count readouts.

Presentation options live on the separate **HUD settings** screen, opened
from the button in the editor's header. It holds the Active Modules
sort/alignment/color/scale rows and a per-element selector with each
element's enable toggle, scale, alignment, background, padding, text shadow,
color, and rainbow setting; Escape returns to the editor. The layout is
stored locally in `config/helikon/hud.json` when either screen closes.

## Local commands

Chat messages that start with `.` are intercepted on the client and are never
sent to the server:

| Command | Effect |
| --- | --- |
| `.help` | Lists all local commands. |
| `.modules` | Lists registered modules and their state. |
| `.toggle <module>` | Enables or disables a module by ID. |
| `.search <text>` | Finds modules by name, ID, or description. |
| `.setting <module> <setting> <value>` | Changes a boolean, number, `#AARRGGBB` color, or documented enum setting. |
| `.reset <module>` | Resets a module's settings to defaults. |
| `.bind <module> <key> [toggle\|hold\|press_once]` | Binds a keyboard/mouse input, optionally with modifiers, to a module. |
| `.unbind <module>` | Removes a module's keybind. |
| `.gui` | Opens the ClickGUI. |
| `.profile list` | Lists saved local profiles. |
| `.profile save\|load\|delete <name>` | Saves, loads, or deletes a local module/ClickGUI snapshot. |
| `.profile duplicate\|rename <from> <to>` | Creates a named copy or renames a local profile. |
| `.profile default <name\|clear>` | Sets or clears the locally persisted startup profile. |
| `.profile server <address> <profile\|clear>` | Associates a local profile with a server address; it overrides the default on matching joins. |
| `.profile world <id> <profile\|clear>` | Associates a local profile with a singleplayer world name; it overrides the default on matching joins. |
| `.profile import <file> <name>` / `.profile export <name> <file>` | Imports from `imports/` or exports to `exports/` below the Helikon config directory. |
| `.friend list\|add\|remove <player>` | Lists or changes local player-name friends. |
| `.friend color <player> <#RRGGBB\|#AARRGGBB>` | Sets the local friend render color. |
| `.waypoint list` | Lists local waypoints for the current world and dimension by distance. |
| `.waypoint add <name> [x y z]` | Saves the current location or supplied coordinates locally. |
| `.waypoint remove <name>` / `.waypoint rename <from> <to>` | Deletes or renames a local waypoint here. |
| `.waypoint toggle\|color\|icon <name> ...` | Changes a waypoint's local visibility, color, or optional icon token. |
| `.macro list` / `.macro create <name> [global\|server]` | Lists or creates a local macro. |
| `.macro add <name> <local\|chat\|command\|delay> <text\|ticks>` | Adds one explicit, bounded macro action. |
| `.macro show\|clear\|scope\|delete <name> ...` | Inspects or changes a stored local macro. |
| `.macro run <name>` / `.macro stop` | Starts one local macro or stops its queued run. |
| `.pm <player> <message>` / `.pm history [player]` | Sends one validated normal server PM or views bounded local outgoing history. Prefix a literal `history` target with `--`. |
| `.reply <message>` / `.reply history [player]` | Sends one validated normal server reply or views the same local history. Prefix a literal `history` reply with `--`. |
| `.chat search <text>` / `.chat copy <newest-index>` / `.chat history [count]` | Searches, explicitly copies, or lists bounded local BetterChat display history while BetterChat is enabled. |
| `.history search <text>` / `.history copy\|player\|reopen <newest-index>` / `.history list [count]` | Searches or lists ChatHistory entries, copies a retained message/player name, or reopens a sent line as an unsent local draft. |
| `.panic` | Disables modules, hides custom HUD for this session, and closes Helikon GUI screens. |
| `.panic bind <key>` / `.panic unbind` | Configures or clears the local persisted panic keyboard/mouse bind. |
| `.panic status` / `.panic restorehud` | Shows the bind or restores HUD visibility without re-enabling modules. |

## Keybinds

Key names follow Minecraft's keyboard names, for example `r`, `f6`, or
`right.shift`; binds also accept `mouse1` through `mouse8` and modifiers such
as `ctrl+r` or `alt+mouse1`. Module keybinds never fire while any screen is
open, so typing into chat or a search box cannot toggle modules. The chat
screen completes unambiguous dot-command names with Tab; it never intercepts
server command completion or command arguments. A local warning identifies
module keybind collisions without silently changing either bind. This
reservation also follows any mouse binding assigned to **Open GUI** in
Minecraft Controls.

## Local data locations

All Helikon data lives under `.minecraft/config/helikon/` as human-readable
JSON with atomic replacement, backups, schema versions, and malformed-file
recovery:

- `global.json` — module state, settings, ClickGUI window and theme.
- `hud.json` — HUD layout.
- `friends.json` — local friend names and colors.
- `waypoints.json` — local waypoints scoped per server/world and dimension.
- `macros.json` — explicit local macros (no scripting or code execution).
- `panic.json` — the optional panic keybind.
- `chat-history/` — opt-in ChatHistory logs (hashed per-server filenames,
  bounded retention; disabled by default).
- `profiles/`, `imports/`, `exports/` — local profile snapshots.

Profile names are local, lowercase file-safe tokens (letters, digits, `_`,
and `-`). No profile, friend, waypoint, macro, or chat data is synchronized
or sent anywhere.
