package dev.helikon.client.module.world;

/** Harvests the nearest supported mature loaded plant with its ordinary interaction path. */
public final class AutoFarm extends BoundedWorldAction {
    public enum HarvestMode {
        BREAK_AND_REPLANT_BELOW,
        BREAK_AND_REPLANT_COCOA,
        PICK_IN_PLACE,
        BREAK_ABOVE_BASE,
        BREAK_FRUIT
    }

    public AutoFarm() {
        super("auto_farm", "AutoFarm", "Harvests nearby mature crops, berries, fruit, and stacked plants.",
                4.0D, 4);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.matureCrop();
    }

    /** Selects the ordinary harvest interaction for a previously validated plant candidate. */
    public HarvestMode harvestMode(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("block ID must not be blank");
        }
        return switch (blockId) {
            case "minecraft:sweet_berry_bush" -> HarvestMode.PICK_IN_PLACE;
            case "minecraft:cocoa" -> HarvestMode.BREAK_AND_REPLANT_COCOA;
            case "minecraft:sugar_cane", "minecraft:bamboo", "minecraft:cactus" ->
                    HarvestMode.BREAK_ABOVE_BASE;
            case "minecraft:melon", "minecraft:pumpkin" -> HarvestMode.BREAK_FRUIT;
            default -> HarvestMode.BREAK_AND_REPLANT_BELOW;
        };
    }
}
