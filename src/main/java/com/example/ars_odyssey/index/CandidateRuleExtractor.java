package com.example.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CandidateRuleExtractor {
    private static final Pattern CLASS_NAME = Pattern.compile("\\bclass\\s+(Effect\\w+)\\b");
    private static final Pattern GLYPH_LIB_ID = Pattern.compile("super\\(\\s*GlyphLib\\.(\\w+)\\s*,");
    private static final Pattern INSTANCEOF = Pattern.compile("\\binstanceof\\s+([A-Za-z0-9_.$]+)");
    private static final Pattern STATE_IS = Pattern.compile("\\b(?:state|blockState)\\.is\\(([^\\n;]+?)\\)");
    private static final Pattern STACK_IS = Pattern.compile("\\b(?:stack|itemStack)\\.is\\(([^\\n;]+?)\\)");
    private static final Pattern ENTITY_TYPE_IS = Pattern.compile("\\b(?:entity|target|rayTraceResult\\.getEntity\\(\\))\\.getType\\(\\)\\.is\\(([^\\n;]+?)\\)");
    private static final Pattern GLYPH_LIB_ENTRY = Pattern.compile("public\\s+static\\s+final\\s+String\\s+(\\w+)\\s*=\\s*prependGlyph\\(\"([^\"]+)\"\\)");

    private static final Set<String> ENTITY_CLASS_HINTS = Set.of(
            "Entity", "LivingEntity", "ItemEntity", "ExperienceOrb", "Player", "ServerPlayer",
            "Mob", "TamableAnimal", "Animal", "Projectile", "AbstractHorse", "Wolf", "Vex"
    );

    private static final List<BehaviorSignal> BEHAVIOR_SIGNALS = List.of(
            new BehaviorSignal("BoneMealItem.applyBonemeal", GlyphTargetRule.MatcherType.BEHAVIOR, "bonemealable"),
            new BehaviorSignal("BoneMealItem.growWaterPlant", GlyphTargetRule.MatcherType.BEHAVIOR, "water_plant_bonemealable"),
            new BehaviorSignal("RecipeType.SMELTING", GlyphTargetRule.MatcherType.BEHAVIOR, "smelting_recipe_matches"),
            new BehaviorSignal("RecipeType.SMOKING", GlyphTargetRule.MatcherType.BEHAVIOR, "smoking_recipe_matches"),
            new BehaviorSignal("RecipeType.BLASTING", GlyphTargetRule.MatcherType.BEHAVIOR, "blasting_recipe_matches"),
            new BehaviorSignal("CrushRecipe.matches", GlyphTargetRule.MatcherType.BEHAVIOR, "crush_recipe_matches"),
            new BehaviorSignal("BlockUtil.canBlockBeHarvested", GlyphTargetRule.MatcherType.BEHAVIOR, "can_block_be_harvested"),
            new BehaviorSignal("EnchantedFallingBlock.canFall", GlyphTargetRule.MatcherType.BEHAVIOR, "enchanted_falling_block_can_fall"),
            new BehaviorSignal("teleportTo", GlyphTargetRule.MatcherType.BEHAVIOR, "teleport"),
            new BehaviorSignal("isValidTeleport", GlyphTargetRule.MatcherType.POSITION_PREDICATE, "valid_teleport_position")
    );

    private CandidateRuleExtractor() {
    }

    public static List<GlyphRuleCandidate> extractCandidates(ResourceLocation glyphId, String effectClass, String source) {
        List<GlyphRuleCandidate> candidates = new ArrayList<>();

        if (source.contains("onResolveEntity(")) {
            add(candidates, glyphId, effectClass, GlyphTargetRule.MatcherType.GENERAL_ENTITY, "true",
                    GlyphRuleCandidate.Confidence.MEDIUM, GlyphRuleCandidate.Polarity.POSITIVE,
                    "onResolveEntity override");
        }
        if (source.contains("onResolveBlock(")) {
            add(candidates, glyphId, effectClass, GlyphTargetRule.MatcherType.GENERAL_BLOCK, "true",
                    GlyphRuleCandidate.Confidence.MEDIUM, GlyphRuleCandidate.Polarity.POSITIVE,
                    "onResolveBlock override");
        }

        extractInstanceof(candidates, glyphId, effectClass, source);
        extractTagCalls(candidates, glyphId, effectClass, source, STATE_IS, GlyphTargetRule.MatcherType.BLOCK_TAG, "state.is(...)");
        extractTagCalls(candidates, glyphId, effectClass, source, STACK_IS, GlyphTargetRule.MatcherType.ITEM_TAG, "stack.is(...)");
        extractEntityTypeTags(candidates, glyphId, effectClass, source);
        extractBehaviorSignals(candidates, glyphId, effectClass, source);

        return List.copyOf(candidates);
    }

    public static Optional<GlyphTargetRule> buildInitialRule(ResourceLocation glyphId, String effectClass, List<GlyphRuleCandidate> candidates) {
        List<GlyphTargetRule.TargetMatcher> matchers = candidates.stream()
                .filter(GlyphRuleCandidate::isInitialRuleEligible)
                .map(GlyphRuleCandidate::toTargetMatcher)
                .distinct()
                .toList();
        if (matchers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GlyphTargetRule(glyphId, matchers, List.of("auto_candidate"), "Auto-generated candidate rule for " + effectClass + "."));
    }

    public static List<GlyphRuleCandidate> extractFromSourceDirectory(Path sourceRoot) throws IOException {
        Map<String, ResourceLocation> glyphConstants = readGlyphConstants(sourceRoot);
        Path effectRoot = sourceRoot.resolve("com/hollingsworth/arsnouveau/common/spell/effect");
        if (!Files.isDirectory(effectRoot)) {
            return List.of();
        }

        List<GlyphRuleCandidate> candidates = new ArrayList<>();
        try (Stream<Path> files = Files.walk(effectRoot)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString().startsWith("Effect"))
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList()) {
                String source = Files.readString(file);
                String effectClass = findFirst(CLASS_NAME, source).orElse(file.getFileName().toString().replace(".java", ""));
                ResourceLocation glyphId = findFirst(GLYPH_LIB_ID, source)
                        .map(glyphConstants::get)
                        .orElseGet(() -> fallbackGlyphId(effectClass));
                candidates.addAll(extractCandidates(glyphId, effectClass, source));
            }
        }
        return List.copyOf(candidates);
    }

    private static Map<String, ResourceLocation> readGlyphConstants(Path sourceRoot) throws IOException {
        Path glyphLib = sourceRoot.resolve("com/hollingsworth/arsnouveau/common/lib/GlyphLib.java");
        if (!Files.exists(glyphLib)) {
            return Map.of();
        }
        String source = Files.readString(glyphLib);
        Map<String, ResourceLocation> constants = new LinkedHashMap<>();
        Matcher matcher = GLYPH_LIB_ENTRY.matcher(source);
        while (matcher.find()) {
            constants.put(matcher.group(1), ResourceLocation.parse("ars_nouveau:glyph_" + matcher.group(2)));
        }
        return constants;
    }

    private static void extractInstanceof(List<GlyphRuleCandidate> candidates, ResourceLocation glyphId, String effectClass, String source) {
        Matcher matcher = INSTANCEOF.matcher(source);
        while (matcher.find()) {
            String className = simpleName(matcher.group(1));
            GlyphTargetRule.MatcherType type = looksLikeEntityClass(className)
                    ? GlyphTargetRule.MatcherType.ENTITY_CLASS
                    : GlyphTargetRule.MatcherType.BLOCK_CLASS;
            GlyphRuleCandidate.Polarity polarity = isNegativeContext(source, matcher.start())
                    ? GlyphRuleCandidate.Polarity.NEGATIVE
                    : GlyphRuleCandidate.Polarity.POSITIVE;
            GlyphTargetRule.MatcherType matcherType = polarity == GlyphRuleCandidate.Polarity.NEGATIVE
                    ? GlyphTargetRule.MatcherType.BLACKLIST
                    : type;
            String value = polarity == GlyphRuleCandidate.Polarity.NEGATIVE
                    ? (type == GlyphTargetRule.MatcherType.ENTITY_CLASS ? "entity_class:" : "block_class:") + className
                    : className;
            add(candidates, glyphId, effectClass, matcherType, value, GlyphRuleCandidate.Confidence.HIGH, polarity, evidenceLine(source, matcher.start()));
        }
    }

    private static void extractTagCalls(
            List<GlyphRuleCandidate> candidates,
            ResourceLocation glyphId,
            String effectClass,
            String source,
            Pattern pattern,
            GlyphTargetRule.MatcherType positiveType,
            String label
    ) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            GlyphRuleCandidate.Polarity polarity = isNegativeContext(source, matcher.start()) || looksLikeDeny(value)
                    ? GlyphRuleCandidate.Polarity.NEGATIVE
                    : GlyphRuleCandidate.Polarity.POSITIVE;
            GlyphTargetRule.MatcherType type = polarity == GlyphRuleCandidate.Polarity.NEGATIVE
                    ? GlyphTargetRule.MatcherType.BLACKLIST
                    : positiveType;
            String matcherValue = polarity == GlyphRuleCandidate.Polarity.NEGATIVE
                    ? (positiveType == GlyphTargetRule.MatcherType.ITEM_TAG ? "item_tag:" : "block_tag:") + value
                    : value;
            add(candidates, glyphId, effectClass, type, matcherValue, GlyphRuleCandidate.Confidence.HIGH, polarity, label + ": " + evidenceLine(source, matcher.start()));
        }
    }

    private static void extractEntityTypeTags(List<GlyphRuleCandidate> candidates, ResourceLocation glyphId, String effectClass, String source) {
        Matcher matcher = ENTITY_TYPE_IS.matcher(source);
        while (matcher.find()) {
            String value = matcher.group(1).trim();
            GlyphRuleCandidate.Polarity polarity = isNegativeContext(source, matcher.start()) || looksLikeDeny(value)
                    ? GlyphRuleCandidate.Polarity.NEGATIVE
                    : GlyphRuleCandidate.Polarity.POSITIVE;
            GlyphTargetRule.MatcherType type = polarity == GlyphRuleCandidate.Polarity.NEGATIVE
                    ? GlyphTargetRule.MatcherType.DENY_ENTITY_TYPE_TAG
                    : GlyphTargetRule.MatcherType.ENTITY_TYPE_TAG;
            add(candidates, glyphId, effectClass, type, value, GlyphRuleCandidate.Confidence.HIGH, polarity, "entity.getType().is(...): " + evidenceLine(source, matcher.start()));
        }
    }

    private static void extractBehaviorSignals(List<GlyphRuleCandidate> candidates, ResourceLocation glyphId, String effectClass, String source) {
        for (BehaviorSignal signal : BEHAVIOR_SIGNALS) {
            int index = source.indexOf(signal.sourceText());
            if (index >= 0) {
                add(candidates, glyphId, effectClass, signal.matcherType(), signal.matcherValue(),
                        GlyphRuleCandidate.Confidence.HIGH, GlyphRuleCandidate.Polarity.POSITIVE,
                        evidenceLine(source, index));
            }
        }
    }

    private static void add(
            List<GlyphRuleCandidate> candidates,
            ResourceLocation glyphId,
            String effectClass,
            GlyphTargetRule.MatcherType matcherType,
            String matcherValue,
            GlyphRuleCandidate.Confidence confidence,
            GlyphRuleCandidate.Polarity polarity,
            String evidenceText
    ) {
        GlyphRuleCandidate candidate = new GlyphRuleCandidate(glyphId, effectClass, matcherType, matcherValue, confidence, polarity, evidenceText);
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private static Optional<String> findFirst(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static ResourceLocation fallbackGlyphId(String effectClass) {
        String path = effectClass.replaceFirst("^Effect", "")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase(java.util.Locale.ROOT);
        return ResourceLocation.parse("ars_nouveau:glyph_" + path);
    }

    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    private static boolean looksLikeEntityClass(String className) {
        if (ENTITY_CLASS_HINTS.contains(className)) {
            return true;
        }
        return className.endsWith("Entity") || className.endsWith("Player") || className.endsWith("Mob");
    }

    private static boolean isNegativeContext(String source, int startIndex) {
        int from = Math.max(0, startIndex - 80);
        String prefix = source.substring(from, startIndex).toLowerCase(java.util.Locale.ROOT);
        return prefix.matches("(?s).*(!\\s*\\(?\\s*)$") || prefix.contains("blacklist") || prefix.contains("deny") || prefix.contains("not_supported");
    }

    private static boolean looksLikeDeny(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("deny") || lower.contains("blacklist") || lower.contains("not_supported") || lower.contains("unsupported");
    }

    private static String evidenceLine(String source, int index) {
        int lineStart = source.lastIndexOf('\n', Math.max(0, index - 1)) + 1;
        int lineEnd = source.indexOf('\n', index);
        if (lineEnd < 0) {
            lineEnd = source.length();
        }
        return source.substring(lineStart, lineEnd).trim();
    }

    private record BehaviorSignal(String sourceText, GlyphTargetRule.MatcherType matcherType, String matcherValue) {
    }
}
