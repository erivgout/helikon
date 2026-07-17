package dev.helikon.client.module.chat;

/** Closed set of locally observed moments that an Announcer message may describe. */
public enum AnnouncementTrigger {
    DEATH("death"),
    KILL("kill"),
    ITEM_PICKUP("item pickup"),
    DISTANCE_TRAVELED("distance traveled"),
    BLOCK_MINED("block mined"),
    DIMENSION_CHANGE("dimension change"),
    JOIN("join"),
    LEAVE("leave"),
    ADVANCEMENT("advancement"),
    LOW_HEALTH("low health"),
    TOTEM_USE("totem use");

    private final String displayName;

    AnnouncementTrigger(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
