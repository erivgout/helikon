package dev.helikon.client.render;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.combat.BowAimAssist;
import dev.helikon.client.module.render.BlockEsp;
import dev.helikon.client.module.render.Breadcrumbs;
import dev.helikon.client.module.render.DamageIndicators;
import dev.helikon.client.module.render.EntityEsp;
import dev.helikon.client.module.render.StorageEsp;
import dev.helikon.client.module.render.Trajectories;
import dev.helikon.client.module.render.Tracers;
import dev.helikon.client.module.render.TrueSight;
import dev.helikon.client.module.world.BuilderAssist;
import dev.helikon.client.module.world.MinecraftBuilderAssistAccess;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
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
    private final BlockEsp blockEsp;
    private final Tracers tracers;
    private final Trajectories trajectories;
    private final TrueSight trueSight;
    private final StorageEsp storageEsp;
    private final DamageIndicators damageIndicators;
    private final Breadcrumbs breadcrumbs;
    private final BuilderAssist builderAssist;
    private final BowAimAssist bowAimAssist;
    private final BlockEspScanCursor blockCursor = new BlockEspScanCursor();
    private final BlockEspScanAnchor blockAnchor = new BlockEspScanAnchor();
    private final BlockEspCache blockCache = new BlockEspCache(MAXIMUM_CACHED_BLOCKS);
    private final BlockEspScanCursor storageCursor = new BlockEspScanCursor();
    private final BlockEspScanAnchor storageAnchor = new BlockEspScanAnchor();
    private final BlockEspCache storageCache = new BlockEspCache(MAXIMUM_CACHED_BLOCKS);
    private ClientLevel observedLevel;
    private long observedBlockScanRevision = Long.MIN_VALUE;
    private long observedStorageScanRevision = Long.MIN_VALUE;
    private boolean blockScannerWasEnabled;
    private boolean storageScannerWasEnabled;

    public MinecraftWorldVisualizationRenderer(ModuleRegistry modules, FriendManager friends, EntityEsp entityEsp,
                                                BlockEsp blockEsp, Tracers tracers, Trajectories trajectories,
                                                TrueSight trueSight, StorageEsp storageEsp, DamageIndicators damageIndicators,
                                                Breadcrumbs breadcrumbs, BuilderAssist builderAssist, BowAimAssist bowAimAssist) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.friends = Objects.requireNonNull(friends, "friends");
        this.entityEsp = Objects.requireNonNull(entityEsp, "entityEsp");
        this.blockEsp = Objects.requireNonNull(blockEsp, "blockEsp");
        this.tracers = Objects.requireNonNull(tracers, "tracers");
        this.trajectories = Objects.requireNonNull(trajectories, "trajectories");
        this.trueSight = Objects.requireNonNull(trueSight, "trueSight");
        this.storageEsp = Objects.requireNonNull(storageEsp, "storageEsp");
        this.damageIndicators = Objects.requireNonNull(damageIndicators, "damageIndicators");
        this.breadcrumbs = Objects.requireNonNull(breadcrumbs, "breadcrumbs");
        this.builderAssist = Objects.requireNonNull(builderAssist, "builderAssist");
        this.bowAimAssist = Objects.requireNonNull(bowAimAssist, "bowAimAssist");
        this.blockEsp.setCacheClearer(this::resetBlockScanner);
        this.storageEsp.setCacheClearer(this::resetStorageScanner);
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
        }
        if (client.level == null || client.player == null) {
            return;
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
    }

    /** Registered for Fabric's verified {@code BEFORE_GIZMOS} level-render phase. */
    public void render(LevelRenderContext context) {
        Objects.requireNonNull(context, "context");
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        if (entityEsp.isEnabled()) {
            modules.runGuarded(entityEsp, "render", () -> renderEntityEsp(client.level, client.player));
        }
        if (tracers.isEnabled()) {
            modules.runGuarded(tracers, "render", () -> renderTracers(client.level, client.player));
        }
        Frustum frustum = context.levelState().cameraRenderState.cullFrustum;
        if (trajectories.isEnabled()) {
            modules.runGuarded(trajectories, "render", () -> renderTrajectories(client.level, frustum));
        }
        if (trueSight.isEnabled()) {
            modules.runGuarded(trueSight, "render", () -> renderTrueSight(client.level, client.player, frustum));
        }
        if (blockEsp.isEnabled()) {
            modules.runGuarded(blockEsp, "render", () -> renderBlocks(client.player));
        }
        if (storageEsp.isEnabled()) {
            modules.runGuarded(storageEsp, "render", () -> renderStorage(client.player, frustum));
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
        if (bowAimAssist.isEnabled()) {
            modules.runGuarded(bowAimAssist, "render", () -> renderBowAimMarker(client.level));
        }
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
        EntityRenderFilter.Options options = entityEsp.options();
        int rendered = 0;
        for (Entity entity : level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, entity == localPlayer,
                    entity.position().distanceToSqr(localPlayer.position()))) {
                continue;
            }
            GizmoStyle style = GizmoStyle.strokeAndFill(entityEsp.color(friend), entityEsp.lineWidth(), entityEsp.fillColor());
            Gizmos.cuboid(entity.getBoundingBox().inflate(0.05D), style).setAlwaysOnTop();
            if (++rendered >= entityEsp.maximumEntities()) {
                return;
            }
        }
    }

    private void renderTracers(ClientLevel level, Player localPlayer) {
        EntityRenderFilter.Options options = tracers.options();
        Vec3 start = localPlayer.getEyePosition();
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

    private void renderBlocks(Player localPlayer) {
        GizmoStyle style = GizmoStyle.strokeAndFill(blockEsp.color(), blockEsp.lineWidth(), blockEsp.fillColor());
        Vec3 start = blockEsp.tracersEnabled() ? localPlayer.getEyePosition() : null;
        for (BlockEspScanCursor.Position position : blockCache.positions()) {
            if (!isWithinCurrentBlockRange(position, localPlayer)) {
                continue;
            }
            BlockPos blockPosition = new BlockPos(position.x(), position.y(), position.z());
            Gizmos.cuboid(new AABB(blockPosition), style).setAlwaysOnTop();
            if (start != null) {
                Gizmos.line(start, Vec3.atCenterOf(blockPosition), blockEsp.color(), blockEsp.lineWidth()).setAlwaysOnTop();
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

    private static Optional<TrajectoryVector> firstBlockCollision(ClientLevel level, Projectile projectile,
                                                                    TrajectoryVector from, TrajectoryVector to) {
        BlockHitResult hit = level.clip(new ClipContext(minecraftVector(from), minecraftVector(to),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
        return hit.getType() == HitResult.Type.MISS ? Optional.empty() : Optional.of(trajectoryVector(hit.getLocation()));
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

    private void resetBlockScanner() {
        blockAnchor.clear();
        clearBlockResults();
    }

    private void resetStorageScanner() {
        storageAnchor.clear();
        clearStorageResults();
    }

    private void clearBlockResults() {
        blockCursor.clear();
        blockCache.clear();
    }

    private void clearStorageResults() {
        storageCursor.clear();
        storageCache.clear();
    }
}
