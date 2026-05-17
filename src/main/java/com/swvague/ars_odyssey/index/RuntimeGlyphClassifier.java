package com.swvague.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.IDamageEffect;
import com.hollingsworth.arsnouveau.api.spell.IPotionEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.swvague.ars_odyssey.index.GlyphTargetRule.EvidenceLevel.INFERRED;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.EvidenceLevel.RUNTIME_CHECK;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.BEHAVIOR;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.ENTITY_CLASS;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_BLOCK;
import static com.swvague.ars_odyssey.index.GlyphTargetRule.MatcherType.GENERAL_ENTITY;

/**
 * Runtime inference classifier that predicts target matchers for {@link AbstractEffect} glyphs
 * without requiring source code. Operates against the live JVM class hierarchy and the AN API
 * surface, making it effective for both known Ars Nouveau glyphs and unknown addon glyphs.
 *
 * <h3>Five-phase signal pipeline</h3>
 * <ol>
 *   <li><b>Interface probes</b> — {@link IDamageEffect} and {@link IPotionEffect} each imply
 *       a definite target type with {@link EvidenceConfidence#HIGH} confidence.</li>
 *   <li><b>Method override detection</b> — Reflection checks whether the concrete class
 *       (or any intermediate superclass short of {@link AbstractEffect}) declares
 *       {@code onResolveEntity} or {@code onResolveBlock}, giving HIGH confidence.</li>
 *   <li><b>School analysis</b> — Maps the effect's declared {@link SpellSchool} IDs
 *       to likely target types with {@link EvidenceConfidence#MEDIUM} or LOW confidence.</li>
 *   <li><b>Augment compatibility probes</b> — Presence of entity-oriented augments (Amplify,
 *       ExtendTime) suggests entity targeting at MEDIUM confidence; area augments (AOE, Pierce)
 *       suggest it at LOW confidence.</li>
 *   <li><b>Name/ID keyword matching</b> — Scans the glyph's registry path, class simple name,
 *       and locale name against curated keyword sets, yielding LOW-confidence signals.</li>
 * </ol>
 *
 * <p>Results are cached after the first call and reused for the lifetime of the game session.
 * Call {@link #clearCache()} if the glyph registry changes (uncommon outside of hot-reloading).
 *
 * <p>Only {@link AbstractEffect} subclasses receive inferred rules; cast methods and augments
 * are skipped because they do not resolve targets in the search context.
 */
public final class RuntimeGlyphClassifier {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Runtime Classifier");

    private static volatile InferredRuleSet cachedRuleSet;

