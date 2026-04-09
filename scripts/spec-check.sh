#!/usr/bin/env bash
# spec-check.sh -- Verify that code aligns with specs.
# Checks: required types exist, records match spec fields, interfaces declare expected methods.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/chimera/chimera-core/src/main/java/com/chimera"
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

# --- 1. Required source files exist ---
echo "1. Required source files"
for f in Trend.java TrendRequest.java TrendResponse.java TrendFetcher.java \
         ContentGenerator.java ContentGenerationRequest.java GeneratedContent.java \
         BudgetExceededException.java; do
    check "$f exists" "$([ -f "$SRC/$f" ] && echo true || echo false)"
done

echo ""

# --- 2. Records match specs/technical.md field names ---
echo "2. Record field alignment with specs/technical.md"

# Trend(String topic, double engagementScore)
check "Trend has 'topic' field" \
    "$(grep -q 'String topic' "$SRC/Trend.java" && echo true || echo false)"
check "Trend has 'engagementScore' field" \
    "$(grep -q 'double engagementScore' "$SRC/Trend.java" && echo true || echo false)"
check "Trend is a record" \
    "$(grep -q 'public record Trend' "$SRC/Trend.java" && echo true || echo false)"

# TrendRequest(String platform, String category)
check "TrendRequest has 'platform' field" \
    "$(grep -q 'String platform' "$SRC/TrendRequest.java" && echo true || echo false)"
check "TrendRequest has 'category' field" \
    "$(grep -q 'String category' "$SRC/TrendRequest.java" && echo true || echo false)"

# TrendResponse(String platform, String category, List<Trend> trends)
check "TrendResponse has 'trends' field" \
    "$(grep -q 'trends' "$SRC/TrendResponse.java" && echo true || echo false)"

# ContentGenerationRequest includes characterReferenceId
check "ContentGenerationRequest has 'characterReferenceId'" \
    "$(grep -q 'characterReferenceId' "$SRC/ContentGenerationRequest.java" && echo true || echo false)"

# GeneratedContent has contentId, script, caption, targetPlatform
for field in contentId script caption targetPlatform; do
    check "GeneratedContent has '$field'" \
        "$(grep -q "$field" "$SRC/GeneratedContent.java" && echo true || echo false)"
done

echo ""

# --- 3. Interface contracts ---
echo "3. Interface contracts"
check "ContentGenerator is an interface" \
    "$(grep -q 'public interface ContentGenerator' "$SRC/ContentGenerator.java" && echo true || echo false)"
check "ContentGenerator.generate() declares BudgetExceededException" \
    "$(grep -q 'throws BudgetExceededException' "$SRC/ContentGenerator.java" && echo true || echo false)"

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
