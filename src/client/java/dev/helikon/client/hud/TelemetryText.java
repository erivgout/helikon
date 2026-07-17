package dev.helikon.client.hud;

import java.util.Locale;

/** Minecraft-free formatting rules for the compact plan telemetry readouts. */
public final class TelemetryText {
    private TelemetryText() {
    }

    public static String direction(float yaw) {
        if (!Float.isFinite(yaw)) {
            return "Direction --";
        }
        String[] directions = {"South", "West", "North", "East"};
        int index = Math.floorMod(Math.round(yaw / 90.0F), directions.length);
        return "Direction " + directions[index];
    }

    public static String fps(int fps) {
        return "FPS " + Math.max(0, fps);
    }

    public static String ping(int milliseconds) {
        return milliseconds < 0 ? "Ping --" : "Ping " + milliseconds + " ms";
    }

    public static String tps(double value) {
        return !Double.isFinite(value) || value < 0.0D ? "TPS --"
                : String.format(Locale.ROOT, "TPS %.1f (local)", Math.min(20.0D, value));
    }

    public static String speed(double blocksPerSecond) {
        return !Double.isFinite(blocksPerSecond) || blocksPerSecond < 0.0D ? "Speed --"
                : String.format(Locale.ROOT, "Speed %.2f m/s", blocksPerSecond);
    }

    public static String durability(String label, int remaining, int maximum) {
        if (label == null || label.isBlank() || remaining < 0 || maximum <= 0 || remaining > maximum) {
            return label == null || label.isBlank() ? "Durability --" : label + " --";
        }
        return String.format(Locale.ROOT, "%s %d%%", label, Math.round(remaining * 100.0D / maximum));
    }

    public static String clock(long worldTime) {
        long normalized = Math.floorMod(worldTime + 6_000L, 24_000L);
        int minutes = (int) (normalized * 1_440L / 24_000L);
        return String.format(Locale.ROOT, "Clock %02d:%02d", minutes / 60, minutes % 60);
    }

    public static String titleCaseIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "--";
        }
        String path = identifier.contains(":") ? identifier.substring(identifier.indexOf(':') + 1) : identifier;
        String[] words = path.split("[_-]");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? "--" : result.toString();
    }
}
