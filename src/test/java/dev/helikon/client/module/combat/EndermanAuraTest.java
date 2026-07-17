package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndermanAuraTest {
    @Test
    void defaultsOffAndHasCombatIdentity() {
        EndermanAura aura = new EndermanAura();
        assertEquals("enderman_aura", aura.id());
        assertEquals(ModuleCategory.COMBAT, aura.category());
        assertFalse(aura.defaultEnabled());
        assertTrue(aura.choose(0L, List.of(threat(false, false)), List.of(destination(true, true, true))).isEmpty());
    }

    @Test
    void choosesSafestDestinationAndExcludesFriendlyProjectiles() {
        EndermanAura aura = enabled();
        assertTrue(aura.choose(0L, List.of(threat(false, true)),
                List.of(destination(true, true, true))).isEmpty());

        EndermanAura.Destination unsafe = new EndermanAura.Destination(
                2.0D, 64.0D, 0.0D, 6.0D, 20.0D, true, false, true);
        EndermanAura.Destination safe = destination(true, true, true);
        assertEquals(safe, aura.choose(0L, List.of(threat(false, false)),
                List.of(unsafe, safe)).orElseThrow().destination());
    }

    @Test
    void cooldownAndDisableCleanupAreDeterministic() {
        EndermanAura aura = enabled();
        List<EndermanAura.Threat> threats = List.of(threat(false, false));
        List<EndermanAura.Destination> destinations = List.of(destination(true, true, true));
        assertTrue(aura.choose(10L, threats, destinations).isPresent());
        aura.markTeleported(10L);
        assertTrue(aura.choose(11L, threats, destinations).isEmpty());
        aura.disable();
        aura.enable();
        assertTrue(aura.choose(0L, threats, destinations).isPresent());
        IntegerSetting cooldown = (IntegerSetting) aura.settings().stream()
                .filter(setting -> setting.id().equals("cooldown_ticks")).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> cooldown.set(201));
    }

    private static EndermanAura enabled() {
        EndermanAura aura = new EndermanAura();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(aura);
        registry.setEnabled(aura, true);
        return aura;
    }

    private static EndermanAura.Threat threat(boolean self, boolean friend) {
        return new EndermanAura.Threat(1, self, friend, 5.0D, 0.5D, 12.0D);
    }

    private static EndermanAura.Destination destination(boolean loaded, boolean collisionFree, boolean safeFloor) {
        return new EndermanAura.Destination(6.0D, 64.0D, 0.0D, 6.0D, 12.0D,
                loaded, collisionFree, safeFloor);
    }
}
