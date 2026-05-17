package com.swvague.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AutoGlyphRuleIndex {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Auto Glyph Index");
    private static AutoRuleSet cachedRuleSet;

    private AutoGlyphRuleIndex() {
    }

    public static AutoRuleSet getRuleSet(List<GlyphTargetRule> manualRules) {
        if (cachedRuleSet == null) {
            cachedRuleSet = loadRuleSet();
        }
        return cachedRuleSet.withoutManualDuplicates(manualRules);
    }

    private static AutoRuleSet loadRuleSet() {
        Optional<Path> sourceRoot = findSourceRoot();
        if (sourceRoot.isEmpty()) {
            LOGGER.info("[Ars Odyssey] Auto source scan skipped; Ars Nouveau source root was not found.");
            return AutoRuleSet.empty();
        }

        try {
            List<GlyphRuleCandidate> candidates = CandidateRuleExtractor.extractFromSourceDirectory(sourceRoot.get());
            AutoRuleSet ruleSet = buildRuleSet(candidates);
            LOGGER.info("[Ars Odyssey] Auto source scan loaded sourceRoot={} candidates={} autoRules={} limitationGlyphs={}",
                    sourceRoot.get(),
                    candidates.size(),
                    ruleSet.rules().size(),
                    ruleSet.limitationsByGlyph().size());
            return ruleSet;
        } catch (IOException e) {
            LOGGER.info("[Ars Odyssey] Auto source scan failed: {}", e.toString());
            return AutoRuleSet.empty();
        }
    }

    private static AutoRuleSet buildRuleSet(List<GlyphRuleCandidate> candidates) {
        Map<ResourceLocation, List<GlyphTargetRule.TargetMatcher>> positiveMatchers = new LinkedHashMap<>();
        Map<ResourceLocation, List<GlyphTargetRule.TargetMatcher>> limitations = new LinkedHashMap<>();
        Map<ResourceLocation, String> effectClasses = new LinkedHashMap<>();

        for (GlyphRuleCandidate candidate : candidates) {
            effectClasses.putIfAbsent(candidate.glyphId(), candidate.effectClass());
            if (candidate.confidence() == GlyphRuleCandidate.Confidence.HIGH && candidate.polarity() == GlyphRuleCandidate.Polarity.POSITIVE) {
                positiveMatchers.computeIfAbsent(candidate.glyphId(), ignored -> new ArrayList<>())
                        .add(autoMatcher(candidate.matcherType(), candidate.matcherValue()));
            } else if (candidate.confidence() == GlyphRuleCandidate.Confidence.HIGH && candidate.polarity() == GlyphRuleCandidate.Polarity.NEGATIVE) {
                limitations.computeIfAbsent(candidate.glyphId(), ignored -> new ArrayList<>())
                        .add(new GlyphTargetRule.TargetMatcher(candidate.matcherType(), candidate.matcherValue(), GlyphTargetRule.EvidenceLevel.LIMITATION));
            }
        }

        List<GlyphTargetRule> rules = positiveMatchers.entrySet().stream()
                .map(entry -> new GlyphTargetRule(
                        entry.getKey(),
                        distinctMatchers(entry.getValue()),
                        List.of("auto_source_scan"),
                        "Auto source scan candidates for " + effectClasses.getOrDefault(entry.getKey(), entry.getKey().toString()) + "."))
                .toList();
        Map<ResourceLocation, List<GlyphTargetRule.TargetMatcher>> distinctLimitations = limitations.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> distinctMatchers(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new));
        return new AutoRuleSet(rules, distinctLimitations);
    }

    private static GlyphTargetRule.TargetMatcher autoMatcher(GlyphTargetRule.MatcherType type, String value) {
        return new GlyphTargetRule.TargetMatcher(type, value, GlyphTargetRule.EvidenceLevel.AUTO_SOURCE_SCAN);
    }

    private static List<GlyphTargetRule.TargetMatcher> distinctMatchers(List<GlyphTargetRule.TargetMatcher> matchers) {
        Set<String> seen = new LinkedHashSet<>();
        List<GlyphTargetRule.TargetMatcher> distinct = new ArrayList<>();
        for (GlyphTargetRule.TargetMatcher matcher : matchers) {
            String key = matcherKey(matcher);
            if (seen.add(key)) {
                distinct.add(matcher);
            }
        }
        return List.copyOf(distinct);
    }

    private static Optional<Path> findSourceRoot() {
        List<Path> candidates = new ArrayList<>();
        String configured = System.getProperty("ars_odyssey.ars_nouveau_source_root");
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured));
        }

        Path cwd = Path.of("").toAbsolutePath();
        candidates.add(cwd.resolve("tmp/ars_nouveau_sources"));
        candidates.add(cwd.resolve("../tmp/ars_nouveau_sources"));
        candidates.add(cwd.resolve("../../tmp/ars_nouveau_sources"));
        candidates.add(cwd.resolve("ars_nouveau_sources"));

        return candidates.stream()
                .map(Path::normalize)
                .filter(path -> Files.isDirectory(path.resolve("com/hollingsworth/arsnouveau/common/spell/effect")))
                .findFirst();
    }

    private static String matcherKey(GlyphTargetRule.TargetMatcher matcher) {
        return matcher.type() + ":" + matcher.value();
    }

    public record AutoRuleSet(
            List<GlyphTargetRule> rules,
            Map<ResourceLocation, List<GlyphTargetRule.TargetMatcher>> limitationsByGlyph
    ) {
        public AutoRuleSet {
            rules = List.copyOf(rules);
            limitationsByGlyph = limitationsByGlyph.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> List.copyOf(entry.getValue())));
        }

        public static AutoRuleSet empty() {
            return new AutoRuleSet(List.of(), Map.of());
        }

        public AutoRuleSet withoutManualDuplicates(List<GlyphTargetRule> manualRules) {
            Set<String> manualMatcherKeys = manualRules.stream()
                    .flatMap(rule -> rule.matchers().stream()
                            .map(matcher -> rule.glyphId() + "|" + matcherKey(matcher)))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            List<GlyphTargetRule> filteredRules = rules.stream()
                    .map(rule -> new GlyphTargetRule(
                            rule.glyphId(),
                            rule.matchers().stream()
                                    .filter(matcher -> !manualMatcherKeys.contains(rule.glyphId() + "|" + matcherKey(matcher)))
                                    .toList(),
                            rule.categories(),
                            rule.note()))
                    .filter(rule -> !rule.matchers().isEmpty())
                    .toList();
            return new AutoRuleSet(filteredRules, limitationsByGlyph);
        }
    }
}
