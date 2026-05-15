package com.example.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

public record GlyphRuleCandidate(
        ResourceLocation glyphId,
        String effectClass,
        GlyphTargetRule.MatcherType matcherType,
        String matcherValue,
        Confidence confidence,
        Polarity polarity,
        String evidenceText
) {
    public GlyphRuleCandidate {
        matcherValue = matcherValue == null ? "" : matcherValue;
        evidenceText = evidenceText == null ? "" : evidenceText;
    }

    public enum Confidence {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum Polarity {
        POSITIVE,
        NEGATIVE,
        UNKNOWN
    }

    public boolean isInitialRuleEligible() {
        return confidence == Confidence.HIGH && polarity == Polarity.POSITIVE;
    }

    public GlyphTargetRule.TargetMatcher toTargetMatcher() {
        return new GlyphTargetRule.TargetMatcher(
                matcherType,
                matcherValue,
                EvidenceConfidence.fromCandidateConfidence(confidence),
                evidenceText
        );
    }
}
