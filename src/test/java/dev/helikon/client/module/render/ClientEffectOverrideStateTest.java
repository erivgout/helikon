package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientEffectOverrideStateTest {
    @Test
    void restoresAnIndistinguishablePreexistingEffectByIdentity() {
        ClientEffectOverrideState<Effect> state = new ClientEffectOverrideState<>();
        Effect serverEffect = new Effect("infinite ambient hidden");
        Effect equallyShapedEffect = new Effect("infinite ambient hidden");

        assertEquals(serverEffect, equallyShapedEffect);
        Effect helikonEffect = state.apply(serverEffect, () -> new Effect("infinite ambient hidden"));
        assertNotSame(serverEffect, helikonEffect);

        ClientEffectOverrideState.Restoration<Effect> restoration = state.restore(helikonEffect);
        assertTrue(restoration.removeOverride());
        assertSame(serverEffect, restoration.original());
    }

    @Test
    void preservesTheLatestServerEffectAfterItReplacesTheOverride() {
        ClientEffectOverrideState<Effect> state = new ClientEffectOverrideState<>();
        Effect firstServerEffect = new Effect("old");
        Effect firstOverride = state.apply(firstServerEffect, () -> new Effect("helikon"));
        Effect latestServerEffect = new Effect("new");

        Effect secondOverride = state.apply(latestServerEffect, () -> new Effect("helikon"));
        ClientEffectOverrideState.Restoration<Effect> restoration = state.restore(secondOverride);

        assertNotSame(firstOverride, secondOverride);
        assertTrue(restoration.removeOverride());
        assertSame(latestServerEffect, restoration.original());
    }

    private record Effect(String appearance) {
    }
}
