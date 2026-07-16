# Version Porting

Helikon currently targets Minecraft 26.2 using Mojang official mappings, Fabric
Loader 0.19.3, Fabric API 0.155.0+26.2, Fabric Loom 1.17.14, and Java 25.

When porting:

1. Update all pinned versions together from Fabric's official release metadata.
2. Build and run the full test suite with the target Java version.
3. Revalidate client lifecycle, key mapping, screen, rendering, and mixin APIs.
   In particular, verify AntiBlind's `FogRenderer.setupFog`,
   `LightmapRenderStateExtractor.extract`, and `Hud` extraction targets before
   release, plus AutoWalk's `KeyboardInput.tick` target; these client-only
   hooks must remain narrowly scoped.
4. Run the manual smoke test in an empty profile and with no internet access.
5. Document compatibility changes and retained limitations before release.

Do not add mapping-specific logic to module classes. Keep version-sensitive code
at Fabric/event/render integration boundaries.
