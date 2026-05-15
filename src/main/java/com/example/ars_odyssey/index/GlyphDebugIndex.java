package com.example.ars_odyssey.index;

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

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public final class GlyphDebugIndex {
    private static final Logger LOGGER = LogManager.getLogger();

    private GlyphDebugIndex() {
    }

    public static void printAllGlyphs() {
        Map<ResourceLocation, AbstractSpellPart> glyphs = GlyphRegistry.getSpellpartMap();
        LOGGER.info("[Ars Odyssey] GlyphRegistry contains {} spell parts.", glyphs.size());

        glyphs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> printGlyph(entry.getKey(), entry.getValue()));

        GlyphApplicationIndex.printRules();
    }

    private static void printGlyph(ResourceLocation id, AbstractSpellPart spellPart) {
        LOGGER.info(
                "[Ars Odyssey] Glyph id={} localeName=\"{}\" class={} abstractEffect={} potionEffect={} damageEffect={} schools={} compatibleAugments={} bookDescription=\"{}\"",
                id,
                spellPart.getLocaleName(),
                spellPart.getClass().getName(),
                spellPart instanceof AbstractEffect,
                spellPart instanceof IPotionEffect,
                spellPart instanceof IDamageEffect,
                spellPart.spellSchools.stream().map(SpellSchool::getId).sorted().collect(Collectors.toList()),
                spellPart.compatibleAugments.stream()
                        .map(AbstractAugment::getRegistryName)
                        .map(ResourceLocation::toString)
                        .sorted()
                        .collect(Collectors.toList()),
                spellPart.getBookDescLang().getString()
        );
    }
}
