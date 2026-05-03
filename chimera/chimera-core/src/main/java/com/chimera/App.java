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
import com.chimera.trend.LlmTrendFetcher;
import com.chimera.trend.TrendFetcher;
import com.chimera.verifier.ContentVerifier;
import com.chimera.verifier.MockContentVerifier;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Entry point.
 *
 * Composition root: builds every concrete implementation from Config and
 * wires them into a ContentPipeline. Then runs the pipeline either once
 * or in a loop (controlled by CHIMERA_RUN_MODE in .env).
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {
        Config config = Config.load();

        LlmClient llm = new GeminiLlmClient(config);
        HikariDataSource dataSource = PostgresPool.create(config);

        TrendFetcher trendFetcher = new LlmTrendFetcher(llm);
        ContentGenerator contentGenerator = new LlmContentGenerator(llm);
        ContentVerifier contentVerifier = new MockContentVerifier();
        PlatformPublisher publisher = new BlueskyPlatformPublisher(config);

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
        log.info("Chimera starting: single run, goal={}", goal);
        PipelineResult result = pipeline.run(goal);
        logResult(result);
    }

    private static void runLoop(ContentPipeline pipeline, PipelineRequest goal, int intervalMinutes)
            throws InterruptedException {
        log.info("Chimera starting: loop mode every {} minutes, goal={}", intervalMinutes, goal);

        while (true) {
            try {
                PipelineResult result = pipeline.run(goal);
                logResult(result);
            } catch (Exception e) {
                // Never crash the loop on a single failed run.
                log.error("Run failed", e);
            }
            log.info("Sleeping {} minutes until next run", intervalMinutes);
            Thread.sleep(Duration.ofMinutes(intervalMinutes).toMillis());
        }
    }

    private static void logResult(PipelineResult result) {
        result.selectedTrend().ifPresent(t ->
                log.info("Trend selected: {} (engagement={})", t.topic(), t.engagementScore()));
        result.generatedContent().ifPresent(c -> {
            log.info("Caption: {}", c.caption());
            log.info("Script:\n{}", c.script());
        });
        result.verificationResult().ifPresent(v ->
                log.info("Verdict: {}", v.verdict()));
        result.publishResult().ifPresent(p -> {
            log.info("Publish status: {}", p.status());
            p.platformPostId().ifPresent(id -> log.info("Post URI: {}", id));
            p.error().ifPresent(err -> log.warn("Publish error: {}", err));
        });
        result.stoppedReason().ifPresent(r -> log.warn("Pipeline stopped: {}", r));
    }
}
