# Helikon

Helikon is a clean-room, open-source Fabric utility client for Minecraft Java
Edition 26.2. This `1.0.0-rc.1` repository contains the client
entrypoint, modular lifecycle API, basic settings, local JSON configuration,
an internal event bus, a functional Right Shift ClickGUI, local chat commands,
module keybinds, an Active Modules HUD, local profiles, local friends, local
waypoints, local macros, and tests.

Saturation Display is a fixed local HUD readout of the player's current hunger
saturation. It changes no food state and sends no packet or request.

Better Nametags adds only local billboard facts for nearby visible players. It
does not change vanilla name tags, player data, packets, or server visibility.

The internal event bus uses typed, Minecraft-free event models. Its currently
wired lifecycle bridge covers ticks, local world connections, identity-aware
screen changes, accepted ordinary chat, world rendering, and local player-state
observations; the remaining event models await target-version-specific adapter
verification.

Its local settings API supports booleans, integers and decimals, enums,
colors, keyboard binds, bounded text and text lists, block/item/entity ID
selections, enum multi-selections, numeric ranges, and bounded safe regular
expressions. Every value is validated and stored only in local JSON.

It is not affiliated with Aristois, and it does not contain copied code, assets,
branding, or configuration formats from that project.

## Status

Helikon currently includes sixteen client-only render modules: Fullbright restores
the previous gamma and Night Vision state when disabled; AntiBlind selectively
hides local impairment visuals; and BetterCrosshair draws a local configurable
HUD crosshair. EntityESP, BlockESP, Tracers, and Breadcrumbs use Minecraft's
local world-render Gizmo phase only: they never alter entities, blocks, client
movement, or packets. BlockESP deliberately scans a bounded cube at a bounded
per-tick budget, so newly loaded or changed blocks can take one scan pass to
appear. AutoSprint, AutoWalk, and AutoSneak apply only normal local
movement input and sprint state; AutoTool selects a safe local hotbar tool only
while the user is already mining; and FastPlace can lower Minecraft's transient
local use cooldown only while the user holds Use. AutoEat selects existing safe
hotbar food and holds Minecraft's ordinary Use key only while its local
threshold and combat rules permit it.

AutoArmor, AutoEject, AutoTotem, and InventoryManager act only in the player's
open vanilla inventory with an empty carried cursor, using Minecraft's normal
container interactions. ChestSteal similarly runs only in an open vanilla
chest and uses normal quick-move actions. Their local plans preserve configured
hotbar slots, named/enchant-protected items, durability reserves, and Binding
Curse equipment where applicable; Minecraft and the server remain authoritative
over every attempted action.

AutoFish observes the local fishing-hook state, waits for a configured bite
delay, uses the selected player-provided rod normally to reel, and recasts once
after the configured delay. AutoReconnect displays a cancellable local
countdown after a multiplayer disconnect and asks Minecraft's normal connection
screen to retry the same remembered server for a bounded number of attempts;
it declines local/singleplayer targets and a disconnect that never reaches the
ordinary disconnect screen. BuilderAssist previews small loaded replaceable
line/floor/wall plans while a player-provided block is held and sends at most
one ordinary Use interaction at its configured cadence. Packet manipulation,
external networking, telemetry, a custom backend, and a server-side component
remain unimplemented.

Combat tools are deliberately constrained to Minecraft's ordinary client
paths. TriggerBot and KillAura make at most one normal locally observed, line-of-sight
attack request per client tick; CriticalAssist acts only while the user holds
Attack during an ordinary falling critical window. BowAimAssist smooths only
the local view while the user holds a bow and draws a local target outline; it
never fires the bow. AutoPotion uses only player-owned, locally identified
restorative hotbar potions, then restores a slot it still owns. AntiBot uses
local tab-list/profile/age/name/invisibility heuristics only. TargetHUD and
ReachDisplay render local observed facts; ReachDisplay reports distances for
Helikon's own ordinary attack requests and never claims modified reach. KillAura
also uses a configurable bounded local rotation adjustment before its normal
attack request.