    private RuntimeGlyphClassifier() {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the cached inferred rule set, building it on first call.
     * Convenience overload that reads the current manual rules automatically.
     * Prefer this form when calling from outside the {@code index} package.
     */
    public static InferredRuleSet getRuleSet() {
        return getRuleSet(ManualGlyphRules.rules());
    }

    /**
     * Returns the cached inferred rule set, building it on first call.
     *
     * @param manualRules the current manual rules — glyphs covered here are skipped by the
     *                    classifier so inferred rules never shadow hand-curated ones.
     */
    public static InferredRuleSet getRuleSet(List<GlyphTargetRule> manualRules) {
        InferredRuleSet result = cachedRuleSet;
        if (result == null) {
            synchronized (RuntimeGlyphClassifier.class) {
                result = cachedRuleSet;
                if (result == null) {
                    result = buildRuleSet(manualRules);
                    cachedRuleSet = result;
                }
            }
        }
        return result;
    }

    /** Drops the cached rule set so it is rebuilt on the next {@link #getRuleSet} call. */
    public static void clearCache() {
        cachedRuleSet = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build
    // ─────────────────────────────────────────────────────────────────────────

    static InferredRuleSet buildRuleSet(List<GlyphTargetRule> manualRules) {
        // Build a set of IDs already covered by manual rules so we never overwrite them.
        Set<ResourceLocation> covered = new java.util.HashSet<>();
        if (manualRules != null) {
            for (GlyphTargetRule rule : manualRules) {
                covered.add(rule.glyphId());
            }
        }

        List<GlyphTargetRule> rules = new ArrayList<>();
        for (Map.Entry<ResourceLocation, AbstractSpellPart> entry : GlyphRegistry.getSpellpartMap().entrySet()) {
            ResourceLocation id = entry.getKey();
            if (covered.contains(id)) {
                continue;
            }
            classify(id, entry.getValue()).ifPresent(rules::add);
        }

        LOGGER.debug("[Ars Odyssey] RuntimeGlyphClassifier: inferred {} rules ({} glyphs already covered by manual rules)",
                rules.size(), covered.size());
        return new InferredRuleSet(List.copyOf(rules));
    }

    /**
     * Attempts to classify a single glyph into a {@link GlyphTargetRule}.
     * Returns {@link Optional#empty()} for non-effect glyphs or when no signals fire.
     */
    static Optional<GlyphTargetRule> classify(ResourceLocation id, AbstractSpellPart part) {
        if (!(part instanceof AbstractEffect)) {
            return Optional.empty(); // forms and augments are not classified
        }

        List<Signal> signals = new ArrayList<>();
        signals.addAll(probeInterfaces(part));
        signals.addAll(probeMethodOverrides(part));
        signals.addAll(probeSchools(part));
        signals.addAll(probeAugments(part));
        signals.addAll(probeName(id, part));

        List<GlyphTargetRule.TargetMatcher> matchers = deduplicateToMatchers(signals);
        if (matchers.isEmpty()) {
            return Optional.empty();
        }

        LOGGER.debug("[Ars Odyssey] RuntimeGlyphClassifier: inferred {} matcher(s) for {}", matchers.size(), id);
        return Optional.of(new GlyphTargetRule(
                id, matchers, List.of("inferred"),
                "Runtime-inferred: " + part.getClass().getSimpleName()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 1 — Interface probes
    // ─────────────────────────────────────────────────────────────────────────

    private static List<Signal> probeInterfaces(AbstractSpellPart part) {
        List<Signal> signals = new ArrayList<>();
        if (part instanceof IDamageEffect) {
            // Damage effects hit any entity, and specifically living entities.
            signals.add(Signal.entity(EvidenceConfidence.HIGH, RUNTIME_CHECK, "IDamageEffect"));
            signals.add(Signal.living(EvidenceConfidence.HIGH, RUNTIME_CHECK, "IDamageEffect"));
            signals.add(new Signal(BEHAVIOR, "damage_entity", EvidenceConfidence.HIGH, RUNTIME_CHECK, "IDamageEffect"));
        }
        if (part instanceof IPotionEffect) {
            // Potion effects are always applied to LivingEntity.
            signals.add(Signal.living(EvidenceConfidence.HIGH, RUNTIME_CHECK, "IPotionEffect"));
        }
        return signals;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2 — Method override detection
    // ─────────────────────────────────────────────────────────────────────────

    private static List<Signal> probeMethodOverrides(AbstractSpellPart part) {
        List<Signal> signals = new ArrayList<>();
        Class<?> concrete = part.getClass();
        if (overridesMethod(concrete, AbstractEffect.class, "onResolveEntity")) {
            signals.add(Signal.entity(EvidenceConfidence.HIGH, RUNTIME_CHECK, "onResolveEntity"));
        }
        if (overridesMethod(concrete, AbstractEffect.class, "onResolveBlock")) {
            signals.add(Signal.block(EvidenceConfidence.HIGH, RUNTIME_CHECK, "onResolveBlock"));
        }
        return signals;
    }

    /**
     * Returns {@code true} if any class in the hierarchy from {@code concrete} (inclusive) up to
     * {@code base} (exclusive) declares a non-synthetic, non-bridge method with {@code name}.
     */
    private static boolean overridesMethod(Class<?> concrete, Class<?> base, String name) {
        try {
            Class<?> cls = concrete;
            while (cls != null && !cls.equals(base)) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().equals(name) && !m.isSynthetic() && !m.isBridge()) {
                        return true;
                    }
                }
                cls = cls.getSuperclass();
            }
        } catch (Exception | LinkageError ignored) {
            // Reflection may fail for obfuscated or classloader-isolated addon classes; ignore.
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3 — School analysis
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * School IDs from {@code SpellSchools} (base AN) and common addon conventions,
     * mapped to their inferred target type and confidence.
     *
     * <p>Base AN schools: abjuration, conjuration, necromancy, manipulation,
     * air, earth, fire, water, elemental.
     */
    private static List<Signal> probeSchools(AbstractSpellPart part) {
        List<Signal> signals = new ArrayList<>();
        try {
            for (SpellSchool school : part.spellSchools) {
                String id = school.getId().toLowerCase(Locale.ROOT);
                // ─ Entity-leaning schools (positive effects on living) ─
                // "holy", "healing", "life" come from addons like Ars Elements.
                if (id.contains("holy") || id.contains("heal") || id.contains("life")
                        || id.equals("abjuration")) {
                    signals.add(Signal.living(EvidenceConfidence.MEDIUM, INFERRED, "school=" + school.getId()));
                }
                // ─ Summon schools → summons appear as entities ─
                if (id.equals("conjuration") || id.contains("summon") || id.contains("conjur")) {
                    signals.add(Signal.entity(EvidenceConfidence.MEDIUM, INFERRED, "school=" + school.getId()));
                }
                // ─ Death/undead → entity ─
                if (id.equals("necromancy") || id.contains("necro") || id.contains("death")) {
                    signals.add(Signal.entity(EvidenceConfidence.MEDIUM, INFERRED, "school=" + school.getId()));
                }
                // ─ Terrain / growth schools → block ─
                // "earth" in base AN covers both growth and block manipulation.
                if (id.equals("earth") || id.contains("nature") || id.contains("growth")
                        || id.contains("terrain") || id.contains("plant")) {
                    signals.add(Signal.block(EvidenceConfidence.MEDIUM, INFERRED, "school=" + school.getId()));
                }
                // ─ Broad elemental / motion / void schools → entity (LOW) ─
                if (id.equals("elemental") || id.equals("fire") || id.equals("water")
                        || id.equals("air") || id.contains("manipul") || id.contains("arcane")
                        || id.contains("ender") || id.contains("void") || id.contains("wind")) {
                    signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "school=" + school.getId()));
                }
            }
        } catch (Exception | LinkageError ignored) {
            // spellSchools field access is safe for all known AN versions; ignore exotic failures.
        }
        return signals;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 4 — Augment compatibility probes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Augment paths that indicate the effect targets living entities (typically used by
     * damage effects, potion effects, and summon effects).
     */
    private static final Set<String> ENTITY_AUGMENT_PATHS = Set.of(
            "augment_amplify",       // increases damage / potion level
            "augment_extend_time",   // extends potion / summon duration
            "augment_sensitive"      // sensitive effects require entity line-of-sight
    );

    /**
     * Augment paths that indicate the effect has an area component — usually entity-targeting
     * but at a lower confidence than the entity-specific augments above.
     */
    private static final Set<String> AREA_AUGMENT_PATHS = Set.of(
            "augment_aoe",
            "augment_pierce",
            "augment_split",
            "augment_scatter"
    );

    private static List<Signal> probeAugments(AbstractSpellPart part) {
        List<Signal> signals = new ArrayList<>();
        try {
            boolean foundEntity = false;
            boolean foundArea = false;
            for (AbstractAugment aug : part.compatibleAugments) {
                ResourceLocation augId = aug.getRegistryName();
                if (augId == null) {
                    continue;
                }
                String path = augId.getPath();
                if (!foundEntity && ENTITY_AUGMENT_PATHS.contains(path)) {
                    signals.add(Signal.entity(EvidenceConfidence.MEDIUM, INFERRED, "augment=" + path));
                    foundEntity = true;
                }
                if (!foundArea && AREA_AUGMENT_PATHS.contains(path)) {
                    signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "augment=" + path));
                    foundArea = true;
                }
                if (foundEntity && foundArea) {
                    break;
                }
            }
        } catch (Exception | LinkageError ignored) {
        }
        return signals;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 5 — Name / ID keyword analysis
    // ─────────────────────────────────────────────────────────────────────────

    private static final Set<String> KW_ENTITY_DAMAGE = Set.of(
            "harm", "damage", "hurt", "strike", "smite", "wound", "assault", "shock",
            "lightning", "bolt", "blast", "flare", "nova", "slash", "shred", "fang",
            "bite", "sting", "decay", "corrupt", "wither", "blight", "plague",
            "pestilence", "curse", "poison", "venom", "burn"
    );
    private static final Set<String> KW_ENTITY_HEAL = Set.of(
            "heal", "mend", "cure", "restore", "regen", "life", "revive", "rejuv", "vit", "salve"
    );
    private static final Set<String> KW_ENTITY_MOTION = Set.of(
            "launch", "fling", "repel", "attract", "gravity", "levit", "propel",
            "knockback", "velocity", "gust", "hurl", "push", "pull"
    );
    private static final Set<String> KW_ENTITY_CONTROL = Set.of(
            "root", "snare", "bind", "entangle", "web", "stun", "paralyze", "slow",
            "haste", "speed", "charm", "confuse", "dizzy", "fear", "freeze", "frost", "ice",
            "chill", "cold", "glacial", "frigid", "blizzard", "snow"
    );
    private static final Set<String> KW_ENTITY_SUMMON = Set.of(
            "summon", "conjure", "raise", "spawn", "awaken", "invoke", "animate"
    );
    private static final Set<String> KW_BLOCK = Set.of(
            "grow", "growth", "bloom", "flourish", "sprout", "harvest", "crop", "plant",
            "seed", "fertil", "ripen", "break", "destroy", "demolish", "excavate",
            "collapse", "shatter", "mine", "dig", "place", "build", "till", "hoe"
    );
    private static final Set<String> KW_TELEPORT = Set.of(
            "teleport", "blink", "warp", "phase", "dash", "leap", "shift", "portal"
    );
    private static final Set<String> KW_FIRE_ENV = Set.of(
            "ignite", "flame", "fire", "blaze", "scorch", "incinerate", "combust",
            "inferno", "heat", "ember", "torch"
    );

    private static List<Signal> probeName(ResourceLocation id, AbstractSpellPart part) {
        List<Signal> signals = new ArrayList<>();
        String tokens = nameTokens(id, part);

        // Damage → entity + living
        if (matchesAny(tokens, KW_ENTITY_DAMAGE)) {
            signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "name~damage"));
            signals.add(Signal.living(EvidenceConfidence.LOW, INFERRED, "name~damage"));
        }
        // Healing → living only
        if (matchesAny(tokens, KW_ENTITY_HEAL)) {
            signals.add(Signal.living(EvidenceConfidence.LOW, INFERRED, "name~heal"));
        }
        // Motion / control → general entity
        if (matchesAny(tokens, KW_ENTITY_MOTION) || matchesAny(tokens, KW_ENTITY_CONTROL)) {
            signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "name~control"));
        }
        // Summoning → general entity
        if (matchesAny(tokens, KW_ENTITY_SUMMON)) {
            signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "name~summon"));
        }
        // Teleport → entity
        if (matchesAny(tokens, KW_TELEPORT)) {
            signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "name~teleport"));
        }
        // Fire-themed → entity (ignite/burn) + block (place fire)
        if (matchesAny(tokens, KW_FIRE_ENV)) {
            signals.add(Signal.entity(EvidenceConfidence.LOW, INFERRED, "name~fire"));
        }
        // Block manipulation / growth
        if (matchesAny(tokens, KW_BLOCK)) {
            signals.add(Signal.block(EvidenceConfidence.LOW, INFERRED, "name~block"));
        }
        return signals;
    }

    private static String nameTokens(ResourceLocation id, AbstractSpellPart part) {
        StringBuilder sb = new StringBuilder();
        sb.append(id.getPath().toLowerCase(Locale.ROOT));
        sb.append(' ');
        sb.append(id.getNamespace().toLowerCase(Locale.ROOT));
        sb.append(' ');
        sb.append(part.getClass().getSimpleName().toLowerCase(Locale.ROOT));
        String localeName = part.getLocaleName();
        if (localeName != null) {
            sb.append(' ').append(localeName.toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static boolean matchesAny(String tokens, Set<String> keywords) {
        for (String kw : keywords) {
            if (tokens.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Signal deduplication → TargetMatcher list
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Merges all signals into a deduplicated list of matchers.
     * When multiple signals map to the same {@code (type, value)} key, only the
     * highest-confidence one is kept; the most descriptive evidence text wins.
     */
    private static List<GlyphTargetRule.TargetMatcher> deduplicateToMatchers(List<Signal> signals) {
        Map<String, Signal> best = new LinkedHashMap<>();
        for (Signal s : signals) {
            String key = s.type().name() + ':' + s.value();
            Signal existing = best.get(key);
            if (existing == null || confidenceOrd(s.confidence()) > confidenceOrd(existing.confidence())) {
                best.put(key, s);
            }
        }
        return best.values().stream().map(Signal::toMatcher).toList();
    }

    private static int confidenceOrd(EvidenceConfidence c) {
        return switch (c) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case UNKNOWN -> 0;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal signal record
    // ─────────────────────────────────────────────────────────────────────────

    private record Signal(
            GlyphTargetRule.MatcherType type,
            String value,
            EvidenceConfidence confidence,
            GlyphTargetRule.EvidenceLevel evidenceLevel,
            String evidenceText
    ) {
        /** Convenience factory: GENERAL_ENTITY → "true" */
        static Signal entity(EvidenceConfidence confidence, GlyphTargetRule.EvidenceLevel evidenceLevel, String evidenceText) {
            return new Signal(GENERAL_ENTITY, "true", confidence, evidenceLevel, evidenceText);
        }

        /** Convenience factory: GENERAL_BLOCK → "true" */
        static Signal block(EvidenceConfidence confidence, GlyphTargetRule.EvidenceLevel evidenceLevel, String evidenceText) {
            return new Signal(GENERAL_BLOCK, "true", confidence, evidenceLevel, evidenceText);
        }

        /** Convenience factory: ENTITY_CLASS → "LivingEntity" */
        static Signal living(EvidenceConfidence confidence, GlyphTargetRule.EvidenceLevel evidenceLevel, String evidenceText) {
            return new Signal(ENTITY_CLASS, "LivingEntity", confidence, evidenceLevel, evidenceText);
        }

        GlyphTargetRule.TargetMatcher toMatcher() {
            return new GlyphTargetRule.TargetMatcher(type, value, confidence, evidenceText, evidenceLevel);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public result type
    // ─────────────────────────────────────────────────────────────────────────

    /** Holds the complete set of rules inferred at runtime for this session. */
    public record InferredRuleSet(List<GlyphTargetRule> rules) {
    }
}
