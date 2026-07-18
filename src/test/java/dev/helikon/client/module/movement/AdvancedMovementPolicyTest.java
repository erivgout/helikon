package dev.helikon.client.module.movement;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import net.minecraft.world.entity.player.Input;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedMovementPolicyTest {
    @Test
    void noSlowKeepsEachConfiguredSlowdownCategoryIndependent() {
        NoSlow module = enabled(new NoSlow());

        assertTrue(module.ignoresUseSlowdown(NoSlow.UseKind.EATING));
        assertTrue(module.ignoresUseSlowdown(NoSlow.UseKind.BOW));
        assertFalse(module.ignoresUseSlowdown(NoSlow.UseKind.OTHER));
        assertFalse(module.ignoresSneakSlowdown(true));
        booleanSetting(module, "sneaking").set(true);
        assertTrue(module.ignoresSneakSlowdown(true));
    }

    @Test
    void ladderAndStepPoliciesRemainBoundedAndContextual() {
        FastLadders ladders = enabled(new FastLadders());
        assertEquals(0.18D, ladders.verticalVelocity(true, true, false, false, 0.02D).orElseThrow(), 0.0001D);
        assertTrue(ladders.verticalVelocity(false, true, false, false, 0.0D).isEmpty());

        Step step = enabled(new Step());
        assertEquals(1.0F, step.stepHeight(0.6F));
        assertEquals(1.7F, step.stepHeight(1.7F));
    }

    @Test
    void speedAndBunnyHopUseCappedLocalMotionOnlyWhileEnabled() {
        Speed speed = enabled(new Speed());
        assertEquals(0.90D, speed.adjust(new HorizontalVelocity(0.30D, 0.0D),
                new HorizontalVelocity(1.0D, 0.0D), true).x(), 0.0001D);
        HorizontalVelocity airborneTurn = speed.adjust(new HorizontalVelocity(0.0D, 0.30D),
                new HorizontalVelocity(-1.0D, 0.0D), true);
        assertEquals(-0.90D, airborneTurn.x(), 0.0001D);
        assertEquals(0.0D, airborneTurn.z(), 0.0001D);
        assertEquals(3.0D, numberSetting(speed, "multiplier").value());
        assertEquals(0.08D, numberSetting(speed, "acceleration").value());
        assertEquals(0.90D, numberSetting(speed, "maximum_speed").value());
        numberSetting(speed, "multiplier").set(10.0D);
        numberSetting(speed, "maximum_speed").set(3.0D);
        assertEquals(3.0D, speed.adjust(new HorizontalVelocity(0.30D, 0.0D),
                new HorizontalVelocity(1.0D, 0.0D), true).x(), 0.0001D);
        BunnyHop hop = enabled(new BunnyHop());
        assertTrue(hop.shouldJump(true, true, false));
        assertFalse(hop.shouldJump(false, true, false));
        assertEquals(0.30D, hop.cap(new HorizontalVelocity(1.0D, 0.0D)).speed(), 0.0001D);
    }

    @Test
    void flightOwnsOnlyMinecraftGrantedFlightState() {
        Flight flight = enabled(new Flight());
        Flight.Action enable = flight.update(new Flight.Abilities(true, false, 0.05F));
        assertTrue(enable.setFlying());
        assertTrue(enable.flying());
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(flight);
        registry.setEnabled(flight, false);
        Flight.Action restore = flight.update(new Flight.Abilities(true, true, 0.05F));
        assertTrue(restore.setFlying());
        assertFalse(restore.flying());

        Flight worldChange = enabled(new Flight());
        worldChange.update(new Flight.Abilities(true, false, 0.05F));
        worldChange.onContextLost();
        assertFalse(worldChange.update(new Flight.Abilities(false, false, 0.05F)).setSpeed());

    }

    @Test
    void noFallResetsOnlyOrdinaryUncontrolledFalls() {
        NoFall noFall = enabled(new NoFall());

        assertTrue(noFall.shouldResetFall(0.5D, false, false, false, false));
        assertFalse(noFall.shouldResetFall(0.0D, false, false, false, false));
        assertFalse(noFall.shouldResetFall(10.0D, true, false, false, false));
        assertFalse(noFall.shouldResetFall(10.0D, false, true, false, false));
        assertFalse(noFall.shouldResetFall(10.0D, false, false, true, false));
        assertFalse(noFall.shouldResetFall(10.0D, false, false, false, true));
        assertTrue(noFall.protectsTeleport(false, false, false));
        assertFalse(noFall.protectsTeleport(true, false, false));
    }

    @Test
    void freecamIsAnIndependentModuleWithBoundedSpeed() {
        Freecam freecam = enabled(new Freecam());

        assertEquals("freecam", freecam.id());
        assertEquals(0.15D, freecam.speed(), 0.0001D);
        numberSetting(freecam, "speed").set(0.4D);
        assertEquals(0.4D, freecam.speed(), 0.0001D);

        Freecam.Rotation turned = freecam.turn(30.0F, 10.0F, 20.0D, -40.0D);
        assertEquals(33.0F, turned.yaw(), 0.0001F);
        assertEquals(4.0F, turned.pitch(), 0.0001F);
        assertEquals(90.0F, freecam.turn(0.0F, 89.0F, 0.0D, 100.0D).pitch(), 0.0001F);
    }

    @Test
    void elytraPoliciesAdjustPitchGraduallyAndReportLocalStatus() {
        ExtraElytra elytra = enabled(new ExtraElytra());
        assertEquals(8.5F, elytra.adjustedPitch(10.0F, true, false, 100.0D), 0.001F);
        assertEquals(-8.5F, elytra.adjustedPitch(-10.0F, true, true, 2.0D), 0.001F);
        ExtraElytra.Status status = elytra.status(new HorizontalVelocity(0.3D, 0.4D), -0.5D, 12);
        assertEquals(Math.sqrt(0.5D), status.speed(), 0.0001D);
        assertTrue(status.lowDurability());
    }

    @Test
    void scaffoldChoosesBoundedTargetsAndOnlyAPlayerProvidedHotbarBlock() {
        Scaffold scaffold = enabled(new Scaffold());
        assertEquals(new dev.helikon.client.module.world.BuildPoint(2, 63, 3), scaffold.nextTarget(0L,
                new dev.helikon.client.module.world.BuildPoint(2, 64, 3), new dev.helikon.client.module.world.BuildVector(0, 0, 1),
                false, true).orElseThrow());
        assertTrue(scaffold.nextTarget(1L, new dev.helikon.client.module.world.BuildPoint(2, 64, 3),
                new dev.helikon.client.module.world.BuildVector(0, 0, 1), false, true).isEmpty());
        assertEquals(new dev.helikon.client.module.world.BuildPoint(2, 63, 3), scaffold.nextTarget(4L,
                new dev.helikon.client.module.world.BuildPoint(2, 64, 3), new dev.helikon.client.module.world.BuildVector(0, 0, 1),
                false, true).orElseThrow());
        assertEquals(4, scaffold.selectBlockSlot(0, false,
                List.of(new Scaffold.HotbarBlock(1, 16), new Scaffold.HotbarBlock(4, 32))).orElseThrow());
        assertTrue(scaffold.selectBlockSlot(2, true,
                List.of(new Scaffold.HotbarBlock(4, 32))).isEmpty());
        assertTrue(scaffold.shouldRequestEdgeSafety(true));

        booleanSetting(scaffold, "tower").set(true);
        AdvancedMovementInputAccess.install(new BunnyHop(), scaffold);
        Input input = new Input(false, false, false, false, false, false, false);
        assertTrue(AdvancedMovementInputAccess.apply(input, false, true, false, true).jump());
        assertTrue(AdvancedMovementInputAccess.apply(input, false, true, false, true).shift());
        assertFalse(AdvancedMovementInputAccess.apply(input, true, true, false, true).jump());
        assertFalse(AdvancedMovementInputAccess.apply(input, true, true, false, true).shift());
    }

    @Test
    void timerUsesOnlyTheConfiguredSafeRateRange() {
        Timer timer = enabled(new Timer());
        numberSetting(timer, "tick_multiplier").set(1.20D);
        assertEquals(1.20F, timer.multiplier(), 0.0001F);
        numberSetting(timer, "tick_multiplier").set(5.0D);
        assertEquals(5.0F, timer.multiplier(), 0.0001F);

        booleanSetting(timer, "digging_only").set(true);
        assertEquals(1.0F, timer.multiplier(), 0.0001F);
        assertEquals(4, timer.extraDiggingSteps(true));
        assertEquals(0, timer.extraDiggingSteps(false));
    }

    @Test
    void diggingOnlyTimerAccumulatesFractionalProgressSteps() {
        Timer timer = enabled(new Timer());
        numberSetting(timer, "tick_multiplier").set(1.5D);
        booleanSetting(timer, "digging_only").set(true);

        assertEquals(0, timer.extraDiggingSteps(true));
        assertEquals(1, timer.extraDiggingSteps(true));
        assertEquals(0, timer.extraDiggingSteps(true));
        assertEquals(1, timer.extraDiggingSteps(true));
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst()
                .orElseThrow();
    }

    private static NumberSetting numberSetting(Module module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst()
                .orElseThrow();
    }
}
