# Configuration

The bootstrap stores only global module state at:

```text
.minecraft/config/helikon/global.json
```

The schema currently contains `schemaVersion` and one entry per registered
module. Each module stores its enabled state, its keybind (`key` code and
`activation` of `toggle`, `hold`, or `press_once`), and setting values. A
missing keybind entry keeps the module's default; an invalid one resets to
unbound and is logged. The same local file optionally stores a `clickGui`
block with its saved top-left window position. Missing or invalid GUI layout
data safely falls back to centered placement. The same block stores a custom
window width and height when the user has resized it; invalid dimensions reset
to the default size without affecting valid module data.
It also stores the selected ClickGUI palette (`midnight`, `high_contrast`, or
`ocean`). Unknown or malformed palette values safely fall back to Midnight.

The Active Modules HUD has its own schema-versioned `hud.json` in the same
directory. It stores only the element's enabled state and top-left scaled-GUI
coordinates. The HUD editor saves it when closed; normal client shutdown also
saves it. Its writes use `hud.json.bak` and move malformed files to
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

Configuration is written when the ClickGUI closes and on normal client
shutdown — never per frame or per tick. Writes use a temporary file followed
by atomic replacement when available. The previously saved valid file is copied to `global.json.bak`.
Malformed files are moved to `global.corrupt-<timestamp>.json`; Helikon then
uses safe defaults. No configuration is synchronized remotely.

Friends, waypoints, macros, remaining HUD layout, and server-specific data are
planned future local stores. Imported data must be validated before activation
and must never execute code or write outside the Helikon configuration folder.
