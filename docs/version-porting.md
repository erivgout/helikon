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
5. Revalidate FastPlace's `Minecraft.rightClickDelay` accessor, FastBreak and
   Nuker's `MultiPlayerGameMode.destroyDelay` accessor, Attack/Use key mappings,
   `Minecraft.hitResult`, `BlockHitResult`, loaded-block and player interaction
   range checks, `Level.clip`, hotbar/tool APIs, and
   `MultiPlayerGameMode.startDestroyBlock`. Keep FastBreak a cooldown reducer,
   Nuker a loaded-target bounded normal interaction, and neither an interaction
   injector or packet builder.
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
   changing BuilderAssist or BlockSelection. Keep BuilderAssist plans bounded
   and every placement a normal held-block use request; keep BlockSelection a
   single render-only current-target overlay.
23. Revalidate `LocalPlayer` input/use-speed methods, `Entity` block-speed,
   stuck-in-block, and step-height methods, plus the relevant use-animation,
   sneaking attribute, honey/soul-sand/cobweb state APIs before changing
   NoSlow or Step. Revalidate `Entity.move`, `MoverType.SELF`, loaded-chunk
   checks, and cactus collision-shape bounds before changing AntiCactus. Keep
   every mixin restricted to the local player and do not suppress damage.
24. Revalidate `KeyboardInput.tick`, local input records, `LocalPlayer` water
   state/facing/block position, loaded-block checks, replaceability, upper-face
   sturdiness, climbable/fall-flying state, movement vectors, velocity setters,
   permitted `Abilities`, and ability-update path before changing WaterJump,
   FastLadders, Speed, BunnyHop, Flight, NoFall, or ExtraElytra. WaterJump must
   remain a loaded-edge ordinary Jump request with no direct position update.
   Retain bounded settings and server-authority warnings.
25. Revalidate `DeltaTracker.Timer.advanceGameTime` and its target-ms-per-tick
   provider before changing Timer. Preserve its safe range and reset-on-leave
   behavior; never use it to fabricate or alter packets.
26. Revalidate `Minecraft.setCameraEntity`, client-only camera-entity
   construction, local mouse-turn routing, and input suppression before
   changing Freecam. It must not add an entity to the level or move the player.
27. Revalidate hotbar selection, held-block tests, loaded target/support
   checks, normal `useItemOn`, rotation, and use-cooldown APIs before changing
    Scaffold. Keep player-provided blocks and ordinary interaction requirements.
28. Revalidate `Minecraft.hitResult`, rendered-entity iteration, player tab-list
    lookup, `LivingEntity` health/armor/effect/line-of-sight APIs,
    `LocalPlayer.getAttackStrengthScale`, `MultiPlayerGameMode.attack`, normal
    held-potion `useItem`, potion components/effects, bow rotation setters, and
    Gizmo cuboids before changing combat modules. Preserve the single ordinary
    Helikon attack-per-tick guard, line-of-sight rule, user-held bow rule, and
    no-packet policy.
29. Revalidate `KeyboardInput.tick` fresh input records for Twerk, plus
    `LocalPlayer.swing(InteractionHand)`, `Options.setModelPart`, and
    `Options.isModelPartEnabled` for Annoy and SkinBlinker. Preserve Twerk's
    screen suppression, Annoy's ordinary bounded swing path, and SkinBlinker's
    session-only no-save/no-broadcast restoration behavior.
30. Revalidate `Inventory.getNonEquipmentItems`, its 36-item/9-hotbar layout,
    `ItemStack` damage methods, `LivingEntity.getItemBySlot`, and HUD
    `GuiGraphicsExtractor.item`/`itemDecorations` calls before changing the
    local inventory preview or durability warnings. Revalidate the local
    waypoint-location adapter at death/disconnect boundaries; retain the
    session-only, no-waypoint/no-file coordinate policy.
31. Run the manual smoke test in an empty profile and with no internet access.
32. Document compatibility changes and retained limitations before release.
33. Re-run `check releaseBundle`, inspect the generated checksum and resolved
    dependency report, and repeat the focused live-client smoke checks before
    packaging a release for the target version.

Do not add mapping-specific logic to module classes. Keep version-sensitive code
at Fabric/event/render integration boundaries.
