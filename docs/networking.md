# Networking

## Current bootstrap

Helikon makes no HTTP, WebSocket, telemetry, analytics, update-check, account,
or backend requests. It only uses Minecraft's normal connection when the player
joins a server.

When the user explicitly runs a configured macro, its `chat` and `command`
actions use that same normal Minecraft server connection. Macro definitions,
action lists, and server-scope choices remain local; Helikon sends no macro
data to any separate service.

Fullbright changes only local client options and local visual effect state. It
does not construct, alter, or send a Minecraft packet.

AntiBlind and BetterCrosshair likewise filter or add only local render state;
they do not remove server effects, change hit detection, or send packets.

EntityESP, BlockESP, Tracers, and Breadcrumbs inspect only the already-loaded
client world and emit local render Gizmos. They do not request chunks, send
coordinates, modify entity/block state, or create any network traffic. The
BlockESP cache and Breadcrumb trail are bounded in-memory state only.
BlockSelection likewise reads only the current loaded block target to draw one
local Gizmo box and optional distance label.

Trajectories, TrueSight, and Radar likewise read only current client-side
entity/projectile/block state to create local overlay geometry or HUD points.
They do not request additional chunks, send coordinates, or modify packets,
entity state, projectile movement, or normal server interactions.

XRay filters and rebuilds only already-loaded local chunk geometry. StorageESP
reads only already-loaded block states while advancing its bounded local scan.
Neither requests chunks, opens inventories, reads server storage contents,
changes blocks, or sends custom/modified packets.

MiniPlayer extracts the existing local player render state for the HUD only.
DamageIndicators reads already-received local health and hurt-state fields from
nearby loaded entities. Neither opens inventories, changes equipment or health,
requests combat data, or sends packets.

AutoSprint, AutoWalk, and AutoSneak use ordinary local client input and player
sprint state. They do not construct, alter, replay, or bypass Minecraft
packets; the connected server remains authoritative for all resulting movement.
Twerk uses the same fresh local input path for a bounded sneak pulse.

SkinBlinker changes only the local in-memory model-part option set. It neither
saves nor broadcasts those option changes and restores its own unmodified
values on disable, panic, world exit, or screen opening. OneClickFriends changes only the local
friend JSON store when its explicit module gate is enabled. Annoy is the single
exception in this group: its 20-tick-or-longer local interval can ask
Minecraft's normal main-hand swing path for one ordinary swing. That path is
server-visible under normal Minecraft rules, but Annoy never constructs,
modifies, or replays a packet, attacks, changes a target, or sends chat.

Local Cape generates one in-memory texture from locally stored color settings
and substitutes it only in the local player's transient render state. Local
Cosmetics produces at most 48 local Gizmo lines around that player. Neither
feature requests a profile, downloads/uploads an asset, reads another player's
cosmetic data, creates a packet, or has a network path.

Debug Overlay records timing and diagnostic values only in local process memory
while its explicit module is enabled. It creates no file, chat message, packet,
telemetry event, service request, or hardware/account identifier.

Inventory Preview and Durability Warnings read only local `Inventory` and
`ItemStack` state to produce HUD extraction calls. They never open a screen,
move an item, or send a packet. Death Coordinates and Logout Coordinates retain
one session-local coordinate snapshot from the existing local waypoint-location
adapter and emit optional local system feedback; they never make a waypoint,
write a coordinate file, or transmit position data.

AutoTool only selects an existing local hotbar slot while the player is already
using Minecraft's normal mining interaction. It does not construct or replay
mining, inventory, or slot-selection packets.

FastPlace only lowers a local transient cooldown after the user holds the
ordinary Use key with a matching held item. It never starts, repeats, or
replays an interaction and does not construct packets; server rate limits and
interaction rules remain authoritative.

