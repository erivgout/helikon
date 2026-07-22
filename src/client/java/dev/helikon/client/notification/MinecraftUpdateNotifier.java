package dev.helikon.client.notification;

import dev.helikon.client.integration.network.GitHubReleaseChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Consumer;

/** Client-thread toast and local-chat adapter for an available release. */
public final class MinecraftUpdateNotifier implements Consumer<GitHubReleaseChecker.AvailableRelease> {
    private static final SystemToast.SystemToastId UPDATE_TOAST = new SystemToast.SystemToastId();

    private final ChatNotifier chat;

    public MinecraftUpdateNotifier(ChatNotifier chat) {
        this.chat = Objects.requireNonNull(chat, "chat");
    }

    @Override
    public void accept(GitHubReleaseChecker.AvailableRelease release) {
        Objects.requireNonNull(release, "release");
        Minecraft client = Minecraft.getInstance();
        SystemToast.add(client.gui.toastManager(), UPDATE_TOAST,
                Component.literal("Helikon update available"),
                Component.literal(release.version() + " is ready on GitHub Releases"));
        chat.info("A new Helikon release (" + release.version() + ") is available to download: "
                + release.releaseUrl());
    }
}
