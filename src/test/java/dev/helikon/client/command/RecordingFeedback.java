package dev.helikon.client.command;

import java.util.ArrayList;
import java.util.List;

/** Captures command output for assertions. */
final class RecordingFeedback implements CommandFeedback {
    final List<String> infos = new ArrayList<>();
    final List<String> errors = new ArrayList<>();

    @Override
    public void info(String message) {
        infos.add(message);
    }

    @Override
    public void error(String message) {
        errors.add(message);
    }

    List<String> all() {
        List<String> all = new ArrayList<>(infos);
        all.addAll(errors);
        return all;
    }
}
