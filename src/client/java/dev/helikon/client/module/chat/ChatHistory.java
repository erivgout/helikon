package dev.helikon.client.module.chat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Keeps bounded local chat records, with optional local per-server persistence. */
public final class ChatHistory extends Module {
    private final NumberSetting historyLimit;
    private final BooleanSetting persistentLogging;
    private final NumberSetting retentionDays;
    private Runnable enabledHook = () -> { };
    private Runnable disabledHook = () -> { };
    private Runnable settingsChangedHook = () -> { };

    public ChatHistory() {
        super("chat_history", "ChatHistory", "Keeps searchable local chat history with optional local logs.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        historyLimit = addSetting(new NumberSetting("history_limit", "History limit",
                "Maximum locally retained chat entries.", 500.0D, 100.0D, 2_000.0D));
        persistentLogging = addSetting(new BooleanSetting("persistent_logging", "Persistent logging",
                "Store retained chat locally by server. Disabled by default.", false));
        retentionDays = addSetting(new NumberSetting("retention_days", "Retention days",
                "Delete persisted entries older than this many days on load or save.", 30.0D, 1.0D, 365.0D));
        settings().forEach(setting -> setting.addChangeListener(ignored -> settingsChangedHook.run()));
    }

    public int historyLimit() {
        return (int) Math.round(historyLimit.value());
    }

    public boolean persistentLogging() {
        return persistentLogging.value();
    }

    public int retentionDays() {
        return (int) Math.round(retentionDays.value());
    }

    /** Installs thin local-storage callbacks without bringing configuration classes into the module policy. */
    public void setStorageHooks(Runnable enabled, Runnable disabled, Runnable settingsChanged) {
        enabledHook = Objects.requireNonNull(enabled, "enabled");
        disabledHook = Objects.requireNonNull(disabled, "disabled");
        settingsChangedHook = Objects.requireNonNull(settingsChanged, "settingsChanged");
    }

    @Override
    protected void onEnable() {
        enabledHook.run();
    }

    @Override
    protected void onDisable() {
        disabledHook.run();
    }
}
