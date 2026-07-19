# Privacy

Helikon's bootstrap has no telemetry and no Helikon-operated service. Local
configuration remains under `.minecraft/config/helikon/`.

The project must not collect or transmit account tokens, session IDs, hardware
identifiers, installed-mod lists, chat history, coordinates, friends, waypoints,
or screenshots. ChatHistory can retain chat content only in its explicit
off-by-default local persistence mode; it is never collected or transmitted.
Future optional integrations require explicit user action and must be documented
in [networking.md](networking.md).

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

ChatHistory is independent of BetterChat and disabled by default. It retains
accepted non-overlay incoming and accepted ordinary outgoing chat only while
enabled; local `.` commands are excluded. Its optional `persistent_logging` setting also
defaults off. If a user enables it, bounded records are kept only in local
per-server files with opaque hashed names, retention pruning, backup, and
corrupt-file recovery. `.history copy` and `.history player` send text only to
the local system clipboard, while `.history reopen` opens an unsent local
draft. No record, search query, or clipboard selection is transmitted.

Announcer is disabled by default, and every individual trigger is disabled by
default. Its transient local position/health/dimension observations, recent
attack candidate, and completed-advancement identifiers stay only in session
memory. If a user enables a trigger, the resulting configured ordinary chat
text uses only Minecraft's normal server connection; it is never sent to a
Helikon service or retained in a Helikon log.

LocalTranslator is disabled by default and sends no text anywhere. Its bounded
glossary is stored with local module settings. Helikon does not provide or
contact a translation server, nor store API keys.

EntityESP, BlockESP, Tracers, and Breadcrumbs use only the entities, blocks,
and local player positions that Minecraft has already loaded for the current
session. Their bounded caches and trails are not written to disk or transmitted
to a server or service.
BlockSelection's current target box and optional distance label are transient
local render facts and are not retained or transmitted.

Trajectories, TrueSight, and Radar similarly consume only already-loaded local
world state for the active session. Their predicted paths, invisible-entity
boxes, and radar points are never persisted or transmitted.

XRay's compiled-geometry snapshot and StorageESP's bounded block-position
cache are in-memory-only local render state. They are not stored, transmitted,
or used to request chunks or access inventories.

MiniPlayer's temporary render state and DamageIndicators' bounded health
snapshot/label state are session-only memory. They are never persisted,
transmitted, or used to query player inventories or server combat history.

Automation observes only the current client menu, selected item, local fishing
hook, loaded replacement candidates, and (for reconnect) the active Minecraft
server target. Its transient plans, countdown, and attempt count remain in
memory for the current client session; it does not persist inventory contents,
fishing events, build locations, or server addresses outside Minecraft's own
normal server list/configuration. No automation data is sent to a Helikon
service because no such service exists.

FastBreak retains only an in-progress client cooldown value so it can restore
an unchanged value on disable. Nuker retains only the current small loaded
target list and a temporary hotbar-slot ownership record; neither writes block
positions, block IDs, tool choices, or break history to disk or transmits them.
Embedded Baritone stores its settings and bounded world/path caches locally in
the game directory. Routes, goals, block selections, and inventory observations
are not sent to Helikon or any external service. Its gameplay actions use only
Minecraft's existing connection to the selected server.

Advanced movement keeps only transient local input, velocity, camera, ability,
and Elytra status needed for the current session. Freecam's temporary camera
entity is never added to the world, persisted, or transmitted. No movement
module writes a route, block-placement history, speed trace, fall trace, or
camera position to disk or a service.
WaterJump's current water-edge facts are transient input-decision data only;
they are neither retained nor transmitted.
AntiCactus retains no movement or cactus history; its bounded collision boxes
exist only while one local movement vector is being evaluated.

Combat keeps only transient local target facts, anti-bot heuristic inputs, a
current HUD target, and the most recent Helikon-requested attack distance.
These values are bounded session memory, are not persisted, and are never sent
to a Helikon service. AutoPotion reads only the player's current hotbar potion
components and does not store inventory contents.
