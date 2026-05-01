-- Schema for Project Chimera's persistent agent memory.
--
-- Run once against chimera_dev:
--   psql -d chimera_dev -f db/init.sql
--
-- Idempotent: safe to re-run. Drops nothing.

CREATE TABLE IF NOT EXISTS run_history (
    run_id     UUID         PRIMARY KEY,
    run_at     TIMESTAMPTZ  NOT NULL,
    goal       JSONB        NOT NULL,
    result     JSONB        NOT NULL
);

-- Index for findByCategory: extracts goal->>'category' as a btree key.
CREATE INDEX IF NOT EXISTS idx_run_history_category
    ON run_history ((goal->>'category'));

-- Index for findByContentId: extracts result->generatedContent->>'contentId'.
-- The double arrows (->) navigate JSONB; (->>) extracts as text.
CREATE INDEX IF NOT EXISTS idx_run_history_content_id
    ON run_history ((result->'generatedContent'->>'contentId'));

-- Index for time-window queries (e.g., "what ran in the last 24 hours?").
CREATE INDEX IF NOT EXISTS idx_run_history_run_at
    ON run_history (run_at DESC);
