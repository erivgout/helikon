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

## Commands

`CommandDispatcher` owns the `.` prefix. A Fabric `ALLOW_CHAT` hook
(`ChatCommands`) routes every prefixed outgoing chat message to the dispatcher
and cancels it, so command attempts — including typos — are never sent to the
server. Each command is a small `HelikonCommand` implementation registered in
`HelikonCommands`; command failures are caught, reported as feedback, and
logged. The command layer has no Minecraft imports (key names resolve through
the `KeyNameResolver` interface; `MinecraftKeyNameResolver` is the only
Minecraft-facing piece), so dispatcher and command behavior is unit-tested
directly. Responses go through `CommandFeedback`, implemented in game by
`ChatNotifier`.

`.gui` cannot open a screen while the chat screen is still closing, so the
entrypoint queues the screen change and applies it on the next tick once no
screen is open.

## Module keybinds

`KeybindManager` polls bound module keys once per client tick and applies the
`Keybind.Activation` mode (toggle on press edge, hold enables while down,
press_once only enables). All transitions go through `ModuleRegistry`. While
any screen is open, keybind actions are suppressed and HOLD modules release;
physical key state keeps being tracked so a key held across the end of
suppression does not count as a fresh press. The key source is an injected
`KeyStateReader`, keeping the edge/hold logic unit-testable. Keybinds are
assigned with `.bind`/`.unbind` and persist in `global.json`.

## Notifications

`ChatNotifier` posts local-only messages to the chat HUD (command feedback and
module-failure notices) and falls back to the log before a player exists.
Nothing is ever sent to the server.

## Settings and configuration

Each setting validates its own values and owns JSON conversion. Invalid values
reset to safe defaults. `ConfigurationManager` writes a schema-versioned,
human-readable `global.json` using a temporary file and atomic replacement when
the filesystem supports it. The previous good file is copied to `global.json.bak`.
Malformed JSON is retained as `global.corrupt-<timestamp>.json` for inspection.

## Rendering, GUI, and HUD

The Right Shift keybind opens `HelikonClickGuiScreen`, a vanilla `Screen`
subclass that uses only supported Minecraft/Fabric GUI APIs (`EditBox`
widgets, `GuiGraphicsExtractor` fills/text/scissor). The screen is a thin
view layer:

- `ClickGuiState` holds the selected category, search query, and selected
  module. It has no Minecraft imports, so filtering and selection rules are
  unit-tested directly. Search matches module name, ID, and description
  case-insensitively; a non-blank query spans all categories.
- `NumberSettingField` owns the text-to-value rules for number edit fields:
  input is applied only when it parses to a finite value inside the setting
  range, otherwise the current value is kept and the field is marked invalid.
- All enable/disable transitions go through `ModuleRegistry`, so lifecycle
  failure isolation applies to GUI toggles exactly as it does everywhere else.
  Boolean and number settings are edited through the settings' own validated
  `set` path.
- The screen saves the registry through `ConfigurationManager` once, when the
  screen closes (`removed()`), never per frame. Shutdown still saves via the
  existing `CLIENT_STOPPING` hook.
- `KeybindAssignment`, `ClickGuiWindowState`, and `ClickGuiWindowDragState`
  are Minecraft-free. They respectively validate in-GUI key capture against
  the existing GUI-key reservation rule, model saved window placement, and
  calculate pointer-offset drag clamping. The screen only forwards 26.2 input
  objects and moves existing vanilla `EditBox` widgets with the panel.
- GUI setting/module reset controls use `Setting.reset()` and
  `Module.resetSettings()`. Module lifecycle changes remain exclusively routed
  through `ModuleRegistry`.

Keyboard safety: the GUI keybind only opens the screen when no other screen is
active, and while the ClickGUI is open Minecraft routes key input to the
focused widget (search box or a number field) rather than to game keybinds.
While a ClickGUI key-capture row is active, it consumes the next keyboard
token before widgets do: Escape cancels, Backspace/Delete unbinds, and the
reserved GUI key is rejected. The module keybind dispatcher continues to
ignore input while any screen is open.

`ActiveModulesHud` is registered through Fabric's supported
`HudElementRegistry` API and only renders enabled modules. `ActiveModules`
contains the Minecraft-free selection rule, while `HudLayout` stores the
validated persisted position and enabled state. `HudEditorState` owns pointer
offsets and bounds clamping without Minecraft imports; `HelikonHudEditorScreen`
is only the input/render adapter. The editor is reached from the **HUD** button
in the ClickGUI header, saves once when it closes, and returns to the ClickGUI.

The initial editor intentionally has one draggable element. Alignment, scale,
background options, snapping, and other HUD elements remain future work.
