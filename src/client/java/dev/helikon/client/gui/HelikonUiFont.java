package dev.helikon.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * The bundled smooth UI font (Inter, SIL OFL) shared by the Helikon screens.
 * Glyph icons (stars, category symbols) should stay in the default font,
 * which covers them through its unifont fallback.
 */
public final class HelikonUiFont {
    public static final Style STYLE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.fromNamespaceAndPath("helikon", "ui")));

    private HelikonUiFont() {
    }

    public static Component ui(String text) {
        return Component.literal(text).setStyle(STYLE);
    }

    public static Component ui(Component text) {
        return text.copy().setStyle(STYLE);
    }

    public static int width(Font font, String text) {
        return font.width(ui(text));
    }

    /** The text styled in the UI font and truncated to fit {@code maxWidth}. */
    public static Component trim(Font font, String text, int maxWidth) {
        FormattedText fitted = font.substrByWidth(FormattedText.of(text, STYLE), Math.max(0, maxWidth));
        return ui(fitted.getString());
    }
}