FastBreak only lowers an existing local normal destroy cooldown while Attack is
held over a loaded visible block target. Nuker is separately disabled by
default, requires an explicit whitelist plus held Attack, scans only loaded
blocks within Minecraft's existing interaction range, and caps itself at two
calls to Minecraft's normal `startDestroyBlock` path per tick. Neither builds,
modifies, or replays a packet; a server decides whether each ordinary destroy
attempt succeeds and can correct client prediction. Baritone compatibility is
only a local Fabric mod-ID check and sends nothing.

AutoEat only selects an existing local hotbar slot and holds Minecraft's
ordinary local Use binding. It never directly invokes, repeats, or fabricates
an interaction or packet; normal client interaction rules and all server-side
food, inventory, and combat rules remain authoritative.

NoSlow, FastLadders, WaterJump, Jesus, Spider, Step, Speed, BunnyHop, ExtraElytra, and Timer
change only local movement/input, collision, view, or client-time calculations.
WaterJump adds only an ordinary local Jump request after loaded water-edge
checks. They do not create, modify, replay, or spoof movement packets, and a
server can reject or correct any client-side motion it does not permit. Flight
uses Minecraft's ordinary ability-update path when `mayfly` is present and
ordinary local velocity otherwise. NoFall sends a standard grounded movement
status while an unmounted player is falling, resets the matching client fall
accumulator, and leaves flight and Elytra states untouched. A multiplayer
server remains authoritative and may reject or penalize that status.

AntiCactus only changes an ordinary local `MoverType.SELF` movement vector when
its bounded loaded-cactus collision observation predicts an intersection. It
does not send, fabricate, suppress, or alter a movement packet; cactus damage
and the final movement outcome remain server-authoritative.

Blink changes only the timing of ordinary local movement sends. While enabled it
cancels the outgoing `ServerboundMovePlayerPacket` at Minecraft's normal
connection boundary and holds the unmodified packet in a bounded local buffer;
disabling, reaching the safety cap, or leaving the world releases those exact
packets in send order back through Minecraft's ordinary
`ClientPacketListener.send` path (a disconnect discards them because the
connection is gone). It never constructs, reorders, duplicates, malforms, or
replays a synthetic packet, and holds no other packet type; the server receives
only genuine vanilla movement packets and remains authoritative over the
resulting position, so it may reject, correct, rubber-band, or kick.

Freecam is entirely local: its camera entity is not added to the client level,
suppresses player movement keys, never moves the player, and never sends a
position, rotation, or camera packet.
While enabled, Scaffold automatically routes one selected player-provided block
through Minecraft's normal `useItemOn` path after loaded-target/support and
bounded-delay checks. It does not choose blocks outside the hotbar or construct
placement packets.

Combat modules do not construct, modify, replay, or spoof packets. TriggerBot,
CriticalAssist, and KillAura may ask Minecraft to perform ordinary local
attacks after their local cooldown, target, and line-of-sight checks. KillAura's
Multi mode bounds each cycle with its configured target limit; the shared bridge
still permits only one combat module to begin attacks in a tick, and the server
remains fully authoritative for damage, reach, cooldown, and validation. BowAimAssist only
changes local view rotation while the user holds a bow and never fires it.
AutoPotion selects an existing restorative hotbar potion and calls Minecraft's
normal held-item Use method; no inventory or potion packet is fabricated.
AntiBot, TargetHUD, and ReachDisplay consume only already-loaded local facts
and create no network traffic.

AutoArmor, AutoEject, AutoTotem, InventoryManager, and ChestSteal make no
packet themselves. When their narrow screen/cursor guards pass, the client
asks Minecraft to process an ordinary vanilla container input (pickup,
quick-move, or throw) for existing visible slots. Minecraft then applies its
normal client/menu and server protocol rules; Helikon neither changes an
inventory directly nor fabricates a container packet.

AutoFish observes only the currently local fishing hook and, after its local
delay/durability checks, invokes Minecraft's ordinary selected-item Use method
once. BuilderAssist reads the local hit result and loaded block states for a
bounded preview, then asks Minecraft for one normal held-block interaction
while Use is already held. Neither feature requests blocks, changes reach, or
constructs/replays a packet.

