# Security review — 1.0.0-rc.1

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
- Module lifecycle and periodic adapters use `ModuleRegistry` failure isolation;
  a failing module is disabled and its normal cleanup is attempted without
  crashing unrelated modules.
- No module constructs malformed packets, bypasses anti-cheat, downloads
  assets, executes arbitrary code, opens an external socket, or emits telemetry.
- The release bundle contains checksums, a resolved dependency report, source,
  license, and local release notes for human review.

## Residual risks and release gate

- Minecraft and Fabric APIs can change across mappings; run the documented
  version-porting and manual smoke checks before tagging.
- Client-side decisions remain subject to server correction and server rules.
- AntiBot and combat target data are local heuristics/observations, not trusted
  identity or server authority.
- Dependencies are pinned but still inherit upstream security risk. Review the
  generated dependency report and upstream advisories before a final tag.

Publish only from a reviewed, signed/tagged source commit after the full Gradle
check, release bundle, manual smoke, performance, and no-internet checks pass.
