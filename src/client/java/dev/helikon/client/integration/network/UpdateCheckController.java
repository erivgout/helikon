package dev.helikon.client.integration.network;

import dev.helikon.client.module.miscellaneous.UpdateChecker;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Client-thread lifecycle coordinator for one lookup per explicit enable/session. */
public final class UpdateCheckController implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(UpdateCheckController.class.getName());

    private final UpdateChecker module;
    private final GitHubReleaseChecker checker;
    private final String currentVersion;
    private final Consumer<GitHubReleaseChecker.AvailableRelease> notification;

    private CompletableFuture<Optional<GitHubReleaseChecker.AvailableRelease>> pending;
    private boolean observedEnabled;

    public UpdateCheckController(UpdateChecker module, GitHubReleaseChecker checker, String currentVersion,
                                 Consumer<GitHubReleaseChecker.AvailableRelease> notification) {
        this.module = Objects.requireNonNull(module, "module");
        this.checker = Objects.requireNonNull(checker, "checker");
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
        this.notification = Objects.requireNonNull(notification, "notification");
    }

    public void tick() {
        if (!module.isEnabled()) {
            if (observedEnabled) {
                cancelPending();
            }
            observedEnabled = false;
            return;
        }
        if (!observedEnabled) {
            observedEnabled = true;
            pending = checker.check(currentVersion);
        }
        if (pending == null || !pending.isDone()) {
            return;
        }
        CompletableFuture<Optional<GitHubReleaseChecker.AvailableRelease>> completed = pending;
        pending = null;
        try {
            completed.join().ifPresent(notification);
        } catch (CancellationException exception) {
            LOGGER.log(Level.FINE, "GitHub release check was cancelled", exception);
        } catch (CompletionException exception) {
            LOGGER.log(Level.INFO, "Unable to check GitHub releases; continuing without an update notice",
                    exception.getCause() == null ? exception : exception.getCause());
        }
    }

    @Override
    public void close() {
        cancelPending();
        observedEnabled = false;
    }

    private void cancelPending() {
        if (pending != null) {
            pending.cancel(true);
            pending = null;
        }
    }
}
