package com.swvague.ars_odyssey.index;

public enum SearchSuggestionFilter {
    ALL("ars_odyssey.search.filter.all"),
    GLYPH("ars_odyssey.search.filter.glyph"),
    ENTITY("ars_odyssey.search.filter.entity"),
    BLOCK("ars_odyssey.search.filter.block"),
    ITEM("ars_odyssey.search.filter.item");

    private final String translationKey;

    SearchSuggestionFilter(String translationKey) {
        this.translationKey = translationKey;
    }

    public boolean accepts(SearchIntentType type) {
        return switch (this) {
            case ALL -> type == SearchIntentType.GLYPH
                    || type == SearchIntentType.ENTITY
                    || type == SearchIntentType.BLOCK
                    || type == SearchIntentType.ITEM;
            case GLYPH -> type == SearchIntentType.GLYPH;
            case ENTITY -> type == SearchIntentType.ENTITY;
            case BLOCK -> type == SearchIntentType.BLOCK;
            case ITEM -> type == SearchIntentType.ITEM;
        };
    }

    public SearchSuggestionFilter next() {
        SearchSuggestionFilter[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return translationKey;
    }
}
