package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoToolTest {
    @Test
    void prefersCorrectFastSafeHotbarTools() {
        AutoTool autoTool = enabledModule();
        List<ToolCandidate> candidates = List.of(
                new ToolCandidate(1, 12.0, false, 100),
                new ToolCandidate(2, 5.0, true, 100),
                new ToolCandidate(3, 8.0, true, 7)
        );

        assertEquals(new AutoTool.Action(AutoTool.ActionType.SELECT, 2), autoTool.update(true, 0, candidates));
    }

    @Test
    void respectsDurabilityGuardAndLeavesBareHandEquivalentItemsAlone() {
        AutoTool autoTool = enabledModule();
        numberSetting(autoTool, "minimum_durability").set(10.0);

        assertEquals(AutoTool.Action.none(), autoTool.update(true, 0, List.of(
                new ToolCandidate(1, 20.0, true, 9),
                new ToolCandidate(2, 1.0, false, Integer.MAX_VALUE)
        )));
    }

    @Test
    void restoresOnlyTheSlotItOwnsAfterMiningEndsOrDisable() {
        AutoTool autoTool = enabledModule();
        List<ToolCandidate> candidates = List.of(new ToolCandidate(1, 8.0, true, 100));

        assertEquals(new AutoTool.Action(AutoTool.ActionType.SELECT, 1), autoTool.update(true, 0, candidates));
        assertEquals(new AutoTool.Action(AutoTool.ActionType.RESTORE, 0), autoTool.update(false, 1, List.of()));

        assertEquals(new AutoTool.Action(AutoTool.ActionType.SELECT, 1), autoTool.update(true, 0, candidates));
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoTool);
        registry.setEnabled(autoTool, false);
        assertEquals(new AutoTool.Action(AutoTool.ActionType.RESTORE, 0), autoTool.update(false, 1, List.of()));
    }

    @Test
    void doesNotRestoreOverAUserSlotChange() {
        AutoTool autoTool = enabledModule();
        assertEquals(new AutoTool.Action(AutoTool.ActionType.SELECT, 1), autoTool.update(true, 0,
                List.of(new ToolCandidate(1, 8.0, true, 100))));

        assertEquals(AutoTool.Action.none(), autoTool.update(false, 4, List.of()));
    }

    @Test
    void leavesAManualSlotChoiceAloneForTheRestOfThatMiningSession() {
        AutoTool autoTool = enabledModule();
        List<ToolCandidate> candidates = List.of(new ToolCandidate(1, 8.0, true, 100));
        assertEquals(new AutoTool.Action(AutoTool.ActionType.SELECT, 1), autoTool.update(true, 0, candidates));

        assertEquals(AutoTool.Action.none(), autoTool.update(true, 4, candidates));
        assertEquals(AutoTool.Action.none(), autoTool.update(true, 4, candidates));

        assertEquals(AutoTool.Action.none(), autoTool.update(false, 4, List.of()));
        assertEquals(new AutoTool.Action(AutoTool.ActionType.SELECT, 1), autoTool.update(true, 4, candidates));
    }

    private static AutoTool enabledModule() {
        AutoTool autoTool = new AutoTool();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoTool);
        registry.setEnabled(autoTool, true);
        return autoTool;
    }

    private static NumberSetting numberSetting(AutoTool module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

}
