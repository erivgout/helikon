package dev.helikon.client.command;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure first-token completion for Helikon's locally intercepted commands. */
public final class CommandCompletion {
    private CommandCompletion() {
    }

    /**
     * Completes the command name immediately after the local prefix. It only
     * handles an unfinished first token, so normal server command completion
     * and user-entered command arguments retain their vanilla behavior.
     */
    public static Result complete(String input, int cursor, Collection<HelikonCommand> commands) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(commands, "commands");
        if (!input.startsWith(CommandDispatcher.PREFIX) || cursor < 1 || cursor > input.length()) {
            return Result.unchanged(input, cursor);
        }
        String beforeCursor = input.substring(0, cursor);
        if (beforeCursor.indexOf(' ') >= 0 || beforeCursor.indexOf('\t') >= 0) {
            return Result.unchanged(input, cursor);
        }
        String prefix = beforeCursor.substring(CommandDispatcher.PREFIX.length()).toLowerCase(Locale.ROOT);
        List<String> matches = commands.stream()
                .map(HelikonCommand::name)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
                .toList();
        if (matches.isEmpty()) {
            return Result.unchanged(input, cursor);
        }
        String completion = commonPrefix(matches);
        if (completion.length() <= prefix.length() && matches.size() > 1) {
            return Result.unchanged(input, cursor);
        }
        String replacement = CommandDispatcher.PREFIX + completion + input.substring(cursor);
        return new Result(replacement, CommandDispatcher.PREFIX.length() + completion.length(), true, List.copyOf(matches));
    }

    private static String commonPrefix(List<String> values) {
        String common = values.getFirst();
        for (int index = 1; index < values.size(); index++) {
            String next = values.get(index);
            int limit = Math.min(common.length(), next.length());
            int shared = 0;
            while (shared < limit && Character.toLowerCase(common.charAt(shared))
                    == Character.toLowerCase(next.charAt(shared))) {
                shared++;
            }
            common = common.substring(0, shared);
        }
        return common;
    }

    public record Result(String value, int cursor, boolean changed, List<String> matches) {
        private static Result unchanged(String value, int cursor) {
            return new Result(value, cursor, false, List.of());
        }
    }
}
