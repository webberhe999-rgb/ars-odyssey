package com.example.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class GlyphRuntimeDiscovery {
    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Glyph Runtime Discovery");
    private static List<DiscoveredGlyph> cachedGlyphs;

    private GlyphRuntimeDiscovery() {
    }

    public static List<DiscoveredGlyph> discoverGlyphs() {
        if (cachedGlyphs == null) {
            cachedGlyphs = GlyphRegistry.getSpellpartMap().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                    .map(entry -> discoverGlyph(entry.getKey(), entry.getValue()))
                    .toList();
            LOGGER.info("[Ars Odyssey] Runtime glyph discovery cached {} glyphs.", cachedGlyphs.size());
        }
        return cachedGlyphs;
    }

    public static void clearCache() {
        cachedGlyphs = null;
        LOGGER.info("[Ars Odyssey] Runtime glyph discovery cache cleared.");
    }

    public static void printDiscoveredGlyphs() {
        List<DiscoveredGlyph> glyphs = discoverGlyphs();
        LOGGER.info("[Ars Odyssey] Runtime glyph discovery contains {} glyphs.", glyphs.size());
        for (DiscoveredGlyph glyph : glyphs) {
            LOGGER.info("[Ars Odyssey] RuntimeGlyph id={} namespace={} class={} category={}",
                    glyph.id(),
                    glyph.namespace(),
                    glyph.className(),
                    glyph.category());
        }
    }

    private static DiscoveredGlyph discoverGlyph(ResourceLocation id, AbstractSpellPart spellPart) {
        String className = spellPart.getClass().getName();
        String simpleName = spellPart.getClass().getSimpleName();
        return new DiscoveredGlyph(id, id.getNamespace(), className, classify(simpleName));
    }

    private static GlyphCategory classify(String simpleName) {
        if (simpleName.startsWith("Effect")) {
            return GlyphCategory.EFFECT;
        }
        if (simpleName.startsWith("Form")) {
            return GlyphCategory.FORM;
        }
        if (simpleName.startsWith("Augment")) {
            return GlyphCategory.AUGMENT;
        }
        return GlyphCategory.UNKNOWN;
    }

    public enum GlyphCategory {
        EFFECT,
        FORM,
        AUGMENT,
        UNKNOWN
    }

    public record DiscoveredGlyph(
            ResourceLocation id,
            String namespace,
            String className,
            GlyphCategory category
    ) {
    }
}
