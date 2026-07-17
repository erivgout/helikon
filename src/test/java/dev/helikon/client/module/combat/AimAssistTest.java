package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimAssistTest {
    @Test
    void disabledModuleNeverAdjustsView() {
        AimAssist aimAssist = new AimAssist();
        assertTrue(aimAssist.nextRotation(List.of(target("a", 3.0D, 30.0D)),
                new CombatAim.Rotation(0.0F, 0.0F)).isEmpty());
        assertTrue(aimAssist.markerTargetId().isEmpty());
    }

    @Test
    void selectsHighestPriorityEligibleTargetAndBoundsTheChange() {
        AimAssist aimAssist = enabled(new AimAssist());
        CombatTarget wideAngle = target("wide", 2.0D, 50.0D);
        CombatTarget nearCrosshair = target("near", 4.0D, 5.0D);
        CombatTarget blocked = blockedTarget("blocked", 1.0D, 1.0D);

        Optional<CombatAim.Rotation> rotation = aimAssist.nextRotation(List.of(wideAngle, blocked, nearCrosshair),
                new CombatAim.Rotation(0.0F, 0.0F));
        assertTrue(rotation.isPresent());
        // Default ANGLE priority prefers the target closest to the current view, and line of sight is required.
        assertEquals("near", aimAssist.markerTargetId().orElseThrow());
        assertTrue(Math.abs(rotation.get().yaw()) <= 6.0F);
        assertTrue(Math.abs(rotation.get().pitch()) <= 6.0F);
    }

    @Test
    void excludesOutOfRangeAndClearsMarkerWhenNoneApply() {
        AimAssist aimAssist = enabled(new AimAssist());
        CombatTarget farTarget = target("far", 20.0D, 5.0D);
        assertTrue(aimAssist.nextRotation(List.of(farTarget), new CombatAim.Rotation(0.0F, 0.0F)).isEmpty());
        assertTrue(aimAssist.markerTargetId().isEmpty());
    }

    @Test
    void defaultsAreOffAndGatedOnAWeapon() {
        AimAssist aimAssist = new AimAssist();
        assertFalse(aimAssist.isEnabled());
        assertTrue(aimAssist.requireWeapon());
        assertFalse(aimAssist.requireAttackKey());
    }

    @Test
    void onContextLostAndDisableClearTheMarker() {
        AimAssist aimAssist = enabled(new AimAssist());
        aimAssist.nextRotation(List.of(target("a", 3.0D, 5.0D)), new CombatAim.Rotation(0.0F, 0.0F));
        assertTrue(aimAssist.markerTargetId().isPresent());
        aimAssist.onContextLost();
        assertTrue(aimAssist.markerTargetId().isEmpty());
    }

    private static CombatTarget target(String id, double distance, double angle) {
        return new CombatTarget(id, id, CombatEntityType.HOSTILE, false, false, true, true, true, distance, angle,
                distance, 0.0D, distance, 0.0D, 0.0D, 0.0D, 20.0D, 0, "minecraft:air", List.of());
    }

    private static CombatTarget blockedTarget(String id, double distance, double angle) {
        return new CombatTarget(id, id, CombatEntityType.HOSTILE, false, false, true, true, false, distance, angle,
                distance, 0.0D, distance, 0.0D, 0.0D, 0.0D, 20.0D, 0, "minecraft:air", List.of());
    }

    private static AimAssist enabled(AimAssist module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
