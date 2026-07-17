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
AntiTotemAnimation uses one separate `GameRenderer.displayItemActivation`
head mixin. It cancels only an item activation with the verified
`DEATH_PROTECTION` component while the module is enabled; the client still
handles the totem event, particles, sound, and every normal server packet.
Dinnerbone uses the verified `LivingEntityRenderer.isEntityUpsideDown` return
value. Its narrow mixin only changes a false vanilla result to true for the
selected local Player, `Monster`, or other living-entity category, so the
normal named-entity behavior is preserved. RainbowEnchant wraps the verified
`ItemFeatureRenderer.getFoilBuffer` return value only while enabled. The wrapper
delegates every vertex attribute and replaces only the ARGB vertex color for
item-stack foil rendering; it does not change item data, textures, or the
separate worn armor-layer glint path.

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

Twerk uses that same fresh-input bridge after AutoSneak. Its small
Minecraft-free state machine alternates a bounded local sneak pulse and always
resets on a screen, disable, or panic, so it cannot leave a pressed mapping
behind. The bridge combines policies only for a fresh vanilla input snapshot.

AutoParkour's screen/ground/forward/speed/ledge/lava/drop policy is likewise
Minecraft-free. Its narrow keyboard adapter samples only loaded blocks at the
cardinal forward position, declines an absent, lava, obstructed, non-sturdy,
or greater-than-two-block landing, and requests only the normal local Jump
input. It does not call a movement method or create a packet. InventoryWalk is another input
policy: the adapter reads configured physical **keyboard** movement bindings
only for `InventoryScreen`, preserves Shift for inventory use, and declines a
focused child widget so typing cannot move the player. AntiAFK keeps its idle
timer and selected action policy Minecraft-free; the same adapter only applies
its bounded local yaw change and ordinary one-tick input requests after the
configured idle interval, never while a screen is open or the user is moving.

WaterJump uses the same narrow `KeyboardInput` adapter, but keeps its
screen/water/forward/solid-step/headroom decision in the Minecraft-free
`WaterJumpContext` policy. The adapter reads only three already-loaded blocks
in the local player's cardinal facing direction; it declines unknown chunks,
an obstructed two-block exit, or any open screen, then can request only
Minecraft's ordinary local Jump input. It does not set position or velocity,
interact with a block, or construct a packet.

Fish keeps its contextual eligibility and bounded input-to-velocity policy
Minecraft-free. The advanced-movement adapter reads only the local player's
existing water, movement-input, flight, and velocity facts, then applies the
returned local velocity without changing fluid state or constructing a packet.

AutoTool separates hotbar candidates and deterministic scoring from the small
Minecraft adapter. The adapter runs only while the user is normally mining a
block through Minecraft's own game mode, selects an existing hotbar slot, and
restores a previous slot only when it still owns the selection. It never sends
its own mining packet or modifies inventory contents.

FastPlace has a similarly small adapter around Minecraft's transient use-delay
field. Its pure policy only lowers an existing cooldown after the user holds
Use with a matching non-empty item; it never synthesizes a click, interaction,
or packet. Its narrow port immediately restores an unchanged module-owned
cooldown on disable or panic. The accessor remains confined to the
version-sensitive client edge.

FastBreak follows the same ownership model for Minecraft's transient normal
destroy cooldown, while keeping its held-Attack/visible-target/block-filter
decision Minecraft-free. `Nuker` keeps the whitelist, blacklist, range,
line-of-sight, hard two-request cap, and temporary tool-slot ownership in pure
classes. Its adapter scans only the small loaded cube around the local player,
uses Minecraft's actual interaction-range and optional block ray facts, then
calls only `MultiPlayerGameMode.startDestroyBlock`. It never creates a packet
or edits world state directly; server validation and correction remain in
control. Optional local view rotation and hotbar selection use existing client
state only.

`BaritoneCompatibility` is a pure injected-predicate detector for the optional
user-installed `baritone` Fabric mod ID. The entrypoint supplies Fabric's local
mod lookup and logs the status. It makes no class reference to Baritone, uses
no reflection, download, external service, or integration call, so Helikon
continues unchanged whether that mod is absent or present.

