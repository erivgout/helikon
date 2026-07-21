package dev.helikon.client.module.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/** Applies FastBreak at Minecraft's shared manual/Baritone destroy boundary. */
public final class MinecraftFastBreakAccess {
    private static FastBreak module;
    private static BaritoneNavigation baritone;
    private static boolean applyingExtraSteps;

    private MinecraftFastBreakAccess() {
    }

    public static void install(FastBreak fastBreak, BaritoneNavigation navigation) {
        module = Objects.requireNonNull(fastBreak, "fastBreak");
        baritone = Objects.requireNonNull(navigation, "navigation");
    }

    /** Releases the mirrored Baritone delay promptly when FastBreak is toggled off. */
    public static void tick() {
        FastBreak current = module;
        BaritoneNavigation navigation = baritone;
        if (current != null && navigation != null && !current.isEnabled()) {
            navigation.synchronizeFastBreak(false, current.breakDelayTicks());
        }
    }

    /** Multiplies an accepted ordinary destroy step without recursively multiplying its own extra calls. */
    public static void afterDestroyStep(MultiPlayerGameMode gameMode, BlockPos position, Direction direction) {
        FastBreak current = module;
        if (current == null || applyingExtraSteps) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.isLoaded(position)) {
            return;
        }
        BlockState state = client.level.getBlockState(position);
        if (state.isAir() || state.getDestroySpeed(client.level, position) < 0.0F) {
            return;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (baritone != null) {
            baritone.synchronizeFastBreak(current.appliesTo(blockId), current.breakDelayTicks());
        }
        current.tick(true, true, blockId);
        int extraSteps = current.extraDestroySteps(true, true, blockId);
        if (extraSteps == 0) {
            return;
        }

        applyingExtraSteps = true;
        try {
            for (int step = 0; step < extraSteps && !client.level.getBlockState(position).isAir(); step++) {
                gameMode.continueDestroyBlock(position, direction);
            }
        } finally {
            applyingExtraSteps = false;
        }
    }
}
