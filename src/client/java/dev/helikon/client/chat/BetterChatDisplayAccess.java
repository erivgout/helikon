package dev.helikon.client.chat;

import dev.helikon.client.mixin.ChatComponentAccessor;
import dev.helikon.client.module.chat.BetterChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Thin Minecraft adapter for BetterChat's local display behavior. */
public final class BetterChatDisplayAccess {
    private static BetterChat betterChat;
    private static final WeakHashMap<ChatComponent, SmoothScrollState> scrollStates = new WeakHashMap<>();
    private static boolean applyingSmoothScroll;
    private static boolean rescaleRequested;
    private static String previousStoredIdentity;

    private BetterChatDisplayAccess() {
    }

    public static void install(BetterChat module) {
        betterChat = module;
        module.setDisplayRefresh(BetterChatDisplayAccess::refreshHistoryLimit);
    }

    public static int historyLimit() {
        return betterChat == null ? BetterChatRenderPolicy.VANILLA_HISTORY_LIMIT : betterChat.historyLimit();
    }

    public static int totalLifetimeTicks() {
        return betterChat == null ? BetterChatRenderPolicy.VANILLA_VISIBLE_TICKS : betterChat.totalLifetimeTicks();
    }

    public static double fadeMultiplier() {
        return betterChat == null ? BetterChatRenderPolicy.VANILLA_FADE_MULTIPLIER : betterChat.fadeMultiplier();
    }

    public static double lineHeight(double vanillaLineHeight) {
        return betterChat == null ? vanillaLineHeight : betterChat.lineHeight(vanillaLineHeight);
    }

    /** Adds a local suggest-command action to a safe standard vanilla sender span. */
    public static GuiMessage decorateClickableNames(GuiMessage message) {
        if (betterChat == null || !betterChat.isEnabled() || !betterChat.clickableNames()) {
            return message;
        }
        TransformResult result = decorateClickableNames(message.content());
        if (!result.changed()) {
            return message;
        }
        return new GuiMessage(message.addedTime(), result.component(), message.signature(), message.source(), message.tag());
    }

    /** Processes a raw stored message exactly once, after its immediate display pass. */
    public static GuiMessage stackStoredMessage(ChatComponent chat, GuiMessage message) {
        if (betterChat == null || !betterChat.isEnabled()) {
            previousStoredIdentity = null;
            return message;
        }
        String identity = message.source().name() + '\u0000' + message.content().getString();
        boolean sameAsPrevious = identity.equals(previousStoredIdentity);
        ChatDuplicateTracker.Decision decision = betterChat.recordDuplicate(identity);
        previousStoredIdentity = identity;
        if (decision.collapsePrevious() && sameAsPrevious && removeLatest(chat, message)) {
            rescaleRequested = true;
        }
        if (!decision.appendCounter()) {
            return message;
        }
        // The immediate display path ran before this stored-message rewrite, even when no line is removed.
        rescaleRequested = true;
        Component counted = Component.empty()
                .append(message.content())
                .append(Component.literal(" [x" + decision.count() + "]").withColor(0xAAAAAA));
        return new GuiMessage(message.addedTime(), counted, message.signature(), message.source(), message.tag());
    }

    public static boolean consumeRescaleRequested() {
        boolean requested = rescaleRequested;
        rescaleRequested = false;
        return requested;
    }

    /** Queues a multi-line scroll for local easing, returning whether vanilla should skip its immediate jump. */
    public static boolean requestSmoothScroll(ChatComponent chat, int lines) {
        if (betterChat == null || !betterChat.isEnabled() || !betterChat.smoothScroll() || applyingSmoothScroll) {
            return false;
        }
        scrollStates.computeIfAbsent(chat, ignored -> new SmoothScrollState()).request(lines);
        return true;
    }

    /** Called from the client tick bridge to apply one bounded easing step per chat component. */
    public static void tickSmoothScroll() {
        if (betterChat == null || !betterChat.isEnabled() || !betterChat.smoothScroll()) {
            scrollStates.clear();
            return;
        }
        for (Map.Entry<ChatComponent, SmoothScrollState> entry : List.copyOf(scrollStates.entrySet())) {
            int step = entry.getValue().nextStep();
            if (step == 0) {
                continue;
            }
            applyingSmoothScroll = true;
            try {
                entry.getKey().scrollChat(step);
            } finally {
                applyingSmoothScroll = false;
            }
        }
    }

    /** Returns non-persistent raw local history, newest first, for a local command or test adapter. */
    public static List<String> localHistory() {
        ChatComponent chat = chatComponent();
        if (chat == null) {
            return List.of();
        }
        List<String> history = new ArrayList<>();
        for (GuiMessage message : ((ChatComponentAccessor) chat).helikon$allMessages()) {
            history.add(ChatDisplayAccess.originalContent(message).getString());
        }
        return List.copyOf(history);
    }

    private static boolean removeLatest(ChatComponent chat, GuiMessage incoming) {
        List<GuiMessage> messages = ((ChatComponentAccessor) chat).helikon$allMessages();
        if (messages.isEmpty() || messages.getFirst().source() != incoming.source()) {
            return false;
        }
        messages.removeFirst();
        return true;
    }

    private static void refreshHistoryLimit() {
        ChatComponent chat = chatComponent();
        if (chat == null) {
            return;
        }
        List<GuiMessage> messages = ((ChatComponentAccessor) chat).helikon$allMessages();
        while (messages.size() > historyLimit()) {
            messages.removeLast();
        }
        chat.rescaleChat();
    }

    private static ChatComponent chatComponent() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud == null) {
            return null;
        }
        return client.gui.hud.getChat();
    }

    private static TransformResult decorateClickableNames(Component source) {
        boolean changed = false;
        MutableComponent copy;
        if (source.getContents() instanceof TranslatableContents translatable
                && "chat.type.text".equals(translatable.getKey())
                && translatable.getArgs().length >= 2 && translatable.getArgs()[0] instanceof Component playerName) {
            Object[] arguments = translatable.getArgs().clone();
            Component clickableName = decoratePlayerName(playerName);
            changed = clickableName != playerName;
            arguments[0] = clickableName;
            copy = changed ? MutableComponent.create(new TranslatableContents(translatable.getKey(),
                    translatable.getFallback(), arguments)) : MutableComponent.create(source.getContents());
        } else {
            copy = MutableComponent.create(source.getContents());
        }
        copy.setStyle(source.getStyle());
        for (Component sibling : source.getSiblings()) {
            TransformResult child = decorateClickableNames(sibling);
            changed |= child.changed();
            copy.append(child.component());
        }
        return changed ? new TransformResult(copy, true) : new TransformResult(source, false);
    }

    private static Component decoratePlayerName(Component source) {
        String name = source.getString();
        Style style = source.getStyle();
        if (!ChatPlayerNamePolicy.isVanillaPlayerName(name) || style.getClickEvent() != null) {
            return source;
        }
        MutableComponent copy = MutableComponent.create(source.getContents());
        copy.setStyle(style.withClickEvent(new ClickEvent.SuggestCommand("/msg " + name + " ")));
        for (Component sibling : source.getSiblings()) {
            copy.append(sibling);
        }
        return copy;
    }

    private record TransformResult(Component component, boolean changed) {
    }
}
