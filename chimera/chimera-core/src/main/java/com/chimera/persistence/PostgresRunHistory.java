package com.chimera.persistence;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL-backed RunHistory.
 *
 * Same contract as InMemoryRunHistory. The pipeline is unchanged -- only the
 * concrete implementation behind the RunHistory interface differs.
 *
 * Schema is defined in db/init.sql. JSON serialization handled by Json.
 */
public class PostgresRunHistory implements RunHistory {

    private static final String SQL_INSERT =
            "INSERT INTO run_history (run_id, run_at, goal, result) " +
                    // ?::jsonb tells Postgres to parse the string as JSONB
                    "VALUES (?, ?, ?::jsonb, ?::jsonb)";

    private static final String SQL_FIND_ALL =
            "SELECT run_id, run_at, goal, result FROM run_history ORDER BY run_at ASC";

    private static final String SQL_FIND_BY_CONTENT_ID =
            "SELECT run_id, run_at, goal, result FROM run_history " +
                    "WHERE result->'generatedContent'->>'contentId' = ? " +
                    "LIMIT 1";

    private static final String SQL_FIND_BY_CATEGORY =
            "SELECT run_id, run_at, goal, result FROM run_history " +
                    "WHERE goal->>'category' = ? " +
                    "ORDER BY run_at ASC";

    private final DataSource dataSource;

    public PostgresRunHistory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(RunRecord record) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {

            stmt.setObject(1, UUID.fromString(record.runId()));
            // Postgres TIMESTAMPTZ wants OffsetDateTime, not Instant directly.
            stmt.setObject(2, record.runAt().atOffset(ZoneOffset.UTC));
            stmt.setString(3, Json.toJson(record.goal()));
            stmt.setString(4, Json.toJson(record.result()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save run " + record.runId(), e);
        }
    }

    @Override
    public List<RunRecord> findAll() {
        return queryList(SQL_FIND_ALL, ps -> {
        });
    }

    @Override
    public Optional<RunRecord> findByContentId(String contentId) {
        return queryList(SQL_FIND_BY_CONTENT_ID, ps -> ps.setString(1, contentId))
                .stream()
                .findFirst();
    }

    @Override
    public List<RunRecord> findByCategory(String category) {
        return queryList(SQL_FIND_BY_CATEGORY, ps -> ps.setString(1, category));
    }

    // --- internals ---

    /** Lambda that binds parameters to a PreparedStatement (may throw SQLException). */
    @FunctionalInterface
    private interface ParamBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<RunRecord> queryList(String sql, ParamBinder binder) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            binder.bind(stmt);

            try (ResultSet rs = stmt.executeQuery()) {
                List<RunRecord> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(toRunRecord(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed: " + sql, e);
        }
    }

    private RunRecord toRunRecord(ResultSet rs) throws SQLException {
        UUID runId = rs.getObject("run_id", UUID.class);
        OffsetDateTime runAt = rs.getObject("run_at", OffsetDateTime.class);
        String goalJson = rs.getString("goal");
        String resultJson = rs.getString("result");

        return new RunRecord(
                runId.toString(),
                runAt.toInstant(),
                Json.fromJson(goalJson, PipelineRequest.class),
                Json.fromJson(resultJson, PipelineResult.class)
        );
    }
}
