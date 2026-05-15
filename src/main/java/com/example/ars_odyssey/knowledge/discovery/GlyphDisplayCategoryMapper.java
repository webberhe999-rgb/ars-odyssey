package com.example.ars_odyssey.knowledge.discovery;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GlyphDisplayCategoryMapper {

    // 在实体目标上下文中有意义的分类——用于搜索实体时过滤（过滤掉 BLOCK_*/RESOURCE_TRANSFORM 等）
    private static final Set<DisplayEffectCategory> ENTITY_RELEVANT = EnumSet.of(
            DisplayEffectCategory.DIRECT_DAMAGE,
            DisplayEffectCategory.INDIRECT_DAMAGE,
            DisplayEffectCategory.DELAYED_DAMAGE,
            DisplayEffectCategory.HEALING,
            DisplayEffectCategory.HEALING_REVERSED_DAMAGE,
            DisplayEffectCategory.STATUS_DEBUFF,
            DisplayEffectCategory.STATUS_BUFF,
            DisplayEffectCategory.STATUS_CONTROL,
            DisplayEffectCategory.STATUS_CLEANSE,
            DisplayEffectCategory.MOTION,
            DisplayEffectCategory.TELEPORT
    );

    private static final Set<String> KNOWN_UNDEAD_ENTITY_IDS = Set.of(
            "minecraft:zombie",
            "minecraft:zombie_villager",
            "minecraft:husk",
            "minecraft:drowned",
            "minecraft:skeleton",
            "minecraft:stray",
            "minecraft:wither_skeleton",
            "minecraft:phantom",
            "minecraft:wither",
            "minecraft:zoglin",
            "minecraft:zombified_piglin",
            "minecraft:skeleton_horse",
            "minecraft:zombie_horse"
    );

    private GlyphDisplayCategoryMapper() {
    }

    public static Set<DisplayEffectCategory> forGlyph(ResourceLocation glyphId) {
        Set<GlyphEffectCategory> cats = GlyphEffectCategoryIndex.getCategories(glyphId);
        Set<DisplayEffectCategory> result = new LinkedHashSet<>();
        for (GlyphEffectCategory cat : cats) {
            result.add(mapToDisplay(cat, false));
        }
        return result;
    }

    public static Set<DisplayEffectCategory> forEntityTarget(ResourceLocation glyphId, ResourceLocation entityTypeId) {
        boolean isUndead = entityTypeId != null && KNOWN_UNDEAD_ENTITY_IDS.contains(entityTypeId.toString());
        Set<GlyphEffectCategory> cats = GlyphEffectCategoryIndex.getCategories(glyphId);
        Set<DisplayEffectCategory> result = new LinkedHashSet<>();
        for (GlyphEffectCategory cat : cats) {
            DisplayEffectCategory dc = mapToDisplay(cat, isUndead);
            // 在实体目标上下文中只显示与实体相关的分类，过滤掉 BLOCK_*/RESOURCE_TRANSFORM 等
            if (ENTITY_RELEVANT.contains(dc)) {
                result.add(dc);
            }
        }
        return result;
    }

    public static DisplayEffectCategory mapToDisplay(GlyphEffectCategory cat, boolean targetIsUndead) {
        return switch (cat) {
            case ENTITY_DIRECT_DAMAGE -> DisplayEffectCategory.DIRECT_DAMAGE;
            case ENTITY_INDIRECT_DAMAGE -> DisplayEffectCategory.INDIRECT_DAMAGE;
            case ENTITY_HEAL -> targetIsUndead
                    ? DisplayEffectCategory.HEALING_REVERSED_DAMAGE
                    : DisplayEffectCategory.HEALING;
            case ENTITY_STATUS_CONTROL -> DisplayEffectCategory.STATUS_CONTROL;
            case ENTITY_STATUS_BUFF -> DisplayEffectCategory.STATUS_BUFF;
            case ENTITY_STATUS_DEBUFF -> DisplayEffectCategory.STATUS_DEBUFF;
            case ENTITY_STATUS_CLEANSE -> DisplayEffectCategory.STATUS_CLEANSE;
            case ENTITY_MOTION -> DisplayEffectCategory.MOTION;
            case ENTITY_TELEPORT -> DisplayEffectCategory.TELEPORT;
            case ENTITY_SUMMON -> DisplayEffectCategory.SUMMON;
            case ENTITY_TRANSFORM -> DisplayEffectCategory.SUMMON;
            case ENTITY_DELAYED_DAMAGE -> DisplayEffectCategory.DELAYED_DAMAGE;
            case BLOCK_BREAK -> DisplayEffectCategory.BLOCK_BREAK;
            case BLOCK_HARVEST -> DisplayEffectCategory.BLOCK_HARVEST;
            case BLOCK_GROWTH -> DisplayEffectCategory.BLOCK_GROWTH;
            case BLOCK_TRANSFORM, BLOCK_PLACE, BLOCK_SUMMON -> DisplayEffectCategory.BLOCK_TRANSFORM;
            case ITEM_COLLECT -> DisplayEffectCategory.ITEM_COLLECT;
            case ITEM_TRANSFORM, RESOURCE_TRANSFORM, PRODUCE -> DisplayEffectCategory.RESOURCE_TRANSFORM;
            case DEFENSE, UTILITY -> DisplayEffectCategory.UTILITY;
            case UNKNOWN_ENTITY, UNKNOWN_BLOCK, UNKNOWN_ITEM, UNKNOWN -> DisplayEffectCategory.UNKNOWN;
        };
    }
}
