package dev.helikon.client.render;

import dev.helikon.client.module.world.SeedCracker;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

/** Renders bounded chunk prisms for confirmed session-local slime evidence. */
public final class MinecraftSeedCrackerRenderer {
    private final SeedCracker module;

    public MinecraftSeedCrackerRenderer(SeedCracker module) {
        this.module = Objects.requireNonNull(module, "module");
    }

    public void render() {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() || !module.renderEvidence()
                || client.level == null || client.player == null) {
            return;
        }
        double halfHeight = module.renderHeight() / 2.0D;
        double minimumY = client.player.getY() - halfHeight;
        double maximumY = client.player.getY() + halfHeight;
        GizmoStyle style = GizmoStyle.strokeAndFill(
                module.evidenceColor(), module.lineWidth(), module.evidenceFillColor());
        for (SeedCracker.Observation observation : module.observations()) {
            if (!client.level.hasChunk(observation.coordinate().x(), observation.coordinate().z())) {
                continue;
            }
            double minimumX = observation.coordinate().x() * 16.0D;
            double minimumZ = observation.coordinate().z() * 16.0D;
            GizmoProperties marker = Gizmos.cuboid(new AABB(
                    minimumX, minimumY, minimumZ,
                    minimumX + 16.0D, maximumY, minimumZ + 16.0D), style);
            if (module.alwaysOnTop()) {
                marker.setAlwaysOnTop();
            }
        }
    }
}
