package com.swvague.ars_odyssey.knowledge.discovery;

public enum DisplayEffectCategory {
    DIRECT_DAMAGE,
    INDIRECT_DAMAGE,
    DELAYED_DAMAGE,
    HEALING,
    HEALING_REVERSED_DAMAGE,

    STATUS_DEBUFF,
    STATUS_BUFF,
    STATUS_CONTROL,
    STATUS_CLEANSE,

    MOTION,
    TELEPORT,
    SUMMON,

    BLOCK_BREAK,
    BLOCK_HARVEST,
    BLOCK_GROWTH,
    BLOCK_TRANSFORM,

    ITEM_COLLECT,
    RESOURCE_TRANSFORM,
    UTILITY,
    UNKNOWN;

    public String translationKey() {
        return "ars_odyssey.effect_category." + name().toLowerCase();
    }
}
