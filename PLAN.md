# Helikon Utility Client — Codex Build Plan

## 1. Project Summary

Build a clean-room, open-source Minecraft Java Edition utility client inspired by the feature breadth and usability of Aristois.

Working name: **Helikon**

The project should be a client-side Fabric mod with a modular architecture, ClickGUI, HUD editor, local profiles, local friends, local waypoints, commands, chat utilities, render tools, movement tools, automation modules, and optional compatibility with locally installed third-party mods.

The project must not copy Aristois source code, assets, branding, logos, sounds, configuration formats, or proprietary implementation details.

The client should operate without any Helikon-owned backend service.

---

## 2. Primary Goals

The finished project should:

- Run as a Fabric client-side mod.
- Work without a custom website backend, database, VPS, API, WebSocket server, or hosted service.
- Store profiles, settings, friends, waypoints, macros, and chat preferences locally.
- Provide a clean, searchable, configurable ClickGUI.
- Provide a drag-and-drop HUD editor.
- Support local commands and keybinds.
- Support modular feature development.
- Be resilient across Minecraft updates.
- Fail safely when one module breaks.
- Be testable and maintainable.
- Be usable offline except when the user joins a normal Minecraft multiplayer server.
- Clearly distinguish client-only effects from server-authoritative behavior.
- Avoid anti-cheat bypass presets, malformed packet exploits, authentication bypasses, telemetry, malware, and hidden behavior.

---

## 3. Recommended Development Stack

Use the newest stable versions compatible with the selected Minecraft target.

Initial target:

- Minecraft Java Edition 26.2
- Java 25
- Fabric Loader 0.19.3 or newer compatible stable version
- Fabric API compatible with the target Minecraft version
- Fabric Loom 1.17 or newer compatible stable version
- Gradle 9.5.1 or newer compatible stable version
- Gradle Kotlin DSL
- Mojang official mappings
- Java
- Mixin only when Fabric events are insufficient
- JUnit 5
- Fabric Loader JUnit
- Minecraft GameTest framework
- GitHub Actions

Start from FabricMC's official example mod template.

Pin dependency versions in `gradle.properties`. Do not use dynamic versions such as `latest.release`.

---

## 4. Self-Contained Operation Policy

The mod must not require the project owner or user to host, maintain, or pay for a separate external service.

The client should function using:

- Local files
- Local configuration
- Minecraft's normal network connection
- Information provided by the Minecraft server the user joins
- Optional third-party mods installed locally by the user
- Optional public services that are not operated specifically for Helikon

### Allowed Features

The following are allowed:

- Ordinary Minecraft chat messages
- Chat spammer
- Chat colors
- Chat timestamps
- Chat filtering
- Chat muting
- Chat highlighting
- Chat history
- Chat search
- Message copying
- Chat suffixes and prefixes
- Auto replies
- Announcer messages
- Message macros
- Private-message helpers
- AutoReconnect
- Server-specific local profiles
- Local friends
- Local waypoints
- Local capes
- Local cosmetics
- Local configuration import and export
- Optional GitHub or Modrinth update checks
- Optional integration with locally installed mods
- Optional third-party translation APIs configured directly by the user

### Prohibited Features

Do not implement anything that requires a Helikon-owned backend, including:

- Global Helikon IRC
- Cross-server Helikon chat
- Helikon-hosted accounts
- Donor authentication
- License-key validation
- Paid module unlocking
- Cloud profile synchronization
- Cloud waypoint synchronization
- Cloud friend synchronization
- Hosted cape distribution
- Hosted cosmetic distribution
- Hosted analytics
- Hosted telemetry
- Hosted crash reporting
- Remote feature flags
- Custom matchmaking
- A custom multiplayer proxy
- A Helikon server plugin requirement
- A custom addon marketplace backend
- Remote code execution
- Remote module loading
- Persistent Helikon WebSocket connections

The project must not require:

- A database
- A VPS
- A custom REST API
- A custom WebSocket server
- A Discord bot
- A custom authentication service
- Recurring hosting costs

### Third-Party Public Services

A feature may optionally use an established public service only when:

1. Helikon does not operate the service.
2. The client remains usable when the service is unavailable.
3. Privacy-sensitive integrations are disabled by default.
4. The user is told what information is transmitted.
5. Minecraft access tokens and launcher credentials are never sent.
6. A local fallback exists when practical.

Examples:

