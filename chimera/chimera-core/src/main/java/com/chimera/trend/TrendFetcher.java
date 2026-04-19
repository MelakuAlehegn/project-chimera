package com.chimera.trend;

/**
 * Skill interface for fetching trending topics from social media platforms.
 *
 * Implementations provide platform-specific logic (e.g., TikTok, Instagram).
 * See specs/technical.md for the API contract.
 */
public interface TrendFetcher {

    TrendResponse fetchTrends(TrendRequest request);
}
