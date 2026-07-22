package dev.helikon.client.integration.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Bounded, unauthenticated lookup of Helikon's latest public GitHub release. */
public final class GitHubReleaseChecker {
    public static final String INTEGRATION_ID = "github_release_check";
    public static final String DISPLAY_NAME = "GitHub release checker";
    public static final Set<String> ALLOWED_HOSTS = Set.of("api.github.com");
    public static final String TRANSMITTED_DATA =
            "A standard HTTPS GET with the installed Helikon version in the User-Agent; no gameplay or account data";
    public static final String DISABLE_BEHAVIOR =
            "Disabling Update Checker cancels its in-flight lookup, ignores late results, and starts no further request";

    static final URI LATEST_RELEASE = URI.create("https://api.github.com/repos/erivgout/helikon/releases/latest");
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    static final int MAXIMUM_RESPONSE_BYTES = 64 * 1024;
    private static final Map<String, String> HEADERS = Map.of(
            "Accept", "application/vnd.github+json",
            "X-GitHub-Api-Version", "2022-11-28"
    );

    private final Transport transport;

    public GitHubReleaseChecker() {
        this(new JdkTransport());
    }

    GitHubReleaseChecker(Transport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public CompletableFuture<Optional<AvailableRelease>> check(String currentVersion) {
        String normalizedCurrentVersion = Objects.requireNonNull(currentVersion, "currentVersion").strip();
        ReleaseVersion installed = ReleaseVersion.parse(normalizedCurrentVersion);
        String userAgent = "Helikon/" + normalizedCurrentVersion + " update-checker";
        CompletableFuture<Response> request = transport.get(
                LATEST_RELEASE, HEADERS, userAgent, REQUEST_TIMEOUT, MAXIMUM_RESPONSE_BYTES);
        CompletableFuture<Optional<AvailableRelease>> result = new CompletableFuture<>();
        request.whenComplete((response, failure) -> {
            if (result.isCancelled()) {
                return;
            }
            if (failure != null) {
                result.completeExceptionally(unwrap(failure));
                return;
            }
            try {
                result.complete(evaluate(response, installed));
            } catch (RuntimeException | IOException exception) {
                result.completeExceptionally(exception);
            }
        });
        result.whenComplete((ignored, failure) -> {
            if (result.isCancelled()) {
                request.cancel(true);
            }
        });
        return result;
    }

    private static Optional<AvailableRelease> evaluate(Response response, ReleaseVersion installed)
            throws IOException {
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw new IOException("GitHub release request returned HTTP " + response.statusCode());
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("GitHub release response was not valid JSON", exception);
        }
        String tag = requiredString(json, "tag_name").strip();
        ReleaseVersion latest = ReleaseVersion.parse(tag);
        if (latest.compareTo(installed) <= 0) {
            return Optional.empty();
        }
        URI releaseUrl = validateReleaseUrl(requiredString(json, "html_url"));
        return Optional.of(new AvailableRelease(tag, releaseUrl));
    }

    private static String requiredString(JsonObject json, String name) throws IOException {
        if (!json.has(name) || !json.get(name).isJsonPrimitive()) {
            throw new IOException("GitHub release response omitted " + name);
        }
        String value = json.get(name).getAsString();
        if (value.isBlank() || value.length() > 256) {
            throw new IOException("GitHub release response contained invalid " + name);
        }
        return value;
    }

    private static URI validateReleaseUrl(String value) throws IOException {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IOException("GitHub release URL was invalid", exception);
        }
        String pathPrefix = "/erivgout/helikon/releases/";
        if (!"https".equals(uri.getScheme()) || !"github.com".equals(uri.getHost())
                || uri.getUserInfo() != null || uri.getPort() != -1 || uri.getQuery() != null
                || uri.getFragment() != null || uri.getPath() == null || !uri.getPath().startsWith(pathPrefix)) {
            throw new IOException("GitHub release URL was outside the allowed repository");
        }
        return uri;
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }

    public record AvailableRelease(String version, URI releaseUrl) {
        public AvailableRelease {
            version = Objects.requireNonNull(version, "version");
            releaseUrl = Objects.requireNonNull(releaseUrl, "releaseUrl");
        }
    }

    record Response(int statusCode, String body) {
        Response {
            body = Objects.requireNonNull(body, "body");
        }
    }

    @FunctionalInterface
    interface Transport {
        CompletableFuture<Response> get(URI uri, Map<String, String> headers, String userAgent,
                                        Duration timeout, int maximumResponseBytes);
    }

    private static final class JdkTransport implements Transport {
        private HttpClient client;

        @Override
        public CompletableFuture<Response> get(URI uri, Map<String, String> headers, String userAgent,
                                               Duration timeout, int maximumResponseBytes) {
            if (!ALLOWED_HOSTS.contains(uri.getHost()) || !"https".equals(uri.getScheme())) {
                return CompletableFuture.failedFuture(new IOException("Release request host is not allowed"));
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(timeout)
                    .header("User-Agent", userAgent);
            headers.forEach(builder::header);
            return client().sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                    .thenApply(response -> read(response, maximumResponseBytes));
        }

        private HttpClient client() {
            if (client == null) {
                client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
            }
            return client;
        }

        private static Response read(HttpResponse<InputStream> response, int maximumResponseBytes) {
            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(maximumResponseBytes + 1);
                if (bytes.length > maximumResponseBytes) {
                    throw new IOException("GitHub release response exceeded the size limit");
                }
                return new Response(response.statusCode(), new String(bytes, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }
    }
}
