package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BudgetPolicyTest {

    private final PipelineRequest goal =
            new PipelineRequest("tiktok", "fitness", "persona-1", 5.00);

    @Test
    void naivePolicyAlwaysApprovesRequestedAmount() {
        Optional<Double> approved = new NaiveBudgetPolicy()
                .approve(5.00, goal, new InMemoryRunHistory());

        assertEquals(5.00, approved.orElseThrow());
    }

    @Test
    void dailyCapApprovesWhenWithinCap() {
        Optional<Double> approved = new DailyCapBudgetPolicy(10.00)
                .approve(5.00, goal, new InMemoryRunHistory());

        assertTrue(approved.isPresent());
    }

    @Test
    void dailyCapDeniesWhenSumExceedsCap() {
        RunHistory history = new InMemoryRunHistory();
        history.save(runRecordWithBudget(6.00));  // already spent 6

        Optional<Double> approved = new DailyCapBudgetPolicy(10.00)
                .approve(5.00, goal, history);  // 6 + 5 = 11 > 10

        assertTrue(approved.isEmpty());
    }

    private RunRecord runRecordWithBudget(double budget) {
        var pastGoal = new PipelineRequest("tiktok", "fitness", "persona-1", budget);
        var emptyResult = new PipelineResult(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty()
        );
        return new RunRecord("run-x", Instant.now(), pastGoal, emptyResult);
    }
}
