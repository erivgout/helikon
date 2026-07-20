# Helikon

Helikon is a powerful open-source Fabric utility client for Minecraft Java
Edition 26.2, with more than 80 toggleable modules, a searchable ClickGUI, a
customizable HUD, profiles, friends, waypoints, and macros.

Helikon is not affiliated with Aristois and contains no copied code, assets,
branding, or configuration formats from that or any other project.

## Features

- **Render** — Fullbright, EntityESP (outline/box/glow/shader), BlockESP,
  StorageESP, XRay, Tracers, Trajectories, TrueSight, Radar, Breadcrumbs,
  BetterNametags, BetterCrosshair, DamageIndicators, MiniPlayer, and more.
- **Movement** — AutoSprint, AutoWalk, AutoSneak, AutoParkour, NoSlow, Jesus,
  Spider, Step, Speed, BunnyHop, Flight, Boat Flight, Freecam, NoFall, ExtraElytra, Scaffold,
  Timer, and more.
- **Player automation** — AutoEat, AutoTool, AutoArmor, AutoTotem, AutoFish,
  AutoEject, AutoPotion, AutoReconnect, InventoryManager, ChestSteal,
  BuilderAssist, Nuker.
- **World navigation** — an embedded, source-vendored Baritone 1.15.0 port
  with its own persistent ClickGUI section, full command field, visible
  path/goal/block-action rendering, Baritone-backed waypoints, mining,
  exploration, following, farming, and building commands.
- **Chat** — BetterChat, ChatFilter, ChatMute, ChatColor, ChatTimestamps,
  ChatHistory, ChatSpammer, ChatPrefix/Suffix, MentionNotifier, AutoReply,
  Announcer, AntiSpam, PrivateMessageHelper, LocalTranslator.
- **Combat** — KillAura, TriggerBot, BowAimAssist, CriticalAssist, AutoPotion,
  TargetHUD, ReachDisplay, AntiBot.
- **Local systems** — ClickGUI with an Active view, themes, settings scrolling, and keyboard navigation, HUD
  editor, per-server profiles, friends, waypoints, macros, panic key, and a
  local-only debug overlay.

Every module is honest about server authority: client-side effects are local,
nothing sends malformed packets, and there are no anti-cheat bypass presets.
See [modules.md](docs/modules.md) for every module's settings, limitations,
and test coverage, and [usage.md](docs/usage.md) for the ClickGUI, HUD
editor, keybind, and dot-command reference.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.0+26.2
- Java 25

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.19.3 or newer for
   Minecraft Java Edition 26.2.
2. Place the Fabric API JAR and the built `helikon-<version>.jar` into your
   `.minecraft/mods` folder. Do not install a second standalone Baritone JAR;
   Helikon already embeds it.
3. Launch the Fabric profile. Press **Right Shift** in game to open the
   ClickGUI, or type `.help` in chat for the local command list.

## Multiplayer warning

Helikon is client-only and must not be installed on servers. Use it only
where the server's rules permit client modifications; Minecraft servers
remain authoritative, and no client-side feature can guarantee a server-side
result.

## Privacy and no-backend policy

- All data (settings, profiles, friends, waypoints, macros, opt-in chat
  history) is stored locally under `.minecraft/config/helikon/`.
- No Helikon-operated backend, telemetry, analytics, account service, remote
  feature flags, or cloud synchronization exists.
- The current release performs no external network requests at all.

See [privacy.md](docs/privacy.md) and [networking.md](docs/networking.md).

## Building from source

```powershell
.\gradlew.bat build        # remapped mod JAR in build/libs
.\gradlew.bat runClient    # development client
.\gradlew.bat check releaseBundle  # tests, checks, and auditable release zip
```

The build automatically compiles the separately licensed source under
`vendor/baritone`; it does not download a Baritone binary. The release bundle
in `build/releases` contains the JAR, Helikon and Baritone sources and licenses,
SHA-256 checksums, and the resolved dependency report. See
[release.md](docs/release.md) for the release gate and
[security-review.md](docs/security-review.md) for its security review scope.

## Status

`1.3.0` is the current stable release. The automated test suite, style and
client-only architecture checks, and Fabric client game tests pass. The
additional live-client smoke checklists in [testing.md](docs/testing.md) remain
recommended for real-world testing on a disposable profile.

## Contributing

Read [contributing.md](docs/contributing.md) for the ground rules, and
[architecture.md](docs/architecture.md), [configuration.md](docs/configuration.md),
and [testing.md](docs/testing.md) before changing core systems. The long-term
roadmap and policies live in [PLAN.md](PLAN.md) and [RULES.md](RULES.md).

## Contributors

- Eric L Wheeler — creator and maintainer.
- OpenAI Codex — AI-assisted implementation and testing.

## License

Helikon's original code is released under the [MIT License](LICENSE). The
embedded Baritone component is separately licensed under LGPL-3.0; its
license, provenance, and port notes are in
[`vendor/baritone/HELIKON_PORT.md`](vendor/baritone/HELIKON_PORT.md).
