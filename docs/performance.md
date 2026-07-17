# Performance and profiling

Helikon keeps high-frequency work bounded: block and storage scans have a
per-tick budget and fixed cache cap; entity overlays cap candidates per frame;
trajectory simulation caps steps and projectiles; chat, macro, and automation
queues are bounded. Disabled modules return before their platform adapters do
meaningful work.

## Local debug overlay

The **Debug Overlay** module is disabled by default, so `ModuleRegistry` does
not call `System.nanoTime()` or allocate timing rows during normal guarded
module work. While explicitly enabled, it records the last local `tick`,
bounded `scan`, and `render` duration for each module. Its 10-row local pages
also show BlockESP and StorageESP cache counts, event-bus subscriber count, and
the in-memory global configuration-save state. It prebuilds display components
and width only once per client tick, leaving the per-frame HUD extraction path
to submit cached values. The overlay writes no diagnostic file and has no
network or telemetry path. Disable it (or use panic) before a normal
performance capture.

## Release-candidate profiling procedure

Profile a production-like client with the same Java 25 and Fabric versions as
the release candidate. Do not profile on a public server unless its rules allow
it.

1. Run `./gradlew.bat runClient` with no Helikon modules enabled and record a
   five-minute baseline in a representative local/test world.
2. Use Java Flight Recorder or the built-in Minecraft profiler to capture a
   five-minute run with the bounded render modules enabled at their defaults.
   Repeat separately for automation, advanced movement, and combat modules.
3. Exercise world join/leave, dimension travel, GUI resize/fullscreen, resource
   reload, chat activity, inventory screens, and panic. Check for sustained
   allocations, growing retained collections, or a module tick/render callback
   dominating the client thread.
4. Complete one continuous one-hour local/test session with typical enabled
   modules. Confirm that bounded caches do not grow past their documented caps
   and that disabling modules returns client cost near the baseline.
5. Record the hardware, JVM flags, world scenario, enabled modules, capture
   location, and any regression in the release issue or tag notes. Do not add a
   profiler capture containing account/session data to the repository.

No automated benchmark is treated as portable across Minecraft updates or
hardware. The release gate is an evidence-backed regression review, not an
absolute frame-rate claim.
