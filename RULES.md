# Helikon Project Rules

This document is the decision standard for proposing, designing, reviewing, and shipping Helikon features. It condenses the governing requirements in `PLAN.md`; where a conflict exists, `PLAN.md` remains authoritative.

## 1. Feature admission gate

Before a feature is accepted for implementation, it must satisfy every applicable rule below.

1. It supports Helikon's purpose as a clean-room, open-source, client-side Fabric utility mod.
2. It can run without a Helikon-operated backend, server plugin, account system, proxy, database, VPS, API, or persistent WebSocket connection.
3. It stores Helikon-owned data locally unless the user explicitly invokes an optional third-party integration.
4. It is honest about Minecraft server authority: a client-side interface must never promise a server-side result that the server may reject.
5. It does not add malformed or exploitative packets, authentication bypasses, remote code/module loading, telemetry, hidden behavior, malware, or arbitrary code execution.
6. It has a clear user-facing purpose, a stable identity, configurable behavior where needed, defined limitations, acceptance criteria, and test coverage.
7. Its performance, security, privacy, failure behavior, documentation, and version-compatibility implications are understood before it is merged.

Reject, redesign, or defer a feature that cannot meet these criteria.

## 2. Clean-room and product identity

- Do not decompile, copy, translate, or closely imitate Aristois code or proprietary implementation details.
- Do not use Aristois assets, branding, logos, sounds, configuration formats, class names, descriptions, layouts, color schemes, or icons.
- General utility-client concepts are acceptable only when implemented with original code and product identity.
- Every official binary must have matching public source. Do not obfuscate releases.
- Use appropriately licensed dependencies and bundled assets, and produce dependency and license reports.

## 3. Client-only boundary

- Use client entrypoints only and declare the Fabric mod environment as `client`.
- Do not register server commands, change dedicated-server startup, require a server installation, or require a Helikon server plugin.
- The mod must remain useful offline, aside from normal Minecraft multiplayer connectivity.
- Support optional compatibility with third-party mods only when those mods are already installed locally by the user. Do not bundle or download them.

## 4. Backend, networking, and privacy rules

### No Helikon backend

Never add a Helikon-owned service or a feature that depends on one. This includes global/cross-server Helikon chat, hosted accounts, donor or license validation, paid unlocks, cloud synchronization, hosted cosmetics/capes, analytics, telemetry, crash reporting, remote flags, matchmaking, proxies, addon marketplaces, remote execution/loading, or persistent connections.

Do not require a database, VPS, custom REST/WebSocket API, Discord bot, custom authentication service, or recurring hosting cost.

### Optional third-party services

An optional public integration is permitted only when all of the following are true:

- Helikon does not operate the service.
- The client remains functional if it is unavailable.
- Privacy-sensitive integrations are disabled by default.
- The user is told which data is sent and why.
- Minecraft access tokens and launcher credentials are never sent.
- A local fallback exists when practical.
- The integration is not silently contacted on every launch.

Keep networking code under `dev.helikon.client.integration.network`. Core modules must not directly use HTTP or WebSockets. Each integration must declare an ID, display name, enabled state, allowed hosts, transmitted-data description, and disable behavior. Document every optional request in `docs/networking.md`, including its purpose, hostnames, trigger, sent/received data, storage behavior, disable instructions, and local fallback.

### Private data

Except for a user-invoked optional feature, never send data outside the normal Minecraft-server connection. This includes chat history, friends, waypoints, server history, settings, installed mods, hardware identifiers, account/session data, local paths, screenshots, and player coordinates.

Never store access tokens, inspect launcher-authentication files, read unrelated files, or collect hardware identifiers. Keep user-configured third-party API keys local and omit them from logs.

## 5. Module feature rules

Every module must provide:

- A stable lowercase ID independent of its display name.
- A display name, clear description, category, default enabled state, settings, serialization support, and optional keybind.
- Clean enable and disable lifecycle behavior.
- Restoration of all client state it changed when disabled or when panic is used.
- Reset of temporary state and relevant caches on world leave.
- Null-safe handling when the player or world is unavailable.
- Friend protection where targeting behavior is involved; friends are excluded by default.
- User-facing settings instead of hidden behavior or unexplained constants.
- Acceptance criteria and at least one automated test or documented manual test.

Use only these categories: Combat, Movement, Player, Render, World, Chat, and Miscellaneous.

## 6. Architecture and implementation rules

- Keep reusable core systems separate from Minecraft-version-specific hooks.
- Use interfaces/adapters around game interactions such as movement, rotation, inventory, world/entity queries, rendering, input, screens, chat, packets, and block interaction.
- Use Fabric events first; use Mixins only when events are insufficient. Keep mixins focused and do not spread mixin logic through modules.
- Use a lightweight internal event bus. Avoid reflection-heavy discovery in tick or render loops and do not use reflection-based module discovery.
- Isolate module failures at the dispatch boundary: catch unexpected exceptions, disable the faulty module, show a local notification, log the stack trace, and continue when safe. Never silently swallow errors, use empty catch blocks, or catch `Throwable`.
- Prefer small, focused classes; clear names; immutable fields where practical; constructor validation; records for immutable data; Java logging; consistent nullability annotations; and dependency injection where useful.
- Avoid giant managers, global mutable state, magic numbers, GUI logic in module business logic, IDs derived from names, excessive static utilities, and premature cross-version abstractions.

