package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyInterfaceModulesTest {
    @Test
    void bookImportIsProtocolBounded() {
        BookHack book = new BookHack();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(book);
        assertTrue(book.paginate("text").isEmpty());
        registry.setEnabled(book, true);
        List<String> pages = book.paginate("x".repeat(200_000));
        assertEquals(100, pages.size());
        assertTrue(pages.stream().allMatch(page -> page.length() <= 1024));
    }

    @Test
    void serverCleanupDeduplicatesAndSortsDeterministically() {
        ServerCleanUp module = new ServerCleanUp();
        List<ServerCleanUp.Entry> cleaned = module.clean(List.of(
                new ServerCleanUp.Entry("Zulu", "same.example", 0),
                new ServerCleanUp.Entry("Alpha", "other.example", 1),
                new ServerCleanUp.Entry("Duplicate", "SAME.EXAMPLE", 2)));
        assertEquals(List.of("Alpha", "Zulu"), cleaned.stream().map(ServerCleanUp.Entry::name).toList());
        assertEquals(List.of(1, 0), cleaned.stream().map(ServerCleanUp.Entry::originalIndex).toList());
    }

    @Test
    void changelogIsAOneShotEnableRequest() {
        Changelog module = new Changelog();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        assertFalse(module.consumeOpenRequest());
        registry.setEnabled(module, true);
        assertTrue(module.consumeOpenRequest());
        assertFalse(module.consumeOpenRequest());
    }
}
