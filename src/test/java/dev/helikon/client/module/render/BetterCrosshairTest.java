package dev.helikon.client.module.render;

import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.ColorSettingText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterCrosshairTest {
    @Test
    void hideVanillaOptionOnlyAppliesWhileTheModuleIsEnabled() {
        BetterCrosshair crosshair = new BetterCrosshair();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(crosshair);

        assertFalse(crosshair.hidesVanillaCrosshair());
        registry.setEnabled(crosshair, true);
        assertTrue(crosshair.hidesVanillaCrosshair());

        ColorSetting color = (ColorSetting) crosshair.settings().stream()
                .filter(setting -> setting.id().equals("color"))
                .findFirst()
                .orElseThrow();
        assertTrue(ColorSettingText.tryApply(color, "#80FF6600"));
        assertEquals(0x80FF6600, crosshair.color());
    }

    @Test
    void hidingTheCustomHudRestoresTheVanillaCrosshair() {
        BetterCrosshair crosshair = new BetterCrosshair();
        HudLayout layout = new HudLayout();
        PanicState panic = new PanicState();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(crosshair);
        RenderModuleAccess.install(new AntiBlind(), new NoFireOverlay(), crosshair, new AntiTotemAnimation(), new Dinnerbone(),
                new RainbowEnchant(), layout, panic);

        registry.setEnabled(crosshair, true);
        assertTrue(RenderModuleAccess.hideVanillaCrosshair());

        layout.element(HudElementId.BETTER_CROSSHAIR).setEnabled(false);
        assertFalse(RenderModuleAccess.hideVanillaCrosshair());

        layout.element(HudElementId.BETTER_CROSSHAIR).setEnabled(true);
        panic.hideCustomHud();
        assertFalse(RenderModuleAccess.hideVanillaCrosshair());
    }
}
