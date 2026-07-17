package dev.helikon.client.hud;

import dev.helikon.client.module.ModuleTimingMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Minecraft-free local formatting and paging policy for the opt-in diagnostics overlay. */
public final class DebugOverlayLines {
    public static final int MODULES_PER_PAGE = 10;

    private DebugOverlayLines() {
    }

    public static List<String> format(List<ModuleTimingMetrics.Snapshot> snapshots, int requestedPage,
                                      int blockEspCacheSize, int storageEspCacheSize, int subscriberCount,
                                      String globalSaveState) {
        Objects.requireNonNull(snapshots, "snapshots");
        Objects.requireNonNull(globalSaveState, "globalSaveState");
        if (requestedPage < 1 || blockEspCacheSize < 0 || storageEspCacheSize < 0 || subscriberCount < 0) {
            throw new IllegalArgumentException("Diagnostic counters and page must not be negative");
        }
        int pageCount = Math.max(1, (snapshots.size() + MODULES_PER_PAGE - 1) / MODULES_PER_PAGE);
        int page = Math.min(requestedPage, pageCount);
        int start = (page - 1) * MODULES_PER_PAGE;
        int end = Math.min(start + MODULES_PER_PAGE, snapshots.size());
        List<String> lines = new ArrayList<>(end - start + 4);
        lines.add("Helikon Debug " + page + "/" + pageCount);
        for (int index = start; index < end; index++) {
            ModuleTimingMetrics.Snapshot timing = snapshots.get(index);
            lines.add(String.format(Locale.ROOT, "%s T %.3fms R %.3fms", timing.moduleId(),
                    timing.tickNanos() / 1_000_000.0D, timing.renderNanos() / 1_000_000.0D));
        }
        lines.add("Caches block-esp=" + blockEspCacheSize + " storage-esp=" + storageEspCacheSize);
        lines.add("Event subscribers: " + subscriberCount);
        lines.add("Global config save: " + globalSaveState);
        return List.copyOf(lines);
    }
}
