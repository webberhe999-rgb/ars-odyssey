package com.swvague.ars_odyssey.knowledge.discovery;

import com.swvague.ars_odyssey.api.event.OdysseyRelationDiscoveredEvent;
import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.swvague.ars_odyssey.knowledge.RelationType;
import com.swvague.ars_odyssey.knowledge.complexity.EntanglementConfidenceMultiplier;
import com.swvague.ars_odyssey.knowledge.complexity.GlyphComplexityRecord;
import com.swvague.ars_odyssey.item.OdysseySpellBook;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DiscoveryFeedbackService {
    private DiscoveryFeedbackService() {
    }

    public static DiscoveryApplyResult applyAndNotify(
            ServerPlayer player,
            GlyphDiscoveryResult result,
            List<AbstractSpellPart> spellParts
    ) {
        if (player == null || result == null || result.relation() == null) {
            return DiscoveryApplyResult.notAdded();
        }

        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        boolean added = GlyphDiscoveryService.applyDiscovery(data, result);
        GlyphRelation relation = result.relation();
        ResourceLocation glyphId = relation.glyphId();
        Optional<GlyphComplexityRecord> complexityIncrease =
                GlyphDiscoveryService.updateComplexity(data, glyphId, spellParts, OdysseySpellBook.activeComplexityTierBonus(player));

        if (!added) {
            if (complexityIncrease.isPresent()) {
                player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
                ModNetwork.syncKnowledge(player, data);
                sendComplexityIncrease(player, complexityIncrease.get());
            }
            int resolvedCount = new PlayerKnowledgeDataView(data, player.level())
                    .countResolvedRelations(glyphId);
            return new DiscoveryApplyResult(false, complexityIncrease, resolvedCount, 0.0, 1.0);
        }

        // ── 真理纠缠度奖励 ──
        // delta = 当前最高复杂度 × 置信倍率（低置信魔符更难预测，奖励更多）
        double currentComplexity = data.getComplexityRecord(glyphId).highestComplexity();
        double confidenceMultiplier = EntanglementConfidenceMultiplier.forGlyph(glyphId);
        double entanglementDelta = currentComplexity * confidenceMultiplier;
        data.addTruthEntanglement(entanglementDelta);

        // 持久化并同步到客户端（包含 totalTruthEntanglement）
        player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
        ModNetwork.syncKnowledge(player, data);

        int resolvedCount = new PlayerKnowledgeDataView(data, player.level())
                .countResolvedRelations(glyphId);
        sendDiscoveryMessage(player, relation, resolvedCount);
        sendEntanglementMessage(player, glyphId, entanglementDelta, confidenceMultiplier,
                data.getTotalTruthEntanglement());
        complexityIncrease.ifPresent(record -> sendComplexityIncrease(player, record));

        // Notify other mods that a new relation was discovered.
        NeoForge.EVENT_BUS.post(new OdysseyRelationDiscoveredEvent(player, relation, resolvedCount));

        return new DiscoveryApplyResult(true, complexityIncrease, resolvedCount,
                entanglementDelta, confidenceMultiplier);
    }

    private static void sendDiscoveryMessage(ServerPlayer player, GlyphRelation relation, int resolvedCount) {
        ResourceLocation targetId = relation.target().id();
        if (relation.relationType() == RelationType.PRODUCES) {
            player.sendSystemMessage(Component.translatable(
                    "message.ars_odyssey.discovery.summon_observed",
                    glyphDisplayName(relation.glyphId()),
                    targetId != null ? entityDisplayName(targetId) : Component.literal("?"),
                    resolvedCount));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.ars_odyssey.discovery.entity_resolved",
                    glyphDisplayName(relation.glyphId()),
                    targetId != null ? entityDisplayName(targetId) : Component.literal("?"),
                    resolvedCount));
        }
    }

    private static void sendEntanglementMessage(
            ServerPlayer player,
            ResourceLocation glyphId,
            double entanglementDelta,
            double confidenceMultiplier,
            double totalEntanglement
    ) {
        player.sendSystemMessage(Component.translatable(
                "message.ars_odyssey.discovery.entanglement_gained",
                glyphDisplayName(glyphId),
                String.format(Locale.ROOT, "%.2f", entanglementDelta),
                String.format(Locale.ROOT, "×%.2f", confidenceMultiplier),
                String.format(Locale.ROOT, "%.2f", totalEntanglement)
        ));
    }

    private static void sendComplexityIncrease(ServerPlayer player, GlyphComplexityRecord record) {
        player.sendSystemMessage(Component.translatable(
                "message.ars_odyssey.complexity.increased",
                glyphDisplayName(record.glyphId()),
                String.format(Locale.ROOT, "%.2f", record.highestComplexity()),
                formatAchievedBy(record)));
    }

    private static Component formatAchievedBy(GlyphComplexityRecord record) {
        net.minecraft.network.chat.MutableComponent text = Component.empty();
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

    private static Component glyphDisplayName(ResourceLocation glyphId) {
        AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(glyphId);
        if (part != null) {
            return Component.literal(part.getLocaleName());
        }
        return Component.literal(glyphId != null ? glyphId.toString() : "?");
    }

    private static Component entityDisplayName(ResourceLocation entityTypeId) {
        if (entityTypeId == null) {
            return Component.literal("?");
        }
        net.minecraft.world.entity.EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(entityTypeId)) {
            return entityType.getDescription();
        }
        return Component.literal(entityTypeId.toString());
    }

    public record DiscoveryApplyResult(
            boolean added,
            Optional<GlyphComplexityRecord> complexityIncrease,
            int resolvedCount,
            /** 本次发现贡献的真理纠缠度增量（= currentComplexity × confidenceMultiplier）。 */
            double entanglementDelta,
            /** 本次使用的置信倍率（1.0 / 1.35 / 2.0）。 */
            double confidenceMultiplier
    ) {
        private static DiscoveryApplyResult notAdded() {
            return new DiscoveryApplyResult(false, Optional.empty(), 0, 0.0, 1.0);
        }

        public DiscoveryApplyResult {
            complexityIncrease = complexityIncrease == null ? Optional.empty() : complexityIncrease;
        }
    }
}
