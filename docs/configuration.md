# Configuration

The bootstrap stores only global module state at:

```text
.minecraft/config/helikon/global.json
```

The schema currently contains `schemaVersion` and one entry per registered
module. Each module stores its enabled state and setting values.

Configuration writes use a temporary file followed by atomic replacement when
available. The previously saved valid file is copied to `global.json.bak`.
Malformed files are moved to `global.corrupt-<timestamp>.json`; Helikon then
uses safe defaults. No configuration is synchronized remotely.

Profiles, friends, waypoints, macros, HUD layout, and server-specific data are
planned future local stores. Imported data must be validated before activation
and must never execute code or write outside the Helikon configuration folder.