- Checking GitHub Releases for updates
- Opening a Modrinth page
- Optional chat translation using a user-configured API
- Looking up a public player skin
- Importing a public configuration after explicit user action

Do not silently contact these services during every launch.

### Privacy Requirements

Do not transmit the following outside the Minecraft server connection unless the user explicitly invokes a feature that requires it:

- Chat history
- Friend lists
- Waypoints
- Server history
- Module settings
- Installed mod lists
- Hardware identifiers
- Account tokens
- Session identifiers
- Local file paths
- Screenshots
- Player coordinates

Document all optional external requests in `docs/networking.md`.

---

## 5. Repository Structure

Use a structure similar to:

```text
helikon/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   ├── pull_request_template.md
│   └── workflows/
│       ├── build.yml
│       ├── test.yml
│       └── release.yml
├── docs/
│   ├── architecture.md
│   ├── configuration.md
│   ├── contributing.md
│   ├── modules.md
│   ├── networking.md
│   ├── privacy.md
│   ├── testing.md
│   └── version-porting.md
├── src/
│   ├── client/
│   │   ├── java/dev/helikon/client/
│   │   │   ├── HelikonClient.java
│   │   │   ├── command/
│   │   │   ├── config/
│   │   │   ├── event/
│   │   │   ├── friend/
│   │   │   ├── gui/
│   │   │   ├── hud/
│   │   │   ├── input/
│   │   │   ├── integration/
│   │   │   ├── module/
│   │   │   │   ├── combat/
│   │   │   │   ├── movement/
│   │   │   │   ├── player/
│   │   │   │   ├── render/
│   │   │   │   ├── world/
│   │   │   │   └── miscellaneous/
│   │   │   ├── notification/
│   │   │   ├── profile/
│   │   │   ├── render/
│   │   │   ├── setting/
│   │   │   ├── util/
│   │   │   └── waypoint/
│   │   └── resources/
│   │       ├── assets/helikon/
│   │       ├── fabric.mod.json
│   │       └── helikon.client.mixins.json
│   └── test/
├── LICENSE
├── README.md
├── SECURITY.md
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── gradlew
```

Use package root:

```java
dev.helikon.client
```

---

## 6. Architectural Principles

### Clean-Room Implementation

Do not decompile, copy, translate, or closely imitate Aristois code.

It is acceptable to reproduce general concepts such as:

- Category-based GUI
- Toggleable modules
- Module settings
- Keybinds
- Commands
- HUD editor
- Local profiles
- Local waypoints
- Local friend lists

Use original:

- Code
- Assets
- Icons
- Layout
- Color scheme
- Class names
- Configuration format
- Descriptions
- Branding

### Client Isolation

All entrypoints must be client entrypoints.

The mod must not:

- Register server commands
- Require server-side installation
- Modify dedicated server startup
- Require a Helikon server plugin

Set the mod environment to client in `fabric.mod.json`.

### Stable Core and Isolated Hooks

Keep reusable systems separate from Minecraft-version-specific code.

Create interfaces around:

- Player movement
- Player rotation
- Inventory interaction
- World queries
- Entity targeting
- Rendering
- Key input
- Screen handling
- Chat handling
- Packet observation
- Block interaction

Do not scatter mixin logic throughout module classes.

### Failure Isolation

A broken module must not crash the whole client.

At the module dispatch boundary:

1. Catch unexpected exceptions.
2. Disable the module.
3. Show a local notification.
4. Log the stack trace.
5. Continue running when safe.

Never silently swallow exceptions.

---

## 7. Core Module API

Create a base module type similar to:

```java
public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private Keybind keybind;

    public final void enable() {}
    public final void disable() {}
    public final void toggle() {}

    protected void onEnable() {}
    protected void onDisable() {}
}
```

Every module must include:

- Stable lowercase ID
- Display name
- Description
- Category
- Default enabled state
- Optional keybind
- Settings
- Enable callback
- Disable callback
- Serialization support

Example IDs:

```text
auto_sprint
fullbright
better_crosshair
block_esp
chat_spammer
```

### Categories

Use:

- Combat
- Movement
- Player
- Render
- World
- Chat
- Miscellaneous

---

## 8. Settings API

Required setting types:

- Boolean
- Integer
- Decimal
- Enum
- Color
- Keybind
- String
- String list
- Block selector
- Item selector
- Entity selector
- Multi-select enum
- Range
- Regular expression

Each setting supports:

- ID
- Display name
- Description
- Default value
- Current value
- Validation
- Reset to default
- Optional minimum and maximum
- Optional visibility predicate
- JSON serialization
- Change listener

