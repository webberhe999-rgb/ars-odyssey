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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;
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
    private static final int FANGS_CARRIER_WINDOW_TICKS = 40;
    private static final int FANGS_DAMAGE_WINDOW_TICKS = 80;
    private static final double MAX_PRODUCE_DISTANCE_SQ = 8.0D * 8.0D;
    private static final double MAX_CARRIER_DISTANCE_SQ = 16.0D * 16.0D;

    private static final ResourceLocation GLYPH_SUMMON_WOLVES = ResourceLocation.parse("ars_nouveau:glyph_summon_wolves");
    private static final ResourceLocation GLYPH_SUMMON_UNDEAD = ResourceLocation.parse("ars_nouveau:glyph_summon_undead");
    private static final ResourceLocation GLYPH_SUMMON_STEED = ResourceLocation.parse("ars_nouveau:glyph_summon_steed");
    private static final ResourceLocation GLYPH_SUMMON_VEX = ResourceLocation.parse("ars_nouveau:glyph_summon_vex");
    private static final ResourceLocation GLYPH_FANGS = ResourceLocation.parse("ars_nouveau:glyph_fangs");

    private static final ResourceLocation ARS_SUMMON_WOLF = ResourceLocation.parse("ars_nouveau:summon_wolf");
    private static final ResourceLocation ARS_SUMMON_SKELETON = ResourceLocation.parse("ars_nouveau:summon_skeleton");
    private static final ResourceLocation ARS_SUMMON_HORSE = ResourceLocation.parse("ars_nouveau:summon_horse");
    private static final ResourceLocation ARS_ALLY_VEX = ResourceLocation.parse("ars_nouveau:ally_vex");
    private static final ResourceLocation VANILLA_EVOKER_FANGS = ResourceLocation.parse("minecraft:evoker_fangs");

    private static final Map<ResourceLocation, SummonProductionSpec> TRUE_SUMMON_GLYPHS = Map.of(
            GLYPH_SUMMON_WOLVES, new SummonProductionSpec(Set.of(ARS_SUMMON_WOLF), ResourceLocation.parse("minecraft:wolf")),
            GLYPH_SUMMON_UNDEAD, new SummonProductionSpec(Set.of(ARS_SUMMON_SKELETON), ResourceLocation.parse("minecraft:skeleton")),
            GLYPH_SUMMON_STEED, new SummonProductionSpec(Set.of(ARS_SUMMON_HORSE), ResourceLocation.parse("minecraft:horse")),
            GLYPH_SUMMON_VEX, new SummonProductionSpec(Set.of(ARS_ALLY_VEX), ResourceLocation.parse("minecraft:vex"))
    );

    private final CopyOnWriteArrayList<PendingSummonObservation> pendingSummons = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<PendingFangsCarrierObservation> pendingFangsCarriers = new CopyOnWriteArrayList<>();
    private final Map<UUID, PendingFangsDamageAttribution> pendingFangsDamage = new ConcurrentHashMap<>();

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
            LOGGER.info("[Ars Odyssey] Pending summon registered: glyph={} caster={} pos={} expires={}",
                    glyphId, event.shooter.getUUID(), castPosition, startGameTime + PRODUCE_WINDOW_TICKS);
            return;
        }

        if (GLYPH_FANGS.equals(glyphId)) {
            pendingFangsCarriers.add(new PendingFangsCarrierObservation(
                    event.shooter.getUUID(),
                    glyphId,
                    dimension,
                    castPosition,
                    startGameTime,
                    startGameTime + FANGS_CARRIER_WINDOW_TICKS,
                    spellParts(event)));
            LOGGER.info("[Ars Odyssey] Pending fangs carrier registered: glyph={} caster={} pos={} expires={}",
                    glyphId, event.shooter.getUUID(), castPosition, startGameTime + FANGS_CARRIER_WINDOW_TICKS);
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

        if (pendingSummons.isEmpty() && pendingFangsCarriers.isEmpty()) {
            return;
        }

        Entity entity = event.getEntity();
        ResourceLocation joinedType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (joinedType == null) {
            logIgnored("unknown entity type", entity);
            return;
        }

        // Diagnostic: log every entity join when we have pending fangs carriers
        if (!pendingFangsCarriers.isEmpty()) {
            LOGGER.info("[Ars Odyssey] [FANGS JOIN] entity joining while fangs pending: " +
                            "type={} uuid={} tickCount={} isEvokerFangsInstance={} pendingCarriers={}",
                    joinedType, entity.getUUID(), entity.tickCount,
                    entity instanceof EvokerFangs, pendingFangsCarriers.size());
        }

        if (entity.tickCount > 0) {
            logIgnored("already ticked/existing entity", entity);
            return;
        }

        observeFangsCarrierJoin(level, dimension, gameTime, entity, joinedType);
        observeSummonJoin(level, dimension, gameTime, entity, joinedType);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        pendingFangsDamage.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < gameTime);
        if (pendingFangsDamage.isEmpty()) {
            return;
        }

        // Diagnostic: log every damage event while we have pending fangs attributions
        {
            Entity directEntity = event.getSource().getDirectEntity();
            Entity causingEntity = event.getSource().getEntity();
            ResourceLocation targetType = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            LOGGER.info("[Ars Odyssey] [FANGS DAMAGE DIAG] LivingDamageEvent.Post while carriers pending:" +
                            " target=[type={} uuid={}]" +
                            " srcMsg={}" +
                            " direct=[type={} uuid={}]" +
                            " causing=[type={} uuid={}]" +
                            " pendingCarrierUUIDs={}",
                    targetType, event.getEntity().getUUID(),
                    event.getSource().getMsgId(),
                    directEntity != null ? BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType()) : "<null>",
                    directEntity != null ? directEntity.getUUID() : "<null>",
                    causingEntity != null ? BuiltInRegistries.ENTITY_TYPE.getKey(causingEntity.getType()) : "<null>",
                    causingEntity != null ? causingEntity.getUUID() : "<null>",
                    pendingFangsDamage.keySet());
        }

        PendingFangsDamageAttribution attribution = findFangsAttribution(event);
        if (attribution == null) {
            LOGGER.info("[Ars Odyssey] [FANGS DAMAGE MISS] No attribution match — direct or causing entity UUID not in pending carriers");
            return;
        }

        ResourceLocation targetType = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (targetType == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(attribution.casterUuid());
        if (player == null) {
            LOGGER.debug("[Ars Odyssey] Fangs damage ignored: caster offline glyph={} target={}", attribution.glyphId(), targetType);
            return;
        }

        boolean added = applyDiscovery(
                player,
                RelationType.APPLIES_TO,
                attribution.glyphId(),
                TargetDescriptor.entityType(targetType),
                "player_cast_fangs_delayed_damage",
                attribution.spellParts());

        LOGGER.info("[Ars Odyssey] Fangs delayed damage attributed: glyph={} target={} discovery={}",
                attribution.glyphId(), targetType, added ? "written" : "already known");
    }

    private void observeSummonJoin(ServerLevel level, ResourceLocation dimension, long gameTime, Entity entity, ResourceLocation joinedType) {
        if (entity instanceof Projectile || entity instanceof EvokerFangs) {
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

    private void observeFangsCarrierJoin(ServerLevel level, ResourceLocation dimension, long gameTime, Entity entity, ResourceLocation joinedType) {
        boolean isFangsType = VANILLA_EVOKER_FANGS.equals(joinedType);
        boolean isFangsInstance = entity instanceof EvokerFangs;

        // Diagnostic: log the full type check result for every entity when we have pending carriers
        if (!pendingFangsCarriers.isEmpty()) {
            LOGGER.info("[Ars Odyssey] [FANGS CARRIER CHECK] type={} isFangsType={} isFangsInstance={} pendingCount={}",
                    joinedType, isFangsType, isFangsInstance, pendingFangsCarriers.size());
        }

        if (!isFangsType && !isFangsInstance) {
            return;
        }

        for (PendingFangsCarrierObservation pending : pendingFangsCarriers) {
            if (!pending.dimension().equals(dimension)) {
                LOGGER.info("[Ars Odyssey] [FANGS CARRIER SKIP] dimension mismatch: got={} expected={}", dimension, pending.dimension());
                continue;
            }
            if (pending.expireGameTime() < gameTime) {
                LOGGER.info("[Ars Odyssey] [FANGS CARRIER SKIP] expired: expireTime={} gameTime={}", pending.expireGameTime(), gameTime);
                continue;
            }
            double distSq = entity.position().distanceToSqr(pending.castPosition());
            if (distSq > MAX_CARRIER_DISTANCE_SQ) {
                LOGGER.info("[Ars Odyssey] [FANGS CARRIER SKIP] too far: distSq={} max={} castPos={} entityPos={}",
                        distSq, MAX_CARRIER_DISTANCE_SQ, pending.castPosition(), entity.position());
                continue;
            }

            pendingFangsDamage.put(entity.getUUID(), new PendingFangsDamageAttribution(
                    pending.casterUuid(),
                    pending.glyphId(),
                    gameTime + FANGS_DAMAGE_WINDOW_TICKS,
                    pending.spellParts()));
            LOGGER.info("[Ars Odyssey] Fangs carrier registered: glyph={} carrier={} uuid={} expires={}",
                    pending.glyphId(), joinedType, entity.getUUID(), gameTime + FANGS_DAMAGE_WINDOW_TICKS);
            return;
        }

        LOGGER.info("[Ars Odyssey] [FANGS CARRIER MISS] carrier matched type but no pending glyph matched: type={} uuid={}",
                joinedType, entity.getUUID());
    }

    private PendingFangsDamageAttribution findFangsAttribution(LivingDamageEvent.Post event) {
        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != null) {
            PendingFangsDamageAttribution attribution = pendingFangsDamage.get(directEntity.getUUID());
            if (attribution != null) {
                return attribution;
            }
        }

        Entity causingEntity = event.getSource().getEntity();
        if (causingEntity != null) {
            return pendingFangsDamage.get(causingEntity.getUUID());
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

        PlayerKnowledgeData data = player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE);
        boolean added = GlyphDiscoveryService.applyDiscovery(data, discovery);
        if (added) {
            Optional<GlyphComplexityRecord> complexityIncrease =
                    GlyphDiscoveryService.updateComplexity(data, glyphId, spellParts);
            player.setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
            ModNetwork.syncKnowledge(player, data);

            // 发现消息：PRODUCES 用召唤消息，APPLIES_TO 用实体效果消息
            int resolvedCount = new PlayerKnowledgeDataView(data, player.level())
                    .countResolvedRelations(glyphId);
            ResourceLocation targetId = target.id();
            if (relationType == RelationType.PRODUCES) {
                player.sendSystemMessage(Component.translatable(
                        "message.ars_odyssey.discovery.summon_observed",
                        glyphDisplayName(glyphId),
                        targetId != null ? entityDisplayName(targetId) : Component.literal("?"),
                        resolvedCount));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "message.ars_odyssey.discovery.entity_resolved",
                        glyphDisplayName(glyphId),
                        targetId != null ? entityDisplayName(targetId) : Component.literal("?"),
                        resolvedCount));
            }

            complexityIncrease.ifPresent(record -> sendComplexityIncrease(player, record));
        }
        return added;
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

    private static Component glyphDisplayName(ResourceLocation glyphId) {
        AbstractSpellPart part = GlyphRegistry.getSpellpartMap().get(glyphId);
        if (part != null) {
            return Component.literal(part.getLocaleName());
        }
        return Component.literal(glyphId != null ? glyphId.toString() : "?");
    }

    private static Component entityDisplayName(ResourceLocation entityTypeId) {
        if (entityTypeId == null) return Component.literal("?");
        net.minecraft.world.entity.EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(entityTypeId)) {
            return entityType.getDescription();
        }
        return Component.literal(entityTypeId.toString());
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

    private void cleanupExpired(long gameTime) {
        pendingSummons.removeIf(pending -> pending.expireGameTime() < gameTime);
        pendingFangsCarriers.removeIf(pending -> pending.expireGameTime() < gameTime);
        pendingFangsDamage.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < gameTime);
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

    private record SummonProductionSpec(
            Set<ResourceLocation> allowedJoinedEntityTypes,
            ResourceLocation knowledgeEntityType
    ) {
        private SummonProductionSpec {
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

    private record PendingFangsCarrierObservation(
            UUID casterUuid,
            ResourceLocation glyphId,
            ResourceLocation dimension,
            Vec3 castPosition,
            long startGameTime,
            long expireGameTime,
            List<AbstractSpellPart> spellParts
    ) {
    }

    private record PendingFangsDamageAttribution(
            UUID casterUuid,
            ResourceLocation glyphId,
            long expireGameTime,
            List<AbstractSpellPart> spellParts
    ) {
    }
}