`CactusCollisionPolicy` owns finite-box intersection and deterministic
horizontal-slide decisions without Minecraft imports. Its narrow adapter scans
at most 64 already-loaded cactus collision boxes for only the local player's
ordinary `MoverType.SELF` movement. It declines world-driven, spectator,
passenger, large, or unknown-chunk movement and does not cancel cactus damage
or change server authority.

AutoEat keeps food ranking, avoid-list parsing, threshold checks, combat
interruption, and user-slot ownership in Minecraft-free classes. Its adapter
only exposes existing hotbar food facts and ordinary local Use-key state. It
does not directly invoke an interaction, create an item, or construct a
packet; the normal client interaction path and connected server remain in
control. When AutoEat ends its own use hold, the input port separately polls
the configured keyboard, mouse, or scancode binding so an overlapping physical
player hold is never cancelled.

Advanced movement keeps category gates, bounded velocity math, water-edge jump
gates, step height, ability-flight ownership, Elytra pitch/status, scaffold
target selection, and timer-rate limits in Minecraft-free modules. The
version-sensitive edge is
narrow: NoSlow/Step/Timer mixins target verified local movement and
`DeltaTracker` calculations; the controller adapts current local input,
velocity, abilities, and held blocks to ordinary Minecraft APIs. No module
constructs movement/container packets or treats client motion as server truth.

Flight changes only `Abilities` that already permit flight and restores only
values it owns. The independent Freecam module uses an invisible, unadded
client-side camera entity; input and mouse turn are redirected locally while
the player receives no movement input, then the normal camera is restored.
Scaffold is similarly constrained to a selected player-provided hotbar block,
a loaded replaceable target with local support, an existing vanilla use
cooldown, and one normal held-block interaction.

Combat uses `CombatTarget`, `CombatTargetFilter`, `CombatAim`, and
`AutoPotion`'s candidate/action policy without Minecraft types. The combat
adapter collects only rendered living entities and local tab-list/profile facts,
then invokes Minecraft's normal `attack` or held-item `useItem` method after
the corresponding module policy permits it. A shared per-tick guard permits at
most one of TriggerBot, CriticalAssist, or KillAura to begin attacks. KillAura's
Multi mode may issue a bounded group of ordinary attacks within that one cycle.
KillAura/TriggerBot/CriticalAssist require the locally observed
line-of-sight fact, so the adapter never requests attacks through solid blocks.
BowAimAssist only updates the local view while the user holds a normal bow; it
does not fire it. AntiBot is an in-memory heuristic filter, not a remote lookup.
TargetHUD and ReachDisplay render session-only observed data; the latter records
only Helikon's normal attack request distance and does not imply server reach.
KillAura applies the same bounded local rotation policy before its normal attack
cycle. TargetHUD retains the last crosshair or Helikon attack target while it
remains in the current locally rendered target set, then clears it on absence or
world loss; it does not flash a non-crosshair target for only one frame.

The inventory-automation modules keep armor ranking, item-ID/slot parsing,
totem restore conditions, chest priority, and conservative manager choices in
Minecraft-free classes. `MinecraftContainerClicker` is the sole narrow adapter:
only an open corresponding vanilla menu with an empty carried cursor can map a
validated plan to Minecraft's ordinary container-input operation. It does not
edit an inventory, construct a packet, or retain item contents. AutoArmor,
AutoEject, AutoTotem, and InventoryManager require the player's inventory;
ChestSteal requires a vanilla chest menu.

AutoFish keeps its bite/recast state machine and durability gate
Minecraft-free. The client edge reads the current selected rod and the local
fishing-hook's verified bite/open-water state through one accessor, then calls
Minecraft's normal held-item Use method only for the state machine's single
action. AutoReconnect likewise owns a local countdown/attempt policy; its
screen is a cancellable local view and the adapter uses Minecraft's regular
disconnect/connect screens for a remembered valid multiplayer target.