Invalid manually edited configuration values must be replaced with safe defaults and logged.

---

## 9. Event System

Build a lightweight internal event bus.

Required events:

- Client tick pre
- Client tick post
- World join
- World leave
- Player death
- Player respawn
- Screen open
- Screen close
- Key press
- Mouse button
- Mouse scroll
- HUD render
- World render
- Entity render
- Block outline render
- Player movement update
- Player rotation update
- Item use
- Attack
- Block break
- Block place
- Inventory update
- Chat receive
- Chat send
- Packet send observation
- Packet receive observation
- Chunk load
- Chunk unload
- Resource reload

Avoid reflection-heavy discovery inside tick or render loops.

---

## 10. Configuration System

Store configuration under:

```text
.minecraft/config/helikon/
```

Suggested structure:

```text
config/helikon/
├── global.json
├── hud.json
├── friends.json
├── waypoints.json
├── macros.json
├── chat-filters.json
├── profiles/
│   ├── default.json
│   ├── building.json
│   └── singleplayer.json
└── servers/
    └── example.org.json
```

Requirements:

- Human-readable JSON
- Atomic file replacement
- Backup files
- Schema version
- Migration support
- Debounced saves
- Save on normal shutdown
- Graceful malformed JSON recovery
- No remote synchronization

### Profiles

Support:

- Create
- Rename
- Duplicate
- Delete
- Import from local file
- Export to local file
- Set default
- Associate with server address
- Associate with singleplayer world

Validate imported profiles before activation.

---

## 11. Keybind System

Support:

- Keyboard keys
- Mouse buttons
- Toggle binds
- Hold binds
- Press-once binds
- Modifier combinations
- Conflict warnings
- Unbound state

Default GUI key:

```text
Right Shift
```

Do not activate module keybinds while typing in text fields.

---

## 12. Local Command System

Use command prefix:

```text
.
```

Required commands:

```text
.help
.toggle <module>
.bind <module> <key>
.unbind <module>
.modules
.search <text>
.setting <module> <setting> <value>
.reset <module>
.profile list
.profile load <name>
.profile save <name>
.profile delete <name>
.friend add <player>
.friend remove <player>
.friend list
.waypoint add <name>
.waypoint remove <name>
.waypoint list
.macro list
.macro run <name>
.panic
.gui
```

Commands must be intercepted locally and not sent to the server.

Add tab completion.

---

## 13. ClickGUI

Create an original interface.

Required features:

- Right Shift default key
- Category sidebar
- Search
- Scrollable module list
- Module toggle
- Expandable settings
- Keybind assignment
- Reset setting
- Reset module
- Tooltips
- Draggable window
- Resizable window
- GUI scaling
- Theme editor
- Color picker
- Keyboard navigation
- Edge clamping
- Saved window positions
- High-contrast text
- Reduced animation option

Suggested layout:

- Left: categories
- Center: modules
- Right: selected settings
- Top: search and active profile
- Bottom: version and local status

Do not include ads, donor status, remote news, or account status.

---

## 14. HUD Editor

Implement a drag-and-drop HUD editor.

HUD elements:

- Active modules
- Coordinates
- Direction
- FPS
- Ping
- TPS estimate
- Speed
- Armor durability
- Held-item durability
- Potion effects
- Inventory preview
- Target information
- Clock
- Biome
- Server address
- Saturation
- Totem count

Each element supports:

- Enabled state
- Position
- Scale
- Alignment
- Background
- Padding
- Text shadow
- Color
- Rainbow mode
- Preview mode

Support snapping to edges, center, and other elements.

---

## 15. Local Friend System

Required behavior:

- Add by player name
- Remove by player name
- Middle-click player to toggle friend status
- Exclude friends from targeting modules by default
- Friend render color
- Local storage in `friends.json`

Do not synchronize friends online.

---

## 16. Local Waypoint System

Each waypoint includes:

- Name
- Coordinates
- Dimension
- Color
- Optional icon
- World identifier or server address
- Enabled state
- Creation date

Features:

- Add current location
- Add manual coordinates
- Delete
- Rename
- Toggle
- Sort by distance
- Render direction and distance
- Hide unrelated waypoints
- Optional death waypoint
- Optional logout waypoint

---

## 17. Module Implementation Rules

Every module must:

