package com.swvague.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

public record SearchSuggestion(
        SearchIntentType type,
        ResourceLocation id,
        String queryText,
        String displayName,
        int score
) {
    public SearchSuggestion {
        type = type == null ? SearchIntentType.UNKNOWN : type;
        queryText = queryText == null ? "" : queryText;
        displayName = displayName == null ? "" : displayName;
    }
}
