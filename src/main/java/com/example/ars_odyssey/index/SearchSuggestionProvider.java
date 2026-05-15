package com.example.ars_odyssey.index;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SearchSuggestionProvider {
    private SearchSuggestionProvider() {
    }

    public static List<SearchSuggestion> suggest(String rawQuery, int limit) {
        return suggest(rawQuery, SearchSuggestionFilter.ALL, limit);
    }

    public static List<SearchSuggestion> suggest(String rawQuery, SearchSuggestionFilter filter, int limit) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isBlank() || limit <= 0) {
            return List.of();
        }

        SearchSuggestionFilter activeFilter = filter == null ? SearchSuggestionFilter.ALL : filter;
        List<SearchSuggestion> suggestions = new ArrayList<>();

        if (activeFilter.accepts(SearchIntentType.GLYPH)) {
            addGlyphSuggestions(suggestions, query);
        }
        if (activeFilter.accepts(SearchIntentType.ENTITY)) {
            addEntitySuggestions(suggestions, query);
        }
        if (activeFilter.accepts(SearchIntentType.BLOCK)) {
            addBlockSuggestions(suggestions, query);
        }
        if (activeFilter.accepts(SearchIntentType.ITEM)) {
            addItemSuggestions(suggestions, query);
        }

        return suggestions.stream()
                .filter(suggestion -> suggestion.score() > 0)
                .sorted(Comparator
                        .comparingInt(SearchSuggestion::score).reversed()
                        .thenComparing(suggestion -> suggestion.type().ordinal())
                        .thenComparing(SearchSuggestion::queryText))
                .limit(limit)
                .toList();
    }

    private static void addGlyphSuggestions(List<SearchSuggestion> suggestions, String query) {
        for (Map.Entry<ResourceLocation, AbstractSpellPart> entry : GlyphRegistry.getSpellpartMap().entrySet()) {
            ResourceLocation id = entry.getKey();
            AbstractSpellPart part = entry.getValue();
            String displayName = part.getLocaleName();
            String path = id.getPath();
            String strippedPath = path.startsWith("glyph_") ? path.substring("glyph_".length()) : path;
            int score = score(query, SearchIntentType.GLYPH, id, id.toString(), displayName, path, strippedPath);
            suggestions.add(new SearchSuggestion(SearchIntentType.GLYPH, id, id.toString(), displayName, score + 20));
        }
    }

    private static void addEntitySuggestions(List<SearchSuggestion> suggestions, String query) {
        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            String displayName = Component.translatable(entityType.getDescriptionId()).getString();
            int score = score(query, SearchIntentType.ENTITY, id, id.toString(), displayName, id.getPath());
            suggestions.add(new SearchSuggestion(SearchIntentType.ENTITY, id, id.toString(), displayName, score));
        }
    }

    private static void addBlockSuggestions(List<SearchSuggestion> suggestions, String query) {
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            String displayName = Component.translatable(block.getDescriptionId()).getString();
            int score = score(query, SearchIntentType.BLOCK, id, id.toString(), displayName, id.getPath());
            suggestions.add(new SearchSuggestion(SearchIntentType.BLOCK, id, id.toString(), displayName, score));
        }
    }

    private static void addItemSuggestions(List<SearchSuggestion> suggestions, String query) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String displayName = Component.translatable(item.getDescriptionId()).getString();
            int score = score(query, SearchIntentType.ITEM, id, id.toString(), displayName, id.getPath());
            suggestions.add(new SearchSuggestion(SearchIntentType.ITEM, id, id.toString(), displayName, score));
        }
    }

    private static int score(String query, SearchIntentType type, ResourceLocation id, String queryText, String displayName, String... aliases) {
        PinyinSearchIndex index = PinyinSearchIndex.of(type, id, queryText, displayName, aliases);
        return index.score(query);
    }
}
