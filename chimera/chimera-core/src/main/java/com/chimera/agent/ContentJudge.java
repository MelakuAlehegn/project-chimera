package com.chimera.agent;

import com.chimera.verifier.ContentVerifier;
import com.chimera.verifier.VerificationRequest;
import com.chimera.verifier.VerificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Judge implementation that delegates evaluation to a ContentVerifier.
 *
 * The wrapping makes the agent role explicit at the architecture level:
 * the Manager talks to a Judge, not to a "verifier skill". This separation
 * lets us swap the underlying verifier (LLM, rule-based, multi-step) without
 * touching the Manager.
 */
public class ContentJudge implements Judge {

    private static final Logger log = LoggerFactory.getLogger(ContentJudge.class);

    private final ContentVerifier verifier;

    public ContentJudge(ContentVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public VerificationResult evaluate(Candidate candidate) {
        log.info("Judge: evaluating candidate (contentId={})", candidate.content().contentId());
        VerificationResult result = verifier.verify(new VerificationRequest(
                candidate.content().contentId(),
                candidate.content().script(),
                candidate.content().caption(),
                candidate.content().targetPlatform()
        ));
        log.info("Judge: verdict={} (safetyScore={}, {} issue(s))",
                result.verdict(), result.safetyScore(), result.issues().size());
        return result;
    }
}
