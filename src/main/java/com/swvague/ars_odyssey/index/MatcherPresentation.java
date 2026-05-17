package com.swvague.ars_odyssey.index;

import java.util.Locale;

public final class MatcherPresentation {
    private MatcherPresentation() {
    }

    public static GlyphTargetRule.EvidenceLevel evidenceOf(GlyphTargetRule.TargetMatcher matcher) {
        if (matcher.evidenceOverride() != null) {
            return matcher.evidenceOverride();
        }
        return switch (matcher.type()) {
            case BEHAVIOR, POSITION_PREDICATE -> GlyphTargetRule.EvidenceLevel.RUNTIME_CHECK;
            case INDEXED_TAG -> GlyphTargetRule.EvidenceLevel.INDEX_HELPER;
            case BLACKLIST, DENY_ENTITY_TYPE_TAG -> GlyphTargetRule.EvidenceLevel.LIMITATION;
            default -> GlyphTargetRule.EvidenceLevel.CODE_CONFIRMED;
        };
    }

    public static GlyphTargetRule.TooltipRole roleOf(GlyphTargetRule.TargetMatcher matcher) {
        return switch (matcher.type()) {
            case BEHAVIOR, POSITION_PREDICATE -> GlyphTargetRule.TooltipRole.CONDITION;
            case BLACKLIST, DENY_ENTITY_TYPE_TAG -> GlyphTargetRule.TooltipRole.LIMIT;
            default -> GlyphTargetRule.TooltipRole.REASON;
        };
    }

    public static String translationKeyOf(GlyphTargetRule.TargetMatcher matcher) {
        return switch (matcher.type()) {
            case ENTITY_CLASS -> "ars_odyssey.target.entity_class." + simpleClassName(matcher.value());
            case BLOCK_CLASS -> "ars_odyssey.target.block_class." + simpleClassName(matcher.value());
            case BEHAVIOR -> "ars_odyssey.target.behavior." + matcher.value();
            case POSITION_PREDICATE -> "ars_odyssey.target.position_predicate." + matcher.value();
            case GENERAL_ENTITY -> "ars_odyssey.target.entity_class.Entity";
            case GENERAL_BLOCK -> "ars_odyssey.target.general_block";
            case ITEM_TAG -> "ars_odyssey.target.item_tag." + sanitizeKey(matcher.value());
            case BLOCK_TAG, INDEXED_TAG -> "ars_odyssey.target.block_tag." + sanitizeKey(matcher.value());
            case ENTITY_TYPE_TAG, DENY_ENTITY_TYPE_TAG -> "ars_odyssey.target.entity_type_tag." + sanitizeKey(matcher.value());
            case BLACKLIST -> translationKeyForLimit(matcher.value());
        };
    }

    public static String rawValueForDisplay(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int prefixIndex = value.indexOf(':');
        if (prefixIndex > 0 && value.substring(0, prefixIndex).contains("_")) {
            return value.substring(prefixIndex + 1);
        }
        return value;
    }

    private static String translationKeyForLimit(String value) {
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        if (normalizedValue.startsWith("entity_class:")) {
            return "ars_odyssey.target.entity_class." + simpleClassName(value.substring("entity_class:".length()));
        }
        if (normalizedValue.startsWith("block_class:")) {
            return "ars_odyssey.target.block_class." + simpleClassName(value.substring("block_class:".length()));
        }
        if (normalizedValue.startsWith("block_tag:")) {
            return "ars_odyssey.target.block_tag." + sanitizeKey(value.substring("block_tag:".length()));
        }
        if (normalizedValue.startsWith("entity_type_tag:")) {
            return "ars_odyssey.target.entity_type_tag." + sanitizeKey(value.substring("entity_type_tag:".length()));
        }
        return "ars_odyssey.target.blacklist." + sanitizeKey(value);
    }

    private static String simpleClassName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : value;
    }

    private static String sanitizeKey(String value) {
        return value.toLowerCase(Locale.ROOT).replace(':', '.').replace('/', '.');
    }
}
