package com.example.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

public record MatchReason(
        ResourceLocation glyphId,
        GlyphTargetRule.TargetMatcher matcher,
        boolean blacklist,
        String displayKey,
        RuleSource source,
        EvidenceConfidence confidence,
        String evidenceText
) {
    public MatchReason {
        displayKey = displayKey == null ? "" : displayKey;
        source = source == null ? RuleSource.MANUAL : source;
        confidence = confidence == null ? EvidenceConfidence.UNKNOWN : confidence;
        evidenceText = evidenceText == null ? "" : evidenceText;
    }

    public MatchReason(
            ResourceLocation glyphId,
            GlyphTargetRule.TargetMatcher matcher,
            boolean blacklist,
            String displayKey,
            RuleSource source
    ) {
        this(
                glyphId,
                matcher,
                blacklist,
                displayKey,
                source,
                matcher == null ? EvidenceConfidence.UNKNOWN : matcher.confidence(),
                matcher == null ? "" : matcher.evidenceText()
        );
    }

    public enum RuleSource {
        MANUAL,
        AUTO
    }
}