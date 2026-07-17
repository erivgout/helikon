package dev.helikon.client.hud;

import dev.helikon.client.module.miscellaneous.InventoryPreview;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Renders a bounded read-only local inventory grid using Minecraft's supported HUD item extraction calls. */
public final class InventoryPreviewHud implements HudElement {
    private static final int BACKGROUND = 0xC014161B;
    private static final int SLOT_BACKGROUND = 0x80333A44;

    private final InventoryPreview module;
    private final PanicState panicState;

    public InventoryPreviewHud(InventoryPreview module, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        List<Integer> slots = InventoryPreviewLayout.slots(client.player.getInventory().getNonEquipmentItems().size(),
                module.rows(), module.includeHotbar());
        int rows = InventoryPreviewLayout.rowsFor(slots.size());
        if (rows == 0) {
            return;
        }
        int width = InventoryPreviewLayout.width();
        int height = InventoryPreviewLayout.height(rows);
        int x = Math.max(5, graphics.guiWidth() - width - 5);
        int y = 5;
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        for (int index = 0; index < slots.size(); index++) {
            int column = index % InventoryPreviewLayout.COLUMNS;
            int row = index / InventoryPreviewLayout.COLUMNS;
            int slotX = x + InventoryPreviewLayout.PADDING + column * InventoryPreviewLayout.SLOT_SIZE;
            int slotY = y + InventoryPreviewLayout.PADDING + row * InventoryPreviewLayout.SLOT_SIZE;
            ItemStack stack = client.player.getInventory().getNonEquipmentItems().get(slots.get(index));
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT_BACKGROUND);
            if (!stack.isEmpty()) {
                graphics.item(stack, slotX, slotY);
                graphics.itemDecorations(client.font, stack, slotX, slotY);
            }
        }
    }
}
