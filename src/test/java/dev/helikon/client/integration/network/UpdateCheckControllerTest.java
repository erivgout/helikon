package dev.helikon.client.integration.network;

import dev.helikon.client.module.miscellaneous.UpdateChecker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckControllerTest {
    @Test
    void staysOfflineUntilOptedInAndNotifiesOnlyOncePerEnable() {
        UpdateChecker module = new UpdateChecker();
        List<GitHubReleaseChecker.AvailableRelease> notifications = new ArrayList<>();
        int[] requests = {0};
        GitHubReleaseChecker checker = new GitHubReleaseChecker((uri, headers, agent, timeout, limit) -> {
            requests[0]++;
            return CompletableFuture.completedFuture(new GitHubReleaseChecker.Response(200,
                    "{\"tag_name\":\"v1.6.0\",\"html_url\":"
                            + "\"https://github.com/erivgout/helikon/releases/tag/v1.6.0\"}"));
        });
        UpdateCheckController controller = new UpdateCheckController(module, checker, "1.5.2", notifications::add);

        controller.tick();
        assertEquals(0, requests[0]);
        module.enable();
        controller.tick();
        controller.tick();
        assertEquals(1, requests[0]);
        assertEquals(1, notifications.size());
        controller.tick();
        assertEquals(1, requests[0]);

        module.disable();
        controller.tick();
        module.enable();
        controller.tick();
        assertEquals(2, requests[0]);
    }

    @Test
    void disablingCancelsAnInFlightLookup() {
        UpdateChecker module = new UpdateChecker();
        module.enable();
        CompletableFuture<GitHubReleaseChecker.Response> transport = new CompletableFuture<>();
        GitHubReleaseChecker checker = new GitHubReleaseChecker(
                (uri, headers, agent, timeout, limit) -> transport);
        UpdateCheckController controller = new UpdateCheckController(module, checker, "1.5.2", ignored -> {
        });

        controller.tick();
        module.disable();
        controller.tick();

        assertTrue(transport.isCancelled());
    }
}
