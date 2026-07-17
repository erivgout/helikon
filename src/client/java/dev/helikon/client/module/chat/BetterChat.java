package dev.helikon.client.module.chat;

import dev.helikon.client.chat.BetterChatRenderPolicy;
import dev.helikon.client.chat.ChatDuplicateTracker;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Local retained-chat and display controls; adapters own all Minecraft access. */
public final class BetterChat extends Module {
    private final NumberSetting historyLimit;
    private final BooleanSetting clickableNames;
    private final BooleanSetting stackDuplicates;
    private final BooleanSetting messageCounters;
    private final NumberSetting visibilitySeconds;
    private final NumberSetting fadeSeconds;
    private final BooleanSetting compactMode;
    private final BooleanSetting smoothScroll;
    private final ChatDuplicateTracker duplicateTracker = new ChatDuplicateTracker();
    private Runnable displayRefresh = () -> {
    };

    public BetterChat() {
        super("better_chat", "BetterChat", "Expands and locally improves the Minecraft chat display.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        historyLimit = addSetting(new NumberSetting("history_limit", "History limit",
                "Maximum locally retained chat messages while BetterChat is enabled.", 500.0D, 100.0D, 2_000.0D));
        clickableNames = addSetting(new BooleanSetting("clickable_names", "Clickable names",
                "Make standard vanilla player names suggest a normal /msg command locally.", true));
        stackDuplicates = addSetting(new BooleanSetting("stack_duplicates", "Stack duplicates",
                "Collapse immediately consecutive identical local chat lines.", true));
        messageCounters = addSetting(new BooleanSetting("message_counters", "Message counters",
                "Append a local [xN] counter to repeated lines.", true));
        visibilitySeconds = addSetting(new NumberSetting("visibility_seconds", "Visibility seconds",
                "How long unfocused local chat remains visible before its fade.", 10.0D, 1.0D, 600.0D));
        fadeSeconds = addSetting(new NumberSetting("fade_seconds", "Fade seconds",
                "Length of the local unfocused-chat fade near the visibility limit.", 1.0D, 0.05D, 60.0D));
        compactMode = addSetting(new BooleanSetting("compact_mode", "Compact mode",
                "Use a smaller local chat line-height base.", false));
        smoothScroll = addSetting(new BooleanSetting("smooth_scroll", "Smooth scroll",
                "Ease multi-line local chat scroll requests over several client ticks.", true));
        settings().forEach(setting -> setting.addChangeListener(ignored -> displayRefresh.run()));
    }

    public int historyLimit() {
        return BetterChatRenderPolicy.historyLimit(isEnabled(), historyLimit.value());
    }

    public boolean clickableNames() {
        return clickableNames.value();
    }

    public ChatDuplicateTracker.Decision recordDuplicate(String identity) {
        return duplicateTracker.record(identity, stackDuplicates.value(), messageCounters.value());
    }

    public int totalLifetimeTicks() {
        return BetterChatRenderPolicy.totalLifetimeTicks(isEnabled(), visibilitySeconds.value(), fadeSeconds.value());
    }

    public double fadeMultiplier() {
        return BetterChatRenderPolicy.fadeMultiplier(isEnabled(), visibilitySeconds.value(), fadeSeconds.value());
    }

    public double lineHeight(double vanillaLineHeight) {
        return BetterChatRenderPolicy.lineHeight(isEnabled(), compactMode.value(), vanillaLineHeight);
    }

    public boolean smoothScroll() {
        return smoothScroll.value();
    }

    /** Installs a thin display adapter callback without bringing Minecraft types into this module. */
    public void setDisplayRefresh(Runnable displayRefresh) {
        this.displayRefresh = Objects.requireNonNull(displayRefresh, "displayRefresh");
    }

    @Override
    protected void onEnable() {
        duplicateTracker.reset();
        displayRefresh.run();
    }

    @Override
    protected void onDisable() {
        duplicateTracker.reset();
        displayRefresh.run();
    }
}
