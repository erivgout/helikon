package dev.helikon.client.render;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.combat.BowAimAssist;
import dev.helikon.client.module.miscellaneous.LocalCosmetics;
import dev.helikon.client.module.render.BaseFinder;
import dev.helikon.client.module.render.BlockEsp;
import dev.helikon.client.module.render.BetterNametags;
import dev.helikon.client.module.render.Breadcrumbs;
import dev.helikon.client.module.render.Chams;
import dev.helikon.client.module.render.ChamsRenderAccess;
import dev.helikon.client.module.render.DamageIndicators;
import dev.helikon.client.module.render.EntityEsp;
import dev.helikon.client.module.render.EntityEspMode;
import dev.helikon.client.module.render.EntityEspRenderAccess;
import dev.helikon.client.module.render.Explosions;
import dev.helikon.client.module.render.ProjectileWarning;
import dev.helikon.client.module.render.ProjectilePreview;
import dev.helikon.client.module.render.StorageEsp;
import dev.helikon.client.module.render.Trajectories;
import dev.helikon.client.module.render.Tracers;
import dev.helikon.client.module.render.TrueSight;
import dev.helikon.client.module.world.BuilderAssist;
import dev.helikon.client.module.world.BlockSelection;
import dev.helikon.client.module.world.MinecraftBuilderAssistAccess;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Narrow 26.2 client adapter for the supported Fabric level Gizmo phase.
 * It renders only already-known local entities and bounded cached block results.
 */
public final class MinecraftWorldVisualizationRenderer {
    private static final int MAXIMUM_CACHED_BLOCKS = 512;
    private static final double NAMETAG_BASE_OFFSET = 0.35D;
    /** Vanilla debug-gizmo spacing (0.25 at scale 0.5) at the default 0.32 text scale. */
    private static final double NAMETAG_LINE_SPACING = 0.17D;
    private static final TrajectorySimulator.Physics ARROW_PHYSICS = new TrajectorySimulator.Physics(
            0.05D, 0.99D, TrajectorySimulator.GravityOrder.AFTER_DRAG, TrajectorySimulator.UpdateTiming.AFTER_MOVE
    );
    private static final TrajectorySimulator.Physics THROWN_PHYSICS = new TrajectorySimulator.Physics(
            0.03D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG, TrajectorySimulator.UpdateTiming.BEFORE_MOVE
    );
    private static final TrajectorySimulator.Physics POTION_PHYSICS = new TrajectorySimulator.Physics(
            0.05D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG, TrajectorySimulator.UpdateTiming.BEFORE_MOVE
    );

    private final ModuleRegistry modules;
    private final FriendManager friends;
    private final EntityEsp entityEsp;
    private final Chams chams;
    private final BetterNametags betterNametags;
    private final BaseFinder baseFinder;
    private final BlockEsp blockEsp;
    private final Tracers tracers;
    private final Trajectories trajectories;
    private final ProjectileWarning projectileWarning;
    private final ProjectilePreview projectilePreview;
    private final TrueSight trueSight;
    private final StorageEsp storageEsp;
    private final DamageIndicators damageIndicators;
    private final Breadcrumbs breadcrumbs;
    private final BuilderAssist builderAssist;
    private final BlockSelection blockSelection;
    private final BowAimAssist bowAimAssist;
    private final LocalCosmetics localCosmetics;
    private final Explosions explosions;
    private final BlockEspScanCursor blockCursor = new BlockEspScanCursor();
    private final BlockEspScanAnchor blockAnchor = new BlockEspScanAnchor();
    private final BlockEspCache blockCache = new BlockEspCache(MAXIMUM_CACHED_BLOCKS);
    private final BlockEspScanCursor storageCursor = new BlockEspScanCursor();
    private final BlockEspScanAnchor storageAnchor = new BlockEspScanAnchor();
    private final BlockEspCache storageCache = new BlockEspCache(MAXIMUM_CACHED_BLOCKS);
    private final BlockEspScanCursor baseFinderCursor = new BlockEspScanCursor();
    private final BlockEspScanAnchor baseFinderAnchor = new BlockEspScanAnchor();
    private final BlockEspCache baseFinderCache = new BlockEspCache(MAXIMUM_CACHED_BLOCKS);
    private ClientLevel observedLevel;
    private long observedBlockScanRevision = Long.MIN_VALUE;
    private long observedStorageScanRevision = Long.MIN_VALUE;
    private long observedBaseFinderScanRevision = Long.MIN_VALUE;
    private boolean blockScannerWasEnabled;
    private boolean storageScannerWasEnabled;
    private boolean baseFinderScannerWasEnabled;
    private boolean nativeOutlineWasInstalled;
    private boolean chamsWasInstalled;

