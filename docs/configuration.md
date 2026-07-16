# Configuration

The bootstrap stores only global module state at:

```text
.minecraft/config/helikon/global.json
```

The schema currently contains `schemaVersion` and one entry per registered
module. Each module stores its enabled state, its keybind (`key` code and
`activation` of `toggle`, `hold`, or `press_once`), and setting values. A
missing keybind entry keeps the module's default; an invalid one resets to
unbound and is logged.

Configuration is written when the ClickGUI closes and on normal client
shutdown — never per frame or per tick. Writes use a temporary file followed
by atomic replacement when available. The previously saved valid file is copied to `global.json.bak`.
Malformed files are moved to `global.corrupt-<timestamp>.json`; Helikon then
uses safe defaults. No configuration is synchronized remotely.

Profiles, friends, waypoints, macros, HUD layout, and server-specific data are
planned future local stores. Imported data must be validated before activation
and must never execute code or write outside the Helikon configuration folder.
