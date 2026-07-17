package dev.helikon.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow mapped accessor used only by AutoSign. */
@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
    @Accessor("messages")
    String[] helikon$messages();

    @Invoker("onDone")
    void helikon$done();
}
