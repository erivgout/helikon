package dev.helikon.client.command;

import java.util.Objects;
import java.util.function.Consumer;

/** Defers a local chat draft until Minecraft has completed the current chat-command callback. */
public final class ScheduledChatInputReopener implements ChatInputReopener {
    private final ChatInputReopener delegate;
    private final Consumer<Runnable> scheduler;

    public ScheduledChatInputReopener(ChatInputReopener delegate, Consumer<Runnable> scheduler) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void reopen(String text) {
        String requestedText = Objects.requireNonNull(text, "text");
        scheduler.accept(() -> delegate.reopen(requestedText));
    }
}
