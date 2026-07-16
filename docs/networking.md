# Networking

## Current bootstrap

Helikon makes no HTTP, WebSocket, telemetry, analytics, update-check, account,
or backend requests. It only uses Minecraft's normal connection when the player
joins a server.

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
