package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldProjectilePreviewTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void matchesTheVerifiedFullChargeBowLaunchDirectionAndSpeed() {
        // Looking straight along +Z (yaw 0, pitch 0), a fully drawn bow launches at speed 3.0.
        Optional<HeldProjectilePreview.Launch> launch = HeldProjectilePreview.launch(
                HeldProjectilePreview.Kind.BOW, 0.0D, 0.0D, 40);

        assertTrue(launch.isPresent());
        TrajectoryVector velocity = launch.get().velocity();
        assertEquals(0.0D, velocity.x(), EPSILON);
        assertEquals(0.0D, velocity.y(), EPSILON);
        assertEquals(3.0D, velocity.z(), EPSILON);
    }

    @Test
    void suppressesABowThatIsNotDrawnFarEnoughToFire() {
        // Vanilla getPowerForTime for a single tick is well under the 0.1 firing threshold.
        assertTrue(HeldProjectilePreview.bowPower(0) < 0.1D);
        assertFalse(HeldProjectilePreview.launch(HeldProjectilePreview.Kind.BOW, 0.0D, 0.0D, 1).isPresent());
    }

    @Test
    void reproducesTheVanillaBowPowerCurve() {
        assertEquals(1.0D, HeldProjectilePreview.bowPower(20), EPSILON);
        assertEquals(1.0D, HeldProjectilePreview.bowPower(40), EPSILON);
        // charge = 0.5 -> (0.25 + 1.0) / 3 = 0.41666...
        assertEquals((0.25D + 1.0D) / 3.0D, HeldProjectilePreview.bowPower(10), EPSILON);
    }

    @Test
    void fixedSpeedFamiliesLaunchAlongTheAimDirection() {
        // Straight down (pitch 90) launches purely along -Y at the family speed.
        TrajectoryVector snowball = HeldProjectilePreview.launch(
                HeldProjectilePreview.Kind.THROWABLE, 0.0D, 90.0D, 0).orElseThrow().velocity();
        assertEquals(0.0D, snowball.x(), 1.0E-6D);
        assertEquals(-1.5D, snowball.y(), 1.0E-6D);
        assertEquals(0.0D, snowball.z(), 1.0E-6D);

        TrajectoryVector trident = HeldProjectilePreview.launch(
                HeldProjectilePreview.Kind.TRIDENT, 0.0D, 90.0D, 0).orElseThrow().velocity();
        assertEquals(-2.5D, trident.y(), 1.0E-6D);
    }

    @Test
    void splashPotionArcsUpwardFromItsPitchOffsetAtItsThrowSpeed() {
        // Aiming flat, the -20 degree potion offset still gives an upward launch component,
        // and the overall speed equals the vanilla 0.5 throw speed.
        TrajectoryVector potion = HeldProjectilePreview.launch(
                HeldProjectilePreview.Kind.SPLASH_POTION, 0.0D, 0.0D, 0).orElseThrow().velocity();
        double speed = Math.sqrt(potion.x() * potion.x() + potion.y() * potion.y() + potion.z() * potion.z());
        assertEquals(0.5D, speed, 1.0E-9D);
        assertTrue(potion.y() > 0.0D, "the potion should be launched slightly upward");
    }

    @Test
    void rejectsNonFiniteAndNonPositiveLaunchParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> HeldProjectilePreview.velocity(Double.NaN, 0.0D, 0.0D, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> HeldProjectilePreview.velocity(0.0D, 0.0D, 0.0D, 0.0D));
    }
}
