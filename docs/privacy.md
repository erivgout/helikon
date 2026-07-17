# Privacy

Helikon's bootstrap has no telemetry and no Helikon-operated service. Local
configuration remains under `.minecraft/config/helikon/`.

The project must not collect or transmit account tokens, session IDs, hardware
identifiers, installed-mod lists, chat history, coordinates, friends, waypoints,
or screenshots. Future optional integrations require explicit user action and
must be documented in [networking.md](networking.md).

PrivateMessageHelper keeps only a bounded, session-memory view of outgoing
`.pm` text for local command history. It never persists that text, transmits it
to a Helikon service, or attempts to recognize private messages from arbitrary
incoming chat.
