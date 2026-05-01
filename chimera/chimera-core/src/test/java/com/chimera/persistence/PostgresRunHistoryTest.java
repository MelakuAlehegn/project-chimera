package com.chimera.persistence;

import com.chimera.config.Config;
import com.chimera.content.GeneratedContent;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import com.chimera.trend.Trend;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Integration test against a real local Postgres.
 *
 * Requires:
 *   - .env at repo root with DATABASE_URL/USER/PASSWORD
 *   - chimera_dev database with the schema from db/init.sql applied
 *
 * TRUNCATEs run_history before each test method. Use only against a dev DB.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresRunHistoryTest {

    private HikariDataSource pool;
    private PostgresRunHistory history;

    @BeforeAll
    void openPool() {
        Config config = Config.load();
        pool = PostgresPool.create(config);
        history = new PostgresRunHistory(pool);
    }

    @AfterAll
    void closePool() {
        if (pool != null) {
            pool.close();
        }
    }

    @BeforeEach
    void clearTable() throws SQLException {
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE run_history");
        }
    }

    @Test
    void savedRunCanBeRetrieved() {
        RunRecord original = sampleRun("fitness", "content-A");

        history.save(original);
        var all = history.findAll();

        org.junit.jupiter.api.Assertions.assertEquals(1, all.size());
        org.junit.jupiter.api.Assertions.assertEquals(original, all.get(0),
                "Round-trip should preserve all fields exactly.");
    }

    @Test
    void findByContentIdReturnsTheRightRun() {
        RunRecord runA = sampleRun("fitness", "content-A");
        RunRecord runB = sampleRun("fitness", "content-B");
        history.save(runA);
        history.save(runB);

        Optional<RunRecord> found = history.findByContentId("content-B");

        org.junit.jupiter.api.Assertions.assertTrue(found.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(runB.runId(), found.get().runId());
    }

    @Test
    void findByCategoryFiltersCorrectly() {
        history.save(sampleRun("fitness", "content-A"));
        history.save(sampleRun("cooking", "content-B"));
        history.save(sampleRun("fitness", "content-C"));

        org.junit.jupiter.api.Assertions.assertEquals(2, history.findByCategory("fitness").size());
        org.junit.jupiter.api.Assertions.assertEquals(1, history.findByCategory("cooking").size());
        org.junit.jupiter.api.Assertions.assertEquals(0, history.findByCategory("travel").size());
    }

    @Test
    void persistedRecordSurvivesNewHistoryInstance() {
        history.save(sampleRun("fitness", "content-A"));

        // New instance, same pool -- proves the data is in the DB, not in memory.
        var freshHistory = new PostgresRunHistory(pool);

        org.junit.jupiter.api.Assertions.assertEquals(1, freshHistory.findAll().size());
    }

    private RunRecord sampleRun(String category, String contentId) {
        var goal = new PipelineRequest("tiktok", category, "persona-1", 5.00);
        var content = new GeneratedContent(contentId, "script", "caption", "tiktok");
        var trend = new Trend(category + "-topic", 0.8);
        var result = new PipelineResult(
                Optional.of(trend),
                Optional.of(content),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        return new RunRecord(
                UUID.randomUUID().toString(),
                // Truncate to microseconds: Postgres TIMESTAMPTZ has microsecond
                // precision; Instant.now() has nanoseconds. Without this, equals()
                // comparisons after a save/load round-trip will fail.
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                goal,
                result
        );
    }
}
