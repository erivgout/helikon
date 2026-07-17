package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.gui.HelikonChangelogScreen;
import dev.helikon.client.module.ModuleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.util.ArrayList;
import java.util.List;

/** Minecraft-only one-shot adapters for local interface utilities. */
public final class MinecraftInterfaceActions {
    private MinecraftInterfaceActions() {
    }

    public static void tickChangelog(Changelog module, ModuleRegistry registry) {
        if (module.consumeOpenRequest()) {
            Minecraft.getInstance().gui.setScreen(new HelikonChangelogScreen(module.notes()));
            registry.setEnabled(module, false);
        }
    }

    public static void tickServerCleanup(ServerCleanUp module, ModuleRegistry registry) {
        if (!module.consumeRunRequest()) {
            return;
        }
        ServerList servers = new ServerList(Minecraft.getInstance());
        servers.load();
        List<ServerData> originals = new ArrayList<>();
        List<ServerCleanUp.Entry> facts = new ArrayList<>();
        for (int index = 0; index < servers.size(); index++) {
            ServerData data = servers.get(index);
            originals.add(data);
            facts.add(new ServerCleanUp.Entry(data.name, data.ip, index));
        }
        List<ServerCleanUp.Entry> cleaned = module.clean(facts);
        for (ServerData data : originals) {
            servers.remove(data);
        }
        for (ServerCleanUp.Entry entry : cleaned) {
            servers.add(originals.get(entry.originalIndex()), false);
        }
        servers.save();
        registry.setEnabled(module, false);
    }
}
