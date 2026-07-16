package dev.helikon.client.macro;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MacroRunnerTest {
    @Test
    void schedulesOneActionPerTickAndHonorsExplicitDelays() {
        Macro macro = new Macro("test", Macro.GLOBAL, List.of(
                MacroAction.local(".toggle example"),
                MacroAction.delay(2),
                MacroAction.chat("hello"),
                MacroAction.command("say done")
        ));
        MacroRunner runner = new MacroRunner();
        RecordingExecutor executor = new RecordingExecutor();
        runner.start(macro, Optional.empty());

        assertEquals(MacroRunner.TickStatus.EXECUTED, runner.tick(Optional.empty(), executor).status());
        assertEquals(MacroRunner.TickStatus.WAITING, runner.tick(Optional.empty(), executor).status());
        assertEquals(MacroRunner.TickStatus.WAITING, runner.tick(Optional.empty(), executor).status());
        assertEquals(MacroRunner.TickStatus.WAITING, runner.tick(Optional.empty(), executor).status());
        assertEquals(MacroRunner.TickStatus.EXECUTED, runner.tick(Optional.empty(), executor).status());
        assertEquals(MacroRunner.TickStatus.COMPLETED, runner.tick(Optional.empty(), executor).status());

        assertEquals(List.of("local:.toggle example", "chat:hello", "command:say done"), executor.actions);
        assertFalse(runner.isRunning());
    }

    @Test
    void restrictsServerMacrosAndContainsExecutorFailures() {
        Macro macro = new Macro("server", "example.org", List.of(MacroAction.chat("hello")));
        MacroRunner runner = new MacroRunner();
        RecordingExecutor executor = new RecordingExecutor();

        runner.start(macro, Optional.of("example.org"));
        assertEquals(MacroRunner.TickStatus.CANCELLED_CONTEXT,
                runner.validateServerContext(Optional.of("other.example")).status());
        assertFalse(runner.isRunning());

        runner.start(new Macro("failure", Macro.GLOBAL, List.of(MacroAction.chat("hello"))), Optional.empty());
        executor.fail = true;
        assertEquals(MacroRunner.TickStatus.FAILED, runner.tick(Optional.empty(), executor).status());
        assertFalse(runner.isRunning());

        assertThrows(IllegalArgumentException.class, () -> runner.start(macro, Optional.empty()));
    }

    private static final class RecordingExecutor implements MacroActionExecutor {
        private final List<String> actions = new ArrayList<>();
        private boolean fail;

        @Override
        public void executeLocalCommand(String command) {
            execute("local:" + command);
        }

        @Override
        public void sendChat(String message) {
            execute("chat:" + message);
        }

        @Override
        public void sendMinecraftCommand(String command) {
            execute("command:" + command);
        }

        private void execute(String action) {
            if (fail) {
                throw new IllegalStateException("intentional failure");
            }
            actions.add(action);
        }
    }
}
