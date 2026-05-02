package com.chimera.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application configuration loaded from environment variables (and .env at the
 * repo root if present).
 *
 * Resolution order: actual environment variable -> .env file -> default.
 * Anything sensitive (API keys, passwords) MUST come from here, never inline.
 */
public final class Config {

    private final Dotenv dotenv;

    private Config(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    public static Config load() {
        Dotenv dotenv = Dotenv.configure()
                .directory(findEnvDirectory())
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        return new Config(dotenv);
    }

    /**
     * Walk up from the current working directory looking for a .env file.
     * This makes the config work whether tests run from the repo root or
     * from a Maven submodule directory.
     */
    private static String findEnvDirectory() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(".env"))) {
                return current.toString();
            }
            current = current.getParent();
        }
        // Fall back to current dir; dotenv will tolerate the missing file.
        return System.getProperty("user.dir");
    }

    // --- LLM ---

    public String geminiApiKey() {
        return required("GEMINI_API_KEY");
    }

    public String geminiModel() {
        return optional("GEMINI_MODEL", "gemini-2.0-flash");
    }

    // --- Database ---

    public String databaseUrl() {
        return required("DATABASE_URL");
    }

    public String databaseUser() {
        return required("DATABASE_USER");
    }

    public String databasePassword() {
        return optional("DATABASE_PASSWORD", "");
    }

    // --- Bluesky ---

    public String blueskyHandle() {
        return required("BLUESKY_HANDLE");
    }

    public String blueskyAppPassword() {
        return required("BLUESKY_APP_PASSWORD");
    }

    // --- Agent runtime ---

    public String chimeraPlatform() {
        return optional("CHIMERA_PLATFORM", "bluesky");
    }

    public String chimeraCategory() {
        return optional("CHIMERA_CATEGORY", "fitness");
    }

    public String chimeraPersona() {
        return optional("CHIMERA_PERSONA", "fit_chimera_v1");
    }

    public double chimeraBudget() {
        return Double.parseDouble(optional("CHIMERA_BUDGET", "2.00"));
    }

    public double chimeraDailyCap() {
        return Double.parseDouble(optional("CHIMERA_DAILY_CAP", "20.00"));
    }

    /** "once" or "loop". */
    public String chimeraRunMode() {
        return optional("CHIMERA_RUN_MODE", "once");
    }

    public int chimeraLoopIntervalMinutes() {
        return Integer.parseInt(optional("CHIMERA_LOOP_INTERVAL_MINUTES", "60"));
    }

    // --- internals ---

    private String required(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required configuration: " + key
                            + ". Set it in your environment or .env file."
            );
        }
        return value;
    }

    private String optional(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
