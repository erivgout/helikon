package dev.helikon.client.setting;

import dev.helikon.client.input.Keybind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingTextTest {
    private enum Choice { ALPHA, BETA }

    @Test
    void appliesEveryCompactSettingRepresentationThroughValidation() {
        IntegerSetting integer = new IntegerSetting("integer", "Integer", "description", 2, 0, 10);
        StringListSetting strings = new StringListSetting("strings", "Strings", "description", List.of(), 3, 8, false);
        BlockSelectorSetting blocks = new BlockSelectorSetting("blocks", "Blocks", "description", List.of(), 3);
        MultiSelectEnumSetting<Choice> choices = new MultiSelectEnumSetting<>("choices", "Choices", "description",
                Choice.class, Set.of());
        RangeSetting range = new RangeSetting("range", "Range", "description", new NumberRange(1.0D, 2.0D), 0.0D, 10.0D);
        RegexSetting regex = new RegexSetting("regex", "Regex", "description", "", 32, true);
        KeybindSetting keybind = new KeybindSetting("keybind", "Keybind", "description", Keybind.unbound());

        assertTrue(SettingText.tryApply(integer, "7"));
        assertTrue(SettingText.tryApply(strings, "one;two"));
        assertTrue(SettingText.tryApply(blocks, "minecraft:stone;minecraft:dirt"));
        assertTrue(SettingText.tryApply(choices, "beta,alpha"));
        assertTrue(SettingText.tryApply(range, "3.5..6"));
        assertTrue(SettingText.tryApply(regex, "a+"));
        assertTrue(SettingText.tryApply(keybind, "mouse:3:hold:shift+alt"));

        assertEquals(7, integer.value());
        assertEquals(List.of("one", "two"), strings.value());
        assertEquals(List.of("minecraft:stone", "minecraft:dirt"), blocks.value());
        assertEquals(Set.of(Choice.ALPHA, Choice.BETA), choices.value());
        assertEquals(new NumberRange(3.5D, 6.0D), range.value());
        assertEquals("a+", regex.value());
        assertEquals(new Keybind(Keybind.InputType.MOUSE_BUTTON, 3,
                Set.of(Keybind.Modifier.SHIFT, Keybind.Modifier.ALT), Keybind.Activation.HOLD), keybind.value());
    }

    @Test
    void rejectsInvalidTextWithoutChangingTheLastValidValue() {
        RangeSetting range = new RangeSetting("range", "Range", "description", new NumberRange(1.0D, 2.0D), 0.0D, 10.0D);
        KeybindSetting keybind = new KeybindSetting("keybind", "Keybind", "description", Keybind.unbound());

        assertFalse(SettingText.tryApply(range, "8..2"));
        assertFalse(SettingText.tryApply(keybind, "keyboard:999:toggle"));
        assertEquals(new NumberRange(1.0D, 2.0D), range.value());
        assertEquals(Keybind.unbound(), keybind.value());
    }
}
