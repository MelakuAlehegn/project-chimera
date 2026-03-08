package com.chimera;

import java.util.List;

/**
 * Response type for TrendFetcher, mirroring the TrendFetcher API contract:
 * it echoes platform and category and returns a list of Trend records.
 */
public record TrendResponse(String platform, String category, List<Trend> trends) {
}

