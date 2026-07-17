package dev.helikon.client.setting;

import java.util.List;
import java.util.function.BooleanSupplier;

/** A bounded selection of entity identifier tokens. */
public final class EntitySelectorSetting extends IdentifierSelectorSetting {
    public EntitySelectorSetting(String id, String name, String description, List<String> defaultValue, int maximumEntries) {
        this(id, name, description, defaultValue, maximumEntries, () -> true);
    }

    public EntitySelectorSetting(
            String id,
            String name,
            String description,
            List<String> defaultValue,
            int maximumEntries,
            BooleanSupplier visibilityPredicate
    ) {
        super(id, name, description, defaultValue, maximumEntries, visibilityPredicate);
    }
}
