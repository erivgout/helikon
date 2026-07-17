package dev.helikon.client.module.chat;

import dev.helikon.client.chat.AnnouncerObservationTracker;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Narrow queued bridge from verified client hooks to the Announcer's Minecraft-free message policy. */
public final class AnnouncerAccess {
    private static final long KILL_CANDIDATE_EXPIRY_MILLIS = 30_000L;
    static final int MAXIMUM_PENDING_OBSERVATIONS = 128;
    static final int MAXIMUM_REMEMBERED_ADVANCEMENTS = 512;
    private static final ConcurrentLinkedQueue<AnnouncerObservationTracker.Observation> pending = new ConcurrentLinkedQueue<>();
    private static final java.util.concurrent.atomic.AtomicInteger pendingCount = new java.util.concurrent.atomic.AtomicInteger();
    private static final Set<String> announcedAdvancements = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<String> advancementOrder = new ConcurrentLinkedQueue<>();
    private static final AnnouncerObservationTracker observations = new AnnouncerObservationTracker();
    private static volatile Announcer announcer;
    private static volatile ChatSpammer.ChatSender sender;
    private static KillCandidate lastAttack;

    private AnnouncerAccess() {
    }

    public static void install(Announcer announcerModule, ChatSpammer.ChatSender chatSender) {
        announcer = Objects.requireNonNull(announcerModule, "announcerModule");
        sender = Objects.requireNonNull(chatSender, "chatSender");
    }

    /** Receives one already-observed local trigger; sending is deferred to the normal client tick. */
    public static void enqueue(AnnouncementTrigger trigger, String detail) {
        Announcer module = announcer;
        if (module != null && module.isEnabled() && module.triggerEnabled(trigger) && reservePendingSlot()) {
            pending.add(new AnnouncerObservationTracker.Observation(trigger, detail == null ? "" : detail));
        }
    }

    /** Retains one completed advancement identifier per connected local world to avoid repeat progress packets. */
    public static void observeAdvancement(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        Announcer module = announcer;
        if (module != null && module.isEnabled() && module.triggerEnabled(AnnouncementTrigger.ADVANCEMENT)
                && !value.isEmpty() && announcedAdvancements.add(value)) {
            advancementOrder.add(value);
            while (announcedAdvancements.size() > MAXIMUM_REMEMBERED_ADVANCEMENTS) {
                String oldest = advancementOrder.poll();
                if (oldest == null) {
                    break;
                }
                announcedAdvancements.remove(oldest);
            }
            enqueue(AnnouncementTrigger.ADVANCEMENT, value);
        }
    }

    /** Records a direct local melee attempt; only a subsequent locally observed death can create a kill trigger. */
    public static void recordAttack(UUID entityId, String detail, long nowMillis) {
        if (entityId == null || nowMillis < 0L || announcer == null || !announcer.isEnabled()) {
            return;
        }
        lastAttack = new KillCandidate(entityId, detail == null ? "" : detail, nowMillis);
    }

    /** Confirms the current kill candidate only when its entity later unloads in a dead state. */
    public static void observeEntityUnload(UUID entityId, boolean deadOrDying, long nowMillis) {
        KillCandidate candidate = lastAttack;
        if (candidate == null || nowMillis < 0L || nowMillis - candidate.observedAtMillis() > KILL_CANDIDATE_EXPIRY_MILLIS) {
            lastAttack = null;
            return;
        }
        if (candidate.entityId().equals(entityId) && deadOrDying) {
            enqueue(AnnouncementTrigger.KILL, candidate.detail());
            lastAttack = null;
        }
    }

    /** Drains source hooks and local player observations through the normal module failure-isolation boundary. */
    public static void tick(AnnouncerObservationTracker.Fact fact, boolean screenOpen, long nowMillis) {
        Announcer module = announcer;
        ChatSpammer.ChatSender chatSender = sender;
        if (module == null || chatSender == null || !module.isEnabled()) {
            reset();
            return;
        }
        for (AnnouncerObservationTracker.Observation observation : observations.observe(fact, module.distanceBlocks(),
                module.lowHealthThreshold())) {
            sendIfAllowed(module, chatSender, observation, screenOpen, nowMillis);
        }
        AnnouncerObservationTracker.Observation observation;
        while ((observation = pending.poll()) != null) {
            pendingCount.decrementAndGet();
            sendIfAllowed(module, chatSender, observation, screenOpen, nowMillis);
        }
    }

    public static void reset() {
        pending.clear();
        pendingCount.set(0);
        announcedAdvancements.clear();
        advancementOrder.clear();
        observations.reset();
        lastAttack = null;
    }

    static int pendingObservationCount() {
        return pendingCount.get();
    }

    static int rememberedAdvancementCount() {
        return announcedAdvancements.size();
    }

    private static void sendIfAllowed(Announcer module, ChatSpammer.ChatSender chatSender,
                                      AnnouncerObservationTracker.Observation observation,
                                      boolean screenOpen, long nowMillis) {
        module.messageFor(observation.trigger(), observation.detail(), nowMillis, screenOpen).ifPresent(chatSender::send);
    }

    private static boolean reservePendingSlot() {
        while (true) {
            int observed = pendingCount.get();
            if (observed >= MAXIMUM_PENDING_OBSERVATIONS) {
                return false;
            }
            if (pendingCount.compareAndSet(observed, observed + 1)) {
                return true;
            }
        }
    }

    private record KillCandidate(UUID entityId, String detail, long observedAtMillis) {
    }
}
