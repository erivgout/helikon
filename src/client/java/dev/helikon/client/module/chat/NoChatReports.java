package dev.helikon.client.module.chat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/**
 * Privacy-safe report protection: ordinary player chat is blocked before it
 * can be signed or sent. It does not spoof signatures or claim server support.
 */
public final class NoChatReports extends Module {
    public NoChatReports() {
        super("no_chat_reports", "NoChatReports",
                "Prevents ordinary player chat from being signed and sent while enabled.",
                ModuleCategory.CHAT, false, Keybind.unbound());
    }

    public boolean allowsOrdinaryChat(String message) {
        return !isEnabled() || message == null || message.startsWith(".");
    }
}
