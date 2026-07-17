# Configuration

The bootstrap stores only global module state at:

```text
.minecraft/config/helikon/global.json
```

The schema currently contains `schemaVersion` and one entry per registered
module. Each module stores its enabled state, its keybind (`inputType`, `key`
code, optional `modifiers`, and `activation` of `toggle`, `hold`, or
`press_once`), and setting values. A
missing keybind entry keeps the module's default; an invalid one resets to
unbound and is logged. The same local file optionally stores a `clickGui`
block with its saved top-left window position. Missing or invalid GUI layout
data safely falls back to centered placement. The same block stores a custom
window width and height when the user has resized it; invalid dimensions reset
to the default size without affecting valid module data.
It also stores the selected ClickGUI palette (`midnight`, `high_contrast`, or
`ocean`), a validated 0.75x–1.50x interface scale, and the reduced-animation
preference. Unknown or malformed palette/scale values safely fall back to
Midnight and 1.0x respectively.
The former bootstrap ID `fullbright_stub` is migrated locally to the production
`fullbright` module on load, preserving its enabled state, keybind, and shared
settings before the next normal save writes the production ID.
The migration suite verifies both directions of this transition: legacy state
loads into the production module, and its subsequent atomic save contains only
the production ID.

`ColorSetting` values use strict eight-digit ARGB strings such as `#80FF6600`.
Malformed color values reset only that setting to its safe default and are
logged, following the existing per-setting recovery policy.

`EnumSetting` values use stable lowercase tokens chosen from each setting's
declared finite list. Unknown or malformed tokens reset only that setting to
its safe default and are logged. The ClickGUI cycles an enum setting when its
row is clicked; `.setting` accepts any listed token case-insensitively.

`StringSetting` values are bounded local text. Invalid or overlong values reset
only that setting to its default and are logged. They appear as validated text
boxes in the ClickGUI; `.setting` accepts a single non-space text token. AutoEat
uses one for its bounded comma-separated food identifier avoid list.
LocalTranslator uses one for its bounded local `glossary`, which has no API,
provider, credential, or network configuration.

`IntegerSetting`, `KeybindSetting`, `StringListSetting`, block/item/entity
selector settings, `MultiSelectEnumSetting`, `RangeSetting`, and `RegexSetting`
share the same per-setting recovery path. Integers reject fractional JSON;
keybinds use validated local keyboard-token or mouse-button ranges plus an
optional immutable modifier set; text lists and selectors
have bounded immutable entries; selector IDs are normalized lowercase resource
tokens; enum selections reject unknown or duplicate tokens; ranges require
finite ordered values inside their configured bounds; and regex settings reject
syntax errors, backreferences, lookarounds, and quantified groups. The
ClickGUI and `.setting` use the same validated compact syntax for these types,
so invalid input leaves the last valid local value unchanged.

The Active Modules HUD has its own schema-versioned `hud.json` in the same
directory. It stores enabled state, top-left scaled-GUI coordinates, scale,
padding, backdrop/text-shadow choices, sort/alignment/color modes. Schema 2
loads the earlier position-only schema 1 safely with presentation defaults.
Schema 3 also persists enabled state plus anchored offsets for each editable
HUD element (waypoints, coordinates, saturation, Elytra, Target HUD, reach,
Inventory Preview, durability warnings, Radar, MiniPlayer, Debug Overlay, and
Better Crosshair, plus opt-in Direction, FPS, Ping, local TPS estimate, Speed,
Armor/held-item durability, Potion effects, Clock, Biome, Server address, and
Totem count); older files retain their safe built-in placements. Schema 4 adds
per-element scale, alignment, background, padding, text shadow, ARGB color,
and rainbow-mode preferences. Missing or invalid presentation values safely
use the element defaults.
The HUD editor saves it when closed; normal client shutdown also saves it. Its
writes use `hud.json.bak` and move malformed files to
`hud.corrupt-<timestamp>.json`. Invalid individual HUD values are logged and
reset to safe defaults.

