package dev.helikon.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Bounded local health-delta tracker with deterministic indicator fade and rise calculations. */
public final class DamageIndicatorTracker {
    private final Map<Integer, Observation> observations = new HashMap<>();
    private final ArrayDeque<Indicator> indicators = new ArrayDeque<>();

    /** Records current visible entity health and emits an indicator for a confirmed local health loss. */
    public void observe(Collection<ObservedEntity> entities, long nowMillis, int maximumIndicators,
                        int maximumTrackedEntities) {
        Objects.requireNonNull(entities, "entities");
        if (maximumIndicators < 1) {
            throw new IllegalArgumentException("maximumIndicators must be positive");
        }
        if (maximumTrackedEntities < 1) {
            throw new IllegalArgumentException("maximumTrackedEntities must be positive");
        }
        Set<Integer> seenIds = new HashSet<>();
        int tracked = 0;
        for (ObservedEntity entity : entities) {
            if (tracked >= maximumTrackedEntities) {
                break;
            }
            validate(entity);
            seenIds.add(entity.id());
            tracked++;
            Observation previous = observations.put(entity.id(), new Observation(entity.health()));
            if (previous != null && entity.hurtTime() > 0 && entity.health() < previous.health()) {
                indicators.addLast(new Indicator(entity.id(), entity.x(), entity.y(), entity.z(),
                        previous.health() - entity.health(), nowMillis));
            }
        }
        observations.keySet().retainAll(seenIds);
        while (indicators.size() > maximumIndicators) {
            indicators.removeFirst();
        }
    }

    /** Builds the bounded local visual state and drops expired indicators. */
    public List<RenderedIndicator> render(long nowMillis, long lifetimeMillis, double riseDistance) {
        if (lifetimeMillis < 1) {
            throw new IllegalArgumentException("lifetimeMillis must be positive");
        }
        if (!Double.isFinite(riseDistance) || riseDistance < 0.0D) {
            throw new IllegalArgumentException("riseDistance must be finite and non-negative");
        }
        List<RenderedIndicator> rendered = new ArrayList<>(indicators.size());
        while (!indicators.isEmpty() && nowMillis - indicators.peekFirst().createdAtMillis() >= lifetimeMillis) {
            indicators.removeFirst();
        }
        for (Indicator indicator : indicators) {
            double progress = Math.clamp((double) (nowMillis - indicator.createdAtMillis()) / lifetimeMillis, 0.0D, 1.0D);
            rendered.add(new RenderedIndicator(indicator.entityId(), indicator.x(), indicator.y() + progress * riseDistance,
                    indicator.z(), indicator.damage(), 1.0D - progress));
        }
        return List.copyOf(rendered);
    }

    public void clear() {
        observations.clear();
        indicators.clear();
    }

    int trackedEntityCount() {
        return observations.size();
    }

    private static void validate(ObservedEntity entity) {
        if (!Double.isFinite(entity.x()) || !Double.isFinite(entity.y()) || !Double.isFinite(entity.z())
                || !Float.isFinite(entity.health()) || entity.health() < 0.0F || entity.hurtTime() < 0) {
            throw new IllegalArgumentException("Observed entity values must be finite and non-negative");
        }
    }

    public record ObservedEntity(int id, double x, double y, double z, float health, int hurtTime) {
    }

    public record RenderedIndicator(int entityId, double x, double y, double z, float damage, double alpha) {
    }

    private record Observation(float health) {
    }

    private record Indicator(int entityId, double x, double y, double z, float damage, long createdAtMillis) {
    }
}
