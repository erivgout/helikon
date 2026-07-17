package dev.helikon.client.chat;

import dev.helikon.client.module.chat.ChatTimestamps;
import dev.helikon.client.module.chat.ChatColor;
import dev.helikon.client.mixin.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.WeakHashMap;

/** Thin Minecraft display adapter for optional local chat decorations. */
public final class ChatDisplayAccess {
    private static ChatTimestamps timestamps;
    private static ChatColor colors;
    /**
     * Keeps the uncolored local display component for each currently retained message.
     * Weak keys ensure this bridge cannot extend a chat line's lifetime.
     */
    private static final WeakHashMap<Component, DisplayOrigin> displayOrigins = new WeakHashMap<>();
    private static final Deque<String> pendingHighlights = new ArrayDeque<>();
    private static final int MAX_PENDING_HIGHLIGHTS = 64;

    private ChatDisplayAccess() {
    }

    public static void install(ChatTimestamps chatTimestamps) {
        timestamps = Objects.requireNonNull(chatTimestamps, "chatTimestamps");
    }

    public static void install(ChatColor chatColors) {
        colors = Objects.requireNonNull(chatColors, "chatColors");
        chatColors.setDisplayRefresh(ChatDisplayAccess::refreshChatDisplay);
    }

    public static Component decorateTimestamp(Component message) {
        if (timestamps == null || !timestamps.isEnabled()) {
            return message;
        }
        return Component.empty()
                .append(Component.literal(timestamps.label(Instant.now(), ZoneId.systemDefault()))
                        .withColor(timestamps.rgbColor()))
                .append(message);
    }

    /** Queues one bounded local highlight for the next matching Minecraft chat-display line. */
    public static void queueHighlight(String displayText) {
        if (displayText == null || displayText.isBlank()) {
            return;
        }
        pendingHighlights.addLast(displayText);
        while (pendingHighlights.size() > MAX_PENDING_HIGHLIGHTS) {
            pendingHighlights.removeFirst();
        }
    }

    /** Rebuilds a display message after Minecraft has already logged its original content. */
    public static GuiMessage decorateTimestamp(GuiMessage message) {
        DisplayOrigin origin = displayOrigins.get(message.content());
        Component original = origin == null ? message.content() : origin.original();
        Component timestamped = origin == null ? decorateTimestamp(original) : origin.timestamped();
        Component decorated = decorateDisplay(message, original, timestamped);
        if (pendingHighlights.removeFirstOccurrence(original.getString())) {
            decorated = Component.empty().append(decorated).withStyle(ChatFormatting.YELLOW);
        }
        if (decorated == message.content()) {
            return message;
        }
        displayOrigins.put(decorated, new DisplayOrigin(original, timestamped));
        return new GuiMessage(message.addedTime(), decorated, message.signature(), message.source(), message.tag());
    }

    /** Returns the original received component for a currently retained locally decorated line. */
    public static Component originalContent(GuiMessage message) {
        DisplayOrigin origin = displayOrigins.get(message.content());
        return origin == null ? message.content() : origin.original();
    }

    /** Applies ChatColor's local opacity multiplier to the vanilla chat background alpha. */
    public static float backgroundOpacity(float vanillaOpacity) {
        return colors != null && colors.isEnabled() ? colors.adjustBackgroundOpacity(vanillaOpacity) : vanillaOpacity;
    }

    /**
     * Applies the configured whole-line fallback and the standard vanilla player-name span.
     *
     * <p>Only the ordinary {@code chat.type.text} translation has a reliably structured
     * sender argument. Custom server formats retain their original component structure and
     * receive the line fallback only.</p>
     */
    static Component decorateColors(Component source, ChatColorPolicy.MessageType type, ChatColor chatColors) {
        return decorateColors(source, type, chatColors, false);
    }

    static Component decorateColors(Component source, ChatColorPolicy.MessageType type, ChatColor chatColors,
                                   boolean hasTimestamp) {
        Component fallback = applyFallbackColor(source, chatColors.rgbColor(type), chatColors.textShadow());
        Component playerName = applyStandardPlayerNameColor(fallback, chatColors.playerNameRgbColor(),
                chatColors.textShadow());
        return hasTimestamp ? recolorTimestamp(playerName, chatColors.timestampRgbColor(), chatColors.textShadow())
                : playerName;
    }