`BuilderPlan` and `BuilderAssist` generate bounded deterministic block plans
without Minecraft imports. The version-sensitive builder adapter converts the
current local block hit, player yaw, loaded replaceable positions, and adjacent
supports into one ordinary held-block interaction. The same adapter supplies
only loaded replaceable preview positions to the supported Gizmo renderer.

Outgoing chat formatting is a Minecraft-free policy layer. ChatPrefix and
ChatSuffix share a conservative guard that preserves local commands, slash
commands, private-message commands, and likely authentication commands
verbatim. The Fabric adapter receives only normal outgoing chat and passes its
result to Minecraft's existing chat path; it never creates a new connection or
retries a message. It also declines formatting that would exceed the verified
26.2 normal-chat packet limit.

Incoming chat filtering first converts Minecraft components into a small record
of local text, sender, channel, overlay flag, and top-level translation key.
ChatMute and ChatFilter then make fully testable local hide decisions. Their
Fabric allow callbacks are guarded through `ModuleRegistry`, so a failed filter
is disabled while the incoming message remains visible rather than disrupting
the client chat stream.

ChatSpammer parses bounded local text into ordinary messages, rejects command
forms, and makes its sequential/random timing decision without Minecraft
types. Its only platform port calls Minecraft's existing normal chat sender;
it does not construct packets, retry rejected messages, or circumvent a
server's own limits. A normal-chat cancellation for the active generated
message is reported back through the Fabric callback; after three observed
cancellations the module stops for the session.

`PrivateMessageCommand` and `PrivateMessageHistory` are likewise
Minecraft-free. They validate the configurable command token, the player-name
target, a bounded single-line message, and the complete command length before
recording a bounded in-memory outgoing entry. `MinecraftServerCommandSender`
is the only 26.2 adapter; it invokes the normal player connection's
`sendCommand` method. This lets the local `.` command be cancelled before any
server traffic while still letting an explicitly requested normal server PM
use the ordinary connection path. The current history is session-only and
does not infer incoming private messages from arbitrary server chat text.

`MentionNotifier` and `AutoReply` evaluate their incoming-message policies in
Minecraft-free modules. The entrypoint first lets the existing mute/filter
policy reject a message; only a locally visible message can then generate a
notice or automatic reply. Mention notifications use the existing local-only
`ChatNotifier`. AutoReply returns an optional already-validated ordinary text
string after applying name allow/deny lists, server restriction, per-sender
cooldown, rolling minute cap, screen pause, safe-regex matching, and a small
recent-own-reply loop guard. Incoming chat keeps both its displayed decorated
text and its signed raw body; AutoReply uses the latter so a normal `<name>`
chat decoration cannot alter exact trigger matching or bypass its loop guard.
The entrypoint alone passes that string to
Minecraft's normal chat sender through `ModuleRegistry.runGuarded`, so a
failure disables only the responsible module and never creates a packet.

`AntiSpam` follows the same Minecraft-free incoming-message model. It bounds
its duplicate identities, sender timestamp queues, and join/leave timestamps;
then returns a typed local show/hide decision plus a repeat count. The client
entrypoint evaluates it after the existing mute/filter policies and before
mention or auto-reply side effects, so a locally suppressed message cannot
produce a notification or outgoing response. A future chat-display adapter can
use the retained repeat count to replace duplicate lines with a counter without
coupling detection to Minecraft rendering.

`ChatTimestampFormat` owns the deterministic 12/24-hour, seconds, brackets,
and session-relative label rules without Minecraft imports. `ChatTimestamps`
contains its validated local settings. A narrow `ChatComponent` mixin modifies
only the verified `GuiMessage` arguments sent to `addMessageToDisplayQueue`
and `addMessageToQueue`, after Minecraft has logged the original content.
`ChatDisplayAccess` prepends a separately styled timestamp component inside an
unstyled wrapper while retaining the original component as a sibling. It cannot
modify a received packet, Minecraft's original chat-log text, or another client.
`ChatColorPolicy` keeps the conservative message-type classification free of
Minecraft imports. When `ChatColor` is enabled, the same display adapter copies
the local component tree to apply a fallback palette and optional no-shadow
style; explicit server colors, click events, and hover events remain attached
to their source components. It colors only the first structured sender argument
of vanilla `chat.type.text`; arbitrary server formats are never flattened to
guess at a player-name span. A verified `ChatComponent.extractRenderState`
local-variable hook multiplies both ordinary-line and prompt background alpha
after the module is enabled. The adapter retains a weak in-memory link to the
uncolored local display component for each current line, so a ChatColor setting
change or disable rebuilds retained chat without changing message packets,
Minecraft's original log text, or timestamp insertion time.

