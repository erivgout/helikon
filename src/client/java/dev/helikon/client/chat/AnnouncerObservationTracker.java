package dev.helikon.client.chat;

import dev.helikon.client.module.chat.AnnouncementTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Converts successive local-player observations into bounded Announcer trigger facts. */
public final class AnnouncerObservationTracker {
    public record Fact(double x, double y, double z, float health, String dimension) {
        public Fact {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(health) || health < 0.0F || health > 20.0F) {
                throw new IllegalArgumentException("Announcer observations must be finite local player facts");
            }
            dimension = Objects.requireNonNull(dimension, "dimension").trim();
            if (dimension.isEmpty() || dimension.length() > 255) {
                throw new IllegalArgumentException("dimension must be a bounded non-empty identifier");
            }
        }
    }

    public record Observation(AnnouncementTrigger trigger, String detail) {
        public Observation {
            trigger = Objects.requireNonNull(trigger, "trigger");
            detail = Objects.requireNonNull(detail, "detail").trim();
        }
    }

    private Fact previous;
    private double distanceSinceAnnouncement;

    public List<Observation> observe(Fact current, int distanceInterval, float lowHealthThreshold) {
        Objects.requireNonNull(current, "current");
        if (distanceInterval < 1 || !Float.isFinite(lowHealthThreshold)
                || lowHealthThreshold < 0.0F || lowHealthThreshold > 20.0F) {
            throw new IllegalArgumentException("Announcer thresholds are outside safe bounds");
        }
        if (previous == null) {
            previous = current;
            return List.of();
        }
        List<Observation> observations = new ArrayList<>(2);
        if (!previous.dimension().equals(current.dimension())) {
            distanceSinceAnnouncement = 0.0D;
            observations.add(new Observation(AnnouncementTrigger.DIMENSION_CHANGE, current.dimension()));
        } else {
            distanceSinceAnnouncement += distance(previous, current);
            if (distanceSinceAnnouncement >= distanceInterval) {
                distanceSinceAnnouncement -= distanceInterval;
                observations.add(new Observation(AnnouncementTrigger.DISTANCE_TRAVELED,
                        distanceInterval + " blocks"));
            }
        }
        if (previous.health() > lowHealthThreshold && current.health() <= lowHealthThreshold) {
            observations.add(new Observation(AnnouncementTrigger.LOW_HEALTH,
                    String.format(Locale.ROOT, "%.1f health", current.health())));
        }
        previous = current;
        return List.copyOf(observations);
    }

    public void reset() {
        previous = null;
        distanceSinceAnnouncement = 0.0D;
    }

    private static double distance(Fact first, Fact second) {
        double x = second.x() - first.x();
        double y = second.y() - first.y();
        double z = second.z() - first.z();
        return Math.sqrt(x * x + y * y + z * z);
    }
}
