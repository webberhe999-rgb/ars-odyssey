package com.example.ars_odyssey.knowledge.discovery.entity;

import com.example.ars_odyssey.index.EvidenceConfidence;
import com.example.ars_odyssey.knowledge.DiscoverySource;
import com.example.ars_odyssey.knowledge.GlyphRelation;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.example.ars_odyssey.knowledge.PlayerKnowledgeDataView;
import com.example.ars_odyssey.knowledge.RelationType;
import com.example.ars_odyssey.knowledge.TargetDescriptor;
import com.example.ars_odyssey.knowledge.TruthDelta;
import com.example.ars_odyssey.knowledge.complexity.GlyphComplexityRecord;
import com.example.ars_odyssey.knowledge.discovery.GlyphDiscoveryResult;
import com.example.ars_odyssey.knowledge.discovery.GlyphDiscoveryService;
import com.example.ars_odyssey.network.ModNetwork;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityEffectObservationHandler {
    public static final EntityEffectObservationHandler INSTANCE = new EntityEffectObservationHandler();

    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Entity Observation");
    private final Map<ObservationKey, EntityTargetSnapshot> pendingObservations = new ConcurrentHashMap<>();

    private EntityEffectObservationHandler() {
    }

    @SubscribeEvent
    public void onEffectPreResolve(EffectResolveEvent.Pre event) {
        if (!canObserve(event)) {
            return;
        }

        Entity target = ((EntityHitResult) event.rayTraceResult).getEntity();
        ResourceLocation glyphId = event.resolveEffect.getRegistryName();
        ObservationKey key = ObservationKey.of(
                event.shooter.getUUID(),
                glyphId,
                target.getUUID(),
                event.world.getGameTime(),
                event.resolver);

        pendingObservations.put(key, EntityTargetSnapshot.capture(target));
    }

    @SubscribeEvent
    public void onEffectPostResolve(EffectResolveEvent.Post event) {
        if (!canObserve(event)) {
            return;
        }

        Entity target = ((EntityHitResult) event.rayTraceResult).getEntity();
        ResourceLocation glyphId = event.resolveEffect.getRegistryName();
        ObservationKey key = ObservationKey.of(
                event.shooter.getUUID(),
                glyphId,
                target.getUUID(),
                event.world.getGameTime(),
                event.resolver);

        EntityTargetSnapshot before = pendingObservations.remove(key);
        if (before == null) {
            LOGGER.debug("[Ars Odyssey] Missing pre-snapshot for {} on {}", glyphId, target.getUUID());
            return;
        }

        EntityTargetSnapshot after = EntityTargetSnapshot.capture(target);
        EntityObservationResult rawResult = EntitySnapshotComparator.compare(glyphId, before, after);

        Set<EntityObservationSignal> rawSignals = rawResult.signals();
        Set<EntityObservationSignal> effectiveSignals = EntityGlyphEffectInterpreter.effectiveSignalsFor(
                glyphId, before, after, rawSignals);

        LOGGER.info("[Ars Odyssey] Observed {} on {}:  raw={},  effective={}",
                glyphId, rawResult.entityTypeId(), rawSignals, effectiveSignals);

        if (effectiveSignals.isEmpty()) {
            return;
        }

        if (!(event.shooter instanceof ServerPlayer player)) {
            return;
        }

        applyDiscovery(player, glyphId, rawResult.entityTypeId(), effectiveSignals, spellParts(event));
    }

    private static void applyDiscovery(
            ServerPlayer player,
            ResourceLocation glyphId,
            ResourceLocation entityTypeId,
            Set<EntityObservationSignal> effectiveSignals,
            List<AbstractSpellPart> spellParts
    ) {
        if (glyphId == null || entityTypeId == null) {
            return;
        }

        TargetDescriptor target = TargetDescriptor.entityType(entityTypeId);
        GlyphRelation relation = new GlyphRelation(
                glyphId,
                RelationType.APPLIES_TO,
                target,
                Optional.empty(),
                "player_cast");

        GlyphDiscoveryResult discovery = GlyphDiscoveryService.resolveRelation(
                relation,
                DiscoverySource.PLAYER_CAST,
                EvidenceConfidence.HIGH,
                new TruthDelta(1, "player_cast_entity_effect"));

        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        boolean added = GlyphDiscoveryService.applyDiscovery(data, discovery);

        if (added) {
            Optional<GlyphComplexityRecord> complexityIncrease =
                    GlyphDiscoveryService.updateComplexity(data, glyphId, spellParts);
            player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
            ModNetwork.syncKnowledge(player, data);

            LOGGER.info("[Ars Odyssey] Discovered by player cast: {} applies to {}, signals={}",
                    glyphId, entityTypeId, effectiveSignals);

            int resolvedCount = new PlayerKnowledgeDataView(data, player.level())
                    .countResolvedRelations(glyphId);
            player.sendSystemMessage(Component.translatable(
                    "message.ars_odyssey.discovery.entity_resolved",
                    glyphDisplayName(glyphId),
                    entityDisplayName(entityTypeId),
                    resolvedCount));
            complexityIncrease.ifPresent(record -> sendComplexityIncrease(player, record));
        } else {
            LOGGER.debug("[Ars Odyssey] Already known: {} applies to {}", glyphId, entityTypeId);
        }
    }

    private static List<AbstractSpellPart> spellParts(EffectResolveEvent event) {
        if (event == null || event.spell == null) {
            return List.of();
        }
        return event.spell.unsafeList();
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
        return Component.literal(glyphId.toString());
    }

    private static Component entityDisplayName(ResourceLocation entityTypeId) {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(entityTypeId)) {
            return entityType.getDescription();
        }
        return Component.literal(entityTypeId.toString());
    }

    private static boolean canObserve(EffectResolveEvent event) {
        return event != null
                && event.world != null
                && !event.world.isClientSide
                && event.shooter != null
                && event.resolveEffect != null
                && event.resolveEffect.getRegistryName() != null
                && event.rayTraceResult instanceof EntityHitResult;
    }

    private record ObservationKey(
            UUID casterUuid,
            ResourceLocation glyphId,
            UUID targetUuid,
            long gameTime,
            int resolverIdentity
    ) {
        private static ObservationKey of(
                UUID casterUuid,
                ResourceLocation glyphId,
                UUID targetUuid,
                long gameTime,
                Object resolver
        ) {
            return new ObservationKey(
                    casterUuid,
                    glyphId,
                    targetUuid,
                    gameTime,
                    System.identityHashCode(resolver));
        }
    }
}
