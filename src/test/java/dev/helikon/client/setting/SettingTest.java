package dev.helikon.client.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.helikon.client.input.Keybind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingTest {
    @Test
    void numberSettingRejectsValuesOutsideItsInclusiveRange() {
        NumberSetting setting = new NumberSetting("speed", "Speed", "Test speed.", 1.0, 0.0, 2.0);

        setting.set(1.5);

        assertEquals(1.5, setting.value());
        assertThrows(IllegalArgumentException.class, () -> setting.set(2.1));
        assertThrows(IllegalArgumentException.class, () -> setting.set(Double.NaN));
    }

    @Test
    void malformedJsonRestoresTheDefaultValue() {
        NumberSetting setting = new NumberSetting("speed", "Speed", "Test speed.", 1.0, 0.0, 2.0);
        setting.set(1.5);

        assertFalse(setting.applyJson(new JsonPrimitive(99.0)));
        assertEquals(1.0, setting.value());
    }

    @Test
    void booleanSettingRoundTripsItsValue() {
        BooleanSetting setting = new BooleanSetting("visible", "Visible", "Test visibility.", true);

        assertTrue(setting.applyJson(new JsonPrimitive(false)));
        assertFalse(setting.value());
        assertFalse(setting.applyJson(new JsonPrimitive("false")));
        assertTrue(setting.value());
    }

    @Test
    void colorSettingUsesStrictArgbTokensAndRecoversMalformedJson() {
        ColorSetting setting = new ColorSetting("color", "Color", "Test color.", 0xFF112233);

        assertTrue(setting.applyJson(new JsonPrimitive("#80445566")));
        assertEquals(0x80445566, setting.value());
        assertEquals("#80445566", ColorSettingText.format(setting.value()));

        assertFalse(setting.applyJson(new JsonPrimitive("#445566")));
        assertEquals(0xFF112233, setting.value());
    }

    @Test
    void enumSettingUsesStableTokensCyclesAndRecoversMalformedJson() {
        EnumSetting<TestMode> setting = new EnumSetting<>("mode", "Mode", "Test mode.",
                TestMode.class, TestMode.FIRST);

        assertTrue(setting.trySet("second"));
        assertEquals(TestMode.SECOND, setting.value());
        assertEquals("second", setting.valueId());
        setting.cycle();
        assertEquals(TestMode.FIRST, setting.value());

        assertFalse(setting.applyJson(new JsonPrimitive("missing")));
        assertEquals(TestMode.FIRST, setting.value());
    }

    @Test
    void stringSettingBoundsTextAndRecoversMalformedJson() {
        StringSetting setting = new StringSetting("foods", "Foods", "Test text.", "", 8, true);

        assertTrue(StringSettingText.tryApply(setting, "bread"));
        assertEquals("bread", setting.value());
        assertFalse(StringSettingText.tryApply(setting, "cooked_beef"));
        assertEquals("bread", setting.value());
        assertFalse(setting.applyJson(new JsonPrimitive(12)));
        assertEquals("", setting.value());
    }

    @Test
    void integerAndRangeSettingsRejectFractionalOrOutOfBoundsJson() {
        IntegerSetting integer = new IntegerSetting("count", "Count", "Test count.", 2, 0, 4);
        assertTrue(integer.applyJson(new JsonPrimitive(4)));
        assertEquals(4, integer.value());
        assertFalse(integer.applyJson(new JsonPrimitive(2.5)));
        assertEquals(2, integer.value());

        RangeSetting range = new RangeSetting("range", "Range", "Test range.",
                new NumberRange(2.0, 4.0), 0.0, 10.0);
        JsonObject valid = new JsonObject();
        valid.addProperty("minimum", 1.0);
        valid.addProperty("maximum", 5.0);
        assertTrue(range.applyJson(valid));
        assertEquals(new NumberRange(1.0, 5.0), range.value());

        JsonObject invalid = new JsonObject();
        invalid.addProperty("minimum", 7.0);
        invalid.addProperty("maximum", 6.0);
        assertFalse(range.applyJson(invalid));
        assertEquals(new NumberRange(2.0, 4.0), range.value());
    }

    @Test
    void keybindSettingRoundTripsAndRejectsInvalidKeyJson() {
        Keybind defaultBind = new Keybind(82, Keybind.Activation.HOLD);
        KeybindSetting setting = new KeybindSetting("action", "Action", "Test bind.", defaultBind);
        JsonObject valid = new JsonObject();
        valid.addProperty("key", 65);
        valid.addProperty("activation", "press_once");

        assertTrue(setting.applyJson(valid));
        assertEquals(new Keybind(65, Keybind.Activation.PRESS_ONCE), setting.value());
        assertEquals("press_once", setting.toJson().getAsJsonObject().get("activation").getAsString());

        valid.addProperty("key", 999_999);
        assertFalse(setting.applyJson(valid));
        assertEquals(defaultBind, setting.value());
    }

    @Test
    void listAndSelectorSettingsDefensivelyCopyAndValidateTokens() {
        List<String> source = new ArrayList<>(List.of("bread"));
        StringListSetting list = new StringListSetting("entries", "Entries", "Test entries.", source, 2, 8, false);
        source.set(0, "changed");
        assertEquals(List.of("bread"), list.value());
        assertThrows(UnsupportedOperationException.class, () -> list.value().add("new"));

        JsonArray invalidList = new JsonArray();
        invalidList.add("one");
        invalidList.add("two");
        invalidList.add("three");
        assertFalse(list.applyJson(invalidList));
        assertEquals(List.of("bread"), list.value());

        BlockSelectorSetting blocks = new BlockSelectorSetting("blocks", "Blocks", "Test blocks.",
                List.of("minecraft:stone"), 3);
        blocks.set(List.of("Minecraft:Glass", "minecraft:stone"));
        assertEquals(List.of("minecraft:glass", "minecraft:stone"), blocks.value());
        assertThrows(IllegalArgumentException.class, () -> blocks.set(List.of("minecraft:stone", "Minecraft:Stone")));

        ItemSelectorSetting items = new ItemSelectorSetting("items", "Items", "Test items.", List.of(), 1);
        EntitySelectorSetting entities = new EntitySelectorSetting("entities", "Entities", "Test entities.", List.of(), 1);
        assertTrue(items.applyJson(singleTokenArray("minecraft:apple")));
        assertTrue(entities.applyJson(singleTokenArray("minecraft:zombie")));
        assertFalse(entities.applyJson(singleTokenArray("invalid token")));
        assertEquals(List.of(), entities.value());
    }

    @Test
    void multiSelectAndRegexSettingsUseStableSafeTokens() {
        MultiSelectEnumSetting<TestMode> modes = new MultiSelectEnumSetting<>("modes", "Modes", "Test modes.",
                TestMode.class, Set.of(TestMode.FIRST));
        JsonArray selected = new JsonArray();
        selected.add("second");
        selected.add("first");
        assertTrue(modes.applyJson(selected));
        assertEquals(Set.of(TestMode.FIRST, TestMode.SECOND), modes.value());
        assertEquals("first", modes.toJson().getAsJsonArray().get(0).getAsString());

        selected.add("first");
        assertFalse(modes.applyJson(selected));
        assertEquals(Set.of(TestMode.FIRST), modes.value());

        RegexSetting expression = new RegexSetting("pattern", "Pattern", "Test pattern.", "foo.*", 32, false);
        assertTrue(expression.applyJson(new JsonPrimitive("bar[0-9]+")));
        assertFalse(expression.applyJson(new JsonPrimitive("(a+)+")));
        assertEquals("foo.*", expression.value());
        assertFalse(expression.applyJson(new JsonPrimitive("((a))\\2")));
        assertEquals("foo.*", expression.value());
        assertFalse(expression.applyJson(new JsonPrimitive("(?<word>a+)\\k<word>")));
        assertEquals("foo.*", expression.value());
    }

    @Test
    void settingsExposeOptionalVisibilityAndNotifyListenersForValidatedChanges() {
        AtomicInteger changes = new AtomicInteger();
        BooleanSetting switchSetting = new BooleanSetting("switch", "Switch", "Controls visibility.", false);
        NumberSetting conditional = new NumberSetting("conditional", "Conditional", "Visible after switch.",
                1.0, 0.0, 2.0, switchSetting::value);
        conditional.addChangeListener(ignored -> changes.incrementAndGet());

        assertFalse(conditional.isVisible());
        switchSetting.set(true);
        assertTrue(conditional.isVisible());
        conditional.set(1.5);
        assertEquals(1, changes.get());
    }

    private static JsonArray singleTokenArray(String token) {
        JsonArray array = new JsonArray();
        array.add(token);
        return array;
    }

    private enum TestMode {
        FIRST,
        SECOND
    }
}
