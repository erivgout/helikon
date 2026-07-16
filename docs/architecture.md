# Architecture

## Bootstrap scope

The current milestone is a client-only Fabric mod. `fabric.mod.json` declares
only `dev.helikon.client.HelikonClient` as a `client` entrypoint; the project has
no server entrypoint and requires no server plugin.

## Modules

`Module` owns immutable metadata, a stable lowercase ID, settings, a local
keybind description, and the enable/disable callbacks. `ModuleRegistry` is the
only normal lifecycle dispatch boundary. It catches unexpected runtime failures,
attempts module cleanup, logs the stack trace, and reports the failure through
registered handlers. A failed module must not crash the rest of the client.

Modules should not directly depend on configuration or GUI classes. Minecraft
version-specific hooks belong in adapters or event bridges, not scattered across
module business logic.

## Events

`EventBus` uses explicit subscriptions by event type. It performs no reflection
or classpath scanning, including during ticks and renders. Fabric lifecycle
events are bridged into `ClientTickEvent` during the bootstrap.

## Settings and configuration

Each setting validates its own values and owns JSON conversion. Invalid values
reset to safe defaults. `ConfigurationManager` writes a schema-versioned,
human-readable `global.json` using a temporary file and atomic replacement when
the filesystem supports it. The previous good file is copied to `global.json.bak`.
Malformed JSON is retained as `global.corrupt-<timestamp>.json` for inspection.

## Rendering and GUI

The current Right Shift screen is intentionally a placeholder. The future
ClickGUI and HUD editor must use supported Minecraft/Fabric rendering APIs,
restore render state, and remain separate from module business logic.
