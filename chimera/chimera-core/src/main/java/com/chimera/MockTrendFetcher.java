package com.chimera;

import java.util.List;

/**
 * Mock implementation of TrendFetcher for development and testing.
 *
 * Returns hardcoded trend data that matches the specs/technical.md contract.
 * No external API calls are made.
 */
public class MockTrendFetcher implements TrendFetcher {

    @Override
    public TrendResponse fetchTrends(TrendRequest request) {
        var trends = List.of(
                new Trend("morning workout routine", 0.89),
                new Trend("protein shake recipes", 0.76),
                new Trend("desk stretches for remote workers", 0.65)
        );
        return new TrendResponse(request.platform(), request.category(), trends);
    }
}
