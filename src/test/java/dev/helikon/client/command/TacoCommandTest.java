package dev.helikon.client.command;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TacoCommandTest {
    @Test
    void staysLocalAndProducesBoundedOutput() {
        TacoCommand command = new TacoCommand();
        List<String> lines = new ArrayList<>();
        command.execute(List.of(), new CommandFeedback() {
            @Override
            public void info(String message) {
                lines.add(message);
            }

            @Override
            public void error(String message) {
                lines.add(message);
            }
        });
        assertEquals(3, lines.size());
        assertTrue(lines.stream().anyMatch(line -> line.contains("taco")));
    }
}
