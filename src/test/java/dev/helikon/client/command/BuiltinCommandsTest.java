package dev.helikon.client.command;

import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.config.PanicConfigurationManager;
import dev.helikon.client.config.ProfileManager;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.PanicKeybindManager;
import dev.helikon.client.macro.MacroManager;
import dev.helikon.client.macro.MacroRunner;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.PrivateMessageHelper;
import dev.helikon.client.panic.PanicController;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.privatechat.PrivateMessageHistory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import dev.helikon.client.waypoint.WaypointContext;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinCommandsTest {
    private static final int KEY_R = 82;
    private static final int KEY_RIGHT_SHIFT = 344;

    @TempDir
    Path temporaryDirectory;

    private final KeyNameResolver fakeKeys = name -> switch (name) {
        case "r" -> OptionalInt.of(KEY_R);
        case "f6" -> OptionalInt.of(295);
        case "right.shift" -> OptionalInt.of(KEY_RIGHT_SHIFT);
        default -> OptionalInt.empty();
    };

    private ModuleRegistry registry;
    private ConfigurableModule module;
    private CommandDispatcher dispatcher;
    private RecordingFeedback feedback;
    private PrivateMessageHistory privateMessageHistory;
    private List<String> sentServerCommands;
    private boolean guiOpened;

    @BeforeEach
    void setUp() {
        registry = new ModuleRegistry();
        module = new ConfigurableModule();
        registry.register(module);
        PrivateMessageHelper privateMessageHelper = new PrivateMessageHelper();
        registry.register(privateMessageHelper);
        registry.setEnabled(privateMessageHelper, true);
        privateMessageHistory = new PrivateMessageHistory();
        sentServerCommands = new ArrayList<>();
        dispatcher = new CommandDispatcher();
        feedback = new RecordingFeedback();
        guiOpened = false;
        PanicState panicState = new PanicState();
        PanicKeybindManager panicKeybinds = new PanicKeybindManager();
        HelikonCommands.registerDefaults(dispatcher, registry, fakeKeys,
                key -> key == KEY_RIGHT_SHIFT, () -> guiOpened = true,
                new ProfileManager(new ConfigurationManager(temporaryDirectory.resolve("helikon"))),
                new ClickGuiWindowState(), new FriendManager(temporaryDirectory.resolve("helikon")),
                new WaypointManager(temporaryDirectory.resolve("helikon")),
                () -> java.util.Optional.of(new WaypointLocation(0, 64, 0,
                        new WaypointContext("world:command-test", "minecraft:overworld"))),
                new MacroManager(temporaryDirectory.resolve("helikon")), new MacroRunner(), java.util.Optional::empty,
                new PanicController(registry, panicState, () -> { }, () -> { }), panicKeybinds,
                new PanicConfigurationManager(temporaryDirectory.resolve("helikon")), privateMessageHelper,
                privateMessageHistory, sentServerCommands::add);
    }

    @Test
    void helpListsEveryRegisteredCommand() {
        dispatcher.dispatch(".help", feedback);
        // The intro line plus one line per built-in command.
        assertEquals(1 + dispatcher.commands().size(), feedback.infos.size());
        assertTrue(feedback.infos.stream().anyMatch(line -> line.contains(".toggle <module>")));
    }

    @Test
    void toggleEnablesAndDisablesThroughTheRegistry() {
        dispatcher.dispatch(".toggle configurable", feedback);
        assertTrue(module.isEnabled());
        assertTrue(feedback.infos.get(0).contains("Enabled 'configurable'"));

        dispatcher.dispatch(".toggle configurable", feedback);
        assertFalse(module.isEnabled());
    }

    @Test
    void toggleReportsUnknownModules() {
        dispatcher.dispatch(".toggle nope", feedback);
        assertTrue(feedback.errors.get(0).contains("Unknown module 'nope'"));
    }

    @Test
    void toggleFailureIsIsolatedAndReported() {
        FailingModule failing = new FailingModule();
        registry.register(failing);

        dispatcher.dispatch(".toggle failing", feedback);
        assertFalse(failing.isEnabled());
        assertTrue(feedback.errors.get(0).contains("Failed to enable 'failing'"));
    }

    @Test
    void modulesListsStatePerCategory() {
        dispatcher.dispatch(".modules", feedback);
        assertTrue(feedback.infos.stream().anyMatch(line ->
                line.startsWith("Miscellaneous:") && line.contains("configurable [off]")));
    }

    @Test
    void searchFindsModulesAndReportsNoMatches() {
        dispatcher.dispatch(".search config", feedback);
        assertTrue(feedback.infos.stream().anyMatch(line -> line.contains("configurable")));

        RecordingFeedback empty = new RecordingFeedback();
        dispatcher.dispatch(".search zzz", empty);
        assertTrue(empty.infos.get(0).contains("No modules match"));
    }

    @Test
    void settingEditsSupportedValues() {
        dispatcher.dispatch(".setting configurable flag false", feedback);
        assertFalse(module.flag.value());

        dispatcher.dispatch(".setting configurable amount 7.5", feedback);
        assertEquals(7.5, module.amount.value());

        dispatcher.dispatch(".setting configurable color #80FF6600", feedback);
        assertEquals(0x80FF6600, module.color.value());

        dispatcher.dispatch(".setting configurable mode second", feedback);
        assertEquals(TestMode.SECOND, module.mode.value());

        dispatcher.dispatch(".setting configurable text bread", feedback);
        assertEquals("bread", module.text.value());
    }

    @Test
    void settingRejectsInvalidValues() {
        dispatcher.dispatch(".setting configurable flag maybe", feedback);
        assertTrue(feedback.errors.get(0).contains("Expected true or false"));
        assertTrue(module.flag.value());

        dispatcher.dispatch(".setting configurable amount 99", feedback);
        assertTrue(feedback.errors.get(1).contains("between 0 and 10"));
        assertEquals(2.0, module.amount.value());

        dispatcher.dispatch(".setting configurable color #FF00FF", feedback);
        assertTrue(feedback.errors.get(2).contains("Expected #AARRGGBB"));

        dispatcher.dispatch(".setting configurable mode missing", feedback);
        assertTrue(feedback.errors.get(3).contains("Expected one of first, second"));
    }

    @Test
    void settingReportsUnknownSetting() {
        dispatcher.dispatch(".setting configurable nope true", feedback);
        assertTrue(feedback.errors.get(0).contains("has no setting 'nope'"));
    }

    @Test
    void resetRestoresDefaults() {
        module.amount.set(9.0);
        module.flag.set(false);

        dispatcher.dispatch(".reset configurable", feedback);
        assertEquals(2.0, module.amount.value());
        assertTrue(module.flag.value());
    }

    @Test
    void bindAssignsKeyAndActivation() {
        dispatcher.dispatch(".bind configurable r", feedback);
        assertEquals(new Keybind(KEY_R, Keybind.Activation.TOGGLE), module.keybind());

        dispatcher.dispatch(".bind configurable f6 hold", feedback);
        assertEquals(Keybind.Activation.HOLD, module.keybind().activation());
    }

    @Test
    void bindRejectsUnknownKeysAndActivations() {
        dispatcher.dispatch(".bind configurable notakey", feedback);
        assertTrue(feedback.errors.get(0).contains("Unknown key 'notakey'"));
        assertFalse(module.keybind().isBound());

        dispatcher.dispatch(".bind configurable r sometimes", feedback);
        assertTrue(feedback.errors.get(1).contains("Unknown activation 'sometimes'"));
        assertFalse(module.keybind().isBound());
    }

    @Test
    void bindRejectsTheGuiKey() {
        dispatcher.dispatch(".bind configurable right.shift", feedback);
        assertTrue(feedback.errors.get(0).contains("opens the Helikon GUI"));
        assertFalse(module.keybind().isBound());
    }

    @Test
    void unbindClearsTheKeybind() {
        module.setKeybind(new Keybind(KEY_R, Keybind.Activation.TOGGLE));
        dispatcher.dispatch(".unbind configurable", feedback);
        assertFalse(module.keybind().isBound());
    }

    @Test
    void guiRunsTheOpener() {
        dispatcher.dispatch(".gui", feedback);
        assertTrue(guiOpened);
    }

    @Test
    void panicDisablesAllModules() {
        registry.setEnabled(module, true);
        dispatcher.dispatch(".panic", feedback);
        assertFalse(module.isEnabled());
        assertTrue(feedback.infos.get(0).contains("disabled 2 module(s)"));
    }

    @Test
    void privateMessageCommandsSendOnlyValidatedNormalCommandsAndKeepLocalHistory() {
        dispatcher.dispatch(".pm Alice_1 hello there", feedback);
        assertEquals(List.of("msg Alice_1 hello there"), sentServerCommands);
        assertEquals("Sent local PM to 'Alice_1'.", feedback.infos.getFirst());

        dispatcher.dispatch(".pm history alice_1", feedback);
        assertTrue(feedback.infos.stream().anyMatch(line -> line.equals("You: hello there")));

        dispatcher.dispatch(".reply received", feedback);
        assertEquals(List.of("msg Alice_1 hello there", "r received"), sentServerCommands);
    }

    @Test
    void privateMessageCommandRejectsUnsafeNamesAndOversizedComposedCommands() {
        dispatcher.dispatch(".pm bad-name hello", feedback);
        assertTrue(feedback.errors.getFirst().contains("Minecraft-style player name"));
        assertTrue(sentServerCommands.isEmpty());

        String tooLong = "x".repeat(PrivateMessageCommand.MAXIMUM_COMMAND_LENGTH);
        dispatcher.dispatch(".pm Alice " + tooLong, feedback);
        assertTrue(feedback.errors.getLast().contains("safe local limit"));
        assertTrue(sentServerCommands.isEmpty());
        assertTrue(privateMessageHistory.tabs().isEmpty());
    }

    @Test
    void privateMessageCommandsReserveHistoryButAllowTheLiteralWordWithEscape() {
        dispatcher.dispatch(".pm -- history hello", feedback);
        dispatcher.dispatch(".reply -- history", feedback);

        assertEquals(List.of("msg history hello", "r history"), sentServerCommands);
        assertTrue(feedback.errors.isEmpty());
    }

    private static final class ConfigurableModule extends Module {
        private final BooleanSetting flag;
        private final NumberSetting amount;
        private final ColorSetting color;
        private final EnumSetting<TestMode> mode;
        private final StringSetting text;

        private ConfigurableModule() {
            super("configurable", "Configurable", "Used by command tests.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
            flag = addSetting(new BooleanSetting("flag", "Flag", "A test flag.", true));
            amount = addSetting(new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0));
            color = addSetting(new ColorSetting("color", "Color", "A test color.", 0xFFFFFFFF));
            mode = addSetting(new EnumSetting<>("mode", "Mode", "A test enum.", TestMode.class, TestMode.FIRST));
            text = addSetting(new StringSetting("text", "Text", "A test string.", "", 8, true));
        }
    }

    private enum TestMode {
        FIRST,
        SECOND
    }

    private static final class FailingModule extends Module {
        private FailingModule() {
            super("failing", "Failing", "Always fails to enable.",
                    ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        }

        @Override
        protected void onEnable() {
            throw new IllegalStateException("intentional test failure");
        }
    }
}
