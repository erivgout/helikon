package dev.helikon.client.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.ChatColor;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.Setting;
import org.junit.jupiter.api.Test;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatColorPolicyTest {
    @Test
    void classifiesSystemPrivateMentionAndNormalLinesConservatively() {
        assertEquals(ChatColorPolicy.MessageType.SYSTEM,
                ChatColorPolicy.classify("Server restart", true, "Alice"));
        assertEquals(ChatColorPolicy.MessageType.PRIVATE_MESSAGE,
                ChatColorPolicy.classify("From Bob: hello", false, "Alice"));
        assertEquals(ChatColorPolicy.MessageType.MENTION,
                ChatColorPolicy.classify("Hi Alice!", false, "Alice"));
        assertEquals(ChatColorPolicy.MessageType.NORMAL,
                ChatColorPolicy.classify("Malice is here", false, "Alice"));
    }

    @Test
    void colorsTheStructuredVanillaPlayerNameWithoutFlatteningTheMessage() {
        ChatColor colors = enabledColors();
        Component sender = Component.literal("Bob");
        Component original = Component.translatable("chat.type.text", sender, Component.literal("hello"));

        Component decorated = ChatDisplayAccess.decorateColors(original, ChatColorPolicy.MessageType.NORMAL, colors);

        assertEquals(0xFFFFFF, decorated.getStyle().getColor().getValue());
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, decorated.getContents());
        Component coloredSender = assertInstanceOf(Component.class, contents.getArgs()[0]);
        assertNotSame(sender, coloredSender);
        assertEquals(0x55FFFF, coloredSender.getStyle().getColor().getValue());
        Component message = assertInstanceOf(Component.class, contents.getArgs()[1]);
        assertNull(message.getStyle().getColor());
    }

    @Test
    void leavesCustomFormatsStructuredWhileApplyingTheLineFallback() {
        ChatColor colors = enabledColors();
        Component sender = Component.literal("Bob");
        Component original = Component.translatable("server.custom.format", sender, Component.literal("hello"));

        Component decorated = ChatDisplayAccess.decorateColors(original, ChatColorPolicy.MessageType.MENTION, colors);

        assertEquals(0xFFFF55, decorated.getStyle().getColor().getValue());
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, decorated.getContents());
        assertEquals(sender, contents.getArgs()[0]);
    }

    @Test
    void appliesConfiguredOpacityAndDisablesTextShadowLocally() {
        ChatColor colors = enabledColors();
        setting(colors, "background_opacity", NumberSetting.class).set(0.25);
        setting(colors, "text_shadow", BooleanSetting.class).set(false);

        assertEquals(0.2F, colors.adjustBackgroundOpacity(0.8F), 0.0001F);
        Component decorated = ChatDisplayAccess.decorateColors(Component.literal("hello"),
                ChatColorPolicy.MessageType.NORMAL, colors);
        assertEquals(Style.NO_SHADOW, decorated.getStyle().getShadowColor());
    }

    @Test
    void overridesOnlyTheKnownTimestampSiblingWhenTheLocalPaletteIsEnabled() {
        ChatColor colors = enabledColors();
        setting(colors, "timestamp_color", ColorSetting.class).set(0xFF112233);
        Component timestamped = Component.empty()
                .append(Component.literal("[12:34] ").withColor(0x808080))
                .append(Component.literal("hello"));

        Component decorated = ChatDisplayAccess.decorateColors(timestamped, ChatColorPolicy.MessageType.NORMAL,
                colors, true);

        assertEquals(0x112233, decorated.getSiblings().getFirst().getStyle().getColor().getValue());
        assertEquals(0xFFFFFF, decorated.getStyle().getColor().getValue());
    }

    private static ChatColor enabledColors() {
        ChatColor colors = new ChatColor();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(colors);
        registry.setEnabled(colors, true);
        return colors;
    }

    private static <T extends Setting<?>> T setting(ChatColor colors, String id, Class<T> type) {
        return type.cast(colors.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow());
    }
}