## 7. Settings, commands, and local data

- Settings must have stable IDs, names, descriptions, defaults, current values, validation, reset behavior, JSON serialization, change listeners, and applicable bounds or visibility predicates.
- Use safe defaults for invalid manually edited configuration values and log the recovery.
- Store Helikon configuration as human-readable JSON under `.minecraft/config/helikon/`.
- Use schema versions, migrations, atomic replacement, backups, debounced saves, normal-shutdown saves, and graceful malformed-JSON recovery.
- Validate imported profiles before activation; profile, friend, waypoint, macro, chat-filter, and related data remain local.
- Treat imported configurations as untrusted: prevent path traversal, never execute configuration data, and never load arbitrary JARs or scripts.
- Local commands must be intercepted and must not reach the multiplayer server. Do not activate module keybinds while the user is typing in a text field.
- Persistent chat logging must be disabled by default and should have retention/deletion controls.

## 8. User experience and accessibility

- Create an original, searchable, configurable ClickGUI and drag-and-drop HUD editor.
- Keep positions and preferences local and persistent; clamp draggable UI to screen edges.
- Provide clear descriptions, tooltips, reset controls, conflict warnings, safe unbound keybinds, and local notifications for meaningful state changes or failures.
- Support readable/high-contrast text, keyboard navigation, scaling, and a reduced-animation option.
- Do not add ads, donor status, remote news, remote account status, or similar service-driven UI.
- Panic must disable modules, restore modified client state, hide the custom HUD, close the GUI, clear temporary render caches, and preserve user configuration. It must never delete logs, hide/disguise the process, interfere with screensharing, or change unrelated files.

## 9. Rendering and performance

- Use supported Fabric and Minecraft rendering APIs; do not use legacy fixed-function OpenGL.
- Keep HUD and world rendering separate, restore render state after every operation, batch compatible geometry, and dispose resources on world unload.
- Apply frustum and distance culling, bound caches, and invalidate caches when relevant chunks or blocks change.
- Disabled modules must have near-zero cost.
- Do not scan all loaded blocks every frame, allocate collections per frame, write files every tick, use reflection in hot loops, or access unsafe Minecraft state from background threads.
- Perform Minecraft state changes on the client thread.
- Maintain a local-only debug overlay for per-module tick/render time, cache sizes, subscriber count, and configuration-save state. Never transmit its data.
- Test practical compatibility with common performance mods and fail gracefully when optional integrations are unavailable.

## 10. Security and safety

- Never add scripting in version 1.0, arbitrary code execution, remote code execution, remote module loading, or arbitrary JAR loading.
- Validate user input, imported data, regular expressions, file paths, integration hostnames, and configuration values.
- Regex-based features must reject invalid patterns and guard against expensive expressions.
- Do not weaken, bypass, or silently remove tests to make a feature pass.
- Keep a current `SECURITY.md` and follow responsible dependency/license reporting.

## 11. Testing and documentation requirements

Each change must be verified in proportion to its risk. Add or update unit tests for core logic and use GameTests where practical for gameplay behavior. Document manual smoke tests where automation is not practical.

At a minimum, preserve coverage for setting validation and visibility, module lifecycle/toggling, keybind and command parsing, profiles and migrations, atomic writes, friends, waypoints, target selection, inventory scoring, color/configuration recovery, regex validation, and external-integration restrictions.

Before release, validate first launch, missing and malformed configuration, GUI/HUD behavior, settings, profiles, world/server transitions, disconnects, resource reload, resizing/fullscreen, death/respawn/dimension travel, panic, offline use, common performance mods, extended runtime, chat utilities, reconnect behavior, and server-separated waypoints.

Update documentation with every meaningful change:

- `README.md` for user-facing purpose, installation, compatibility, privacy, no-backend policy, multiplayer warning, building, licensing, and contribution guidance.
- `docs/architecture.md` for lifecycle, events, configuration, hooks, rendering, failure isolation, and integration boundaries.
- `docs/modules.md` for each module's ID, category, settings, limitations, acceptance criteria, and tests.
- `docs/networking.md` for every optional external request.
- `docs/version-porting.md` for adapter, mapping, Mixin, and rendering changes plus porting validation.

## 12. Delivery workflow

For each task:

1. Read the relevant architecture and feature documentation.
2. Inspect existing interfaces before introducing abstractions.
3. Implement the smallest complete, cohesive change.
4. Add or update tests and documentation.
5. Run the full applicable test suite, formatting, and static checks.
6. Report changed files, verification performed, known limitations, and any server-authority or privacy implications.

Keep work in small, reviewable commits. Do not combine unrelated modules or features in one commit. Do not add networking without its required documentation.

## 13. Release readiness

A release candidate is ready only when the core framework, GUI, HUD editor, local profiles/friends/waypoints/macros, and required modules are stable; configuration migrations are safe; panic restores state; disabled-module overhead is negligible; integrations are optional and documented; CI builds and tests the project; and published binaries match the public source.
