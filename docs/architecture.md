# Architecture

## Bootstrap scope

The current milestone is a client-only Fabric mod. `fabric.mod.json` declares
only `dev.helikon.client.HelikonClient` as a `client` entrypoint; the project has
no server entrypoint and requires no server plugin.

## Persistent discovered map

Radar's compact HUD minimap remains a render-local `NativeImage`. Persistent
discovery is a separate tick-bound pipeline under `dev.helikon.client.map`:

1. `MapDiscoveryController` accepts existing chunk/block/world lifecycle facts,
   confirms the current `WaypointContext`, and samples at most one still-loaded
   chunk per client tick.
2. `MinecraftMapChunkSampler` is the only adapter that reads `ClientLevel`,
   `LevelChunk`, heightmaps, block states, and native map colors. It emits an
   immutable 16x16 ARGB `MapChunkSnapshot`; no Minecraft object crosses the
   thread boundary.
3. `MapTileStore` owns one daemon storage worker, a bounded update queue, and a
   64-region LRU. It merges snapshots into 256x256 one-pixel-per-block regions,
   publishes immutable snapshots for rendering, and performs every file read,
   compression, backup, and write off the render/client tick path.
4. `HelikonMapScreen` requests visible snapshots asynchronously. A missing tile
   draws as undiscovered for that frame. `MapTextureCache` uploads only changed
   revisions and releases its bounded 64 dynamic textures on eviction, screen
   removal, resource reload, context loss, panic, or shutdown.

Map paths use full SHA-256 tokens of the existing waypoint world/server scope
and dimension, plus self-validating `context.json` metadata. Region files are
schema-versioned bounded GZIP streams with coordinate, pixel-count, and CRC
validation. Writes use same-directory temporary files, backups, and atomic
replacement. Malformed primaries are preserved before a valid backup is used;
newer schemas remain untouched and pause capture.

The global map quota includes primary, backup, metadata, and preserved corrupt
files. Quota exhaustion and I/O errors pause recording without deleting existing
discoveries or blocking gameplay. World changes flush the old context before
the worker opens the next; client shutdown waits at most five seconds for a
final flush.

## Optional GitHub release integration

`UpdateChecker` is a disabled-by-default Miscellaneous module that supplies the
explicit enabled state required for the only external integration. Its module
class contains no HTTP code. `UpdateCheckController` observes that state on the
client thread, starts no more than one lookup per enable/session, publishes a
completed result through `MinecraftUpdateNotifier`, and cancels pending work on
disable or shutdown.

All HTTP types are confined to `dev.helikon.client.integration.network`.
`GitHubReleaseChecker` hard-codes and validates the HTTPS `api.github.com`
release endpoint, sends no credential, rejects redirects and untrusted result
URLs, caps the JSON response at 64 KiB, and applies connect/request timeouts.
`ReleaseVersion` and response policy remain Minecraft-free and unit-tested.
Failures leave the client functional and produce no false update notice; the
integration never downloads or loads code.

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
NoFireOverlay redirects only the verified `ScreenEffectRenderer.submit`
`LocalPlayer.isOnFire` branch, leaving burning state and all other screen effects intact.
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

AntiWaterPush keeps its enable-state decision Minecraft-free. Its narrow
`Entity.updateFluidInteraction` mixin suppresses only the local player's
verified `EntityFluidInteraction.applyCurrentTo` water call, preserving water
contact processing and the separate lava-current path.
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

Baritone is a separately licensed, source-vendored component compiled into a
distinct nested Fabric JAR. `BaritoneAccess` is Helikon's narrow command,
settings, status, and path-snapshot boundary; `MinecraftBaritoneAccess` owns the
direct API calls. `BaritoneNavigation` exposes those controls as an ordinary
World module, while `MinecraftBaritoneVisualizationRenderer` translates its
snapshots into Minecraft 26.2 gizmos. Focused Baritone mixins keep its tick
hooks alive when a container or Helikon screen is open. The build records the
upstream commits and ships the LGPL license and corresponding source. It
performs no runtime download or Helikon service request. The embedded port
defaults Baritone's legacy `randomLooking113` yaw offset to zero so forced
client-visible block-interaction aim remains stable while mining; the ordinary
Baritone setting remains available as an explicit opt-in.

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
Glide's descent cap is a separate Minecraft-free policy. Its adapter reads the
current local falling context and changes only an excessive downward velocity;
it never resets fall distance or sends a movement-status packet.

