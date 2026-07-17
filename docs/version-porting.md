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
10. Revalidate Fabric's `LevelRenderEvents.BEFORE_GIZMOS` phase and the 26.2
   `Gizmos` box/line APIs before changing EntityESP, BlockESP, Tracers, or
   Breadcrumbs. Keep visualizers in that supported render phase; do not
   replace them with legacy OpenGL calls or stateful shader hooks.
11. Revalidate `ClientLevel.entitiesForRendering`, loaded-chunk checks,
   `BlockState.getBlock`, registry ID lookup, and local player/entity bounding
   box APIs used by the bounded BlockESP scanner and entity visualizers.
12. Revalidate projectile package names, `getDeltaMovement`, local block
   `ClipContext`, and the drag/gravity bytecode for each Trajectories family.
   Preserve its observed-in-flight-only behavior and stop prediction at the
   first local block hit.
13. Revalidate `Entity.isInvisibleTo`, HUD extraction, and the entity state
   available to TrueSight/Radar. Keep TrueSight as a local overlay unless a
   separately verified reversible model-render path is introduced.
14. Revalidate `SectionCompiler.compile`, `ModelBlockRenderer` face/quad
   output hooks, `BakedQuad.MaterialInfo`, and `LevelRenderer.invalidateCompiledGeometry`
   before changing XRay. Confirm it restores normal chunk geometry on disable.
15. Revalidate `BlockState.hasBlockEntity`, loaded-chunk checks, and frustum
   visibility before changing StorageESP's bounded scan/render adapter.
16. Revalidate GUI entity-state extraction, `HumanoidRenderState` equipment
   fields, and `GuiGraphicsExtractor.entity` before changing MiniPlayer. Never
   replace its temporary render-state change with inventory/equipment mutation.
17. Revalidate `LivingEntity.getHealth`, `hurtTime`, entity IDs, text Gizmos,
   and frustum visibility before changing DamageIndicators.
18. Revalidate `InventoryMenu` slot constants, `Slot` inventory ownership,
   `ContainerInput`, `MultiPlayerGameMode.handleContainerInput`, and the
   carried-cursor/menu checks before changing AutoArmor, AutoEject, AutoTotem,
   InventoryManager, or ChestSteal. Keep the adapter on Minecraft's normal
   container-input path; do not construct inventory packets.
19. Revalidate `DataComponents.EQUIPPABLE`, item attribute modifiers, Binding
   Curse enchantment lookup, offhand equipment, item rarity, and durability
   APIs used by the local inventory policies.
20. Revalidate `FishingHook`'s bite/open-water fields, selected rod handling,
   and `MultiPlayerGameMode.useItem` before changing AutoFish. Keep the mixin
   accessor narrow and use normal held-item interaction only.
21. Revalidate Fabric client play connect/disconnect callbacks,
   `DisconnectedScreen`, `ConnectScreen.startConnecting`, `ServerAddress`, and
   local-server checks before changing AutoReconnect. Preserve its cancel,
   explicit-leave, and bounded-attempt safeguards.
22. Revalidate `Minecraft.hitResult`, player yaw/direction, `BlockHitResult`,
   `BlockState.canBeReplaced`, loaded-chunk/build-height checks,
   `MultiPlayerGameMode.useItemOn`, `rightClickDelay`, and Gizmo cuboids before
   changing BuilderAssist. Keep plans bounded and every placement a normal
   held-block use request.
23. Run the manual smoke test in an empty profile and with no internet access.
24. Document compatibility changes and retained limitations before release.

Do not add mapping-specific logic to module classes. Keep version-sensitive code
at Fabric/event/render integration boundaries.
