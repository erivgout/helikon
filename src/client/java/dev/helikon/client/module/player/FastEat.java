package dev.helikon.client.module.player;

import dev.helikon.client.setting.IntegerSetting;

/** Repeatedly requests ordinary food use and optionally releases early on permissive servers. */
public final class FastEat extends HotbarUseModule {
    private final IntegerSetting releaseTicks;

    public FastEat() {
        super("fast_eat", "FastEat", "Uses existing hotbar food and attempts an early vanilla release.", 4);
        releaseTicks = addSetting(new IntegerSetting("release_ticks", "Release ticks",
                "Release the use action after this many local use ticks.", 12, 4, 32));
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.food();
    }

    public boolean shouldRelease(int useTicks) {
        return isEnabled() && useTicks >= releaseTicks.value();
    }
}
