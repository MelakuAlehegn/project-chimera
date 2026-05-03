package com.chimera.agent;

import com.chimera.verifier.ContentVerifier;
import com.chimera.verifier.VerificationRequest;
import com.chimera.verifier.VerificationResult;

/**
 * Judge implementation that delegates evaluation to a ContentVerifier.
 *
 * The wrapping makes the agent role explicit at the architecture level:
 * the Manager talks to a Judge, not to a "verifier skill". This separation
 * lets us swap the underlying verifier (LLM, rule-based, multi-step) without
 * touching the Manager.
 */
public class ContentJudge implements Judge {

    private final ContentVerifier verifier;

    public ContentJudge(ContentVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public VerificationResult evaluate(Candidate candidate) {
        return verifier.verify(new VerificationRequest(
                candidate.content().contentId(),
                candidate.content().script(),
                candidate.content().caption(),
                candidate.content().targetPlatform()
        ));
    }
}
