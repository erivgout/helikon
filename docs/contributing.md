# Contributing

Helikon is a clean-room, client-only Fabric mod. Contributions are welcome
under the constraints below; they exist to keep the project safe to use,
review, and republish.

## Ground rules

- Never copy, decompile, translate, or closely imitate proprietary clients.
  All code, assets, names, and text must be original.
- Client-only: no server entrypoints, no server-side requirements, and no
  Helikon-owned backend of any kind.
- No networking in core code. The Gradle `verifyClientOnlyArchitecture` check
  rejects HTTP/WebSocket usage; optional integrations require the isolation
  and documentation rules in [networking.md](networking.md).
- No anti-cheat bypass presets, malformed packets, telemetry, hidden
  behavior, or arbitrary code execution.
- Keep decision logic Minecraft-free and unit-tested; only thin adapters and
  narrowly scoped mixins may touch Minecraft classes. Verify every Minecraft
  API against the mapped jars before use (see
  [version-porting.md](version-porting.md)).

## Workflow

1. Read [architecture.md](architecture.md), [configuration.md](configuration.md),
   and [testing.md](testing.md) before changing core systems.
2. Add or update tests with every behavior change.
3. Run the full local gate before submitting:

   ```powershell
   .\gradlew.bat check
   ```

4. Update the affected documentation (`docs/modules.md` rows, README status,
   and the manual checklists in `testing.md`) in the same change.
5. Keep commits small and single-purpose, with an honest summary of changed
   files and known limitations.

## Style

- Java, four-space indentation, clear names, immutable fields where practical,
  records for immutable data, constructor validation.
- No reflection-based discovery, no catching `Throwable`, no empty catch
  blocks, no GUI logic inside module business logic.
