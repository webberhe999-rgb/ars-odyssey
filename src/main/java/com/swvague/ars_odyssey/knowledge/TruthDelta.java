package com.swvague.ars_odyssey.knowledge;

public record TruthDelta(
        int amount,
        String reasonKey
) {
    public TruthDelta {
        reasonKey = reasonKey == null ? "" : reasonKey;
    }
}
