package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.input.HelikonKeybinds;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.KeybindConflicts;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.ModuleSearch;
import dev.helikon.client.setting.ActionSetting;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorPickerValue;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.NumberSettingText;
import dev.helikon.client.setting.NumberSlider;
import dev.helikon.client.setting.Setting;
import dev.helikon.client.setting.SettingText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * The Helikon ClickGUI: one draggable floating panel per module category
 * (plus Favorites and Active) with an icon header, accent-highlighted enabled modules,
 * and inline settings expanded beneath a module row, plus a floating search
 * palette. The screen only renders and forwards input; module lifecycle
 * changes always go through {@link ModuleRegistry} and persistence through
 * {@link ConfigurationManager} when the screen closes.
 *
 * <p>Mouse model: left-click a row toggles the module, right-click expands its
 * settings, middle-click stars it as a favorite. Left-drag a header moves the
 * panel and right-click collapses it to its header.</p>
 */
public final class HelikonClickGuiScreen extends Screen {
    private static final String SEARCH_PANEL_KEY = "search";
    private static final String FAVORITES_PANEL_KEY = "favorites";
    private static final String ACTIVE_PANEL_KEY = "active";

    private static final Style UI_STYLE = HelikonUiFont.STYLE;

    private static final int BASE_PANEL_WIDTH = 98;
    private static final int BASE_HEADER_HEIGHT = 14;
    private static final int BASE_ROW_HEIGHT = 13;
    private static final int BASE_SETTING_ROW_HEIGHT = 12;
    private static final int BASE_EDIT_ROW_HEIGHT = 16;
    private static final int BASE_SEARCH_WIDTH = 190;
    private static final int BASE_SEARCH_ROW_HEIGHT = 20;
    private static final int BASE_RESULT_ROW_HEIGHT = 12;
    private static final int SEARCH_VISIBLE_RESULT_ROWS = 8;
    private static final int BASE_SLIDER_TRACK_WIDTH = 38;
    private static final int COLOR_PICKER_EXTRA = 23;
    private static final int COLOR_PICKER_CHANNEL_HEIGHT = 4;
    private static final int PANEL_MARGIN = 8;
    private static final int SCREEN_EDGE_MARGIN = 8;

    private static final int COLOR_TOGGLE_ON_KNOB = 0xFFF5F6FA;
    private static final int COLOR_TOGGLE_OFF_TRACK = 0xFF43474F;
    private static final int COLOR_TOGGLE_OFF_KNOB = 0xFFA6ABB5;
    private static final int COLOR_ROW_DIVIDER = 0x50000000;
    private static final int COLOR_SETTINGS_INSET = 0x28000000;
    private static final int COLOR_PANEL_SHADOW = 0x30000000;
    private static final int COLOR_EDIT_BACKGROUND = 0x66000000;

    private int COLOR_PANEL;
    private int COLOR_HEADER;
    private int COLOR_ROW_HOVER;
    private int COLOR_ACCENT;
    private int COLOR_ACCENT_LIGHT;
    private int COLOR_SETTING_LABEL;
    private int COLOR_TEXT;
    private int COLOR_TEXT_DIM;
    private int COLOR_OUTLINE;
    private int COLOR_INVALID;
    private int COLOR_SLIDER_TRACK;

    private final ModuleRegistry modules;
    private final ConfigurationManager configuration;
    private final ClickGuiWindowState windowState;
    private final HudLayout hudLayout;
    private final HudConfigurationManager hudConfiguration;
    private final KeybindAssignment keybindAssignment;
    private final KeyCaptureSuppression keyCaptureSuppression = new KeyCaptureSuppression();
    private final Map<PanelSetting, EditBox> textFields = new LinkedHashMap<>();
    private final Set<Setting<?>> expandedColorPickers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<String, Double> panelScrolls = new HashMap<>();

    /** Panels in draw order; the last entry renders topmost and is hit-tested first. */
    private final List<PanelView> panels = new ArrayList<>();

    private EditBox searchField;
    private String searchQuery = "";
    private String keybindStatus = "";

    private PanelView draggedPanel;
    private int dragOffsetX;
    private int dragOffsetY;
    private SliderDrag activeSlider;
    private ColorDrag activeColorPicker;
    private final ClickGuiScrollbarState activeScrollbar = new ClickGuiScrollbarState();
    private PanelView scrollbarPanel;

    private int panelWidth;
    private int headerHeight;
    private int rowHeight;
    private int settingRowHeight;
    private int editRowHeight;
    private int searchWidth;
    private int searchRowHeight;
    private int resultRowHeight;
    private int sliderTrackWidth;

