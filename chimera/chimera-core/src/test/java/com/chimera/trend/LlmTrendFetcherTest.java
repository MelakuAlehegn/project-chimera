package com.chimera.trend;

import com.chimera.llm.LlmClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmTrendFetcherTest {

    @Test
    void parsesCleanJsonResponse() {
        LlmClient stub = prompt -> """
                {"trends":[
                    {"topic":"morning workout","engagementScore":0.9},
                    {"topic":"meal prep","engagementScore":0.7}
                ]}
                """;
        var fetcher = new LlmTrendFetcher(stub);

        TrendResponse response = fetcher.fetchTrends(new TrendRequest("bluesky", "fitness"));

        assertEquals("bluesky", response.platform());
        assertEquals("fitness", response.category());
        assertEquals(2, response.trends().size());
        assertEquals("morning workout", response.trends().get(0).topic());
        assertEquals(0.9, response.trends().get(0).engagementScore());
    }

    @Test
    void toleratesCodeFences() {
        LlmClient stub = prompt -> """
                ```json
                {"trends":[{"topic":"alpha","engagementScore":0.5}]}
                ```
                """;
        var fetcher = new LlmTrendFetcher(stub);

        TrendResponse response = fetcher.fetchTrends(new TrendRequest("bluesky", "fitness"));

        assertEquals(1, response.trends().size());
    }

    @Test
    void throwsOnMalformedJson() {
        LlmClient stub = prompt -> "not json at all";
        var fetcher = new LlmTrendFetcher(stub);

        assertThrows(IllegalStateException.class,
                () -> fetcher.fetchTrends(new TrendRequest("bluesky", "fitness")));
    }

    @Test
    void throwsWhenTrendsArrayMissing() {
        LlmClient stub = prompt -> """
                {"oops":"no trends here"}
                """;
        var fetcher = new LlmTrendFetcher(stub);

        assertThrows(IllegalStateException.class,
                () -> fetcher.fetchTrends(new TrendRequest("bluesky", "fitness")));
    }
}