`BetterChatRenderPolicy`, `ChatDuplicateTracker`, `ChatHistorySearch`,
`ChatPlayerNamePolicy`, and `SmoothScrollState` hold its calculations and
bounded state without Minecraft imports. `BetterChatDisplayAccess` is the thin
adapter: verified constant hooks expand the two local chat queues, a verified
alpha-calculator hook changes only unfocused display timing, and standard
`chat.type.text` name components may receive a local suggest-command click
action only when they do not already have one. The stored-message path handles
one consecutive duplicate at a time, removes only the immediately previous
local entry, then asks Minecraft to rebuild its local trimmed display. Search,
history listing, and clipboard copying use explicit `.` commands and a
non-persistent current-display provider; no chat text is written to disk.

`ChatHistory`, `ChatHistoryEntry`, and `ChatHistoryManager` are deliberately
separate from BetterChat's display queue. The manager owns bounded,
Minecraft-free entries and optional schema-versioned per-server storage;
`ChatHistoryCommand` owns local search, explicit clipboard actions, and draft
selection. The client bridge records a non-overlay message only after existing
incoming filters allow it, records ordinary outgoing chat only after Fabric
accepts it, and records no local `.` command. Persistence defaults off and writes only at
lifecycle boundaries with atomic replacement, backup, retention pruning, and
corrupt-file recovery. `MinecraftChatInputReopener` is the sole Minecraft
adapter for reopening an unsent `ChatScreen` draft; the Minecraft-free
`ScheduledChatInputReopener` queues that action for the next client tick so
the local-command callback cannot race normal chat-screen closure.

`Announcer` owns a Minecraft-free trigger policy: every trigger defaults off,
the rendered ordinary-chat template rejects command-like text, and one shared
minimum delay/session cap prevents automatic bursts. `AnnouncerObservationTracker`
turns successive local position, health, and dimension facts into threshold
crossings. `AnnouncerAccess` queues only verified hook observations and drains
them during the existing guarded client tick; it never constructs a packet.
Confirmed kills are deliberately conservative (a direct local melee attempt
followed by that entity unloading dead), while a post-disconnect leave is local
Helikon feedback because the normal connection is no longer valid.

`LocalTranslator` runs only after the existing incoming filters have allowed a
non-overlay message. It returns an optional local display line from a bounded
exact glossary. This path contains no HTTP client, API key, remote endpoint,
downloaded model, runtime extension discovery, or outgoing chat modification.

## Events

`EventBus` uses explicit subscriptions by event type. It performs no reflection
or classpath scanning, including during ticks and renders. Its Minecraft-free
catalog models tick pre/post; world join/leave; player death/respawn; screen
open/close; keyboard, mouse-button, and scroll input; HUD/world/entity/block
outline rendering; player movement/rotation; item use, attack, block break, and
block place; inventory updates; chat send/receive; packet observation; chunk
load/unload; and resource reload boundaries. The current Fabric bridge publishes
tick, world connection, identity-aware screen transitions, accepted normal chat,
HUD/world/entity/block-outline rendering, completed ordinary local interactions,
packet-class send/receive boundaries, and sampled local-player
lifecycle/movement/rotation/inventory observations. The player sampler uses a bounded local fingerprint of
the current local inventory and publishes only a revision when it changes; it
does not retain item stacks or inventory contents. Every event payload deliberately contains local primitives
and IDs, never Minecraft packet, screen, entity, or world objects.

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

`CommandCompletion` is similarly Minecraft-free. `ChatScreenMixin` forwards
only Tab presses in a dot-command's first token; it replaces an unambiguous
name locally and leaves normal server-command suggestions and arguments to
vanilla. Completion is never a send path.