The advanced movement modules remain deliberately conservative. NoSlow changes
only selected local vanilla slowdown calculations; FastLadders, Step, Speed,
and BunnyHop use bounded local input/collision/velocity choices with no
anti-cheat-named modes. Flight and NoFall enable only Minecraft-granted flight
abilities, while Flight's optional freecam view is a local invisible camera
that suppresses player movement keys and never moves the player. ExtraElytra adds local pitch/speed/durability
assistance. Scaffold uses player-provided hotbar blocks and normal Use
interactions only while Use is held. Timer is constrained to 0.5×–1.25×,
resets on world leave, and cannot make a multiplayer server advance faster.

Trajectories predicts only frustum-visible in-flight arrows, tridents, and
thrown items through a local block-collision simulation; it does not alter
projectile physics or generate an aiming preview. TrueSight makes selected
invisible entities locatable with transparent local boxes, without changing
vanilla model rendering. Radar is a fixed-position local HUD for selected
nearby entities. None of these features sends data or changes the server game.

XRay rebuilds only local compiled chunk geometry so non-selected blocks are
absent and configured targets remain locally visible; toggling it or changing
its block list/opacity rebuilds the geometry again, including a full restoration
on disable. StorageESP uses a separate bounded loaded-chunk scan for selected
block entities and draws only frustum-visible local boxes. Neither feature
requests chunks, modifies blocks, or sends packets.

MiniPlayer renders the current local player model in a fixed HUD panel with
local rotation, scale, armor, and background controls. DamageIndicators tracks
only observed nearby local health decreases while the target has a current
hurt indication, then draws a bounded fading/rising amount. Neither changes
entity health, combat, packets, or server-side UI.

ChatPrefix and ChatSuffix can format only ordinary outgoing chat, with explicit
guards for Helikon commands, slash commands, private messages, and likely
authentication commands. They use the player's normal server chat connection;
no messages are relayed through a Helikon service.

ChatMute and ChatFilter apply only local incoming-message decisions. They can
hide structured vanilla message categories or bounded keyword/player/regex
matches from the local HUD without affecting what a server sends to anyone
else.

ChatSpammer is deliberately constrained: it accepts ordinary local text only,
waits at least two seconds between sends, pauses in screens by default, stops
after disconnect, and has a small per-session cap. Servers may still punish
spam, so it is off by default.

MentionNotifier and AutoReply are off by default. MentionNotifier watches
ordinary incoming player chat for the local player's name or configured local
terms and posts an in-game Helikon notice. AutoReply evaluates one configured
rule only, ignores the local player's own messages, pauses in screens by
default, bounds replies per minute, and sends only safe ordinary chat through
Minecraft's normal connection. It never sends commands or retries a failure.

AntiSpam is also off by default. Its local policy can hide repeated or rapid
incoming messages, collapse bursts of same-type join/leave notices, and exempt
message categories. It has no server effect and never changes protocol traffic.
The first slice records duplicate counts for a later display-level stacker; it
does not yet replace shown chat lines with a visible counter.

ChatTimestamps is off by default and prepends a locally rendered timestamp to
incoming player and server-system chat lines. It supports 12/24-hour time,
seconds, brackets, a local color, and a session-relative label mode. The
original message component is preserved; no timestamp is transmitted to a
server. Minecraft logs the original message before Helikon decorates the
locally stored display line.

ChatColor is off by default and applies a configurable local palette to
displayed player, server-system, mention, and conservatively recognized
private-message lines. It also controls the timestamp-label color, standard
vanilla `<player>` span color, background-opacity multiplier, and text shadow.
It rebuilds only local display components after Minecraft logs the original
message; custom server chat formats retain their own structured name styling.

BetterChat is off by default and extends only the local chat display: bounded
expanded history, consecutive duplicate stacking and `[xN]` counters, standard
vanilla player names that suggest (but never send) a `/msg <name>` command,
longer visibility with an adjustable fade, compact line height, and eased
multi-line scrolling. Its `.chat` command searches, lists, or explicitly copies
currently retained local lines; no history is persisted.

