package com.chimera.persistence;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory RunHistory for development and tests.
 *
 * Uses CopyOnWriteArrayList so reads are safe while writes happen
 * concurrently -- agentic systems often run multiple workers in parallel.
 */
public class InMemoryRunHistory implements RunHistory {

    private final List<RunRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void save(RunRecord record) {
        records.add(record);
    }

    @Override
    public List<RunRecord> findAll() {
        return List.copyOf(records);
    }

    @Override
    public Optional<RunRecord> findByContentId(String contentId) {
        return records.stream()
                .filter(r -> r.result().generatedContent()
                        .map(c -> c.contentId().equals(contentId))
                        .orElse(false))
                .findFirst();
    }

    @Override
    public List<RunRecord> findByCategory(String category) {
        return records.stream()
                .filter(r -> r.goal().category().equals(category))
                .toList();
    }
}
