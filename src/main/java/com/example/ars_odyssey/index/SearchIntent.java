package com.example.ars_odyssey.index;

import net.minecraft.resources.ResourceLocation;

public record SearchIntent(
        String rawQuery,
        SearchIntentType type,
        ResourceLocation resolvedId,
        String displayName
) {
    public SearchIntent {
        rawQuery = rawQuery == null ? "" : rawQuery;
        type = type == null ? SearchIntentType.UNKNOWN : type;
        displayName = displayName == null ? "" : displayName;
    }
}
