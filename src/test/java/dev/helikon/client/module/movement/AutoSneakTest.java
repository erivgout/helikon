package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.KeybindManager;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoSneakTest {
    private static final MovementInput STANDING = new MovementInput(false, false, false, false, false, false, false);
    private static final MovementInput MOVING = new MovementInput(true, false, false, false, false, false, false);

    @Test
    void toggleModeAddsSneakOnlyWhileEnabledOutsideScreens() {
        AutoSneak autoSneak = enabledModule();

        assertEquals(withSneak(STANDING), autoSneak.apply(STANDING, false, false));
        assertEquals(STANDING, autoSneak.apply(STANDING, true, false));
    }

    @Test
    void holdModeReservesTheConfiguredModuleKeyAsInputOnly() {
        AutoSneak autoSneak = new AutoSneak();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoSneak);
        mode(autoSneak).set(AutoSneakMode.HOLD);
        autoSneak.setKeybind(new Keybind(82, Keybind.Activation.TOGGLE));
        registry.setEnabled(autoSneak, true);
        KeybindManager keybinds = new KeybindManager(registry);
        Set<Integer> downKeys = new HashSet<>();

        assertEquals(STANDING, autoSneak.apply(STANDING, false, false));
        downKeys.add(82);
        keybinds.tick(downKeys::contains, false);
        assertEquals(true, autoSneak.isEnabled());
        assertEquals(withSneak(STANDING), autoSneak.apply(STANDING, false, true));
        downKeys.clear();
        keybinds.tick(downKeys::contains, false);
        assertEquals(true, autoSneak.isEnabled());
        assertEquals(STANDING, autoSneak.apply(STANDING, false, false));
    }

    @Test
    void edgeOnlyModeAppliesSneakOnlyWhileMoving() {
        AutoSneak autoSneak = enabledModule();
        mode(autoSneak).set(AutoSneakMode.EDGE_ONLY);

        assertEquals(STANDING, autoSneak.apply(STANDING, false, false));
        assertEquals(withSneak(MOVING), autoSneak.apply(MOVING, false, false));
    }

    private static AutoSneak enabledModule() {
        AutoSneak autoSneak = new AutoSneak();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoSneak);
        registry.setEnabled(autoSneak, true);
        return autoSneak;
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<AutoSneakMode> mode(AutoSneak module) {
        return (EnumSetting<AutoSneakMode>) module.settings().stream()
                .filter(setting -> setting.id().equals("mode"))
                .findFirst()
                .orElseThrow();
    }

    private static MovementInput withSneak(MovementInput input) {
        return new MovementInput(input.forward(), input.backward(), input.left(), input.right(),
                input.jump(), true, input.sprint());
    }
}