- Have a clear description.
- Disable cleanly.
- Restore modified client state.
- Reset temporary state on world leave.
- Check for null player and world state.
- Respect friends where appropriate.
- Avoid malformed packet sequences.
- Avoid anti-cheat bypass presets.
- Expose settings instead of hidden constants.
- Include acceptance criteria.
- Include at least one automated or documented manual test.

When the server is authoritative, the UI must not falsely claim a client-side change is guaranteed to work.

---

# 18. Module Roadmap

## Phase A: Core Framework

Build before individual modules:

1. Fabric bootstrap
2. Module registry
3. Settings API
4. Event bus
5. Configuration system
6. Profile system
7. Keybind manager
8. Local command manager
9. Notification system
10. ClickGUI shell
11. HUD editor shell
12. Friend system
13. Waypoint system
14. Panic system
15. Logging and crash isolation
16. Test harness

Acceptance criteria:

- Client launches.
- ClickGUI opens.
- Dummy module toggles.
- Settings save and reload.
- Profiles work.
- Panic resets enabled modules.
- No external service is required.

---

## Phase B: Basic Render Modules

### Fullbright

Settings:

- Gamma mode
- Night vision mode
- Brightness level

Must restore original brightness when disabled.

### AntiBlind

Hide configured effects:

- Blindness
- Darkness
- Nausea
- Pumpkin overlay
- Powder snow overlay

### BetterCrosshair

Settings:

- Size
- Gap
- Thickness
- Outline
- Color
- Dynamic movement
- Hide vanilla crosshair

### ActiveModules

Settings:

- Alphabetical sorting
- Width sorting
- Animation
- Alignment
- Color mode

### SaturationDisplay

Show hunger saturation in HUD.

### BetterNametags

Display:

- Health
- Distance
- Armor
- Held item
- Friend status

### AntiTotemAnimation

Suppress or reduce the local totem overlay.

### Dinnerbone

Render selected entities upside down locally.

### RainbowEnchant

Customize enchantment glint locally.

---

## Phase C: Basic Movement Modules

### AutoSprint

Settings:

- Always
- Forward only
- Hunger check
- Collision check

### AutoWalk

Settings:

- Continue forward
- Stop on GUI
- Allow steering

### AutoSneak

Modes:

- Toggle
- Hold
- Edge-only

### AutoParkour

Requirements:

- Jump near safe ledges
- Avoid lava
- Avoid large drops
- Minimum movement speed

### InventoryWalk

Allow movement in ordinary inventory screens.

Exclude text-entry screens.

### AntiAFK

Actions:

- Rotation
- Jump
- Short movement

Include configurable interval.

---

## Phase D: Player Automation

### AutoEat

Settings:

- Hunger threshold
- Health threshold
- Food priority
- Avoided foods
- Combat interruption rule

### AutoTool

Features:

- Select best hotbar tool
- Durability guard
- Restore prior slot

### AutoArmor

Features:

- Equip best armor
- Durability preference
- Binding Curse protection
- Delay

### AutoEject

Features:

- Item blacklist
- Drop stack or item
- Delay
- Protected slots

### AutoFish

Features:

- Bobber detection
- Reel
- Recast
- Delays
- Low durability stop

### AutoReconnect

Features:

- Countdown
- Cancel button
- Maximum attempts
- Do not reconnect after explicit user disconnect

### AutoTotem

Features:

- Health threshold
- Fall threshold
- Restore previous offhand item

### Macro

Features:

- Local commands
- Minecraft commands
- Chat messages
- Multiple actions
- Optional delays
- Per-server macros

Do not allow arbitrary code execution.

---

## Phase E: Chat Modules

### ChatSpammer

Settings:

- Message list
- Delay
- Sequential order
- Random order
- Stop on disconnect
- Pause in GUI
- Counters
- Timestamps
- Session message cap

Requirements:

- Enforce minimum delay.
- Do not bypass rate limits.
- Stop after repeated rejection.
- Warn the user that servers may punish spam.

### ChatColor

Local rendering settings:

- Normal message color
- Player name color
- Timestamp color
- Mention color
- System message color
- Private message color
- Background opacity
- Text shadow

Optional outgoing formatting may be used only when the server normally permits it.

### ChatMute

Filters:

- Global chat
- System messages
- Death messages
- Advancement messages
- Join and leave
- Command feedback
- Repeated messages
- Custom text filters

### ChatFilter

Features:

- Keyword filters
- Regex filters
- Player filters
- Case sensitivity
- Hide
- Highlight
- Sound
- Desktop notification
- Per-server profiles

