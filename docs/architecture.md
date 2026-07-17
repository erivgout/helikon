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

`Fullbright` follows this split: `FullbrightGammaController` owns the
Minecraft-free capture, override, and restoration decision, while
`MinecraftGammaAccess` and `MinecraftNightVisionAccess` are the small 26.2 API
adapters. The Night Vision adapter keeps an effect snapshot per local player,
tracks its exact injected effect instance, reasserts only that visual effect,
and restores the snapshot on disable. Periodic module callbacks use
`ModuleRegistry.runGuarded`, so adapter failures also disable and clean up the
affected module through normal lifecycle isolation.

AntiBlind uses three narrowly targeted 26.2 client mixins: fog-environment
selection for Blindness/Darkness, the Darkness lightmap blend, and the exact
HUD calls for Nausea, pumpkin, and powder-snow overlays. BetterCrosshair is a
Fabric HUD element with Minecraft-free arm geometry; a separate HUD mixin
suppresses the vanilla crosshair only when its local setting requests it.

AutoWalk uses one narrow `KeyboardInput.tick` tail mixin after Minecraft has
freshly polled the user's physical keys. Its transformation is a tested,
Minecraft-free `MovementInput` policy; it updates both the input flags and the
matching normalized movement vector every tick, so it does not leave a key
mapping pressed after disable. AutoSprint's input,
hunger, and collision decision is likewise Minecraft-free; a small client-tick
adapter applies only its reversible normal `LocalPlayer.setSprinting` request.
It releases only sprint state that it previously requested and never constructs
or alters a packet.

AutoSneak shares the same fresh-input bridge. Its mode policy is Minecraft-free:
**Toggle** holds local sneak while enabled, **Hold** uses only its configured
module key, and **Edge-only** holds local sneak while moving so Minecraft's
normal careful-movement handling guards ledges. The bridge is always inactive
while a screen is open, and it recomputes the effective movement vector after
the final input transformation.

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

`ProfileManager` stores named module and ClickGUI snapshots below
`config/helikon/profiles/`. It reuses the global configuration codec so profile
activation has the same schema and individual-setting validation. Names are
restricted to safe lowercase file tokens, writes are atomic with per-profile
backups, and a malformed profile is moved to
`<name>.corrupt-<timestamp>.json` before it can activate. `ProfileCommand` is
only local chat wiring around that Minecraft-free store. Duplicate and rename
operations parse and rewrite the embedded profile name before atomically
creating their destination, without activating a profile; they share the
configuration codec's non-mutating schema validation first.
Profile import/export uses fixed local `imports/` and `exports/` directories
below the Helikon configuration root, validates imported schema before copy,
and never accepts an arbitrary chat-supplied filesystem path.
The optional default profile is a separate recoverable `profiles.json`
manifest, which also owns server and singleplayer-world profile associations.
Profile rename/delete keeps every preference reference valid without requiring
Minecraft classes in the storage layer.

`FriendManager` is likewise a Minecraft-free, schema-versioned local store for
validated player names and ARGB render colors. `FriendToggleGesture` contains
the middle-click edge and screen-suppression policy; the client entrypoint is
limited to resolving the targeted player through the 26.2 hit result and
calling the store. Future targeting modules can query `FriendManager.contains`
to exclude friends by default without coupling their decision logic to input or
JSON handling.

`WaypointManager` follows the same atomic local-storage rules for
`waypoints.json`. `WaypointContext`, `WaypointLocation`, and
`WaypointNavigation` own validation, context filtering, distance ordering, and
compass labels without Minecraft imports. `MinecraftWaypointLocationProvider`
is the small 26.2 adapter that derives the current server/world directory,
dimension identifier, and block position. `WaypointHud` only renders the
nearest enabled entries for that current context; it caches a bounded nearest
set until the player position, context, or waypoint revision changes. It
neither sends waypoint data nor accesses storage during rendering.

`MacroManager` is another schema-versioned local store. `MacroAction` has a
closed validated action set, and `MacroRunner` schedules at most one action per
client tick, honors bounded delay actions, stops server-scoped macros when the
connection changes, and contains executor failures. The Minecraft action
adapter is deliberately narrow: local actions dispatch through
`CommandDispatcher`, while chat and command actions use the normal 26.2 client
connection only after a user explicitly runs that stored macro. It cannot load
scripts or execute arbitrary code.

`PanicController` is a Minecraft-free coordinator. It always uses
`ModuleRegistry.disableAll`, so each module's normal disable cleanup restores
the client state it owns; it also cancels temporary macro work and hides custom
HUD without changing persisted layout. A thin client callback closes only
Helikon-owned GUI screens. `PanicKeybindManager` handles its own press edge and
is screen-safe: normal screens suppress it, while Helikon screens permit it so
the key can close the ClickGUI. Its recoverable local persistence is isolated
in `PanicConfigurationManager`.

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
- `ClickGuiWindowResizeState` owns the bottom-right drag-handle math. It keeps
  the panel's top-left fixed, clamps dimensions to the usable viewport, and
  relies on the same validated, persisted `ClickGuiWindowState` as movement.
- `ClickGuiTheme` is a closed local palette list, while `ClickGuiWindowState`
  persists the selected ID with the GUI layout. `HelikonThemeEditorScreen`
  only selects a palette and saves once when it closes; no remote themes,
  downloaded assets, or external requests are used.
- `ClickGuiState` also owns category and visible-module keyboard selection,
  including wraparound and clearing an invalid selection. The screen maps
  arrow, Enter, and Space keys only when no text field is focused, then routes
  toggles through `ModuleRegistry` and keeps the selected row in view.
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
