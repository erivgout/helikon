# Usage

## ClickGUI

Press **Right Shift** in game to open the ClickGUI. Each module category is
its own floating panel with an icon header and accent underline, and a
floating search palette sits at the bottom of the screen.

- **Left-click** a module row to toggle the module; enabled modules show
  their name in the accent color.
- **Right-click** a module row to expand its settings inline beneath the row;
  right-click again to collapse them. Expanded modules are remembered.
- **Middle-click** a module row to star it as a favorite; favorites show a
  star beside their name and are collected in the dedicated **Favorites**
  panel.
- Drag a panel by its header to move it; **right-click** a header to collapse
  the panel to just its header. Panel positions are clamped to the screen and
  restored when the GUI is reopened.
- A panel never grows taller than half the screen; longer module lists
  scroll with the mouse wheel.
- The search palette filters across module names, IDs, and descriptions in
  every category; click a result to toggle that module. Drag an empty part of
  the palette to move it.
- In a module's expanded settings, click **Bind** and press a key to assign a
  keybind; Backspace/Delete removes the bind and Escape cancels. Held
  modifiers are captured.
- **Right-click** any setting row to reset that setting; the **Reset module**
  row at the end of the expanded settings restores every default.
- Select **Theme** in the search palette to choose a palette — Slate
  (default), Helikon Gold, Midnight, High Contrast, Ocean, Emerald, Crimson,
  or Amethyst — plus interface scale (0.75x–1.50x) and a reduced-animation
  preference. Panel underlines, icons, sliders, and highlights recolor from
  the chosen theme's accent. Themes are local; the client never downloads
  themes or contacts a service. **HUD** beside it opens the HUD editor.
- Prefer the previous single-window GUI? Click **Classic** in the search
  palette to switch to it; its header gains a **Panels** button that switches
  back. The choice persists across sessions.
- Module state, settings, and the panel layout are written to
  `config/helikon/global.json` when the screen closes.

Text controls validate every supported compact setting type before changing
the stored value: semicolons separate text/identifier lists, commas separate
multi-enum selections, `minimum..maximum` expresses ranges, and
`keyboard|mouse:code:toggle|hold|press_once[:modifiers]` expresses standalone
keybind settings. Invalid input is shown in red and the last valid value is
retained.

Numeric settings show an inline slider: drag or click the track to set a
value, or point at the track and use the mouse wheel to step it; hovering
shows the current value. Enum settings cycle on click. Color settings show a
swatch — click it to open alpha/red/green/blue picker tracks beneath the row.

## HUD editor

Select **HUD** in the ClickGUI search palette. The editor is a clean drag-only canvas
with a compact toolbar and visible eight-pixel grid. Drag the Active Modules
preview or any active element handle; every movable widget snaps to the grid,
stays below the toolbar, and remains fully on-screen. Module-backed handles
stay hidden while their module is off unless selected through HUD settings,
which keeps the canvas focused on the HUD that will actually render.

Presentation options live on the separate **HUD settings** screen, opened
from the button in the editor's toolbar. Its body is scrollable and holds the Active Modules
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
| `.waypoint list` | Lists Baritone waypoints for the current world and dimension by distance. |
| `.waypoint add <name> [x y z]` | Saves the current location or supplied coordinates to Baritone's current-world collection. |
| `.waypoint remove <name>` / `.waypoint rename <from> <to>` | Deletes or renames a Baritone waypoint here. |
| `.macro list` / `.macro create <name> [global\|server]` | Lists or creates a local macro. |
| `.macro add <name> <local\|chat\|command\|delay> <text\|ticks>` | Adds one explicit, bounded macro action. |
| `.macro show\|clear\|scope\|delete <name> ...` | Inspects or changes a stored local macro. |
| `.macro run <name>` / `.macro stop` | Starts one local macro or stops its queued run. |
| `.pm <player> <message>` / `.pm history [player]` | Sends one validated normal server PM or views bounded local outgoing history. Prefix a literal `history` target with `--`. |
| `.reply <message>` / `.reply history [player]` | Sends one validated normal server reply or views the same local history. Prefix a literal `history` reply with `--`. |
| `.chat search <text>` / `.chat copy <newest-index>` / `.chat history [count]` | Searches, explicitly copies, or lists bounded local BetterChat display history while BetterChat is enabled. |
| `.history search <text>` / `.history copy\|player\|reopen <newest-index>` / `.history list [count]` | Searches or lists ChatHistory entries, copies a retained message/player name, or reopens a sent line as an unsent local draft. |
| `.baritone status` | Shows whether the embedded pathfinder is enabled, pathing, paused, or idle and describes its goal. |
| `.baritone goto <x> <y> <z>` / `.baritone mine <block...>` | Enables Baritone and runs a destination or mining command locally. Other Baritone commands can follow the same prefix. |
| `.baritone stop` | Cancels all active Baritone processes and releases movement input. |
| `.seedcracker status` / `.seedcracker candidates` | Shows session evidence, bounded-search progress, and retained lower-48-bit structure-seed candidates locally. |
| `.seedcracker search` / `.seedcracker clear` | Starts the configured bounded search or clears all current-world session evidence/results. |
| `.seedcracker addslime <chunkX> <chunkZ>` / `.seedcracker removeslime <chunkX> <chunkZ>` | Adds or removes manual slime-chunk evidence after the user verifies the chunk coordinates. |
| `.panic` | Disables modules, hides custom HUD for this session, and closes Helikon GUI screens. |
| `.panic bind <key>` / `.panic unbind` | Configures or clears the local persisted panic keyboard/mouse bind. |
| `.panic status` / `.panic restorehud` | Shows the bind or restores HUD visibility without re-enabling modules. |

The **Baritone** module in the World panel contains pathing permissions,
path/goal/break/place/walk-into visualization controls, destination and mine
shortcuts, pause/resume/stop, and a generic command field for the complete
Baritone command surface. If `# commands` is enabled, Baritone's normal local
`#` prefix is also accepted. Do not install a second standalone Baritone JAR
beside Helikon.

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
- `waypoints.json` — legacy Helikon waypoint input retained for one-way
  migration into Baritone; live waypoint writes use Baritone's world data.
- `macros.json` — explicit local macros (no scripting or code execution).
- `panic.json` — the optional panic keybind.
- `chat-history/` — opt-in ChatHistory logs (hashed per-server filenames,
  bounded retention; disabled by default).
- `profiles/`, `imports/`, `exports/` — local profile snapshots.

Profile names are local, lowercase file-safe tokens (letters, digits, `_`,
and `-`). No profile, friend, waypoint, macro, or chat data is synchronized
or sent anywhere.
