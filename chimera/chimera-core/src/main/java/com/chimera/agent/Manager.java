package com.chimera.agent;

import com.chimera.orchestrator.PipelineRequest;

/**
 * Coordinating role: runs the agent loop until the goal is met or budget
 * runs out. The Manager is the only agent that knows about Workers, Judges,
 * publishers, policies, and memory together -- it owns the strategy.
 */
public interface Manager {

    ManagerResult run(PipelineRequest goal);
}
