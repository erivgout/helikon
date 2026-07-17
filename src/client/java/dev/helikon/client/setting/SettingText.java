package dev.helikon.client.setting;

import dev.helikon.client.input.Keybind;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minecraft-free text representation used by the ClickGUI for every validated
 * setting type that is naturally editable as one compact field.
 */
public final class SettingText {
    private SettingText() {
    }

    public static boolean isEditable(Setting<?> setting) {
        return setting instanceof NumberSetting || setting instanceof IntegerSetting || setting instanceof ColorSetting
                || setting instanceof StringSetting || setting instanceof StringListSetting || setting instanceof RegexSetting
                || setting instanceof BlockSelectorSetting || setting instanceof ItemSelectorSetting
                || setting instanceof EntitySelectorSetting || setting instanceof MultiSelectEnumSetting<?>
                || setting instanceof RangeSetting || setting instanceof KeybindSetting;
    }

    public static String format(Setting<?> setting) {
        if (setting instanceof NumberSetting numberSetting) {
            return NumberSettingText.format(numberSetting.value());
        }
        if (setting instanceof IntegerSetting integerSetting) {
            return Integer.toString(integerSetting.value());
        }
        if (setting instanceof ColorSetting colorSetting) {
            return ColorSettingText.format(colorSetting.value());
        }
        if (setting instanceof StringSetting stringSetting) {
            return stringSetting.value();
        }
        if (setting instanceof RegexSetting regexSetting) {
            return regexSetting.value();
        }
        if (setting instanceof StringListSetting stringList) {
            return String.join(";", stringList.value());
        }
        if (setting instanceof IdentifierSelectorSetting selector) {
            return String.join(";", selector.value());
        }
        if (setting instanceof MultiSelectEnumSetting<?> multiSelect) {
            return multiSelect.value().stream().map(value -> value.name().toLowerCase(Locale.ROOT))
                    .sorted().collect(Collectors.joining(","));
        }
        if (setting instanceof RangeSetting rangeSetting) {
            NumberRange range = rangeSetting.value();
            return NumberSettingText.format(range.minimum()) + ".." + NumberSettingText.format(range.maximum());
        }
        if (setting instanceof KeybindSetting keybindSetting) {
            return formatKeybind(keybindSetting.value());
        }
        throw new IllegalArgumentException("Setting does not have a compact text editor: " + setting.id());
    }

    public static boolean tryApply(Setting<?> setting, String text) {
        if (setting == null || text == null) {
            return false;
        }
        try {
            if (setting instanceof NumberSetting numberSetting) {
                return NumberSettingText.tryApply(numberSetting, text);
            }
            if (setting instanceof IntegerSetting integerSetting) {
                integerSetting.set(Integer.parseInt(text));
                return true;
            }
            if (setting instanceof ColorSetting colorSetting) {
                return ColorSettingText.tryApply(colorSetting, text);
            }
            if (setting instanceof StringSetting stringSetting) {
                return StringSettingText.tryApply(stringSetting, text);
            }
            if (setting instanceof RegexSetting regexSetting) {
                regexSetting.set(text);
                return true;
            }
            if (setting instanceof StringListSetting stringList) {
                stringList.set(split(text, ";"));
                return true;
            }
            if (setting instanceof IdentifierSelectorSetting selector) {
                selector.set(split(text, ";"));
                return true;
            }
            if (setting instanceof MultiSelectEnumSetting<?> multiSelect) {
                applyMultiSelect(multiSelect, text);
                return true;
            }
            if (setting instanceof RangeSetting rangeSetting) {
                rangeSetting.set(parseRange(text));
                return true;
            }
            if (setting instanceof KeybindSetting keybindSetting) {
                keybindSetting.set(parseKeybind(text));
                return true;
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return false;
    }

    public static int maximumLength(Setting<?> setting) {
        if (setting instanceof StringSetting stringSetting) {
            return stringSetting.maximumLength();
        }
        if (setting instanceof RegexSetting regexSetting) {
            return regexSetting.maximumLength();
        }
        if (setting instanceof StringListSetting stringList) {
            return Math.min(512, stringList.maximumEntries() * (stringList.maximumEntryLength() + 1));
        }
        if (setting instanceof IdentifierSelectorSetting selector) {
            return Math.min(512, selector.maximumEntries() * 65);
        }
        return 128;
    }

    private static List<String> split(String text, String separator) {
        if (text.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(text.split(separator, -1)).map(String::trim).toList();
    }

    private static NumberRange parseRange(String text) {
        String[] values = text.split("\\.\\.", -1);
        if (values.length != 2) {
            throw new IllegalArgumentException("Range must use minimum..maximum");
        }
        return new NumberRange(Double.parseDouble(values[0].trim()), Double.parseDouble(values[1].trim()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyMultiSelect(MultiSelectEnumSetting<?> setting, String text) {
        Class<? extends Enum> enumType = setting.allowedValues().iterator().next().getDeclaringClass();
        EnumSet values = EnumSet.noneOf(enumType);
        if (!text.isBlank()) {
            for (String token : text.split(",", -1)) {
                String normalized = token.trim();
                Enum value = Arrays.stream(enumType.getEnumConstants())
                        .filter(candidate -> candidate.name().equalsIgnoreCase(normalized))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Unknown enum token: " + normalized));
                values.add(value);
            }
        }
        ((MultiSelectEnumSetting) setting).set(Set.copyOf(values));
    }

    private static String formatKeybind(Keybind bind) {
        String modifiers = bind.modifiers().stream().sorted(Comparator.comparing(Enum::name))
                .map(value -> value.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining("+"));
        return bind.inputType().name().toLowerCase(Locale.ROOT) + ":" + bind.keyCode() + ":"
                + bind.activation().name().toLowerCase(Locale.ROOT) + (modifiers.isEmpty() ? "" : ":" + modifiers);
    }

    private static Keybind parseKeybind(String text) {
        String[] parts = text.trim().split(":", -1);
        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Keybind must use input:code:activation[:modifiers]");
        }
        Keybind.InputType type = switch (parts[0].trim().toLowerCase(Locale.ROOT)) {
            case "keyboard" -> Keybind.InputType.KEYBOARD;
            case "mouse", "mouse_button" -> Keybind.InputType.MOUSE_BUTTON;
            default -> throw new IllegalArgumentException("Unknown keybind input type");
        };
        int code = Integer.parseInt(parts[1].trim());
        Keybind.Activation activation = Keybind.Activation.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
        EnumSet<Keybind.Modifier> modifiers = EnumSet.noneOf(Keybind.Modifier.class);
        if (parts.length == 4 && !parts[3].isBlank()) {
            for (String token : parts[3].split("\\+", -1)) {
                modifiers.add(Keybind.Modifier.valueOf(token.trim().toUpperCase(Locale.ROOT)));
            }
        }
        return new Keybind(type, code, modifiers, activation);
    }
}
