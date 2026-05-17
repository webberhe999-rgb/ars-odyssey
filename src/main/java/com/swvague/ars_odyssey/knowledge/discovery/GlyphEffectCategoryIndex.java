package com.swvague.ars_odyssey.knowledge.discovery;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import com.swvague.ars_odyssey.knowledge.DiscoverySource;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

public final class GlyphEffectCategoryIndex {
    private static final Map<ResourceLocation, GlyphEffectCategoryHint> HINTS = Map.ofEntries(
            hint("ars_nouveau:glyph_harm",
                    GlyphEffectCategory.ENTITY_DIRECT_DAMAGE),
            hint("ars_nouveau:glyph_heal",
                    GlyphEffectCategory.ENTITY_HEAL),
            hint("ars_nouveau:glyph_ignite",
                    GlyphEffectCategory.ENTITY_STATUS_DEBUFF,
                    GlyphEffectCategory.ENTITY_INDIRECT_DAMAGE),
            hint("ars_nouveau:glyph_freeze",
                    GlyphEffectCategory.ENTITY_STATUS_CONTROL,
                    GlyphEffectCategory.BLOCK_TRANSFORM),
            hint("ars_nouveau:glyph_lightning",
                    GlyphEffectCategory.ENTITY_INDIRECT_DAMAGE,
                    GlyphEffectCategory.ENTITY_STATUS_DEBUFF),
            hint("ars_nouveau:glyph_blink",
                    GlyphEffectCategory.ENTITY_TELEPORT),
            hint("ars_nouveau:glyph_bounce",
                    GlyphEffectCategory.ENTITY_MOTION),
            hint("ars_nouveau:glyph_launch",
                    GlyphEffectCategory.ENTITY_MOTION),
            hint("ars_nouveau:glyph_gust",
                    GlyphEffectCategory.ENTITY_MOTION),
            hint("ars_nouveau:glyph_pull",
                    GlyphEffectCategory.ENTITY_MOTION),
            hint("ars_nouveau:glyph_break",
                    GlyphEffectCategory.BLOCK_BREAK,
                    GlyphEffectCategory.BLOCK_HARVEST,
                    GlyphEffectCategory.PRODUCE),
            hint("ars_nouveau:glyph_harvest",
                    GlyphEffectCategory.BLOCK_HARVEST,
                    GlyphEffectCategory.PRODUCE),
            hint("ars_nouveau:glyph_grow",
                    GlyphEffectCategory.BLOCK_GROWTH,
                    GlyphEffectCategory.PRODUCE),
            hint("ars_nouveau:glyph_pickup",
                    GlyphEffectCategory.ITEM_COLLECT),
            hint("ars_nouveau:glyph_smelt",
                    GlyphEffectCategory.RESOURCE_TRANSFORM,
                    GlyphEffectCategory.PRODUCE),
            hint("ars_nouveau:glyph_crush",
                    GlyphEffectCategory.RESOURCE_TRANSFORM,
                    GlyphEffectCategory.PRODUCE),
            hint("ars_nouveau:glyph_exchange",
                    GlyphEffectCategory.BLOCK_TRANSFORM),
            hint("ars_nouveau:glyph_place_block",
                    GlyphEffectCategory.BLOCK_PLACE),
            hint("ars_nouveau:glyph_animate_block",
                    GlyphEffectCategory.ENTITY_SUMMON,
                    GlyphEffectCategory.BLOCK_TRANSFORM),
            // fangs: 召唤 EvokerFangs 作为伤害载体，最终归因到被伤害实体（APPLIES_TO）
            // 不归为 ENTITY_SUMMON（EvokerFangs 是实现手段，不是产物）
            hint("ars_nouveau:glyph_fangs",
                    GlyphEffectCategory.ENTITY_DELAYED_DAMAGE),
            // 召唤类：均为 ENTITY_SUMMON，不作用于目标实体本身
            hint("ars_nouveau:glyph_summon_wolves",
                    GlyphEffectCategory.ENTITY_SUMMON),
            hint("ars_nouveau:glyph_summon_undead",
                    GlyphEffectCategory.ENTITY_SUMMON),
            hint("ars_nouveau:glyph_summon_steed",
                    GlyphEffectCategory.ENTITY_SUMMON),
            hint("ars_nouveau:glyph_summon_vex",
                    GlyphEffectCategory.ENTITY_SUMMON),
            hint("ars_nouveau:glyph_summon_decoy",
                    GlyphEffectCategory.ENTITY_SUMMON)
    );

    private GlyphEffectCategoryIndex() {
    }

    public static GlyphEffectCategoryHint getHint(ResourceLocation glyphId) {
        GlyphEffectCategoryHint hint = HINTS.get(glyphId);
        if (hint != null) {
            return hint;
        }
        return new GlyphEffectCategoryHint(
                glyphId,
                Set.of(GlyphEffectCategory.UNKNOWN),
                EvidenceConfidence.UNKNOWN,
                DiscoverySource.STATIC_SOURCE_SCAN,
                "");
    }

    public static Set<GlyphEffectCategory> getCategories(ResourceLocation glyphId) {
        return getHint(glyphId).categories();
    }

    public static boolean hasCategory(ResourceLocation glyphId, GlyphEffectCategory category) {
        return getCategories(glyphId).contains(category);
    }

    private static Map.Entry<ResourceLocation, GlyphEffectCategoryHint> hint(String glyphId, GlyphEffectCategory... categories) {
        ResourceLocation id = ResourceLocation.parse(glyphId);
        return Map.entry(id, new GlyphEffectCategoryHint(
                id,
                Set.of(categories),
                EvidenceConfidence.HIGH,
                DiscoverySource.STATIC_SOURCE_SCAN,
                "manual_effect_category_hint"));
    }
}