PrivateMessageHelper intercepts `.pm` and `.reply` locally, validates a player
name and a bounded message, then uses Minecraft's normal server-command route
with configurable `msg` and `r` command tokens. The `.` command itself is
never sent. Recent outgoing conversation tabs stay in memory only; Helikon
does not inspect, relay, or persist private-message content.

The ClickGUI currently provides:

- a category sidebar driven by `ModuleCategory`
- a scrollable module list with per-module toggles
- search across module names, IDs, and descriptions
- a settings panel with metadata plus editable boolean, number, and ARGB color settings
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

To validate the release candidate and create the locally auditable release
bundle (JAR, SHA-256 checksums, resolved-dependency report, release notes, and
the relevant documentation), run:

```powershell
.\gradlew.bat check releaseBundle
```

The bundle is written to `build/releases`. See [release.md](docs/release.md)
for the release gate and [security-review.md](docs/security-review.md) for its
security review scope.

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
Select **ChatColor** in the Chat category to edit its `#AARRGGBB` local
palette, opacity multiplier, and text-shadow setting. The module never changes
outgoing chat or server-visible formatting.

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
| `.bind <module> <key> [toggle\|hold\|press_once]` | Binds a key to a module. |
| `.unbind <module>` | Removes a module's keybind. |
| `.gui` | Opens the ClickGUI. |
| `.profile list` | Lists saved local profiles. |
| `.profile save\|load\|delete <name>` | Saves, loads, or deletes a local module/ClickGUI snapshot. |
| `.profile duplicate\|rename <from> <to>` | Creates a named copy or renames a local profile. |
| `.profile default <name\|clear>` | Sets or clears the locally persisted default profile. |
| `.profile server <address> <profile\|clear>` | Associates a local profile with a server address. |
| `.profile world <id> <profile\|clear>` | Associates a local profile with a singleplayer world ID. |
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
| `.panic` | Disables modules, hides custom HUD for this session, and closes Helikon GUI screens. |
| `.panic bind <key>` / `.panic unbind` | Configures or clears the local persisted panic key. |
| `.panic status` / `.panic restorehud` | Shows the bind or restores HUD visibility without re-enabling modules. |

Key names follow Minecraft's keyboard names, for example `r`, `f6`, or
`right.shift`. Module keybinds never fire while any screen is open, so typing
into chat or a search box cannot toggle modules. Tab completion is not
implemented yet. Profile names are local, lowercase file-safe tokens (letters,
digits, `_`, and `-`), and no profile data is synchronized or sent to a server.
Friend names and colors are also stored locally only in `friends.json`.
Middle-clicking a player in the world toggles that local friend entry; this
does not consume or alter normal middle-click actions on blocks or items.
Waypoints are scoped locally to the current server/world and dimension; their
nearest enabled entries appear as a small direction-and-distance HUD list.
Waypoint names, coordinates, world/server scope, dimension, colors, icons, and
enabled state are stored locally only in `waypoints.json`.
Macros are stored locally in `macros.json` and run only explicitly configured
local Helikon commands, ordinary chat messages, Minecraft commands, and bounded
delays. There is no macro scripting or arbitrary code execution.
PrivateMessageHelper's command-token settings are stored with the other module
settings in `global.json`; its recent message text is deliberately session-only
memory and is discarded on client exit.
The optional panic key is stored locally in `panic.json`; it is suppressed while
typing in chat or ordinary screens, but works inside Helikon screens to close
them immediately.

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

## Release candidate status

`1.0.0-rc.1` is a client-only release candidate for Minecraft 26.2. The live
client smoke checklists in [testing.md](docs/testing.md) remain a release gate;
the automated suite and local release-bundle checks do not replace real-world
testing on a disposable profile.

## Development

Read [architecture.md](docs/architecture.md), [configuration.md](docs/configuration.md),
and [testing.md](docs/testing.md) before changing core systems. The long-term
roadmap and policies live in [PLAN.md](PLAN.md). Contributions
should include tests and keep all behavior local-only unless an explicitly
documented optional integration is approved.

## License

Helikon is released under the [MIT License](LICENSE).
