package dev.helikon.client.chat;

import java.util.Objects;

/** Bounded consecutive-message counter used by the local BetterChat display adapter. */
public final class ChatDuplicateTracker {
    private String previousIdentity;
    private int count;

    public Decision record(String identity, boolean stackDuplicates, boolean showCounter) {
        String checked = Objects.requireNonNull(identity, "identity");
        if (checked.equals(previousIdentity)) {
            count = Math.min(count + 1, 9_999);
        } else {
            previousIdentity = checked;
            count = 1;
        }
        return new Decision(count, stackDuplicates && count > 1, showCounter && count > 1);
    }

    public void reset() {
        previousIdentity = null;
        count = 0;
    }

    public record Decision(int count, boolean collapsePrevious, boolean appendCounter) {
    }
}
