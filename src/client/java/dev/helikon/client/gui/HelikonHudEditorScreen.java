package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.ActiveModules;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.ActiveModulesLayout;
import dev.helikon.client.hud.HudBounds;
import dev.helikon.client.hud.HudEditorState;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudElementPlacement;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * HUD editor for Active Modules presentation and bounded placement of each
 * registered custom HUD element.
 */
public final class HelikonHudEditorScreen extends Screen {
    private static final int COLOR_SCRIM = 0x90000000;
    private static final int COLOR_PANEL = 0xEE14171D;
    private static final int COLOR_OUTLINE = 0xFF2A2F38;
    private static final int COLOR_TEXT = 0xFFE6E6E6;
    private static final int COLOR_TEXT_DIM = 0xFF9AA1AB;
    private static final int COLOR_ACCENT = 0xFFE8A33D;
    private static final int COLOR_DISABLED = 0xFF777D86;
    private static final int CHECKBOX_SIZE = 8;
    private static final int HEADER_BOTTOM = 260;
    private static final HudElementId[] PLACEMENT_ELEMENTS = {
            HudElementId.WAYPOINTS, HudElementId.COORDINATES, HudElementId.SATURATION,
            HudElementId.ELYTRA, HudElementId.TARGET, HudElementId.REACH, HudElementId.INVENTORY_PREVIEW,
            HudElementId.DURABILITY_WARNINGS, HudElementId.RADAR, HudElementId.MINI_PLAYER,
            HudElementId.DEBUG_OVERLAY, HudElementId.BETTER_CROSSHAIR, HudElementId.DIRECTION, HudElementId.FPS,
            HudElementId.PING, HudElementId.TPS, HudElementId.SPEED, HudElementId.ARMOR_DURABILITY,
            HudElementId.HELD_ITEM_DURABILITY, HudElementId.POTION_EFFECTS, HudElementId.CLOCK,
            HudElementId.BIOME, HudElementId.SERVER_ADDRESS, HudElementId.TOTEM_COUNT
    };

    private final Screen parent;
    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final HudConfigurationManager configuration;
    private final HudEditorState state;
    private HudElementId selectedElement = HudElementId.WAYPOINTS;
    private boolean elementDragging;
    private int elementDragOffsetX;
    private int elementDragOffsetY;

