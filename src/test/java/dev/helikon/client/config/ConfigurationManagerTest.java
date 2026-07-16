package dev.helikon.client.config;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
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
    void invalidStoredKeybindFallsBackToUnbound() throws IOException {
        ConfigurationManager manager = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.configurationDirectory());
        Files.writeString(manager.globalConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "modules": {
                    "configurable": {
                      "enabled": false,
                      "keybind": {"key": 82, "activation": "sometimes"},
                      "settings": {}
                    }
                  }
                }
                """);

        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        module.setKeybind(new Keybind(65, Keybind.Activation.TOGGLE));
        registry.register(module);

        assertEquals(ConfigurationManager.LoadResult.LOADED, manager.load(registry));
        assertFalse(module.keybind().isBound());
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

    private static final class ConfigurableModule extends Module {
        private final NumberSetting amount;

        private ConfigurableModule() {
            super("configurable", "Configurable", "Used by configuration tests.", ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            amount = addSetting(new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0));
        }
    }
}
