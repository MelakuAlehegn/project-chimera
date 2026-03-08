package com.chimera;

/**
 * Request parameters for fetching trends.
 *
 * Matches the input contract from specs/technical.md:
 * { "platform": "...", "category": "..." }.
 */
public record TrendRequest(String platform, String category) {
}

