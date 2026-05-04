package com.chimera.agent;

/**
 * Saves a generated draft somewhere durable for human review.
 *
 * The Manager calls save() once per cycle. Implementations decide where
 * the draft goes (local disk via MCP, S3, Google Drive, etc.).
 */
public interface DraftStore {

    void save(Candidate candidate, String outcome);

    /** No-op store; used when draft saving is disabled. */
    DraftStore NONE = (candidate, outcome) -> { };
}
