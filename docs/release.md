# Release process

The current version is `1.4.0`. Tagged `v*` commits are built and published by
the GitHub release workflow after its checks pass.

## Build the bundle

```powershell
Push-Location vendor/baritone
.\gradlew.bat :fabric:remapJar --console=plain
Pop-Location
.\gradlew.bat check releaseBundle
```

On a clean checkout, build the vendored Baritone component first with its
pinned wrapper as shown above. The resulting archive in `build/releases/`
contains the distributable JAR(s),
Helikon and Baritone corresponding source, both projects' licenses and
provenance notes, `README`, `CHANGELOG`, SHA-256 checksums, and the resolved
client runtime dependency report. Verify the checksum sidecar before sharing a
binary.

## Final release gate

1. Confirm the full test suite, `verifySourceStyle`, and
   `verifyClientOnlyArchitecture` pass.
2. Complete the manual smoke and no-internet checks in `testing.md`, including
   world transitions, panic, malformed configuration, and one-hour runtime.
3. Complete the profiling procedure in `performance.md` and review the
   generated dependency report against current advisories.
4. Review `security-review.md`, README/module limitations, and the source/JAR
   contents. Confirm no proprietary assets, backend code, telemetry, or server
   entrypoint is present.
5. Tag only the reviewed commit intended for publication (for example
   `v1.0.1`). The release workflow publishes the installable JAR, sources JAR,
   auditable bundle, checksums, and dependency report from that tagged commit.
