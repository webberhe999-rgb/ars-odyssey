package com.example.ars_odyssey.knowledge.discovery.entity;

import com.example.ars_odyssey.knowledge.discovery.DisplayEffectCategory;
import com.example.ars_odyssey.knowledge.discovery.GlyphDisplayCategoryMapper;
import com.example.ars_odyssey.knowledge.discovery.GlyphEffectCategory;
import com.example.ars_odyssey.knowledge.discovery.GlyphEffectCategoryIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ObservedEffectClassifier {

    private ObservedEffectClassifier() {
    }

    /**
     * 根据实体观察结果（快照 + effective signals）推断展示分类。
     * 使用 before.undead() 区分 heal 对亡灵目标的行为（造成伤害而非治疗）。
     * effectiveSignals 参数保留用于未来扩展（当前以静态 index 为准）。
     */
    public static Set<DisplayEffectCategory> classifyForEntityTarget(
            ResourceLocation glyphId,
            EntityTargetSnapshot before,
            EntityTargetSnapshot after,
            Set<EntityObservationSignal> effectiveSignals
    ) {
        if (glyphId == null) {
            return Set.of(DisplayEffectCategory.UNKNOWN);
        }

        boolean isUndead = before != null && before.undead();
        Set<GlyphEffectCategory> cats = GlyphEffectCategoryIndex.getCategories(glyphId);
        Set<DisplayEffectCategory> result = new LinkedHashSet<>();

        for (GlyphEffectCategory cat : cats) {
            result.add(GlyphDisplayCategoryMapper.mapToDisplay(cat, isUndead));
        }

        return Collections.unmodifiableSet(result);
    }
}
