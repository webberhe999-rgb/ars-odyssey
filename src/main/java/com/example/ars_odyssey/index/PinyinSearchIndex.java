package com.example.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record PinyinSearchIndex(
        SearchIntentType type,
        ResourceLocation id,
        String queryText,
        String displayName,
        List<String> exactKeys,
        List<String> prefixKeys,
        List<String> containsKeys
) {
    public PinyinSearchIndex {
        queryText = queryText == null ? "" : queryText;
        displayName = displayName == null ? "" : displayName;
        exactKeys = List.copyOf(exactKeys == null ? List.of() : exactKeys);
        prefixKeys = List.copyOf(prefixKeys == null ? List.of() : prefixKeys);
        containsKeys = List.copyOf(containsKeys == null ? List.of() : containsKeys);
    }

    public static PinyinSearchIndex of(SearchIntentType type, ResourceLocation id, String queryText, String displayName, String... aliases) {
        List<String> exact = new ArrayList<>();
        List<String> prefix = new ArrayList<>();
        List<String> contains = new ArrayList<>();

        addTextKeys(exact, prefix, contains, id == null ? "" : id.toString());
        addTextKeys(exact, prefix, contains, id == null ? "" : id.getPath());
        addTextKeys(exact, prefix, contains, queryText);
        addTextKeys(exact, prefix, contains, displayName);
        for (String alias : aliases) {
            addTextKeys(exact, prefix, contains, alias);
        }

        return new PinyinSearchIndex(type, id, queryText, displayName, exact, prefix, contains);
    }

    public int score(String rawQuery) {
        String query = ChineseSearchNormalizer.normalizeLoose(rawQuery);
        if (query.isBlank()) {
            return 0;
        }

        if (exactKeys.contains(query)) {
            return 1000;
        }
        if (startsWithAny(prefixKeys, query)) {
            return 820;
        }
        if (containsAny(containsKeys, query)) {
            return 620;
        }
        return 0;
    }

    private static void addTextKeys(List<String> exact, List<String> prefix, List<String> contains, String value) {
        String normalized = ChineseSearchNormalizer.normalizeLoose(value);
        if (!normalized.isBlank()) {
            exact.add(normalized);
            prefix.add(normalized);
            contains.add(normalized);
        }

        ChineseSearchNormalizer.PinyinKeys pinyinKeys = ChineseSearchNormalizer.keysFor(value);
        addGeneratedKey(exact, prefix, contains, pinyinKeys.fullPinyin());
        addGeneratedKey(exact, prefix, contains, pinyinKeys.initials());
    }

    private static void addGeneratedKey(List<String> exact, List<String> prefix, List<String> contains, String value) {
        String normalized = ChineseSearchNormalizer.normalizeLoose(value);
        if (!normalized.isBlank()) {
            exact.add(normalized);
            prefix.add(normalized);
            contains.add(normalized);
        }
    }

    private static boolean startsWithAny(List<String> values, String query) {
        for (String value : values) {
            if (value.startsWith(query)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(List<String> values, String query) {
        for (String value : values) {
            if (value.contains(query)) {
                return true;
            }
        }
        return false;
    }
}
