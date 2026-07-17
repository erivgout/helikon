package dev.helikon.client.module.miscellaneous;

import java.util.List;

/** Narrow bridge for the vanilla book editor mixin. */
public final class BookHackAccess {
    private static volatile BookHack module;

    private BookHackAccess() {
    }

    public static void install(BookHack bookHack) {
        module = bookHack;
    }

    public static List<String> paginate(String clipboard) {
        BookHack current = module;
        return current == null ? List.of() : current.paginate(clipboard);
    }
}
