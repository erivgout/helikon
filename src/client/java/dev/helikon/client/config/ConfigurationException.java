package dev.helikon.client.config;

/** Raised when a local configuration write cannot be completed safely. */
public final class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
