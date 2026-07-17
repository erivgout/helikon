package dev.helikon.client.hud;

import dev.helikon.client.module.miscellaneous.DurabilityWarnings;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Displays bounded warnings for already-loaded local held-item and armor durability facts. */
public final class DurabilityWarningsHud implements HudElement {
    private static final int X = 5;
    private static final int Y = 112;
    private static final int PADDING = 3;

    private final DurabilityWarnings module;
    private final PanicState panicState;
    private final HudLayout layout;

    public DurabilityWarningsHud(DurabilityWarnings module, PanicState panicState) {
        this(module, panicState, new HudLayout());
    }

    public DurabilityWarningsHud(DurabilityWarnings module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        HudElementPlacement placement = layout.element(HudElementId.DURABILITY_WARNINGS);
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        List<DurabilityWarnings.Item> warnings = module.warnings(observedItems(client, module));
        if (warnings.isEmpty()) {
            return;
        }
        List<String> lines = warnings.stream().map(DurabilityWarningsHud::format).toList();
        HudPresentation.drawLines(graphics, client.font, lines, placement);
    }

    private static List<DurabilityWarnings.Item> observedItems(Minecraft client, DurabilityWarnings module) {
        List<DurabilityWarnings.Item> items = new ArrayList<>(5);
        if (module.includeHeldItem()) {
            addIfDamageable(items, "Held", client.player.getMainHandItem());
        }
        if (module.includeArmor()) {
            addIfDamageable(items, "Helmet", client.player.getItemBySlot(EquipmentSlot.HEAD));
            addIfDamageable(items, "Chest", client.player.getItemBySlot(EquipmentSlot.CHEST));
            addIfDamageable(items, "Legs", client.player.getItemBySlot(EquipmentSlot.LEGS));
            addIfDamageable(items, "Boots", client.player.getItemBySlot(EquipmentSlot.FEET));
        }
        return List.copyOf(items);
    }

    private static void addIfDamageable(List<DurabilityWarnings.Item> items, String label, ItemStack stack) {
        if (stack.isDamageableItem()) {
            int maximum = stack.getMaxDamage();
            int remaining = Math.max(0, maximum - stack.getDamageValue());
            items.add(new DurabilityWarnings.Item(label, remaining, maximum));
        }
    }

    private static String format(DurabilityWarnings.Item item) {
        int percent = (int) Math.round(item.remaining() * 100.0D / item.maximum());
        return String.format(Locale.ROOT, "%s durability %d/%d (%d%%)", item.label(), item.remaining(), item.maximum(), percent);
    }
}
