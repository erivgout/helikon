package dev.helikon.client.module.combat;

import java.util.Objects;
import java.util.Optional;

/** Computes the smallest padded, bounded arena that can contain both two-block-tall players. */
public final class DomainBoundsCalculator {
    private DomainBoundsCalculator() {
    }

    public static Optional<DomainBounds> calculate(
            DomainPosition localFeet,
            DomainPosition targetFeet,
            int interiorPadding,
            int interiorHeight,
            int maximumWidth,
            int maximumLength
    ) {
        Objects.requireNonNull(localFeet, "localFeet");
        Objects.requireNonNull(targetFeet, "targetFeet");
        if (interiorPadding < 0 || interiorHeight < 2 || maximumWidth < 3 || maximumLength < 3) {
            throw new IllegalArgumentException("Domain structure settings are invalid");
        }

        int floorY = Math.min(localFeet.y(), targetFeet.y());
        int highestHeadY = Math.max(localFeet.y(), targetFeet.y()) + 1;
        if (highestHeadY >= floorY + interiorHeight) {
            return Optional.empty();
        }

        DomainBounds bounds = new DomainBounds(
                Math.min(localFeet.x(), targetFeet.x()) - interiorPadding,
                Math.max(localFeet.x(), targetFeet.x()) + interiorPadding,
                Math.min(localFeet.z(), targetFeet.z()) - interiorPadding,
                Math.max(localFeet.z(), targetFeet.z()) + interiorPadding,
                floorY,
                interiorHeight
        );
        return bounds.width() <= maximumWidth && bounds.length() <= maximumLength
                ? Optional.of(bounds)
                : Optional.empty();
    }
}
