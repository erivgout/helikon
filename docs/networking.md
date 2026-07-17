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

AutoSprint, AutoWalk, and AutoSneak use ordinary local client input and player
sprint state. They do not construct, alter, replay, or bypass Minecraft
packets; the connected server remains authoritative for all resulting movement.

AutoTool only selects an existing local hotbar slot while the player is already
using Minecraft's normal mining interaction. It does not construct or replay
mining, inventory, or slot-selection packets.

FastPlace only lowers a local transient cooldown after the user holds the
ordinary Use key with a matching held item. It never starts, repeats, or
replays an interaction and does not construct packets; server rate limits and
interaction rules remain authoritative.

AutoEat only selects an existing local hotbar slot and holds Minecraft's
ordinary local Use binding. It never directly invokes, repeats, or fabricates
an interaction or packet; normal client interaction rules and all server-side
food, inventory, and combat rules remain authoritative.

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
