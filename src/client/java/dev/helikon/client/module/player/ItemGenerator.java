package dev.helikon.client.module.player;

import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.StringSetting;

/** Requests one configured registered item through the normal Creative inventory action. */
public final class ItemGenerator extends CreativeItemModule {
    private final StringSetting itemId;
    private final IntegerSetting count;
    private final StringSetting customName;

    public ItemGenerator() {
        super("item_generator", "ItemGenerator", "Adds one configured registered item to the selected Creative slot.");
        itemId = addSetting(new StringSetting("item_id", "Item ID", "Registered Minecraft item identifier.",
                "minecraft:stone", 128, false));
        count = addSetting(new IntegerSetting("count", "Count", "Requested stack count.", 1, 1, 64));
        customName = addSetting(new StringSetting("custom_name", "Custom name",
                "Optional bounded literal display name.", "", 64, true));
    }

    @Override
    protected Request request() {
        return new Request(Kind.ITEM, itemId.value(), count.value(), customName.value());
    }
}
