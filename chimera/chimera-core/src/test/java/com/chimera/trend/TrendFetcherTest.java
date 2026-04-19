package com.chimera.trend;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for the TrendFetcher component as specified in specs/technical.md.
 */
class TrendFetcherTest {

    @Test
    void trendRecordShouldBeJavaRecordAndMatchSpecContract() {
        Class<?> trendClass = Trend.class;

        assertTrue(trendClass.isRecord(), "Trend must be defined as a Java 21 record for immutability.");

        var components = trendClass.getRecordComponents();
        assertEquals(2, components.length, "Trend record should have exactly two components: topic, engagementScore.");

        assertEquals("topic", components[0].getName());
        assertEquals(String.class, components[0].getType(), "topic should be a String.");

        assertEquals("engagementScore", components[1].getName());
        assertEquals(double.class, components[1].getType(), "engagementScore should be a primitive double.");
    }

    @Test
    void trendResponseRecordShouldExposePlatformCategoryAndTrendsList() {
        Class<?> responseClass = TrendResponse.class;

        assertTrue(responseClass.isRecord(), "TrendResponse must be defined as a Java 21 record.");

        var components = responseClass.getRecordComponents();
        var names = Arrays.stream(components).map(c -> c.getName()).toList();

        assertTrue(names.contains("platform"), "TrendResponse must expose 'platform'.");
        assertTrue(names.contains("category"), "TrendResponse must expose 'category'.");
        assertTrue(names.contains("trends"), "TrendResponse must expose 'trends'.");
    }

    @Test
    void fetchTrendsShouldMatchTechnicalSpecInputOutputContract() {
        TrendFetcher client = new MockTrendFetcher();
        TrendRequest request = new TrendRequest("tiktok", "fitness");

        TrendResponse response = client.fetchTrends(request);

        assertNotNull(response, "fetchTrends must not return null.");
        assertEquals("tiktok", response.platform(), "Response platform should echo request platform.");
        assertEquals("fitness", response.category(), "Response category should echo request category.");

        assertNotNull(response.trends(), "Response trends list must not be null.");
        assertFalse(response.trends().isEmpty(), "Response trends list should not be empty for a valid request.");

        Trend first = response.trends().get(0);
        assertNotNull(first.topic(), "Trend.topic must not be null.");
        assertTrue(first.engagementScore() >= 0.0 && first.engagementScore() <= 1.0,
                "Trend.engagementScore should be normalized between 0.0 and 1.0.");
    }
}
