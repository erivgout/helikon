package dev.helikon.client.command;

import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.ProfileManager;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileCommandTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesListsLoadsAndDeletesLocalProfiles() {
        ModuleRegistry registry = new ModuleRegistry();
        ConfigurableModule module = new ConfigurableModule();
        registry.register(module);
        ClickGuiWindowState window = new ClickGuiWindowState();
        ProfileManager profiles = new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon")));
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new ProfileCommand(profiles, registry, window));
        RecordingFeedback feedback = new RecordingFeedback();

        module.amount.set(8.0);
        dispatcher.dispatch(".profile save builder", feedback);
        dispatcher.dispatch(".profile list", feedback);
        module.amount.set(1.0);
        dispatcher.dispatch(".profile load builder", feedback);
        dispatcher.dispatch(".profile delete builder", feedback);

        assertEquals(8.0, module.amount.value());
        assertTrue(feedback.infos.stream().anyMatch(message -> message.contains("Profiles: builder")));
        assertTrue(feedback.infos.stream().anyMatch(message -> message.contains("Deleted local profile 'builder'")));
    }

    private static final class ConfigurableModule extends Module {
        private final NumberSetting amount;

        private ConfigurableModule() {
            super("configurable", "Configurable", "Used by profile command tests.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            amount = addSetting(new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0));
        }
    }
}
