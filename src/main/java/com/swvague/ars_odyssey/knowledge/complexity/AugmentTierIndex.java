package com.swvague.ars_odyssey.knowledge.complexity;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;

/**
 * 查询增强魔符（Augment）的 tier 值。
 *
 * 数据来源：Ars Nouveau 注册表 GlyphRegistry + AbstractSpellPart.getConfigTier().value
 * tier 最小为 1，最大允许到 99。
 * CREATIVE tier 通常为 99，不再截断为 4。
 * 未注册或无法识别的 augment 默认 tier 1。
 */
public final class AugmentTierIndex {

    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 99;

    private AugmentTierIndex() {
    }

    /**
     * 返回指定 augment 的 tier。
     * 若 augment 不存在，返回 1。
     * 若 tier 小于 1，修正为 1。
     * 若 tier 大于 99，修正为 99。
     */
    public static int tierOf(ResourceLocation augmentId) {
        if (augmentId == null) return MIN_TIER;

        AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(augmentId);
        if (part == null) return MIN_TIER;

        int raw = part.getConfigTier().value;
        return Math.min(Math.max(raw, MIN_TIER), MAX_TIER);
    }
}
