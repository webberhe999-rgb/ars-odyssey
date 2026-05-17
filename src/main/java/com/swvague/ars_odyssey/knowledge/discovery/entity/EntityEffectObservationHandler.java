package com.swvague.ars_odyssey.knowledge.discovery.entity;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import com.swvague.ars_odyssey.knowledge.DiscoverySource;
import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.RelationType;
import com.swvague.ars_odyssey.knowledge.TargetDescriptor;
import com.swvague.ars_odyssey.knowledge.TruthDelta;
import com.swvague.ars_odyssey.knowledge.discovery.DiscoveryFeedbackService;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDiscoveryResult;
import com.swvague.ars_odyssey.knowledge.discovery.GlyphDiscoveryService;
import com.hollingsworth.arsnouveau.api.event.EffectResolveEvent;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
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

        LOGGER.debug("[Ars Odyssey] Observed {} on {}:  raw={},  effective={}",
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

        DiscoveryFeedbackService.DiscoveryApplyResult applyResult =
                DiscoveryFeedbackService.applyAndNotify(player, discovery, spellParts);
        if (applyResult.added()) {
            LOGGER.info("[Ars Odyssey] Discovered by player cast: {} applies to {}, signals={}",
                    glyphId, entityTypeId, effectiveSignals);
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