    /** Opens whichever ClickGUI layout the user last selected. */
    public static Screen create(
            ModuleRegistry modules,
            ConfigurationManager configuration,
            ClickGuiWindowState windowState,
            HudLayout hudLayout,
            HudConfigurationManager hudConfiguration
    ) {
        return windowState.classicLayout()
                ? new HelikonClassicClickGuiScreen(modules, configuration, windowState, hudLayout, hudConfiguration)
                : new HelikonClickGuiScreen(modules, configuration, windowState, hudLayout, hudConfiguration);
    }

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
        this.hudLayout = Objects.requireNonNull(hudLayout, "hudLayout");
        this.hudConfiguration = Objects.requireNonNull(hudConfiguration, "hudConfiguration");
        this.keybindAssignment = new KeybindAssignment(HelikonKeybinds::isGuiKey);
        refreshThemeColors();
    }

    @Override
    protected void init() {
        float scale = windowState.interfaceScale();
        panelWidth = Math.round(BASE_PANEL_WIDTH * scale);
        headerHeight = Math.max(13, Math.round(BASE_HEADER_HEIGHT * scale));
        rowHeight = Math.max(12, Math.round(BASE_ROW_HEIGHT * scale));
        settingRowHeight = Math.max(11, Math.round(BASE_SETTING_ROW_HEIGHT * scale));
        editRowHeight = Math.max(15, Math.round(BASE_EDIT_ROW_HEIGHT * scale));
        searchWidth = Math.round(BASE_SEARCH_WIDTH * scale);
        searchRowHeight = Math.max(16, Math.round(BASE_SEARCH_ROW_HEIGHT * scale));
        resultRowHeight = Math.max(11, Math.round(BASE_RESULT_ROW_HEIGHT * scale));
        sliderTrackWidth = Math.round(BASE_SLIDER_TRACK_WIDTH * scale);

        rebuildPanels();

        PanelView search = searchPanel();
        searchField = new EditBox(font, 0, 0, searchFieldWidth(), 12,
                Component.translatable("screen.helikon.search_hint"));
        searchField.setBordered(false);
        searchField.setMaxLength(64);
        searchField.setHint(ui(Component.translatable("screen.helikon.search_hint")));
        searchField.setValue(searchQuery);
        searchField.setTextShadow(false);
        searchField.addFormatter((text, index) -> FormattedCharSequence.forward(text, UI_STYLE));
        searchField.setResponder(text -> {
            searchQuery = text;
            panelScrolls.put(SEARCH_PANEL_KEY, 0.0D);
            clampPanel(search);
            positionSearchField(search);
        });
        addRenderableWidget(searchField);
        positionSearchField(search);

        rebuildSettingWidgets();
    }

    /**
     * Starts with no text field focused so the search bar does not swallow the
     * GUI toggle key; clicking or tabbing to an editor still focuses it.
     */
    @Override
    protected void setInitialFocus() {
        clearFocus();
    }

    /** Rebuilds the panel list, preferring saved placements over the default cascade. */
    private void rebuildPanels() {
        Map<String, PanelView> existing = new HashMap<>();
        for (PanelView panel : panels) {
            existing.put(panel.key, panel);
        }
        panels.clear();

        ModuleCategory[] categories = ModuleCategory.values();
        int fitColumns = Math.max(1, (width - 2 * SCREEN_EDGE_MARGIN + PANEL_MARGIN) / (panelWidth + PANEL_MARGIN));
        for (int index = 0; index < categories.length + 2; index++) {
            boolean favorites = index == categories.length;
            boolean active = index == categories.length + 1;
            ModuleCategory category = favorites || active ? null : categories[index];
            String key = favorites ? FAVORITES_PANEL_KEY
                    : active ? ACTIVE_PANEL_KEY : category.name().toLowerCase(Locale.ROOT);
            int column = index % fitColumns;
            int cascadeRow = index / fitColumns;
            int defaultX = SCREEN_EDGE_MARGIN + column * (panelWidth + PANEL_MARGIN) + cascadeRow * 12;
            int defaultY = SCREEN_EDGE_MARGIN + cascadeRow * (headerHeight + 6);

            PanelView panel = existing.getOrDefault(key, new PanelView(key, category));
            Optional<ClickGuiWindowState.PanelPlacement> saved = windowState.panelPlacement(key);
            if (existing.containsKey(key)) {
                // keep live position through resizes
            } else if (saved.isPresent()) {
                panel.x = saved.orElseThrow().x();
                panel.y = saved.orElseThrow().y();
                panel.collapsed = saved.orElseThrow().collapsed();
            } else {
                panel.x = defaultX;
                panel.y = defaultY;
            }
            clampPanel(panel);
            panels.add(panel);
        }

        PanelView search = existing.get(SEARCH_PANEL_KEY);
        if (search == null) {
            search = new PanelView(SEARCH_PANEL_KEY, null);
            Optional<ClickGuiWindowState.PanelPlacement> saved = windowState.panelPlacement(SEARCH_PANEL_KEY);
            if (saved.isPresent()) {
                search.x = saved.orElseThrow().x();
                search.y = saved.orElseThrow().y();
            } else {
                search.x = (width - searchWidth) / 2;
                search.y = height - searchRowHeight - 24;
            }
        }
        clampPanel(search);
        panels.add(search);
    }

    private PanelView searchPanel() {
        for (PanelView panel : panels) {
            if (panel.isSearch()) {
                return panel;
            }
        }
        throw new IllegalStateException("Search panel missing");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        for (PanelView panel : panels) {
            windowState.setPanelPlacement(panel.key, Math.max(0, panel.x), Math.max(0, panel.y), panel.collapsed);
        }
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
        for (PanelView panel : panels) {
            if (panel.isSearch()) {
                drawSearchPanel(graphics, panel, mouseX, mouseY);
            } else {
                drawCategoryPanel(graphics, panel, mouseX, mouseY);
            }
        }
        syncSettingWidgetPositions();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void refreshThemeColors() {
        ClickGuiTheme theme = windowState.theme();
        int accent = dev.helikon.client.module.render.RainbowUiAccess.accent(
                System.currentTimeMillis(), theme.accent());
        COLOR_PANEL = theme.panel();
        COLOR_HEADER = withAlpha(mix(theme.panel() | 0xFF000000, 0xFFFFFFFF, 0.06D), 0xF8);
        COLOR_ROW_HOVER = theme.rowHover();
        COLOR_ACCENT = accent;
        COLOR_ACCENT_LIGHT = mix(accent, 0xFFFFFFFF, 0.42D);
        COLOR_TEXT = theme.text();
        COLOR_TEXT_DIM = theme.textDim();
        COLOR_SETTING_LABEL = mix(accent, theme.textDim(), 0.50D);
        COLOR_OUTLINE = theme.outline();
        COLOR_INVALID = theme.invalid();
        COLOR_SLIDER_TRACK = mix(theme.outline(), 0xFF000000, 0.25D);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void drawCategoryPanel(GuiGraphicsExtractor graphics, PanelView panel, int mouseX, int mouseY) {
        int right = panel.x + panelWidth;
        int contentTop = panel.y + headerHeight;
        List<Row> rows = panelRows(panel);
        int contentHeight = contentHeight(rows);
        int visibleHeight = panel.collapsed ? 0 : visibleContentHeight(panel, contentHeight);
        double scroll = panelScroll(panel, contentHeight, visibleHeight);
        int bottom = contentTop + visibleHeight;

        fillRounded(graphics, panel.x + 2, panel.y + 2, right + 2, bottom + 2, COLOR_PANEL_SHADOW);
        drawHeader(graphics, panel, panel.collapsed || visibleHeight <= 0);
        if (panel.collapsed || visibleHeight <= 0) {
            return;
        }

        graphics.fill(panel.x, contentTop, right, bottom - 1, COLOR_PANEL);
        graphics.fill(panel.x + 1, bottom - 1, right - 1, bottom, COLOR_PANEL);

        boolean scrollable = contentHeight > visibleHeight;
        if (scrollable) {
            graphics.enableScissor(panel.x, contentTop, right, bottom);
        }
        Module hoveredModule = null;
        Setting<?> hoveredSetting = null;
        boolean hintHovered = false;
        for (Row row : rows) {
            int rowY = contentTop + row.y() - (int) scroll;
            if (rowY + row.height() <= contentTop || rowY >= bottom) {
                continue;
            }
            boolean hovered = isInside(mouseX, mouseY, panel.x, rowY, panelWidth, row.height())
                    && mouseY >= contentTop && mouseY < bottom;
            if (row.kind() == RowKind.MODULE) {
                if (row.y() > 0) {
                    graphics.fill(panel.x, rowY, right, rowY + 1, COLOR_ROW_DIVIDER);
                }
                drawModuleRow(graphics, panel, row, rowY, hovered);
                if (hovered) {
                    hoveredModule = row.module();
                }
            } else {
                drawSettingRow(graphics, panel, row, rowY, hovered, mouseX);
                if (hovered && row.setting() != null) {
                    hoveredSetting = row.setting();
                } else if (hovered && row.kind() == RowKind.HINT) {
                    hintHovered = true;
                }
            }
        }
        if (scrollable) {
            graphics.disableScissor();
            ClickGuiScrollbarState.thumb(contentTop, bottom, contentHeight, scroll).ifPresent(thumb ->
                    graphics.fill(right - 2, thumb.y(), right - 1, thumb.y() + thumb.height(),
                            withAlpha(COLOR_ACCENT, 0xB0)));
        }

        if (hintHovered) {
            graphics.setTooltipForNextFrame(font,
                    ui(panel.isActive() ? "Enabled modules appear here"
                            : "Middle-click a module to star it as a favorite"), mouseX, mouseY);
        } else if (hoveredSetting != null && !hoveredSetting.description().isBlank()) {
            String extra = isSlider(hoveredSetting)
                    ? " (" + NumberSettingText.format(valueOf(hoveredSetting)) + ")" : "";
            graphics.setTooltipForNextFrame(font,
                    ui(hoveredSetting.description() + extra), mouseX, mouseY);
        } else if (hoveredModule != null && !hoveredModule.description().isBlank()) {
            graphics.setTooltipForNextFrame(font, ui(hoveredModule.description()), mouseX, mouseY);
        }
    }

    private void drawHeader(GuiGraphicsExtractor graphics, PanelView panel, boolean roundBottom) {
        int panelViewWidth = panelWidthFor(panel);
        int right = panel.x + panelViewWidth;
        int bottom = panel.y + headerHeight;
        graphics.fill(panel.x + 1, panel.y, right - 1, panel.y + 1, COLOR_HEADER);
        graphics.fill(panel.x, panel.y + 1, right, bottom - 1, COLOR_HEADER);
        graphics.fill(panel.x + (roundBottom ? 1 : 0), bottom - 1, right - (roundBottom ? 1 : 0), bottom,
                COLOR_ACCENT);
        int textY = panel.y + (headerHeight - 8) / 2;
        graphics.text(font, panel.icon(), panel.x + 6, textY, COLOR_ACCENT, false);
        graphics.text(font, uiTrim(panel.title(), panelViewWidth - 24), panel.x + 17, textY, COLOR_TEXT, false);
    }

    private void drawModuleRow(GuiGraphicsExtractor graphics, PanelView panel, Row row, int rowY, boolean hovered) {
        Module module = row.module();
        int panelViewWidth = panelWidthFor(panel);
        if (hovered) {
            graphics.fill(panel.x, rowY, panel.x + panelViewWidth, rowY + row.height(), COLOR_ROW_HOVER);
        }
        int textY = rowY + (row.height() - 8) / 2;
        boolean favorite = windowState.isFavorite(module.id());
        int nameLimit = panelViewWidth - 14 - (favorite ? 10 : 0);
        graphics.text(font, uiTrim(module.name(), nameLimit), panel.x + 7, textY,
                module.isEnabled() ? COLOR_ACCENT : COLOR_TEXT, false);
        if (favorite) {
            graphics.text(font, "★", panel.x + panelViewWidth - 12, textY, COLOR_ACCENT, false);
        }
    }

    private void drawSettingRow(GuiGraphicsExtractor graphics, PanelView panel, Row row, int rowY,
                                boolean hovered, int mouseX) {
        int panelViewWidth = panelWidthFor(panel);
        int right = panel.x + panelViewWidth;
        graphics.fill(panel.x, rowY, right, rowY + row.height(), COLOR_SETTINGS_INSET);
        if (hovered && row.kind() != RowKind.SETTING_TEXT) {
            graphics.fill(panel.x, rowY, right, rowY + row.height(), COLOR_ROW_HOVER);
        }
        int labelX = panel.x + 13;
        int textY = rowY + (settingRowHeight - 8) / 2;
        Setting<?> setting = row.setting();

        switch (row.kind()) {
            case SETTING_ACTION -> graphics.centeredText(font, ui(setting.name()),
                    panel.x + panelViewWidth / 2, textY,
                    hovered ? COLOR_ACCENT_LIGHT : COLOR_ACCENT);
            case SETTING_BOOL -> {
                BooleanSetting booleanSetting = (BooleanSetting) setting;
                drawSettingLabel(graphics, setting, labelX, textY, right - 24 - labelX);
                drawToggle(graphics, right - 8 - miniToggleWidth(), rowY + (settingRowHeight - miniToggleHeight()) / 2,
                        miniToggleWidth(), miniToggleHeight(), booleanSetting.value());
            }
            case SETTING_SLIDER -> {
                int trackX = sliderTrackX(panel);
                drawSettingLabel(graphics, setting, labelX, textY, trackX - 6 - labelX);
                int trackY = rowY + settingRowHeight / 2 - 1;
                fillRounded(graphics, trackX, trackY, trackX + sliderTrackWidth, trackY + 3, COLOR_SLIDER_TRACK);
                int handleX = NumberSlider.handleX(valueOf(setting), minOf(setting), maxOf(setting),
                        trackX, sliderTrackWidth);
                if (handleX > trackX) {
                    fillRounded(graphics, trackX, trackY, handleX, trackY + 3, COLOR_ACCENT);
                }
            }
            case SETTING_COLOR -> {
                ColorSetting colorSetting = (ColorSetting) setting;
                int swatchX = right - 8 - 18;
                drawSettingLabel(graphics, setting, labelX, textY, swatchX - 6 - labelX);
                fillRounded(graphics, swatchX, rowY + 2, swatchX + 18, rowY + settingRowHeight - 2,
                        0xFF000000 | colorSetting.value());
                graphics.outline(swatchX, rowY + 2, 18, settingRowHeight - 4, COLOR_OUTLINE);
                if (expandedColorPickers.contains(setting)) {
                    drawColorPicker(graphics, panel, colorSetting, rowY + settingRowHeight);
                }
            }
            case SETTING_ENUM -> {
                EnumSetting<?> enumSetting = (EnumSetting<?>) setting;
                Component value = uiTrim(enumSetting.valueId(), (panelViewWidth - 20) / 2);
                int valueWidth = font.width(value);
                drawSettingLabel(graphics, setting, labelX, textY, right - 12 - valueWidth - labelX);
                graphics.text(font, value, right - 8 - valueWidth, textY,
                        hovered ? COLOR_ACCENT_LIGHT : COLOR_TEXT_DIM, false);
            }
            case SETTING_TEXT -> {
                drawSettingLabel(graphics, setting, labelX, textY, right - 12 - labelX);
                int editY = rowY + settingRowHeight;
                fillRounded(graphics, labelX, editY + 1, right - 8, editY + editRowHeight - 2, COLOR_EDIT_BACKGROUND);
            }
            case SETTING_BIND -> {
                String bindText = keybindAssignment.isAssigning(row.module())
                        ? keybindStatus.isBlank() ? "Press a key..." : keybindStatus
                        : "Bind: " + keyDisplayName(row.module().keybind()) + conflictWarning(row.module());
                graphics.text(font, uiTrim(bindText, panelViewWidth - 20), labelX, textY,
                        keybindAssignment.isAssigning(row.module()) ? COLOR_ACCENT
                                : hovered ? COLOR_TEXT : COLOR_TEXT_DIM, false);
            }
            case SETTING_RESET -> graphics.centeredText(font,
                    ui(Component.translatable("screen.helikon.reset_module")),
                    panel.x + panelViewWidth / 2, textY,
                    hovered ? COLOR_ACCENT : COLOR_TEXT_DIM);
            case SETTING_LABEL -> drawSettingLabel(graphics, setting, labelX, textY, right - 12 - labelX);
            case HINT -> graphics.text(font, uiTrim(panel.isActive() ? "No active modules" : "No favorites",
                            panelViewWidth - 14),
                    panel.x + 7, rowY + (row.height() - 8) / 2, COLOR_TEXT_DIM, false);
            case MODULE -> throw new IllegalStateException("Module rows render separately");
        }
    }

    private void drawSettingLabel(GuiGraphicsExtractor graphics, Setting<?> setting, int x, int y, int maxWidth) {
        graphics.text(font, uiTrim(setting.name(), maxWidth), x, y, COLOR_SETTING_LABEL, false);
    }

    private void drawColorPicker(GuiGraphicsExtractor graphics, PanelView panel, ColorSetting setting, int top) {
        int x = pickerX(panel);
        int width = pickerWidth(panel);
        for (int channel = 0; channel < ColorPickerValue.CHANNEL_COUNT; channel++) {
            int y = top + 2 + channel * (COLOR_PICKER_CHANNEL_HEIGHT + 1);
            for (int segment = 0; segment < 32; segment++) {
                int start = x + segment * width / 32;
                int end = x + (segment + 1) * width / 32;
                int value = segment * 255 / 31;
                graphics.fill(start, y, end, y + COLOR_PICKER_CHANNEL_HEIGHT,
                        pickerColor(setting.value(), channel, value));
            }
            int markerX = x + (int) Math.round(ColorPickerValue.channel(setting.value(), channel)
                    * (width - 1) / 255.0D);
            graphics.outline(markerX - 1, y - 1, 3, COLOR_PICKER_CHANNEL_HEIGHT + 2, COLOR_TEXT);
        }
    }

    private static int pickerColor(int color, int channel, int value) {
        if (channel == 0) {
            return 0xFF000000 | value << 16 | value << 8 | value;
        }
        return ColorPickerValue.withChannel(color, channel, value) | 0xFF000000;
    }

    private void drawToggle(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean on) {
        fillRounded(graphics, x, y, x + width, y + height, on ? COLOR_ACCENT : COLOR_TOGGLE_OFF_TRACK);
        int knobSize = height - 2;
        int knobX = on ? x + width - 1 - knobSize : x + 1;
        fillRounded(graphics, knobX, y + 1, knobX + knobSize, y + 1 + knobSize,
                on ? COLOR_TOGGLE_ON_KNOB : COLOR_TOGGLE_OFF_KNOB);
    }

    private void drawSearchPanel(GuiGraphicsExtractor graphics, PanelView panel, int mouseX, int mouseY) {
        int right = panel.x + searchWidth;
        int bottom = panel.y + searchPanelHeight();
        fillRounded(graphics, panel.x + 2, panel.y + 2, right + 2, bottom + 2, COLOR_PANEL_SHADOW);
        fillRounded(graphics, panel.x, panel.y, right, panel.y + searchRowHeight, COLOR_HEADER);
        if (bottom > panel.y + searchRowHeight) {
            fillRounded(graphics, panel.x, panel.y + searchRowHeight, right, bottom, COLOR_PANEL);
        }
        graphics.fill(panel.x + 1, panel.y + searchRowHeight - 1, right - 1, panel.y + searchRowHeight,
                COLOR_ACCENT);

        int iconX = panel.x + 8;
        int iconY = panel.y + (searchRowHeight - 7) / 2;
        graphics.outline(iconX, iconY, 5, 5, COLOR_SETTING_LABEL);
        graphics.fill(iconX + 4, iconY + 4, iconX + 6, iconY + 6, COLOR_SETTING_LABEL);
        if (isInside(mouseX, mouseY, panel.x, panel.y, 18, searchRowHeight)) {
            graphics.setTooltipForNextFrame(font, ui("Drag search"), mouseX, mouseY);
        }

        int classicX = classicLinkX(panel);
        int hudX = hudLinkX(panel);
        int themeX = themeLinkX(panel);
        int linkY = panel.y + (searchRowHeight - 8) / 2;
        graphics.text(font, ui(Component.translatable("screen.helikon.layout_classic")), classicX, linkY,
                isInside(mouseX, mouseY, classicX - 2, panel.y, uiWidth(classicLabel()) + 4, searchRowHeight)
                        ? COLOR_ACCENT : COLOR_TEXT_DIM, false);
        graphics.text(font, ui(Component.translatable("screen.helikon.hud_button")), hudX, linkY,
                isInside(mouseX, mouseY, hudX - 2, panel.y, uiWidth(hudLabel()) + 4, searchRowHeight)
                        ? COLOR_ACCENT : COLOR_TEXT_DIM, false);
        graphics.text(font, ui(Component.translatable("screen.helikon.theme_button")), themeX, linkY,
                isInside(mouseX, mouseY, themeX - 2, panel.y, uiWidth(themeLabel()) + 4, searchRowHeight)
                        ? COLOR_ACCENT : COLOR_TEXT_DIM, false);

        List<Row> rows = searchRows();
        int contentTop = panel.y + searchRowHeight + 1;
        int contentHeight = contentHeight(rows);
        int visibleHeight = visibleSearchResultsHeight(rows);
        double scroll = searchResultScroll(contentHeight, visibleHeight);
        if (rows.isEmpty()) {
            int textY = contentTop + (resultRowHeight - 8) / 2;
            graphics.text(font, ui(Component.translatable("screen.helikon.no_results")),
                    panel.x + 8, textY, COLOR_TEXT_DIM, false);
        } else {
            int contentBottom = contentTop + visibleHeight;
            boolean scrollable = contentHeight > visibleHeight;
            if (scrollable) {
                graphics.enableScissor(panel.x, contentTop, right, contentBottom);
            }
            for (Row row : rows) {
                int rowY = contentTop + row.y() - (int) scroll;
                if (rowY + row.height() <= contentTop || rowY >= contentBottom) {
                    continue;
                }
                boolean hovered = isInside(mouseX, mouseY, panel.x, rowY, searchWidth, row.height())
                        && mouseY >= contentTop && mouseY < contentBottom;
                if (row.kind() == RowKind.MODULE) {
                    drawSearchModuleRow(graphics, panel, row, rowY, hovered);
                } else {
                    drawSettingRow(graphics, panel, row, rowY, hovered, mouseX);
                }
            }
            if (scrollable) {
                graphics.disableScissor();
            }
        }
        if (contentHeight > visibleHeight) {
            graphics.fill(right - 3, contentTop, right - 2, contentTop + visibleHeight, COLOR_OUTLINE);
            ClickGuiScrollbarState.thumb(contentTop, contentTop + visibleHeight, contentHeight, scroll)
                    .ifPresent(thumb -> graphics.fill(right - 4, thumb.y(), right - 1,
                            thumb.y() + thumb.height(), COLOR_ACCENT));
        }
    }

    private void drawSearchModuleRow(
            GuiGraphicsExtractor graphics, PanelView panel, Row row, int rowY, boolean hovered
    ) {
        Module module = row.module();
        int right = panel.x + searchWidth;
        if (hovered) {
            graphics.fill(panel.x + 2, rowY, right - 2, rowY + row.height(), COLOR_ROW_HOVER);
        }
        int textY = rowY + (row.height() - 8) / 2;
        Component category = ui(module.category().displayName());
        int categoryWidth = font.width(category);
        boolean favorite = windowState.isFavorite(module.id());
        int starWidth = favorite ? 10 : 0;
        graphics.text(font, uiTrim(module.name(), searchWidth - 22 - categoryWidth - starWidth),
                panel.x + 8, textY,
                module.isEnabled() ? COLOR_ACCENT : hovered ? COLOR_TEXT : COLOR_TEXT_DIM, false);
        if (favorite) {
            graphics.text(font, "★", right - 10 - categoryWidth - 8, textY, COLOR_ACCENT, false);
        }
        graphics.text(font, category, right - 8 - categoryWidth, textY, COLOR_SETTING_LABEL, false);
    }

    // ------------------------------------------------------------------
    // Row layout
    // ------------------------------------------------------------------

    /** The modules a panel lists: its category roster, favorites, or currently enabled modules. */
    private List<Module> panelModules(PanelView panel) {
        if (panel.isFavorites()) {
            return modules.all().stream().filter(module -> windowState.isFavorite(module.id())).toList();
        }
        if (panel.isActive()) {
            return modules.all().stream().filter(Module::isEnabled).toList();
        }
        return modules.all().stream().filter(module -> module.category() == panel.category).toList();
    }

    /** Rows for one panel with y offsets relative to the content top, before scrolling. */
    private List<Row> panelRows(PanelView panel) {
        return rowsForModules(panelModules(panel), rowHeight, panel.isFavorites() || panel.isActive());
    }

    /** Search rows use the same expansion model while retaining the search palette's compact module height. */
    private List<Row> searchRows() {
        return rowsForModules(searchResults(), resultRowHeight, false);
    }

    private List<Row> rowsForModules(List<Module> listedModules, int moduleRowHeight, boolean showEmptyHint) {
        List<Row> rows = new ArrayList<>();
        int y = 0;
        if (showEmptyHint && listedModules.isEmpty()) {
            rows.add(new Row(null, null, RowKind.HINT, 0, moduleRowHeight));
            return rows;
        }
        for (Module module : listedModules) {
            rows.add(new Row(module, null, RowKind.MODULE, y, moduleRowHeight));
            y += moduleRowHeight;
            if (!windowState.isModuleExpanded(module.id())) {
                continue;
            }
            for (Setting<?> setting : module.settings()) {
                if (!setting.isVisible()) {
                    continue;
                }
                RowKind kind = kindOf(setting);
                int height = switch (kind) {
                    case SETTING_TEXT -> settingRowHeight + editRowHeight;
                    case SETTING_COLOR -> settingRowHeight
                            + (expandedColorPickers.contains(setting) ? COLOR_PICKER_EXTRA : 0);
                    default -> settingRowHeight;
                };
                rows.add(new Row(module, setting, kind, y, height));
                y += height;
            }
            rows.add(new Row(module, null, RowKind.SETTING_BIND, y, settingRowHeight));
            y += settingRowHeight;
            rows.add(new Row(module, null, RowKind.SETTING_RESET, y, settingRowHeight));
            y += settingRowHeight;
        }
        return rows;
    }

    private static RowKind kindOf(Setting<?> setting) {
        if (setting instanceof ActionSetting) {
            return RowKind.SETTING_ACTION;
        }
        if (setting instanceof BooleanSetting) {
            return RowKind.SETTING_BOOL;
        }
        if (setting instanceof ColorSetting) {
            return RowKind.SETTING_COLOR;
        }
        if (isSlider(setting)) {
            return RowKind.SETTING_SLIDER;
        }
        if (setting instanceof EnumSetting<?>) {
            return RowKind.SETTING_ENUM;
        }
        if (SettingText.isEditable(setting)) {
            return RowKind.SETTING_TEXT;
        }
        return RowKind.SETTING_LABEL;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        KeybindAssignment.Result captureResult = keybindAssignment.acceptMouseButton(event.button(), event.modifiers());
        if (captureResult != KeybindAssignment.Result.IGNORED) {
            keybindStatus = keybindStatus(captureResult);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        clearFocus();

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        for (int index = panels.size() - 1; index >= 0; index--) {
            PanelView panel = panels.get(index);
            if (!isOverPanel(panel, mouseX, mouseY)) {
                continue;
            }
            panels.remove(index);
            panels.add(panel);
            if (panel.isSearch()) {
                return handleSearchPanelClick(panel, mouseX, mouseY, event.button());
            }
            return handleCategoryPanelClick(panel, mouseX, mouseY, event.button());
        }
        return false;
    }

    private boolean handleCategoryPanelClick(PanelView panel, int mouseX, int mouseY, int button) {
        if (mouseY < panel.y + headerHeight) {
            if (button == 1) {
                panel.collapsed = !panel.collapsed;
                windowState.setPanelPlacement(panel.key, Math.max(0, panel.x), Math.max(0, panel.y), panel.collapsed);
                rebuildSettingWidgets();
                return true;
            }
            if (button == 0) {
                draggedPanel = panel;
                dragOffsetX = mouseX - panel.x;
                dragOffsetY = mouseY - panel.y;
            }
            return true;
        }

        int contentTop = panel.y + headerHeight;
        List<Row> rows = panelRows(panel);
        int contentHeight = contentHeight(rows);
        int visibleHeight = visibleContentHeight(panel, contentHeight);
        if (mouseY >= contentTop + visibleHeight) {
            return true;
        }
        double scroll = panelScroll(panel, contentHeight, visibleHeight);
        if (button == 0 && beginScrollbarDrag(panel, mouseX, mouseY, contentTop,
                visibleHeight, contentHeight, scroll)) {
            return true;
        }
        for (Row row : rows) {
            int rowY = contentTop + row.y() - (int) scroll;
            if (!isInside(mouseX, mouseY, panel.x, rowY, panelWidth, row.height())) {
                continue;
            }
            if (row.kind() == RowKind.MODULE) {
                handleModuleRowClick(row.module(), button);
            } else {
                handleSettingRowClick(panel, row, rowY, mouseX, mouseY, button);
            }
            return true;
        }
        return true;
    }

    private void handleModuleRowClick(Module module, int button) {
        if (button == 0) {
            modules.toggle(module);
            // Toggling can add or remove the module from the Active panel.
            rebuildSettingWidgets();
        } else if (button == 1) {
            boolean expanded = !windowState.isModuleExpanded(module.id());
            windowState.setModuleExpanded(module.id(), expanded);
            if (!expanded && keybindAssignment.isAssigning(module)) {
                keybindAssignment.cancel();
                keyCaptureSuppression.clear();
            }
            rebuildSettingWidgets();
        } else if (button == 2) {
            windowState.setFavorite(module.id(), !windowState.isFavorite(module.id()));
            // The Favorites panel gains or loses this module's rows, so its
            // expanded text-setting widgets must be recreated to match.
            rebuildSettingWidgets();
        }
    }

    private void handleSettingRowClick(PanelView panel, Row row, int rowY, int mouseX, int mouseY, int button) {
        Setting<?> setting = row.setting();
        if (button == 1) {
            if (setting != null) {
                setting.reset();
                refreshSettingField(setting);
            }
            return;
        }
        if (button != 0) {
            return;
        }
        switch (row.kind()) {
            case SETTING_ACTION -> ((ActionSetting) setting).run();
            case SETTING_BOOL -> {
                BooleanSetting booleanSetting = (BooleanSetting) setting;
                booleanSetting.set(!booleanSetting.value());
            }
            case SETTING_SLIDER -> {
                int trackX = sliderTrackX(panel);
                if (mouseX >= trackX - 4 && mouseX < trackX + sliderTrackWidth + 4) {
                    activeSlider = new SliderDrag(setting, trackX, sliderTrackWidth);
                    applySliderValue(setting, NumberSlider.valueAt(mouseX, trackX, sliderTrackWidth,
                            minOf(setting), maxOf(setting), integral(setting)));
                }
            }
            case SETTING_COLOR -> {
                if (mouseY < rowY + settingRowHeight) {
                    if (expandedColorPickers.contains(setting)) {
                        expandedColorPickers.remove(setting);
                    } else {
                        expandedColorPickers.add(setting);
                    }
                    rebuildSettingWidgets();
                } else {
                    beginColorPickerDrag(panel, (ColorSetting) setting, rowY + settingRowHeight, mouseX, mouseY);
                }
            }
            case SETTING_ENUM -> ((EnumSetting<?>) setting).cycle();
            case SETTING_BIND -> {
                keybindAssignment.begin(row.module());
                keybindStatus = "";
                keyCaptureSuppression.clear();
            }
            case SETTING_RESET -> {
                row.module().resetSettings();
                expandedColorPickers.clear();
                rebuildSettingWidgets();
            }
            default -> {
            }
        }
    }

    private void beginColorPickerDrag(PanelView panel, ColorSetting setting, int pickerTop, int mouseX, int mouseY) {
        int relativeY = mouseY - pickerTop - 2;
        int channel = relativeY / (COLOR_PICKER_CHANNEL_HEIGHT + 1);
        if (relativeY < 0 || channel >= ColorPickerValue.CHANNEL_COUNT) {
            return;
        }
        activeColorPicker = new ColorDrag(setting, channel, pickerX(panel), pickerWidth(panel));
        applyColorPickerDrag(mouseX);
    }

    private boolean handleSearchPanelClick(PanelView panel, int mouseX, int mouseY, int button) {
        if (button == 0 && isInside(mouseX, mouseY, classicLinkX(panel) - 2, panel.y, uiWidth(classicLabel()) + 4,
                searchRowHeight)) {
            windowState.setClassicLayout(true);
            minecraft.setScreenAndShow(new HelikonClassicClickGuiScreen(
                    modules, configuration, windowState, hudLayout, hudConfiguration));
            return true;
        }
        if (button == 0 && isInside(mouseX, mouseY, hudLinkX(panel) - 2, panel.y,
                uiWidth(hudLabel()) + 4, searchRowHeight)) {
            minecraft.setScreenAndShow(new HelikonHudEditorScreen(this, modules, hudLayout, hudConfiguration));
            return true;
        }
        if (button == 0 && isInside(mouseX, mouseY, themeLinkX(panel) - 2, panel.y,
                uiWidth(themeLabel()) + 4, searchRowHeight)) {
            minecraft.setScreenAndShow(new HelikonThemeEditorScreen(this, modules, configuration, windowState));
            return true;
        }
        if (mouseY >= panel.y + searchRowHeight) {
            List<Row> rows = searchRows();
            int contentTop = panel.y + searchRowHeight + 1;
            int contentHeight = contentHeight(rows);
            int visibleHeight = visibleSearchResultsHeight(rows);
            if (mouseY >= contentTop && mouseY < contentTop + visibleHeight && !rows.isEmpty()) {
                double scroll = searchResultScroll(contentHeight, visibleHeight);
                if (button == 0 && beginScrollbarDrag(panel, mouseX, mouseY, contentTop,
                        visibleHeight, contentHeight, scroll)) {
                    return true;
                }
                for (Row row : rows) {
                    int rowY = contentTop + row.y() - (int) scroll;
                    if (!isInside(mouseX, mouseY, panel.x, rowY, searchWidth, row.height())) {
                        continue;
                    }
                    if (row.kind() == RowKind.MODULE) {
                        handleModuleRowClick(row.module(), button);
                    } else {
                        handleSettingRowClick(panel, row, rowY, mouseX, mouseY, button);
                    }
                    return true;
                }
            }
            return true;
        }
        if (button == 0) {
            draggedPanel = panel;
            dragOffsetX = mouseX - panel.x;
            dragOffsetY = mouseY - panel.y;
        }
        return true;
    }

    private boolean beginScrollbarDrag(
            PanelView panel, int mouseX, int mouseY, int trackTop, int visibleHeight,
            int contentHeight, double scroll
    ) {
        int right = panel.x + panelWidthFor(panel);
        if (contentHeight <= visibleHeight
                || !isInside(mouseX, mouseY, right - 6, trackTop, 6, visibleHeight)) {
            return false;
        }
        activeScrollbar.beginDrag(mouseY, trackTop, trackTop + visibleHeight, contentHeight, scroll)
                .ifPresent(value -> panelScrolls.put(panel.key, value));
        scrollbarPanel = activeScrollbar.isDragging() ? panel : null;
        syncSettingWidgetPositions();
        return scrollbarPanel != null;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() != 0) {
            return super.mouseDragged(event, dragX, dragY);
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (activeScrollbar.isDragging() && scrollbarPanel != null) {
            List<Row> rows = rowsFor(scrollbarPanel);
            int contentHeight = contentHeight(rows);
            int trackTop = contentTop(scrollbarPanel);
            int visibleHeight = visibleHeight(scrollbarPanel, rows);
            activeScrollbar.dragTo(mouseY, trackTop, trackTop + visibleHeight, contentHeight)
                    .ifPresent(value -> panelScrolls.put(scrollbarPanel.key, value));
            syncSettingWidgetPositions();
            return true;
        }
        if (activeColorPicker != null) {
            applyColorPickerDrag(mouseX);
            return true;
        }
        if (activeSlider != null) {
            applySliderValue(activeSlider.setting(), NumberSlider.valueAt(mouseX, activeSlider.trackX(),
                    activeSlider.trackWidth(), minOf(activeSlider.setting()), maxOf(activeSlider.setting()),
                    integral(activeSlider.setting())));
            return true;
        }
        if (draggedPanel != null) {
            draggedPanel.x = mouseX - dragOffsetX;
            draggedPanel.y = mouseY - dragOffsetY;
            clampPanel(draggedPanel);
            if (draggedPanel.isSearch()) {
                positionSearchField(draggedPanel);
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && (draggedPanel != null || activeSlider != null
                || activeColorPicker != null || activeScrollbar.isDragging())) {
            if (draggedPanel != null) {
                windowState.setPanelPlacement(draggedPanel.key, Math.max(0, draggedPanel.x),
                        Math.max(0, draggedPanel.y), draggedPanel.collapsed);
            }
            draggedPanel = null;
            activeSlider = null;
            activeColorPicker = null;
            activeScrollbar.endDrag();
            scrollbarPanel = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            return true;
        }
        for (int index = panels.size() - 1; index >= 0; index--) {
            PanelView panel = panels.get(index);
            if (!isOverPanel(panel, (int) mouseX, (int) mouseY)) {
                continue;
            }
            if (panel.isSearch()) {
                List<Row> rows = searchRows();
                int visibleHeight = visibleSearchResultsHeight(rows);
                int contentHeight = contentHeight(rows);
                if (contentHeight > visibleHeight) {
                    double scroll = searchResultScroll(contentHeight, visibleHeight);
                    panelScrolls.put(SEARCH_PANEL_KEY, Math.clamp(
                            scroll - vertical * resultRowHeight, 0.0D, contentHeight - visibleHeight));
                }
                return true;
            }
            if (nudgeHoveredSlider(panel, (int) mouseX, (int) mouseY, vertical)) {
                return true;
            }
            List<Row> rows = panelRows(panel);
            int contentHeight = contentHeight(rows);
            int visibleHeight = visibleContentHeight(panel, contentHeight);
            if (visibleHeight > 0 && contentHeight > visibleHeight) {
                double scroll = panelScroll(panel, contentHeight, visibleHeight);
                panelScrolls.put(panel.key,
                        Math.clamp(scroll - vertical * rowHeight, 0.0D, contentHeight - visibleHeight));
                return true;
            }
            return true;
        }
        return false;
    }

    /** Increases or decreases a hovered slider by one step from the mouse wheel. */
    private boolean nudgeHoveredSlider(PanelView panel, int mouseX, int mouseY, double vertical) {
        if (vertical == 0.0D) {
            return false;
        }
        int contentTop = panel.y + headerHeight;
        List<Row> rows = panelRows(panel);
        int contentHeight = contentHeight(rows);
        int visibleHeight = visibleContentHeight(panel, contentHeight);
        double scroll = panelScroll(panel, contentHeight, visibleHeight);
        for (Row row : rows) {
            if (row.kind() != RowKind.SETTING_SLIDER) {
                continue;
            }
            int rowY = contentTop + row.y() - (int) scroll;
            int trackX = sliderTrackX(panel);
            if (isInside(mouseX, mouseY, trackX - 4, rowY, sliderTrackWidth + 8, row.height())) {
                Setting<?> setting = row.setting();
                applySliderValue(setting, NumberSlider.nudge(valueOf(setting), minOf(setting), maxOf(setting),
                        integral(setting), vertical > 0.0D ? 1 : -1));
                return true;
            }
        }
        return false;
    }

    private void applyColorPickerDrag(int mouseX) {
        ColorDrag drag = activeColorPicker;
        if (drag == null) {
            return;
        }
        int value = ColorPickerValue.channelAt(mouseX, drag.x(), drag.width());
        drag.setting().set(ColorPickerValue.withChannel(drag.setting().value(), drag.channel(), value));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (keyCaptureSuppression.consumesKeyPress(event.key())) {
            return true;
        }
        KeybindAssignment.Result result = keybindAssignment.acceptKey(event.key(), event.modifiers());
        if (result != KeybindAssignment.Result.IGNORED) {
            if (isTerminalCaptureResult(result)) {
                keyCaptureSuppression.begin(event.key());
            }
            keybindStatus = keybindStatus(result);
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

    // ------------------------------------------------------------------
    // Widgets
    // ------------------------------------------------------------------

    /** Removes and recreates the text-setting edit boxes for every expanded module. */
    private void rebuildSettingWidgets() {
        textFields.values().forEach(this::removeWidget);
        textFields.clear();
        for (PanelView panel : panels) {
            if (!panel.isSearch() && panel.collapsed) {
                continue;
            }
            for (Row row : rowsFor(panel)) {
                if (row.kind() != RowKind.SETTING_TEXT) {
                    continue;
                }
                Setting<?> setting = row.setting();
                EditBox field = new EditBox(font, 0, 0, panelWidthFor(panel) - 23, 12,
                        Component.literal(setting.name()));
                field.setBordered(false);
                field.setTextShadow(false);
                field.addFormatter((text, index) -> FormattedCharSequence.forward(text, UI_STYLE));
                field.setMaxLength(SettingText.maximumLength(setting));
                field.setValue(SettingText.format(setting));
                field.setResponder(text -> field.setTextColor(
                        SettingText.tryApply(setting, text) ? COLOR_TEXT : COLOR_INVALID));
                addRenderableWidget(field);
                textFields.put(new PanelSetting(panel.key, setting), field);
            }
        }
        syncSettingWidgetPositions();
    }

    /** Keeps edit boxes aligned with their setting rows and hides clipped ones. */
    private void syncSettingWidgetPositions() {
        if (textFields.isEmpty()) {
            return;
        }
        Set<EditBox> positioned = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PanelView panel : panels) {
            if (!panel.isSearch() && panel.collapsed) {
                continue;
            }
            int contentTop = contentTop(panel);
            List<Row> rows = rowsFor(panel);
            int contentHeight = contentHeight(rows);
            int visibleHeight = visibleHeight(panel, rows);
            double scroll = scrollFor(panel, contentHeight, visibleHeight);
            for (Row row : rows) {
                EditBox field = row.setting() == null ? null
                        : textFields.get(new PanelSetting(panel.key, row.setting()));
                if (field == null || row.kind() != RowKind.SETTING_TEXT) {
                    continue;
                }
                int fieldY = contentTop + row.y() - (int) scroll + settingRowHeight + 3;
                field.setX(panel.x + 16);
                field.setY(fieldY);
                field.visible = fieldY >= contentTop && fieldY + 12 <= contentTop + visibleHeight;
                positioned.add(field);
            }
        }
        for (EditBox field : textFields.values()) {
            if (!positioned.contains(field)) {
                field.visible = false;
            }
        }
    }

    /** Keeps a setting's paired text fields showing the same value after a reset. */
    private void refreshSettingField(Setting<?> setting) {
        textFields.forEach((key, field) -> {
            if (key.setting() == setting) {
                field.setValue(SettingText.format(setting));
            }
        });
    }

    private void positionSearchField(PanelView search) {
        if (searchField == null) {
            return;
        }
        searchField.setX(search.x + 18);
        searchField.setY(search.y + (searchRowHeight - 8) / 2);
    }

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------

    private boolean isOverPanel(PanelView panel, int mouseX, int mouseY) {
        if (panel.isSearch()) {
            return isInside(mouseX, mouseY, panel.x, panel.y, searchWidth, searchPanelHeight());
        }
        int contentTop = panel.y + headerHeight;
        if (panel.collapsed) {
            return isInside(mouseX, mouseY, panel.x, panel.y, panelWidth, headerHeight);
        }
        List<Row> rows = panelRows(panel);
        int visibleHeight = visibleContentHeight(panel, contentHeight(rows));
        return isInside(mouseX, mouseY, panel.x, panel.y, panelWidth, headerHeight + visibleHeight);
    }

    private static int contentHeight(List<Row> rows) {
        return rows.isEmpty() ? 0 : rows.getLast().y() + rows.getLast().height();
    }

    private List<Row> rowsFor(PanelView panel) {
        return panel.isSearch() ? searchRows() : panelRows(panel);
    }

    private int contentTop(PanelView panel) {
        return panel.y + (panel.isSearch() ? searchRowHeight + 1 : headerHeight);
    }

    private int visibleHeight(PanelView panel, List<Row> rows) {
        return panel.isSearch() ? visibleSearchResultsHeight(rows)
                : visibleContentHeight(panel, contentHeight(rows));
    }

    private double scrollFor(PanelView panel, int contentHeight, int visibleHeight) {
        return panel.isSearch() ? searchResultScroll(contentHeight, visibleHeight)
                : panelScroll(panel, contentHeight, visibleHeight);
    }

    /**
     * The on-screen height available for a panel's rows, never negative. A
     * panel including its header is capped at half the screen height; the
     * remaining rows scroll.
     */
    private int visibleContentHeight(PanelView panel, int contentHeight) {
        int bottomLimit = Math.min(panel.y + height / 2, maxPanelBottom());
        return Math.max(0, Math.min(contentHeight, bottomLimit - panel.y - headerHeight));
    }

    private double panelScroll(PanelView panel, int contentHeight, int visibleHeight) {
        double scroll = panelScrolls.getOrDefault(panel.key, 0.0D);
        double clamped = Math.clamp(scroll, 0.0D, Math.max(0, contentHeight - visibleHeight));
        if (clamped != scroll) {
            panelScrolls.put(panel.key, clamped);
        }
        return clamped;
    }

    private void clampPanel(PanelView panel) {
        int panelViewWidth = panel.isSearch() ? searchWidth : panelWidth;
        int panelViewHeight = panel.isSearch() ? searchPanelHeight() : headerHeight;
        panel.x = Math.clamp(panel.x, 0, Math.max(0, width - panelViewWidth));
        panel.y = Math.clamp(panel.y, 0, Math.max(0, height - panelViewHeight));
    }

    private int maxPanelBottom() {
        return height - SCREEN_EDGE_MARGIN;
    }

    private int searchPanelHeight() {
        return searchRowHeight + visibleSearchResultsHeight(searchRows()) + 2;
    }

    private List<Module> searchResults() {
        return searchQuery.isBlank() ? List.copyOf(modules.all()) : ModuleSearch.filter(modules.all(), searchQuery);
    }

    private int visibleSearchResultsHeight(List<Row> rows) {
        int desiredHeight = rows.isEmpty() ? resultRowHeight
                : Math.min(contentHeight(rows), SEARCH_VISIBLE_RESULT_ROWS * resultRowHeight);
        int availableHeight = Math.max(resultRowHeight, height - searchRowHeight - 2);
        return Math.min(desiredHeight, availableHeight);
    }

    private double searchResultScroll(int contentHeight, int visibleHeight) {
        double scroll = panelScrolls.getOrDefault(SEARCH_PANEL_KEY, 0.0D);
        double clamped = Math.clamp(scroll, 0.0D, Math.max(0, contentHeight - visibleHeight));
        if (clamped != scroll) {
            panelScrolls.put(SEARCH_PANEL_KEY, clamped);
        }
        return clamped;
    }

    private int searchFieldWidth() {
        return Math.max(40, searchWidth - 18
                - (uiWidth(classicLabel()) + uiWidth(hudLabel()) + uiWidth(themeLabel()) + 32));
    }

    private String hudLabel() {
        return Component.translatable("screen.helikon.hud_button").getString();
    }

    private String themeLabel() {
        return Component.translatable("screen.helikon.theme_button").getString();
    }

    private String classicLabel() {
        return Component.translatable("screen.helikon.layout_classic").getString();
    }

    private int themeLinkX(PanelView panel) {
        return panel.x + searchWidth - 8 - uiWidth(themeLabel());
    }

    private int hudLinkX(PanelView panel) {
        return themeLinkX(panel) - 10 - uiWidth(hudLabel());
    }

    private int classicLinkX(PanelView panel) {
        return hudLinkX(panel) - 10 - uiWidth(classicLabel());
    }

    private int miniToggleWidth() {
        return Math.max(12, settingRowHeight);
    }

    private int miniToggleHeight() {
        return Math.max(7, settingRowHeight / 2 + 1);
    }

    private int sliderTrackX(PanelView panel) {
        return panel.x + panelWidthFor(panel) - 8 - sliderTrackWidth;
    }

    private int pickerX(PanelView panel) {
        return panel.x + 13;
    }

    private int pickerWidth(PanelView panel) {
        return panelWidthFor(panel) - 21;
    }

    private int panelWidthFor(PanelView panel) {
        return panel.isSearch() ? searchWidth : panelWidth;
    }

    // ------------------------------------------------------------------
    // UI font helpers
    // ------------------------------------------------------------------

    private static Component ui(String text) {
        return HelikonUiFont.ui(text);
    }

    private static Component ui(Component text) {
        return HelikonUiFont.ui(text);
    }

    private int uiWidth(String text) {
        return HelikonUiFont.width(font, text);
    }

    /** The text styled in the UI font and truncated to fit {@code maxWidth}. */
    private Component uiTrim(String text, int maxWidth) {
        return HelikonUiFont.trim(font, text, maxWidth);
    }

    // ------------------------------------------------------------------
    // Drawing primitives
    // ------------------------------------------------------------------

    /** Fills a rectangle with softly rounded corners; coordinates are exclusive like {@code fill}. */
    private static void fillRounded(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        if (x2 - x1 <= 2 || y2 - y1 <= 2) {
            graphics.fill(x1, y1, x2, y2, color);
            return;
        }
        graphics.fill(x1 + 1, y1, x2 - 1, y1 + 1, color);
        graphics.fill(x1, y1 + 1, x2, y2 - 1, color);
        graphics.fill(x1 + 1, y2 - 1, x2 - 1, y2, color);
    }

    /** Blends two opaque ARGB colors, {@code t} toward the second. */
    private static int mix(int first, int second, double t) {
        int red = (int) Math.round(((first >> 16) & 0xFF) * (1.0D - t) + ((second >> 16) & 0xFF) * t);
        int green = (int) Math.round(((first >> 8) & 0xFF) * (1.0D - t) + ((second >> 8) & 0xFF) * t);
        int blue = (int) Math.round((first & 0xFF) * (1.0D - t) + (second & 0xFF) * t);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha & 0xFF) << 24 | (color & 0xFFFFFF);
    }

    // ------------------------------------------------------------------
    // Setting helpers
    // ------------------------------------------------------------------

    private static boolean isSlider(Setting<?> setting) {
        return setting instanceof NumberSetting || setting instanceof IntegerSetting;
    }

    private static double minOf(Setting<?> setting) {
        return setting instanceof IntegerSetting integer ? integer.minimum() : ((NumberSetting) setting).minimum();
    }

    private static double maxOf(Setting<?> setting) {
        return setting instanceof IntegerSetting integer ? integer.maximum() : ((NumberSetting) setting).maximum();
    }

    private static double valueOf(Setting<?> setting) {
        return setting instanceof IntegerSetting integer ? integer.value() : ((NumberSetting) setting).value();
    }

    private static boolean integral(Setting<?> setting) {
        return setting instanceof IntegerSetting;
    }

    private void applySliderValue(Setting<?> setting, double value) {
        if (setting instanceof IntegerSetting integer) {
            integer.set((int) Math.rint(value));
        } else if (setting instanceof NumberSetting number) {
            number.set(value);
        }
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

    private String conflictWarning(Module module) {
        StringBuilder conflicts = new StringBuilder();
        for (Module conflict : KeybindConflicts.find(module, modules.all())) {
            if (!conflicts.isEmpty()) {
                conflicts.append(", ");
            }
            conflicts.append(conflict.id());
        }
        return conflicts.isEmpty() ? "" : " [conflicts: " + conflicts + "]";
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
        String primary = (keybind.isKeyboard() ? InputConstants.Type.KEYSYM : InputConstants.Type.MOUSE)
                .getOrCreate(keybind.keyCode()).getDisplayName().getString();
        String modifiers = keybind.modifiers().stream()
                .sorted()
                .map(modifier -> modifier.name().charAt(0) + modifier.name().substring(1).toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.joining("+"));
        return (modifiers.isEmpty() ? "" : modifiers + "+") + primary
                + " (" + keybind.activation().name().toLowerCase(Locale.ROOT) + ")";
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // ------------------------------------------------------------------
    // Model records
    // ------------------------------------------------------------------

    /** One floating panel; the search palette and favorites list use a {@code null} category. */
    private static final class PanelView {
        final String key;
        final ModuleCategory category;
        int x;
        int y;
        boolean collapsed;

        PanelView(String key, ModuleCategory category) {
            this.key = key;
            this.category = category;
        }

        boolean isSearch() {
            return SEARCH_PANEL_KEY.equals(key);
        }

        boolean isFavorites() {
            return FAVORITES_PANEL_KEY.equals(key);
        }

        boolean isActive() {
            return ACTIVE_PANEL_KEY.equals(key);
        }

        String title() {
            return category != null ? category.displayName() : isActive() ? "Active" : "Favorites";
        }

        /** A small glyph drawn in the accent color beside the header title. */
        String icon() {
            if (category == null) {
                return isActive() ? "●" : "★";
            }
            return switch (category) {
                case COMBAT -> "⚔";
                case MOVEMENT -> "➤";
                case PLAYER -> "☺";
                case RENDER -> "✦";
                case WORLD -> "♦";
                case CHAT -> "✉";
                case MISCELLANEOUS -> "●";
            };
        }
    }

    private enum RowKind {
        MODULE,
        HINT,
        SETTING_ACTION,
        SETTING_BOOL,
        SETTING_SLIDER,
        SETTING_COLOR,
        SETTING_ENUM,
        SETTING_TEXT,
        SETTING_LABEL,
        SETTING_BIND,
        SETTING_RESET
    }

    /** One rendered row; {@code y} is relative to the panel content top before scrolling. */
    private record Row(Module module, Setting<?> setting, RowKind kind, int y, int height) {
    }

    private record SliderDrag(Setting<?> setting, int trackX, int trackWidth) {
    }

    /** Identity key for one setting's edit box within one panel. */
    private record PanelSetting(String panelKey, Setting<?> setting) {
    }

    private record ColorDrag(ColorSetting setting, int channel, int x, int width) {
    }
}
