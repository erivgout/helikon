package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/**
 * Client-only brightness controls. Gamma and Night Vision are independently
 * selectable and all state changed by the module is restored on disable.
 */
public final class Fullbright extends Module {
    private final FullbrightGammaController.GammaAccess gamma;
    private final NightVisionAccess nightVision;
    private final FullbrightGammaController gammaController = new FullbrightGammaController();
    private final BooleanSetting gammaMode;
    private final BooleanSetting nightVisionMode;
    private final NumberSetting brightness;
    private boolean nightVisionApplied;

    public Fullbright(FullbrightGammaController.GammaAccess gamma, NightVisionAccess nightVision) {
        super(
                "fullbright",
                "Fullbright",
                "Locally increases brightness with reversible gamma or Night Vision.",
                ModuleCategory.RENDER,
                false,
                Keybind.unbound()
        );
        this.gamma = Objects.requireNonNull(gamma, "gamma");
        this.nightVision = Objects.requireNonNull(nightVision, "nightVision");
        gammaMode = addSetting(new BooleanSetting(
                "use_gamma",
                "Gamma mode",
                "Use Minecraft's local gamma option at the selected brightness level.",
                true
        ));
        nightVisionMode = addSetting(new BooleanSetting(
                "night_vision",
                "Night Vision mode",
                "Apply a local visual Night Vision effect while Fullbright is enabled.",
                false
        ));
        brightness = addSetting(new NumberSetting(
                "brightness",
                "Brightness",
                "Local gamma level from 0.0 (dark) to 1.0 (bright).",
                1.0,
                0.0,
                1.0
        ));

        gammaMode.addChangeListener(ignored -> reconcile());
        nightVisionMode.addChangeListener(ignored -> reconcile());
        brightness.addChangeListener(ignored -> reconcile());
    }

    public BooleanSetting gammaMode() {
        return gammaMode;
    }

    public BooleanSetting nightVisionMode() {
        return nightVisionMode;
    }

    public NumberSetting brightness() {
        return brightness;
    }

    /** Reasserts the local Night Vision visual after a server effect update. */
    public void tick() {
        if (isEnabled() && nightVisionMode.value()) {
            nightVision.apply();
            nightVisionApplied = true;
        }
    }

    @Override
    protected void onEnable() {
        reconcile();
    }

    @Override
    protected void onDisable() {
        gammaController.restore(gamma);
        restoreNightVision();
    }

    private void reconcile() {
        if (!isEnabled()) {
            return;
        }

        gammaController.reconcile(gamma, gammaMode.value(), brightness.value());
        if (nightVisionMode.value()) {
            nightVision.apply();
            nightVisionApplied = true;
        } else {
            restoreNightVision();
        }
    }

    private void restoreNightVision() {
        if (!nightVisionApplied) {
            return;
        }
        nightVision.restore();
        nightVisionApplied = false;
    }

    /** Thin platform port for client-local Night Vision state. */
    public interface NightVisionAccess {
        void apply();

        void restore();
    }
}
