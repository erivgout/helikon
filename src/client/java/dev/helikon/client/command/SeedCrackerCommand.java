package dev.helikon.client.command;

import dev.helikon.client.module.world.SeedCracker;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Local command surface for reviewing and correcting SeedCracker evidence. */
public final class SeedCrackerCommand implements HelikonCommand {
    private final SeedCracker module;

    public SeedCrackerCommand(SeedCracker module) {
        this.module = Objects.requireNonNull(module, "module");
    }

    @Override
    public String name() {
        return "seedcracker";
    }

    @Override
    public String usage() {
        return ".seedcracker status|search|clear|candidates|addslime <chunkX> <chunkZ>"
                + "|removeslime <chunkX> <chunkZ>";
    }

    @Override
    public String description() {
        return "Manages local SeedCracker evidence and bounded candidate searches.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        if (arguments.isEmpty()) {
            feedback.error("Usage: " + usage());
            return;
        }
        try {
            switch (arguments.getFirst()) {
                case "status" -> status(arguments, feedback);
                case "search" -> search(arguments, feedback);
                case "clear" -> clear(arguments, feedback);
                case "candidates" -> candidates(arguments, feedback);
                case "addslime" -> addSlime(arguments, feedback);
                case "removeslime" -> removeSlime(arguments, feedback);
                default -> feedback.error("Usage: " + usage());
            }
        } catch (IllegalArgumentException exception) {
            feedback.error("SeedCracker action failed: " + exception.getMessage());
        }
    }

    private void status(List<String> arguments, CommandFeedback feedback) {
        requireSize(arguments, 1, ".seedcracker status");
        module.statusLines().forEach(feedback::info);
    }

    private void search(List<String> arguments, CommandFeedback feedback) {
        requireSize(arguments, 1, ".seedcracker search");
        if (!module.requestSearch()) {
            feedback.error("Enable SeedCracker and confirm enough slime chunks with valid search settings.");
            return;
        }
        feedback.info("SeedCracker started the configured bounded candidate search.");
    }

    private void clear(List<String> arguments, CommandFeedback feedback) {
        requireSize(arguments, 1, ".seedcracker clear");
        module.clearSession();
        feedback.info("Cleared session-local SeedCracker evidence and results.");
    }

    private void candidates(List<String> arguments, CommandFeedback feedback) {
        requireSize(arguments, 1, ".seedcracker candidates");
        SeedCracker.Snapshot snapshot = module.snapshot();
        if (snapshot.candidates().isEmpty()) {
            feedback.info("SeedCracker has no retained candidates.");
            return;
        }
        feedback.info("Structure-seed candidates: " + snapshot.candidates().stream()
                .map(SeedCracker::formatStructureSeed)
                .collect(Collectors.joining(", ")));
        if (snapshot.matchingCandidates() > snapshot.candidates().size()) {
            feedback.info((snapshot.matchingCandidates() - snapshot.candidates().size())
                    + " additional match(es) were not retained due to Maximum results.");
        }
    }

    private void addSlime(List<String> arguments, CommandFeedback feedback) {
        requireSize(arguments, 3, ".seedcracker addslime <chunkX> <chunkZ>");
        int chunkX = parseChunk(arguments.get(1));
        int chunkZ = parseChunk(arguments.get(2));
        if (!module.addManualObservation(chunkX, chunkZ)) {
            feedback.error("That slime chunk is already confirmed.");
            return;
        }
        feedback.info("Added manual slime-chunk evidence at " + chunkX + ", " + chunkZ + ".");
    }

    private void removeSlime(List<String> arguments, CommandFeedback feedback) {
        requireSize(arguments, 3, ".seedcracker removeslime <chunkX> <chunkZ>");
        int chunkX = parseChunk(arguments.get(1));
        int chunkZ = parseChunk(arguments.get(2));
        if (!module.removeObservation(chunkX, chunkZ)) {
            feedback.error("No confirmed slime evidence exists at that chunk.");
            return;
        }
        feedback.info("Removed slime-chunk evidence at " + chunkX + ", " + chunkZ + ".");
    }

    private static int parseChunk(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Chunk coordinates must be whole numbers");
        }
    }

    private static void requireSize(List<String> arguments, int expected, String usage) {
        if (arguments.size() != expected) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }
}
