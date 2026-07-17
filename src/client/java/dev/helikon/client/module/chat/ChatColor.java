package dev.helikon.client.module.chat;

import dev.helikon.client.chat.ChatColorPolicy;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Local chat palette; rendering remains in the narrow chat-display adapter. */
public final class ChatColor extends Module {
    private final ColorSetting normalMessageColor;
    private final ColorSetting playerNameColor;
    private final ColorSetting timestampColor;
    private final ColorSetting mentionColor;
    private final ColorSetting systemMessageColor;
    private final ColorSetting privateMessageColor;
    private final NumberSetting backgroundOpacity;
    private final BooleanSetting textShadow;
    private Runnable displayRefresh = () -> {
    };

    public ChatColor() {
        super("chat_color", "ChatColor", "Applies a local palette to displayed chat lines.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        normalMessageColor = addSetting(new ColorSetting("normal_message_color", "Normal message color",
                "Fallback ARGB color for ordinary local player-chat text.", 0xFFFFFFFF));
        playerNameColor = addSetting(new ColorSetting("player_name_color", "Player name color",
                "Local ARGB color for the standard vanilla <player> chat span.", 0xFF55FFFF));
        timestampColor = addSetting(new ColorSetting("timestamp_color", "Timestamp color",
                "Local ARGB color applied to ChatTimestamps labels.", 0xFF808080));
        mentionColor = addSetting(new ColorSetting("mention_color", "Mention color",
                "Fallback ARGB color for a line mentioning the local player name.", 0xFFFFFF55));
        systemMessageColor = addSetting(new ColorSetting("system_message_color", "System message color",
                "Fallback ARGB color for server-system chat lines.", 0xFFFFAA00));
        privateMessageColor = addSetting(new ColorSetting("private_message_color", "Private message color",
                "Fallback ARGB color for conservatively recognized private-message lines.", 0xFFFF55FF));
        backgroundOpacity = addSetting(new NumberSetting("background_opacity", "Background opacity",
                "Local chat-background opacity multiplier.", 1.0, 0.0, 1.0));
        textShadow = addSetting(new BooleanSetting("text_shadow", "Text shadow",
                "Retain Minecraft's local chat text shadow.", true));
        settings().forEach(setting -> setting.addChangeListener(ignored -> displayRefresh.run()));
    }

    public int rgbColor(ChatColorPolicy.MessageType type) {
        return switch (type) {
            case NORMAL -> rgb(normalMessageColor.value());
            case SYSTEM -> rgb(systemMessageColor.value());
            case MENTION -> rgb(mentionColor.value());
            case PRIVATE_MESSAGE -> rgb(privateMessageColor.value());
        };
    }

    public int timestampRgbColor() {
        return rgb(timestampColor.value());
    }

    public int playerNameRgbColor() {
        return rgb(playerNameColor.value());
    }

    public float backgroundOpacity() {
        return backgroundOpacity.value().floatValue();
    }

    /** Applies the configured local multiplier to a vanilla chat-background alpha. */
    public float adjustBackgroundOpacity(float vanillaOpacity) {
        return vanillaOpacity * backgroundOpacity();
    }

    public boolean textShadow() {
        return textShadow.value();
    }

    /** Installs the thin Minecraft display refresh adapter without coupling this module to it. */
    public void setDisplayRefresh(Runnable displayRefresh) {
        this.displayRefresh = Objects.requireNonNull(displayRefresh, "displayRefresh");
        this.displayRefresh.run();
    }

    @Override
    protected void onEnable() {
        displayRefresh.run();
    }

    @Override
    protected void onDisable() {
        displayRefresh.run();
    }

    private static int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }
}
