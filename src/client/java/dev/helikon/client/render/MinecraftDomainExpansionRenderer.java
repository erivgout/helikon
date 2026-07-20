package dev.helikon.client.render;

import dev.helikon.client.gui.ClickGuiTheme;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.module.combat.DomainExpansion;
import dev.helikon.client.module.combat.DomainPosition;
import dev.helikon.client.module.render.RainbowUiAccess;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Read-only themed Gizmo rendering for Domain Expansion's bounded plan snapshot. */
public final class MinecraftDomainExpansionRenderer {
    private final DomainExpansion module;
    private final ClickGuiWindowState window;

    public MinecraftDomainExpansionRenderer(DomainExpansion module, ClickGuiWindowState window) {
        this.module = Objects.requireNonNull(module, "module");
        this.window = Objects.requireNonNull(window, "window");
    }

    public void render() {
        if (!module.isEnabled() || !module.renderPlan()) {
            return;
        }
        DomainExpansion.RenderSnapshot snapshot = module.renderSnapshot();
        ClickGuiTheme theme = window.theme();
        int accent = RainbowUiAccess.accent(System.currentTimeMillis(), theme.accent());
        for (DomainExpansion.RenderBlock block : snapshot.blocks()) {
            if (module.renderRemainingOnly()
                    && (block.status() == DomainExpansion.PlacementStatus.PLACED
                    || block.status() == DomainExpansion.PlacementStatus.EXISTING)) {
                continue;
            }
            int color = color(block, theme, accent);
            GizmoStyle style = GizmoStyle.strokeAndFill(color, 1.0F, RenderColor.withAlpha(color, 0.18D));
            DomainPosition position = block.position();
            GizmoProperties gizmo = Gizmos.cuboid(new AABB(position.x(), position.y(), position.z(),
                    position.x() + 1.0D, position.y() + 1.0D, position.z() + 1.0D), style);
            if (module.renderThroughWalls()) {
                gizmo.setAlwaysOnTop();
            }
        }
        if (module.renderTarget() && snapshot.bounds() != null && !snapshot.targetName().isBlank()) {
            double centerX = (snapshot.bounds().wallMinX() + snapshot.bounds().wallMaxX() + 1.0D) * 0.5D;
            double centerZ = (snapshot.bounds().wallMinZ() + snapshot.bounds().wallMaxZ() + 1.0D) * 0.5D;
            String label = snapshot.targetName() + "  " + Math.round(snapshot.completion() * 100.0D) + "%";
            GizmoProperties text = Gizmos.billboardText(label,
                    new Vec3(centerX, snapshot.bounds().roofY() + 1.0D, centerZ),
                    TextGizmo.Style.forColorAndCentered(accent));
            if (module.renderThroughWalls()) {
                text.setAlwaysOnTop();
            }
        }
    }

    private static int color(DomainExpansion.RenderBlock block, ClickGuiTheme theme, int accent) {
        return switch (block.status()) {
            case PENDING -> block.part() == dev.helikon.client.module.combat.DomainPlacementPlan.Part.WALL
                    ? accent : theme.textDim();
            case REQUESTED -> theme.scrollbar();
            case PLACED -> theme.text();
            case EXISTING -> theme.outline();
            case INVALID -> theme.invalid();
            case FAILED -> theme.invalid();
        };
    }
}
