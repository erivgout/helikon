package dev.helikon.client.module.player;

/** Drinks an existing milk bucket when harmful effects are locally observed. */
public final class AntiPotion extends HotbarUseModule {
    public AntiPotion() {
        super("anti_potion", "AntiPotion", "Drinks an existing hotbar milk bucket when a harmful effect is active.", 40);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.milk();
    }
}
