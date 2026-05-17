package com.swvague.ars_odyssey.knowledge.discovery;

public record ResearchResult(
        GlyphDiscoveryResult discoveryResult,
        boolean success,
        String failureKey
) {
    public ResearchResult {
        failureKey = failureKey == null ? "" : failureKey;
    }
}
