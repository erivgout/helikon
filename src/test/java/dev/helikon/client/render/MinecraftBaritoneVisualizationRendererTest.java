package dev.helikon.client.render;

import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalInverted;
import baritone.api.pathing.goals.GoalXZ;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftBaritoneVisualizationRendererTest {
    @Test
    void recursivelyCountsCompositeMiningGoalsWithinMarkerBudget() {
        GoalComposite miningTargets = new GoalComposite(
                new GoalBlock(1, 64, 1),
                new GoalBlock(5, 12, -3),
                new GoalInverted(new GoalXZ(30, 40))
        );

        assertEquals(3, MinecraftBaritoneVisualizationRenderer.countRenderableGoalMarkers(miningTargets, 128));
        assertEquals(2, MinecraftBaritoneVisualizationRenderer.countRenderableGoalMarkers(miningTargets, 2));
    }
}
