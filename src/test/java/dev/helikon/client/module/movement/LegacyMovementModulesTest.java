package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMovementModulesTest {
    @Test
    void noClipProducesBoundedDirectionalAndVerticalMotion() {
        NoClip module = enabled(new NoClip());
        NoClip.Motion motion = module.motion(0.0D, 1.0D, 0.0D, true, false);
        assertEquals(0.0D, motion.x(), 1.0E-8D);
        assertEquals(0.2D, motion.y(), 1.0E-8D);
        assertEquals(0.2D, motion.z(), 1.0E-8D);
    }

    @Test
    void snowShoeOnlyRaisesVelocityInsidePowderSnow() {
        SnowShoe module = enabled(new SnowShoe());
        assertEquals(0.0D, module.verticalVelocity(true, false, -0.4D), 0.0D);
        assertEquals(0.12D, module.verticalVelocity(true, true, -0.4D), 0.0D);
        assertEquals(-0.4D, module.verticalVelocity(false, true, -0.4D), 0.0D);
    }

    @Test
    void mountMotionAndForcePushAreBoundedAndFriendSafe() {
        MountBypass mount = enabled(new MountBypass());
        assertEquals(0.35D, mount.motion(0.0D, 1.0D, 0.0D, false, false).z(), 1.0E-8D);

        ForcePush push = enabled(new ForcePush());
        assertTrue(push.motion(true, List.of(
                new ForcePush.Candidate(1, true, 1.0D, 1.0D, 0.0D),
                new ForcePush.Candidate(2, false, 2.0D, 2.0D, 0.0D))).isPresent());
        assertEquals(2, push.motion(true, List.of(
                new ForcePush.Candidate(1, true, 1.0D, 1.0D, 0.0D),
                new ForcePush.Candidate(2, false, 2.0D, 2.0D, 0.0D))).orElseThrow().entityId());
        assertTrue(push.motion(false, List.of(new ForcePush.Candidate(2, false, 2.0D, 2.0D, 0.0D))).isEmpty());
    }

    @Test
    void phaseRequiresCollisionMovementAndCooldown() {
        Phase phase = enabled(new Phase());
        assertTrue(phase.canAttempt(0L, true, true));
        phase.markAttempt(0L);
        assertFalse(phase.canAttempt(1L, true, true));
        assertTrue(phase.canAttempt(10L, true, true));
        phase.disable();
        phase.enable();
        assertTrue(phase.canAttempt(0L, true, true));
    }

    private static <T extends dev.helikon.client.module.Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