`.gui` cannot open a screen while the chat screen is still closing, so the
entrypoint queues the screen change and applies it on the next tick once no
screen is open.

## Module keybinds

`KeybindManager` polls bound module keyboard keys or mouse buttons once per client tick and applies the
`Keybind.Activation` mode (toggle on press edge, hold enables while down,
press_once only enables). All transitions go through `ModuleRegistry`. While
any screen is open, keybind actions are suppressed and HOLD modules release;
physical key state keeps being tracked so a key held across the end of
suppression does not count as a fresh press. A bind may require Shift,
Control, Alt, and/or Super; all required modifiers must be held with the
primary input. The key source is an injected `KeyStateReader`, keeping the
edge/hold logic unit-testable. Keybinds are assigned with `.bind`/`.unbind`
and persist in `global.json`. `KeybindConflicts` compares the primary input
and modifiers independently of activation mode so the command and ClickGUI
can warn about overlapping module binds without changing them.

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

The core setting layer has no Minecraft imports. It supports boolean, integer,
decimal, enum, color, keybind, bounded string/string-list, block/item/entity
identifier selector, multi-select enum, numeric-range, and safe-regex values.
Collection-backed settings store defensive immutable copies; all types serialize
their own JSON and reset only themselves after malformed input. Settings can
also expose a pure `BooleanSupplier` visibility predicate and notify local
change listeners after a validated change. The ClickGUI settings panel skips
rows whose predicate is false and re-evaluates after every boolean toggle or
enum cycle, so dependent rows appear and disappear immediately; hidden
settings keep their stored value and stay reachable through `.setting`.
Editors decide how to present a type, while configuration remains
type-agnostic through `Setting.toJson()` and `Setting.applyJson()`.

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
the middle-click edge and screen-suppression policy; `OneClickFriends` is the
explicit module gate. The client entrypoint records every physical edge before
consulting that gate, then is limited to resolving a targeted player through
the 26.2 hit result and calling the store. Future targeting modules can query
`FriendManager.contains` to exclude friends by default without coupling their
decision logic to input or JSON handling.

`Annoy` is a Minecraft-free interval policy whose narrow adapter uses only
Minecraft's verified ordinary main-hand swing method after a player/no-screen
check. `SkinBlinker` owns a small `SkinLayerAccess` port: it snapshots the
session-local skin-layer options, alternates their visible state, and restores
only values it still owns on disable, panic, world exit, or a screen opening. Its Minecraft
adapter calls the local options API only; it neither saves nor broadcasts those
option changes.

`LocalCapeTexturePattern` and `LocalAuraGeometry` keep the cosmetic decisions
Minecraft-free and bounded. The only version-sensitive cape adapter runs after
`AvatarRenderer` has extracted a transient `AvatarRenderState`; it substitutes
an in-memory `DynamicTexture` only when that avatar is `Minecraft.player` and
the Local Cape module is enabled. The original skin object is never mutated,
and the next vanilla state extraction restores it automatically. The texture
is procedural from the module's locally persisted colors; it has no profile,
file download, or publishing path. The supported world-Gizmo adapter renders
at most 48 aura segments around that same local player and does not inspect
other players.

`ModuleTimingMetrics` is an opt-in, Minecraft-free recorder installed in
`ModuleRegistry`. Its disabled path only checks a volatile flag; when Debug
Overlay enables it, guarded `tick`, bounded `scan`, and `render` operations
record the last duration per module. `DebugOverlayHud` pages those local rows
and reads only the existing event-bus count, two bounded world-render cache
counts, and `ConfigurationManager`'s in-memory global-save state. It neither
changes gameplay state nor persists or transmits diagnostics. Panic disables
the module, which stops timing and hides the overlay with the other custom HUD.