AutoReconnect has no Helikon network service and does not scan servers. After
Minecraft reports a valid multiplayer disconnect, it may hand the same stored
server target back to Minecraft's ordinary `ConnectScreen` after a local
cancellable countdown and bounded attempts. Local/singleplayer targets and
explicit leaves are declined.

ChatPrefix and ChatSuffix modify only a normal outgoing chat string immediately
before Minecraft sends it through the player's existing server connection.
They never touch slash, private-message, likely authentication, or Helikon
local-command input, and they do not create requests, retries, proxies, or
external connections.

ChatMute and ChatFilter only decide whether an incoming message is shown in
this client. They do not acknowledge, alter, suppress at the protocol layer,
or forward messages, and never affect another player or the server.

ChatSpammer sends only configured ordinary text through Minecraft's normal
player chat connection. It rejects command-like entries and applies a local
minimum delay and session cap; it neither creates packets nor bypasses server
rate limits, rejections, moderation, or punishments.

PrivateMessageHelper builds a configured server command only after the local
`.pm` or `.reply` command has been cancelled, its command token and target have
passed validation, and the final payload is inside Helikon's conservative safe
length limit. Its Minecraft adapter calls the normal `sendCommand` connection
method; it does not create packets, probe a server's private-message syntax,
retry failures, or send conversation history anywhere.

MentionNotifier reads ordinary incoming chat only to decide whether to show a
local Helikon notification. It sends no acknowledgement, sound request, taskbar
signal, or external desktop notification.

AutoReply can send one validated ordinary chat string only after its local
incoming-message policy permits it. It rejects local/slash-command output,
pauses in screens by default, uses the existing normal chat sender, and keeps
per-sender and per-minute cooldown state locally. It never constructs packets,
retries a failed send, or relays chat through a Helikon service.

AntiSpam uses only local incoming-message facts to decide whether the chat HUD
should show a repeated, rapid, or same-type join/leave message. It neither
acknowledges nor changes messages at the protocol layer, and sends no data to
the server or another service.

ChatTimestamps changes only the local component handed to Minecraft's chat
display after Minecraft has logged the original incoming content. The original
server message, signature, client log text, and all network traffic are
unchanged.

ChatColor reads only the already received local display component and modifies
the later local chat-display copy plus its rendered background alpha. It does
not alter outbound chat, message signatures, client-log text, packets, or
server-visible formatting.

BetterChat changes only local retained/displayed chat. Its clickable standard
player names use Minecraft's local **suggest command** action, which merely
places a normal `/msg <name>` string in the chat input; it does not send the
string. `.chat copy` writes only the user-selected retained line to the local
system clipboard. BetterChat never sends history, search text, counters, or
display settings to a server or external service.

ChatHistory records only local copies of already accepted non-overlay chat and
has no network path of its own. Persistent logging is explicit and disabled by
default; enabling it writes a local per-server file rather than sending logs,
searches, copied content, player names, or reopened drafts to any service.

Announcer may pass one configured, bounded ordinary chat line to Minecraft's
existing player-chat connection only after a user enables both the module and
the relevant trigger. It applies a local cooldown and session cap, rejects
slash/local-command templates, and never constructs or changes a packet. Its
observed trigger facts and completed-advancement IDs stay only in session memory.

LocalTranslator has no network implementation. It uses only a bounded local
glossary after the module is enabled; source chat text and translations are
never sent to a Helikon or third-party service.

## Future policy

Optional external integrations must be isolated under
`dev.helikon.client.integration.network`, disabled by default when
privacy-sensitive, manually initiated where practical, and documented here with:

- purpose and hostname allowlist;
- trigger and transmitted/received data;
- local storage behavior;
- disable instructions and local fallback.

Minecraft access tokens, session data, chat history, coordinates, friends,
waypoints, and local file paths must never be transmitted by Helikon.
