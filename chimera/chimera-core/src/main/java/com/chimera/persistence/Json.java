package com.chimera.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

/**
 * JSON helper used by Postgres-backed persistence.
 *
 * The ObjectMapper is configured to handle:
 *   - Optional<T>           (Jdk8Module)
 *   - Instant / java.time   (JavaTimeModule, written as ISO-8601 strings)
 *
 * Failures here are programmer errors, not recoverable conditions, so we wrap
 * checked exceptions in IllegalStateException to keep call sites clean.
 */
public final class Json {

    private static final ObjectMapper MAPPER = buildMapper();

    private Json() {
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + value.getClass(), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize into " + type, e);
        }
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new Jdk8Module());
        m.registerModule(new JavaTimeModule());
        // Write Instants as "2026-04-19T12:34:56.789Z" instead of epoch millis.
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }
}