`InventoryPreviewLayout` chooses at most the 27 storage slots and optional
nine hotbar slots from the verified local 36-item inventory list without a
Minecraft import. Its HUD adapter only submits existing item stacks through the
supported HUD extraction API; it never opens or mutates an inventory.
`DurabilityWarnings` filters a bounded caller-supplied list of observed
damageable held/armor facts through a Minecraft-free inclusive percentage rule.
`CoordinateTracker` holds only session memory: the entrypoint updates its last
safe local `WaypointLocation`, then enabled death/logout modules capture it at
the already-wired lifecycle boundary. The HUD shows a snapshot only in the
same local server/world scope (while retaining its recorded dimension); the
chat notifier displays it immediately. Neither creates a waypoint nor writes
coordinate data to disk.

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

`MinecraftWorldVisualizationRenderer` is the only 26.2 world-render port for
the first render-utility slice. It registers with Fabric's verified
`LevelRenderEvents.BEFORE_GIZMOS` phase and emits only supported local
`Gizmos` boxes and lines. `EntityRenderFilter`, `BlockIdList`,
`BlockEspScanCursor`, `BlockEspCache`, and `BreadcrumbTrail` are Minecraft-free
and unit-tested. EntityESP and Tracers iterate only already-rendered local
entities with configurable category/range/frame caps; local friend names are
looked up through `FriendManager`.

EntityESP's Glow and Shader modes reuse Minecraft's own entity-outline
post-processing pass instead of drawing Gizmos. Each client tick the shared
renderer filters entities exactly like the Gizmo modes and installs an
immutable, Minecraft-free `EntityEspNativeOutlineTargets` snapshot (built by
the bounded `EntityEspNativeOutlineTargetsBuilder`) into the atomic
`EntityEspRenderAccess` bridge. Two narrowly scoped mixins read that bridge:
`MinecraftEntityEspGlowMixin` answers `Minecraft.shouldEntityAppearGlowing`
as true for snapshotted entity IDs (it can only add local outlines, never
suppress a genuine Glowing effect), and
`EntityRendererEntityEspOutlineMixin` overrides the extracted
`EntityRenderState.outlineColor` in Shader mode only. Neither mixin mutates
`Entity` state, so disabling the module, leaving Glow/Shader, or changing
worlds restores vanilla behavior by simply clearing the snapshot. BlockESP advances a bounded cube cursor on
the client tick, checks only loaded chunks, and retains at most 512 matching
coordinates for rendering. Breadcrumbs samples the local player into a
bounded session-only deque and clears it when the level instance changes.
None of these visualizers changes entity state, block state, input, packets,
or disk storage.

`BlockSelection` keeps its style and bounded distance-label formatting in a
Minecraft-free module. The shared renderer reads only Minecraft's current
visible `BlockHitResult` in a loaded chunk, then emits one local Gizmo box and
optional label; it never changes the hit result, block state, reach, or input.

The same renderer draws Trajectories and TrueSight during the Gizmo phase. Both
consult the render state's current frustum before emitting any Gizmos; an
unavailable frustum means no new overlay is emitted.
`TrajectorySimulator` owns finite-vector, drag/gravity-order, bounded-step,
and injected-collision decisions without Minecraft imports; the adapter only
maps observed in-flight projectile state to verified 26.2 block clipping.
TrueSight deliberately leaves vanilla entity render state untouched and uses
bounded transparent local boxes only when entities are invisible to the local
player. `RadarProjection` is also Minecraft-free; `RadarHud` projects currently
loaded local entities onto a bounded fixed-position HUD surface.

XRay has a deliberately narrow compiled-geometry path. `XRayRenderState` is
an immutable Minecraft-free snapshot, published by `XRayRenderAccess`; its
two verified chunk-render mixins run only while `SectionCompiler.compile` is
building a local chunk. They omit non-selected blocks, retain faces that would
normally be hidden by surrounding blocks, and route selected quads through the
translucent layer with the configured local alpha. `MinecraftXRayRendererInvalidator`
calls the verified 26.2 geometry invalidation method on enable, disable, and
setting changes, so normal world geometry is rebuilt on disable. `StorageEsp`
reuses the bounded cursor/anchor/cache decision objects but owns a separate
cache and revision; its Minecraft adapter scans only loaded chunks and renders
only locally frustum-visible block-entity boxes.

