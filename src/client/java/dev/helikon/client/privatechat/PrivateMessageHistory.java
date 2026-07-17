package dev.helikon.client.privatechat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded in-memory recent conversation tabs; intentionally not persisted. */
public final class PrivateMessageHistory {
    public static final int MAX_CONVERSATIONS = 100;

    private final Map<String, MutableConversation> conversations = new LinkedHashMap<>();
    private long sequence;

    public void recordOutgoing(String participant, String text, int perConversationLimit) {
        record(participant, text, PrivateConversation.Direction.OUTGOING, perConversationLimit);
    }

    public void recordIncoming(String participant, String text, int perConversationLimit) {
        record(participant, text, PrivateConversation.Direction.INCOMING, perConversationLimit);
    }

    public List<PrivateConversation> tabs() {
        return conversations.values().stream()
                .map(MutableConversation::snapshot)
                .sorted(Comparator.comparingLong(this::latestSequence).reversed())
                .toList();
    }

    public List<PrivateConversation.Entry> entries(String participant) {
        MutableConversation conversation = conversations.get(
                PrivateConversation.normalizeParticipant(participant).toLowerCase(java.util.Locale.ROOT)
        );
        return conversation == null ? List.of() : conversation.snapshot().entries();
    }

    private void record(String participant, String text, PrivateConversation.Direction direction, int perConversationLimit) {
        if (perConversationLimit < 1 || perConversationLimit > 100) {
            throw new IllegalArgumentException("perConversationLimit must be 1 through 100");
        }
        String normalized = PrivateConversation.normalizeParticipant(participant);
        String key = normalized.toLowerCase(java.util.Locale.ROOT);
        MutableConversation conversation = conversations.get(key);
        if (conversation == null) {
            evictOldestConversationIfNeeded();
            conversation = new MutableConversation(normalized);
            conversations.put(key, conversation);
        }
        conversation.entries.add(new PrivateConversation.Entry(direction, text, sequence++));
        while (conversation.entries.size() > perConversationLimit) {
            conversation.entries.removeFirst();
        }
    }

    private long latestSequence(PrivateConversation conversation) {
        List<PrivateConversation.Entry> entries = conversation.entries();
        return entries.isEmpty() ? -1 : entries.getLast().sequence();
    }

    private void evictOldestConversationIfNeeded() {
        if (conversations.size() < MAX_CONVERSATIONS) {
            return;
        }
        String oldestKey = conversations.entrySet().stream()
                .min(Comparator.comparingLong(entry -> latestSequence(entry.getValue().snapshot())))
                .map(Map.Entry::getKey)
                .orElseThrow();
        conversations.remove(oldestKey);
    }

    private static final class MutableConversation {
        private final String participant;
        private final List<PrivateConversation.Entry> entries = new ArrayList<>();

        private MutableConversation(String participant) {
            this.participant = participant;
        }

        private PrivateConversation snapshot() {
            return new PrivateConversation(participant, entries);
        }
    }
}
