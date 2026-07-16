package dev.helikon.client.command;

import dev.helikon.client.macro.Macro;
import dev.helikon.client.macro.MacroActionType;
import dev.helikon.client.macro.MacroManager;
import dev.helikon.client.macro.MacroRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroCommandTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsConfiguresAndRunsExplicitMacroActions() {
        MacroManager macros = new MacroManager(temporaryDirectory.resolve("helikon"));
        MacroRunner runner = new MacroRunner();
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new MacroCommand(macros, runner, () -> Optional.of("example.org")));
        RecordingFeedback feedback = new RecordingFeedback();

        assertTrue(dispatcher.dispatch(".macro create demo server", feedback));
        assertTrue(dispatcher.dispatch(".macro add demo local .toggle fullbright_stub", feedback));
        assertTrue(dispatcher.dispatch(".macro add demo delay 3", feedback));
        assertTrue(dispatcher.dispatch(".macro add demo chat hello there", feedback));
        assertTrue(dispatcher.dispatch(".macro add demo command say ready", feedback));
        assertTrue(dispatcher.dispatch(".macro show demo", feedback));
        assertTrue(dispatcher.dispatch(".macro run demo", feedback));

        Macro macro = macros.find("demo").orElseThrow();
        assertEquals("example.org", macro.serverAddress());
        assertEquals(4, macro.actions().size());
        assertEquals(MacroActionType.LOCAL, macro.actions().getFirst().type());
        assertTrue(runner.isRunning());
        assertTrue(feedback.infos.stream().anyMatch(line -> line.contains("Started local macro 'demo'")));

        dispatcher.dispatch(".macro stop", feedback);
        assertTrue(feedback.infos.stream().anyMatch(line -> line.contains("Stopped local macro 'demo'")));
    }

    @Test
    void rejectsServerScopeWithoutAMultiplayerServer() {
        MacroManager macros = new MacroManager(temporaryDirectory.resolve("helikon"));
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new MacroCommand(macros, new MacroRunner(), Optional::empty));
        RecordingFeedback feedback = new RecordingFeedback();

        dispatcher.dispatch(".macro create serveronly server", feedback);

        assertTrue(feedback.errors.get(0).contains("require an active multiplayer server"));
    }
}
