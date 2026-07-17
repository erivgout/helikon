# Version Porting

Helikon currently targets Minecraft 26.2 using Mojang official mappings, Fabric
Loader 0.19.3, Fabric API 0.155.0+26.2, Fabric Loom 1.17.14, and Java 25.

When porting:

1. Update all pinned versions together from Fabric's official release metadata.
2. Build and run the full test suite with the target Java version.
3. Revalidate client lifecycle, key mapping, screen, rendering, and mixin APIs.
   In particular, verify AntiBlind's `FogRenderer.setupFog`,
   `LightmapRenderStateExtractor.extract`, and `Hud` extraction targets before
   release, plus AutoWalk/AutoSneak's `KeyboardInput.tick` target and the
   `ClientInput.moveVector` accessor; these client-only hooks must remain
   narrowly scoped.
4. Revalidate AutoTool's ordinary mining-state (`MultiPlayerGameMode`), hotbar
   (`Inventory`), block-state, and `ItemStack` destroy-speed/durability APIs.
5. Revalidate FastPlace's `Minecraft.rightClickDelay` accessor, Use key mapping,
   held-item, and `BlockItem` APIs. Keep it a cooldown reducer rather than an
   interaction injector.
6. Revalidate AutoEat's `DataComponents.FOOD`, `FoodProperties`, food-data,
   health, hotbar, hurt-time, configured-key, and normal Use-key APIs. Preserve
   physical keyboard, mouse, and scancode Use input; keep selection and use
   input local and do not replace the ordinary client interaction path.
7. Revalidate Fabric's client `MODIFY_CHAT` callback. Preserve local commands
   and protected command-like text before transforming ordinary chat, and keep
   all behavior on Minecraft's normal send path.
8. Revalidate Fabric's incoming chat/game allow callbacks and the 26.2
   `Component`/`TranslatableContents` APIs. If category extraction breaks,
   prefer showing messages over hiding them.
9. Revalidate `ClientPacketListener.sendChat` and its 256-character ordinary
   chat limit. Keep ChatSpammer on that normal send path and retain its local
   delay/cap safeguards.
10. Run the manual smoke test in an empty profile and with no internet access.
11. Document compatibility changes and retained limitations before release.

Do not add mapping-specific logic to module classes. Keep version-sensitive code
at Fabric/event/render integration boundaries.
