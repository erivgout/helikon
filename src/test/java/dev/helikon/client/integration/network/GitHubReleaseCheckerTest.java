package dev.helikon.client.integration.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubReleaseCheckerTest {
    @Test
    void reportsOnlyANewerReleaseAndUsesTheBoundedPublicEndpointContract() {
        AtomicReference<URI> requestedUri = new AtomicReference<>();
        AtomicReference<String> userAgent = new AtomicReference<>();
        AtomicReference<Duration> timeout = new AtomicReference<>();
        AtomicReference<Integer> maximumBytes = new AtomicReference<>();
        GitHubReleaseChecker checker = new GitHubReleaseChecker((uri, headers, agent, requestTimeout, limit) -> {
            requestedUri.set(uri);
            userAgent.set(agent);
            timeout.set(requestTimeout);
            maximumBytes.set(limit);
            assertEquals("application/vnd.github+json", headers.get("Accept"));
            assertEquals("2022-11-28", headers.get("X-GitHub-Api-Version"));
            return completed(200, release("v1.6.0", "https://github.com/erivgout/helikon/releases/tag/v1.6.0"));
        });

        Optional<GitHubReleaseChecker.AvailableRelease> result = checker.check("1.5.2").join();

        assertTrue(result.isPresent());
        assertEquals("v1.6.0", result.orElseThrow().version());
        assertEquals(GitHubReleaseChecker.LATEST_RELEASE, requestedUri.get());
        assertEquals("Helikon/1.5.2 update-checker", userAgent.get());
        assertEquals(GitHubReleaseChecker.REQUEST_TIMEOUT, timeout.get());
        assertEquals(GitHubReleaseChecker.MAXIMUM_RESPONSE_BYTES, maximumBytes.get());
    }

    @Test
    void ignoresCurrentOlderAndMissingReleases() {
        assertFalse(checker(200, release("v1.5.2",
                "https://github.com/erivgout/helikon/releases/tag/v1.5.2")).check("1.5.2").join().isPresent());
        assertFalse(checker(200, release("v1.4.9",
                "https://github.com/erivgout/helikon/releases/tag/v1.4.9")).check("1.5.2").join().isPresent());
        assertFalse(checker(404, "{}").check("1.5.2").join().isPresent());
    }

    @Test
    void rejectsBadStatusMalformedJsonAndUntrustedReleaseLinks() {
        assertFailure(checker(403, "{}").check("1.5.2"), IOException.class);
        assertFailure(checker(200, "not-json").check("1.5.2"), IOException.class);
        assertFailure(checker(200, release("v1.6.0", "https://example.com/download"))
                .check("1.5.2"), IOException.class);
    }

    @Test
    void cancellationPropagatesToTheNetworkTransport() {
        CompletableFuture<GitHubReleaseChecker.Response> transport = new CompletableFuture<>();
        GitHubReleaseChecker checker = new GitHubReleaseChecker((uri, headers, agent, timeout, limit) -> transport);

        CompletableFuture<Optional<GitHubReleaseChecker.AvailableRelease>> check = checker.check("1.5.2");
        check.cancel(true);

        assertTrue(transport.isCancelled());
    }

    private static GitHubReleaseChecker checker(int status, String body) {
        return new GitHubReleaseChecker((uri, headers, agent, timeout, limit) -> completed(status, body));
    }

    private static CompletableFuture<GitHubReleaseChecker.Response> completed(int status, String body) {
        return CompletableFuture.completedFuture(new GitHubReleaseChecker.Response(status, body));
    }

    private static String release(String version, String url) {
        return "{\"tag_name\":\"" + version + "\",\"html_url\":\"" + url + "\"}";
    }

    private static void assertFailure(CompletableFuture<?> future, Class<? extends Throwable> expected) {
        CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(expected, exception.getCause());
    }
}
