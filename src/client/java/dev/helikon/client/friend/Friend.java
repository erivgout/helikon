package dev.helikon.client.friend;

/** A local player-name friendship entry and its render color. */
public record Friend(String name, int color) {
    public static final int DEFAULT_COLOR = 0xFF55FFFF;
}
