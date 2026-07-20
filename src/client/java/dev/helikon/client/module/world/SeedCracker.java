package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Collects legitimate local slime observations and incrementally filters a
 * user-bounded range of Minecraft's lower 48-bit structure seed.
 */
public final class SeedCracker extends Module {
    private static final int MAXIMUM_TRACKED_SLIME_ENTITIES = 4_096;

    public enum State {
        DISABLED,
        COLLECTING,
        READY,
        SEARCHING,
        COMPLETE,
        SOLVED,
        ERROR
    }

    public enum EvidenceSource {
        AUTOMATIC_SLIME,
        MANUAL
    }

    private final BooleanSetting automaticSlimes;
    private final BooleanSetting revealSingleplayerSeed;
    private final IntegerSetting confirmationsPerChunk;
    private final IntegerSetting minimumObservations;
    private final IntegerSetting maximumObservations;
    private final BooleanSetting automaticSearch;
    private final StringSetting searchStart;
    private final IntegerSetting searchCount;
    private final IntegerSetting candidatesPerTick;
    private final IntegerSetting maximumResults;
    private final BooleanSetting showHud;
    private final BooleanSetting renderEvidence;
    private final IntegerSetting renderHeight;
    private final BooleanSetting alwaysOnTop;
    private final NumberSetting lineWidth;
    private final ColorSetting evidenceColor;
    private final ColorSetting evidenceFillColor;

    private final LinkedHashMap<ChunkCoordinate, Observation> observations = new LinkedHashMap<>();
    private final LinkedHashMap<ChunkCoordinate, Integer> pendingConfirmations = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> observedSlimeEntities = new LinkedHashSet<>();
    private final List<Long> candidates = new ArrayList<>();
    private Consumer<String> statusNotifier = ignored -> {
    };
    private State state = State.DISABLED;
    private String error = "";
    private Long fullWorldSeed;
    private long rangeStart;
    private long rangeCount;
    private long scannedCandidates;
    private long matchingCandidates;

    public SeedCracker() {
        super("seed_cracker", "SeedCracker",
                "Uses observed low-altitude slime chunks to filter a bounded 48-bit structure-seed range.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        automaticSlimes = addSetting(new BooleanSetting("automatic_slimes", "Automatic slimes",
                "Record distinct loaded slime entities observed below Y=40 in the Overworld.", true));
        revealSingleplayerSeed = addSetting(new BooleanSetting(
                "reveal_singleplayer_seed", "Reveal singleplayer seed",
                "Show the full seed of a locally owned integrated-server world.", true));
        confirmationsPerChunk = addSetting(new IntegerSetting(
                "confirmations_per_chunk", "Confirmations per chunk",
                "Distinct slime entities required before a chunk becomes evidence.", 1, 1, 8));
        minimumObservations = addSetting(new IntegerSetting(
                "minimum_observations", "Minimum observations",
                "Confirmed slime chunks required before candidate filtering begins.", 4, 1, 16));
        maximumObservations = addSetting(new IntegerSetting(
                "maximum_observations", "Maximum observations",
                "Hard cap for retained slime-chunk evidence in the current world session.", 64, 16, 256));
        automaticSearch = addSetting(new BooleanSetting("automatic_search", "Automatic search",
                "Start filtering the configured candidate range when enough evidence exists.", true));
        searchStart = addSetting(new StringSetting("search_start", "Search start",
                "First lower-48-bit candidate in decimal or 0x hexadecimal notation.", "0", 32, false));
        searchCount = addSetting(new IntegerSetting("search_count", "Search count",
                "Number of consecutive structure-seed candidates to test.", 1_000_000, 1, 100_000_000));
        candidatesPerTick = addSetting(new IntegerSetting(
                "candidates_per_tick", "Candidates per tick",
                "Maximum candidates tested per client tick.", 10_000, 1_000, 500_000));
        maximumResults = addSetting(new IntegerSetting("maximum_results", "Maximum results",
                "Maximum matching candidates retained for display.", 32, 1, 256));
        showHud = addSetting(new BooleanSetting("show_hud", "Show HUD",
                "Show collection and candidate-search progress while enabled.", true));
        renderEvidence = addSetting(new BooleanSetting("render_evidence", "Render evidence",
                "Outline confirmed slime-evidence chunks in the loaded world.", true));
        renderHeight = addSetting(new IntegerSetting("render_height", "Render height",
                "Vertical height of each evidence-chunk marker.", 64, 16, 384));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Draw evidence markers through terrain.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Evidence marker line width.", 1.0D, 0.5D, 4.0D));
        evidenceColor = addSetting(new ColorSetting("evidence_color", "Evidence color",
                "ARGB outline color for confirmed slime chunks.", 0xFF66BB6A));
        evidenceFillColor = addSetting(new ColorSetting("evidence_fill_color", "Evidence fill color",
                "ARGB fill color for confirmed slime chunks.", 0x2866BB6A));

