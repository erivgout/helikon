package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Extends only local fog planes, without changing loaded terrain or server visibility. */
public final class NoFog extends Module {
    /** The six fog-plane distances emitted by Minecraft's local fog renderer. */
    public record FogPlanes(float environmentalStart, float renderDistanceStart, float environmentalEnd,
                            float renderDistanceEnd, float skyEnd, float cloudEnd) {
        public FogPlanes {
            requireFinite(environmentalStart, "environmentalStart");
            requireFinite(renderDistanceStart, "renderDistanceStart");
            requireFinite(environmentalEnd, "environmentalEnd");
            requireFinite(renderDistanceEnd, "renderDistanceEnd");
            requireFinite(skyEnd, "skyEnd");
            requireFinite(cloudEnd, "cloudEnd");
        }

        private static void requireFinite(float value, String name) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(name + " must be finite");
            }
        }
    }

    private final NumberSetting distance;

    public NoFog() {
        super("no_fog", "NoFog", "Extends local fog planes to reduce visible fog.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        distance = addSetting(new NumberSetting("distance", "Fog distance",
                "Minimum local fog distance in blocks; higher values reduce visible fog.", 1024.0D, 16.0D, 4096.0D));
    }

    /** Returns local-only fog planes that never bring a vanilla plane closer to the camera. */
    public FogPlanes extend(FogPlanes vanillaPlanes) {
        Objects.requireNonNull(vanillaPlanes, "vanillaPlanes");
        if (!isEnabled()) {
            return vanillaPlanes;
        }
        float minimumStart = distance.value().floatValue();
        float minimumEnd = minimumStart + 1.0F;
        return new FogPlanes(
                Math.max(vanillaPlanes.environmentalStart(), minimumStart),
                Math.max(vanillaPlanes.renderDistanceStart(), minimumStart),
                Math.max(vanillaPlanes.environmentalEnd(), minimumEnd),
                Math.max(vanillaPlanes.renderDistanceEnd(), minimumEnd),
                Math.max(vanillaPlanes.skyEnd(), minimumEnd),
                Math.max(vanillaPlanes.cloudEnd(), minimumEnd)
        );
    }

    public double distance() {
        return distance.value();
    }
}
