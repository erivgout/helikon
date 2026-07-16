package dev.helikon.client.config;

import dev.helikon.client.gui.ClickGuiTheme;
import dev.helikon.client.gui.ClickGuiWindowState;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesListsLoadsAndBacksUpNamedProfiles() {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        sourceRegistry.register(source);
        sourceRegistry.setEnabled(source, true);
        source.amount.set(7.5);
        ClickGuiWindowState sourceWindow = new ClickGuiWindowState();
        sourceWindow.setPosition(44, 72);
        sourceWindow.setTheme(ClickGuiTheme.OCEAN);

        profiles.save("Builder", sourceRegistry, sourceWindow);
        assertEquals(java.util.List.of("builder"), profiles.list());

        source.amount.set(3.0);
        profiles.save("builder", sourceRegistry, sourceWindow);
        assertTrue(Files.exists(profiles.profilesDirectory().resolve("builder.json.bak")));

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);
        ClickGuiWindowState targetWindow = new ClickGuiWindowState();

        assertEquals(ProfileManager.LoadResult.LOADED, profiles.load("BUILDER", targetRegistry, targetWindow));
        assertTrue(target.isEnabled());
        assertEquals(3.0, target.amount.value());
        assertTrue(targetWindow.isPositioned());
        assertEquals(44, targetWindow.x());
        assertEquals(ClickGuiTheme.OCEAN, targetWindow.theme());
    }

    @Test
    void malformedProfileIsRecoveredBeforeItCanChangeLiveState() throws IOException {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        Files.createDirectories(profiles.profilesDirectory());
        Files.writeString(profiles.profilePath("unsafe"), "not valid json");

        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        registry.register(module);
        registry.setEnabled(module, true);

        assertEquals(ProfileManager.LoadResult.RECOVERED_FROM_ERROR,
                profiles.load("unsafe", registry, new ClickGuiWindowState()));
        assertTrue(module.isEnabled());
        assertTrue(profiles.list().isEmpty());
        try (var files = Files.list(profiles.profilesDirectory())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("unsafe.corrupt-")));
        }
    }

    @Test
    void incompleteProfileResetsAllMissingModuleStateToDefaults() throws IOException {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        Files.createDirectories(profiles.profilesDirectory());
        Files.writeString(profiles.profilePath("incomplete"), """
                {"schemaVersion": 1, "profileName": "incomplete", "modules": {}}
                """);

        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        registry.register(module);
        registry.setEnabled(module, true);
        module.amount.set(9.0);
        module.setKeybind(new Keybind(82, Keybind.Activation.HOLD));

        assertEquals(ProfileManager.LoadResult.LOADED,
                profiles.load("incomplete", registry, new ClickGuiWindowState()));
        assertFalse(module.isEnabled());
        assertEquals(2.0, module.amount.value());
        assertFalse(module.keybind().isBound());
    }

    @Test
    void unreadableProfileIsLeftInPlaceWithoutChangingLiveState() throws IOException {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        Files.createDirectories(profiles.profilePath("unreadable"));

        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        registry.register(module);
        registry.setEnabled(module, true);

        assertEquals(ProfileManager.LoadResult.UNAVAILABLE,
                profiles.load("unreadable", registry, new ClickGuiWindowState()));
        assertTrue(module.isEnabled());
        assertTrue(Files.isDirectory(profiles.profilePath("unreadable")));
    }

    @Test
    void rejectsUnsafeProfileNamesAndReportsMissingProfiles() {
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon")));
        ModuleRegistry registry = new ModuleRegistry();

        assertThrows(IllegalArgumentException.class,
                () -> profiles.save("../outside", registry, new ClickGuiWindowState()));
        assertEquals(ProfileManager.LoadResult.MISSING,
                profiles.load("missing", registry, new ClickGuiWindowState()));
        assertFalse(profiles.delete("missing"));
    }

    private static final class ConfigurableModule extends Module {
        private final NumberSetting amount;

        private ConfigurableModule() {
            super("configurable", "Configurable", "Used by profile tests.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            amount = addSetting(new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0));
        }
    }
}