        minimumObservations.addChangeListener(ignored -> invalidateSearch());
        confirmationsPerChunk.addChangeListener(ignored -> pendingConfirmations.clear());
        maximumObservations.addChangeListener(ignored -> {
            trimObservations();
            invalidateSearch();
        });
        searchStart.addChangeListener(ignored -> invalidateSearch());
        searchCount.addChangeListener(ignored -> invalidateSearch());
        maximumResults.addChangeListener(ignored -> trimCandidates());
    }

    public void setStatusNotifier(Consumer<String> statusNotifier) {
        this.statusNotifier = Objects.requireNonNull(statusNotifier, "statusNotifier");
    }

    public boolean observeSlime(UUID entityId, int chunkX, int chunkZ, long tick) {
        if (!isEnabled() || !automaticSlimes.value() || entityId == null || tick < 0L
                || !observedSlimeEntities.add(entityId)) {
            return false;
        }
        trimObservedEntities();
        ChunkCoordinate coordinate = new ChunkCoordinate(chunkX, chunkZ);
        if (observations.containsKey(coordinate)) {
            Observation existing = observations.get(coordinate);
            observations.put(coordinate, new Observation(
                    coordinate, existing.source(), existing.confirmations() + 1, existing.firstSeenTick()));
            return false;
        }
        int confirmations = pendingConfirmations.merge(coordinate, 1, Integer::sum);
        if (confirmations < confirmationsPerChunk.value()) {
            return false;
        }
        pendingConfirmations.remove(coordinate);
        addObservation(new Observation(coordinate, EvidenceSource.AUTOMATIC_SLIME, confirmations, tick));
        return true;
    }

    public boolean addManualObservation(int chunkX, int chunkZ) {
        ChunkCoordinate coordinate = new ChunkCoordinate(chunkX, chunkZ);
        if (observations.containsKey(coordinate)) {
            return false;
        }
        pendingConfirmations.remove(coordinate);
        addObservation(new Observation(coordinate, EvidenceSource.MANUAL, 1, 0L));
        return true;
    }

    public boolean removeObservation(int chunkX, int chunkZ) {
        boolean removed = observations.remove(new ChunkCoordinate(chunkX, chunkZ)) != null;
        if (removed) {
            invalidateSearch();
        }
        return removed;
    }

    public boolean requestSearch() {
        if (!isEnabled() || observations.size() < minimumObservations.value()) {
            state = isEnabled() ? State.COLLECTING : State.DISABLED;
            return false;
        }
        try {
            rangeStart = parseStructureSeed(searchStart.value());
            rangeCount = searchCount.value();
            scannedCandidates = 0L;
            matchingCandidates = 0L;
            candidates.clear();
            error = "";
            fullWorldSeed = null;
            state = State.SEARCHING;
            return true;
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
            return false;
        }
    }

    public void tick() {
        if (!isEnabled() || fullWorldSeed != null) {
            return;
        }
        if (observations.size() < minimumObservations.value()) {
            state = State.COLLECTING;
            return;
        }
        if (state == State.COLLECTING || state == State.READY) {
            state = State.READY;
            if (automaticSearch.value()) {
                requestSearch();
            }
        }
        if (state != State.SEARCHING) {
            return;
        }
        scanBatch();
    }

    public void revealLocalWorldSeed(long worldSeed) {
        if (!isEnabled() || !revealSingleplayerSeed.value() || Objects.equals(fullWorldSeed, worldSeed)) {
            return;
        }
        fullWorldSeed = worldSeed;
        candidates.clear();
        candidates.add(worldSeed & SeedSlimeMath.STRUCTURE_SEED_MASK);
        matchingCandidates = 1L;
        state = State.SOLVED;
        error = "";
        statusNotifier.accept("SeedCracker found the local world seed: " + worldSeed);
    }

    public void clearSession() {
        observations.clear();
        pendingConfirmations.clear();
        observedSlimeEntities.clear();
        candidates.clear();
        fullWorldSeed = null;
        rangeStart = 0L;
        rangeCount = 0L;
        scannedCandidates = 0L;
        matchingCandidates = 0L;
        error = "";
        state = isEnabled() ? State.COLLECTING : State.DISABLED;
    }

    public Snapshot snapshot() {
        return new Snapshot(state, observations.size(), minimumObservations.value(),
                scannedCandidates, rangeCount, matchingCandidates, List.copyOf(candidates),
                fullWorldSeed, error);
    }

    public List<Observation> observations() {
        return List.copyOf(observations.values());
    }

    public List<String> statusLines() {
        Snapshot snapshot = snapshot();
        List<String> lines = new ArrayList<>();
        lines.add("SeedCracker: " + title(snapshot.state().name()));
        if (snapshot.fullWorldSeed() != null) {
            lines.add("World seed: " + snapshot.fullWorldSeed());
            lines.add("Structure seed: " + formatStructureSeed(snapshot.candidates().getFirst()));
            return List.copyOf(lines);
        }
        lines.add("Slime chunks: " + snapshot.observations() + "/" + snapshot.minimumObservations());
        if (snapshot.state() == State.SEARCHING || snapshot.state() == State.COMPLETE) {
            lines.add("Scanned: " + snapshot.scannedCandidates() + "/" + snapshot.rangeCount());
            lines.add("Matches: " + snapshot.matchingCandidates());
        }
        if (!snapshot.candidates().isEmpty()) {
            lines.add("First: " + formatStructureSeed(snapshot.candidates().getFirst()));
        }
        if (!snapshot.error().isBlank()) {
            lines.add("Error: " + snapshot.error());
        }
        return List.copyOf(lines);
    }

    public boolean automaticSlimes() {
        return automaticSlimes.value();
    }

    public boolean revealSingleplayerSeed() {
        return revealSingleplayerSeed.value();
    }

    public boolean showHud() {
        return showHud.value();
    }

    public boolean renderEvidence() {
        return renderEvidence.value();
    }

    public int renderHeight() {
        return renderHeight.value();
    }

    public boolean alwaysOnTop() {
        return alwaysOnTop.value();
    }

    public float lineWidth() {
        return lineWidth.value().floatValue();
    }

    public int evidenceColor() {
        return evidenceColor.value();
    }

    public int evidenceFillColor() {
        return evidenceFillColor.value();
    }

    @Override
    protected void onEnable() {
        if (fullWorldSeed != null) {
            candidates.clear();
            candidates.add(fullWorldSeed & SeedSlimeMath.STRUCTURE_SEED_MASK);
            state = State.SOLVED;
            return;
        }
        state = observations.size() >= minimumObservations.value() ? State.READY : State.COLLECTING;
    }

    @Override
    protected void onDisable() {
        candidates.clear();
        fullWorldSeed = null;
        scannedCandidates = 0L;
        matchingCandidates = 0L;
        state = State.DISABLED;
    }

    private void scanBatch() {
        List<Observation> evidence = observations();
        long remaining = rangeCount - scannedCandidates;
        int batch = (int) Math.min(remaining, candidatesPerTick.value());
        for (int index = 0; index < batch; index++) {
            long candidate = (rangeStart + scannedCandidates) & SeedSlimeMath.STRUCTURE_SEED_MASK;
            if (SeedSlimeMath.matchesAll(candidate, evidence)) {
                matchingCandidates++;
                if (candidates.size() < maximumResults.value()) {
                    candidates.add(candidate);
                }
            }
            scannedCandidates++;
        }
        if (scannedCandidates >= rangeCount) {
            state = State.COMPLETE;
            statusNotifier.accept("SeedCracker search complete: " + matchingCandidates
                    + " matching structure-seed candidate(s).");
        }
    }

    private void addObservation(Observation observation) {
        observations.put(observation.coordinate(), observation);
        trimObservations();
        invalidateSearch();
        statusNotifier.accept("SeedCracker confirmed slime-chunk evidence at "
                + observation.coordinate().x() + ", " + observation.coordinate().z() + ".");
    }

    private void invalidateSearch() {
        candidates.clear();
        scannedCandidates = 0L;
        matchingCandidates = 0L;
        error = "";
        if (fullWorldSeed != null) {
            candidates.add(fullWorldSeed & SeedSlimeMath.STRUCTURE_SEED_MASK);
            matchingCandidates = 1L;
            state = State.SOLVED;
        } else if (isEnabled()) {
            state = observations.size() >= minimumObservations.value() ? State.READY : State.COLLECTING;
        } else {
            state = State.DISABLED;
        }
    }

    private void fail(String message) {
        error = message == null || message.isBlank() ? "Invalid search configuration" : message;
        state = State.ERROR;
        statusNotifier.accept("SeedCracker error: " + error);
    }

    private void trimObservations() {
        while (observations.size() > maximumObservations.value()) {
            observations.remove(observations.keySet().iterator().next());
        }
    }

    private void trimObservedEntities() {
        while (observedSlimeEntities.size() > MAXIMUM_TRACKED_SLIME_ENTITIES) {
            observedSlimeEntities.remove(observedSlimeEntities.iterator().next());
        }
    }

    private void trimCandidates() {
        while (candidates.size() > maximumResults.value()) {
            candidates.removeLast();
        }
    }

    static long parseStructureSeed(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim().replace("_", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Search start must not be blank");
        }
        try {
            return Long.decode(normalized) & SeedSlimeMath.STRUCTURE_SEED_MASK;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Search start must be a decimal or 0x hexadecimal integer");
        }
    }

    public static String formatStructureSeed(long seed) {
        return String.format(Locale.ROOT, "0x%012X", seed & SeedSlimeMath.STRUCTURE_SEED_MASK);
    }

    private static String title(String enumName) {
        String text = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public record ChunkCoordinate(int x, int z) {
    }

    public record Observation(
            ChunkCoordinate coordinate,
            EvidenceSource source,
            int confirmations,
            long firstSeenTick
    ) {
        public Observation {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            source = Objects.requireNonNull(source, "source");
            if (confirmations < 1 || firstSeenTick < 0L) {
                throw new IllegalArgumentException("Seed evidence values must be valid");
            }
        }
    }

    public record Snapshot(
            State state,
            int observations,
            int minimumObservations,
            long scannedCandidates,
            long rangeCount,
            long matchingCandidates,
            List<Long> candidates,
            Long fullWorldSeed,
            String error
    ) {
        public Snapshot {
            state = Objects.requireNonNull(state, "state");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
            error = error == null ? "" : error;
        }

        public double progress() {
            return rangeCount <= 0L ? 0.0D : Math.clamp((double) scannedCandidates / rangeCount, 0.0D, 1.0D);
        }
    }
}