Named local profiles live in `profiles/<name>.json`. Each is a schema-versioned
snapshot of module state, settings, keybinds, and ClickGUI layout. Names are
limited to lowercase letters, digits, `_`, and `-` to prevent directory
traversal. Saving creates `<name>.json.bak` before replacement; malformed
profiles become `<name>.corrupt-<timestamp>.json` and are not activated.
Missing module blocks, settings, or keybinds in an otherwise valid profile
restore those values to their module defaults. Profiles that cannot be read are
left in place rather than being treated as corrupt.
Profiles can be duplicated or renamed locally; both operations rewrite the
stored profile name before the new file is activated later, so a renamed file
cannot fail validation due to a stale embedded name. Renaming also transfers
the source profile's `.bak` file to the new name. The profile schema is
validated before either operation writes a new file.
To import, place `<file>.json` in `config/helikon/imports/` and run
`.profile import <file> <name>`; validated imports are copied into `profiles/`.
`.profile export <name> <file>` writes atomically to
`config/helikon/exports/<file>.json` (with a `.bak` on replacement). Commands
accept only safe file tokens, so chat input cannot read or write arbitrary
paths.
`profiles.json` stores the optional default profile with the same atomic write,
backup, and corrupt-file recovery rules. Renaming or deleting the default
updates or clears that local preference. The same manifest stores optional
server-address and singleplayer-world associations. The default profile applies
after global configuration at startup; on a matching world join, a scoped
association overrides it. Addresses are normalized case-insensitively; world
identifiers are trimmed but otherwise case-preserved. Renaming/deleting a
profile updates or clears every association that points to it.

Configuration is written when the ClickGUI closes and on normal client
shutdown — never per frame or per tick. Writes use a temporary file followed
by atomic replacement when available. The previously saved valid file is copied to `global.json.bak`.
Malformed files are moved to `global.corrupt-<timestamp>.json`; Helikon then
uses safe defaults. No configuration is synchronized remotely.

Friends live in schema-versioned `friends.json`, storing validated player names
and local ARGB render colors. Friend writes use `friends.json.bak`; malformed
files become `friends.corrupt-<timestamp>.json`. Nothing in this store is sent
to Minecraft servers or an external service. Case-insensitive duplicate names
are invalid persisted data and recover through the same corrupt-file path.

Waypoints live in schema-versioned `waypoints.json`. Every entry has a
validated name, coordinates, local `server:` or `world:` scope, dimension,
ARGB color, optional icon token, enabled state, and creation timestamp. Names
only need to be unique case-insensitively within the same scope and dimension,
so `Home` can exist separately in the Overworld and Nether. Writes use
`waypoints.json.bak`; malformed, duplicate, or out-of-policy entries become
`waypoints.corrupt-<timestamp>.json`. Waypoints are never synchronized or sent
to a Minecraft server. Command mutations roll back in memory if their atomic
save fails, so a failed add or edit is never later persisted unexpectedly.

Macros live in schema-versioned `macros.json`. A macro has a safe local name,
an optional normalized multiplayer-server restriction, and up to 64 explicit
actions: a Helikon local command, ordinary chat message, Minecraft command, or
1-12,000 tick delay. Text is validated and bounded; no scripts, filesystem
paths, downloaded content, or arbitrary code are accepted. Writes use
`macros.json.bak`, malformed files become `macros.corrupt-<timestamp>.json`,
and a command mutation rolls back if saving cannot complete. Macro definitions
are never sent to a server; configured chat and command actions use only the
player's existing normal Minecraft connection.

ChatHistory creates no log file unless both the module and its
`persistent_logging` setting are enabled. Opted-in records are bounded by
`history_limit` and `retention_days`, and are written only on module disable,
server-scope change, disconnect, or client shutdown — never per chat line,
frame, or tick. Each schema-versioned record is stored under
`chat-history/<opaque-server-hash>.json`; the filename does not expose the
server address. Replacements first create a `.bak` copy and use an atomic move
when available. A malformed per-server file becomes
`<opaque-server-hash>.corrupt-<timestamp>.json` and is ignored. Only accepted
non-overlay incoming lines and accepted ordinary outgoing chat can enter the
store; local `.` commands are neither sent nor retained.

`panic.json` stores a schema-versioned keyboard or mouse input plus optional
modifiers for the local panic bind. It has the same atomic write, `.bak`, corrupt-file recovery,
invalid-key fallback rules as other local stores; the reserved Helikon GUI key
is invalid even when manually written to the file. Panic activation does not
rewrite `global.json`, `hud.json`, or module settings: HUD hiding is transient
for the current session and can be restored with `.panic restorehud`.
