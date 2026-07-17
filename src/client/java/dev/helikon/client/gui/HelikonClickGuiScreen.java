package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.input.HelikonKeybinds;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.ColorSettingText;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.NumberSettingText;
import dev.helikon.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final int HEADER_HEIGHT = 22;
    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int SIDEBAR_WIDTH = 72;
    private static final int SETTINGS_WIDTH = 132;
    private static final int ROW_HEIGHT = 14;
    private static final int MODULE_RESET_ROW_HEIGHT = 14;
    private static final int BIND_ROW_HEIGHT = 14;
    private static final int ENABLED_ROW_HEIGHT = 14;
    private static final int BOOLEAN_ROW_HEIGHT = 14;
    private static final int NUMBER_ROW_HEIGHT = 28;
    private static final int CHECKBOX_SIZE = 8;
    private static final int RESET_BUTTON_SIZE = 10;
    private static final int HUD_BUTTON_WIDTH = 30;
    private static final int THEME_BUTTON_WIDTH = 42;

    private int COLOR_PANEL;
    private int COLOR_HEADER;
    private int COLOR_SIDEBAR;
    private int COLOR_SETTINGS;
    private int COLOR_ROW_HOVER;
    private int COLOR_ROW_SELECTED;
    private int COLOR_ACCENT;
    private int COLOR_TEXT;
    private int COLOR_TEXT_DIM;
    private int COLOR_OUTLINE;
    private int COLOR_INVALID;
    private int COLOR_SCROLLBAR;

    private final ModuleRegistry modules;
    private final ConfigurationManager configuration;
    private final ClickGuiWindowState windowState;
    private final ClickGuiWindowDragState windowDrag;
    private final ClickGuiWindowResizeState windowResize;
    private final HudLayout hudLayout;
    private final HudConfigurationManager hudConfiguration;
    private final ClickGuiState state;
    private final KeybindAssignment keybindAssignment;
    private final KeyCaptureSuppression keyCaptureSuppression = new KeyCaptureSuppression();
    private final Map<Setting<?>, EditBox> textFields = new LinkedHashMap<>();

    private EditBox searchField;
    private double listScroll;
    private String keybindStatus = "";

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int listX;
    private int listWidth;
    private int settingsX;

    public HelikonClickGuiScreen(
            ModuleRegistry modules,
            ConfigurationManager configuration,
            ClickGuiWindowState windowState,
            HudLayout hudLayout,
            HudConfigurationManager hudConfiguration
    ) {
        super(Component.translatable("screen.helikon.title"));
        this.modules = Objects.requireNonNull(modules, "modules");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.windowState = Objects.requireNonNull(windowState, "windowState");
        this.windowDrag = new ClickGuiWindowDragState(this.windowState);
        this.windowResize = new ClickGuiWindowResizeState(this.windowState);
        this.hudLayout = Objects.requireNonNull(hudLayout, "hudLayout");
        this.hudConfiguration = Objects.requireNonNull(hudConfiguration, "hudConfiguration");
        this.state = new ClickGuiState(modules);
        this.keybindAssignment = new KeybindAssignment(HelikonKeybinds::isGuiKey);
        refreshThemeColors();
    }

    @Override
    protected void init() {
        ClickGuiWindowState.Size size = windowState.resolveSize(width, height);
        panelWidth = size.width();
        panelHeight = size.height();
        applyWindowPosition(windowState.resolve(width, height, panelWidth, panelHeight));

        searchField = new EditBox(font, searchFieldX(), panelY + 4, 110, 14,
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

    /**
     * Starts with no text field focused so the documented arrow-key navigation
     * works immediately; clicking or tabbing to an editor still focuses it.
     */
    @Override
    protected void setInitialFocus() {
        clearFocus();
    }

    /** Applies a resolved window position and moves the existing vanilla widgets with the panel. */
    private void applyWindowPosition(ClickGuiWindowState.Position position) {
        panelX = position.x();
        panelY = position.y();
        contentTop = panelY + HEADER_HEIGHT + 1;
        contentBottom = panelY + panelHeight - 4;
        listX = panelX + SIDEBAR_WIDTH + 1;
        settingsX = panelX + panelWidth - SETTINGS_WIDTH;
        listWidth = settingsX - listX - 1;

        if (searchField != null) {
            searchField.setX(searchFieldX());
            searchField.setY(panelY + 4);
        }

        Module module = state.selectedModule().orElse(null);
        if (module == null) {
            return;
        }
        for (SettingRow row : settingRows(module)) {
            if (row.setting() instanceof NumberSetting || row.setting() instanceof ColorSetting) {
                EditBox field = textFields.get(row.setting());
                if (field != null) {
                    field.setX(settingsX + 6);
                    field.setY(row.y() + 12);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        try {
            configuration.save(modules, windowState);
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
        refreshThemeColors();
        drawPanel(graphics, mouseX, mouseY);
        drawSidebar(graphics, mouseX, mouseY);
        drawModuleList(graphics, mouseX, mouseY);
        drawSettingsPanel(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void refreshThemeColors() {
        ClickGuiTheme theme = windowState.theme();
        COLOR_PANEL = theme.panel();
        COLOR_HEADER = theme.header();
        COLOR_SIDEBAR = theme.sidebar();
        COLOR_SETTINGS = theme.settings();
        COLOR_ROW_HOVER = theme.rowHover();
        COLOR_ROW_SELECTED = theme.rowSelected();
        COLOR_ACCENT = theme.accent();
        COLOR_TEXT = theme.text();
        COLOR_TEXT_DIM = theme.textDim();
        COLOR_OUTLINE = theme.outline();
        COLOR_INVALID = theme.invalid();
        COLOR_SCROLLBAR = theme.scrollbar();
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
        return handleHudEditorClick(mouseX, mouseY)
                || handleThemeEditorClick(mouseX, mouseY)
                || handleCategoryClick(mouseX, mouseY)
                || handleModuleListClick(mouseX, mouseY)
                || handleSettingsClick(mouseX, mouseY)
                || handleWindowResizeStart(mouseX, mouseY)
                || handleWindowDragStart(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && windowResize.resizeTo(
                (int) event.x(), (int) event.y(), new ClickGuiWindowState.Position(panelX, panelY), width, height
        )) {
            ClickGuiWindowState.Size size = windowState.resolveSize(width, height);
            panelWidth = size.width();
            panelHeight = size.height();
            applyWindowPosition(windowState.resolve(width, height, panelWidth, panelHeight));
            return true;
        }
        if (event.button() == 0 && windowDrag.dragTo(
                (int) event.x(), (int) event.y(), width, height, panelWidth, panelHeight
        )) {
            applyWindowPosition(windowState.resolve(width, height, panelWidth, panelHeight));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && windowResize.isResizing()) {
            windowResize.endResize();
            return true;
        }
        if (event.button() == 0 && windowDrag.isDragging()) {
            windowDrag.endDrag();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (keyCaptureSuppression.consumesKeyPress(event.key())) {
            return true;
        }
        KeybindAssignment.Result result = keybindAssignment.acceptKey(event.key());
        if (result != KeybindAssignment.Result.IGNORED) {
            if (isTerminalCaptureResult(result)) {
                keyCaptureSuppression.begin(event.key());
            }
            keybindStatus = keybindStatus(result);
            return true;
        }
        if (getFocused() == null && handleKeyboardNavigation(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (keyCaptureSuppression.consumesCharacterInput()) {
            return true;
        }
        if (keybindAssignment.target().isPresent()) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (keyCaptureSuppression.release(event.key())) {
            return true;
        }
        return super.keyReleased(event);
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
        graphics.fill(panelX + panelWidth - RESIZE_HANDLE_SIZE, panelY + panelHeight - 2,
                panelX + panelWidth - 2, panelY + panelHeight, COLOR_TEXT_DIM);

        int buttonColor = isInside(mouseX, mouseY, hudButtonX(), panelY + 4, HUD_BUTTON_WIDTH, 14)
                ? COLOR_ROW_HOVER : COLOR_OUTLINE;
        graphics.outline(hudButtonX(), panelY + 4, HUD_BUTTON_WIDTH, 14, buttonColor);
        graphics.centeredText(font, Component.translatable("screen.helikon.hud_button"),
                hudButtonX() + HUD_BUTTON_WIDTH / 2, panelY + 7, COLOR_TEXT);
        int themeButtonColor = isInside(mouseX, mouseY, themeButtonX(), panelY + 4, THEME_BUTTON_WIDTH, 14)
                ? COLOR_ROW_HOVER : COLOR_OUTLINE;
        graphics.outline(themeButtonX(), panelY + 4, THEME_BUTTON_WIDTH, 14, themeButtonColor);
        graphics.centeredText(font, Component.translatable("screen.helikon.theme_button"),
                themeButtonX() + THEME_BUTTON_WIDTH / 2, panelY + 7, COLOR_TEXT);
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

        int moduleResetY = settingControlsTop(module);
        drawWideButton(graphics, Component.translatable("screen.helikon.reset_module"), moduleResetY, mouseX, mouseY);

        int bindY = moduleResetY + MODULE_RESET_ROW_HEIGHT;
        String bindText = keybindAssignment.isAssigning(module)
                ? keybindStatus.isBlank() ? "Press a key..." : keybindStatus
                : "Bind: " + keyDisplayName(module.keybind());
        drawWideButton(graphics, Component.literal(font.plainSubstrByWidth(bindText, SETTINGS_WIDTH - 18)), bindY, mouseX, mouseY);

        int enabledY = bindY + BIND_ROW_HEIGHT;
        graphics.text(font, Component.translatable("screen.helikon.enabled"), textX, enabledY + 3, COLOR_TEXT, false);
        drawCheckbox(graphics, settingsCheckboxX(), enabledY + (ENABLED_ROW_HEIGHT - CHECKBOX_SIZE) / 2, module.isEnabled());

        for (SettingRow row : settingRows(module)) {
            Setting<?> setting = row.setting();
            String label = font.plainSubstrByWidth(setting.name(), SETTINGS_WIDTH - 38);
            graphics.text(font, label, textX, row.y() + 3, COLOR_TEXT, false);
            drawResetButton(graphics, settingResetX(), row.y() + 2, mouseX, mouseY);

            if (setting instanceof BooleanSetting booleanSetting) {
                drawCheckbox(graphics, settingsCheckboxX(),
                        row.y() + (BOOLEAN_ROW_HEIGHT - CHECKBOX_SIZE) / 2, booleanSetting.value());
            } else if (setting instanceof NumberSetting numberSetting) {
                String range = "(" + NumberSettingText.format(numberSetting.minimum())
                        + "–" + NumberSettingText.format(numberSetting.maximum()) + ")";
                int rangeWidth = font.width(range);
                graphics.text(font, range, settingResetX() - 4 - rangeWidth, row.y() + 3, COLOR_TEXT_DIM, false);
            } else if (setting instanceof ColorSetting colorSetting) {
                int swatchX = settingResetX() - 18;
                graphics.fill(swatchX, row.y() + 3, swatchX + 12, row.y() + 10, colorSetting.value());
                graphics.outline(swatchX, row.y() + 3, 12, 7, COLOR_TEXT_DIM);
            } else if (setting instanceof EnumSetting<?> enumSetting) {
                String value = enumSetting.valueId();
                int valueWidth = font.width(value);
                graphics.text(font, value, settingResetX() - 4 - valueWidth, row.y() + 3, COLOR_TEXT_DIM, false);
            }

            if (isInside(mouseX, mouseY, settingResetX(), row.y() + 2, RESET_BUTTON_SIZE, RESET_BUTTON_SIZE)) {
                graphics.setTooltipForNextFrame(font, Component.translatable("screen.helikon.reset_setting", setting.name()), mouseX, mouseY);
            } else if (isInside(mouseX, mouseY, settingsX, row.y(), SETTINGS_WIDTH, 10)) {
                graphics.setTooltipForNextFrame(font, Component.literal(setting.description()), mouseX, mouseY);
            }
        }
    }

    private void drawWideButton(GuiGraphicsExtractor graphics, Component text, int y, int mouseX, int mouseY) {
        int x = settingsX + 6;
        int buttonWidth = SETTINGS_WIDTH - 12;
        int color = isInside(mouseX, mouseY, x, y + 2, buttonWidth, 10) ? COLOR_ROW_HOVER : COLOR_OUTLINE;
        graphics.outline(x, y + 2, buttonWidth, 10, color);
        graphics.centeredText(font, text, x + buttonWidth / 2, y + 3, COLOR_TEXT_DIM);
    }

    private void drawResetButton(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        int color = isInside(mouseX, mouseY, x, y, RESET_BUTTON_SIZE, RESET_BUTTON_SIZE)
                ? COLOR_ACCENT : COLOR_TEXT_DIM;
        graphics.outline(x, y, RESET_BUTTON_SIZE, RESET_BUTTON_SIZE, color);
        graphics.centeredText(font, "R", x + RESET_BUTTON_SIZE / 2, y + 1, color);
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

    private boolean handleHudEditorClick(int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY, hudButtonX(), panelY + 4, HUD_BUTTON_WIDTH, 14)) {
            return false;
        }
        minecraft.setScreenAndShow(new HelikonHudEditorScreen(this, modules, hudLayout, hudConfiguration));
        return true;
    }

    private boolean handleThemeEditorClick(int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY, themeButtonX(), panelY + 4, THEME_BUTTON_WIDTH, 14)) {
            return false;
        }
        minecraft.setScreenAndShow(new HelikonThemeEditorScreen(this, modules, configuration, windowState));
        return true;
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
                keybindAssignment.cancel();
                keyCaptureSuppression.clear();
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

        int moduleResetY = settingControlsTop(module);
        if (isInside(mouseX, mouseY, settingsX + 6, moduleResetY + 2, SETTINGS_WIDTH - 12, 10)) {
            module.resetSettings();
            rebuildSettingWidgets();
            return true;
        }

        int bindY = moduleResetY + MODULE_RESET_ROW_HEIGHT;
        if (isInside(mouseX, mouseY, settingsX + 6, bindY + 2, SETTINGS_WIDTH - 12, 10)) {
            keybindAssignment.begin(module);
            keybindStatus = "";
            keyCaptureSuppression.clear();
            return true;
        }

        int enabledY = bindY + BIND_ROW_HEIGHT;
        if (isInside(mouseX, mouseY, settingsX, enabledY, SETTINGS_WIDTH, ENABLED_ROW_HEIGHT)) {
            modules.toggle(module);
            return true;
        }

        for (SettingRow row : settingRows(module)) {
            Setting<?> setting = row.setting();
            if (isInside(mouseX, mouseY, settingResetX(), row.y() + 2, RESET_BUTTON_SIZE, RESET_BUTTON_SIZE)) {
                setting.reset();
                rebuildSettingWidgets();
                return true;
            }
            if (setting instanceof BooleanSetting booleanSetting
                    && isInside(mouseX, mouseY, settingsX, row.y(), SETTINGS_WIDTH, BOOLEAN_ROW_HEIGHT)) {
                booleanSetting.set(!booleanSetting.value());
                return true;
            }
            if (setting instanceof EnumSetting<?> enumSetting
                    && isInside(mouseX, mouseY, settingsX, row.y(), SETTINGS_WIDTH, BOOLEAN_ROW_HEIGHT)) {
                enumSetting.cycle();
                return true;
            }
        }
        return false;
    }

    /** Handles ClickGUI navigation only while no text editor owns the keyboard. */
    private boolean handleKeyboardNavigation(int key) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            searchField.setValue("");
            state.selectAdjacentCategory(key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            keybindAssignment.cancel();
            keyCaptureSuppression.clear();
            listScroll = 0;
            rebuildSettingWidgets();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            state.selectAdjacentModule(key == GLFW.GLFW_KEY_UP ? -1 : 1);
            keybindAssignment.cancel();
            keyCaptureSuppression.clear();
            ensureSelectedModuleVisible();
            rebuildSettingWidgets();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            Module module = state.selectedModule().orElse(null);
            if (module == null) {
                return false;
            }
            modules.toggle(module);
            return true;
        }
        return false;
    }

    /** Scrolls just enough to keep the keyboard-selected module row on-screen. */
    private void ensureSelectedModuleVisible() {
        Module module = state.selectedModule().orElse(null);
        if (module == null) {
            return;
        }
        int index = state.visibleModules().indexOf(module);
        if (index < 0) {
            return;
        }
        int rowTop = index * ROW_HEIGHT;
        int rowBottom = rowTop + ROW_HEIGHT;
        int viewHeight = contentBottom - contentTop;
        if (rowTop < listScroll) {
            listScroll = rowTop;
        } else if (rowBottom > listScroll + viewHeight) {
            listScroll = rowBottom - viewHeight;
        }
        listScroll = clampScroll(listScroll);
    }

    /** Removes and recreates the text-setting edit boxes for the selection. */
    private void rebuildSettingWidgets() {
        textFields.values().forEach(this::removeWidget);
        textFields.clear();

        Module module = state.selectedModule().orElse(null);
        if (module == null) {
            return;
        }

        for (SettingRow row : settingRows(module)) {
            Setting<?> setting = row.setting();
            if (!(setting instanceof NumberSetting) && !(setting instanceof ColorSetting)) {
                continue;
            }
            EditBox field = new EditBox(font, settingsX + 6, row.y() + 12, SETTINGS_WIDTH - 12, 14,
                    Component.literal(setting.name()));
            field.setMaxLength(32);
            if (setting instanceof NumberSetting numberSetting) {
                field.setValue(NumberSettingText.format(numberSetting.value()));
                field.setResponder(text -> field.setTextColor(
                        NumberSettingText.tryApply(numberSetting, text) ? COLOR_TEXT : COLOR_INVALID));
            } else {
                ColorSetting colorSetting = (ColorSetting) setting;
                field.setValue(ColorSettingText.format(colorSetting.value()));
                field.setResponder(text -> field.setTextColor(
                        ColorSettingText.tryApply(colorSetting, text) ? COLOR_TEXT : COLOR_INVALID));
            }
            addRenderableWidget(field);
            textFields.put(setting, field);
        }
    }

    /** The y position where module reset controls start, below the wrapped description. */
    private int settingControlsTop(Module module) {
        int descriptionHeight = font.wordWrapHeight(Component.literal(module.description()), SETTINGS_WIDTH - 12);
        return contentTop + 4 + 10 + 12 + descriptionHeight + 4;
    }

    private List<SettingRow> settingRows(Module module) {
        List<SettingRow> rows = new ArrayList<>();
        int y = settingControlsTop(module) + MODULE_RESET_ROW_HEIGHT + BIND_ROW_HEIGHT + ENABLED_ROW_HEIGHT;
        for (Setting<?> setting : module.settings()) {
            int rowHeight = setting instanceof NumberSetting || setting instanceof ColorSetting
                    ? NUMBER_ROW_HEIGHT : BOOLEAN_ROW_HEIGHT;
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
        return settingsX + SETTINGS_WIDTH - CHECKBOX_SIZE - RESET_BUTTON_SIZE - 10;
    }

    private int settingResetX() {
        return settingsX + SETTINGS_WIDTH - RESET_BUTTON_SIZE - 6;
    }

    private int hudButtonX() {
        return panelX + panelWidth - 150;
    }

    private int themeButtonX() {
        return hudButtonX() - THEME_BUTTON_WIDTH - 4;
    }

    private int searchFieldX() {
        return panelX + panelWidth - 116;
    }

    private boolean handleWindowDragStart(int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY, panelX, panelY, panelWidth, HEADER_HEIGHT)) {
            return false;
        }
        windowDrag.beginDrag(mouseX, mouseY, new ClickGuiWindowState.Position(panelX, panelY));
        return true;
    }

    private boolean handleWindowResizeStart(int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY, panelX + panelWidth - RESIZE_HANDLE_SIZE,
                panelY + panelHeight - RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)) {
            return false;
        }
        // Resizing anchors the current top-left corner; without this, an
        // unsaved centered window would recenter after each size change.
        windowState.setPosition(panelX, panelY);
        windowResize.beginResize(mouseX, mouseY, new ClickGuiWindowState.Position(panelX, panelY),
                new ClickGuiWindowState.Size(panelWidth, panelHeight));
        return true;
    }

    private static String keybindStatus(KeybindAssignment.Result result) {
        return switch (result) {
            case ASSIGNED -> "Bound";
            case UNBOUND -> "Unbound";
            case CANCELLED -> "Cancelled";
            case RESERVED -> "GUI key is reserved";
            case INVALID -> "Unsupported key";
            case IGNORED -> "";
        };
    }

    private static boolean isTerminalCaptureResult(KeybindAssignment.Result result) {
        return result == KeybindAssignment.Result.ASSIGNED
                || result == KeybindAssignment.Result.UNBOUND
                || result == KeybindAssignment.Result.CANCELLED;
    }

    private static String keyDisplayName(Keybind keybind) {
        if (!keybind.isBound()) {
            return "Unbound";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keybind.keyCode()).getDisplayName().getString()
                + " (" + keybind.activation().name().toLowerCase(Locale.ROOT) + ")";
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record SettingRow(Setting<?> setting, int y, int height) {
    }
}
