package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Narrow 26.2 bridge from {@link CrystalAura}'s Minecraft-free decision to Minecraft's ordinary
 * held-item-use and attack interactions. It reads already-loaded local blocks and entities, never
 * builds packets, and lets the server validate every attempted placement or detonation.
 */
public final class MinecraftCrystalAccess {
    /** Bound on how many nearest eligible targets the local scan considers. */
    private static final int MAX_TARGETS = 8;
    /** Vanilla item-use cooldown applied after a successful placement so cadence stays normal. */
    private static final int USE_COOLDOWN_TICKS = 4;

    private MinecraftCrystalAccess() {
    }

    /**
     * Performs at most one ordinary crystal placement or attack this tick. Returns {@code true} when
     * a normal interaction was requested, so the shared combat loop can keep one action per tick.
     */
    public static boolean tick(long tick, CrystalAura module, MinecraftCombatAccess.Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
            return false;
        }
        LocalPlayer player = client.player;
        Vec3 eye = player.getEyePosition();
        List<Vec3> targets = eligibleTargets(module, snapshot, eye);
        if (targets.isEmpty()) {
            return false;
        }
        List<CrystalHandle> crystals = crystalHandles(client, eye, targets, module.attackRange(), module.damageRadius());
        boolean placeReady = ((MinecraftAccessor) client).helikon$getRightClickDelay() == 0;
        boolean holdingCrystal = player.getInventory().getSelectedItem().getItem() == Items.END_CRYSTAL;
        // Bound the block scan cost: only run it when a placement could actually happen this tick.
        List<CrystalAura.Placement> placements = placeReady && holdingCrystal && module.placeEnabled()
                ? placementCandidates(client, eye, targets, crystals, module.placeRange(), module.damageRadius())
                : List.of();

        CrystalAura.State state = new CrystalAura.State(holdingCrystal, placeReady, attackReady(player), true,
                placements, crystals.stream().map(CrystalHandle::facts).toList());
        CrystalAura.Action action = module.decide(tick, state);
        return switch (action.type()) {
            case ATTACK -> attack(client, crystals, action.crystalId());
            case PLACE -> placeCrystal(client, action);
            case NONE -> false;
        };
    }

    private static boolean attack(Minecraft client, List<CrystalHandle> crystals, String crystalId) {
        for (CrystalHandle handle : crystals) {
            if (handle.facts().id().equals(crystalId)) {
                EndCrystal crystal = handle.entity();
                if (crystal.isRemoved() || !crystal.isAlive()) {
                    return false;
                }
                client.gameMode.attack(client.player, crystal);
                return true;
            }
        }
        return false;
    }

    private static boolean placeCrystal(Minecraft client, CrystalAura.Action action) {
        BlockPos support = new BlockPos(action.x(), action.y(), action.z());
        if (!isSupport(client.level.getBlockState(support)) || !isOpen(client, support.above())) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.0D, 0.5D, 0.0D), Direction.UP, support,
                false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        ((MinecraftAccessor) client).helikon$setRightClickDelay(USE_COOLDOWN_TICKS);
        return true;
    }

    private static List<Vec3> eligibleTargets(CrystalAura module, MinecraftCombatAccess.Snapshot snapshot, Vec3 eye) {
        List<Vec3> targets = new ArrayList<>();
        for (CombatTarget target : snapshot.targets()) {
            if (!target.alive() || target.distance() > module.targetRange()) {
                continue;
            }
            if (target.friend() && module.excludeFriends()) {
                continue;
            }
            if (!isEngaged(module, target.type())) {
                continue;
            }
            targets.add(eye.add(target.relativeX(), target.relativeY(), target.relativeZ()));
            if (targets.size() == MAX_TARGETS) {
                break;
            }
        }
        return targets;
    }

    private static boolean isEngaged(CrystalAura module, CombatEntityType type) {
        return switch (type) {
            case PLAYER -> module.allowPlayers();
            case HOSTILE -> module.allowHostiles();
            case PASSIVE -> false;
        };
    }

    private static List<CrystalHandle> crystalHandles(Minecraft client, Vec3 eye, List<Vec3> targets,
                                                      double attackRange, double damageRadius) {
        List<CrystalHandle> handles = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) {
                continue;
            }
            Vec3 center = crystal.position().add(0.0D, 0.5D, 0.0D);
            double playerDistance = eye.distanceTo(center);
            if (playerDistance > attackRange + 1.0D) {
                continue;
            }
            double targetDistance = nearestTargetDistance(center, targets);
            if (targetDistance > damageRadius + 1.0D) {
                continue;
            }
            handles.add(new CrystalHandle(crystal,
                    new CrystalAura.Crystal(crystal.getUUID().toString(), playerDistance, targetDistance)));
        }
        return handles;
    }

    private static List<CrystalAura.Placement> placementCandidates(Minecraft client, Vec3 eye, List<Vec3> targets,
                                                                   List<CrystalHandle> crystals, double placeRange,
                                                                   double damageRadius) {
        int radius = (int) Math.ceil(placeRange);
        BlockPos origin = BlockPos.containing(eye.x, eye.y, eye.z);
        List<CrystalAura.Placement> placements = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos support = origin.offset(dx, dy, dz);
                    if (!isLoaded(client, support) || !isSupport(client.level.getBlockState(support))
                            || !isOpen(client, support.above())) {
                        continue;
                    }
                    Vec3 center = new Vec3(support.getX() + 0.5D, support.getY() + 1.5D, support.getZ() + 0.5D);
                    double playerDistance = eye.distanceTo(center);
                    if (playerDistance > placeRange) {
                        continue;
                    }
                    double targetDistance = nearestTargetDistance(center, targets);
                    if (targetDistance > damageRadius || occupied(center, crystals)) {
                        continue;
                    }
                    placements.add(new CrystalAura.Placement(support.getX(), support.getY(), support.getZ(),
                            playerDistance, targetDistance));
                }
            }
        }
        return placements;
    }

    private static boolean occupied(Vec3 center, List<CrystalHandle> crystals) {
        for (CrystalHandle handle : crystals) {
            EndCrystal crystal = handle.entity();
            if (crystal.position().add(0.0D, 0.5D, 0.0D).distanceTo(center) < 1.0D) {
                return true;
            }
        }
        return false;
    }

    private static double nearestTargetDistance(Vec3 point, List<Vec3> targets) {
        double nearest = Double.MAX_VALUE;
        for (Vec3 target : targets) {
            nearest = Math.min(nearest, point.distanceTo(target));
        }
        return nearest;
    }

    private static boolean isSupport(BlockState state) {
        return state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK;
    }

    private static boolean isOpen(Minecraft client, BlockPos position) {
        return isLoaded(client, position) && client.level.getBlockState(position).isAir();
    }

    private static boolean isLoaded(Minecraft client, BlockPos position) {
        return client.level.isInsideBuildHeight(position.getY())
                && client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    private static boolean attackReady(LocalPlayer player) {
        return player.getAttackStrengthScale(0.0F) >= 0.9F;
    }

    /** Pairs the live crystal entity with the immutable facts handed to the decision. */
    private record CrystalHandle(EndCrystal entity, CrystalAura.Crystal facts) {
    }
}
