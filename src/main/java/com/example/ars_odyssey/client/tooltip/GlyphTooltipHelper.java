package com.example.ars_odyssey.client.tooltip;

import com.example.ars_odyssey.index.EvidenceConfidence;
import com.example.ars_odyssey.index.GlyphApplicationIndex;
import com.example.ars_odyssey.index.MatchDisplayLabel;
import com.example.ars_odyssey.index.MatchReason;
import com.example.ars_odyssey.knowledge.EmptyPlayerKnowledgeView;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeView;
import com.example.ars_odyssey.knowledge.complexity.GlyphComplexityRecord;
import com.example.ars_odyssey.knowledge.discovery.DisplayEffectCategory;
import com.example.ars_odyssey.knowledge.discovery.GlyphDisplayCategoryMapper;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
        if (glyphId == null) {
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
        tooltip.add(Component.translatable("ars_odyssey.tooltip.truth_entanglement", formatDouble(record.truthEntanglement(resolvedCount)))
                .withStyle(ChatFormatting.GRAY));
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
