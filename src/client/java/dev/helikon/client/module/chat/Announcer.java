package dev.helikon.client.module.chat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Produces capped ordinary-chat announcements from already observed local client facts. */
public final class Announcer extends Module {
    private static final int MAXIMUM_CHAT_LENGTH = 256;

    private final Map<AnnouncementTrigger, BooleanSetting> triggers = new EnumMap<>(AnnouncementTrigger.class);
    private final NumberSetting distanceBlocks;
    private final NumberSetting lowHealthThreshold;
    private final NumberSetting minimumDelaySeconds;
    private final NumberSetting sessionMessageCap;
    private final BooleanSetting pauseInGui;
    private final StringSetting messageTemplate;
    private long lastAnnouncementMillis = Long.MIN_VALUE;
    private int sentThisSession;

    public Announcer() {
        super("announcer", "Announcer", "Sends capped ordinary chat for selected local gameplay moments.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        for (AnnouncementTrigger trigger : AnnouncementTrigger.values()) {
            triggers.put(trigger, addSetting(new BooleanSetting(settingId(trigger), settingName(trigger),
                    "Allow ordinary local chat for this trigger.", false)));
        }
        distanceBlocks = addSetting(new NumberSetting("distance_blocks", "Distance interval",
                "Local blocks traveled before one distance announcement.", 100.0D, 10.0D, 10_000.0D));
        lowHealthThreshold = addSetting(new NumberSetting("low_health_threshold", "Low health threshold",
                "Announce only when local health crosses down through this value.", 6.0D, 1.0D, 20.0D));
        minimumDelaySeconds = addSetting(new NumberSetting("minimum_delay_seconds", "Minimum delay",
                "Minimum local seconds between any two automatic announcements.", 10.0D, 1.0D, 300.0D));
        sessionMessageCap = addSetting(new NumberSetting("session_message_cap", "Session message cap",
                "Maximum automatic announcements until the client restarts.", 20.0D, 1.0D, 100.0D));
        pauseInGui = addSetting(new BooleanSetting("pause_in_gui", "Pause in GUI",
                "Do not send automatic announcements while any screen is open.", true));
        messageTemplate = addSetting(new StringSetting("message_template", "Message template",
                "Ordinary chat template using {event} and {detail}.", "{event}: {detail}", MAXIMUM_CHAT_LENGTH, false));
    }

    /** Returns one safe ordinary chat line only when the complete local policy permits it. */
    public Optional<String> messageFor(AnnouncementTrigger trigger, String detail, long nowMillis, boolean screenOpen) {
        Objects.requireNonNull(trigger, "trigger");
        if (!isEnabled() || !triggers.get(trigger).value() || (screenOpen && pauseInGui.value())
                || nowMillis < 0L || sentThisSession >= sessionMessageCap()) {
            return Optional.empty();
        }
        if (lastAnnouncementMillis != Long.MIN_VALUE
                && nowMillis - lastAnnouncementMillis < minimumDelayMillis()) {
            return Optional.empty();
        }
        String message = render(trigger, detail);
        if (!isSafeOrdinaryChat(message)) {
            return Optional.empty();
        }
        lastAnnouncementMillis = nowMillis;
        sentThisSession++;
        return Optional.of(message);
    }

    public boolean triggerEnabled(AnnouncementTrigger trigger) {
        return triggers.get(Objects.requireNonNull(trigger, "trigger")).value();
    }

    public int distanceBlocks() {
        return (int) Math.round(distanceBlocks.value());
    }

    public float lowHealthThreshold() {
        return lowHealthThreshold.value().floatValue();
    }

    public int sentThisSession() {
        return sentThisSession;
    }

    private int sessionMessageCap() {
        return (int) Math.round(sessionMessageCap.value());
    }

    private long minimumDelayMillis() {
        return Math.round(minimumDelaySeconds.value() * 1_000.0D);
    }

    private String render(AnnouncementTrigger trigger, String detail) {
        String safeDetail = detail == null ? "" : detail.replace('\r', ' ').replace('\n', ' ').trim();
        return messageTemplate.value().replace("{event}", trigger.displayName()).replace("{detail}", safeDetail).trim();
    }

    private static boolean isSafeOrdinaryChat(String value) {
        return !value.isEmpty() && value.length() <= MAXIMUM_CHAT_LENGTH
                && value.indexOf('\r') < 0 && value.indexOf('\n') < 0
                && !value.startsWith(".") && !value.startsWith("/");
    }

    private static String settingId(AnnouncementTrigger trigger) {
        return switch (trigger) {
            case DEATH -> "death";
            case KILL -> "kill";
            case ITEM_PICKUP -> "item_pickup";
            case DISTANCE_TRAVELED -> "distance_traveled";
            case BLOCK_MINED -> "block_mined";
            case DIMENSION_CHANGE -> "dimension_change";
            case JOIN -> "join";
            case LEAVE -> "leave";
            case ADVANCEMENT -> "advancement";
            case LOW_HEALTH -> "low_health";
            case TOTEM_USE -> "totem_use";
        };
    }

    private static String settingName(AnnouncementTrigger trigger) {
        return switch (trigger) {
            case DEATH -> "Death";
            case KILL -> "Kill";
            case ITEM_PICKUP -> "Item pickup";
            case DISTANCE_TRAVELED -> "Distance traveled";
            case BLOCK_MINED -> "Block mined";
            case DIMENSION_CHANGE -> "Dimension change";
            case JOIN -> "Join";
            case LEAVE -> "Leave";
            case ADVANCEMENT -> "Advancement";
            case LOW_HEALTH -> "Low health";
            case TOTEM_USE -> "Totem use";
        };
    }
}
