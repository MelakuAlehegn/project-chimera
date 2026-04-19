package com.chimera.verifier;

/**
 * Input to the ContentVerifier skill.
 *
 * Flattened from the nested JSON shape in skills/skill_content_verifier/README.md
 * -- nested context/policies objects will be added when a consumer actually needs them.
 */
public record VerificationRequest(
        String contentId,
        String script,
        String caption,
        String targetPlatform
) {
}
