package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.TextMatchRules;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.StringSetting;

/** Locally hides selected incoming chat and game-message categories. */
public final class ChatMute extends Module {
    private final BooleanSetting globalChat;
    private final BooleanSetting systemMessages;
    private final BooleanSetting deathMessages;
    private final BooleanSetting advancementMessages;
    private final BooleanSetting joinLeaveMessages;
    private final BooleanSetting commandFeedback;
    private final BooleanSetting repeatedMessages;
    private final StringSetting customTextFilters;

    private String previousText = "";

    public ChatMute() {
        super("chat_mute", "ChatMute", "Locally hides selected incoming chat categories.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        globalChat = addSetting(new BooleanSetting("global_chat", "Global chat", "Hide normal player chat locally.", false));
        systemMessages = addSetting(new BooleanSetting("system_messages", "System messages", "Hide non-overlay game messages locally.", false));
        deathMessages = addSetting(new BooleanSetting("death_messages", "Death messages", "Hide translatable death messages locally.", false));
        advancementMessages = addSetting(new BooleanSetting("advancement_messages", "Advancement messages", "Hide advancement announcements locally.", false));
        joinLeaveMessages = addSetting(new BooleanSetting("join_leave_messages", "Join and leave", "Hide player join/leave messages locally.", false));
        commandFeedback = addSetting(new BooleanSetting("command_feedback", "Command feedback", "Hide translatable command feedback locally.", false));
        repeatedMessages = addSetting(new BooleanSetting("repeated_messages", "Repeated messages", "Hide consecutive duplicate text locally.", false));
        customTextFilters = addSetting(new StringSetting("custom_text_filters", "Custom text filters",
                "Comma-separated local text fragments to hide.", "", 255, true));
    }

    public boolean shouldHide(IncomingChatMessage message) {
        if (!isEnabled()) {
            return false;
        }
        boolean duplicate = !message.text().isBlank() && message.text().equals(previousText);
        previousText = message.text();
        return (message.channel() == IncomingChatMessage.Channel.CHAT && globalChat.value())
                || (message.channel() == IncomingChatMessage.Channel.GAME && !message.overlay() && systemMessages.value())
                || (message.isDeathMessage() && deathMessages.value())
                || (message.isAdvancementMessage() && advancementMessages.value())
                || (message.isJoinLeaveMessage() && joinLeaveMessages.value())
                || (message.isCommandFeedback() && commandFeedback.value())
                || (duplicate && repeatedMessages.value())
                || TextMatchRules.containsAny(message.text(), customTextFilters.value(), false);
    }

    @Override
    protected void onDisable() {
        previousText = "";
    }
}
