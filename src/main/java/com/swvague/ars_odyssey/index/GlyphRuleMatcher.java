package com.swvague.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BEHAVIOR;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BLACKLIST;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BLOCK_CLASS;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BLOCK_TAG;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.DENY_ENTITY_TYPE_TAG;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.ENTITY_CLASS;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.ENTITY_TYPE_TAG;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_BLOCK;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_ENTITY;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.ITEM_TAG;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.POSITION_PREDICATE;

final class GlyphRuleMatcher {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Glyph Index");

    private GlyphRuleMatcher() {
    }

    static RuleMatch matchesRule(GlyphTargetRule rule, TargetQuery targetQuery, MatchReason.RuleSource source) {
        String query = targetQuery.normalizedQuery();
        boolean positiveMatched = false;
        boolean blacklistMatched = false;
        List<MatchReason> reasons = new ArrayList<>();
        List<MatchReason> blacklistReasons = new ArrayList<>();
        if (containsNormalized(rule.glyphId().toString(), query) || containsNormalized(rule.note(), query)) {
            positiveMatched = true;
        }

        for (String category : rule.categories()) {
            if (containsNormalized(category, query)) {
                positiveMatched = true;
            }
        }

        for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
            ResourceLocation matcherId = ResourceLocation.tryParse(matcher.value().toLowerCase(Locale.ROOT));
            if (matcher.type() == GENERAL_ENTITY && isEntityQuery(targetQuery)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == GENERAL_BLOCK && isBlockQuery(targetQuery)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == ITEM_TAG && tagMatches(matcherId, targetQuery.itemTags(), targetQuery.queriedTags())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == BLOCK_TAG && tagMatches(matcherId, targetQuery.blockTags(), targetQuery.queriedTags())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == ENTITY_TYPE_TAG && tagMatches(matcherId, targetQuery.entityTypeTags(), targetQuery.queriedTags())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == GlyphTargetRule.MatcherType.INDEXED_TAG && indexedTagMatches(matcherId, targetQuery)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == DENY_ENTITY_TYPE_TAG && tagMatches(matcherId, targetQuery.entityTypeTags(), targetQuery.queriedTags())) {
                blacklistMatched = true;
                addReason(blacklistReasons, rule, matcher, true, source);
            }
            if (matcher.type() == ENTITY_CLASS && derivedMatcherMatches(targetQuery, "entity_class", matcher.value())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == BLOCK_CLASS && derivedMatcherMatches(targetQuery, "block_class", matcher.value())) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matcher.type() == BLACKLIST && blacklistMatches(matcher.value(), targetQuery)) {
                blacklistMatched = true;
                addReason(blacklistReasons, rule, matcher, true, source);
            }

