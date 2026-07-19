package dev.helikon.client.module.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bounded loaded-world adapter for the small ordinary-use and ordinary-destroy world modules. */
public final class MinecraftLegacyWorldAccess {
    private static final int MAXIMUM_SCAN_CANDIDATES = 512;

    private MinecraftLegacyWorldAccess() {
    }

    public static void tickAutoFarm(long tick, AutoFarm module) {
        tickBlockAction(tick, module, Action.HARVEST);
    }

    public static void tickBonemeal(long tick, BonemealAura module) {
        tickBlockAction(tick, module, Action.BONEMEAL);
    }

    public static void tickExcavator(long tick, Excavator module) {
        tickBlockAction(tick, module, Action.EXCAVATE);
    }

    public static void tickKaboom(long tick, Kaboom module) {
        tickBlockAction(tick, module, Action.IGNITE);
    }

    public static void tickLiquids(long tick, Liquids module) {
        tickBlockAction(tick, module, Action.LIQUID);
    }

    public static void tickTillAura(long tick, TillAura module) {
        tickBlockAction(tick, module, Action.TILL);
    }

    public static void tickTreeBot(long tick, TreeBot module) {
        tickBlockAction(tick, module, Action.MINE_LOG);
    }

    public static void tickTunneller(long tick, Tunneller module) {
        tickBlockAction(tick, module, Action.TUNNEL);
    }

    public static void tickVeinMiner(long tick, VeinMiner module) {
        tickBlockAction(tick, module, Action.VEIN);
    }

    public static void tickBuildRandom(long tick, BuildRandom module) {
        tickBlockAction(tick, module, Action.PLACE_RANDOM);
    }

