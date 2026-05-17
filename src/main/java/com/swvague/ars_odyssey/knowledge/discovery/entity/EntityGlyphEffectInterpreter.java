package com.swvague.ars_odyssey.knowledge.discovery.entity;

import com.swvague.ars_odyssey.knowledge.discovery.GlyphEffectCategory;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphEffectCategoryIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 根据魔符的 GlyphEffectCategory 和目标特性，将 raw signals 过滤为 effective signals。
 *
 * raw signals  = 实体快照比较发现的所有实际变化
 * effective signals = 对当前 glyph 语义有解释价值的变化子集
 *
 * 例：
 *   harm 打 zombie：raw=[HEALTH_DECREASED, VELOCITY_CHANGED] → effective=[HEALTH_DECREASED]
 *   heal 打 zombie（亡灵）：raw=[HEALTH_DECREASED] → effective=[HEALTH_DECREASED]
 *   heal 打 cow（活物）：raw=[HEALTH_INCREASED] → effective=[HEALTH_INCREASED]
 *   ignite 打 zombie：raw=[FIRE_TICKS_INCREASED] → effective=[FIRE_TICKS_INCREASED]
 */
public final class EntityGlyphEffectInterpreter {
    private EntityGlyphEffectInterpreter() {
    }

    /**
     * 主入口：根据 glyphId 查分类，结合目标的 before 快照过滤 effective signals。
     */
    public static Set<EntityObservationSignal> effectiveSignalsFor(
            ResourceLocation glyphId,
            EntityTargetSnapshot before,
            EntityTargetSnapshot after,
            Set<EntityObservationSignal> rawSignals
    ) {
        if (rawSignals == null || rawSignals.isEmpty()) {
            return Set.of();
        }
        if (rawSignals.contains(EntityObservationSignal.NO_OBSERVABLE_CHANGE)) {
            return Set.of();
        }

        Set<GlyphEffectCategory> categories = GlyphEffectCategoryIndex.getCategories(glyphId);
        boolean targetIsUndead = before != null && before.undead();

        EnumSet<EntityObservationSignal> effective = EnumSet.noneOf(EntityObservationSignal.class);

        for (GlyphEffectCategory category : categories) {
            addSignalsForCategory(effective, rawSignals, category, targetIsUndead);
        }

        return Collections.unmodifiableSet(effective);
    }

    private static void addSignalsForCategory(
            EnumSet<EntityObservationSignal> out,
            Set<EntityObservationSignal> raw,
            GlyphEffectCategory category,
            boolean targetIsUndead
    ) {
        switch (category) {
            case ENTITY_DIRECT_DAMAGE ->
                // harm 类：只保留血量变化，击退/位移是伴随噪声
                keepInto(out, raw,
                        EntityObservationSignal.HEALTH_DECREASED,
                        EntityObservationSignal.ENTITY_DIED);

            case ENTITY_INDIRECT_DAMAGE ->
                // 间接伤害：主要通过火焰/冰冻间接造成伤害
                keepInto(out, raw,
                        EntityObservationSignal.FIRE_TICKS_INCREASED,
                        EntityObservationSignal.HEALTH_DECREASED,
                        EntityObservationSignal.ENTITY_DIED);

            case ENTITY_HEAL -> {
                // heal 对亡灵实体：heal 会造成伤害（isInvertedHealAndHarm）
                // heal 对普通实体：heal 会回血
                if (targetIsUndead) {
                    keepInto(out, raw,
                            EntityObservationSignal.HEALTH_DECREASED,
                            EntityObservationSignal.ENTITY_DIED,
                            EntityObservationSignal.HEALTH_INCREASED,
                            EntityObservationSignal.MOB_EFFECT_REMOVED);
                } else {
                    keepInto(out, raw,
                            EntityObservationSignal.HEALTH_INCREASED,
                            EntityObservationSignal.MOB_EFFECT_REMOVED);
                    // 对活物，HEALTH_DECREASED 和 ENTITY_DIED 不是 heal 的预期效果，排除
                }
            }

            case ENTITY_STATUS_DEBUFF ->
                keepInto(out, raw,
                        EntityObservationSignal.FIRE_TICKS_INCREASED,
                        EntityObservationSignal.FROZEN_TICKS_INCREASED,
                        EntityObservationSignal.MOB_EFFECT_ADDED);

            case ENTITY_STATUS_BUFF ->
                keepInto(out, raw,
                        EntityObservationSignal.MOB_EFFECT_ADDED,
                        EntityObservationSignal.HEALTH_INCREASED);

            case ENTITY_STATUS_CONTROL ->
                // freeze 等控制：冰冻 tick 是主要信号，位移变化暂不算有效
                keepInto(out, raw,
                        EntityObservationSignal.FROZEN_TICKS_INCREASED,
                        EntityObservationSignal.MOB_EFFECT_ADDED);

            case ENTITY_STATUS_CLEANSE ->
                keepInto(out, raw,
                        EntityObservationSignal.MOB_EFFECT_REMOVED);

            case ENTITY_MOTION ->
                // bounce / launch：速度和位置变化都是有效信号
                keepInto(out, raw,
                        EntityObservationSignal.VELOCITY_CHANGED,
                        EntityObservationSignal.POSITION_CHANGED);

            case ENTITY_TELEPORT ->
                // blink：位置变化是主信号
                keepInto(out, raw,
                        EntityObservationSignal.POSITION_CHANGED);

            case ENTITY_DELAYED_DAMAGE -> {
                // 召唤体/投射物造成的延迟伤害（如 Fangs）
                // Pre/Post 同步窗口内无法观察到，intentionally no signals
            }

            default -> {
                // BLOCK_* / ITEM_* / RESOURCE_TRANSFORM / PRODUCE / UTILITY / UNKNOWN
                // 以及 ENTITY_SUMMON、ENTITY_TRANSFORM 等：对实体快照观察无语义
            }
        }
    }

    private static void keepInto(
            EnumSet<EntityObservationSignal> out,
            Set<EntityObservationSignal> raw,
            EntityObservationSignal... candidates
    ) {
        for (EntityObservationSignal signal : candidates) {
            if (raw.contains(signal)) {
                out.add(signal);
            }
        }
    }
}
