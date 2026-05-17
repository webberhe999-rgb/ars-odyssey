package com.swvague.ars_odyssey.client.tooltip;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import com.swvague.ars_odyssey.config.OdysseyConfig;
import com.swvague.ars_odyssey.index.GlyphApplicationIndex;
import com.swvague.ars_odyssey.index.MatchDisplayLabel;
import com.swvague.ars_odyssey.index.MatchReason;
import com.swvague.ars_odyssey.glyph.AugmentOrbitSelf;
import com.swvague.ars_odyssey.knowledge.EmptyPlayerKnowledgeView;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeView;
import com.swvague.ars_odyssey.knowledge.complexity.GlyphComplexityRecord;
import com.swvague.ars_odyssey.knowledge.complexity.TruthEntanglementCalculator;
import com.swvague.ars_odyssey.knowledge.truth.AugmentTruthificationResolver;
import com.swvague.ars_odyssey.knowledge.truth.TruthificationInsightRules;
import com.swvague.ars_odyssey.knowledge.discovery.DisplayEffectCategory;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDisplayCategoryMapper;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class GlyphTooltipHelper {
    private GlyphTooltipHelper() {
    }

    /**
     * 向 tooltip 追加 Ars Odyssey 摘要行，适用于任何效果类魔符物品。
     *
     * 显示规则（互斥，优先级从高到低）：
     * 1. resolvedCount > 0  → [已解析] 已解析个数 X
     * 2. 有系统规则推断    → [HIGH/MEDIUM/LOW] 系统推断提示
     * 3. 无规则数据        → [UNKNOWN] 暂无系统数据
     *
     * 总是会追加内容，调用方只需保证 glyphId 是 AbstractEffect 对应的 id。
     */
    public static void appendOdysseyTooltip(ResourceLocation glyphId, List<Component> tooltip) {
        appendOdysseyTooltip(glyphId, tooltip, null);
    }

    public static void appendOdysseyTooltip(ResourceLocation glyphId, List<Component> tooltip, Boolean truthificationEnabledForSlot) {
        if (glyphId == null) {
            return;
        }
        AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(glyphId);
        replaceTruthifiedDescriptionIfNeeded(glyphId, tooltip, part, truthificationEnabledForSlot);
        if (!(part instanceof AbstractEffect)) {
            return;
        }

        PlayerKnowledgeView knowledge = getKnowledgeView();
        int resolvedCount = knowledge.countResolvedRelations(glyphId);

        appendHeader(tooltip);

        boolean resolved = resolvedCount > 0;
        if (resolved) {
            MatchDisplayLabel label = MatchDisplayLabel.resolved();
            tooltip.add(buildLabelLine(label,
                    Component.translatable("ars_odyssey.tooltip.resolved_count", resolvedCount)));
        } else {
            Optional<MatchReason> bestReason = GlyphApplicationIndex.bestReasonForGlyph(glyphId);
            if (bestReason.isPresent()) {
                MatchDisplayLabel label = MatchDisplayLabel.confidence(bestReason.get().confidence());
                tooltip.add(buildLabelLine(label,
                        Component.translatable("ars_odyssey.tooltip.system_hint")));
            } else {
                MatchDisplayLabel label = MatchDisplayLabel.confidence(EvidenceConfidence.UNKNOWN);
                tooltip.add(buildLabelLine(label,
                        Component.translatable("ars_odyssey.tooltip.no_system_data")));
            }
        }

        // 已解析时显示"效果类型"，未解析时显示"系统推断分类"
        appendCategoryLine(tooltip, GlyphDisplayCategoryMapper.forGlyph(glyphId), !resolved);
        appendComplexitySummary(tooltip, glyphId, resolvedCount);
    }

    public static void appendEntityTargetCategoryLine(List<Component> tooltip, ResourceLocation glyphId, ResourceLocation entityTypeId) {
        // 实体目标上下文：仅展示与实体相关的分类，且始终标注为系统推断
        Set<DisplayEffectCategory> cats = GlyphDisplayCategoryMapper.forEntityTarget(glyphId, entityTypeId);
        appendCategoryLine(tooltip, cats, true);
    }

    private static void appendCategoryLine(List<Component> tooltip, Set<DisplayEffectCategory> categories, boolean inferred) {
        Set<DisplayEffectCategory> filtered = new java.util.LinkedHashSet<>();
        for (DisplayEffectCategory cat : categories) {
            if (cat != DisplayEffectCategory.UNKNOWN) {
                filtered.add(cat);
            }
        }
        if (filtered.isEmpty()) {
            return;
        }
        // inferred=true → "系统推断分类: X"；inferred=false → "效果类型: X"
        String prefixKey = inferred ? "ars_odyssey.tooltip.inferred_category" : "ars_odyssey.tooltip.effect_type";
        MutableComponent line = Component.translatable(prefixKey)
                .append(Component.literal(": "));
        boolean first = true;
        for (DisplayEffectCategory cat : filtered) {
            if (!first) {
                line = line.append(Component.literal(", "));
            }
            line = line.append(Component.translatable(cat.translationKey()));
            first = false;
        }
        tooltip.add(line.withStyle(ChatFormatting.GRAY));
    }

    private static void appendComplexitySummary(List<Component> tooltip, ResourceLocation glyphId, int resolvedCount) {
        GlyphComplexityRecord record = getComplexityRecord(glyphId);
        tooltip.add(Component.translatable("ars_odyssey.tooltip.max_complexity", formatDouble(record.highestComplexity()))
                .withStyle(ChatFormatting.GRAY));
        if (!record.achievedByGlyphs().isEmpty()) {
            tooltip.add(Component.translatable("ars_odyssey.tooltip.achieved_by", formatAchievedBy(record))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("ars_odyssey.tooltip.glyph_entanglement", formatDouble(getGlyphEntanglement(glyphId)))
                .withStyle(ChatFormatting.GRAY));
    }

    public static void replaceTruthifiedDescriptionIfNeeded(ResourceLocation glyphId, List<Component> tooltip) {
        replaceTruthifiedDescriptionIfNeeded(glyphId, tooltip, (Boolean) null);
    }

    public static void replaceTruthifiedDescriptionIfNeeded(ResourceLocation glyphId, List<Component> tooltip, Boolean truthificationEnabledForSlot) {
        replaceTruthifiedDescriptionIfNeeded(
                glyphId,
                tooltip,
                GlyphRegistry.getSpellpartMap().get(glyphId),
                truthificationEnabledForSlot);
    }

    private static void replaceTruthifiedDescriptionIfNeeded(ResourceLocation glyphId, List<Component> tooltip, AbstractSpellPart part) {
        replaceTruthifiedDescriptionIfNeeded(glyphId, tooltip, part, null);
    }

    private static void replaceTruthifiedDescriptionIfNeeded(
            ResourceLocation glyphId,
            List<Component> tooltip,
            AbstractSpellPart part,
            Boolean truthificationEnabledForSlot
    ) {
        if (glyphId == null || tooltip == null || part == null) {
            return;
        }
        if (!AugmentOrbitSelf.INSTANCE.getRegistryName().equals(glyphId)) {
            return;
        }

        Component truthifiedDescription = Component.translatable("ars_odyssey.glyph_desc.glyph_orbit_self")
                .withStyle(ChatFormatting.GRAY);
        List<Component> conditionLines = orbitSelfTruthificationConditionLines(truthificationEnabledForSlot);
        String originalDescription = part.getBookDescLang().getString();
        for (int i = 0; i < tooltip.size(); i++) {
            String line = tooltip.get(i).getString();
            boolean exactDescriptionLine = line.equals(originalDescription);
            boolean wrappedDescriptionLine = line.length() >= 12 && originalDescription.contains(line);
            if (!originalDescription.isBlank() && (exactDescriptionLine || wrappedDescriptionLine)) {
                tooltip.set(i, truthifiedDescription);
                tooltip.addAll(i + 1, conditionLines);
                return;
            }
        }

        tooltip.add(Component.empty());
        tooltip.add(truthifiedDescription);
        tooltip.addAll(conditionLines);
    }

    private static List<Component> orbitSelfTruthificationConditionLines() {
        return orbitSelfTruthificationConditionLines(null);
    }

    private static List<Component> orbitSelfTruthificationConditionLines(Boolean truthificationEnabledForSlot) {
        boolean forcedOff = Boolean.FALSE.equals(truthificationEnabledForSlot);
        double total = getTotalEntanglement();
        double strongestEffect = getHighestKnownEffectEntanglement();
        double required = OdysseyConfig.ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT.get();
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable(forcedOff
                        ? "ars_odyssey.glyph_desc.glyph_orbit_self.conditions_off"
                        : "ars_odyssey.glyph_desc.glyph_orbit_self.conditions")
                .withStyle(ChatFormatting.DARK_GRAY));
        lines.add(conditionLine(
                "ars_odyssey.glyph_desc.glyph_orbit_self.condition_unlock",
                !forcedOff && strongestEffect >= required,
                formatDouble(required)));
        lines.add(conditionLine(
                "ars_odyssey.glyph_desc.glyph_orbit_self.condition_stage_1",
                !forcedOff && strongestEffect >= AugmentTruthificationResolver.orbitTierOneThreshold(),
                formatDouble(AugmentTruthificationResolver.orbitTierOneThreshold()),
                formatPercent(OdysseyConfig.ORBIT_CONSUME_CHANCE_ONE.get())));
        lines.add(conditionLine(
                "ars_odyssey.glyph_desc.glyph_orbit_self.condition_stage_2",
                !forcedOff && strongestEffect >= OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_TWO.get(),
                formatDouble(OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_TWO.get()),
                formatPercent(OdysseyConfig.ORBIT_CONSUME_CHANCE_TWO.get())));
        lines.add(conditionLine(
                "ars_odyssey.glyph_desc.glyph_orbit_self.condition_stage_3",
                !forcedOff && strongestEffect >= OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_THREE.get(),
                formatDouble(OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_THREE.get()),
                formatPercent(OdysseyConfig.ORBIT_CONSUME_CHANCE_THREE.get())));

        double completeRequirement = TruthificationInsightRules.orbitSelfThirdCondition() * 15.0D;
        boolean complete = !forcedOff && TruthificationInsightRules.hasOrbitSelfCompleteInsight(total);
        Component completeLine = Component.translatable(
                "ars_odyssey.glyph_desc.glyph_orbit_self.condition_complete_insight",
                formatDouble(completeRequirement),
                formatPercent(OdysseyConfig.ORBIT_COMPLETE_INSIGHT_CONSUME_CHANCE.get()))
                .withColor(complete ? rainbowColor() : 0x777777);
        lines.add(completeLine);
        return List.copyOf(lines);
    }

    private static Component conditionLine(String key, boolean achieved, Object... args) {
        return Component.translatable(key, args)
                .withStyle(achieved ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    private static Component formatAchievedBy(GlyphComplexityRecord record) {
        if (record.achievedByGlyphs().isEmpty()) {
            return Component.translatable("ars_odyssey.tooltip.none");
        }

        MutableComponent text = Component.empty();
        boolean first = true;
        for (ResourceLocation glyphId : record.achievedByGlyphs()) {
            if (!first) {
                text = text.append(Component.literal(" + "));
            }
            text = text.append(glyphDisplayName(glyphId));
            first = false;
        }
        return text;
    }

    private static GlyphComplexityRecord getComplexityRecord(ResourceLocation glyphId) {
        if (Minecraft.getInstance().player == null) {
            return GlyphComplexityRecord.initial(glyphId);
        }
        try {
            PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            return data.getComplexityRecord(glyphId);
        } catch (Exception e) {
            return GlyphComplexityRecord.initial(glyphId);
        }
    }

    private static double getGlyphEntanglement(ResourceLocation glyphId) {
        if (Minecraft.getInstance().player == null) {
            return 0.0D;
        }
        try {
            PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            return TruthEntanglementCalculator.glyph(data, glyphId, Minecraft.getInstance().level);
        } catch (Exception e) {
            return 0.0D;
        }
    }

    private static double getHighestKnownEffectEntanglement() {
        if (Minecraft.getInstance().player == null) {
            return 0.0D;
        }
        try {
            PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            double highest = 0.0D;
            for (ResourceLocation glyphId : GlyphRegistry.getSpellpartMap().keySet()) {
                AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(glyphId);
                if (part instanceof AbstractEffect) {
                    highest = Math.max(highest, TruthEntanglementCalculator.glyph(data, glyphId, Minecraft.getInstance().level));
                }
            }
            return highest;
        } catch (Exception e) {
            return 0.0D;
        }
    }

    private static double getTotalEntanglement() {
        if (Minecraft.getInstance().player == null) {
            return 0.0D;
        }
        try {
            PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            return TruthEntanglementCalculator.total(data, Minecraft.getInstance().level);
        } catch (Exception e) {
            return 0.0D;
        }
    }

    private static boolean isTruthified(ResourceLocation glyphId) {
        if (Minecraft.getInstance().player == null || glyphId == null) {
            return false;
        }
        try {
            PlayerKnowledgeData data = Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
            return data != null && data.isTruthified(glyphId);
        } catch (Exception e) {
            return false;
        }
    }

    private static Component glyphDisplayName(ResourceLocation glyphId) {
        AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(glyphId);
        if (part != null) {
            return Component.literal(part.getLocaleName());
        }
        return Component.literal(glyphId.toString());
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    private static int rainbowColor() {
        float hue = (Util.getMillis() % 3000L) / 3000.0F;
        return java.awt.Color.HSBtoRGB(hue, 0.85F, 1.0F) & 0xFFFFFF;
    }

    private static void appendHeader(List<Component> tooltip) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("ars_odyssey.tooltip.header")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    private static Component buildLabelLine(MatchDisplayLabel label, Component body) {
        return Component.empty()
                .append(Component.literal("["))
                .append(label.text().copy().withColor(label.color()))
                .append(Component.literal("] "))
                .append(body);
    }

    private static PlayerKnowledgeView getKnowledgeView() {
        if (Minecraft.getInstance().player == null) {
            return EmptyPlayerKnowledgeView.INSTANCE;
        }
        try {
            return new PlayerKnowledgeDataView(
                    Minecraft.getInstance().player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE),
                    Minecraft.getInstance().level);
        } catch (Exception e) {
            return EmptyPlayerKnowledgeView.INSTANCE;
        }
    }
}