Validate regex patterns and guard against expensive expressions.

### ChatSuffix

Settings:

- Suffix
- Separator
- Exclude commands
- Exclude private messages
- Per-server suffix
- Random suffix list

Do not modify authentication commands or likely password messages.

### ChatPrefix

Use the same protections as ChatSuffix.

### ChatTimestamps

Settings:

- 12-hour or 24-hour
- Seconds
- Brackets
- Color
- Relative mode

### ChatHistory

Features:

- Expanded history
- Search
- Copy message
- Copy player name
- Reopen sent messages
- Optional local persistence
- Per-server files
- Automatic deletion period

Persistent logging must be disabled by default.

### BetterChat

Features:

- Expanded history
- Message copying
- Clickable names
- Duplicate stacking
- Message counters
- Longer visibility
- Adjustable fade
- Compact mode
- Smooth scroll
- Search

### MentionNotifier

Notifications:

- Sound
- HUD notification
- Taskbar flash
- Highlight
- Optional desktop notification

### AutoReply

Rule fields:

- Trigger
- Exact, contains, or regex mode
- Reply
- Cooldown
- Whitelist
- Blacklist
- Server restriction
- Replies per minute

Prevent loops.

### Announcer

Possible triggers:

- Death
- Kill
- Item pickup
- Distance traveled
- Block mined
- Dimension change
- Join
- Leave
- Advancement
- Low health
- Totem use

Each trigger must be individually configurable.

### PrivateMessageHelper

Features:

- Configurable `/msg` command
- Configurable `/r` command
- Recent conversations
- Click name to message
- Local conversation tabs
- PM notifications

### AntiSpam

Features:

- Stack duplicates
- Hide repeats
- Limit rapid messages from one player
- Collapse join and leave spam
- Thresholds
- Whitelisted message types

### LocalTranslator

Implementation priority:

1. Locally installed translation library or model
2. Optional third-party API configured by user
3. Never require a Helikon translation server

API keys must be stored locally and omitted from logs.

---

## Phase F: Basic World Modules

### FastPlace

Settings:

- Client-side use delay
- Item filter
- Safe minimum delay

### AntiCactus

Adjust local movement near cactus collision boxes.

### BlockSelection

Settings:

- Outline color
- Fill
- Line width
- Distance label

---

## Phase G: Information and ESP Modules

### BlockESP

Features:

- Block whitelist
- Outline
- Box
- Tracer
- Distance limit
- Per-block color
- Chunk cache
- Scan budget

### EntityESP

Filters:

- Players
- Hostile mobs
- Passive mobs
- Items
- Projectiles
- Friends

Modes:

- Outline
- Box
- Glow
- Shader

### Tracers

Settings:

- Entity filters
- Start position
- Distance fading
- Friend colors

### Trajectories

Support:

- Bows
- Crossbows
- Tridents
- Snowballs
- Eggs
- Ender pearls
- Splash potions

Show collision prediction.

### Breadcrumbs

Settings:

- Maximum points
- Time limit
- Distance sampling
- Clear on world change

### TrueSight

Features:

- Show invisible entities locally
- Transparency setting
- Entity filters

### Radar

Modes:

- Circular
- Square

Settings:

- Rotation
- Zoom
- Entity filters
- Friend colors

### MiniPlayer

Settings:

- Rotation
- Scale
- Armor
- Background

### XRay

Features:

- Configurable block list
- Opacity
- Renderer reload warning
- Correct restoration on disable

### StorageESP

Detect:

- Chests
- Barrels
- Shulker boxes
- Furnaces
- Hoppers
- Spawners
- Configurable block entities

### DamageIndicators

Features:

- Recent damage
- Damage amount where locally available
- Fade animation

---

## Phase H: Advanced Movement

### NoSlow

Options:

- Eating
- Blocking
- Bow use
- Sneaking
- Soul sand
- Honey
- Cobwebs where feasible

No anti-cheat bypass modes.

### FastLadders

Configurable climb speed.

### WaterJump

Automatically jump at suitable water edges.

### Step

Configurable step height.

No packet-bypass modes.

### Speed

Conservative modes:

- Vanilla acceleration
- Strafe assistance
- Multiplier

Display multiplayer warning.

### BunnyHop

Features:

- Auto jump
- Direction handling
- Conservative speed limit

Do not include anti-cheat-named presets.

### Flight

Support:

- Creative mode
- Spectator mode
- Singleplayer when permitted
- Separate freecam-style view mode

