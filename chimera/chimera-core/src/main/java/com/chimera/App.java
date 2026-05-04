package com.chimera;

import com.chimera.agent.ContentJudge;
import com.chimera.agent.ContentManager;
import com.chimera.agent.ContentWorker;
import com.chimera.agent.DraftStore;
import com.chimera.agent.Judge;
import com.chimera.agent.Manager;
import com.chimera.agent.ManagerResult;
import com.chimera.agent.McpDraftStore;
import com.chimera.agent.Worker;
import com.chimera.config.Config;
import com.chimera.content.ContentGenerator;
import com.chimera.content.LlmContentGenerator;
import com.chimera.llm.GeminiLlmClient;
import com.chimera.llm.LlmClient;
import com.chimera.mcp.McpClient;
import com.chimera.mcp.StdioMcpClient;
import com.chimera.orchestrator.PipelineRequest;
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
import com.chimera.verifier.LlmContentVerifier;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Entry point and composition root.
 *
 * Wires every concrete implementation from Config, then drives a Manager
 * agent. Two run modes (CHIMERA_RUN_MODE):
 *   - "once": one Manager.run() and exit
 *   - "loop": Manager.run() every CHIMERA_LOOP_INTERVAL_MINUTES forever
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {
        Config config = Config.load();

        // External clients
        LlmClient llm = new GeminiLlmClient(config);
        HikariDataSource dataSource = PostgresPool.create(config);

        // Skills
        TrendFetcher trendFetcher = new LlmTrendFetcher(llm);
        ContentGenerator contentGenerator = new LlmContentGenerator(llm);
        ContentVerifier contentVerifier = new LlmContentVerifier(llm);
        PlatformPublisher publisher = new BlueskyPlatformPublisher(config);

        // Memory + policies
        RunHistory runHistory = new PostgresRunHistory(dataSource);
        TrendSelector trendSelector = new LlmTrendSelector(llm);
        BudgetPolicy budgetPolicy = new DailyCapBudgetPolicy(config.chimeraDailyCap());
        VerdictPolicy verdictPolicy = new StrictVerdictPolicy();

        // Optional MCP integration: filesystem server for saving drafts.
        McpClient mcpClient = config.mcpDraftsEnabled()
                ? startFilesystemMcp(config.mcpDraftsDir())
                : null;
        DraftStore draftStore = mcpClient != null
                ? new McpDraftStore(mcpClient, config.mcpDraftsDir())
                : DraftStore.NONE;

        // Agents
        Worker worker = new ContentWorker(trendFetcher, trendSelector, contentGenerator, runHistory);
        Judge judge = new ContentJudge(contentVerifier);
        Manager manager = new ContentManager(
                worker, judge, publisher,
                budgetPolicy, verdictPolicy, runHistory, draftStore,
                config.chimeraPostsPerRun(),
                config.chimeraMaxRevisions()
        );

        PipelineRequest goal = new PipelineRequest(
                config.chimeraPlatform(),
                config.chimeraCategory(),
                config.chimeraPersona(),
                config.chimeraBudget()
        );

        try {
            if ("loop".equalsIgnoreCase(config.chimeraRunMode())) {
                runLoop(manager, goal, config.chimeraLoopIntervalMinutes());
            } else {
                runOnce(manager, goal);
            }
        } finally {
            if (mcpClient != null) {
                mcpClient.close();
            }
            dataSource.close();
        }
    }

    /**
     * Start the official filesystem MCP server pointing at the drafts dir.
     * Requires Node + npx on PATH; the package is fetched on first run.
     */
    private static McpClient startFilesystemMcp(String allowedDir) {
        log.info("Starting MCP filesystem server, allowed dir={}", allowedDir);
        var client = new StdioMcpClient(List.of(
                "npx", "-y", "@modelcontextprotocol/server-filesystem", allowedDir
        ));
        client.initialize();
        return client;
    }

    private static void runOnce(Manager manager, PipelineRequest goal) {
        log.info("Chimera starting: single Manager run, goal={}", goal);
        ManagerResult result = manager.run(goal);
        logResult(result);
    }

    private static void runLoop(Manager manager, PipelineRequest goal, int intervalMinutes)
            throws InterruptedException {
        log.info("Chimera starting: loop mode every {} minutes, goal={}", intervalMinutes, goal);

        while (true) {
            try {
                ManagerResult result = manager.run(goal);
                logResult(result);
            } catch (Exception e) {
                log.error("Manager run failed", e);
            }
            log.info("Sleeping {} minutes until next run", intervalMinutes);
            Thread.sleep(Duration.ofMinutes(intervalMinutes).toMillis());
        }
    }

    private static void logResult(ManagerResult result) {
        log.info("Manager result: requested={}, published={}, rejected={}, errored={}",
                result.requested(), result.published(), result.rejected(), result.errored());
        for (int i = 0; i < result.cycles().size(); i++) {
            ManagerResult.CycleTrace cycle = result.cycles().get(i);
            log.info("--- cycle {} ---", i + 1);
            cycle.candidate().ifPresent(c ->
                    log.info("Trend: {} (engagement={})",
                            c.selectedTrend().topic(), c.selectedTrend().engagementScore()));
            cycle.candidate().ifPresent(c -> log.info("Caption: {}", c.content().caption()));
            cycle.verification().ifPresent(v ->
                    log.info("Verdict: {} (revisions used: {})", v.verdict(), cycle.revisionsUsed()));
            cycle.publishResult().ifPresent(p -> {
                log.info("Publish: {}", p.status());
                p.platformPostId().ifPresent(id -> log.info("Post URI: {}", id));
            });
            cycle.stoppedReason().ifPresent(r -> log.warn("Stopped: {}", r));
        }
    }
}
