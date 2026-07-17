package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.LocalGlossary;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.StringSetting;

import java.util.Optional;

/** Displays offline translations for locally visible incoming chat; it never transmits source text. */
public final class LocalTranslator extends Module {
    private static final int MAXIMUM_TRANSLATABLE_TEXT_LENGTH = 256;

    private final StringSetting glossary;

    public LocalTranslator() {
        super("local_translator", "LocalTranslator", "Displays offline local translations for incoming chat.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        glossary = addSetting(new StringSetting("glossary", "Local glossary",
                "Semicolon-separated exact local mappings: source=translation.", "", 2_048, true));
    }

    /** Returns an offline translation only for locally visible non-overlay incoming messages. */
    public Optional<String> translate(IncomingChatMessage message) {
        if (!isEnabled() || message == null || message.overlay() || message.text().isBlank()
                || message.text().length() > MAXIMUM_TRANSLATABLE_TEXT_LENGTH) {
            return Optional.empty();
        }
        Optional<String> translated = LocalGlossary.translate(message.text(), glossary.value());
        return translated.map(String::trim).filter(value -> !value.isEmpty() && value.length() <= MAXIMUM_TRANSLATABLE_TEXT_LENGTH)
                .filter(value -> !value.equals(message.text()));
    }
}
