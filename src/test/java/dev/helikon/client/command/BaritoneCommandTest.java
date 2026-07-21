package dev.helikon.client.command;

import dev.helikon.client.integration.BaritoneAccess;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.world.BaritoneNavigation;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ActionSetting;
import dev.helikon.client.setting.StringSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaritoneCommandTest {
    @Test
    void commandEnablesModuleAndForwardsCompleteBaritoneCommand() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new BaritoneCommand(modules, module));

        assertTrue(dispatcher.dispatch(".baritone goto 100 64 -30", new RecordingFeedback()));

        assertTrue(module.isEnabled());
        assertTrue(access.options.active());
        assertEquals("goto 100 64 -30", access.command);
    }

    @Test
    void settingsSynchronizeAndDisablingCancelsPathing() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        modules.setEnabled(module, true);
        BooleanSetting allowBreak = (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("allow_break"))
                .findFirst()
                .orElseThrow();

        allowBreak.set(false);
        modules.setEnabled(module, false);

        assertFalse(access.options.active());
        assertFalse(access.options.allowBreak());
        assertEquals(1, access.cancelCount);
    }

    @Test
    void statusDoesNotEnableIdleModule() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        BaritoneCommand command = new BaritoneCommand(modules, module);
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(java.util.List.of("status"), feedback);

        assertFalse(module.isEnabled());
        assertTrue(feedback.infos.getFirst().contains("disabled"));
    }

    @Test
    void clickGuiCommandFieldExposesTheFullBaritoneCommandSurface() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        modules.setEnabled(module, true);
        StringSetting command = (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("command"))
                .findFirst()
                .orElseThrow();
        ActionSetting run = (ActionSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("run_command"))
                .findFirst()
                .orElseThrow();

        command.set("waypoint list");
        run.run();

        assertEquals("waypoint list", access.command);
    }

    @Test
    void combatAttacksTemporarilyPauseAndThenReleaseBaritone() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        modules.setEnabled(module, true);
        NumberSetting delay = (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("combat_resume_delay"))
                .findFirst()
                .orElseThrow();
        delay.set(3.0D);

        module.observeCombatAttack(20L);
        module.tickCombatPause(23L);
        assertTrue(access.combatPaused);

        module.tickCombatPause(24L);
        assertFalse(access.combatPaused);
    }

    @Test
    void disablingBaritoneReleasesItsCombatPause() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        modules.setEnabled(module, true);

        module.observeCombatAttack(5L);
        assertTrue(access.combatPaused);

        modules.setEnabled(module, false);
        assertFalse(access.combatPaused);
    }

    @Test
    void fastBreakCompatibilityIsOwnedOnlyWhileBothModulesAreEnabled() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        modules.setEnabled(module, true);

        module.synchronizeFastBreak(true, 2);
        assertTrue(access.fastBreakEnabled);
        assertEquals(2, access.fastBreakDelay);

        modules.setEnabled(module, false);
        assertFalse(access.fastBreakEnabled);
    }

    @Test
    void autoEatPauseFollowsEatingAndCanBeDisabled() {
        FakeBaritoneAccess access = new FakeBaritoneAccess();
        BaritoneNavigation module = new BaritoneNavigation(access);
        ModuleRegistry modules = new ModuleRegistry();
        modules.register(module);
        modules.setEnabled(module, true);

        module.setAutoEatPaused(true);
        assertTrue(access.autoEatPaused);
        module.setAutoEatPaused(false);
        assertFalse(access.autoEatPaused);

        BooleanSetting setting = (BooleanSetting) module.settings().stream()
                .filter(candidate -> candidate.id().equals("pause_for_auto_eat"))
                .findFirst()
                .orElseThrow();
        setting.set(false);
        module.setAutoEatPaused(true);
        assertFalse(access.autoEatPaused);
    }

    private static final class FakeBaritoneAccess implements BaritoneAccess {
        private Options options;
        private String command;
        private int cancelCount;
        private boolean combatPaused;
        private boolean autoEatPaused;
        private boolean fastBreakEnabled;
        private int fastBreakDelay;

        @Override
        public void apply(Options options) {
            this.options = options;
        }

        @Override
        public boolean execute(String command) {
            this.command = command;
            return true;
        }

        @Override
        public void cancel() {
            cancelCount++;
        }

        @Override
        public void setCombatPaused(boolean paused) {
            combatPaused = paused;
        }

        @Override
        public void setAutoEatPaused(boolean paused) {
            autoEatPaused = paused;
        }

        @Override
        public void setFastBreakDelay(boolean enabled, int delayTicks) {
            fastBreakEnabled = enabled;
            fastBreakDelay = delayTicks;
        }

        @Override
        public boolean isMovementForced() {
            return false;
        }

        @Override
        public boolean isPathing() {
            return false;
        }

        @Override
        public boolean hasPath() {
            return false;
        }

        @Override
        public String goalDescription() {
            return "none";
        }
    }
}
