package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Plans a bounded local build preview and requests only normal held-block placements while Use is held. */
public final class BuilderAssist extends Module {
    public record Context(boolean useHeld, boolean heldBlock, BuilderPlan.Anchor anchor, Set<BuildPoint> replaceable) {
        public Context {
            anchor = Objects.requireNonNull(anchor, "anchor");
            replaceable = Set.copyOf(Objects.requireNonNull(replaceable, "replaceable"));
        }
    }

    private final EnumSetting<BuilderPlan.Mode> mode;
    private final NumberSetting length;
    private final NumberSetting width;
    private final NumberSetting height;
    private final BooleanSetting repeatPlacement;
    private final NumberSetting placementDelayTicks;
    private final ColorSetting previewColor;
    private final ColorSetting previewFillColor;
    private long nextActionTick;

    public BuilderAssist() {
        super("builder_assist", "BuilderAssist", "Previews bounded structures and places held blocks through normal use input.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        mode = addSetting(new EnumSetting<>("mode", "Mode", "Choose a bounded local build plan.",
                BuilderPlan.Mode.class, BuilderPlan.Mode.SINGLE));
        length = addSetting(new NumberSetting("length", "Length", "Line/floor length in blocks.", 4.0D, 1.0D, 16.0D));
        width = addSetting(new NumberSetting("width", "Width", "Floor/wall width in blocks.", 4.0D, 1.0D, 16.0D));
        height = addSetting(new NumberSetting("height", "Height", "Wall/vertical-line height in blocks.", 4.0D, 1.0D, 16.0D));
        repeatPlacement = addSetting(new BooleanSetting("repeat_placement", "Repeat placement",
                "Continue with the next replaceable preview block while Use remains held.", true));
        placementDelayTicks = addSetting(new NumberSetting("placement_delay_ticks", "Placement delay",
                "Minimum ticks between normal held-block use actions.", 4.0D, 1.0D, 40.0D));
        previewColor = addSetting(new ColorSetting("preview_color", "Preview color", "Local preview outline ARGB color.",
                0xFF64B5F6));
        previewFillColor = addSetting(new ColorSetting("preview_fill_color", "Preview fill color", "Local preview fill ARGB color.",
                0x2864B5F6));
    }

    public List<BuildPoint> preview(BuilderPlan.Anchor anchor) {
        return BuilderPlan.positions(mode.value(), anchor, size(length), size(width), size(height));
    }

    /** Returns one existing-block placement target after checking the pure bounded plan. */
    public Optional<BuildPoint> nextAction(long tick, Context context) {
        if (!isEnabled() || !context.useHeld() || !context.heldBlock() || tick < nextActionTick) {
            return Optional.empty();
        }
        List<BuildPoint> plan = preview(context.anchor());
        int limit = repeatPlacement.value() ? plan.size() : 1;
        for (int index = 0; index < limit; index++) {
            BuildPoint point = plan.get(index);
            if (context.replaceable().contains(point)) {
                nextActionTick = tick + Math.round(placementDelayTicks.value());
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    public int previewColor() {
        return previewColor.value();
    }

    public int previewFillColor() {
        return previewFillColor.value();
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }

    private static int size(NumberSetting setting) {
        return (int) Math.round(setting.value());
    }
}
