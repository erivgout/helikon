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
