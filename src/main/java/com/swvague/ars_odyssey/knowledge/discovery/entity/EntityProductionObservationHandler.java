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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EntityProductionObservationHandler {
    public static final EntityProductionObservationHandler INSTANCE = new EntityProductionObservationHandler();

    private static final Logger LOGGER = LogManager.getLogger("Ars Odyssey Entity Production");
    private static final int PRODUCE_WINDOW_TICKS = 40;
    private static final int CARRIER_JOIN_WINDOW_TICKS = 40;
    private static final int CARRIER_EFFECT_WINDOW_TICKS = 80;
    private static final double MAX_PRODUCE_DISTANCE_SQ = 8.0D * 8.0D;
    private static final double MAX_CARRIER_DISTANCE_SQ = 16.0D * 16.0D;

    private static final ResourceLocation GLYPH_SUMMON_WOLVES = ResourceLocation.parse("ars_nouveau:glyph_summon_wolves");
    private static final ResourceLocation GLYPH_SUMMON_UNDEAD = ResourceLocation.parse("ars_nouveau:glyph_summon_undead");
    private static final ResourceLocation GLYPH_SUMMON_STEED = ResourceLocation.parse("ars_nouveau:glyph_summon_steed");
    private static final ResourceLocation GLYPH_SUMMON_VEX = ResourceLocation.parse("ars_nouveau:glyph_summon_vex");
    private static final ResourceLocation GLYPH_FANGS = ResourceLocation.parse("ars_nouveau:glyph_fangs");
    private static final ResourceLocation GLYPH_LIGHTNING = ResourceLocation.parse("ars_nouveau:glyph_lightning");

    private static final ResourceLocation ARS_SUMMON_WOLF = ResourceLocation.parse("ars_nouveau:summon_wolf");
    private static final ResourceLocation ARS_SUMMON_SKELETON = ResourceLocation.parse("ars_nouveau:summon_skeleton");
    private static final ResourceLocation ARS_SUMMON_HORSE = ResourceLocation.parse("ars_nouveau:summon_horse");
    private static final ResourceLocation ARS_ALLY_VEX = ResourceLocation.parse("ars_nouveau:ally_vex");
    private static final ResourceLocation ARS_FANGS = ResourceLocation.parse("ars_nouveau:fangs");
    private static final ResourceLocation ARS_LIGHTNING = ResourceLocation.parse("ars_nouveau:an_lightning");
    private static final ResourceLocation VANILLA_EVOKER_FANGS = ResourceLocation.parse("minecraft:evoker_fangs");

    private static final Map<ResourceLocation, SummonProductionSpec> TRUE_SUMMON_GLYPHS = Map.of(
            GLYPH_SUMMON_WOLVES, new SummonProductionSpec(Set.of(ARS_SUMMON_WOLF), ResourceLocation.parse("minecraft:wolf")),
            GLYPH_SUMMON_UNDEAD, new SummonProductionSpec(Set.of(ARS_SUMMON_SKELETON), ResourceLocation.parse("minecraft:skeleton")),
            GLYPH_SUMMON_STEED, new SummonProductionSpec(Set.of(ARS_SUMMON_HORSE), ResourceLocation.parse("minecraft:horse")),
            GLYPH_SUMMON_VEX, new SummonProductionSpec(Set.of(ARS_ALLY_VEX), ResourceLocation.parse("minecraft:vex"))
    );

    private static final Map<ResourceLocation, CarrierObservationSpec> CARRIER_GLYPHS = Map.of(
            GLYPH_FANGS, new CarrierObservationSpec(
                    CarrierKind.DELAYED_DAMAGE,
                    Set.of(ARS_FANGS, VANILLA_EVOKER_FANGS),
                    "player_cast_fangs_delayed_damage"),
            GLYPH_LIGHTNING, new CarrierObservationSpec(
                    CarrierKind.LIGHTNING_DAMAGE_STATUS,
                    Set.of(ARS_LIGHTNING),
                    "player_cast_lightning_carrier")
    );

    private final CopyOnWriteArrayList<PendingSummonObservation> pendingSummons = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PendingCarrierObservation> pendingCarriers = new CopyOnWriteArrayList<>();
    private final Map<UUID, PendingCarrierAttribution> pendingCarrierAttributions = new ConcurrentHashMap<>();

    private EntityProductionObservationHandler() {
    }

    @SubscribeEvent
    public void onEffectPreResolve(EffectResolveEvent.Pre event) {
        if (!canObserve(event) || !(event.shooter instanceof ServerPlayer)) {
            return;
        }

        ResourceLocation glyphId = event.resolveEffect.getRegistryName();
        Vec3 castPosition = event.rayTraceResult != null ? event.rayTraceResult.getLocation() : event.shooter.position();
        long startGameTime = event.world.getGameTime();
        ResourceLocation dimension = event.world.dimension().location();

        SummonProductionSpec summonSpec = TRUE_SUMMON_GLYPHS.get(glyphId);
        if (summonSpec != null) {
            pendingSummons.add(new PendingSummonObservation(
                    event.shooter.getUUID(),
                    glyphId,
                    dimension,
                    castPosition,
                    startGameTime,
                    startGameTime + PRODUCE_WINDOW_TICKS,
                    summonSpec,
                    spellParts(event)));
            LOGGER.debug("[Ars Odyssey] Pending summon registered: glyph={} caster={} pos={} expires={}",
                    glyphId, event.shooter.getUUID(), castPosition, startGameTime + PRODUCE_WINDOW_TICKS);
            return;
        }

        CarrierObservationSpec carrierSpec = CARRIER_GLYPHS.get(glyphId);
        if (carrierSpec != null) {
            pendingCarriers.add(new PendingCarrierObservation(
                    event.shooter.getUUID(),
                    glyphId,
                    dimension,
                    castPosition,
                    startGameTime,
                    startGameTime + CARRIER_JOIN_WINDOW_TICKS,
                    carrierSpec,
                    spellParts(event)));
            LOGGER.debug("[Ars Odyssey] Pending effect carrier registered: glyph={} kind={} caster={} pos={} expires={}",
                    glyphId, carrierSpec.kind(), event.shooter.getUUID(), castPosition, startGameTime + CARRIER_JOIN_WINDOW_TICKS);
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        ResourceLocation dimension = level.dimension().location();
        cleanupExpired(gameTime);

        if (pendingSummons.isEmpty() && pendingCarriers.isEmpty()) {
            return;
        }

        Entity entity = event.getEntity();
        ResourceLocation joinedType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (joinedType == null) {
            logIgnored("unknown entity type", entity);
            return;
        }

        if (!pendingCarriers.isEmpty()) {
            LOGGER.debug("[Ars Odyssey] [CARRIER JOIN] type={} uuid={} tickCount={} fangs={} lightning={} pending={}",
                    joinedType, entity.getUUID(), entity.tickCount,
                    entity instanceof EvokerFangs, entity instanceof LightningBolt, pendingCarriers.size());
        }

        if (entity.tickCount > 0) {
            logIgnored("already ticked/existing entity", entity);
            return;
        }

        observeCarrierJoin(level, dimension, gameTime, entity, joinedType);
        observeSummonJoin(level, dimension, gameTime, entity, joinedType);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        pendingCarrierAttributions.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < gameTime);
        if (pendingCarrierAttributions.isEmpty()) {
            return;
        }

        PendingCarrierAttribution attribution = findCarrierAttribution(event);
        if (attribution == null) {
            LOGGER.debug("[Ars Odyssey] [CARRIER DAMAGE MISS] No attribution match: direct or causing entity UUID not in pending carriers");
            return;
        }

        ResourceLocation targetType = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (targetType == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(attribution.casterUuid());
        if (player == null) {
            LOGGER.debug("[Ars Odyssey] Carrier damage ignored: caster offline glyph={} target={}", attribution.glyphId(), targetType);
            return;
        }

        boolean added = applyDiscovery(
                player,
                RelationType.APPLIES_TO,
                attribution.glyphId(),
                TargetDescriptor.entityType(targetType),
                attribution.evidenceKey(),
                attribution.spellParts());

        LOGGER.info("[Ars Odyssey] Carrier damage attributed: glyph={} kind={} target={} discovery={}",
                attribution.glyphId(), attribution.kind(), targetType, added ? "written" : "already known");
    }

    @SubscribeEvent
    public void onEntityStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        pendingCarrierAttributions.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < gameTime);

        PendingCarrierAttribution attribution = pendingCarrierAttributions.get(event.getLightning().getUUID());
        if (attribution == null || attribution.kind() != CarrierKind.LIGHTNING_DAMAGE_STATUS) {
            return;
        }

        ResourceLocation targetType = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (targetType == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(attribution.casterUuid());
        if (player == null) {
            LOGGER.debug("[Ars Odyssey] Lightning strike ignored: caster offline glyph={} target={}", attribution.glyphId(), targetType);
            return;
        }

        boolean added = applyDiscovery(
                player,
                RelationType.APPLIES_TO,
                attribution.glyphId(),
                TargetDescriptor.entityType(targetType),
                attribution.evidenceKey(),
                attribution.spellParts());

        LOGGER.info("[Ars Odyssey] Lightning carrier attributed: glyph={} target={} discovery={}",
                attribution.glyphId(), targetType, added ? "written" : "already known");
    }

    private void observeSummonJoin(ServerLevel level, ResourceLocation dimension, long gameTime, Entity entity, ResourceLocation joinedType) {
        if (entity instanceof Projectile || entity instanceof EvokerFangs || entity instanceof LightningBolt) {
            logIgnored("projectile/carrier is not a summon product", entity);
            return;
        }
        if (!(entity instanceof LivingEntity)) {
            logIgnored("non-living helper/visual entity", entity);
            return;
        }

        for (PendingSummonObservation pending : pendingSummons) {
            if (!pending.dimension().equals(dimension)) {
                continue;
            }
            if (pending.expireGameTime() < gameTime) {
                continue;
            }
            if (entity.getUUID().equals(pending.casterUuid())) {
                logIgnored("caster entity", entity);
                continue;
            }
            if (entity.position().distanceToSqr(pending.castPosition()) > MAX_PRODUCE_DISTANCE_SQ) {
                logIgnored("too far from summon cast", entity);
                continue;
            }
            if (!pending.spec().allowedJoinedEntityTypes().contains(joinedType)) {
                logIgnored("not allowed summon entity type", entity);
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(pending.casterUuid());
            if (player == null) {
                LOGGER.debug("[Ars Odyssey] Summon entity ignored: caster offline glyph={} joined={}", pending.glyphId(), joinedType);
                return;
            }

            TargetDescriptor producedTarget = TargetDescriptor.entityType(pending.spec().knowledgeEntityType());
            boolean added = applyDiscovery(player, RelationType.PRODUCES, pending.glyphId(), producedTarget,
                    "player_cast_summon", pending.spellParts());
            LOGGER.info("[Ars Odyssey] Summon entity confirmed: glyph={} joined={} produces={} discovery={}",
                    pending.glyphId(), joinedType, pending.spec().knowledgeEntityType(), added ? "written" : "already known");
            return;
        }

        logIgnored("no matching pending summon", entity);
    }

    private void observeCarrierJoin(ServerLevel level, ResourceLocation dimension, long gameTime, Entity entity, ResourceLocation joinedType) {
        for (PendingCarrierObservation pending : pendingCarriers) {
            if (!pending.dimension().equals(dimension)) {
                continue;
            }
            if (pending.expireGameTime() < gameTime) {
                continue;
            }
            double distSq = entity.position().distanceToSqr(pending.castPosition());
            if (distSq > MAX_CARRIER_DISTANCE_SQ) {
                LOGGER.debug("[Ars Odyssey] [CARRIER SKIP] too far: glyph={} kind={} distSq={} max={} castPos={} entityPos={}",
                        pending.glyphId(), pending.spec().kind(), distSq, MAX_CARRIER_DISTANCE_SQ, pending.castPosition(), entity.position());
                continue;
            }
            if (!isAllowedCarrierEntity(pending.spec(), entity, joinedType)) {
                continue;
            }

            pendingCarrierAttributions.put(entity.getUUID(), new PendingCarrierAttribution(
                    pending.casterUuid(),
                    pending.glyphId(),
                    pending.spec().kind(),
                    pending.spec().evidenceKey(),
                    gameTime + CARRIER_EFFECT_WINDOW_TICKS,
                    pending.spellParts()));
            LOGGER.info("[Ars Odyssey] Effect carrier registered: glyph={} kind={} carrier={} uuid={} expires={}",
                    pending.glyphId(), pending.spec().kind(), joinedType, entity.getUUID(), gameTime + CARRIER_EFFECT_WINDOW_TICKS);
            return;
        }

        LOGGER.debug("[Ars Odyssey] [CARRIER MISS] no pending glyph matched: type={} uuid={}", joinedType, entity.getUUID());
    }

    private static boolean isAllowedCarrierEntity(CarrierObservationSpec spec, Entity entity, ResourceLocation joinedType) {
        if (spec.allowedJoinedEntityTypes().contains(joinedType)) {
            return true;
        }
        return switch (spec.kind()) {
            case DELAYED_DAMAGE -> entity instanceof EvokerFangs;
            case LIGHTNING_DAMAGE_STATUS -> entity instanceof LightningBolt;
        };
    }

    private PendingCarrierAttribution findCarrierAttribution(LivingDamageEvent.Post event) {
        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != null) {
            PendingCarrierAttribution attribution = pendingCarrierAttributions.get(directEntity.getUUID());
            if (attribution != null) {
                return attribution;
            }
        }

        Entity causingEntity = event.getSource().getEntity();
        if (causingEntity != null) {
            return pendingCarrierAttributions.get(causingEntity.getUUID());
        }

        return null;
    }

    private boolean applyDiscovery(
            ServerPlayer player,
            RelationType relationType,
            ResourceLocation glyphId,
            TargetDescriptor target,
            String evidenceKey,
            List<AbstractSpellPart> spellParts
    ) {
        GlyphRelation relation = new GlyphRelation(glyphId, relationType, target, Optional.empty(), evidenceKey);
        GlyphDiscoveryResult discovery = GlyphDiscoveryService.resolveRelation(
                relation,
                DiscoverySource.PLAYER_CAST,
                EvidenceConfidence.HIGH,
                new TruthDelta(1, evidenceKey));

        return DiscoveryFeedbackService.applyAndNotify(player, discovery, spellParts).added();
    }

    private static List<AbstractSpellPart> spellParts(EffectResolveEvent event) {
        if (event == null || event.spell == null) {
            return List.of();
        }
        return event.spell.unsafeList();
    }

    private void cleanupExpired(long gameTime) {
        pendingSummons.removeIf(pending -> pending.expireGameTime() < gameTime);
        pendingCarriers.removeIf(pending -> pending.expireGameTime() < gameTime);
        pendingCarrierAttributions.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < gameTime);
    }

    private static boolean canObserve(EffectResolveEvent event) {
        return event != null
                && event.world != null
                && !event.world.isClientSide
                && event.shooter != null
                && event.resolveEffect != null
                && event.resolveEffect.getRegistryName() != null;
    }

    private static void logIgnored(String reason, Entity entity) {
        LOGGER.debug("[Ars Odyssey] Entity join ignored: reason={} entity={} uuid={}",
                reason,
                entity == null ? "<null>" : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                entity == null ? "<null>" : entity.getUUID());
    }

    private enum CarrierKind {
        DELAYED_DAMAGE,
        LIGHTNING_DAMAGE_STATUS
    }

    private record SummonProductionSpec(
            Set<ResourceLocation> allowedJoinedEntityTypes,
            ResourceLocation knowledgeEntityType
    ) {
        private SummonProductionSpec {
            allowedJoinedEntityTypes = Set.copyOf(allowedJoinedEntityTypes);
        }
    }

    private record CarrierObservationSpec(
            CarrierKind kind,
            Set<ResourceLocation> allowedJoinedEntityTypes,
            String evidenceKey
    ) {
        private CarrierObservationSpec {
            allowedJoinedEntityTypes = Set.copyOf(allowedJoinedEntityTypes);
        }
    }

    private record PendingSummonObservation(
            UUID casterUuid,
            ResourceLocation glyphId,
            ResourceLocation dimension,
            Vec3 castPosition,
            long startGameTime,
            long expireGameTime,
            SummonProductionSpec spec,
            List<AbstractSpellPart> spellParts
    ) {
    }

    private record PendingCarrierObservation(
            UUID casterUuid,
            ResourceLocation glyphId,
            ResourceLocation dimension,
            Vec3 castPosition,
            long startGameTime,
            long expireGameTime,
            CarrierObservationSpec spec,
            List<AbstractSpellPart> spellParts
    ) {
    }

    private record PendingCarrierAttribution(
            UUID casterUuid,
            ResourceLocation glyphId,
            CarrierKind kind,
            String evidenceKey,
            long expireGameTime,
            List<AbstractSpellPart> spellParts
    ) {
    }
}
