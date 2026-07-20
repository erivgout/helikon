package dev.helikon.client.config;

import com.google.gson.JsonParser;
import dev.helikon.client.input.Keybind;
import org.lwjgl.glfw.GLFW;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.gui.ClickGuiTheme;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsTheLocalGlobalSaveStateWithoutPersistingDiagnostics() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new ConfigurableModule());
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));

        assertEquals(ConfigurationManager.SaveStatus.NOT_SAVED, manager.saveStatus());
        manager.save(registry);
        assertEquals(ConfigurationManager.SaveStatus.SAVED, manager.saveStatus());
    }

    @Test
    void saveAndLoadPreserveModuleStateAndSettingValues() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        sourceRegistry.register(source);
        sourceRegistry.setEnabled(source, true);
        source.amount.set(7.5);

        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        manager.save(sourceRegistry);

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);

        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(targetRegistry));
        assertTrue(target.isEnabled());
        assertEquals(7.5, target.amount.value());
    }

    @Test
    void aStateChangeHandlerCanPersistAToggleBeforeShutdown() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        sourceRegistry.register(source);
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        sourceRegistry.addStateChangeHandler((module, enabled) -> manager.save(sourceRegistry));

        sourceRegistry.setEnabled(source, true);

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(targetRegistry));
        assertTrue(target.isEnabled());
    }

    @Test
    void legacyFullbrightStubStateLoadsIntoTheProductionModule() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {
                    "fullbright_stub": {
                      "enabled": true,
                      "keybind": {"key": -1, "activation": "toggle"},
                      "settings": {"use_gamma": false, "brightness": 0.6}
                    }
                  }
                }
                """);

        ModuleRegistry registry = new ModuleRegistry();
        LegacyFullbrightModule fullbright = new LegacyFullbrightModule();
        registry.register(fullbright);

        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(registry));
        assertTrue(fullbright.isEnabled());
        assertFalse(fullbright.gammaMode.value());
        assertEquals(0.6, fullbright.brightness.value());
    }

    @Test
    void legacyFullbrightMigrationIsWrittenBackOnlyUnderTheProductionId() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {
                    "fullbright_stub": {
                      "enabled": true,
                      "keybind": {"key": -1, "activation": "toggle"},
                      "settings": {"use_gamma": false, "brightness": 0.6}
                    }
                  }
                }
                """);

        ModuleRegistry registry = new ModuleRegistry();
        LegacyFullbrightModule fullbright = new LegacyFullbrightModule();
        registry.register(fullbright);
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(registry));

        manager.save(registry);

        var modules = JsonParser.parseString(Files.readString(manager.globalConfigurationPath()))
                .getAsJsonObject()
                .getAsJsonObject("modules");
        assertTrue(modules.has("fullbright"));
        assertFalse(modules.has("fullbright_stub"));
        var productionState = modules.getAsJsonObject("fullbright");
        assertTrue(productionState.get("enabled").getAsBoolean());
        var settings = productionState.getAsJsonObject("settings");
        assertFalse(settings.get("use_gamma").getAsBoolean());
        assertEquals(0.6, settings.get("brightness").getAsDouble());
    }

    @Test
    void keybindsRoundTripThroughConfiguration() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        sourceRegistry.register(source);
        source.setKeybind(new Keybind(82, Keybind.Activation.HOLD));

        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        manager.save(sourceRegistry);

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);

        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(targetRegistry));
        assertEquals(new Keybind(82, Keybind.Activation.HOLD), target.keybind());
    }

    @Test
    void mouseModifierKeybindsRoundTripThroughConfiguration() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        sourceRegistry.register(source);
        source.setKeybind(new Keybind(Keybind.InputType.MOUSE_BUTTON, GLFW.GLFW_MOUSE_BUTTON_5,
                java.util.Set.of(Keybind.Modifier.ALT), Keybind.Activation.PRESS_ONCE));

        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        manager.save(sourceRegistry);

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);

        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(targetRegistry));
        assertEquals(source.keybind(), target.keybind());
    }

    @Test
    void invalidStoredKeybindFallsBackToUnbound() throws IOException {
        assertStoredKeybindIsRejected("{\"key\": 82, \"activation\": \"sometimes\"}");
        assertStoredKeybindIsRejected("{\"key\": 0, \"activation\": \"toggle\"}");
        assertStoredKeybindIsRejected("{\"key\": 999999, \"activation\": \"toggle\"}");
    }

    private void assertStoredKeybindIsRejected(String keybindJson) throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {
                    "configurable": {
                      "enabled": false,
                      "keybind": %s,
                      "settings": {}
                    }
                  }
                }
                """.formatted(keybindJson));

        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        module.setKeybind(new Keybind(65, Keybind.Activation.TOGGLE));
        registry.register(module);

        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(registry));
        assertFalse(module.keybind().isBound(), "keybind " + keybindJson + " must reset to unbound");
    }

    @Test
    void malformedConfigurationIsPreservedAndDoesNotEnableModules() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), "not valid json");

        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        registry.register(module);

        assertEquals(ConfigurationManager.LoadResult.RECOVERED_FROM_ERROR, manager.load(registry));
        assertFalse(module.isEnabled());
        try (var files = Files.list(manager.configurationDirectory())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("global.corrupt-")));
        }
    }

    @Test
    void clickGuiPositionRoundTripsThroughGlobalConfiguration() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        sourceRegistry.register(new ConfigurableModule());
        ClickGuiWindowState sourceWindow = new ClickGuiWindowState();
        sourceWindow.setPosition(44, 72);
        sourceWindow.setSize(420, 260);
        sourceWindow.setTheme(ClickGuiTheme.OCEAN);

        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        manager.save(sourceRegistry, sourceWindow);

        ModuleRegistry targetRegistry = new ModuleRegistry();
        targetRegistry.register(new ConfigurableModule());
        ClickGuiWindowState targetWindow = new ClickGuiWindowState();
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(targetRegistry, targetWindow));
        assertTrue(targetWindow.isPositioned());
        assertEquals(44, targetWindow.x());
        assertEquals(72, targetWindow.y());
        assertTrue(targetWindow.isSized());
        assertEquals(420, targetWindow.width());
        assertEquals(260, targetWindow.height());
        assertEquals(ClickGuiTheme.OCEAN, targetWindow.theme());
    }

    @Test
    void clickGuiNavigationAndScrollRoundTripThroughGlobalConfiguration() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule sourceModule = new ConfigurableModule();
        sourceRegistry.register(sourceModule);
        ClickGuiWindowState sourceWindow = new ClickGuiWindowState();
        assertTrue(sourceWindow.setViewState(dev.helikon.client.gui.ClickGuiState.ViewMode.FAVORITES,
                ModuleCategory.MISCELLANEOUS, "config", sourceModule.id(), 84.0D, 126.0D));

        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        manager.save(sourceRegistry, sourceWindow);

        ClickGuiWindowState targetWindow = new ClickGuiWindowState();
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(sourceRegistry, targetWindow));
        assertTrue(targetWindow.hasSavedViewState());
        assertEquals(dev.helikon.client.gui.ClickGuiState.ViewMode.FAVORITES, targetWindow.viewMode());
        assertEquals(ModuleCategory.MISCELLANEOUS, targetWindow.selectedCategory());
        assertEquals("config", targetWindow.searchQuery());
        assertEquals(sourceModule.id(), targetWindow.selectedModuleId());
        assertEquals(84.0D, targetWindow.listScroll());
        assertEquals(126.0D, targetWindow.settingsScroll());
    }

    @Test
    void invalidStoredClickGuiPositionResetsToCentering() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {},
                  "clickGui": {
                    "positioned": true,
                    "x": -1,
                    "y": 20
                  }
                }
                """);

        ClickGuiWindowState window = new ClickGuiWindowState();
        window.setPosition(50, 50);
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(new ModuleRegistry(), window));
        assertFalse(window.isPositioned());
    }

    @Test
    void oldClickGuiLayoutWithoutSizeKeepsDefaultDimensions() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {},
                  "clickGui": {"positioned": false}
                }
                """);

        ClickGuiWindowState window = new ClickGuiWindowState();
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(new ModuleRegistry(), window));
        assertFalse(window.isPositioned());
        assertFalse(window.isSized());
        assertEquals(ClickGuiWindowState.DEFAULT_WIDTH, window.width());
        assertEquals(ClickGuiWindowState.DEFAULT_HEIGHT, window.height());
    }

    @Test
    void invalidStoredClickGuiDimensionsResetWithoutDiscardingPosition() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {},
                  "clickGui": {
                    "positioned": true,
                    "x": 40,
                    "y": 60,
                    "sized": true,
                    "width": 0,
                    "height": 220
                  }
                }
                """);

        ClickGuiWindowState window = new ClickGuiWindowState();
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(new ModuleRegistry(), window));
        assertTrue(window.isPositioned());
        assertEquals(40, window.x());
        assertEquals(60, window.y());
        assertFalse(window.isSized());
        assertEquals(ClickGuiWindowState.DEFAULT_WIDTH, window.width());
        assertEquals(ClickGuiWindowState.DEFAULT_HEIGHT, window.height());
    }

    @Test
    void invalidStoredClickGuiThemeFallsBackToDefault() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {},
                  "clickGui": {"positioned": false, "theme": "unknown"}
                }
                """);

        ClickGuiWindowState window = new ClickGuiWindowState();
        window.setTheme(ClickGuiTheme.OCEAN);
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(new ModuleRegistry(), window));
        assertEquals(ClickGuiTheme.SLATE, window.theme());
    }

    @Test
    void clickGuiPanelLayoutRoundTripsThroughGlobalConfiguration() {
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        sourceRegistry.register(new ConfigurableModule());
        ClickGuiWindowState sourceWindow = new ClickGuiWindowState();
        assertTrue(sourceWindow.setPanelPlacement("render", 24, 36, false));
        assertTrue(sourceWindow.setPanelPlacement("search", 120, 300, true));
        sourceWindow.setModuleExpanded("configurable", true);
        sourceWindow.setClassicLayout(true);

        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        manager.save(sourceRegistry, sourceWindow);

        ClickGuiWindowState targetWindow = new ClickGuiWindowState();
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(sourceRegistry, targetWindow));
        ClickGuiWindowState.PanelPlacement render = targetWindow.panelPlacement("render").orElseThrow();
        assertEquals(24, render.x());
        assertEquals(36, render.y());
        assertFalse(render.collapsed());
        assertTrue(targetWindow.panelPlacement("search").orElseThrow().collapsed());
        assertTrue(targetWindow.isModuleExpanded("configurable"));
        assertTrue(targetWindow.classicLayout());
    }

    @Test
    void invalidStoredPanelPlacementsAreSkippedIndividually() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {},
                  "clickGui": {
                    "positioned": false,
                    "panels": {
                      "render": {"x": -5, "y": 10},
                      "combat": {"x": 5, "y": 10, "collapsed": true}
                    },
                    "expandedModules": ["configurable", "Not Valid!"]
                  }
                }
                """);

        ClickGuiWindowState window = new ClickGuiWindowState();
        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(new ModuleRegistry(), window));
        assertTrue(window.panelPlacement("render").isEmpty());
        ClickGuiWindowState.PanelPlacement combat = window.panelPlacement("combat").orElseThrow();
        assertEquals(5, combat.x());
        assertTrue(combat.collapsed());
        assertTrue(window.isModuleExpanded("configurable"));
        assertFalse(window.isModuleExpanded("Not Valid!"));
    }

    private static final class ConfigurableModule extends Module {
        private final NumberSetting amount;

        private ConfigurableModule() {
            super("configurable", "Configurable", "Used by configuration tests.", ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            amount = addSetting(new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0));
        }
    }

    private static final class LegacyFullbrightModule extends Module {
        private final BooleanSetting gammaMode;
        private final NumberSetting brightness;

        private LegacyFullbrightModule() {
            super("fullbright", "Fullbright", "Used to test legacy configuration migration.",
                    ModuleCategory.RENDER, false, Keybind.unbound());
            gammaMode = addSetting(new BooleanSetting("use_gamma", "Gamma mode", "A test gamma mode.", true));
            brightness = addSetting(new NumberSetting("brightness", "Brightness", "A test brightness.", 1.0, 0.0, 1.0));
        }
    }
}
