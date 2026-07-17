package dev.helikon.client.module.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuilderPlanTest {
    private static final BuilderPlan.Anchor ANCHOR = new BuilderPlan.Anchor(new BuildPoint(10, 64, 10),
            new BuildVector(1, 0, 0), new BuildVector(0, 0, 1));

    @Test
    void buildsDeterministicBoundedLinesAndFloors() {
        assertEquals(List.of(new BuildPoint(10, 64, 10), new BuildPoint(11, 64, 10), new BuildPoint(12, 64, 10)),
                BuilderPlan.positions(BuilderPlan.Mode.HORIZONTAL_LINE, ANCHOR, 3, 1, 1));
        assertEquals(List.of(new BuildPoint(10, 64, 10), new BuildPoint(11, 64, 10),
                        new BuildPoint(10, 64, 11), new BuildPoint(11, 64, 11)),
                BuilderPlan.positions(BuilderPlan.Mode.FLOOR, ANCHOR, 2, 2, 1));
    }

    @Test
    void buildsVerticalWallsFromTheClickedTarget() {
        assertEquals(List.of(new BuildPoint(10, 64, 10), new BuildPoint(11, 64, 10),
                        new BuildPoint(10, 65, 10), new BuildPoint(11, 65, 10)),
                BuilderPlan.positions(BuilderPlan.Mode.WALL, ANCHOR, 1, 2, 2));
        assertEquals(List.of(new BuildPoint(10, 64, 10), new BuildPoint(10, 65, 10), new BuildPoint(10, 66, 10)),
                BuilderPlan.positions(BuilderPlan.Mode.VERTICAL_LINE, ANCHOR, 1, 1, 3));
    }

    @Test
    void rejectsNonPerpendicularOrOversizedPlans() {
        assertThrows(IllegalArgumentException.class, () -> new BuilderPlan.Anchor(new BuildPoint(0, 0, 0),
                new BuildVector(1, 0, 0), new BuildVector(1, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> BuilderPlan.positions(BuilderPlan.Mode.FLOOR, ANCHOR,
                17, 1, 1));
    }
}
