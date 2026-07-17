package dev.helikon.client.input;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindManagerTest {
    private static final int KEY = 82;

    private static TestModule module(Keybind.Activation activation) {
        return new TestModule(new Keybind(KEY, activation));
    }

    private record Fixture(ModuleRegistry registry, KeybindManager manager, Set<Integer> downKeys) {
        static Fixture of(Module module) {
            ModuleRegistry registry = new ModuleRegistry();
            registry.register(module);
            return new Fixture(registry, new KeybindManager(registry), new HashSet<>());
        }

        void tick(boolean suppressed) {
            manager.tick(downKeys::contains, suppressed);
        }
    }

    private record MouseFixture(ModuleRegistry registry, KeybindManager manager, Set<Integer> downKeys,
                                Set<Integer> downButtons) {
        static MouseFixture of(Module module) {
            ModuleRegistry registry = new ModuleRegistry();
            registry.register(module);
            return new MouseFixture(registry, new KeybindManager(registry), new HashSet<>(), new HashSet<>());
        }

        void tick(boolean suppressed) {
            manager.tick(new KeybindManager.KeyStateReader() {
                @Override
                public boolean isKeyDown(int keyCode) {
                    return downKeys.contains(keyCode);
                }

                @Override
                public boolean isMouseButtonDown(int button) {
                    return downButtons.contains(button);
                }
            }, suppressed);
        }
    }

    @Test
    void togglePressEdgeTogglesOnceWhileHeld() {
        TestModule module = module(Keybind.Activation.TOGGLE);
        Fixture fixture = Fixture.of(module);

        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertTrue(module.isEnabled());

        fixture.tick(false);
        assertTrue(module.isEnabled(), "holding the key must not re-toggle");

        fixture.downKeys().remove(KEY);
        fixture.tick(false);
        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertFalse(module.isEnabled(), "a second press toggles back off");
    }

    @Test
    void holdEnablesWhileDownAndDisablesOnRelease() {
        TestModule module = module(Keybind.Activation.HOLD);
        Fixture fixture = Fixture.of(module);

        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertTrue(module.isEnabled());

        fixture.downKeys().remove(KEY);
        fixture.tick(false);
        assertFalse(module.isEnabled());
    }

    @Test
    void pressOnceOnlyEnables() {
        TestModule module = module(Keybind.Activation.PRESS_ONCE);
        Fixture fixture = Fixture.of(module);

        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertTrue(module.isEnabled());

        fixture.downKeys().remove(KEY);
        fixture.tick(false);
        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertTrue(module.isEnabled(), "press_once never disables");
    }

    @Test
    void suppressedInputIgnoresPressesAndReleasesHolds() {
        TestModule toggled = module(Keybind.Activation.TOGGLE);
        Fixture fixture = Fixture.of(toggled);

        fixture.downKeys().add(KEY);
        fixture.tick(true);
        assertFalse(toggled.isEnabled(), "presses while a screen is open are ignored");

        TestModule held = module(Keybind.Activation.HOLD);
        Fixture heldFixture = Fixture.of(held);
        heldFixture.downKeys().add(KEY);
        heldFixture.tick(false);
        assertTrue(held.isEnabled());

        heldFixture.tick(true);
        assertFalse(held.isEnabled(), "opening a screen releases HOLD modules");
    }

    @Test
    void keyHeldAcrossEndOfSuppressionIsNotANewPress() {
        TestModule module = module(Keybind.Activation.TOGGLE);
        Fixture fixture = Fixture.of(module);

        fixture.downKeys().add(KEY);
        fixture.tick(true);
        fixture.tick(false);
        assertFalse(module.isEnabled(), "a key already held while a screen closed must not fire");

        fixture.downKeys().remove(KEY);
        fixture.tick(false);
        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertTrue(module.isEnabled(), "a fresh press after release fires normally");
    }

    @Test
    void unboundModulesAreIgnored() {
        TestModule module = new TestModule(Keybind.unbound());
        Fixture fixture = Fixture.of(module);

        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertFalse(module.isEnabled());
    }

    @Test
    void inputConsumerKeepsItsBoundKeyOutOfNormalActivation() {
        InputConsumerModule module = new InputConsumerModule(new Keybind(KEY, Keybind.Activation.TOGGLE));
        Fixture fixture = Fixture.of(module);

        fixture.downKeys().add(KEY);
        fixture.tick(false);
        assertFalse(module.isEnabled());

        fixture.registry().setEnabled(module, true);
        fixture.tick(false);
        fixture.downKeys().remove(KEY);
        fixture.tick(false);
        assertTrue(module.isEnabled());
    }

    @Test
    void mouseBindRequiresEveryConfiguredModifier() {
        TestModule module = new TestModule(new Keybind(Keybind.InputType.MOUSE_BUTTON, GLFW.GLFW_MOUSE_BUTTON_4,
                Set.of(Keybind.Modifier.CONTROL), Keybind.Activation.TOGGLE));
        MouseFixture fixture = MouseFixture.of(module);

        fixture.downButtons().add(GLFW.GLFW_MOUSE_BUTTON_4);
        fixture.tick(false);
        assertFalse(module.isEnabled());

        fixture.downKeys().add(GLFW.GLFW_KEY_LEFT_CONTROL);
        fixture.tick(false);
        assertTrue(module.isEnabled());
    }

    private static final class TestModule extends Module {
        private TestModule(Keybind keybind) {
            super("bound", "Bound", "Used by keybind tests.", ModuleCategory.MISCELLANEOUS, false, keybind);
        }
    }

    private static final class InputConsumerModule extends Module implements KeybindInputConsumer {
        private InputConsumerModule(Keybind keybind) {
            super("input_consumer", "Input consumer", "Reserves its bound key.",
                    ModuleCategory.MISCELLANEOUS, false, keybind);
        }

        @Override
        public boolean consumesKeybindInput() {
            return true;
        }
    }
}