    public static void tickGhostHand(long tick, GhostHand module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!ready(client, module) || !client.options.keyUse.isDown()) {
            return;
        }
        BlockPos target = secondSolidAlongView(client, player, module.scanRadius());
        if (target == null) {
            return;
        }
        BoundedWorldAction.Candidate candidate = facts(client, player, target, Action.GHOST, null, null);
        module.select(tick, false, List.of(candidate)).ifPresent(selected -> {
            use(client, target);
            module.markActed(tick);
        });
    }

    public static void tickFeedAura(long tick, FeedAura module) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            module.onContextLost();
            return;
        }
        ItemStack held = player.getMainHandItem();
        List<FeedAura.Candidate> candidates = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Animal animal && entity != player && !entity.isRemoved()) {
                candidates.add(new FeedAura.Candidate(entity.getId(), entity.distanceTo(player), animal.isFood(held)));
            }
        }
        module.select(tick, dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()),
                !held.isEmpty(), candidates).ifPresent(entityId -> {
            Entity entity = client.level.getEntity(entityId);
            if (entity instanceof Animal animal) {
                client.gameMode.interact(player, animal, new EntityHitResult(animal), InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);
                module.markFed(tick);
            }
        });
    }

    private static void tickBlockAction(long tick, BoundedWorldAction module, Action action) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (!ready(client, module)) {
            return;
        }
        if (action.requiresAttack && !client.options.keyAttack.isDown()) {
            return;
        }
        if (action.requiresUse && !client.options.keyUse.isDown()) {
            return;
        }
        BlockHitResult crosshair = client.hitResult instanceof BlockHitResult hit ? hit : null;
        BlockPos origin = crosshair == null ? null : crosshair.getBlockPos();
        String originId = origin == null ? null : blockId(client.level.getBlockState(origin));
        List<BoundedWorldAction.Candidate> candidates = action == Action.VEIN
                ? veinCandidates(client, player, module.scanRadius(), origin, originId)
                : scan(client, player, module.scanRadius(), action, origin, originId);
        module.select(tick, false, candidates).ifPresent(candidate -> {
            BlockPos position = new BlockPos(candidate.x(), candidate.y(), candidate.z());
            perform(client, module, action, position, candidate);
            module.markActed(tick);
        });
    }

    private static boolean ready(Minecraft client, BoundedWorldAction module) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            module.onContextLost();
            return false;
        }
        return module.isEnabled()
                && dev.helikon.client.gui.GameplayScreenPolicy.allowsAutomation(client.gui.screen());
    }

    private static List<BoundedWorldAction.Candidate> scan(Minecraft client, LocalPlayer player, int radius,
                                                            Action action, BlockPos origin, String originId) {
        List<BoundedWorldAction.Candidate> result = new ArrayList<>();
        BlockPos center = player.blockPosition();
        for (int y = -radius; y <= radius && result.size() < MAXIMUM_SCAN_CANDIDATES; y++) {
            for (int x = -radius; x <= radius && result.size() < MAXIMUM_SCAN_CANDIDATES; x++) {
                for (int z = -radius; z <= radius && result.size() < MAXIMUM_SCAN_CANDIDATES; z++) {
                    BlockPos position = center.offset(x, y, z);
                    if (!client.level.isInsideBuildHeight(position.getY())
                            || !client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
                        continue;
                    }
                    BoundedWorldAction.Candidate candidate = facts(client, player, position, action, origin, originId);
                    if (interesting(candidate, action)) {
                        result.add(candidate);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<BoundedWorldAction.Candidate> veinCandidates(Minecraft client, LocalPlayer player, int radius,
                                                                     BlockPos origin, String originId) {
        if (origin == null || originId == null || !isOre(originId)) {
            return List.of();
        }
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BoundedWorldAction.Candidate> result = new ArrayList<>();
        pending.add(origin);
        while (!pending.isEmpty() && result.size() < 128) {
            BlockPos position = pending.removeFirst();
            if (!visited.add(position) || !client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)
                    || position.distManhattan(origin) > radius * 3
                    || !blockId(client.level.getBlockState(position)).equals(originId)) {
                continue;
            }
            result.add(facts(client, player, position, Action.VEIN, origin, originId));
            for (Direction direction : Direction.values()) {
                pending.addLast(position.relative(direction));
            }
        }
        return List.copyOf(result);
    }

    private static BoundedWorldAction.Candidate facts(Minecraft client, LocalPlayer player, BlockPos position,
                                                       Action action, BlockPos origin, String originId) {
        BlockState state = client.level.getBlockState(position);
        String id = blockId(state);
        boolean mature = isHarvestablePlant(client, position, state);
        boolean growable = state.getBlock() instanceof BonemealableBlock;
        boolean tillable = state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT);
        boolean source = !state.getFluidState().isEmpty() && state.getFluidState().isSource();
        boolean log = state.is(BlockTags.LOGS);
        boolean ore = isOre(id) && (originId == null || originId.equals(id));
        boolean excavatable = origin != null && originId != null && originId.equals(id)
                && excavatorPlane(position, origin, client.hitResult instanceof BlockHitResult hit
                ? hit.getDirection().getAxis() : Direction.Axis.Y);
        boolean tunnel = isTunnelPosition(player, position);
        boolean replaceable = state.canBeReplaced() && hasSupport(client, position);
        if (action == Action.GHOST) {
            excavatable = !state.isAir();
        }
        return new BoundedWorldAction.Candidate(position.getX(), position.getY(), position.getZ(), id,
                Math.sqrt(position.distToCenterSqr(player.getX(), player.getY(), player.getZ())),
                mature, growable, tillable, source, state.is(Blocks.TNT), log, ore,
                excavatable, tunnel && !state.isAir(), replaceable);
    }

    private static boolean interesting(BoundedWorldAction.Candidate candidate, Action action) {
        return switch (action) {
            case HARVEST -> candidate.matureCrop();
            case BONEMEAL -> candidate.growable();
            case EXCAVATE, GHOST -> candidate.excavatable();
            case IGNITE -> candidate.tnt();
            case LIQUID -> candidate.liquidSource() || candidate.replaceable();
            case TILL -> candidate.tillable();
            case MINE_LOG -> candidate.log();
            case TUNNEL -> candidate.tunnel();
            case VEIN -> candidate.ore();
            case PLACE_RANDOM -> candidate.replaceable();
        };
    }

    private static void perform(Minecraft client, BoundedWorldAction module, Action action, BlockPos position,
                                BoundedWorldAction.Candidate candidate) {
        switch (action) {
            case HARVEST -> harvest(client, (AutoFarm) module, position, candidate.blockId());
            case BONEMEAL -> {
                if (client.player.getMainHandItem().is(Items.BONE_MEAL)) {
                    use(client, position);
                }
            }
            case IGNITE -> {
                ItemStack held = client.player.getMainHandItem();
                if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) {
                    use(client, position);
                }
            }
            case LIQUID -> {
                ItemStack held = client.player.getMainHandItem();
                if (held.is(Items.BUCKET) || held.is(Items.WATER_BUCKET) || held.is(Items.LAVA_BUCKET)) {
                    use(client, position);
                }
            }
            case TILL -> {
                if (client.player.getMainHandItem().getItem() instanceof HoeItem) {
                    use(client, position);
                }
            }
            case PLACE_RANDOM -> {
                if (client.player.getMainHandItem().getItem() instanceof BlockItem) {
                    use(client, position.below());
                }
            }
            case EXCAVATE, MINE_LOG, TUNNEL, VEIN -> destroy(client, position);
            case GHOST -> use(client, position);
        }
    }

    private static void harvest(Minecraft client, AutoFarm module, BlockPos position, String blockId) {
        switch (module.harvestMode(blockId)) {
            case PICK_IN_PLACE -> use(client, position);
            case BREAK_ABOVE_BASE, BREAK_FRUIT -> destroy(client, position);
            case BREAK_AND_REPLANT_BELOW -> {
                destroy(client, position);
                use(client, position.below());
            }
            case BREAK_AND_REPLANT_COCOA -> {
                BlockState state = client.level.getBlockState(position);
                Direction facing = state.getBlock() instanceof CocoaBlock
                        ? state.getValue(CocoaBlock.FACING)
                        : Direction.NORTH;
                destroy(client, position);
                use(client, position.relative(facing), facing.getOpposite());
            }
        }
    }

    private static void destroy(Minecraft client, BlockPos position) {
        Direction face = Direction.UP;
        if (client.gameMode.isDestroying()) {
            client.gameMode.continueDestroyBlock(position, face);
        } else {
            client.gameMode.startDestroyBlock(position, face);
        }
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void use(Minecraft client, BlockPos position) {
        use(client, position, Direction.UP);
    }

    private static void use(Minecraft client, BlockPos position, Direction face) {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(position).add(face.getUnitVec3().scale(0.5D)),
                face, position, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private static boolean isHarvestablePlant(Minecraft client, BlockPos position, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            return true;
        }
        if (state.is(Blocks.NETHER_WART)) {
            return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            return state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE;
        }
        if (state.is(Blocks.COCOA)) {
            return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
        }
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            return true;
        }
        if (!state.is(Blocks.SUGAR_CANE) && !state.is(Blocks.BAMBOO) && !state.is(Blocks.CACTUS)) {
            return false;
        }
        BlockState below = client.level.getBlockState(position.below());
        BlockState twoBelow = client.level.getBlockState(position.below(2));
        return below.is(state.getBlock()) && !twoBelow.is(state.getBlock());
    }

    private static BlockPos secondSolidAlongView(Minecraft client, LocalPlayer player, int range) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        BlockPos first = null;
        BlockPos previous = null;
        for (double distance = 0.25D; distance <= range; distance += 0.1D) {
            BlockPos position = BlockPos.containing(start.add(direction.scale(distance)));
            if (position.equals(previous)) {
                continue;
            }
            previous = position;
            if (!client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
                return null;
            }
            if (!client.level.getBlockState(position).isAir()) {
                if (first == null) {
                    first = position;
                } else if (!position.equals(first)) {
                    return position;
                }
            }
        }
        return null;
    }

    private static boolean excavatorPlane(BlockPos position, BlockPos origin, Direction.Axis axis) {
        return switch (axis) {
            case X -> position.getX() == origin.getX();
            case Y -> position.getY() == origin.getY();
            case Z -> position.getZ() == origin.getZ();
        };
    }

    private static boolean isTunnelPosition(LocalPlayer player, BlockPos position) {
        Vec3 view = player.getViewVector(1.0F);
        int directionX = Math.abs(view.x) >= Math.abs(view.z) ? (view.x >= 0.0D ? 1 : -1) : 0;
        int directionZ = directionX == 0 ? (view.z >= 0.0D ? 1 : -1) : 0;
        int deltaX = position.getX() - player.getBlockX();
        int deltaZ = position.getZ() - player.getBlockZ();
        int forward = deltaX * directionX + deltaZ * directionZ;
        int lateral = Math.abs(deltaX * directionZ - deltaZ * directionX);
        int deltaY = position.getY() - player.getBlockY();
        return forward >= 1 && lateral == 0 && (deltaY == 0 || deltaY == 1);
    }

    private static boolean hasSupport(Minecraft client, BlockPos position) {
        for (Direction direction : Direction.values()) {
            if (!client.level.getBlockState(position.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOre(String id) {
        return id.endsWith("_ore") || id.equals("minecraft:ancient_debris");
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private enum Action {
        HARVEST(false, false),
        BONEMEAL(false, true),
        EXCAVATE(true, false),
        IGNITE(false, true),
        LIQUID(false, true),
        TILL(false, true),
        MINE_LOG(true, false),
        TUNNEL(true, false),
        VEIN(true, false),
        PLACE_RANDOM(false, true),
        GHOST(false, true);

        private final boolean requiresAttack;
        private final boolean requiresUse;

        Action(boolean requiresAttack, boolean requiresUse) {
            this.requiresAttack = requiresAttack;
            this.requiresUse = requiresUse;
        }
    }
}
