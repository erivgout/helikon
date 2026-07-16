package dev.helikon.client.command;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Resolves user-typed key names ({@code r}, {@code f6}, {@code right.shift})
 * through Minecraft's keyboard key registry.
 */
public final class MinecraftKeyNameResolver implements KeyNameResolver {
    @Override
    public OptionalInt resolve(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return OptionalInt.empty();
        }

        try {
            InputConstants.Key key = InputConstants.getKey("key.keyboard." + keyName.toLowerCase(Locale.ROOT));
            if (key.equals(InputConstants.UNKNOWN)) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(key.getValue());
        } catch (RuntimeException exception) {
            return OptionalInt.empty();
        }
    }
}