`MiniPlayerLayout` provides the fixed, tested HUD geometry. `MiniPlayerHud`
extracts a temporary 26.2 player render state, optionally clears only the
temporary humanoid-equipment fields, and submits it through the supported GUI
entity extraction API; it never edits inventory or player equipment. Damage
indicators keep all decision logic in `DamageIndicatorTracker`: health snapshots,
hurt-state confirmation, indicator caps, fade, and rise calculations are
Minecraft-free. The world adapter supplies bounded observed living entities and
draws only current, frustum-visible local text Gizmos.

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
While a ClickGUI key-capture row is active, it consumes the next keyboard or
mouse token before widgets do and captures its supplied modifier mask: Escape
cancels, Backspace/Delete unbinds, and the reserved GUI key or mouse binding
is rejected. The reservation asks the current Minecraft `KeyMapping`, so it
also follows a user rebind in Controls. The
module keybind dispatcher continues to ignore input while any screen is open.

`ActiveModulesHud` is registered through Fabric's supported
`HudElementRegistry` API and only renders enabled modules. `ActiveModules`
contains Minecraft-free filtering, alphabetical/width sorting, and rainbow
color selection; `ActiveModulesLayout` stores validated scale, padding,
alignment, color mode, text shadow, and backdrop choices. `HudLayout` persists
that state. `HudEditorState` owns pointer offsets, clamping, and edge/centre
snapping without Minecraft imports; `HelikonHudEditorScreen` is only the
input/render adapter. The editor is reached from the **HUD** button in the
ClickGUI header, saves once when it closes, and returns to the ClickGUI.

Layout and presentation are separate screens. `HelikonHudEditorScreen` is a
drag-only canvas: it draws the Active Modules preview plus one placement
handle per enabled element (the selected element stays visible, greyed, while
disabled), and any of them can be dragged directly. Its
header button opens `HelikonHudSettingsScreen`, which owns every presentation
row (Active Modules sort/alignment/color/scale plus the per-element selector
and its style options) and performs no dragging; Escape returns to the
editor. Both screens save the shared `HudLayout` when they close and share
preview drawing through the package-private `HudPreviewRenderer`.

`HudElementPlacement` provides the shared non-Active-Modules layout model:
validated local enable state plus a top/bottom/left/right/centre anchor and
offsets. Every registered custom HUD renderer resolves its content bounds
against that model instead of hard-coding screen coordinates. Dragging a
placement handle intentionally converts it to a stable top-left placement.
The preview is a placement handle; every renderer clamps its actual local
content bounds to the scaled viewport, including when dynamic content changes
size.

`PlanTelemetryHud` supplies the remaining version-one telemetry surfaces as
opt-in placement entries: direction, FPS, local-player latency, a clearly
labelled local tick-cadence estimate, speed, durability summaries, potion
effects, clock, biome, server address, and totem count. It reads only the
already-loaded client/player/connection state; it never requests server data.
`ClientTpsEstimate` and `TelemetryText` keep cadence calculation and display
rules Minecraft-free and unit-testable.

`SaturationHud` is a separate local HUD element. It reads the current
local player's `FoodData.getSaturationLevel()` through the narrow 26.2 adapter
layer and formats only a bounded display string. It neither modifies hunger nor
requests any server information; panic hides it with the other custom HUD.

`BetterNametags` uses the supported world Gizmo phase to add one local
billboard for each frustum-visible nearby player. `BetterNametagText` composes
the configured facts without Minecraft imports; the adapter only reads already
rendered player state and local friend storage, so it cannot expose unloaded
entities or alter vanilla/server name tags.

## Stabilization and release boundaries

The release candidate keeps runtime decision paths Minecraft-free wherever
possible and routes integration failures through `ModuleRegistry` isolation.
Gradle's `check` task also rejects Java source tabs/trailing whitespace and
obvious network-client imports, while `releaseBundle` assembles the non-dev
remapped JAR with its SHA-256 checksum and resolved-dependency report. These
are release safeguards, not a claim that static checks replace the documented
live-client smoke tests.
