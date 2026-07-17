package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinBlinkerTest {
    @Test
    void alternatesAllLocalLayersAndRestoresTheirOriginalValuesOnDisable() {
        FakeSkinLayers layers = new FakeSkinLayers(true);
        layers.setEnabled(SkinLayer.CAPE, false);
        SkinBlinker blinker = enabledModule(layers);

        blinker.tick(0L, true, false);
        assertAll(layers, false);
        blinker.tick(7L, true, false);
        assertAll(layers, false);
        blinker.tick(8L, true, false);
        assertFalse(layers.isEnabled(SkinLayer.CAPE));
        assertTrue(layers.isEnabled(SkinLayer.HAT));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(blinker);
        registry.setEnabled(blinker, false);
        assertFalse(layers.isEnabled(SkinLayer.CAPE));
        assertTrue(layers.isEnabled(SkinLayer.HAT));
    }

    @Test
    void restoresOnWorldExitAndDoesNotOverwriteAUserChange() {
        FakeSkinLayers layers = new FakeSkinLayers(true);
        SkinBlinker blinker = enabledModule(layers);
        blinker.tick(0L, true, false);
        assertAll(layers, false);

        layers.setEnabled(SkinLayer.HAT, true);
        blinker.tick(1L, false, false);
        assertTrue(layers.isEnabled(SkinLayer.HAT));
        assertTrue(layers.isEnabled(SkinLayer.CAPE));
    }

    @Test
    void restoresAndResnapshotsUserLayerChangesMadeWhileAScreenIsOpen() {
        FakeSkinLayers layers = new FakeSkinLayers(true);
        SkinBlinker blinker = enabledModule(layers);
        blinker.tick(0L, true, false);
        assertAll(layers, false);

        blinker.tick(1L, true, true);
        assertAll(layers, true);
        layers.setEnabled(SkinLayer.HAT, false);
        blinker.tick(2L, true, false);

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(blinker);
        registry.setEnabled(blinker, false);
        assertFalse(layers.isEnabled(SkinLayer.HAT));
        assertTrue(layers.isEnabled(SkinLayer.CAPE));
    }

    private static SkinBlinker enabledModule(SkinLayerAccess layers) {
        SkinBlinker blinker = new SkinBlinker(layers);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(blinker);
        registry.setEnabled(blinker, true);
        return blinker;
    }

    private static void assertAll(FakeSkinLayers layers, boolean expected) {
        for (SkinLayer layer : SkinLayer.values()) {
            if (expected) {
                assertTrue(layers.isEnabled(layer), layer.name());
            } else {
                assertFalse(layers.isEnabled(layer), layer.name());
            }
        }
    }

    private static final class FakeSkinLayers implements SkinLayerAccess {
        private final Map<SkinLayer, Boolean> values = new EnumMap<>(SkinLayer.class);

        private FakeSkinLayers(boolean initialValue) {
            for (SkinLayer layer : SkinLayer.values()) {
                values.put(layer, initialValue);
            }
        }

        @Override
        public boolean isEnabled(SkinLayer layer) {
            return values.get(layer);
        }

        @Override
        public void setEnabled(SkinLayer layer, boolean enabled) {
            values.put(layer, enabled);
        }
    }
}
