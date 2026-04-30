package com.chimera.persistence;

import java.util.List;
import java.util.Optional;

/**
 * Repository for pipeline run history -- the agent's long-term memory.
 *
 * Implementations can be in-memory (dev/test), PostgreSQL (production),
 * or anything else as long as the contract holds.
 */
public interface RunHistory {

    /**
     * Persist a single run. Implementations must be safe to call concurrently.
     */
    void save(RunRecord record);

    /**
     * All runs in the order they were recorded (oldest first).
     */
    List<RunRecord> findAll();

    /**
     * Look up a run by the contentId it produced (for idempotency checks).
     */
    Optional<RunRecord> findByContentId(String contentId);

    /**
     * All runs for a given category (for novelty / "what trends did we use?").
     */
    List<RunRecord> findByCategory(String category);
}
