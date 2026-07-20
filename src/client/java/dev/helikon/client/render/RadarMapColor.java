package dev.helikon.client.render;

/** Deterministic, inexpensive terrain colors for the Radar minimap. */
public final class RadarMapColor {
    private RadarMapColor() {
    }

    public static int forBlock(String blockId, int heightDifference) {
        if (blockId == null || blockId.isBlank()) {
            return 0xFF3A3F47;
        }
        String id = blockId.toLowerCase(java.util.Locale.ROOT);
        int color;
        if (id.contains("water") || id.contains("ice")) {
            color = 0xFF3D71C7;
        } else if (id.contains("lava") || id.contains("magma")) {
            color = 0xFFE05B24;
        } else if (id.contains("snow") || id.contains("quartz")) {
            color = 0xFFE8EDF2;
        } else if (id.contains("sand") || id.contains("sandstone")) {
            color = 0xFFD9C27A;
        } else if (id.contains("grass") || id.contains("moss")) {
            color = 0xFF5E9E43;
        } else if (id.contains("leaves") || id.contains("vine")) {
            color = 0xFF397D3C;
        } else if (id.contains("dirt") || id.contains("mud") || id.contains("podzol")) {
            color = 0xFF79553A;
        } else if (id.contains("log") || id.contains("wood") || id.contains("plank")) {
            color = 0xFF8A6844;
        } else if (id.contains("stone") || id.contains("ore") || id.contains("deepslate")) {
            color = 0xFF777D83;
        } else {
            int hash = id.hashCode();
            color = 0xFF000000 | (0x505050 + (hash & 0x2F2F2F));
        }
        return shade(color, Math.clamp(1.0D + heightDifference * 0.018D, 0.68D, 1.25D));
    }

    /**
     * Applies map relief to Minecraft's native block map color. Relative height
     * gives broad elevation context while slope makes neighboring terrain legible.
     */
    public static int forMapColor(int mapColor, int heightDifference, int slopeDifference) {
        double factor = 1.0D + heightDifference * 0.006D + slopeDifference * 0.075D;
        return shade(mapColor, Math.clamp(factor, 0.62D, 1.32D));
    }

    private static int shade(int color, double shade) {
        int red = (int) Math.round(Math.min(255.0D, ((color >>> 16) & 0xFF) * shade));
        int green = (int) Math.round(Math.min(255.0D, ((color >>> 8) & 0xFF) * shade));
        int blue = (int) Math.round(Math.min(255.0D, (color & 0xFF) * shade));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
