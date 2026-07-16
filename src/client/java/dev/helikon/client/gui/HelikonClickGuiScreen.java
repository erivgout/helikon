package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.NumberSettingText;
import dev.helikon.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * The Helikon ClickGUI: a category sidebar, a scrollable searchable module
 * list, and an editor for the selected module's settings. The screen only
 * renders and forwards input; module lifecycle changes always go through
 * {@link ModuleRegistry} and persistence through {@link ConfigurationManager}
 * when the screen closes.
 */
public final class HelikonClickGuiScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 360;
    private static final int PANEL_MAX_HEIGHT = 220;
    private static final int HEADER_HEIGHT = 22;
    private static final int SIDEBAR_WIDTH = 72;
    private static final int SETTINGS_WIDTH = 132;
    private static final int ROW_HEIGHT = 14;
    private static final int ENABLED_ROW_HEIGHT = 14;
    private static final int BOOLEAN_ROW_HEIGHT = 14;
    private static final int NUMBER_ROW_HEIGHT = 28;
    private static final int CHECKBOX_SIZE = 8;

    private static final int COLOR_PANEL = 0xF014161B;
    private static final int COLOR_HEADER = 0xFF1C2027;
    private static final int COLOR_SIDEBAR = 0xFF181B21;
    private static final int COLOR_SETTINGS = 0xFF171A20;
    private static final int COLOR_ROW_HOVER = 0x22FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x33E8A33D;
    private static final int COLOR_ACCENT = 0xFFE8A33D;
    private static final int COLOR_TEXT = 0xFFE6E6E6;
    private static final int COLOR_TEXT_DIM = 0xFF9AA1AB;
    private static final int COLOR_OUTLINE = 0xFF2A2F38;
    private static final int COLOR_INVALID = 0xFFFF6060;
    private static final int COLOR_SCROLLBAR = 0xFF3A4150;

    private final ModuleRegistry modules;
    private final ConfigurationManager configuration;
    private final ClickGuiState state;
    private final Map<NumberSetting, EditBox> numberFields = new LinkedHashMap<>();

    private EditBox searchField;
    private double listScroll;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int listX;
    private int listWidth;
    private int settingsX;

    public HelikonClickGuiScreen(ModuleRegistry modules, ConfigurationManager configuration) {
        super(Component.translatable("screen.helikon.title"));
        this.modules = Objects.requireNonNull(modules, "modules");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.state = new ClickGuiState(modules);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(width - 16, PANEL_MAX_WIDTH);
        panelHeight = Math.min(height - 16, PANEL_MAX_HEIGHT);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        contentTop = panelY + HEADER_HEIGHT + 1;
        contentBottom = panelY + panelHeight - 4;
        listX = panelX + SIDEBAR_WIDTH + 1;
        settingsX = panelX + panelWidth - SETTINGS_WIDTH;
        listWidth = settingsX - listX - 1;

        searchField = new EditBox(font, panelX + panelWidth - 116, panelY + 4, 110, 14,
                Component.translatable("screen.helikon.search_hint"));
        searchField.setMaxLength(64);
        searchField.setHint(Component.translatable("screen.helikon.search_hint"));
        searchField.setValue(state.searchQuery());
        searchField.setResponder(text -> {
            state.setSearchQuery(text);
            listScroll = 0;
        });
        addRenderableWidget(searchField);

        rebuildSettingWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        try {
            configuration.save(modules);
        } catch (ConfigurationException exception) {
            HelikonClient.LOGGER.log(Level.WARNING, "Unable to save configuration while closing the ClickGUI", exception);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawPanel(graphics, mouseX, mouseY);
        drawSidebar(graphics, mouseX, mouseY);
        drawModuleList(graphics, mouseX, mouseY);
        drawSettingsPanel(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        clearFocus();
        if (event.button() != 0) {
            return false;
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        return handleCategoryClick(mouseX, mouseY)
                || handleModuleListClick(mouseX, mouseY)
                || handleSettingsClick(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            return true;
        }
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= contentTop && mouseY < contentBottom) {
            listScroll = clampScroll(listScroll - vertical * ROW_HEIGHT);
            return true;
        }
        return false;
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL);
        graphics.outline(panelX - 1, panelY - 1, panelWidth + 2, panelHeight + 2, COLOR_OUTLINE);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + HEADER_HEIGHT, COLOR_HEADER);
        graphics.text(font, title, panelX + 6, panelY + 7, COLOR_ACCENT, true);
    }

    private void drawSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(panelX, contentTop, panelX + SIDEBAR_WIDTH, panelY + panelHeight, COLOR_SIDEBAR);

        ModuleCategory[] categories = ModuleCategory.values();
        for (int index = 0; index < categories.length; index++) {
            ModuleCategory category = categories[index];
            int rowY = contentTop + index * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT > contentBottom) {
                break;
            }

            boolean selected = !state.isSearching() && category == state.selectedCategory();
            if (selected) {
                graphics.fill(panelX, rowY, panelX + SIDEBAR_WIDTH, rowY + ROW_HEIGHT, COLOR_ROW_SELECTED);
            } else if (isInside(mouseX, mouseY, panelX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT)) {
                graphics.fill(panelX, rowY, panelX + SIDEBAR_WIDTH, rowY + ROW_HEIGHT, COLOR_ROW_HOVER);
            }

            int textColor = selected ? COLOR_ACCENT : (state.isSearching() ? COLOR_TEXT_DIM : COLOR_TEXT);
            graphics.text(font, category.displayName(), panelX + 6, rowY + 3, textColor, false);
        }
    }

    private void drawModuleList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component heading = state.isSearching()
                ? Component.translatable("screen.helikon.search_results")
                : Component.literal(state.selectedCategory().displayName());
        graphics.text(font, heading, listX + 4, panelY + HEADER_HEIGHT - 12, COLOR_TEXT_DIM, false);

        List<Module> visible = state.visibleModules();
        listScroll = clampScroll(listScroll);

        if (visible.isEmpty()) {
            graphics.text(font, Component.translatable("screen.helikon.no_results"),
                    listX + 4, contentTop + 6, COLOR_TEXT_DIM, false);
            return;
        }

        graphics.enableScissor(listX, contentTop, listX + listWidth, contentBottom);
        for (int index = 0; index < visible.size(); index++) {
            Module module = visible.get(index);
            int rowY = contentTop + index * ROW_HEIGHT - (int) listScroll;
            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) {
                continue;
            }

            boolean selected = state.selectedModule().filter(module::equals).isPresent();
            boolean hovered = isInside(mouseX, mouseY, listX, rowY, listWidth, ROW_HEIGHT)
                    && mouseY >= contentTop && mouseY < contentBottom;
            if (selected) {
                graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT, COLOR_ROW_SELECTED);
            } else if (hovered) {
                graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT, COLOR_ROW_HOVER);
            }

            String name = font.plainSubstrByWidth(module.name(), listWidth - 24);
            graphics.text(font, name, listX + 4, rowY + 3, module.isEnabled() ? COLOR_ACCENT : COLOR_TEXT, false);
            drawCheckbox(graphics, toggleBoxX(), rowY + (ROW_HEIGHT - CHECKBOX_SIZE) / 2, module.isEnabled());
        }
        graphics.disableScissor();

        drawListScrollbar(graphics, visible.size());
    }

    private void drawListScrollbar(GuiGraphicsExtractor graphics, int rowCount) {
        int viewHeight = contentBottom - contentTop;
        int contentHeight = rowCount * ROW_HEIGHT;
        if (contentHeight <= viewHeight) {
            return;
        }

        int barX = listX + listWidth - 2;
        int barHeight = Math.max(8, viewHeight * viewHeight / contentHeight);
        int barY = contentTop + (int) ((viewHeight - barHeight) * listScroll / (contentHeight - viewHeight));
        graphics.fill(barX, contentTop, barX + 2, contentBottom, COLOR_OUTLINE);
        graphics.fill(barX, barY, barX + 2, barY + barHeight, COLOR_SCROLLBAR);
    }

    private void drawSettingsPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(settingsX, contentTop, panelX + panelWidth, panelY + panelHeight, COLOR_SETTINGS);

        Module module = state.selectedModule().orElse(null);
        if (module == null) {
            graphics.text(font, Component.translatable("screen.helikon.no_selection"),
                    settingsX + 6, contentTop + 6, COLOR_TEXT_DIM, false);
            return;
        }

        int textX = settingsX + 6;
        int y = contentTop + 4;
        graphics.text(font, font.plainSubstrByWidth(module.name(), SETTINGS_WIDTH - 12), textX, y, COLOR_ACCENT, false);
        y += 10;
        String meta = module.category().displayName() + " · " + module.id();
        graphics.text(font, font.plainSubstrByWidth(meta, SETTINGS_WIDTH - 12), textX, y, COLOR_TEXT_DIM, false);
        y += 12;
        graphics.textWithWordWrap(font, Component.literal(module.description()), textX, y,
                SETTINGS_WIDTH - 12, COLOR_TEXT_DIM);

        int enabledY = settingControlsTop(module);
        graphics.text(font, Component.translatable("screen.helikon.enabled"), textX, enabledY + 3, COLOR_TEXT, false);
        drawCheckbox(graphics, settingsCheckboxX(), enabledY + (ENABLED_ROW_HEIGHT - CHECKBOX_SIZE) / 2, module.isEnabled());

        for (SettingRow row : settingRows(module)) {
            Setting<?> setting = row.setting();
            String label = font.plainSubstrByWidth(setting.name(), SETTINGS_WIDTH - 24);
            graphics.text(font, label, textX, row.y() + 3, COLOR_TEXT, false);

            if (setting instanceof BooleanSetting booleanSetting) {
                drawCheckbox(graphics, settingsCheckboxX(),
                        row.y() + (BOOLEAN_ROW_HEIGHT - CHECKBOX_SIZE) / 2, booleanSetting.value());
            } else if (setting instanceof NumberSetting numberSetting) {
                String range = "(" + NumberSettingText.format(numberSetting.minimum())
                        + "–" + NumberSettingText.format(numberSetting.maximum()) + ")";
                int rangeWidth = font.width(range);
                graphics.text(font, range, settingsX + SETTINGS_WIDTH - 6 - rangeWidth, row.y() + 3, COLOR_TEXT_DIM, false);
            }

            if (isInside(mouseX, mouseY, settingsX, row.y(), SETTINGS_WIDTH, 10)) {
                graphics.setTooltipForNextFrame(font, Component.literal(setting.description()), mouseX, mouseY);
            }
        }
    }

    private void drawCheckbox(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        if (checked) {
            graphics.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, COLOR_ACCENT);
        } else {
            graphics.outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_TEXT_DIM);
        }
    }

    private boolean handleCategoryClick(int mouseX, int mouseY) {
        ModuleCategory[] categories = ModuleCategory.values();
        for (int index = 0; index < categories.length; index++) {
            int rowY = contentTop + index * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT > contentBottom) {
                break;
            }
            if (isInside(mouseX, mouseY, panelX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT)) {
                state.selectCategory(categories[index]);
                searchField.setValue("");
                listScroll = 0;
                return true;
            }
        }
        return false;
    }

    private boolean handleModuleListClick(int mouseX, int mouseY) {
        if (mouseX < listX || mouseX >= listX + listWidth || mouseY < contentTop || mouseY >= contentBottom) {
            return false;
        }

        List<Module> visible = state.visibleModules();
        for (int index = 0; index < visible.size(); index++) {
            int rowY = contentTop + index * ROW_HEIGHT - (int) listScroll;
            if (!isInside(mouseX, mouseY, listX, rowY, listWidth, ROW_HEIGHT)) {
                continue;
            }

            Module module = visible.get(index);
            if (mouseX >= toggleBoxX() - 3) {
                modules.toggle(module);
            } else {
                state.selectModule(module);
                rebuildSettingWidgets();
            }
            return true;
        }
        return true;
    }

    private boolean handleSettingsClick(int mouseX, int mouseY) {
        Module module = state.selectedModule().orElse(null);
        if (module == null || mouseX < settingsX || mouseX >= panelX + panelWidth) {
            return false;
        }

        int enabledY = settingControlsTop(module);
        if (isInside(mouseX, mouseY, settingsX, enabledY, SETTINGS_WIDTH, ENABLED_ROW_HEIGHT)) {
            modules.toggle(module);
            return true;
        }

        for (SettingRow row : settingRows(module)) {
            if (row.setting() instanceof BooleanSetting booleanSetting
                    && isInside(mouseX, mouseY, settingsX, row.y(), SETTINGS_WIDTH, BOOLEAN_ROW_HEIGHT)) {
                booleanSetting.set(!booleanSetting.value());
                return true;
            }
        }
        return false;
    }

    /** Removes and recreates the number-setting edit boxes for the selection. */
    private void rebuildSettingWidgets() {
        numberFields.values().forEach(this::removeWidget);
        numberFields.clear();

        Module module = state.selectedModule().orElse(null);
        if (module == null) {
            return;
        }

        for (SettingRow row : settingRows(module)) {
            if (!(row.setting() instanceof NumberSetting numberSetting)) {
                continue;
            }
            EditBox field = new EditBox(font, settingsX + 6, row.y() + 12, SETTINGS_WIDTH - 12, 14,
                    Component.literal(numberSetting.name()));
            field.setMaxLength(32);
            field.setValue(NumberSettingText.format(numberSetting.value()));
            field.setResponder(text ->
                    field.setTextColor(NumberSettingText.tryApply(numberSetting, text) ? COLOR_TEXT : COLOR_INVALID));
            addRenderableWidget(field);
            numberFields.put(numberSetting, field);
        }
    }

    /** The y position where the enabled row starts, below the wrapped description. */
    private int settingControlsTop(Module module) {
        int descriptionHeight = font.wordWrapHeight(Component.literal(module.description()), SETTINGS_WIDTH - 12);
        return contentTop + 4 + 10 + 12 + descriptionHeight + 4;
    }

    private List<SettingRow> settingRows(Module module) {
        List<SettingRow> rows = new ArrayList<>();
        int y = settingControlsTop(module) + ENABLED_ROW_HEIGHT;
        for (Setting<?> setting : module.settings()) {
            int rowHeight = setting instanceof NumberSetting ? NUMBER_ROW_HEIGHT : BOOLEAN_ROW_HEIGHT;
            rows.add(new SettingRow(setting, y, rowHeight));
            y += rowHeight;
        }
        return rows;
    }

    private double clampScroll(double scroll) {
        int contentHeight = state.visibleModules().size() * ROW_HEIGHT;
        int viewHeight = contentBottom - contentTop;
        return Math.clamp(scroll, 0, Math.max(0, contentHeight - viewHeight));
    }

    private int toggleBoxX() {
        return listX + listWidth - CHECKBOX_SIZE - 8;
    }

    private int settingsCheckboxX() {
        return settingsX + SETTINGS_WIDTH - CHECKBOX_SIZE - 6;
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record SettingRow(Setting<?> setting, int y, int height) {
    }
}
