package dev.helikon.client.mixin;

import dev.helikon.client.chat.BetterChatDisplayAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Adjusts only the verified unfocused-chat alpha constants in Minecraft 26.2. */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$AlphaCalculator")
interface ChatAlphaCalculatorMixin {
    @ModifyConstant(method = "lambda$timeBased$0", constant = @Constant(doubleValue = 200.0D))
    private static double helikon$adjustVisibleDuration(double vanillaTicks) {
        return BetterChatDisplayAccess.totalLifetimeTicks();
    }

    @ModifyConstant(method = "lambda$timeBased$0", constant = @Constant(doubleValue = 10.0D))
    private static double helikon$adjustFadeMultiplier(double vanillaMultiplier) {
        return BetterChatDisplayAccess.fadeMultiplier();
    }
}
