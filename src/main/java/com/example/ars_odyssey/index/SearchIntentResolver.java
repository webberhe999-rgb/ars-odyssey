package com.example.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SearchIntentResolver {
    private SearchIntentResolver() {
    }

    public static SearchIntent resolve(String query) {
        String rawQuery = query == null ? "" : query;
        String normalizedQuery = rawQuery.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return new SearchIntent(rawQuery, SearchIntentType.EMPTY, null, "");
        }

        boolean explicitTag = normalizedQuery.startsWith("#");
        String idText = explicitTag ? normalizedQuery.substring(1) : normalizedQuery;
        Optional<ResourceLocation> resolvedId = parseQueryId(idText);

        if (explicitTag) {
            return new SearchIntent(rawQuery, SearchIntentType.TAG, resolvedId.orElse(null), displayName(resolvedId, idText));
        }

        if (resolvedId.isPresent()) {
            ResourceLocation id = resolvedId.get();
            if (isEntityType(id)) {
                return new SearchIntent(rawQuery, SearchIntentType.ENTITY, id, id.toString());
            }
            if (isBlock(id)) {
                return new SearchIntent(rawQuery, SearchIntentType.BLOCK, id, id.toString());
            }
            if (isItem(id)) {
                return new SearchIntent(rawQuery, SearchIntentType.ITEM, id, id.toString());
            }
            if (GlyphRegistry.getSpellpartMap().containsKey(id)) {
                return new SearchIntent(rawQuery, SearchIntentType.GLYPH, id, id.toString());
            }
        }

        Optional<ResourceLocation> glyphId = findGlyphId(normalizedQuery);
        if (glyphId.isPresent()) {
            return new SearchIntent(rawQuery, SearchIntentType.GLYPH, glyphId.get(), glyphId.get().toString());
        }

        return new SearchIntent(rawQuery, SearchIntentType.TEXT, null, normalizedQuery);
    }

    private static Optional<ResourceLocation> parseQueryId(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation explicitId = ResourceLocation.tryParse(query);
        if (explicitId != null && query.contains(":")) {
            return Optional.of(explicitId);
        }
        return Optional.ofNullable(ResourceLocation.tryParse("minecraft:" + query));
    }

    private static Optional<ResourceLocation> findGlyphId(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation arsGlyphId = ResourceLocation.tryParse("ars_nouveau:glyph_" + query);
        if (arsGlyphId != null && GlyphRegistry.getSpellpartMap().containsKey(arsGlyphId)) {
            return Optional.of(arsGlyphId);
        }

        ResourceLocation arsId = ResourceLocation.tryParse("ars_nouveau:" + query);
        if (arsId != null && GlyphRegistry.getSpellpartMap().containsKey(arsId)) {
            return Optional.of(arsId);
        }

        String normalizedQuery = normalizeLoose(query);
        for (Map.Entry<ResourceLocation, AbstractSpellPart> entry : GlyphRegistry.getSpellpartMap().entrySet()) {
            ResourceLocation glyphId = entry.getKey();
            String path = glyphId.getPath().toLowerCase(Locale.ROOT);
            String strippedPath = path.startsWith("glyph_") ? path.substring("glyph_".length()) : path;
            if (path.equals(query) || path.equals("glyph_" + query) || strippedPath.equals(query)) {
                return Optional.of(glyphId);
            }

            AbstractSpellPart spellPart = entry.getValue();
            String localeName = spellPart.getLocaleName();
            if (localeName != null) {
                String normalizedLocaleName = normalizeLoose(localeName);
                if (normalizedLocaleName.equals(normalizedQuery) || normalizedLocaleName.contains(normalizedQuery)) {
                    return Optional.of(glyphId);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isEntityType(ResourceLocation id) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(id);
    }

    private static boolean isBlock(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return BuiltInRegistries.BLOCK.getKey(block).equals(id);
    }

    private static boolean isItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return BuiltInRegistries.ITEM.getKey(item).equals(id);
    }

    private static String displayName(Optional<ResourceLocation> id, String fallback) {
        return id.map(ResourceLocation::toString).orElse(fallback);
    }

    private static String normalizeLoose(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }
}
