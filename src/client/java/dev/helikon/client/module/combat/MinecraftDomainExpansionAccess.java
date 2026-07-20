package dev.helikon.client.module.combat;

import dev.helikon.client.automation.ContainerClickSequence;
import dev.helikon.client.automation.MinecraftContainerClicker;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.gui.GameplayScreenPolicy;
import dev.helikon.client.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Version-bound bridge for loaded-world observations and normal block-use interactions.
 * It never edits world state, teleports, constructs custom placement packets, or assumes acceptance.
 */
public final class MinecraftDomainExpansionAccess {
    private final DomainExpansion module;
    private final FriendManager friends;
    private final Consumer<String> notifier;
    private final DomainInventoryOwnership slotOwnership = new DomainInventoryOwnership();
    private MovedInventory movedInventory;
    private boolean activationKeyDown;

    public MinecraftDomainExpansionAccess(DomainExpansion module, FriendManager friends, Consumer<String> notifier) {
        this.module = Objects.requireNonNull(module, "module");
        this.friends = Objects.requireNonNull(friends, "friends");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        module.setCleanupHook(this::restoreState);
    }

    /** Runs one state-machine tick and returns true only when the registry should disable the module. */
    public boolean tick(long tick) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled()) {
            restoreState();
            return false;
        }
        if (client.player == null || client.level == null || client.gameMode == null) {
            restoreState();
            return true;
        }
        if (GameplayScreenPolicy.blocksAutomation(client.gui.screen())) {
            return false;
        }

        LocalPlayer local = client.player;
        List<DomainTarget> targets = targets(client, local);
        InventorySelection selection = inventorySelection(client);
        boolean inventoryConflict = !module.silentSwitch()
                && slotOwnership.ownedSlot() >= 0
                && slotOwnership.hasConflict(local.getInventory().getSelectedSlot());
        MinecraftWorldView world = new MinecraftWorldView(client, local);
        DomainExpansion.TickResult result = module.tick(new DomainExpansion.Context(
                tick,
                local.isAlive(),
                position(local.blockPosition()),
                targets,
                selection.availableBlocks(),
                false,
                inventoryConflict,
                world
        ));

        for (DomainPosition placement : result.placements()) {
            Optional<BlockCandidate> candidate = selection.bestHotbar();
            if (candidate.isEmpty()) {
                selection = inventorySelection(client);
                candidate = selection.bestHotbar();
            }
            boolean accepted = candidate.isPresent() && place(client, placement, candidate.get(), world);
            module.recordPlacementAttempt(placement, accepted, tick);
        }
        if (result.completedNow()) {
            notifier.accept("DOMAIN COMPLETE");
        } else if (result.cancelReason() != DomainExpansion.CancelReason.NONE
                && module.activationMode() == DomainExpansion.ActivationMode.MANUAL) {
            notifier.accept("Domain Expansion cancelled: "
                    + result.cancelReason().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
        }
        return result.disableRequested();
    }

    /** Polls the assigned module key so Manual Final Seal can reserve a later key press. */
    public void pollActivationKey(boolean down) {
        boolean pressed = down && !activationKeyDown;
        activationKeyDown = down;
        if (!down) {
            module.releaseFinalSealInput();
        }
        if (pressed && module.consumesKeybindInput()) {
            module.requestFinalSeal();
        }
    }

    /** Restores only state still owned by this adapter, preserving external user/module changes. */
    public void restoreState() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            LocalPlayer player = client.player;
            int current = player.getInventory().getSelectedSlot();
            int restoration = slotOwnership.restorationSlot(current);
            if (restoration >= 0) {
                player.getInventory().setSelectedSlot(restoration);
                if (client.gameMode != null) {
                    ((MultiPlayerGameModeAccessor) client.gameMode).helikon$ensureHasSentCarriedItem();
                }
            }
            restoreMovedInventory(client);
        } else {
            slotOwnership.clear();
            movedInventory = null;
        }
    }

    private boolean place(Minecraft client, DomainPosition position, BlockCandidate candidate,
                          MinecraftWorldView world) {
        if (!world.loaded(position) || !world.replaceable(position) || world.intersectsPlayer(position)) {
            return false;
        }
        Optional<BlockHitResult> hit = placementHit(client, blockPos(position));
        if (hit.isEmpty() || client.player.getEyePosition().distanceTo(hit.get().getLocation())
                > client.player.blockInteractionRange() || !visible(client, hit.get())) {
            return false;
        }
        int slot = candidate.inventorySlot();
        if (slot < 0 || slot > 8 || client.player.getInventory().getItem(slot).getItem() != candidate.item()) {
            return false;
        }

        LocalPlayer player = client.player;
        int previousSlot = player.getInventory().getSelectedSlot();
        if (module.silentSwitch()) {
            if (previousSlot != slot) {
                player.getInventory().setSelectedSlot(slot);
            }
        } else {
            slotOwnership.acquire(previousSlot, slot);
            if (previousSlot != slot) {
                player.getInventory().setSelectedSlot(slot);
            }
        }

        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        Rotation rotation = rotation(player.getEyePosition(), hit.get().getLocation());
        applyRotation(player, rotation);
        try {
            InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit.get());
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
            }
            return !(result instanceof InteractionResult.Fail);
        } finally {
            restoreRotation(player, originalYaw, originalPitch);
            if (module.silentSwitch() && previousSlot != slot
                    && player.getInventory().getSelectedSlot() == slot) {
                player.getInventory().setSelectedSlot(previousSlot);
                ((MultiPlayerGameModeAccessor) client.gameMode).helikon$ensureHasSentCarriedItem();
            }
        }
    }

    private void applyRotation(LocalPlayer player, Rotation rotation) {
        switch (module.rotationMode()) {
            case NONE -> {
            }
            case VISIBLE -> {
                player.setYRot(rotation.yaw());
                player.setXRot(rotation.pitch());
            }
            case SILENT -> player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    rotation.yaw(), rotation.pitch(), player.onGround(), player.horizontalCollision));
        }
    }

    private void restoreRotation(LocalPlayer player, float yaw, float pitch) {
        switch (module.rotationMode()) {
            case NONE -> {
            }
            case VISIBLE -> {
                player.setYRot(yaw);
                player.setXRot(pitch);
            }
            case SILENT -> player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    yaw, pitch, player.onGround(), player.horizontalCollision));
        }
    }

    private InventorySelection inventorySelection(Minecraft client) {
        List<BlockCandidate> all = candidates(client.player);
        Optional<BlockCandidate> bestHotbar = all.stream()
                .filter(candidate -> candidate.inventorySlot() < 9)
                .min(candidateComparator());
        if (bestHotbar.isEmpty() && module.inventoryToHotbar()) {
            all.stream().filter(candidate -> candidate.inventorySlot() >= 9)
                    .min(candidateComparator())
                    .filter(candidate -> moveToHotbar(client, candidate))
                    .ifPresent(ignored -> {
                    });
            all = candidates(client.player);
            bestHotbar = all.stream().filter(candidate -> candidate.inventorySlot() < 9)
                    .min(candidateComparator());
        }
        int available = all.stream().mapToInt(BlockCandidate::count).sum();
        if (!module.inventoryToHotbar()) {
            available = all.stream().filter(candidate -> candidate.inventorySlot() < 9)
                    .mapToInt(BlockCandidate::count).sum();
        }
        return new InventorySelection(bestHotbar, available);
    }

    private List<BlockCandidate> candidates(LocalPlayer player) {
        List<BlockCandidate> result = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof BlockItem blockItem) || stack.isEmpty()) {
                continue;
            }
            Block block = blockItem.getBlock();
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            int priority = blockPriority(id);
            if (priority < 0 || !allowedBlock(block, block.defaultBlockState())) {
                continue;
            }
            result.add(new BlockCandidate(slot, stack.getItem(), stack.getCount(), priority,
                    block.getExplosionResistance()));
        }
        return List.copyOf(result);
    }

    private int blockPriority(String id) {
        List<String> allowed = module.allowedBlocks();
        int wildcard = -1;
        for (int index = 0; index < allowed.size(); index++) {
            String token = allowed.get(index).trim().toLowerCase(Locale.ROOT);
            if (token.equals(id)) {
                return index;
            }
            if (token.equals("*")) {
                wildcard = index;
            }
        }
        return wildcard;
    }

    private boolean allowedBlock(Block block, BlockState state) {
        if (!module.allowFallingBlocks() && block instanceof FallingBlock) {
            return false;
        }
        if (!module.allowContainers() && block instanceof BaseEntityBlock) {
            return false;
        }
        return state.isCollisionShapeFullBlock(Minecraft.getInstance().level, BlockPos.ZERO);
    }

    private Comparator<BlockCandidate> candidateComparator() {
        Comparator<BlockCandidate> comparator = Comparator.comparingInt(BlockCandidate::priority);
        if (module.preferBlastResistant()) {
            comparator = comparator.thenComparing(Comparator.comparingDouble(BlockCandidate::blastResistance)
                    .reversed());
        }
        return comparator.thenComparingInt(BlockCandidate::inventorySlot);
    }

    private boolean moveToHotbar(Minecraft client, BlockCandidate candidate) {
        LocalPlayer player = client.player;
        if (movedInventory != null || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty()) {
            return false;
        }
        int destination = firstAvailableHotbarSlot(player.getInventory());
        int sourceMenu = menuSlot(player.inventoryMenu, player.getInventory(), candidate.inventorySlot());
        int destinationMenu = menuSlot(player.inventoryMenu, player.getInventory(), destination);
        if (sourceMenu < 0 || destinationMenu < 0) {
            return false;
        }
        ItemStack displaced = player.getInventory().getItem(destination).copy();
        Item movedItem = candidate.item();
        if (!MinecraftContainerClicker.apply(client, ContainerClickSequence.swap(sourceMenu, destinationMenu))) {
            return false;
        }
        movedInventory = new MovedInventory(sourceMenu, destinationMenu, candidate.inventorySlot(), destination,
                movedItem, displaced);
        return true;
    }

    private void restoreMovedInventory(Minecraft client) {
        MovedInventory move = movedInventory;
        if (move == null || client.player == null || client.player.containerMenu != client.player.inventoryMenu) {
            return;
        }
        Inventory inventory = client.player.getInventory();
        ItemStack source = inventory.getItem(move.sourceInventorySlot());
        ItemStack destination = inventory.getItem(move.destinationInventorySlot());
        boolean sourceUnchanged = sameStack(source, move.displaced());
        boolean destinationOwned = destination.isEmpty() || destination.getItem() == move.movedItem();
        if (sourceUnchanged && destinationOwned
                && MinecraftContainerClicker.apply(client,
                ContainerClickSequence.swap(move.sourceMenuSlot(), move.destinationMenuSlot()))) {
            movedInventory = null;
        }
    }

    private static boolean sameStack(ItemStack current, ItemStack expected) {
        if (current.isEmpty() || expected.isEmpty()) {
            return current.isEmpty() && expected.isEmpty();
        }
        return current.getItem() == expected.getItem() && current.getCount() == expected.getCount()
                && current.getComponents().equals(expected.getComponents());
    }

    private static int firstAvailableHotbarSlot(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return inventory.getSelectedSlot();
    }

    private static int menuSlot(InventoryMenu menu, Inventory inventory, int inventorySlot) {
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            Slot candidate = menu.getSlot(slot);
            if (candidate.container == inventory && candidate.getContainerSlot() == inventorySlot) {
                return slot;
            }
        }
        return -1;
    }

    private List<DomainTarget> targets(Minecraft client, LocalPlayer local) {
        Map<String, Boolean> crosshair = new HashMap<>();
        if (client.hitResult instanceof EntityHitResult hit) {
            crosshair.put(hit.getEntity().getUUID().toString(), true);
        }
        Vec3 view = local.getViewVector(1.0F);
        List<DomainTarget> result = new ArrayList<>();
        for (Player player : client.level.players()) {
            if (player == local) {
                continue;
            }
            Vec3 delta = player.getEyePosition().subtract(local.getEyePosition());
            double distance = delta.length();
            double angle = distance == 0.0D ? 0.0D : Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D,
                    view.dot(delta) / distance))));
            Vec3 velocity = player.getDeltaMovement();
            Vec3 facing = player.getLookAngle();
            BlockPos feet = player.blockPosition();
            String id = player.getUUID().toString();
            String name = player.getGameProfile().name();
            result.add(new DomainTarget(id, name == null || name.isBlank() ? "unknown" : name,
                    position(feet), player.getX(), player.getZ(), distance, Math.max(0.0D, player.getHealth()), angle,
                    velocity.x, velocity.z, facing.x, facing.z, friends.contains(name), player.isAlive(),
                    player.isSpectator(), player.isCreative(),
                    client.level.hasChunk(feet.getX() >> 4, feet.getZ() >> 4),
                    crosshair.getOrDefault(id, false)));
        }
        return List.copyOf(result);
    }

    private static Optional<BlockHitResult> placementHit(Minecraft client, BlockPos target) {
        for (Direction face : Direction.values()) {
            BlockPos support = target.relative(face.getOpposite());
            if (!loaded(client, support)) {
                continue;
            }
            BlockState supportState = client.level.getBlockState(support);
            if (!supportState.canBeReplaced() && supportState.getFluidState().isEmpty()) {
                Vec3 location = Vec3.atCenterOf(support).add(
                        face.getStepX() * 0.5D,
                        face.getStepY() * 0.5D,
                        face.getStepZ() * 0.5D
                );
                return Optional.of(new BlockHitResult(location, face, support, false));
            }
        }
        return Optional.empty();
    }

    private static Rotation rotation(Vec3 eye, Vec3 hit) {
        double dx = hit.x - eye.x;
        double dy = hit.y - eye.y;
        double dz = hit.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Rotation(yaw, Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private static boolean visible(Minecraft client, BlockHitResult placement) {
        BlockHitResult hit = client.level.clip(new ClipContext(
                client.player.getEyePosition(),
                placement.getLocation(),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                client.player
        ));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(placement.getBlockPos());
    }

    private static boolean loaded(Minecraft client, BlockPos position) {
        return client.level != null && client.level.isInsideBuildHeight(position.getY())
                && client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    private static DomainPosition position(BlockPos position) {
        return new DomainPosition(position.getX(), position.getY(), position.getZ());
    }

    private static BlockPos blockPos(DomainPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private record BlockCandidate(int inventorySlot, Item item, int count, int priority, double blastResistance) {
    }

    private record InventorySelection(Optional<BlockCandidate> bestHotbar, int availableBlocks) {
    }

    private record MovedInventory(int sourceMenuSlot, int destinationMenuSlot, int sourceInventorySlot,
                                  int destinationInventorySlot, Item movedItem, ItemStack displaced) {
    }

    private record Rotation(float yaw, float pitch) {
    }

    private static final class MinecraftWorldView implements DomainExpansion.WorldView {
        private final Minecraft client;
        private final LocalPlayer local;

        private MinecraftWorldView(Minecraft client, LocalPlayer local) {
            this.client = client;
            this.local = local;
        }

        @Override
        public boolean loaded(DomainPosition position) {
            return MinecraftDomainExpansionAccess.loaded(client, blockPos(position));
        }

        @Override
        public boolean solid(DomainPosition position) {
            if (!loaded(position)) {
                return false;
            }
            BlockPos block = blockPos(position);
            return client.level.getBlockState(block).isCollisionShapeFullBlock(client.level, block);
        }

        @Override
        public boolean replaceable(DomainPosition position) {
            return loaded(position) && client.level.getBlockState(blockPos(position)).canBeReplaced();
        }

        @Override
        public boolean liquid(DomainPosition position) {
            return loaded(position) && !client.level.getBlockState(blockPos(position)).getFluidState().isEmpty();
        }

        @Override
        public boolean supported(DomainPosition position) {
            return placementHit(client, blockPos(position)).isPresent();
        }

        @Override
        public boolean reachable(DomainPosition position) {
            return placementHit(client, blockPos(position))
                    .map(hit -> local.getEyePosition().distanceTo(hit.getLocation()) <= local.blockInteractionRange()
                            && visible(client, hit))
                    .orElse(false);
        }

        @Override
        public boolean intersectsPlayer(DomainPosition position) {
            AABB block = new AABB(blockPos(position));
            for (Player player : client.level.players()) {
                if (block.intersects(player.getBoundingBox())) {
                    return true;
                }
            }
            return false;
        }
    }
}
