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

    @Test
    void duplicatesAndRenamesProfilesWithMatchingStoredNames() {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        registry.register(source);
        source.amount.set(6.0);
        profiles.save("builder", registry, new ClickGuiWindowState());

        assertTrue(profiles.duplicate("builder", "copy"));
        assertTrue(profiles.rename("copy", "renovated"));
        assertEquals(java.util.List.of("builder", "renovated"), profiles.list());
        assertFalse(Files.exists(profiles.profilePath("copy")));
        assertThrows(IllegalArgumentException.class, () -> profiles.duplicate("builder", "renovated"));

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);
        assertEquals(ProfileManager.LoadResult.LOADED,
                profiles.load("renovated", targetRegistry, new ClickGuiWindowState()));
        assertEquals(6.0, target.amount.value());
    }

    @Test
    void renamePreservesTheSourceBackupUnderTheNewName() {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        registry.register(module);

        profiles.save("builder", registry, new ClickGuiWindowState());
        module.amount.set(5.0);
        profiles.save("builder", registry, new ClickGuiWindowState());
        assertTrue(Files.exists(profiles.profilesDirectory().resolve("builder.json.bak")));

        assertTrue(profiles.rename("builder", "renovated"));

        assertFalse(Files.exists(profiles.profilePath("builder")));
        assertFalse(Files.exists(profiles.profilesDirectory().resolve("builder.json.bak")));
        assertTrue(Files.exists(profiles.profilesDirectory().resolve("renovated.json.bak")));
    }

    @Test
    void copyAndRenameRejectBadSourcesAndUnsafeDestinationsWithoutWritingTargets() throws IOException {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        Files.createDirectories(profiles.profilesDirectory());
        Files.writeString(profiles.profilePath("broken"), "not JSON");
        Files.createDirectories(profiles.profilePath("unreadable"));

        assertThrows(IllegalArgumentException.class, () -> profiles.duplicate("broken", "copy"));
        assertTrue(Files.exists(profiles.profilePath("broken")));
        assertFalse(Files.exists(profiles.profilePath("copy")));
        assertThrows(ConfigurationException.class, () -> profiles.duplicate("unreadable", "copy"));
        assertTrue(Files.isDirectory(profiles.profilePath("unreadable")));
        assertFalse(Files.exists(profiles.profilePath("copy")));
        Files.writeString(profiles.profilePath("unsupported"),
                "{\"schemaVersion\": 99, \"profileName\": \"unsupported\", \"modules\": {}}");
        assertThrows(IllegalArgumentException.class, () -> profiles.duplicate("unsupported", "copy"));
        assertTrue(Files.exists(profiles.profilePath("unsupported")));
        assertFalse(Files.exists(profiles.profilePath("copy")));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new ConfigurableModule());
        profiles.save("source", registry, new ClickGuiWindowState());
        profiles.save("target", registry, new ClickGuiWindowState());
        assertThrows(IllegalArgumentException.class, () -> profiles.rename("source", "target"));
        assertThrows(IllegalArgumentException.class, () -> profiles.rename("source", "source"));
        assertTrue(Files.exists(profiles.profilePath("source")));
        assertTrue(Files.exists(profiles.profilePath("target")));
    }

    @Test
    void exportsAndImportsValidatedProfilesWithinTheHelikonDirectory() throws IOException {
        ConfigurationManager configuration = new ConfigurationManager(temporaryDirectory.resolve("helikon"));
        ProfileManager profiles = new ProfileManager(configuration);
        ModuleRegistry sourceRegistry = new ModuleRegistry();
        ConfigurableModule source = new ConfigurableModule();
        sourceRegistry.register(source);
        source.amount.set(7.0);
        profiles.save("builder", sourceRegistry, new ClickGuiWindowState());

        assertTrue(profiles.exportProfile("builder", "portable"));
        Path exportPath = profiles.exportsDirectory().resolve("portable.json");
        assertTrue(Files.exists(exportPath));
        Files.createDirectories(profiles.importsDirectory());
        Files.copy(exportPath, profiles.importsDirectory().resolve("incoming.json"));
        assertTrue(profiles.importProfile("incoming", "imported"));

        ModuleRegistry targetRegistry = new ModuleRegistry();
        ConfigurableModule target = new ConfigurableModule();
        targetRegistry.register(target);
        assertEquals(ProfileManager.LoadResult.LOADED,
                profiles.load("imported", targetRegistry, new ClickGuiWindowState()));
        assertEquals(7.0, target.amount.value());
    }

    @Test
    void rejectsInvalidImportsWithoutCreatingAProfile() throws IOException {
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon")));
        Files.createDirectories(profiles.importsDirectory());
        Files.writeString(profiles.importsDirectory().resolve("invalid.json"), "{\"schemaVersion\": 99, \"modules\": {}}");

        assertThrows(IllegalArgumentException.class, () -> profiles.importProfile("invalid", "target"));
        assertFalse(Files.exists(profiles.profilePath("target")));
        assertTrue(Files.exists(profiles.importsDirectory().resolve("invalid.json")));
    }

    @Test
    void persistsDefaultProfileAndUpdatesItForRenameAndDelete() {
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon")));
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new ConfigurableModule());
        profiles.save("builder", registry, new ClickGuiWindowState());

        assertTrue(profiles.setDefault("builder"));
        assertEquals("builder", profiles.defaultProfile().orElseThrow());
        assertTrue(profiles.rename("builder", "renovated"));
        assertEquals("renovated", profiles.defaultProfile().orElseThrow());
        assertTrue(profiles.delete("renovated"));
        assertTrue(profiles.defaultProfile().isEmpty());
    }

    @Test
    void persistsServerAndSingleplayerAssociationsAndUpdatesReferences() {
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon")));
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new ConfigurableModule());
        profiles.save("builder", registry, new ClickGuiWindowState());

        assertTrue(profiles.setServerProfile("Example.Org:25565", "builder"));
        assertTrue(profiles.setSingleplayerProfile("My World", "builder"));
        assertEquals("builder", profiles.serverProfile("example.org:25565").orElseThrow());
        assertEquals("builder", profiles.singleplayerProfile("My World").orElseThrow());

        assertTrue(profiles.rename("builder", "renovated"));
        assertEquals("renovated", profiles.serverProfile("EXAMPLE.ORG:25565").orElseThrow());
        assertEquals("renovated", profiles.singleplayerProfile("My World").orElseThrow());
        profiles.clearServerProfile("example.org:25565");
        assertTrue(profiles.serverProfile("example.org:25565").isEmpty());
        assertTrue(profiles.delete("renovated"));
        assertTrue(profiles.singleplayerProfile("My World").isEmpty());
    }

    @Test
    void refusesToRemoveAProfileWhenItsPreferenceManifestCannotBeUpdated() throws IOException {
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon")));
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new ConfigurableModule());
        profiles.save("builder", registry, new ClickGuiWindowState());
        profiles.setDefault("builder");
        Path preferencesPath = temporaryDirectory.resolve("helikon").resolve("profiles.json");
        Files.delete(preferencesPath);
        Files.createDirectory(preferencesPath);

        assertThrows(ConfigurationException.class, () -> profiles.rename("builder", "renovated"));
        assertTrue(Files.exists(profiles.profilePath("builder")));
    }

    @Test
    void malformedPreferenceAssociationKeyIsRecovered() throws IOException {
        Path configurationDirectory = temporaryDirectory.resolve("helikon");
        Files.createDirectories(configurationDirectory);
        Files.writeString(configurationDirectory.resolve("profiles.json"), """
                {"schemaVersion":1,"serverProfiles":{"bad\u0001key":"builder"}}
                """);
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(configurationDirectory));

        assertTrue(profiles.defaultProfile().isEmpty());
        try (var files = Files.list(configurationDirectory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("profiles.corrupt-")));
        }
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