    public HelikonHudEditorScreen(
            Screen parent,
            ModuleRegistry modules,
            HudLayout layout,
            HudConfigurationManager configuration
    ) {
        super(Component.translatable("screen.helikon.hud_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.state = new HudEditorState(layout);
    }

    @Override
    protected void init() {
        super.init();
        state.clampToViewport(width, height, previewBounds());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, COLOR_SCRIM);
        drawHeader(graphics);

        HudBounds bounds = previewBounds();
        ActiveModulesLayout style = layout.activeModules();
        graphics.pose().pushMatrix();
        graphics.pose().translate(style.x(), style.y());
        graphics.pose().scale(style.scale());
        ActiveModulesHud.draw(graphics, font, previewLines(), new HudBounds(0, 0,
                unscaledPreviewBounds().width(), unscaledPreviewBounds().height()),
                style.enabled() ? previewColor() : COLOR_DISABLED, true, style.padding(), style.alignment(),
                style.background(), style.textShadow());
        graphics.pose().popMatrix();
        drawElementPreview(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        ActiveModulesLayout style = layout.activeModules();
        if (isInside(mouseX, mouseY, 14, 126, 220, 11)) {
            selectedElement = nextElement();
            return true;
        }
        HudElementPlacement element = layout.element(selectedElement);
        if (isInside(mouseX, mouseY, 14, 139, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            element.setEnabled(!element.enabled());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 152, 220, 11)) {
            element.setAlignment(next(element.alignment()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 165, 220, 11)) {
            element.setBackground(!element.background());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 178, 220, 11)) {
            element.setTextShadow(!element.textShadow());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 191, 42, 11)) {
            element.setScale(Math.max(HudElementPlacement.MIN_SCALE, element.scale() - 0.25F));
            return true;
        }
        if (isInside(mouseX, mouseY, 59, 191, 42, 11)) {
            element.setScale(Math.min(HudElementPlacement.MAX_SCALE, element.scale() + 0.25F));
            return true;
        }
        if (isInside(mouseX, mouseY, 104, 191, 42, 11)) {
            element.setPadding(Math.max(HudElementPlacement.MIN_PADDING, element.padding() - 1));
            return true;
        }
        if (isInside(mouseX, mouseY, 149, 191, 42, 11)) {
            element.setPadding(Math.min(HudElementPlacement.MAX_PADDING, element.padding() + 1));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 204, 220, 11)) {
            element.setColor(nextColor(element.color()));
            element.setRainbow(false);
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 217, 220, 11)) {
            element.setRainbow(!element.rainbow());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 230, 220, 11)) {
            element.reset(selectedElement);
            return true;
        }
        HudBounds elementBounds = elementPreviewBounds();
        if (element.enabled() && elementBounds.contains(mouseX, mouseY)) {
            elementDragging = true;
            elementDragOffsetX = mouseX - elementBounds.x();
            elementDragOffsetY = mouseY - elementBounds.y();
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            style.setEnabled(!style.enabled());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 48, 220, 11)) {
            style.setSort(next(style.sort()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 61, 220, 11)) {
            style.setAlignment(next(style.alignment()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 74, 220, 11)) {
            style.setColorMode(next(style.colorMode()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 87, 220, 11)) {
            style.setBackground(!style.background());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 100, 220, 11)) {
            if (mouseX >= 194) {
                style.setAnimations(!style.animations());
            } else {
                style.setTextShadow(!style.textShadow());
            }
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 113, 42, 11)) {
            style.setScale(Math.max(ActiveModulesLayout.MIN_SCALE, style.scale() - 0.25F));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 59, 113, 42, 11)) {
            style.setScale(Math.min(ActiveModulesLayout.MAX_SCALE, style.scale() + 0.25F));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 104, 113, 42, 11)) {
            style.setPadding(Math.max(ActiveModulesLayout.MIN_PADDING, style.padding() - 1));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 149, 113, 42, 11)) {
            style.setPadding(Math.min(ActiveModulesLayout.MAX_PADDING, style.padding() + 1));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 194, 113, 42, 11)) {
            style.reset();
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        return state.beginDrag(mouseX, mouseY, previewBounds());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && elementDragging) {
            HudBounds bounds = elementPreviewBounds();
            int x = Math.clamp((int) event.x() - elementDragOffsetX, 0, Math.max(0, width - bounds.width()));
            int y = Math.clamp((int) event.y() - elementDragOffsetY, 0, Math.max(0, height - bounds.height()));
            layout.element(selectedElement).setAbsolutePosition(x, y);
            return true;
        }
        if (event.button() == 0 && state.dragTo((int) event.x(), (int) event.y(), width, height, previewBounds())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && elementDragging) {
            elementDragging = false;
            return true;
        }
        if (event.button() == 0 && state.isDragging()) {
            state.endDrag();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        super.removed();
        try {
            configuration.save(layout);
        } catch (ConfigurationException exception) {
            HelikonClient.LOGGER.log(Level.WARNING, "Unable to save HUD layout while closing the HUD editor", exception);
        }
    }

    private void drawHeader(GuiGraphicsExtractor graphics) {
        graphics.fill(8, 8, width - 8, HEADER_BOTTOM, COLOR_PANEL);
        graphics.outline(8, 8, width - 16, HEADER_BOTTOM - 8, COLOR_OUTLINE);
        graphics.text(font, title, 14, 14, COLOR_ACCENT, true);
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.instructions"), 14, 25, COLOR_TEXT_DIM, false);

        ActiveModulesLayout style = layout.activeModules();
        if (style.enabled()) {
            graphics.fill(14, 31, 14 + CHECKBOX_SIZE, 31 + CHECKBOX_SIZE, COLOR_ACCENT);
        } else {
            graphics.outline(14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_TEXT_DIM);
        }
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.active_modules"), 27, 31, COLOR_TEXT, false);
        graphics.text(font, "Sort: " + style.sort().name().toLowerCase(), 14, 48, COLOR_TEXT_DIM, false);
        graphics.text(font, "Alignment: " + style.alignment().name().toLowerCase(), 14, 61, COLOR_TEXT_DIM, false);
        graphics.text(font, "Color: " + style.colorMode().name().toLowerCase(), 14, 74, COLOR_TEXT_DIM, false);
        graphics.text(font, "Background: " + (style.background() ? "on" : "off"), 14, 87, COLOR_TEXT_DIM, false);
        graphics.text(font, "Text shadow: " + (style.textShadow() ? "on" : "off"), 14, 100, COLOR_TEXT_DIM, false);
        graphics.text(font, "Animation: " + (style.animations() ? "on" : "off"), 194, 100, COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale -", 14, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale +", 59, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad -", 104, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad +", 149, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Reset", 194, 113, COLOR_ACCENT, false);
        HudElementPlacement element = layout.element(selectedElement);
        graphics.text(font, "HUD element: " + elementName(selectedElement) + " (click to change)",
                14, 126, COLOR_TEXT_DIM, false);
        if (element.enabled()) {
            graphics.fill(14, 139, 14 + CHECKBOX_SIZE, 139 + CHECKBOX_SIZE, COLOR_ACCENT);
        } else {
            graphics.outline(14, 139, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_TEXT_DIM);
        }
        graphics.text(font, "Show selected element; drag its preview", 27, 139, COLOR_TEXT, false);
        graphics.text(font, "Alignment: " + element.alignment().name().toLowerCase(), 14, 152, COLOR_TEXT_DIM, false);
        graphics.text(font, "Background: " + (element.background() ? "on" : "off"), 14, 165, COLOR_TEXT_DIM, false);
        graphics.text(font, "Text shadow: " + (element.textShadow() ? "on" : "off"), 14, 178, COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale -", 14, 191, COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale +", 59, 191, COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad -", 104, 191, COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad +", 149, 191, COLOR_TEXT_DIM, false);
        graphics.text(font, "Color: " + colorToken(element.color()) + " (click to cycle)", 14, 204, COLOR_TEXT_DIM, false);
        graphics.text(font, "Rainbow: " + (element.rainbow() ? "on" : "off"), 14, 217, COLOR_TEXT_DIM, false);
        graphics.text(font, "Reset selected element", 14, 230, COLOR_ACCENT, false);
    }

    private List<String> previewLines() {
        List<String> names = ActiveModules.enabledNames(modules, layout.activeModules().sort(), font::width);
        return names.isEmpty() ? List.of("No modules enabled") : names;
    }

    private HudBounds previewBounds() {
        HudBounds raw = unscaledPreviewBounds();
        float scale = layout.activeModules().scale();
        return new HudBounds(raw.x(), raw.y(), (int) Math.ceil(raw.width() * scale), (int) Math.ceil(raw.height() * scale));
    }

    private HudBounds unscaledPreviewBounds() {
        ActiveModulesLayout style = layout.activeModules();
        return ActiveModulesHud.bounds(font, previewLines(), style.x(), style.y(), style.padding());
    }

    private int previewColor() {
        return layout.activeModules().colorMode() == ActiveModulesLayout.ColorMode.RAINBOW
                ? ActiveModules.rainbowColor(System.currentTimeMillis() / 50L)
                : ActiveModulesHud.COLOR_TEXT;
    }

    private void drawElementPreview(GuiGraphicsExtractor graphics) {
        HudBounds bounds = elementPreviewBounds();
        HudElementPlacement placement = layout.element(selectedElement);
        int width = font.width(elementName(selectedElement)) + placement.padding() * 2;
        int height = font.lineHeight + placement.padding() * 2;
        int color = placement.enabled() ? (placement.rainbow()
                ? ActiveModules.rainbowColor(System.currentTimeMillis() / 50L) : placement.color()) : COLOR_DISABLED;
        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.x(), bounds.y());
        graphics.pose().scale(placement.scale());
        if (placement.background()) {
            graphics.fill(0, 0, width, height, COLOR_PANEL);
        }
        graphics.outline(0, 0, width, height, COLOR_ACCENT);
        int textX = switch (placement.alignment()) {
            case LEFT -> placement.padding();
            case CENTER -> (width - font.width(elementName(selectedElement))) / 2;
            case RIGHT -> width - placement.padding() - font.width(elementName(selectedElement));
        };
        graphics.text(font, elementName(selectedElement), textX, placement.padding(), color, placement.textShadow());
        graphics.pose().popMatrix();
    }

    private HudBounds elementPreviewBounds() {
        String name = elementName(selectedElement);
        HudElementPlacement placement = layout.element(selectedElement);
        int contentWidth = font.width(name) + placement.padding() * 2;
        int contentHeight = font.lineHeight + placement.padding() * 2;
        return placement.scaledBounds(width, height, contentWidth, contentHeight);
    }

    private HudElementId nextElement() {
        for (int index = 0; index < PLACEMENT_ELEMENTS.length; index++) {
            if (PLACEMENT_ELEMENTS[index] == selectedElement) {
                return PLACEMENT_ELEMENTS[(index + 1) % PLACEMENT_ELEMENTS.length];
            }
        }
        return PLACEMENT_ELEMENTS[0];
    }

    private static String elementName(HudElementId element) {
        return switch (element) {
            case WAYPOINTS -> "Waypoints";
            case COORDINATES -> "Coordinates";
            case SATURATION -> "Saturation";
            case ELYTRA -> "Elytra";
            case TARGET -> "Target HUD";
            case REACH -> "Reach";
            case INVENTORY_PREVIEW -> "Inventory Preview";
            case DURABILITY_WARNINGS -> "Durability warnings";
            case RADAR -> "Radar";
            case MINI_PLAYER -> "Mini Player";
            case DEBUG_OVERLAY -> "Debug Overlay";
            case BETTER_CROSSHAIR -> "Better Crosshair";
            case DIRECTION -> "Direction";
            case FPS -> "FPS";
            case PING -> "Ping";
            case TPS -> "TPS estimate";
            case SPEED -> "Speed";
            case ARMOR_DURABILITY -> "Armor durability";
            case HELD_ITEM_DURABILITY -> "Held-item durability";
            case POTION_EFFECTS -> "Potion effects";
            case CLOCK -> "Clock";
            case BIOME -> "Biome";
            case SERVER_ADDRESS -> "Server address";
            case TOTEM_COUNT -> "Totem count";
        };
    }

    private static <E extends Enum<E>> E next(E value) {
        E[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    private static int nextColor(int value) {
        int[] colors = {0xFFE5EDF5, 0xFFFFD180, 0xFF80D8FF, 0xFFA5D6A7, 0xFFFF8A80};
        for (int index = 0; index < colors.length; index++) {
            if (colors[index] == value) {
                return colors[(index + 1) % colors.length];
            }
        }
        return colors[0];
    }

    private static String colorToken(int color) {
        return String.format("#%08X", color);
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
