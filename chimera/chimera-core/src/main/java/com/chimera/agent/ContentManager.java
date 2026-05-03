package com.chimera.agent;

import com.chimera.content.BudgetExceededException;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import com.chimera.policy.BudgetPolicy;
import com.chimera.policy.VerdictPolicy;
import com.chimera.publisher.PlatformPublisher;
import com.chimera.publisher.PublishRequest;
import com.chimera.publisher.PublishResult;
import com.chimera.verifier.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manager agent: runs a multi-cycle loop until the goal is met or budget runs out.
 *
 * For each cycle:
 *   1. Ask the BudgetPolicy whether this cycle is allowed.
 *   2. Ask the Worker to produce a Candidate.
 *   3. Ask the Judge to evaluate it.
 *   4. If APPROVE -> publish; if REVISE -> ask Worker to revise (up to maxRevisions);
 *      otherwise -> skip and move on.
 *   5. Save a RunRecord to memory regardless of outcome (for traceability).
 *
 * The Manager is the only class that knows about ALL the pieces -- skills,
 * policies, persistence, publishing. That's by design: it's the single
 * place where strategy lives.
 */
public class ContentManager implements Manager {

    private static final Logger log = LoggerFactory.getLogger(ContentManager.class);

    private final Worker worker;
    private final Judge judge;
    private final PlatformPublisher publisher;
    private final BudgetPolicy budgetPolicy;
    private final VerdictPolicy verdictPolicy;
    private final RunHistory runHistory;

    private final int targetPostsPerRun;
    private final int maxRevisionsPerPost;

    public ContentManager(
            Worker worker,
            Judge judge,
            PlatformPublisher publisher,
            BudgetPolicy budgetPolicy,
            VerdictPolicy verdictPolicy,
            RunHistory runHistory,
            int targetPostsPerRun,
            int maxRevisionsPerPost
    ) {
        this.worker = worker;
        this.judge = judge;
        this.publisher = publisher;
        this.budgetPolicy = budgetPolicy;
        this.verdictPolicy = verdictPolicy;
        this.runHistory = runHistory;
        this.targetPostsPerRun = targetPostsPerRun;
        this.maxRevisionsPerPost = maxRevisionsPerPost;
    }

    @Override
    public ManagerResult run(PipelineRequest goal) {
        log.info("Manager starting: target={} posts, maxRevisions={}, goal={}",
                targetPostsPerRun, maxRevisionsPerPost, goal);

        List<ManagerResult.CycleTrace> cycles = new ArrayList<>();
        int published = 0;
        int rejected = 0;
        int errored = 0;

        for (int i = 0; i < targetPostsPerRun; i++) {
            log.info("Cycle {}/{}", i + 1, targetPostsPerRun);
            ManagerResult.CycleTrace trace = runOneCycle(goal);
            cycles.add(trace);

            if (trace.publishResult().isPresent()
                    && trace.publishResult().get().status()
                    == com.chimera.publisher.PublishStatus.PUBLISHED) {
                published++;
            } else if (trace.candidate().isPresent() && trace.publishResult().isEmpty()) {
                rejected++;
            } else {
                errored++;
            }

            // Save every cycle to memory, success or not.
            runHistory.save(toRunRecord(goal, trace));
        }

        log.info("Manager done: requested={}, published={}, rejected={}, errored={}",
                targetPostsPerRun, published, rejected, errored);

        return new ManagerResult(targetPostsPerRun, published, rejected, errored, cycles);
    }

    private ManagerResult.CycleTrace runOneCycle(PipelineRequest goal) {
        // 1. Budget check
        Optional<Double> approvedBudget = budgetPolicy.approve(goal.budget(), goal, runHistory);
        if (approvedBudget.isEmpty()) {
            return cycleStopped("budget policy denied request");
        }

        // 2. Worker produces a candidate
        Optional<Candidate> initial;
        try {
            initial = worker.produce(goal);
        } catch (BudgetExceededException e) {
            return cycleStopped("budget exceeded: " + e.getMessage());
        }
        if (initial.isEmpty()) {
            return cycleStopped("worker returned no candidate (no usable trend?)");
        }

        Candidate candidate = initial.get();

        // 3. Judge -> revise loop (up to maxRevisionsPerPost retries)
        VerificationResult verification = judge.evaluate(candidate);
        int revisionsUsed = 0;

        while (verification.verdict() == com.chimera.verifier.Verdict.REVISE
                && revisionsUsed < maxRevisionsPerPost) {
            log.info("Judge requested revision ({} so far): {}", revisionsUsed,
                    verification.issues().size() + " issues");

            try {
                Optional<Candidate> revised = worker.revise(goal, candidate, verification.issues());
                if (revised.isEmpty()) {
                    return cycleStoppedWith(candidate, verification, revisionsUsed,
                            "worker could not revise");
                }
                candidate = revised.get();
                verification = judge.evaluate(candidate);
                revisionsUsed++;
            } catch (BudgetExceededException e) {
                return cycleStoppedWith(candidate, verification, revisionsUsed,
                        "budget exceeded during revision: " + e.getMessage());
            }
        }

        // 4. Verdict policy decides whether to publish
        if (!verdictPolicy.shouldPublish(verification)) {
            return cycleStoppedWith(candidate, verification, revisionsUsed,
                    "verdict policy rejected publish (verdict=" + verification.verdict() + ")");
        }

        // 5. Publish
        PublishResult publishResult = publisher.publish(new PublishRequest(
                candidate.content().contentId(),
                candidate.content().script(),
                candidate.content().caption(),
                candidate.content().targetPlatform()
        ));

        return new ManagerResult.CycleTrace(
                Optional.of(candidate),
                Optional.of(verification),
                Optional.of(publishResult),
                revisionsUsed,
                Optional.empty()
        );
    }

    private ManagerResult.CycleTrace cycleStopped(String reason) {
        return new ManagerResult.CycleTrace(
                Optional.empty(), Optional.empty(), Optional.empty(), 0,
                Optional.of(reason)
        );
    }

    private ManagerResult.CycleTrace cycleStoppedWith(
            Candidate candidate, VerificationResult verification, int revisions, String reason
    ) {
        return new ManagerResult.CycleTrace(
                Optional.of(candidate),
                Optional.of(verification),
                Optional.empty(),
                revisions,
                Optional.of(reason)
        );
    }

    private RunRecord toRunRecord(PipelineRequest goal, ManagerResult.CycleTrace trace) {
        // Bridge to existing PipelineResult for memory storage.
        PipelineResult pipelineResult = new PipelineResult(
                trace.candidate().map(Candidate::selectedTrend),
                trace.candidate().map(Candidate::content),
                trace.verification(),
                trace.publishResult(),
                trace.stoppedReason()
        );
        return new RunRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                goal,
                pipelineResult
        );
    }
}