            if (matchesStringFallback(matcher.type()) && containsNormalized(matcher.value(), query)) {
                positiveMatched = true;
                addReason(reasons, rule, matcher, false, source);
            }
            if (matchesBlacklistString(matcher.type()) && containsNormalized(matcher.value(), query)) {
                blacklistMatched = true;
                addReason(blacklistReasons, rule, matcher, true, source);
            }
        }

        if (blacklistMatched && reasons.isEmpty()) {
            positiveMatched = false;
        }

        if (positiveMatched || blacklistMatched) {
            LOGGER.debug("[Ars Odyssey] {} rule matched glyphId={} positiveMatched={} blacklistMatched={}",
                    source.name().toLowerCase(Locale.ROOT),
                    rule.glyphId(),
                    positiveMatched,
                    blacklistMatched);
        }
        return new RuleMatch(positiveMatched, blacklistMatched, reasons, blacklistReasons);
    }

    static List<MatchReason> matchingLimitations(
            ResourceLocation glyphId,
            List<GlyphTargetRule.TargetMatcher> limitations,
            TargetQuery targetQuery
    ) {
        List<MatchReason> reasons = new ArrayList<>();
        for (GlyphTargetRule.TargetMatcher matcher : limitations) {
            ResourceLocation matcherId = ResourceLocation.tryParse(matcher.value().toLowerCase(Locale.ROOT));
            if (matcher.type() == DENY_ENTITY_TYPE_TAG && tagMatches(matcherId, targetQuery.entityTypeTags(), targetQuery.queriedTags())) {
                addReason(reasons, glyphId, matcher, true, MatchReason.RuleSource.AUTO);
            } else if (matcher.type() == BLACKLIST && blacklistMatches(matcher.value(), targetQuery)) {
                addReason(reasons, glyphId, matcher, true, MatchReason.RuleSource.AUTO);
            }
        }
        return reasons;
    }

    static void addPositiveSummaryReasons(
            List<MatchReason> reasons,
            GlyphTargetRule rule,
            MatchReason.RuleSource source
    ) {
        for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
            if (MatcherPresentation.roleOf(matcher) != GlyphTargetRule.TooltipRole.LIMIT) {
                reasons.add(new MatchReason(rule.glyphId(), matcher, false, MatcherPresentation.translationKeyOf(matcher), source));
            }
        }
    }

    private static void addReason(
            List<MatchReason> reasons,
            GlyphTargetRule rule,
            GlyphTargetRule.TargetMatcher matcher,
            boolean blacklist,
            MatchReason.RuleSource source
    ) {
        addReason(reasons, rule.glyphId(), matcher, blacklist, source);
    }

    private static void addReason(
            List<MatchReason> reasons,
            ResourceLocation glyphId,
            GlyphTargetRule.TargetMatcher matcher,
            boolean blacklist,
            MatchReason.RuleSource source
    ) {
        MatchReason reason = new MatchReason(glyphId, matcher, blacklist, MatcherPresentation.translationKeyOf(matcher), source);
        if (!reasons.contains(reason)) {
            reasons.add(reason);
            LOGGER.debug("[Ars Odyssey] matched {} by {} rule {}={} blacklist={}",
                    glyphId,
                    source.name().toLowerCase(Locale.ROOT),
                    matcher.type(),
                    matcher.value(),
                    blacklist);
        }
    }

    private static boolean isEntityQuery(TargetQuery targetQuery) {
        return targetQuery.entityType().isPresent() || targetQuery.derivedMatchers().contains("entity_class:Entity");
    }

    private static boolean isBlockQuery(TargetQuery targetQuery) {
        return targetQuery.block().isPresent() || targetQuery.item().orElse(null) instanceof BlockItem;
    }

    private static boolean blacklistMatches(String value, TargetQuery targetQuery) {
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        if (normalizedValue.startsWith("entity_class:")) {
            return derivedMatcherMatches(targetQuery, "entity_class", value.substring("entity_class:".length()));
        }
        if (normalizedValue.startsWith("block_class:")) {
            return derivedMatcherMatches(targetQuery, "block_class", value.substring("block_class:".length()));
        }
        if (normalizedValue.startsWith("block_tag:")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring("block_tag:".length()).toLowerCase(Locale.ROOT));
            return tagMatches(id, targetQuery.blockTags(), targetQuery.queriedTags());
        }
        if (normalizedValue.startsWith("entity_type_tag:")) {
            ResourceLocation id = ResourceLocation.tryParse(value.substring("entity_type_tag:".length()).toLowerCase(Locale.ROOT));
            return tagMatches(id, targetQuery.entityTypeTags(), targetQuery.queriedTags());
        }
        return containsNormalized(value, targetQuery.normalizedQuery());
    }

    private static boolean containsNormalized(String value, String query) {
        if (value == null) {
            return false;
        }
        String normalizedValue = normalizeLoose(value);
        String normalizedQuery = normalizeLoose(query);
        return normalizedValue.contains(normalizedQuery);
    }

    private static String normalizeLoose(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "");
    }

    private static boolean matchesStringFallback(GlyphTargetRule.MatcherType matcherType) {
        return matcherType == BEHAVIOR
                || matcherType == BLOCK_CLASS
                || matcherType == ENTITY_CLASS
                || matcherType == POSITION_PREDICATE;
    }

    private static boolean matchesBlacklistString(GlyphTargetRule.MatcherType matcherType) {
        return matcherType == BLACKLIST || matcherType == DENY_ENTITY_TYPE_TAG;
    }

    private static boolean tagMatches(ResourceLocation matcherId, Set<ResourceLocation> resolvedTags, Set<ResourceLocation> queriedTags) {
        return matcherId != null && (resolvedTags.contains(matcherId) || queriedTags.contains(matcherId));
    }

    private static boolean indexedTagMatches(ResourceLocation matcherId, TargetQuery targetQuery) {
        return matcherId != null && (targetQuery.queriedTags().contains(matcherId)
                || targetQuery.itemTags().contains(matcherId)
                || targetQuery.blockTags().contains(matcherId)
                || targetQuery.entityTypeTags().contains(matcherId));
    }

    private static boolean derivedMatcherMatches(TargetQuery targetQuery, String prefix, String matcherValue) {
        String simpleMatcherValue = simpleClassName(matcherValue);
        return targetQuery.derivedMatchers().contains(prefix + ":" + simpleMatcherValue);
    }

    private static String simpleClassName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int lastDot = value.lastIndexOf('.');
        return lastDot >= 0 ? value.substring(lastDot + 1) : value;
    }

    record RuleMatch(
            boolean positiveMatched,
            boolean blacklistMatched,
            List<MatchReason> reasons,
            List<MatchReason> blacklistReasons
    ) {
        RuleMatch {
            reasons = List.copyOf(reasons);
            blacklistReasons = List.copyOf(blacklistReasons);
        }
    }
}