Do not claim unrestricted server survival flight.

### NoFall

Support safe singleplayer or environment-supported behavior.

Warn that the server is authoritative.

### ExtraElytra

Features:

- Pitch assistance
- Speed display
- Safer landing
- Durability warning

### Scaffold

Features:

- Place below or ahead
- Hotbar selection
- Rotation
- Tower option
- Edge safety
- Placement delay

### Timer

Features:

- Client tick multiplier
- Safe range
- Reset on world leave
- Multiplayer warning

---

## Phase I: World Automation

### FastBreak

Features:

- Client interaction optimization
- Block filters
- Honest handling of server authority

### Nuker

Settings:

- Radius
- Blocks per tick
- Whitelist
- Blacklist
- Tool selection
- Line of sight
- Rotation
- Safety limit

No crash or packet-flood modes.

### ChestSteal

Settings:

- Delay
- Item filters
- Priority
- Close after completion

### InventoryManager

Features:

- Sort inventory
- Preferred hotbar slots
- Junk dropping
- Preserve named items
- Preserve enchanted items
- Durability rules

### BuilderAssist

Features:

- Placement preview
- Repeat placement
- Horizontal line
- Vertical line
- Walls
- Floors
- Uses player-provided blocks

### Baritone Compatibility Adapter

Rules:

- Do not bundle Baritone.
- Do not download Baritone.
- Detect only when installed by the user.
- Keep integration optional.
- Helikon must work without it.

---

## Phase J: Combat Utilities

### TriggerBot

Settings:

- Delay
- Weapon requirement
- Entity filters
- Friend exclusion

### BowAimAssist

Features:

- Target selection
- Projectile prediction
- Gravity and velocity
- Friend exclusion
- Adjustment speed limit
- Target marker

### CriticalAssist

Use legitimate critical conditions where possible.

Do not use packet exploits.

### AutoPotion

Settings:

- Health threshold
- Potion whitelist
- Splash or drinkable
- Delay
- Restore hotbar slot

### TargetHUD

Display:

- Name
- Health
- Armor
- Distance
- Held item
- Status effects

### KillAura

Settings:

- Entity filters
- Friend exclusion
- Range
- Field of view
- Attack delay
- Single target
- Switch target
- Priority by distance, health, or angle
- Rotation speed
- Line-of-sight requirement

Rules:

- No anti-cheat bypass presets.
- Do not attack through solid blocks.
- Do not send malformed packet sequences.

### ReachDisplay

Display measured attack distance.

Do not falsely claim the server accepts modified reach.

### AntiBot

Local heuristics:

- Tab-list presence
- Impossible entity state
- Spawn age
- Duplicate names
- Invisible or missing-profile heuristics

Never contact an external bot database.

---

## Phase K: Miscellaneous Modules

Include:

- SkinBlinker
- Twerk
- Annoy
- OneClickFriends
- Local cape renderer
- Local cosmetics
- Panic mode
- Inventory preview
- Item durability warnings
- Death coordinates
- Logout coordinates

All cosmetics must use local files or assets bundled in the mod.

---

## 19. Panic System

Implement `.panic` and a configurable key.

Panic behavior:

1. Disable all modules.
2. Restore gamma.
3. Restore timer.
4. Restore FOV.
5. Restore step height.
6. Restore modified client state.
7. Hide custom HUD.
8. Close ClickGUI.
9. Clear temporary render caches.
10. Preserve user configuration.

Do not:

- Delete logs
- Hide the process
- Disguise the mod
- Interfere with screenshare tools
- Modify unrelated files

---

## 20. Optional External Integrations

Networking code must be isolated under:

```text
dev.helikon.client.integration.network
```

Use an interface similar to:

```java
public interface ExternalIntegration {
    String id();
    String displayName();
    boolean isEnabled();
    Set<String> allowedHosts();
    List<String> transmittedDataDescription();
    void disable();
}
```

Rules:

- External integrations must be optional.
- They must be disabled by default if privacy-sensitive.
- They must list allowed domains.
- They must document transmitted data.
- Core modules must not directly use HTTP or WebSockets.
- No Helikon-owned service may be required.
- No persistent background connection.
- The client must work with every integration disabled.

Initial 1.0 release should preferably include only a manually initiated GitHub update check.

---

## 21. Rendering Requirements

