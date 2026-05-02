package com.chimera;

import com.chimera.config.Config;
import com.chimera.content.ContentGenerator;
import com.chimera.content.LlmContentGenerator;
import com.chimera.llm.GeminiLlmClient;
import com.chimera.llm.LlmClient;
import com.chimera.orchestrator.ContentPipeline;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import com.chimera.persistence.PostgresPool;
import com.chimera.persistence.PostgresRunHistory;
import com.chimera.persistence.RunHistory;
import com.chimera.policy.BudgetPolicy;
import com.chimera.policy.DailyCapBudgetPolicy;
import com.chimera.policy.LlmTrendSelector;
import com.chimera.policy.StrictVerdictPolicy;
import com.chimera.policy.TrendSelector;
import com.chimera.policy.VerdictPolicy;
import com.chimera.publisher.BlueskyPlatformPublisher;
import com.chimera.publisher.PlatformPublisher;
import com.chimera.publisher.PublishResult;
import com.chimera.trend.LlmTrendFetcher;
import com.chimera.trend.TrendFetcher;
import com.chimera.verifier.ContentVerifier;
import com.chimera.verifier.MockContentVerifier;
import com.zaxxer.hikari.HikariDataSource;

import java.time.Duration;
import java.time.Instant;

/**
 * Entry point.
 *
 * Composition root: builds every concrete implementation from Config and
 * wires them into a ContentPipeline. Then runs the pipeline either once
 * or in a loop (controlled by CHIMERA_RUN_MODE in .env).
 *
 * Two modes:
 *   - "once":  run one cycle, print result, exit. Ideal for testing.
 *   - "loop":  run every CHIMERA_LOOP_INTERVAL_MINUTES until interrupted.
 *             This is the autonomous mode -- the agent runs by itself.
 */
public class App {

    public static void main(String[] args) throws InterruptedException {
        Config config = Config.load();

        // External clients
        LlmClient llm = new GeminiLlmClient(config);
        HikariDataSource dataSource = PostgresPool.create(config);

        // Skills (4 of 5 are real LLM/HTTP-backed)
        TrendFetcher trendFetcher = new LlmTrendFetcher(llm);
        ContentGenerator contentGenerator = new LlmContentGenerator(llm);
        ContentVerifier contentVerifier = new MockContentVerifier();
        PlatformPublisher publisher = new BlueskyPlatformPublisher(config);

        // State + policies
        RunHistory runHistory = new PostgresRunHistory(dataSource);
        TrendSelector trendSelector = new LlmTrendSelector(llm);
        BudgetPolicy budgetPolicy = new DailyCapBudgetPolicy(config.chimeraDailyCap());
        VerdictPolicy verdictPolicy = new StrictVerdictPolicy();

        ContentPipeline pipeline = new ContentPipeline(
                trendFetcher, contentGenerator, contentVerifier, publisher,
                runHistory, trendSelector, budgetPolicy, verdictPolicy
        );

        PipelineRequest goal = new PipelineRequest(
                config.chimeraPlatform(),
                config.chimeraCategory(),
                config.chimeraPersona(),
                config.chimeraBudget()
        );

        try {
            if ("loop".equalsIgnoreCase(config.chimeraRunMode())) {
                runLoop(pipeline, goal, config.chimeraLoopIntervalMinutes());
            } else {
                runOnce(pipeline, goal);
            }
        } finally {
            dataSource.close();
        }
    }

    private static void runOnce(ContentPipeline pipeline, PipelineRequest goal) {
        System.out.println("== Chimera: single run @ " + Instant.now() + " ==");
        System.out.println("Goal: " + goal);
        PipelineResult result = pipeline.run(goal);
        printResult(result);
    }

    private static void runLoop(ContentPipeline pipeline, PipelineRequest goal, int intervalMinutes)
            throws InterruptedException {
        System.out.println("== Chimera: loop mode, every " + intervalMinutes + "m ==");
        System.out.println("Goal: " + goal);
        System.out.println("(Ctrl+C to stop)\n");

        while (true) {
            System.out.println("\n-- run @ " + Instant.now() + " --");
            try {
                PipelineResult result = pipeline.run(goal);
                printResult(result);
            } catch (Exception e) {
                // Never crash the loop on a single failed run -- log and continue.
                System.err.println("Run failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            Thread.sleep(Duration.ofMinutes(intervalMinutes).toMillis());
        }
    }

    private static void printResult(PipelineResult result) {
        result.selectedTrend().ifPresent(t ->
                System.out.println("Trend:  " + t.topic() + " (" + t.engagementScore() + ")"));
        result.generatedContent().ifPresent(c -> {
            System.out.println("Caption: " + c.caption());
            System.out.println("Script:");
            System.out.println(c.script());
        });
        result.verificationResult().ifPresent(v ->
                System.out.println("Verdict: " + v.verdict()));
        result.publishResult().ifPresent(p -> {
            System.out.println("Publish: " + p.status());
            p.platformPostId().ifPresent(id -> System.out.println("Post URI: " + id));
            PublishResult pr = p; // satisfy linter
            pr.error().ifPresent(err -> System.out.println("Error: " + err));
        });
        result.stoppedReason().ifPresent(r -> System.out.println("Stopped: " + r));
    }
}