    public MinecraftWorldVisualizationRenderer(ModuleRegistry modules, FriendManager friends, EntityEsp entityEsp,
                                                Chams chams, BetterNametags betterNametags,
                                                BaseFinder baseFinder,
                                                BlockEsp blockEsp, Tracers tracers, Trajectories trajectories,
                                                ProjectileWarning projectileWarning, ProjectilePreview projectilePreview,
                                                TrueSight trueSight,
                                                StorageEsp storageEsp, DamageIndicators damageIndicators,
                                                Breadcrumbs breadcrumbs, BuilderAssist builderAssist, BlockSelection blockSelection,
                                                BowAimAssist bowAimAssist, LocalCosmetics localCosmetics, Explosions explosions) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.friends = Objects.requireNonNull(friends, "friends");
        this.entityEsp = Objects.requireNonNull(entityEsp, "entityEsp");
        this.chams = Objects.requireNonNull(chams, "chams");
        this.betterNametags = Objects.requireNonNull(betterNametags, "betterNametags");
        this.baseFinder = Objects.requireNonNull(baseFinder, "baseFinder");
        this.blockEsp = Objects.requireNonNull(blockEsp, "blockEsp");
        this.tracers = Objects.requireNonNull(tracers, "tracers");
        this.trajectories = Objects.requireNonNull(trajectories, "trajectories");
        this.projectileWarning = Objects.requireNonNull(projectileWarning, "projectileWarning");
        this.projectilePreview = Objects.requireNonNull(projectilePreview, "projectilePreview");
        this.trueSight = Objects.requireNonNull(trueSight, "trueSight");
        this.storageEsp = Objects.requireNonNull(storageEsp, "storageEsp");
        this.damageIndicators = Objects.requireNonNull(damageIndicators, "damageIndicators");
        this.breadcrumbs = Objects.requireNonNull(breadcrumbs, "breadcrumbs");
        this.builderAssist = Objects.requireNonNull(builderAssist, "builderAssist");
        this.blockSelection = Objects.requireNonNull(blockSelection, "blockSelection");
        this.bowAimAssist = Objects.requireNonNull(bowAimAssist, "bowAimAssist");
        this.localCosmetics = Objects.requireNonNull(localCosmetics, "localCosmetics");
        this.explosions = Objects.requireNonNull(explosions, "explosions");
        this.blockEsp.setCacheClearer(this::resetBlockScanner);
        this.storageEsp.setCacheClearer(this::resetStorageScanner);
        this.baseFinder.setCacheClearer(this::resetBaseFinderScanner);
    }

    /** Samples the trail and advances the bounded block cache from the client tick bridge. */
    public void tick(Minecraft client) {
        Objects.requireNonNull(client, "client");
        if (client.level != observedLevel) {
            observedLevel = client.level;
            breadcrumbs.clearTrail();
            damageIndicators.clear();
            resetBlockScanner();
            resetStorageScanner();
            resetBaseFinderScanner();
            clearNativeOutlineTargets();
            clearChamsTargets();
        }
        if (client.level == null || client.player == null) {
            clearNativeOutlineTargets();
            clearChamsTargets();
            return;
        }
        if (entityEsp.isEnabled() && entityEsp.mode().usesNativeOutline()) {
            nativeOutlineWasInstalled = true;
            modules.runGuarded(entityEsp, "tick", () -> installNativeOutlineTargets(client.level, client.player));
        } else {
            clearNativeOutlineTargets();
        }
        if (chams.isEnabled()) {
            chamsWasInstalled = true;
            modules.runGuarded(chams, "tick", () -> installChamsTargets(client.level, client.player));
        } else {
            clearChamsTargets();
        }
        if (breadcrumbs.isEnabled()) {
            modules.runGuarded(breadcrumbs, "tick", () -> breadcrumbs.sample(
                    client.player.getX(), client.player.getY(), client.player.getZ(), System.currentTimeMillis()));
        }
        if (damageIndicators.isEnabled()) {
            modules.runGuarded(damageIndicators, "tick", () -> observeDamage(client.level, client.player,
                    System.currentTimeMillis()));
        }
        if (!blockEsp.isEnabled()) {
            if (blockScannerWasEnabled) {
                resetBlockScanner();
                blockScannerWasEnabled = false;
            }
        } else {
            blockScannerWasEnabled = true;
            if (!BlockEspScanPolicy.shouldScan(true, blockEsp.targetBlocks())) {
                clearBlockResults();
            } else {
                modules.runGuarded(blockEsp, "scan", () -> scanBlocks(client.level, client.player));
            }
        }
        if (!storageEsp.isEnabled()) {
            if (storageScannerWasEnabled) {
                resetStorageScanner();
                storageScannerWasEnabled = false;
            }
        } else {
            storageScannerWasEnabled = true;
            if (!BlockEspScanPolicy.shouldScan(true, storageEsp.targetBlocks())) {
                clearStorageResults();
            } else {
                modules.runGuarded(storageEsp, "scan", () -> scanStorage(client.level, client.player));
            }
        }
        if (!baseFinder.isEnabled()) {
            if (baseFinderScannerWasEnabled) {
                resetBaseFinderScanner();
                baseFinderScannerWasEnabled = false;
            }
        } else {
            baseFinderScannerWasEnabled = true;
            if (!BlockEspScanPolicy.shouldScan(true, baseFinder.targetBlocks())) {
                clearBaseFinderResults();
            } else {
                modules.runGuarded(baseFinder, "scan", () -> scanBaseEvidence(client.level, client.player));
            }
        }
    }

    /** Registered for Fabric's verified {@code BEFORE_GIZMOS} level-render phase. */
    public void render(LevelRenderContext context) {
        Objects.requireNonNull(context, "context");
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (entityEsp.isEnabled()) {
            modules.runGuarded(entityEsp, "render", () -> renderEntityEsp(client.level, client.player));
        }
        if (localCosmetics.isEnabled()) {
            modules.runGuarded(localCosmetics, "render", () -> renderLocalCosmetics(client.player));
        }
        if (tracers.isEnabled()) {
            modules.runGuarded(tracers, "render", () -> renderTracers(client.level, client.player, camera));
        }
        Frustum frustum = camera.cullFrustum;
        if (betterNametags.isEnabled()) {
            modules.runGuarded(betterNametags, "render", () -> renderBetterNametags(client.level, client.player, frustum));
        }
        if (trajectories.isEnabled()) {
            modules.runGuarded(trajectories, "render", () -> renderTrajectories(client.level, frustum));
        }
        if (explosions.isEnabled()) {
            modules.runGuarded(explosions, "render", () -> renderExplosions(client.level, frustum));
        }
        if (projectileWarning.isEnabled()) {
            modules.runGuarded(projectileWarning, "render",
                    () -> renderProjectileWarning(client.level, client.player, frustum));
        }
        if (projectilePreview.isEnabled()) {
            modules.runGuarded(projectilePreview, "render", () -> renderProjectilePreview(client.level, client.player));
        }
        if (trueSight.isEnabled()) {
            modules.runGuarded(trueSight, "render", () -> renderTrueSight(client.level, client.player, frustum));
        }
        if (blockEsp.isEnabled()) {
            modules.runGuarded(blockEsp, "render", () -> renderBlocks(client.level, client.player, camera));
        }
        if (storageEsp.isEnabled()) {
            modules.runGuarded(storageEsp, "render", () -> renderStorage(client.player, frustum));
        }
        if (baseFinder.isEnabled()) {
            modules.runGuarded(baseFinder, "render",
                    () -> renderBaseEvidence(client.player, camera));
        }
        if (damageIndicators.isEnabled()) {
            modules.runGuarded(damageIndicators, "render", () -> renderDamageIndicators(client.level, client.player,
                    frustum, System.currentTimeMillis()));
        }
        if (breadcrumbs.isEnabled()) {
            modules.runGuarded(breadcrumbs, "render", this::renderBreadcrumbs);
        }
        if (builderAssist.isEnabled()) {
            modules.runGuarded(builderAssist, "render", () -> renderBuilderPreview(client));
        }
        if (blockSelection.isEnabled()) {
            modules.runGuarded(blockSelection, "render", () -> renderBlockSelection(client));
        }
        if (bowAimAssist.isEnabled()) {
            modules.runGuarded(bowAimAssist, "render", () -> renderBowAimMarker(client.level));
        }
    }

    /** Current bounded local block-result count exposed only to the diagnostics HUD. */
    public int blockEspCacheSize() {
        return blockCache.size();
    }

    /** Current bounded local storage-result count exposed only to the diagnostics HUD. */
    public int storageEspCacheSize() {
        return storageCache.size();
    }

    private void scanBlocks(ClientLevel level, Player player) {
        if (blockEsp.scanRevision() != observedBlockScanRevision) {
            observedBlockScanRevision = blockEsp.scanRevision();
            resetBlockScanner();
        }
        BlockEspScanAnchor.Update anchor = blockAnchor.update(player.getBlockX(), player.getBlockY(), player.getBlockZ(),
                blockEsp.horizontalRange(), blockEsp.verticalRange(), blockCursor.isAtPassBoundary());
        if (anchor.changed()) {
            clearBlockResults();
        }
        BlockEspScanCursor.Region region = anchor.region();
        for (int index = 0; index < blockEsp.scanBudget(); index++) {
            BlockEspScanCursor.Position position = blockCursor.next(region);
            if (!level.isInsideBuildHeight(position.y()) || !level.hasChunk(position.x() >> 4, position.z() >> 4)) {
                blockCache.observe(position, false);
                continue;
            }
            BlockPos blockPosition = new BlockPos(position.x(), position.y(), position.z());
            BlockState state = level.getBlockState(blockPosition);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            blockCache.observe(position, blockEsp.targetBlocks().contains(blockId));
        }
    }

    private void scanStorage(ClientLevel level, Player player) {
        if (storageEsp.scanRevision() != observedStorageScanRevision) {
            observedStorageScanRevision = storageEsp.scanRevision();
            resetStorageScanner();
        }
        BlockEspScanAnchor.Update anchor = storageAnchor.update(player.getBlockX(), player.getBlockY(), player.getBlockZ(),
                storageEsp.horizontalRange(), storageEsp.verticalRange(), storageCursor.isAtPassBoundary());
        if (anchor.changed()) {
            clearStorageResults();
        }
        BlockEspScanCursor.Region region = anchor.region();
        for (int index = 0; index < storageEsp.scanBudget(); index++) {
            BlockEspScanCursor.Position position = storageCursor.next(region);
            if (!level.isInsideBuildHeight(position.y()) || !level.hasChunk(position.x() >> 4, position.z() >> 4)) {
                storageCache.observe(position, false);
                continue;
            }
            BlockPos blockPosition = new BlockPos(position.x(), position.y(), position.z());
            BlockState state = level.getBlockState(blockPosition);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            storageCache.observe(position, state.hasBlockEntity() && storageEsp.targetBlocks().contains(blockId));
        }
    }

    private void scanBaseEvidence(ClientLevel level, Player player) {
        if (baseFinder.scanRevision() != observedBaseFinderScanRevision) {
            observedBaseFinderScanRevision = baseFinder.scanRevision();
            resetBaseFinderScanner();
        }
        BlockEspScanAnchor.Update anchor = baseFinderAnchor.update(
                player.getBlockX(), player.getBlockY(), player.getBlockZ(),
                baseFinder.horizontalRange(), baseFinder.verticalRange(), baseFinderCursor.isAtPassBoundary());
        if (anchor.changed()) {
            clearBaseFinderResults();
        }
        BlockEspScanCursor.Region region = anchor.region();
        for (int index = 0; index < baseFinder.scanBudget(); index++) {
            BlockEspScanCursor.Position position = baseFinderCursor.next(region);
            if (!level.isInsideBuildHeight(position.y()) || !level.hasChunk(position.x() >> 4, position.z() >> 4)) {
                baseFinderCache.observe(position, false);
                continue;
            }
            BlockState state = level.getBlockState(new BlockPos(position.x(), position.y(), position.z()));
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            baseFinderCache.observe(position, baseFinder.targetBlocks().contains(blockId));
        }
    }

    private void observeDamage(ClientLevel level, Player localPlayer, long nowMillis) {
        EntityRenderFilter.Options options = damageIndicators.options();
        int capacity = damageIndicators.maximumTrackedEntities();
        PriorityQueue<LivingEntity> nearest = new PriorityQueue<>(capacity,
                (left, right) -> compareDamageCandidates(right, left, localPlayer));
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)
                    || !EntityRenderFilter.shouldRender(options, entityType(entity), false, entity == localPlayer,
                    entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            if (nearest.size() < capacity) {
                nearest.add(living);
            } else if (compareDamageCandidates(living, nearest.peek(), localPlayer) < 0) {
                nearest.poll();
                nearest.add(living);
            }
        }
        List<LivingEntity> selected = new ArrayList<>(nearest);
        selected.sort((left, right) -> compareDamageCandidates(left, right, localPlayer));
        List<DamageIndicatorTracker.ObservedEntity> observed = new ArrayList<>(selected.size());
        for (LivingEntity living : selected) {
            observed.add(new DamageIndicatorTracker.ObservedEntity(living.getId(), living.getX(),
                    living.getY() + living.getBbHeight(), living.getZ(), living.getHealth(), living.hurtTime));
        }
        damageIndicators.observe(observed, nowMillis);
    }

    /** Orders local damage candidates nearest-first, then by stable entity ID. */
    private static int compareDamageCandidates(LivingEntity left, LivingEntity right, Player localPlayer) {
        int distance = Double.compare(left.position().distanceToSqr(localPlayer.position()),
                right.position().distanceToSqr(localPlayer.position()));
        return distance != 0 ? distance : Integer.compare(left.getId(), right.getId());
    }

    private void renderEntityEsp(ClientLevel level, Player localPlayer) {
        EntityEspMode mode = entityEsp.mode();
        if (mode.usesNativeOutline()) {
            return;
        }
        EntityRenderFilter.Options options = entityEsp.options();
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, entity == localPlayer,
                    entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            GizmoStyle style = mode == EntityEspMode.BOX
                    ? GizmoStyle.strokeAndFill(entityEsp.color(friend), entityEsp.lineWidth(), entityEsp.fillColor())
                    : GizmoStyle.stroke(entityEsp.color(friend), entityEsp.lineWidth());
            Gizmos.cuboid(entity.getBoundingBox().inflate(0.05D), style).setAlwaysOnTop();
            if (++rendered >= entityEsp.maximumEntities()) {
                return;
            }
        }
    }

    /** Snapshots this tick's selected entities for Minecraft's native outline pass. */
    private void installNativeOutlineTargets(ClientLevel level, Player localPlayer) {
        EntityRenderFilter.Options options = entityEsp.options();
        EntityEspNativeOutlineTargetsBuilder builder = new EntityEspNativeOutlineTargetsBuilder(
                entityEsp.maximumEntities(), entityEsp.mode().usesShaderColor());
        for (Entity entity : level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, entity == localPlayer,
                    entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            if (!builder.offer(entity.getId(), entityEsp.color(friend))) {
                break;
            }
        }
        EntityEspRenderAccess.install(builder.build());
    }

    private void clearNativeOutlineTargets() {
        if (nativeOutlineWasInstalled) {
            nativeOutlineWasInstalled = false;
            EntityEspRenderAccess.clear();
        }
    }

    /** Snapshots this tick's selected entities and colors for the Chams outline pass. */
    private void installChamsTargets(ClientLevel level, Player localPlayer) {
        EntityRenderFilter.Options options = chams.options();
        ChamsTargetsBuilder builder = new ChamsTargetsBuilder(chams.maximumEntities());
        for (Entity entity : level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, entity == localPlayer,
                    entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            boolean living = entity instanceof LivingEntity;
            double healthFraction = 1.0D;
            if (entity instanceof LivingEntity livingEntity) {
                float maxHealth = livingEntity.getMaxHealth();
                healthFraction = maxHealth > 0.0F ? livingEntity.getHealth() / maxHealth : 0.0D;
            }
            if (!builder.offer(entity.getId(), chams.colorFor(friend, living, healthFraction))) {
                break;
            }
        }
        ChamsRenderAccess.install(builder.build());
    }

    private void clearChamsTargets() {
        if (chamsWasInstalled) {
            chamsWasInstalled = false;
            ChamsRenderAccess.clear();
        }
    }

    /** Renders one bounded local-only aura at the local player's feet. */
    private void renderLocalCosmetics(Player localPlayer) {
        for (LocalAuraGeometry.Segment segment : LocalAuraGeometry.ring(localPlayer.getX(),
                localPlayer.getY() + 0.06D, localPlayer.getZ(), localCosmetics.radius(), localCosmetics.segments())) {
            LocalAuraGeometry.Point from = segment.from();
            LocalAuraGeometry.Point to = segment.to();
            Gizmos.line(new Vec3(from.x(), from.y(), from.z()), new Vec3(to.x(), to.y(), to.z()),
                    localCosmetics.color(), 1.25F);
        }
    }

    private void renderTracers(ClientLevel level, Player localPlayer, CameraRenderState camera) {
        EntityRenderFilter.Options options = tracers.options();
        Vec3 start = tracerStart(camera, localPlayer);
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, entity == localPlayer,
                    entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            Gizmos.line(start, entity.getBoundingBox().getCenter(), tracers.color(friend), tracers.lineWidth()).setAlwaysOnTop();
            if (++rendered >= tracers.maximumEntities()) {
                return;
            }
        }
    }

    private void renderBetterNametags(ClientLevel level, Player localPlayer, Frustum frustum) {
        BetterNametags.Options options = betterNametags.options();
        double maximumDistanceSquared = options.range() * options.range();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == localPlayer || player.isInvisibleTo(localPlayer)
                    || !localPlayer.hasLineOfSight(player) || !isFrustumVisible(frustum, player)
                    || player.position().distanceToSqr(localPlayer.position()) > maximumDistanceSquared) {
                continue;
            }
            boolean friend = isFriend(player);
            BetterNametagText.Facts facts = new BetterNametagText.Facts(player.getGameProfile().name(), player.getHealth(),
                    player.getMaxHealth(), player.getArmorValue(), Math.sqrt(player.position().distanceToSqr(localPlayer.position())),
                    BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
            List<BetterNametagText.Line> lines = BetterNametagText.lines(facts, options, friend);
            double baseY = player.getY() + player.getBbHeight() + NAMETAG_BASE_OFFSET;
            for (int index = 0; index < lines.size(); index++) {
                BetterNametagText.Line line = lines.get(index);
                Gizmos.billboardText(line.text(),
                        new Vec3(player.getX(),
                                baseY + BetterNametagText.stackOffset(index, lines.size()) * NAMETAG_LINE_SPACING,
                                player.getZ()),
                        TextGizmo.Style.forColorAndCentered(line.color()));
            }
        }
    }

    /** Draws one local outline around BowAimAssist's currently predicted target. */
    private void renderBowAimMarker(ClientLevel level) {
        bowAimAssist.markerTargetId().ifPresent(targetId -> {
            for (Entity entity : level.entitiesForRendering()) {
                if (entity.getUUID().toString().equals(targetId)) {
                    Gizmos.cuboid(entity.getBoundingBox().inflate(0.08D),
                            GizmoStyle.strokeAndFill(0xFFFFD54F, 1.5F, 0x303FFF00)).setAlwaysOnTop();
                    return;
                }
            }
        });
    }

    private void renderTrajectories(ClientLevel level, Frustum frustum) {
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile)) {
                continue;
            }
            Optional<Trajectories.ProjectileType> type = projectileType(projectile);
            if (type.isEmpty() || !trajectories.includes(type.get())) {
                continue;
            }
            if (!isFrustumVisible(frustum, projectile)) {
                continue;
            }
            TrajectorySimulator.Physics physics = switch (type.get()) {
                case ARROW, TRIDENT -> ARROW_PHYSICS;
                case SPLASH_POTION -> POTION_PHYSICS;
                case SNOWBALL, EGG, ENDER_PEARL -> THROWN_PHYSICS;
            };
            TrajectorySimulator.TraceResult result = TrajectorySimulator.trace(
                    trajectoryVector(projectile.position()), trajectoryVector(projectile.getDeltaMovement()), physics,
                    trajectories.maximumSteps(), (from, to) -> firstBlockCollision(level, projectile, from, to),
                    (from, to) -> Gizmos.line(minecraftVector(from), minecraftVector(to), trajectories.color(),
                            trajectories.lineWidth()).setAlwaysOnTop()
            );
            if (result.collided()) {
                Gizmos.point(minecraftVector(result.terminalPoint()), trajectories.impactColor(), 3.0F).setAlwaysOnTop();
            }
            if (++rendered >= trajectories.maximumProjectiles()) {
                return;
            }
        }
    }

    private void renderExplosions(ClientLevel level, Frustum frustum) {
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            Optional<Explosions.Source> source = explosionSource(entity);
            if (source.isEmpty() || !explosions.includes(source.get()) || !isFrustumVisible(frustum, entity)) {
                continue;
            }
            double radius = source.get().damageRadius();
            Vec3 center = entity.getBoundingBox().getCenter();
            int color = explosions.color();
            for (ExplosionSphereGeometry.Segment segment : ExplosionSphereGeometry.wireframe(center.x(), center.y(),
                    center.z(), radius, explosions.segments())) {
                ExplosionSphereGeometry.Point from = segment.from();
                ExplosionSphereGeometry.Point to = segment.to();
                Gizmos.line(new Vec3(from.x(), from.y(), from.z()), new Vec3(to.x(), to.y(), to.z()),
                        color, explosions.lineWidth()).setAlwaysOnTop();
            }
            if (explosions.showRadiusLabel()) {
                String label = String.format(java.util.Locale.ROOT, "%.1f", radius);
                Gizmos.billboardText(label, center.add(0.0D, radius + 0.5D, 0.0D),
                        TextGizmo.Style.forColorAndCentered(color)).setAlwaysOnTop();
            }
            if (++rendered >= explosions.maximumSources()) {
                return;
            }
        }
    }

    /**
     * Highlights frustum-visible projectiles whose short-horizon linear path passes
     * within the configured hit radius of the local player. It never changes projectile
     * state, velocity, or packets; the server remains authoritative over damage.
     */
    private void renderProjectileWarning(ClientLevel level, Player localPlayer, Frustum frustum) {
        Vec3 center = localPlayer.getBoundingBox().getCenter();
        Vec3 playerVelocity = localPlayer.getDeltaMovement();
        GizmoStyle style = GizmoStyle.strokeAndFill(projectileWarning.color(), projectileWarning.lineWidth(),
                projectileWarning.fillColor());
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Projectile projectile)) {
                continue;
            }
            Optional<Trajectories.ProjectileType> type = projectileType(projectile);
            if (type.isEmpty() || !projectileWarning.includes(type.get())) {
                continue;
            }
            Entity owner = projectile.getOwner();
            if (owner == localPlayer) {
                continue;
            }
            if (projectileWarning.excludeFriendProjectiles() && isFriend(owner)) {
                continue;
            }
            if (!isFrustumVisible(frustum, projectile)) {
                continue;
            }
            Vec3 position = projectile.getBoundingBox().getCenter();
            Vec3 velocity = projectile.getDeltaMovement();
            Optional<ProjectileThreatPolicy.ProjectileThreat> threat = ProjectileThreatPolicy.assess(
                    position.x() - center.x(), position.y() - center.y(), position.z() - center.z(),
                    velocity.x() - playerVelocity.x(), velocity.y() - playerVelocity.y(), velocity.z() - playerVelocity.z(),
                    projectileWarning.hitRadius(), projectileWarning.warningTicks(), projectileWarning.detectionRange());
            if (threat.isEmpty()) {
                continue;
            }
            Gizmos.cuboid(projectile.getBoundingBox().inflate(0.25D), style).setAlwaysOnTop();
            if (projectileWarning.showLabel()) {
                String label = String.format(java.util.Locale.ROOT, "! %.1fs",
                        threat.get().timeToImpactTicks() / 20.0D);
                Gizmos.billboardText(label, position.add(0.0D, 0.35D, 0.0D),
                        TextGizmo.Style.forColorAndCentered(projectileWarning.color())).setAlwaysOnTop();
            }
            if (++rendered >= projectileWarning.maximumProjectiles()) {
                return;
            }
        }
    }

    /** Classifies an entity as a primed local explosion source, honoring the armed-creeper filter. */
    private Optional<Explosions.Source> explosionSource(Entity entity) {
        if (entity instanceof PrimedTnt) {
            return Optional.of(Explosions.Source.TNT);
        }
        if (entity instanceof MinecartTNT minecart) {
            return minecart.isPrimed() ? Optional.of(Explosions.Source.TNT_MINECART) : Optional.empty();
        }
        if (entity instanceof EndCrystal) {
            return Optional.of(Explosions.Source.END_CRYSTAL);
        }
        if (entity instanceof Creeper creeper) {
            if (explosions.armedCreepersOnly() && creeper.getSwellDir() <= 0 && !creeper.isIgnited()) {
                return Optional.empty();
            }
            return Optional.of(creeper.isPowered() ? Explosions.Source.CHARGED_CREEPER : Explosions.Source.CREEPER);
        }
        return Optional.empty();
    }

    private void renderTrueSight(ClientLevel level, Player localPlayer, Frustum frustum) {
        EntityRenderFilter.Options options = trueSight.options();
        GizmoStyle style = GizmoStyle.strokeAndFill(trueSight.transparentColor(), trueSight.lineWidth(),
                trueSight.transparentColor());
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!entity.isInvisibleTo(localPlayer) || !isFrustumVisible(frustum, entity)
                    || !EntityRenderFilter.shouldRender(options, entityType(entity), false,
                    entity == localPlayer, entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            Gizmos.cuboid(entity.getBoundingBox().inflate(0.05D), style).setAlwaysOnTop();
            if (++rendered >= trueSight.maximumEntities()) {
                return;
            }
        }
    }

    private void renderBlocks(ClientLevel level, Player localPlayer, CameraRenderState camera) {
        Vec3 start = blockEsp.tracersEnabled() ? tracerStart(camera, localPlayer) : null;
        for (BlockEspScanCursor.Position position : blockCache.positions()) {
            if (!isWithinCurrentBlockRange(position, localPlayer)) {
                continue;
            }
            BlockPos blockPosition = new BlockPos(position.x(), position.y(), position.z());
            if (!level.hasChunk(position.x() >> 4, position.z() >> 4)) continue;
            String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockPosition).getBlock()).toString();
            int color = blockEsp.color(blockId);
            GizmoStyle style = GizmoStyle.strokeAndFill(color, blockEsp.lineWidth(), blockEsp.fillColor());
            Gizmos.cuboid(new AABB(blockPosition), style).setAlwaysOnTop();
            if (start != null) {
                Gizmos.line(start, Vec3.atCenterOf(blockPosition), color, blockEsp.lineWidth()).setAlwaysOnTop();
            }
        }
    }

    private void renderStorage(Player localPlayer, Frustum frustum) {
        GizmoStyle style = GizmoStyle.strokeAndFill(storageEsp.color(), storageEsp.lineWidth(), storageEsp.fillColor());
        for (BlockEspScanCursor.Position position : storageCache.positions()) {
            if (!isWithinCurrentStorageRange(position, localPlayer)) {
                continue;
            }
            AABB bounds = new AABB(position.x(), position.y(), position.z(),
                    position.x() + 1.0D, position.y() + 1.0D, position.z() + 1.0D);
            if (frustum == null || !frustum.isVisible(bounds)) {
                continue;
            }
            Gizmos.cuboid(bounds, style).setAlwaysOnTop();
        }
    }

    private void renderBaseEvidence(Player localPlayer, CameraRenderState camera) {
        List<BaseFinder.Evidence> evidence = new ArrayList<>();
        for (BlockEspScanCursor.Position position : baseFinderCache.positions()) {
            if (isWithinCurrentBaseFinderRange(position, localPlayer)) {
                evidence.add(new BaseFinder.Evidence(position.x(), position.y(), position.z()));
            }
        }
        if (evidence.isEmpty()) {
            return;
        }
        Vec3 start = baseFinder.tracersEnabled() ? tracerStart(camera, localPlayer) : null;
        GizmoStyle style = GizmoStyle.strokeAndFill(
                baseFinder.color(), baseFinder.lineWidth(), baseFinder.fillColor());
        int rendered = 0;
        for (BaseFinder.Evidence marker : evidence) {
            if (!baseFinder.shouldHighlight(marker, evidence)) {
                continue;
            }
            BlockPos position = new BlockPos(marker.x(), marker.y(), marker.z());
            Gizmos.cuboid(new AABB(position), style).setAlwaysOnTop();
            if (start != null) {
                Gizmos.line(start, Vec3.atCenterOf(position), baseFinder.color(), baseFinder.lineWidth())
                        .setAlwaysOnTop();
            }
            if (++rendered >= baseFinder.maximumMarkers()) {
                return;
            }
        }
    }

    private void renderDamageIndicators(ClientLevel level, Player localPlayer, Frustum frustum, long nowMillis) {
        for (DamageIndicatorTracker.RenderedIndicator indicator : damageIndicators.renderedIndicators(nowMillis)) {
            Entity entity = level.getEntity(indicator.entityId());
            if (entity == null || !isFrustumVisible(frustum, entity)
                    || !EntityRenderFilter.shouldRender(damageIndicators.options(), entityType(entity), false,
                    entity == localPlayer, entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            int color = RenderColor.withAlpha(damageIndicators.color(), indicator.alpha());
            String text = String.format(java.util.Locale.ROOT, "-%.1f", indicator.damage());
            Gizmos.billboardText(text, new Vec3(indicator.x(), indicator.y(), indicator.z()),
                    TextGizmo.Style.forColorAndCentered(color)).setAlwaysOnTop();
        }
    }

    /** A missing render frustum is treated as not renderable to preserve the local render boundary. */
    private static boolean isFrustumVisible(Frustum frustum, Entity entity) {
        return frustum != null && frustum.isVisible(entity.getBoundingBox());
    }

    /**
     * Tracer lines must start at the interpolated render-camera position, not the
     * player's tick position: a line that begins on or behind the near plane
     * renders as disconnected streaks. A short forward offset makes lines radiate
     * from the crosshair.
     */
    private static Vec3 tracerStart(CameraRenderState camera, Player localPlayer) {
        if (camera == null || camera.pos == null) {
            return localPlayer.getEyePosition();
        }
        return camera.pos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot).scale(0.4D));
    }

    private void renderBreadcrumbs() {
        BreadcrumbTrail.Point previous = null;
        for (BreadcrumbTrail.Point point : breadcrumbs.points()) {
            if (previous != null) {
                GizmoProperties line = Gizmos.line(new Vec3(previous.x(), previous.y(), previous.z()),
                        new Vec3(point.x(), point.y(), point.z()), breadcrumbs.color(), breadcrumbs.lineWidth());
                if (breadcrumbs.alwaysOnTop()) {
                    line.setAlwaysOnTop();
                }
            }
            previous = point;
        }
    }

    private void renderBuilderPreview(Minecraft client) {
        GizmoStyle style = GizmoStyle.strokeAndFill(builderAssist.previewColor(), 1.0F, builderAssist.previewFillColor());
        for (BlockPos position : MinecraftBuilderAssistAccess.previewPositions(client, builderAssist)) {
            Gizmos.cuboid(new AABB(position), style).setAlwaysOnTop();
        }
    }

    private void renderBlockSelection(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos position = hit.getBlockPos();
        if (!client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
            return;
        }
        BlockSelection.Options options = blockSelection.options();
        GizmoStyle style = options.fill()
                ? GizmoStyle.strokeAndFill(options.outlineColor(), options.lineWidth(),
                RenderColor.withAlpha(options.outlineColor(), 0.2D))
                : GizmoStyle.stroke(options.outlineColor(), options.lineWidth());
        Gizmos.cuboid(new AABB(position), style).setAlwaysOnTop();
        if (!options.distanceLabel()) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(position);
        blockSelection.distanceLabel(client.player.getEyePosition().distanceTo(center)).ifPresent(label ->
                Gizmos.billboardText(label, center.add(0.0D, 0.7D, 0.0D),
                        TextGizmo.Style.forColorAndCentered(options.outlineColor())).setAlwaysOnTop());
    }

    private static Optional<TrajectoryVector> firstBlockCollision(ClientLevel level, Entity collisionContext,
                                                                    TrajectoryVector from, TrajectoryVector to) {
        BlockHitResult hit = level.clip(new ClipContext(minecraftVector(from), minecraftVector(to),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, collisionContext));
        return hit.getType() == HitResult.Type.MISS ? Optional.empty() : Optional.of(trajectoryVector(hit.getLocation()));
    }

    /**
     * Predicts the path of the projectile the player is currently holding, before it is
     * launched. The main hand is checked first; a throwable in the offhand is checked when
     * the main hand holds nothing previewable and the offhand option is enabled.
     */
    private void renderProjectilePreview(ClientLevel level, Player player) {
        Optional<HeldProjectilePreview.Launch> launch = detectHeldProjectile(player, player.getMainHandItem(), false);
        if (launch.isEmpty() && projectilePreview.previewsOffhand()) {
            launch = detectHeldProjectile(player, player.getOffhandItem(), true);
        }
        launch.ifPresent(predicted -> {
            Vec3 eye = player.getEyePosition();
            TrajectoryVector start = new TrajectoryVector(eye.x(), eye.y() - 0.1D, eye.z());
            TrajectorySimulator.TraceResult result = TrajectorySimulator.trace(start, predicted.velocity(),
                    predicted.physics(), projectilePreview.maximumSteps(),
                    (from, to) -> firstBlockCollision(level, player, from, to),
                    (from, to) -> Gizmos.line(minecraftVector(from), minecraftVector(to), projectilePreview.color(),
                            projectilePreview.lineWidth()).setAlwaysOnTop());
            if (result.collided()) {
                Gizmos.point(minecraftVector(result.terminalPoint()), projectilePreview.impactColor(), 3.0F).setAlwaysOnTop();
            }
        });
    }

    /**
     * Maps a held stack to a predicted launch, or empty when it is not a previewable held
     * projectile in a launchable state. Bows are previewed only while actually being drawn;
     * crossbows only while loaded; {@code throwablesOnly} restricts the offhand to thrown items.
     */
    private Optional<HeldProjectilePreview.Launch> detectHeldProjectile(Player player, ItemStack stack,
                                                                        boolean throwablesOnly) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Item item = stack.getItem();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (!throwablesOnly && item instanceof BowItem) {
            if (!projectilePreview.includes(HeldProjectilePreview.Kind.BOW)
                    || !player.isUsingItem() || player.getUseItem() != stack) {
                return Optional.empty();
            }
            int drawTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
            return HeldProjectilePreview.launch(HeldProjectilePreview.Kind.BOW, yaw, pitch, drawTicks);
        }
        if (!throwablesOnly && item instanceof CrossbowItem) {
            ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
            if (!projectilePreview.includes(HeldProjectilePreview.Kind.CROSSBOW) || charged == null || charged.isEmpty()) {
                return Optional.empty();
            }
            return HeldProjectilePreview.launch(HeldProjectilePreview.Kind.CROSSBOW, yaw, pitch, 0);
        }
        if (!throwablesOnly && item instanceof TridentItem) {
            if (!projectilePreview.includes(HeldProjectilePreview.Kind.TRIDENT)) {
                return Optional.empty();
            }
            return HeldProjectilePreview.launch(HeldProjectilePreview.Kind.TRIDENT, yaw, pitch, 0);
        }
        HeldProjectilePreview.Kind throwableKind = throwableKind(item);
        if (throwableKind != null && projectilePreview.includes(throwableKind)) {
            return HeldProjectilePreview.launch(throwableKind, yaw, pitch, 0);
        }
        return Optional.empty();
    }

    private static HeldProjectilePreview.Kind throwableKind(Item item) {
        if (item instanceof SplashPotionItem) {
            return HeldProjectilePreview.Kind.SPLASH_POTION;
        }
        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderpearlItem) {
            return HeldProjectilePreview.Kind.THROWABLE;
        }
        return null;
    }

    private static Optional<Trajectories.ProjectileType> projectileType(Projectile projectile) {
        if (projectile instanceof ThrownTrident) {
            return Optional.of(Trajectories.ProjectileType.TRIDENT);
        }
        if (projectile instanceof AbstractArrow) {
            return Optional.of(Trajectories.ProjectileType.ARROW);
        }
        if (projectile instanceof Snowball) {
            return Optional.of(Trajectories.ProjectileType.SNOWBALL);
        }
        if (projectile instanceof ThrownEgg) {
            return Optional.of(Trajectories.ProjectileType.EGG);
        }
        if (projectile instanceof ThrownEnderpearl) {
            return Optional.of(Trajectories.ProjectileType.ENDER_PEARL);
        }
        if (projectile instanceof ThrownSplashPotion) {
            return Optional.of(Trajectories.ProjectileType.SPLASH_POTION);
        }
        return Optional.empty();
    }

    private static TrajectoryVector trajectoryVector(Vec3 vector) {
        return new TrajectoryVector(vector.x(), vector.y(), vector.z());
    }

    private static Vec3 minecraftVector(TrajectoryVector vector) {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    private boolean isFriend(Entity entity) {
        return entity instanceof Player player && friends.contains(player.getGameProfile().name());
    }

    private static EntityRenderFilter.EntityType entityType(Entity entity) {
        if (entity instanceof Player) {
            return EntityRenderFilter.EntityType.PLAYER;
        }
        if (entity instanceof Monster) {
            return EntityRenderFilter.EntityType.HOSTILE;
        }
        if (entity instanceof ItemEntity) {
            return EntityRenderFilter.EntityType.ITEM;
        }
        if (entity instanceof Projectile) {
            return EntityRenderFilter.EntityType.PROJECTILE;
        }
        if (entity instanceof LivingEntity) {
            return EntityRenderFilter.EntityType.PASSIVE;
        }
        return EntityRenderFilter.EntityType.OTHER;
    }

    private boolean isWithinCurrentBlockRange(BlockEspScanCursor.Position position, Player player) {
        return BlockEspRange.contains(position, player.getX(), player.getY(), player.getZ(),
                blockEsp.horizontalRange(), blockEsp.verticalRange());
    }

    private boolean isWithinCurrentStorageRange(BlockEspScanCursor.Position position, Player player) {
        return BlockEspRange.contains(position, player.getX(), player.getY(), player.getZ(),
                storageEsp.horizontalRange(), storageEsp.verticalRange());
    }

    private boolean isWithinCurrentBaseFinderRange(BlockEspScanCursor.Position position, Player player) {
        return BlockEspRange.contains(position, player.getX(), player.getY(), player.getZ(),
                baseFinder.horizontalRange(), baseFinder.verticalRange());
    }

    private void resetBlockScanner() {
        blockAnchor.clear();
        clearBlockResults();
    }

    private void resetStorageScanner() {
        storageAnchor.clear();
        clearStorageResults();
    }

    private void resetBaseFinderScanner() {
        baseFinderAnchor.clear();
        clearBaseFinderResults();
    }

    private void clearBlockResults() {
        blockCursor.clear();
        blockCache.clear();
    }

    private void clearStorageResults() {
        storageCursor.clear();
        storageCache.clear();
    }

    private void clearBaseFinderResults() {
        baseFinderCursor.clear();
        baseFinderCache.clear();
    }
}
