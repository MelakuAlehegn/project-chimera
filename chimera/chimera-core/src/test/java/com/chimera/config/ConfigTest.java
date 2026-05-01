package com.chimera.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight test: Config should load without errors as long as the .env
 * (or environment) supplies the required values. We don't assert specific
 * values to avoid coupling tests to local secrets.
 */
class ConfigTest {

    @Test
    void configShouldLoadAndExposeRequiredFields() {
        Config config = Config.load();

        assertNotNull(config.geminiApiKey(), "GEMINI_API_KEY should be loaded.");
        assertNotNull(config.geminiModel());
        assertNotNull(config.databaseUrl());
        assertNotNull(config.databaseUser());
        // Password is allowed to be empty (peer auth), so don't assert non-blank.
        assertNotNull(config.databasePassword());
    }
}
