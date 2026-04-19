package com.chimera.verifier;

/**
 * Closed set of verification outcomes.
 *
 * Using an enum (rather than String) makes the compiler enforce exhaustive
 * switch handling and prevents typos at usage sites.
 */
public enum Verdict {
    APPROVE,
    REVISE,
    REJECT
}