- Do not use legacy fixed-function OpenGL.
- Use supported Fabric and Minecraft rendering APIs.
- Separate HUD and world rendering.
- Restore render state after operations.
- Batch compatible geometry.
- Use frustum culling.
- Use distance culling.
- Bound caches.
- Dispose resources on world unload.
- Test with common performance mods when practical.
- Fail gracefully when an optional integration is unavailable.

Do not build a custom shader framework in the first milestone.

---

## 22. Performance Requirements

- Disabled modules should have near-zero cost.
- Do not scan every loaded block every frame.
- Use chunk caches for BlockESP and XRay.
- Invalidate caches on chunk and block changes.
- Avoid reflection in hot loops.
- Avoid per-frame collection allocation.
- Do not write files every tick.
- Do not access unsafe Minecraft state from background threads.
- Perform Minecraft state changes on the client thread.

Add a local debug overlay with:

- Tick time per module
- Render time per module
- Cache sizes
- Event subscriber count
- Configuration save state

Do not transmit this information.

---

## 23. Security Requirements

- Treat imported configurations as untrusted.
- Prevent path traversal.
- Do not execute code from configuration files.
- Do not load arbitrary JARs.
- Do not include scripting in version 1.0.
- Do not store access tokens.
- Do not inspect launcher authentication files.
- Do not read unrelated files.
- Do not collect hardware identifiers.
- Do not obfuscate official releases.
- Publish matching source for every official binary.
- Generate dependency and license reports.
- Add `SECURITY.md`.

---

## 24. Testing Strategy

### Unit Tests

Test:

- Setting validation
- Setting visibility
- Module lifecycle
- Module toggling
- Keybind parsing
- Command parsing
- Profile serialization
- Profile migration
- Atomic writes
- Friend operations
- Waypoint filtering
- Target selection
- Inventory scoring
- Color serialization
- Corrupt configuration recovery
- Regex validation
- External integration restrictions

### Game Tests

Test where practical:

- AutoTool
- AutoArmor
- AutoTotem
- Block filters
- Entity filters
- Scaffold selection
- Nuker selection
- ChestSteal order
- World join cleanup
- World leave cleanup

### Manual Smoke Tests

Document tests for:

- First launch
- No configuration directory
- ClickGUI open and close
- Every setting type
- Profile save and load
- World switching
- Server disconnect
- Resource reload
- Window resize
- Fullscreen switching
- Death
- Respawn
- Dimension travel
- Panic reset
- No internet connection
- Malformed configuration
- Common Fabric performance mods
- One-hour continuous runtime
- Chat filters
- Chat macros
- AutoReconnect
- Local waypoint separation by server

---

## 25. Continuous Integration

GitHub Actions should:

1. Set up Java.
2. Validate Gradle wrapper.
3. Compile.
4. Run unit tests.
5. Run applicable GameTests.
6. Run formatting checks.
7. Run static analysis.
8. Run networking architecture checks.
9. Build the JAR.
10. Generate checksums.
11. Generate dependency report.
12. Upload build artifacts.

Only publish releases from tagged commits.

---

## 26. Coding Standards

Use:

- Four-space indentation
- Clear names
- Immutable fields where practical
- Constructor validation
- Records for immutable data
- Java logging
- Consistent nullability annotations
- Small focused classes
- Dependency injection where useful

Avoid:

- Giant managers
- Global mutable state
- Reflection-based module discovery
- Magic numbers
- Catching `Throwable`
- Empty catch blocks
- Mixins with unrelated responsibilities
- GUI logic inside module business logic
- IDs derived from display names
- Excessive static utility classes
- Premature cross-version abstraction

---

## 27. Documentation Requirements

### README.md

Include:

- Project purpose
- Screenshots
- Installation
- Supported Minecraft version
- Fabric requirement
- Feature overview
- Privacy statement
- No-backend statement
- Multiplayer server rules warning
- Build instructions
- License
- Contributing link

### docs/architecture.md

Explain:

- Module lifecycle
- Event bus
- Configuration
- Version-specific hooks
- Render architecture
- Failure isolation
- External integration boundaries

### docs/modules.md

For each module include:

- ID
- Category
- Description
- Settings
- Limitations
- Acceptance criteria
- Test coverage

### docs/networking.md

For every optional external request include:

- Purpose
- Hostnames
- Trigger
- Data sent
- Data received
- Storage behavior
- Disable instructions
- Local fallback

### docs/version-porting.md

Document:

- Version adapters
- Mixins
- Mapping changes
- Rendering changes
- Common porting failures
- Validation checklist

---

## 28. Licensing

