package dev.helikon.client.module.player;

/** Requests a bounded harmful splash potion in Creative mode. */
public final class KillPotion extends CreativeItemModule {
    public KillPotion() {
        super("kill_potion", "KillPotion",
                "Adds a bounded high-damage splash potion to the selected Creative slot.");
    }

    @Override
    protected Request request() {
        return new Request(Kind.KILL_POTION, "minecraft:splash_potion", 1, "Helikon Damage Potion");
    }
}