    private static Component applyFallbackColor(Component source, int color, boolean textShadow) {
        MutableComponent copy = MutableComponent.create(source.getContents());
        Style style = source.getStyle();
        if (style.getColor() == null) {
            style = style.withColor(color);
        }
        copy.setStyle(textShadow ? style : style.withoutShadow());
        for (Component sibling : source.getSiblings()) {
            copy.append(applyFallbackColor(sibling, color, textShadow));
        }
        return copy;
    }

    private static Component applyStandardPlayerNameColor(Component source, int color, boolean textShadow) {
        TranslatableContents translatable = source.getContents() instanceof TranslatableContents value ? value : null;
        MutableComponent copy;
        if (translatable != null && "chat.type.text".equals(translatable.getKey())
                && translatable.getArgs().length >= 2 && translatable.getArgs()[0] instanceof Component playerName) {
            Object[] arguments = translatable.getArgs().clone();
            arguments[0] = applyFallbackColor(playerName, color, textShadow);
            copy = MutableComponent.create(new TranslatableContents(translatable.getKey(),
                    translatable.getFallback(), arguments));
        } else {
            copy = MutableComponent.create(source.getContents());
        }
        copy.setStyle(source.getStyle());
        for (Component sibling : source.getSiblings()) {
            copy.append(applyStandardPlayerNameColor(sibling, color, textShadow));
        }
        return copy;
    }

    private static Component recolorTimestamp(Component source, int color, boolean textShadow) {
        MutableComponent copy = MutableComponent.create(source.getContents());
        copy.setStyle(source.getStyle());
        boolean first = true;
        for (Component sibling : source.getSiblings()) {
            if (first) {
                copy.append(recolorRoot(sibling, color, textShadow));
                first = false;
            } else {
                copy.append(sibling);
            }
        }
        return copy;
    }

    private static Component recolorRoot(Component source, int color, boolean textShadow) {
        MutableComponent copy = MutableComponent.create(source.getContents());
        Style style = source.getStyle().withColor(color);
        copy.setStyle(textShadow ? style : style.withoutShadow());
        for (Component sibling : source.getSiblings()) {
            copy.append(sibling);
        }
        return copy;
    }

    private static Component decorateDisplay(GuiMessage message, Component original, Component timestamped) {
        if (colors == null || !colors.isEnabled()) {
            return timestamped;
        }
        Minecraft client = Minecraft.getInstance();
        String localPlayerName = client.player == null ? "" : client.player.getGameProfile().name();
        ChatColorPolicy.MessageType type = ChatColorPolicy.classify(original.getString(),
                message.source() == GuiMessageSource.SYSTEM_SERVER, localPlayerName);
        boolean hasTimestamp = timestamps != null && timestamps.isEnabled();
        return decorateColors(timestamped, type, colors, hasTimestamp);
    }

    /** Rebuilds retained local chat lines whenever ChatColor changes state or settings. */
    private static void refreshChatDisplay() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud == null) {
            return;
        }
        ChatComponent chat = client.gui.hud.getChat();
        List<GuiMessage> messages = ((ChatComponentAccessor) chat).helikon$allMessages();
        for (int index = 0; index < messages.size(); index++) {
            GuiMessage message = messages.get(index);
            DisplayOrigin origin = displayOrigins.get(message.content());
            if (origin == null) {
                origin = new DisplayOrigin(message.content(), message.content());
            }
            Component decorated = decorateDisplay(message, origin.original(), origin.timestamped());
            displayOrigins.remove(message.content());
            if (decorated != origin.original()) {
                displayOrigins.put(decorated, origin);
            }
            if (decorated != message.content()) {
                messages.set(index, new GuiMessage(message.addedTime(), decorated, message.signature(),
                        message.source(), message.tag()));
            }
        }
        chat.rescaleChat();
    }

    private record DisplayOrigin(Component original, Component timestamped) {
    }
}
