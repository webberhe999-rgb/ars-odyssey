package com.swvague.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record GlyphTargetRule(
        ResourceLocation glyphId,
        List<TargetMatcher> matchers,
        List<String> categories,
        String note
) {
    public GlyphTargetRule {
        matchers = List.copyOf(matchers);
        categories = List.copyOf(categories);
    }

    public enum MatcherType {
        BLOCK_TAG,
        ITEM_TAG,
        ENTITY_TYPE_TAG,
        BLOCK_CLASS,
        ENTITY_CLASS,
        BEHAVIOR,
        DENY_ENTITY_TYPE_TAG,
        POSITION_PREDICATE,
        GENERAL_ENTITY,
        GENERAL_BLOCK,
        INDEXED_TAG,
        BLACKLIST
    }

    public enum EvidenceLevel {
        CODE_CONFIRMED,
        RUNTIME_CHECK,
        INDEX_HELPER,
        INFERRED,
        LIMITATION,
        AUTO_SOURCE_SCAN
    }

    public enum TooltipRole {
        REASON,
        CONDITION,
        LIMIT
    }

    public record TargetMatcher(
            MatcherType type,
            String value,
            EvidenceConfidence confidence,
            String evidenceText,
            EvidenceLevel evidenceOverride
    ) {
        public TargetMatcher(MatcherType type, String value) {
            this(type, value, EvidenceConfidence.HIGH, "manual_rule", null);
        }

        public TargetMatcher(MatcherType type, String value, EvidenceLevel evidenceOverride) {
            this(type, value, EvidenceConfidence.HIGH, "manual_rule", evidenceOverride);
        }

        public TargetMatcher(
                MatcherType type,
                String value,
                EvidenceConfidence confidence,
                String evidenceText
        ) {
            this(type, value, confidence, evidenceText, null);
        }
    }
}
