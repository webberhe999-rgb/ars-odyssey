package com.swvague.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GlyphApplicationIndex {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Glyph Index");

    private GlyphApplicationIndex() {
    }

    public static List<GlyphTargetRule> getRules() {
        return ManualGlyphRules.rules();
    }

    public static Optional<GlyphTargetRule> getRule(ResourceLocation glyphId) {
        return ManualGlyphRules.find(glyphId);
    }

    public static List<AbstractSpellPart> search(String query, List<AbstractSpellPart> unlockedSpells) {
        return searchWithReasons(query, unlockedSpells).glyphs();
    }

    public static SearchResult searchWithReasons(String query, List<AbstractSpellPart> unlockedSpells) {
        return GlyphSearchService.searchWithReasons(query, unlockedSpells);
    }

    public static Optional<MatchReason> bestReasonForGlyph(ResourceLocation glyphId) {
        return GlyphSearchService.bestReasonForGlyph(glyphId);
    }

    public static TargetQuery parseTargetQuery(String query) {
        return TargetQueryParser.parse(query);
    }

    /**
     * Called during {@code FMLCommonSetupEvent} to register glyph rules from addon mods.
     * Collected from {@link com.swvague.ars_odyssey.api.event.OdysseyGlyphRuleEvent}.
     */
    public static void registerExternalRules(List<GlyphTargetRule> rules) {
        ManualGlyphRules.registerExternal(rules);
    }

    public static void printRules() {
        LOGGER.info("[Ars Odyssey] Glyph target rules: {}", ManualGlyphRules.rules().size());
        for (GlyphTargetRule rule : ManualGlyphRules.rules()) {
            LOGGER.info("[Ars Odyssey] TargetRule id={} categories={} note={}",
                    rule.glyphId(),
                    rule.categories(),
                    rule.note());
            for (GlyphTargetRule.TargetMatcher matcher : rule.matchers()) {
                LOGGER.info("[Ars Odyssey]   matcher type={} value={} role={} evidence={} override={}",
                        matcher.type(),
                        matcher.value(),
                        MatcherPresentation.roleOf(matcher),
                        MatcherPresentation.evidenceOf(matcher),
                        matcher.evidenceOverride());
            }
        }
    }

    public record SearchResult(List<AbstractSpellPart> glyphs, Map<ResourceLocation, List<MatchReason>> reasonsByGlyph) {
        public SearchResult {
            glyphs = List.copyOf(glyphs);
            reasonsByGlyph = Map.copyOf(reasonsByGlyph);
        }
    }
}
