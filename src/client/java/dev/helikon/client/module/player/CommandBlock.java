package dev.helikon.client.module.player;

/** Requests an ordinary command-block item through Creative inventory handling. */
public final class CommandBlock extends CreativeItemModule {
    public CommandBlock() {
        super("command_block", "CMD-Block", "Adds an ordinary command block to the selected Creative slot.");
    }

    @Override
    protected Request request() {
        return new Request(Kind.COMMAND_BLOCK, "minecraft:command_block", 1, "");
    }
}
