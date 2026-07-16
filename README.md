# Helikon

Helikon is a clean-room, open-source Fabric utility client for Minecraft Java
Edition. This repository currently contains the **bootstrap/core skeleton**:
the client entrypoint, modular lifecycle API, basic settings, local JSON
configuration, an internal event bus, a Right Shift placeholder GUI, and tests.

It is not affiliated with Aristois, and it does not contain copied code, assets,
branding, or configuration formats from that project.

## Status

The first milestone deliberately does **not** implement gameplay automation,
combat tools, packet manipulation, external networking, telemetry, a custom
backend, or a server-side component. `FullbrightStub` exists only to exercise
the registry and settings architecture; it does not change game brightness.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.0+26.2
- Java 25

## Build

```powershell
.\gradlew.bat build
```

The remapped mod JAR is produced in `build/libs`. To start a development client:

```powershell
.\gradlew.bat runClient
```

Press Right Shift in the client to open the bootstrap ClickGUI placeholder.

## Local data and privacy

Helikon stores its initial global configuration locally at:

```text
.minecraft/config/helikon/global.json
```

No Helikon-operated backend, telemetry, account service, remote feature flags,
or cloud synchronization is used. See [networking.md](docs/networking.md) and
[privacy.md](docs/privacy.md).

## Multiplayer

Use only on servers whose rules permit client modifications. Minecraft servers
remain authoritative; future client-side features must not claim to guarantee
server-side behavior.

## Development

Read [architecture.md](docs/architecture.md), [configuration.md](docs/configuration.md),
and [testing.md](docs/testing.md) before changing core systems. Contributions
should include tests and keep all behavior local-only unless an explicitly
documented optional integration is approved.

## License

Helikon is released under the [MIT License](LICENSE).
