package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.ArrayList;
import java.util.List;

/** Protocol-safe bulk clipboard pagination for the vanilla book editor. */
public final class BookHack extends Module {
    public static final int PAGE_LENGTH = 1024;
    public static final int MAXIMUM_PAGES = 100;

    public BookHack() {
        super("book_hack", "BookHack",
                "Adds Ctrl+Shift+V bulk clipboard pagination to the vanilla writable-book editor.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    public List<String> paginate(String clipboard) {
        if (!isEnabled() || clipboard == null || clipboard.isEmpty()) {
            return List.of();
        }
        String normalized = clipboard.replace("\r\n", "\n").replace('\r', '\n');
        List<String> pages = new ArrayList<>();
        for (int offset = 0; offset < normalized.length() && pages.size() < MAXIMUM_PAGES; offset += PAGE_LENGTH) {
            pages.add(normalized.substring(offset, Math.min(normalized.length(), offset + PAGE_LENGTH)));
        }
        return List.copyOf(pages);
    }
}
