package dev.helikon.client.module.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Produces bounded, deterministic local placement-preview coordinates without Minecraft dependencies. */
public final class BuilderPlan {
    private BuilderPlan() {
    }

    public enum Mode {
        SINGLE,
        HORIZONTAL_LINE,
        VERTICAL_LINE,
        FLOOR,
        WALL
    }

    public record Anchor(BuildPoint target, BuildVector right, BuildVector forward) {
        public Anchor {
            target = Objects.requireNonNull(target, "target");
            right = Objects.requireNonNull(right, "right");
            forward = Objects.requireNonNull(forward, "forward");
            if (right.y() != 0 || forward.y() != 0 || right.x() * forward.x() + right.z() * forward.z() != 0) {
                throw new IllegalArgumentException("right and forward must be perpendicular horizontal unit vectors");
            }
        }
    }

    public static List<BuildPoint> positions(Mode mode, Anchor anchor, int length, int width, int height) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(anchor, "anchor");
        validateSize(length, "length");
        validateSize(width, "width");
        validateSize(height, "height");
        List<BuildPoint> result = new ArrayList<>();
        switch (mode) {
            case SINGLE -> result.add(anchor.target());
            case HORIZONTAL_LINE -> addLine(result, anchor.target(), anchor.right(), length);
            case VERTICAL_LINE -> addLine(result, anchor.target(), BuildVector.UP, height);
            case FLOOR -> {
                for (int forward = 0; forward < length; forward++) {
                    BuildPoint row = anchor.target().offset(anchor.forward(), forward);
                    addLine(result, row, anchor.right(), width);
                }
            }
            case WALL -> {
                for (int vertical = 0; vertical < height; vertical++) {
                    BuildPoint row = anchor.target().offset(BuildVector.UP, vertical);
                    addLine(result, row, anchor.right(), width);
                }
            }
        }
        return List.copyOf(result);
    }

    private static void addLine(List<BuildPoint> result, BuildPoint start, BuildVector direction, int length) {
        for (int index = 0; index < length; index++) {
            result.add(start.offset(direction, index));
        }
    }

    private static void validateSize(int value, String name) {
        if (value < 1 || value > 16) {
            throw new IllegalArgumentException(name + " must be between 1 and 16");
        }
    }
}
