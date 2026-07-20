package dev.helikon.client.command;

import dev.helikon.client.module.world.SeedCracker;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedCrackerCommandTest {
    @Test
    void managesEvidenceStatusAndManualSearchLocally() {
        SeedCracker module = new SeedCracker();
        ((IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("minimum_observations"))
                .findFirst().orElseThrow()).set(1);
        module.enable();
        SeedCrackerCommand command = new SeedCrackerCommand(module);
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(List.of("addslime", "4", "-8"), feedback);
        command.execute(List.of("status"), feedback);
        command.execute(List.of("search"), feedback);
        command.execute(List.of("candidates"), feedback);
        command.execute(List.of("removeslime", "4", "-8"), feedback);
        command.execute(List.of("clear"), feedback);

        assertTrue(feedback.info.stream().anyMatch(line -> line.contains("Added manual")));
        assertTrue(feedback.info.stream().anyMatch(line -> line.startsWith("SeedCracker:")));
        assertTrue(feedback.info.stream().anyMatch(line -> line.contains("started")));
        assertTrue(module.observations().isEmpty());
    }

    @Test
    void rejectsInvalidSubcommandsAndChunkCoordinates() {
        SeedCrackerCommand command = new SeedCrackerCommand(new SeedCracker());
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(List.of(), feedback);
        command.execute(List.of("addslime", "x", "2"), feedback);

        assertEquals(2, feedback.error.size());
    }

    private static final class RecordingFeedback implements CommandFeedback {
        private final List<String> info = new ArrayList<>();
        private final List<String> error = new ArrayList<>();

        @Override
        public void info(String message) {
            info.add(message);
        }

        @Override
        public void error(String message) {
            error.add(message);
        }
    }
}