Use a permissive open-source license unless another license is intentionally chosen.

Recommended:

- MIT
- Apache-2.0

Do not include copied Aristois assets or code.

All third-party dependencies and bundled assets must have compatible licenses.

---

## 29. Milestone Plan

### Milestone 0: Bootstrap

Deliver:

- Fabric project
- Gradle build
- Client entrypoint
- Logging
- CI
- Basic README

### Milestone 1: Core Systems

Deliver:

- Module API
- Settings API
- Event bus
- Config system
- Keybinds
- Commands
- Notifications
- Dummy module

### Milestone 2: GUI and HUD

Deliver:

- ClickGUI
- Search
- Settings editor
- Theme editor
- HUD editor
- Active modules list

### Milestone 3: Local Data Systems

Deliver:

- Profiles
- Friends
- Waypoints
- Macros
- Panic system

### Milestone 4: Basic Modules

Deliver:

- Fullbright
- AntiBlind
- BetterCrosshair
- AutoSprint
- AutoWalk
- AutoSneak
- AutoEat
- AutoTool
- FastPlace

### Milestone 5: Chat Utilities

Deliver:

- BetterChat
- ChatColor
- ChatMute
- ChatFilter
- ChatTimestamps
- ChatSpammer
- ChatSuffix
- ChatPrefix
- MentionNotifier
- AutoReply
- AntiSpam
- PrivateMessageHelper

### Milestone 6: Render Utilities

Deliver:

- EntityESP
- BlockESP
- Tracers
- Trajectories
- Breadcrumbs
- TrueSight
- Radar
- XRay
- StorageESP

### Milestone 7: Automation

Deliver:

- AutoArmor
- AutoEject
- AutoFish
- AutoReconnect
- AutoTotem
- ChestSteal
- InventoryManager
- BuilderAssist

### Milestone 8: Advanced Movement

Deliver:

- NoSlow
- FastLadders
- Step
- Speed
- BunnyHop
- Flight
- NoFall
- ExtraElytra
- Scaffold
- Timer

### Milestone 9: Combat

Deliver:

- TriggerBot
- BowAimAssist
- CriticalAssist
- AutoPotion
- TargetHUD
- KillAura
- ReachDisplay
- AntiBot

### Milestone 10: Stabilization

Deliver:

- Performance profiling
- Crash isolation review
- Configuration migration tests
- Documentation
- Security review
- Release packaging
- Version 1.0 release candidate

---

## 30. Codex Working Instructions

Codex should work in small, reviewable commits.

For each task:

1. Read the relevant architecture document.
2. Inspect existing interfaces before adding new abstractions.
3. Add or update tests.
4. Implement the smallest complete feature.
5. Run the full test suite.
6. Run formatting.
7. Update documentation.
8. Summarize changed files and known limitations.

Do not:

- Implement multiple unrelated modules in one commit.
- Add networking without documenting it.
- Introduce a backend dependency.
- Copy code from proprietary clients.
- Add anti-cheat bypass presets.
- Add telemetry.
- Add hidden behavior.
- Add arbitrary code execution.
- Silently weaken tests.

---

## 31. First Codex Task

Use the following as the first task prompt:

```text
Initialize the Helikon Fabric client project using the repository structure and architecture in PLAN.md.

Implement only the bootstrap and core skeleton:

1. Fabric client entrypoint
2. Gradle Kotlin DSL build
3. ModuleCategory enum
4. Abstract Module class
5. ModuleRegistry
6. Basic BooleanSetting and NumberSetting
7. Minimal event bus
8. JSON configuration loader and saver
9. Right Shift keybind that opens a placeholder ClickGUI
10. One test module named FullbrightStub
11. JUnit tests for module lifecycle and setting validation
12. GitHub Actions build workflow
13. README with build instructions

Do not implement real gameplay modules yet.
Do not add external networking.
Do not add telemetry.
Do not require a server component.
Run the tests and provide a summary of all generated files.
```

---

## 32. Definition of Version 1.0

Version 1.0 is complete when:

- Core framework is stable.
- ClickGUI is usable.
- HUD editor is usable.
- Profiles, friends, waypoints, and macros work.
- At least 40 modules are implemented.
- Chat utilities work through normal Minecraft chat.
- No Helikon-owned backend is required.
- External integrations are optional and documented.
- Configurations migrate safely.
- Panic restores modified state.
- Disabled modules have negligible overhead.
- CI builds and tests the project.
- Documentation is complete.
- Release binaries match public source.
