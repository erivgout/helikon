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

ChatColor operates only on components Minecraft has already received for this
client's chat display. Its local palette and opacity settings are stored with
other module configuration, but it does not store message content or send any
chat formatting, metadata, or display preference to a server or service.

BetterChat keeps only the messages Minecraft already retains for the active
client session. Its `.chat search`, `.chat history`, and explicit `.chat copy`
operations read that in-memory display list; the copied selection goes only to
the local system clipboard. BetterChat does not create a chat-log file, persist
chat content, transmit search text, or send display settings anywhere.

EntityESP, BlockESP, Tracers, and Breadcrumbs use only the entities, blocks,
and local player positions that Minecraft has already loaded for the current
session. Their bounded caches and trails are not written to disk or transmitted
to a server or service.

Trajectories, TrueSight, and Radar similarly consume only already-loaded local
world state for the active session. Their predicted paths, invisible-entity
boxes, and radar points are never persisted or transmitted.

XRay's compiled-geometry snapshot and StorageESP's bounded block-position
cache are in-memory-only local render state. They are not stored, transmitted,
or used to request chunks or access inventories.
