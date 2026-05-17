package com.swvague.ars_odyssey.index;

public final class SearchIntentResolver {
    private SearchIntentResolver() {
    }

    public static SearchIntent resolve(String query) {
        return TargetInputResolver.resolveIntent(query);
    }
}
