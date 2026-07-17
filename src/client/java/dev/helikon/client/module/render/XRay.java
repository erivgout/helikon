package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.render.BlockIdList;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Objects;
import java.util.Set;

/** Local chunk-model filter with a bounded block-ID list and reversible geometry invalidation. */
public final class XRay extends Module {
    private static final String DEFAULT_BLOCKS = "minecraft:diamond_ore;minecraft:deepslate_diamond_ore;"
            + "minecraft:emerald_ore;minecraft:deepslate_emerald_ore;minecraft:ancient_debris;"
            + "minecraft:gold_ore;minecraft:deepslate_gold_ore;minecraft:iron_ore;"
            + "minecraft:deepslate_iron_ore;minecraft:redstone_ore;minecraft:deepslate_redstone_ore;"
            + "minecraft:lapis_ore;minecraft:deepslate_lapis_ore;minecraft:coal_ore;minecraft:deepslate_coal_ore";

    private final RendererInvalidator rendererInvalidator;
    private final StringSetting blocks;
    private final NumberSetting opacity;
    private Set<String> targetBlocks;

    public XRay() {
        this(() -> {
        });
    }

    public XRay(RendererInvalidator rendererInvalidator) {
        super("xray", "XRay", "Locally rebuilds chunk geometry to show only configured block types.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        this.rendererInvalidator = Objects.requireNonNull(rendererInvalidator, "rendererInvalidator");
        blocks = addSetting(new StringSetting("blocks", "Blocks",
                "Semicolon-separated block IDs kept by local XRay compilation; invalid entries are ignored.",
                DEFAULT_BLOCKS, 1_024, false));
        opacity = addSetting(new NumberSetting("opacity", "Opacity",
                "Local opacity for rendered configured block models; a renderer rebuild occurs after changes.",
                0.85D, 0.1D, 1.0D));
        targetBlocks = BlockIdList.parse(blocks.value());
        blocks.addChangeListener(ignored -> refreshTargets());
        opacity.addChangeListener(ignored -> reconcile());
    }

    public StringSetting blocks() {
        return blocks;
    }

    public NumberSetting opacity() {
        return opacity;
    }

    public Set<String> targetBlocks() {
        return targetBlocks;
    }

    @Override
    protected void onEnable() {
        reconcile();
    }

    @Override
    protected void onDisable() {
        XRayRenderAccess.deactivate();
        rendererInvalidator.invalidateGeometry();
    }

    private void refreshTargets() {
        targetBlocks = BlockIdList.parse(blocks.value());
        reconcile();
    }

    private void reconcile() {
        if (!isEnabled()) {
            return;
        }
        XRayRenderAccess.activate(targetBlocks, opacity.value().floatValue());
        rendererInvalidator.invalidateGeometry();
    }

    /** Thin platform boundary that requests a local renderer geometry rebuild. */
    @FunctionalInterface
    public interface RendererInvalidator {
        void invalidateGeometry();
    }
}
