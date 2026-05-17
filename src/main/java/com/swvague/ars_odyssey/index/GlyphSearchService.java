package com.swvague.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class GlyphSearchService {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Glyph Index");

    private GlyphSearchService() {
    }

    static GlyphApplicationIndex.SearchResult searchWithReasons(String query, List<AbstractSpellPart> unlockedSpells) {
        LOGGER.debug("[Ars Odyssey] Search rawQuery=\"{}\" unlockedSpells={}",
                query == null ? "" : query,
                unlockedSpells.size());
        TargetQuery targetQuery = TargetQueryParser.parse(query);
        SearchIntent searchIntent = SearchIntentResolver.resolve(query);
        if (targetQuery.normalizedQuery().isEmpty()) {
            LOGGER.debug("[Ars Odyssey] Search matched glyph ids={}", unlockedSpells.stream()
                    .map(AbstractSpellPart::getRegistryName)
                    .toList());
            return new GlyphApplicationIndex.SearchResult(new ArrayList<>(unlockedSpells), Map.of());
        }

        List<GlyphTargetRule> manualRules = ManualGlyphRules.rules();
        Set<ResourceLocation> matchingRuleGlyphIds = new HashSet<>();
        Set<ResourceLocation> blacklistMatchedRuleGlyphIds = new HashSet<>();
        Map<ResourceLocation, List<MatchReason>> reasonsByRuleGlyphId = new LinkedHashMap<>();

        // ── Pass 1: manual hand-curated rules ──
        for (GlyphTargetRule rule : manualRules) {
            GlyphRuleMatcher.RuleMatch match = GlyphRuleMatcher.matchesRule(rule, targetQuery, MatchReason.RuleSource.MANUAL);
            if (match.positiveMatched()) {
                matchingRuleGlyphIds.add(rule.glyphId());
                reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.reasons());
            }
            if (match.blacklistMatched()) {
                blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                if (match.positiveMatched()) {
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.blacklistReasons());
                }
            }
        }

        // ── Pass 2: source-scan auto rules (requires AN .java files on disk) ──
        AutoGlyphRuleIndex.AutoRuleSet autoRuleSet = AutoGlyphRuleIndex.getRuleSet(manualRules);
        for (GlyphTargetRule rule : autoRuleSet.rules()) {
            GlyphRuleMatcher.RuleMatch match = GlyphRuleMatcher.matchesRule(rule, targetQuery, MatchReason.RuleSource.AUTO);
            if (match.positiveMatched()) {
                matchingRuleGlyphIds.add(rule.glyphId());
                reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.reasons());
                List<MatchReason> matchingLimitations = GlyphRuleMatcher.matchingLimitations(
                        rule.glyphId(),
                        autoRuleSet.limitationsByGlyph().getOrDefault(rule.glyphId(), List.of()),
                        targetQuery);
                if (!matchingLimitations.isEmpty()) {
                    blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(matchingLimitations);
                }
            }
            if (match.blacklistMatched()) {
                blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                if (match.positiveMatched()) {
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.blacklistReasons());
                }
            }
        }

        // ── Pass 3: runtime-inferred rules (works for any addon without source files) ──
        RuntimeGlyphClassifier.InferredRuleSet inferredRuleSet = RuntimeGlyphClassifier.getRuleSet(manualRules);
        for (GlyphTargetRule rule : inferredRuleSet.rules()) {
            GlyphRuleMatcher.RuleMatch match = GlyphRuleMatcher.matchesRule(rule, targetQuery, MatchReason.RuleSource.INFERRED);
            if (match.positiveMatched()) {
                matchingRuleGlyphIds.add(rule.glyphId());
                reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.reasons());
            }
            if (match.blacklistMatched()) {
                blacklistMatchedRuleGlyphIds.add(rule.glyphId());
                if (match.positiveMatched()) {
                    reasonsByRuleGlyphId.computeIfAbsent(rule.glyphId(), ignored -> new ArrayList<>()).addAll(match.blacklistReasons());
                }
            }
        }

        LOGGER.debug("[Ars Odyssey] Search positive rule glyph ids={}", matchingRuleGlyphIds);
        LOGGER.debug("[Ars Odyssey] Search blacklist matched rule glyph ids={}", blacklistMatchedRuleGlyphIds);

        List<AbstractSpellPart> results = new ArrayList<>();
        Map<ResourceLocation, List<MatchReason>> reasonsByGlyph = new LinkedHashMap<>();
        for (AbstractSpellPart spellPart : unlockedSpells) {
            ResourceLocation glyphId = spellPart.getRegistryName();
            if (searchIntent.type() == SearchIntentType.GLYPH && searchIntent.resolvedId() != null) {
                if (searchIntent.resolvedId().equals(glyphId)) {
                    results.add(spellPart);
                    List<MatchReason> reasons = reasonsByRuleGlyphId.get(glyphId);
                    if (reasons != null && !reasons.isEmpty()) {
                        reasonsByGlyph.put(glyphId, List.copyOf(reasons));
                    } else {
                        bestReasonForGlyph(glyphId).ifPresent(reason -> reasonsByGlyph.put(glyphId, List.of(reason)));
                    }
                }
                continue;
            }
            if (matchesGlyphNameOrId(spellPart, glyphId, targetQuery.normalizedQuery()) || matchingRuleGlyphIds.contains(glyphId)) {
                results.add(spellPart);
                if (reasonsByRuleGlyphId.containsKey(glyphId)) {
                    reasonsByGlyph.put(glyphId, List.copyOf(reasonsByRuleGlyphId.get(glyphId)));
                }
            }
        }
        LOGGER.debug("[Ars Odyssey] Search matched glyph ids={}", results.stream()
                .map(AbstractSpellPart::getRegistryName)
                .toList());
        return new GlyphApplicationIndex.SearchResult(results, reasonsByGlyph);
    }

    static Optional<MatchReason> bestReasonForGlyph(ResourceLocation glyphId) {
        if (glyphId == null) {
            return Optional.empty();
        }

        List<MatchReason> reasons = new ArrayList<>();
        List<GlyphTargetRule> manualRules = ManualGlyphRules.rules();

        for (GlyphTargetRule rule : manualRules) {
            if (rule.glyphId().equals(glyphId)) {
                GlyphRuleMatcher.addPositiveSummaryReasons(reasons, rule, MatchReason.RuleSource.MANUAL);
            }
        }

        AutoGlyphRuleIndex.AutoRuleSet autoRuleSet = AutoGlyphRuleIndex.getRuleSet(manualRules);
        for (GlyphTargetRule rule : autoRuleSet.rules()) {
            if (rule.glyphId().equals(glyphId)) {
                GlyphRuleMatcher.addPositiveSummaryReasons(reasons, rule, MatchReason.RuleSource.AUTO);
            }
        }

        RuntimeGlyphClassifier.InferredRuleSet inferredRuleSet = RuntimeGlyphClassifier.getRuleSet(manualRules);
        for (GlyphTargetRule rule : inferredRuleSet.rules()) {
            if (rule.glyphId().equals(glyphId)) {
                GlyphRuleMatcher.addPositiveSummaryReasons(reasons, rule, MatchReason.RuleSource.INFERRED);
            }
        }

        return MatchReasonDisplayReducer.reduce(reasons).stream()
                .filter(reason -> !reason.blacklist())
                .filter(reason -> MatcherPresentation.roleOf(reason.matcher()) != GlyphTargetRule.TooltipRole.LIMIT)
                .findFirst();
    }

    private static boolean matchesGlyphNameOrId(AbstractSpellPart spellPart, ResourceLocation glyphId, String query) {
        String localeName = spellPart.getLocaleName();
        if (localeName != null && containsNormalized(localeName, query)) {
            return true;
        }

        if (glyphId == null) {
            return false;
        }

        String fullId = glyphId.toString().toLowerCase(Locale.ROOT);
        String path = glyphId.getPath().toLowerCase(Locale.ROOT);
        return fullId.contains(query) || path.contains(query);
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
}
