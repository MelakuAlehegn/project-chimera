#!/usr/bin/env bash
# spec-check.sh -- Verify that code aligns with specs.
# Checks: required types exist, records match spec fields, interfaces declare expected methods.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="$ROOT/chimera/chimera-core/src/main/java/com/chimera"
TREND="$BASE/trend"
CONTENT="$BASE/content"
VERIFIER="$BASE/verifier"
SPECS="$ROOT/specs"
PASS=0
FAIL=0

check() {
    local desc="$1" result="$2"
    if [ "$result" = "true" ]; then
        echo "  [PASS] $desc"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] $desc"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== Spec-Check: Code vs specs/ alignment ==="
echo ""

# --- 1. Required source files exist (in their skill packages) ---
echo "1. Required source files"
for f in Trend.java TrendRequest.java TrendResponse.java TrendFetcher.java MockTrendFetcher.java; do
    check "trend/$f exists" "$([ -f "$TREND/$f" ] && echo true || echo false)"
done
for f in ContentGenerator.java ContentGenerationRequest.java GeneratedContent.java \
         BudgetExceededException.java MockContentGenerator.java; do
    check "content/$f exists" "$([ -f "$CONTENT/$f" ] && echo true || echo false)"
done
for f in ContentVerifier.java VerificationRequest.java VerificationResult.java \
         VerificationIssue.java Verdict.java MockContentVerifier.java; do
    check "verifier/$f exists" "$([ -f "$VERIFIER/$f" ] && echo true || echo false)"
done

echo ""

# --- 2. Records match specs/technical.md field names ---
echo "2. Record field alignment with specs/technical.md"

check "Trend has 'topic' field" \
    "$(grep -q 'String topic' "$TREND/Trend.java" && echo true || echo false)"
check "Trend has 'engagementScore' field" \
    "$(grep -q 'double engagementScore' "$TREND/Trend.java" && echo true || echo false)"
check "Trend is a record" \
    "$(grep -q 'public record Trend' "$TREND/Trend.java" && echo true || echo false)"

check "TrendRequest has 'platform' field" \
    "$(grep -q 'String platform' "$TREND/TrendRequest.java" && echo true || echo false)"
check "TrendRequest has 'category' field" \
    "$(grep -q 'String category' "$TREND/TrendRequest.java" && echo true || echo false)"

check "TrendResponse has 'trends' field" \
    "$(grep -q 'trends' "$TREND/TrendResponse.java" && echo true || echo false)"

check "ContentGenerationRequest has 'characterReferenceId'" \
    "$(grep -q 'characterReferenceId' "$CONTENT/ContentGenerationRequest.java" && echo true || echo false)"

for field in contentId script caption targetPlatform; do
    check "GeneratedContent has '$field'" \
        "$(grep -q "$field" "$CONTENT/GeneratedContent.java" && echo true || echo false)"
done

echo ""

# --- 3. Interface contracts ---
echo "3. Interface contracts"
check "TrendFetcher is an interface" \
    "$(grep -q 'public interface TrendFetcher' "$TREND/TrendFetcher.java" && echo true || echo false)"
check "ContentGenerator is an interface" \
    "$(grep -q 'public interface ContentGenerator' "$CONTENT/ContentGenerator.java" && echo true || echo false)"
check "ContentGenerator.generate() declares BudgetExceededException" \
    "$(grep -q 'throws BudgetExceededException' "$CONTENT/ContentGenerator.java" && echo true || echo false)"
check "ContentVerifier is an interface" \
    "$(grep -q 'public interface ContentVerifier' "$VERIFIER/ContentVerifier.java" && echo true || echo false)"
check "Verdict is an enum" \
    "$(grep -q 'public enum Verdict' "$VERIFIER/Verdict.java" && echo true || echo false)"

echo ""

# --- 4. Spec files exist and are non-trivial ---
echo "4. Spec files"
for f in _meta.md functional.md technical.md; do
    exists="$([ -f "$SPECS/$f" ] && echo true || echo false)"
    check "$f exists" "$exists"
    if [ "$exists" = "true" ]; then
        lines=$(wc -l < "$SPECS/$f")
        check "$f has substantive content (>10 lines, got $lines)" \
            "$([ "$lines" -gt 10 ] && echo true || echo false)"
    fi
done

echo ""

# --- 5. Database schema referenced in technical.md ---
echo "5. Database schema in specs/technical.md"
check "Trends table defined" \
    "$(grep -qi 'trends' "$SPECS/technical.md" && echo true || echo false)"
check "Videos table defined" \
    "$(grep -qi 'videos' "$SPECS/technical.md" && echo true || echo false)"
check "ERD diagram present" \
    "$(grep -q 'erDiagram' "$SPECS/technical.md" && echo true || echo false)"

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
