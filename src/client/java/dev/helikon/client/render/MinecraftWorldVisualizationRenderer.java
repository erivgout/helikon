package dev.helikon.client.render;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.render.BlockEsp;
import dev.helikon.client.module.render.Breadcrumbs;
import dev.helikon.client.module.render.EntityEsp;
import dev.helikon.client.module.render.Tracers;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Narrow 26.2 client adapter for the supported Fabric level Gizmo phase.
 * It renders only already-known local entities and bounded cached block results.
 */
public final class MinecraftWorldVisualizationRenderer {
    private static final int MAXIMUM_CACHED_BLOCKS = 512;

    private final ModuleRegistry modules;
    private final FriendManager friends;
    private final EntityEsp entityEsp;
    private final BlockEsp blockEsp;
    private final Tracers tracers;
    private final Breadcrumbs breadcrumbs;
    private final BlockEspScanCursor blockCursor = new BlockEspScanCursor();
    private final BlockEspScanAnchor blockAnchor = new BlockEspScanAnchor();
    private final BlockEspCache blockCache = new BlockEspCache(MAXIMUM_CACHED_BLOCKS);
    private ClientLevel observedLevel;
    private long observedBlockScanRevision = Long.MIN_VALUE;
    private boolean blockScannerWasEnabled;

    public MinecraftWorldVisualizationRenderer(ModuleRegistry modules, FriendManager friends, EntityEsp entityEsp,
                                                BlockEsp blockEsp, Tracers tracers, Breadcrumbs breadcrumbs) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.friends = Objects.requireNonNull(friends, "friends");
        this.entityEsp = Objects.requireNonNull(entityEsp, "entityEsp");
        this.blockEsp = Objects.requireNonNull(blockEsp, "blockEsp");
        this.tracers = Objects.requireNonNull(tracers, "tracers");
        this.breadcrumbs = Objects.requireNonNull(breadcrumbs, "breadcrumbs");
        this.blockEsp.setCacheClearer(this::resetBlockScanner);
    }

    /** Samples the trail and advances the bounded block cache from the client tick bridge. */
    public void tick(Minecraft client) {
        Objects.requireNonNull(client, "client");
        if (client.level != observedLevel) {
            observedLevel = client.level;
            breadcrumbs.clearTrail();
            resetBlockScanner();
        }
        if (client.level == null || client.player == null) {
            return;
        }
        if (breadcrumbs.isEnabled()) {
            modules.runGuarded(breadcrumbs, "tick", () -> breadcrumbs.sample(
                    client.player.getX(), client.player.getY(), client.player.getZ(), System.currentTimeMillis()));
        }
        if (!blockEsp.isEnabled()) {
            if (blockScannerWasEnabled) {
                resetBlockScanner();
                blockScannerWasEnabled = false;
            }
            return;
        }
        blockScannerWasEnabled = true;
        if (!BlockEspScanPolicy.shouldScan(blockEsp.isEnabled(), blockEsp.targetBlocks())) {
            clearBlockResults();
            return;
        }
        modules.runGuarded(blockEsp, "scan", () -> scanBlocks(client.level, client.player));
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
        if (blockEsp.isEnabled()) {
            modules.runGuarded(blockEsp, "render", () -> renderBlocks(client.player));
        }
        if (breadcrumbs.isEnabled()) {
            modules.runGuarded(breadcrumbs, "render", this::renderBreadcrumbs);
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

    private void resetBlockScanner() {
        blockAnchor.clear();
        clearBlockResults();
    }

    private void clearBlockResults() {
        blockCursor.clear();
        blockCache.clear();
    }
}