Flight changes only `Abilities` that already permit flight and restores only
values it owns. The independent Freecam module uses an invisible, unadded
client-side camera entity; input and mouse turn are redirected locally while
the player receives no movement input, then the normal camera is restored.
Scaffold is similarly constrained to a selected player-provided hotbar block,
a loaded replaceable target with local support, an existing vanilla use
interaction, and its own bounded placement delay. It runs automatically while
enabled without synthesizing Use input; automatic block selection borrows and
restores a hotbar slot only around an actual interaction. Clutch keeps its
falling/ground-proximity/mode/cooldown decision in the Minecraft-free `Clutch`
policy; the shared advanced-movement adapter only samples the local descent,
available space below, and hotbar block/water-bucket facts, then requests one
ordinary block placement onto the loaded ground below or a water-bucket use. It
never constructs a packet, and the server may still reject or correct the
resulting interaction.

Blink keeps its hold/release decision in the Minecraft-free `Blink` module and a
generic bounded `BlinkBuffer`. The only version-sensitive edge is
`ConnectionBlinkMixin`, a HEAD cancellable inject that hands each outgoing
`ServerboundMovePlayerPacket` to `BlinkPacketAccess`; the bridge buffers the
unmodified packet while enabled and, each client tick, releases the held packets
in send order once the module is disabled or its safety cap is reached. Release
resends through Minecraft's ordinary `LocalPlayer.connection.send` path behind a
re-entrancy guard so its own sends are not re-held, and a disconnect discards the
buffer instead of sending to a dead connection. It never constructs, reorders, or
malforms a packet; the server stays authoritative over the resulting movement.

Combat uses `CombatTarget`, `CombatTargetFilter`, `CombatAim`, and
`AutoPotion`'s candidate/action policy without Minecraft types. The combat
adapter collects only rendered living entities and local tab-list/profile facts,
then invokes Minecraft's normal `attack` or held-item `useItem` method after
the corresponding module policy permits it. A shared per-tick guard permits at
most one of TriggerBot, CriticalAssist, KillAura, or AutoClicker to begin attacks. KillAura's
Multi mode may issue a bounded group of ordinary attacks within that one cycle.
AutoClicker keeps its randomized clicks-per-second timing, hold/screen/entity-target/friend gates, and
first-click scheduling in a Minecraft-free policy; it runs last within that guard and turns each due click
into Minecraft's normal `attack` on the eligible crosshair entity (after the ordinary attack cooldown and
local line-of-sight) or an ordinary main-hand swing, so it replays a physical left click without building a
packet or bypassing server authority.
KillAura/TriggerBot/CriticalAssist require the locally observed
line-of-sight fact, so the adapter never requests attacks through solid blocks.
BowAimAssist only updates the local view while the user holds a normal bow; it
does not fire it. AntiBot is an in-memory heuristic filter, not a remote lookup.
TargetHUD and ReachDisplay render session-only observed data; the latter records
only Helikon's normal attack request distance and does not imply server reach.
KillAura applies the same bounded local rotation policy before its normal attack
cycle. HitFlick keeps its flick geometry in the Minecraft-free `HitFlickPolicy`;
`MinecraftHitFlickAccess` listens to Fabric's ordinary `AttackEntityCallback` and,
for a non-friend living target, sends one well-formed vanilla rotation packet with
the flicked yaw before the attack packet, then a restore rotation packet the next
tick. It never changes the local player's own yaw, edits an entity, or builds a
malformed packet, and it steers only the server's sprint/knockback bonus, which the
server still authorizes. TargetHUD retains the last crosshair or Helikon attack target while it
remains in the current locally rendered target set, then clears it on absence or
world loss; it does not flash a non-crosshair target for only one frame. BlockHit
keeps its threat-in-range, screen, shield, and unblock-window decision in a
Minecraft-free policy; `MinecraftBlockHitUseKey` is the only adapter and just
owns the ordinary local Use key (raising a held `BLOCKS_ATTACKS` shield) while an
eligible non-friend target is inside its bounded range, releasing that key around
a ready attack and on disable, panic, or player loss. It never constructs a
packet, so the server still validates every shield-blocked or unblocked hit.
world loss; it does not flash a non-crosshair target for only one frame.
RightClicker keeps its click-rate, hand-target, and friend-exclusion policy in a
Minecraft-free `decide` method that returns a single interaction category per
tick. Its narrow adapter mirrors Minecraft's own use handling for the current
crosshair hit through the public `useItemOn`/`interact`/`useItem` methods, tries
the main then off hand, swings only on a consuming result, and briefly raises the
transient `rightClickDelay` so vanilla's own held-use path does not double the
configured rate. It builds no packet and leaves server authority intact.
AntiFireball keeps its nearest-approaching-visible-in-range selection and delay
policy Minecraft-free; `MinecraftAntiFireballAccess` observes only rendered
`LargeFireball`/`SmallFireball` entities, then requests one ordinary
`MultiPlayerGameMode.attack` and swing for the single selected fireball. It joins
the same per-tick attack guard, so Helikon still issues at most one ordinary
attack per client tick, and it never builds a packet or extends reach; the server
remains authoritative over whether a hit deflects the fireball.

