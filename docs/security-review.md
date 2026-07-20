# Security review — 1.4.1

## Scope

This review covers Helikon source, local stores, build tasks, and the declared
client-only distribution boundary. It does not assess Minecraft, Fabric,
Gradle, the operating system, or server plugins.

## Confirmed controls

- `fabric.mod.json` declares a client entrypoint only; there is no Helikon
  server component.
- `verifyClientOnlyArchitecture` runs under Gradle `check` and rejects common
  external-network API tokens in Java source while checking the client-only
  descriptor.
- `verifySourceStyle` rejects Java tabs and trailing whitespace so review diffs
  remain readable.
- Local JSON stores validate their schemas/values, write through temporary files
  with backup replacement, and preserve malformed input as a recoverable
  `.corrupt-<timestamp>.json` file. Profile import paths are fixed below the
  Helikon configuration directory and use safe name tokens.
- Baritone waypoint names are treated as external data at the repository
  boundary. Blank or unrestricted names receive bounded display-safe forms,
  while structurally invalid entries are isolated instead of failing HUD
  rendering.
- Module lifecycle and periodic adapters use `ModuleRegistry` failure isolation;
  a failing module is disabled and its normal cleanup is attempted without
  crashing unrelated modules.
- No module constructs malformed packets, bypasses anti-cheat, downloads
  assets, executes arbitrary code, opens an external socket, or emits telemetry.
- Domain Expansion uses loaded client observations, full-block and hitbox
  checks, ordinary inventory/container interactions, and Minecraft's normal
  block-use path. Placement reach, support, visibility, acceptance, and world
  updates remain server-authoritative; retries and activation attempts are
  bounded.
- SeedCracker reads only already-loaded slime entities and the seed of a
  locally owned integrated world. Its exact slime predicate and candidate
  filter are local, per-tick/range/result bounded, and session-only; it sends
  no packets, requests no chunks, writes no evidence file, and opens no
  external connection.
- Debug Overlay retains only transient in-process timing/cache/event/save facts
  while explicitly enabled; it is not a telemetry, profiler-upload, or
  persistent diagnostic system.
- Baritone is built from pinned, vendored source into a distinct nested JAR.
  Its upstream commits, Helikon port delta, LGPL license, and corresponding
  source are included in the auditable release bundle; no runtime binary
  download or remote module loading path exists.
- The release bundle contains checksums, a resolved dependency report, Helikon
  and Baritone source/licenses, and local release notes for human review.

## Residual risks and release gate

- Minecraft and Fabric APIs can change across mappings; run the documented
  version-porting and manual smoke checks before tagging.
- Client-side decisions remain subject to server correction and server rules.
- AntiBot and combat target data are local heuristics/observations, not trusted
  identity or server authority.
- Multiplayer SeedCracker evidence cannot prove spawn provenance, can contain
  false positives, constrains only the lower 48-bit structure seed, and is not
  full 64-bit seed recovery. The UI and module documentation state those
  limits and expose manual evidence removal.
- Dependencies are pinned but still inherit upstream security risk. Review the
  generated dependency report and upstream advisories before a final tag.

Publish only from a reviewed, signed/tagged source commit after the full Gradle
check, release bundle, manual smoke, performance, and no-internet checks pass.
