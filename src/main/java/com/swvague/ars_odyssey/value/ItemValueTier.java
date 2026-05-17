package com.swvague.ars_odyssey.value;

public enum ItemValueTier {
    COMMON(0, 1.0D),
    MATERIAL(1, 2.0D),
    ARCANE(2, 4.0D),
    RARE(3, 8.0D),
    LEGENDARY(4, 16.0D),
    FORBIDDEN(5, Double.POSITIVE_INFINITY),
    MYTHIC(10, 64.0D),
    PRIMORDIAL(25, 256.0D),
    AXIOMATIC(77, 4096.0D),
    SINGULARITY(99, 65536.0D);

    private final int level;
    private final double sourceCostMultiplier;

    ItemValueTier(int level, double sourceCostMultiplier) {
        this.level = level;
        this.sourceCostMultiplier = sourceCostMultiplier;
    }

    public int level() {
        return level;
    }

    public double sourceCostMultiplier() {
        return sourceCostMultiplier;
    }

    public boolean isAtLeast(ItemValueTier other) {
        return level >= other.level;
    }

    public static ItemValueTier fromLevel(int level) {
        for (ItemValueTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        ItemValueTier closest = COMMON;
        for (ItemValueTier tier : values()) {
            if (tier.level <= level && tier.level > closest.level) {
                closest = tier;
            }
        }
        return closest;
    }

    public static ItemValueTier max(ItemValueTier first, ItemValueTier second) {
        return first.level >= second.level ? first : second;
    }
}