AutoLeave evaluates its low-health and fall-distance thresholds from an
immutable Minecraft-free context. `MinecraftAutoLeaveAccess` reads only local
player facts and invokes Minecraft's ordinary multiplayer disconnect flow;
Helikon marks that intentional leave so AutoReconnect does not rejoin.

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

`WaypointManager` remains a read-only migration source for the former
`waypoints.json` format. `BaritoneWaypointRepository` adapts Baritone's
authoritative per-world/per-dimension collection to `WaypointRepository`, which
is shared by Helikon's commands and world renderer. Because Baritone permits
blank and otherwise unrestricted names, the adapter gives valid external
entries deterministic, bounded display names and uses the same names for
lookup, rename, and removal. Structurally invalid external entries are logged
and omitted from the current snapshot rather than failing the render frame.
`WaypointContext`, `WaypointLocation`, and `WaypointNavigation` own validation,
context filtering, distance ordering, and compass labels without Minecraft
imports.
`MinecraftWaypointLocationProvider` derives the current scope, dimension, and
block position. Migration waits until Baritone has opened its world data and
does not replace a same-named entry.

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

### SeedCracker analysis boundary

`SeedCracker` owns only version-independent session state: confirmed chunk
observations, distinct-entity confirmation counts, a bounded candidate cursor,
retained matches, and the collection/search/solved/error state machine.
`SeedSlimeMath` implements Minecraft 26.2's verified legacy-random slime-chunk
predicate with its intentional 32-bit coordinate overflow and lower-48-bit
seed mask. Search work advances by the configured hard per-tick budget and
retains only the configured result cap; it does not spawn a worker that could
touch unsafe live Minecraft state.

`MinecraftSeedCrackerAccess` is the narrow version adapter. It considers only
already-loaded, alive `Slime` entities below Y=40 in the Overworld, checks the
chunk is loaded, and supplies immutable UUID/chunk/tick facts to the core.
Spawn provenance is deliberately not guessed because the multiplayer client
cannot reliably distinguish natural slime-chunk spawns from summoned,
spawner-created, Oozing-created, or recently moved slimes. In a locally owned
integrated world only, the adapter can read that server's exact full seed.
World events clear evidence, deduplication, candidates, errors, and results.

