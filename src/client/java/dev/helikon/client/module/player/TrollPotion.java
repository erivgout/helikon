package dev.helikon.client.module.player;

/** Requests a bounded disruptive-effect splash potion in Creative mode. */
public final class TrollPotion extends CreativeItemModule {
    public TrollPotion() {
        super("troll_potion", "TrollPotion",
                "Adds a bounded nausea, slowness, and glowing splash potion to the selected Creative slot.");
    }

    @Override
    protected Request request() {
        return new Request(Kind.TROLL_POTION, "minecraft:splash_potion", 1, "Helikon Dizzy Potion");
    }
}
