package com.chimera.agent;

import com.chimera.mcp.McpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DraftStore that writes one .md file per draft via an MCP filesystem server.
 *
 * The Manager calls save() once per cycle; we serialize the candidate and
 * outcome into a markdown file under the configured directory.
 */
public class McpDraftStore implements DraftStore {

    private static final Logger log = LoggerFactory.getLogger(McpDraftStore.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final McpClient client;
    private final String directory;

    public McpDraftStore(McpClient client, String directory) {
        this.client = client;
        this.directory = directory;
    }

    @Override
    public void save(Candidate candidate, String outcome) {
        String filename = "draft-" + candidate.content().contentId() + ".md";
        String path = directory + "/" + filename;

        String body = """
                # %s

                **Outcome:** %s
                **Trend:** %s (engagement %s)
                **Platform:** %s

                ## Caption
                %s

                ## Script
                %s
                """.formatted(
                candidate.content().contentId(),
                outcome,
                candidate.selectedTrend().topic(),
                candidate.selectedTrend().engagementScore(),
                candidate.content().targetPlatform(),
                candidate.content().caption(),
                candidate.content().script()
        );

        try {
            ObjectNode args = MAPPER.createObjectNode();
            args.put("path", path);
            args.put("content", body);
            client.callTool("write_file", args);
            log.info("Saved draft via MCP: {}", path);
        } catch (RuntimeException e) {
            // A failed draft save shouldn't fail the cycle. Log and move on.
            log.warn("Failed to save draft via MCP: {}", e.getMessage());
        }
    }
}