`MinecraftSeedCrackerRenderer` emits one bounded loaded-chunk Gizmo prism per
confirmed observation. `SeedCrackerHud` uses the ordinary persisted
`HudElementPlacement` and shared text presentation path, so it is draggable,
scaled, clamped, theme-compatible, and panic-hidden. `SeedCrackerCommand`
provides local-only status, search, clear, candidate, and manual evidence
correction actions through the existing intercepted dot-command dispatcher.
No SeedCracker component requests chunks, sends a Minecraft packet, writes
evidence to disk, or opens an external network connection.

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
worlds restores vanilla behavior by simply clearing the snapshot. Chams reuses that same native-outline mechanism through its own independent
bridge. `ChamsColorPolicy`, `ChamsTargets`, and `ChamsTargetsBuilder` are
Minecraft-free and unit-tested: the policy resolves an opaque friend, health, or
base color, while the bounded builder snapshots at most `maximum_entities`
selected IDs. Each client tick the shared renderer filters entities exactly like
EntityESP (via `EntityRenderFilter`, with local friends looked up through
`FriendManager`), then installs an immutable `ChamsTargets` snapshot into the
atomic `ChamsRenderAccess` bridge. Two narrowly scoped mixins read that bridge:
`MinecraftChamsGlowMixin` answers `Minecraft.shouldEntityAppearGlowing` as true
for snapshotted IDs (it can only add local outlines, never suppress a genuine
Glowing effect), and `EntityRendererChamsOutlineMixin` overrides the extracted
`EntityRenderState.outlineColor` for those IDs. Neither mixin mutates `Entity`
state, so disabling the module or changing worlds clears the snapshot and
restores vanilla behavior exactly. Chams therefore renders an occlusion-visible
colored silhouette (an outline material), distinct from EntityESP's box/wireframe
modes and its static Shader color by defaulting to players and offering
health-based coloring. BlockESP advances a bounded cube cursor on
the client tick, checks only loaded chunks, and retains at most 512 matching
coordinates for rendering. A narrow `ClientLevel.setBlock` observation updates
nearby changed coordinates immediately, while each tick revalidates the bounded
cache so stale matches cannot linger. Breadcrumbs samples the local player into a
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
ProjectilePreview shares the same simulator for the complementary case: the
Minecraft-free `HeldProjectilePreview` reproduces the verified
`shootFromRotation` launch direction, bow draw-power curve, and per-family
speed/pitch-offset, and the render adapter reads only the local held item
(bow while drawn, loaded crossbow, held trident/throwables, offhand throwables),
the player's aim, and the same block clipping. It launches from the eye position
and never creates a projectile entity or packet; the server stays authoritative
over any real shot. TrueSight deliberately leaves vanilla entity render state
untouched and uses bounded transparent local boxes only when entities are
invisible to the local player. `RadarProjection` is also Minecraft-free;
`RadarHud` projects currently loaded local entities onto a bounded HUD surface,
samples loaded surface blocks through Minecraft's native map-color API, caches
terrain samples, and layers relief, compass, player heading, and type/altitude
entity markers over that terrain.

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
- `NumberSlider` is the Minecraft-free geometry and value mapping for the
  numeric slider drawn above every `NumberSetting`/`IntegerSetting` field. It
  converts a value to a handle pixel, a track pixel to a value, and a wheel
  notch to a one-step increase/decrease. Integral sliders round to whole
  numbers and decimal sliders snap to a range-relative grid so their text stays
  clean; the paired text field still accepts exact entry. The screen only
  supplies pixel coordinates and applies each result through the setting's own
  validated `set` path, then refreshes the field text.
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
handle for every registered element regardless of module/enabled state, and
any unlocked handle can be dragged directly. Its
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

`BetterNametags` uses the supported world Gizmo phase to add bounded billboard
rows for each frustum-visible nearby player. `BetterNametagText` composes
the configured facts without Minecraft imports; the adapter only reads already
rendered player state and local friend storage, so it cannot expose unloaded
entities or alter vanilla/server name tags.

The independent `Waypoints` Render module owns marker visibility, labels,
beams, depth behavior, scale, width, and result cap. `MinecraftWaypointRenderer`
uses the supported Gizmo phase for the world-space beams of cached,
current-dimension saved waypoints. `WaypointLabelsHud` projects each matching
label into bounded GUI space with a dark panel and colored initial, which avoids
Minecraft culling a distant ground-anchored text primitive. It intentionally has
no `HudElementId` or draggable HUD layout entry, so visibility and presentation
remain controlled only by the Render module rather than HUD Settings.
`WaypointMarkerPresentation` keeps the label, distance-scale, and elevated
beam-anchor policy Minecraft-free; neither adapter can load terrain or disclose
waypoints absent from the local Baritone repository.

## Stabilization and release boundaries

The release candidate keeps runtime decision paths Minecraft-free wherever
possible and routes integration failures through `ModuleRegistry` isolation.
Gradle's `check` task also rejects Java source tabs/trailing whitespace and
obvious network-client imports, while `releaseBundle` assembles the non-dev
remapped JAR with its SHA-256 checksum and resolved-dependency report. These
are release safeguards, not a claim that static checks replace the documented
live-client smoke tests.
