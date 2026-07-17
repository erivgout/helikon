package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** One-shot stable sort and exact-address deduplication policy. */
public final class ServerCleanUp extends Module {
    private boolean pending;

    public ServerCleanUp() {
        super("server_cleanup", "ServerCleanUp",
                "Sorts the local multiplayer list by name and removes exact duplicate addresses.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    @Override
    protected void onEnable() {
        pending = true;
    }

    @Override
    protected void onDisable() {
        pending = false;
    }

    public boolean consumeRunRequest() {
        if (!isEnabled() || !pending) {
            return false;
        }
        pending = false;
        return true;
    }

    public List<Entry> clean(List<Entry> entries) {
        LinkedHashMap<String, Entry> unique = new LinkedHashMap<>();
        for (Entry entry : entries) {
            unique.putIfAbsent(entry.address().toLowerCase(Locale.ROOT), entry);
        }
        List<Entry> sorted = new ArrayList<>(unique.values());
        sorted.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Entry::address, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(sorted);
    }

    public record Entry(String name, String address, int originalIndex) {
    }
}
