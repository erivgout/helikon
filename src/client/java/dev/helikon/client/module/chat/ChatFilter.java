package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.TextMatchRules;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.StringSetting;

/** Applies bounded local keyword, player, and regex hide rules to incoming text. */
public final class ChatFilter extends Module {
    private final StringSetting keywordFilters;
    private final StringSetting regexFilters;
    private final StringSetting playerFilters;
    private final BooleanSetting caseSensitive;
    private final BooleanSetting hideMatches;
    private final BooleanSetting highlightMatches;
    private final BooleanSetting soundMatches;
    private final BooleanSetting hudNotifications;

    public ChatFilter() {
        super("chat_filter", "ChatFilter", "Locally hides incoming text matching bounded filters.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        keywordFilters = addSetting(new StringSetting("keyword_filters", "Keyword filters",
                "Comma-separated local text fragments to match.", "", 255, true));
        regexFilters = addSetting(new StringSetting("regex_filters", "Regex filters",
                "Semicolon-separated bounded local regular expressions to match.", "", 255, true));
        playerFilters = addSetting(new StringSetting("player_filters", "Player filters",
                "Comma-separated local player names to match.", "", 255, true));
        caseSensitive = addSetting(new BooleanSetting("case_sensitive", "Case sensitive",
                "Use exact case when matching local text and player filters.", false));
        hideMatches = addSetting(new BooleanSetting("hide_matches", "Hide matches",
                "Hide matching messages locally instead of sending them to the chat HUD.", true));
        highlightMatches = addSetting(new BooleanSetting("highlight_matches", "Highlight matches",
                "Highlight a matching visible line locally in the chat HUD.", false));
        soundMatches = addSetting(new BooleanSetting("sound_matches", "Sound matches",
                "Play a local UI sound for a matching visible line.", false));
        hudNotifications = addSetting(new BooleanSetting("hud_notifications", "HUD notifications",
                "Post local Helikon feedback for a matching visible line.", false));
    }

    public boolean shouldHide(IncomingChatMessage message) {
        return evaluate(message).hide();
    }

    /** Returns all local presentation choices for one incoming message without Minecraft types. */
    public Decision evaluate(IncomingChatMessage message) {
        boolean matched = isEnabled() && matches(message);
        return new Decision(matched, matched && hideMatches.value(), matched && highlightMatches.value(),
                matched && soundMatches.value(), matched && hudNotifications.value());
    }

    public boolean matches(IncomingChatMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        return TextMatchRules.containsAny(message.text(), keywordFilters.value(), caseSensitive.value())
                || TextMatchRules.matchesRegex(message.text(), regexFilters.value(), caseSensitive.value())
                || TextMatchRules.containsAny(message.sender(), playerFilters.value(), caseSensitive.value());
    }

    public record Decision(boolean matched, boolean hide, boolean highlight, boolean sound, boolean hudNotification) {
    }
}
