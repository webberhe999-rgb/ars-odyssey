package com.swvague.ars_odyssey.mixin;

import com.swvague.ars_odyssey.config.OdysseyConfig;
import com.swvague.ars_odyssey.glyph.AugmentOrbitSelf;
import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectile;
import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectileRegistry;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeAttachments;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;
import com.swvague.ars_odyssey.knowledge.complexity.TruthEntanglementCalculator;
import com.swvague.ars_odyssey.knowledge.truth.AugmentTruthificationResolver;
import com.swvague.ars_odyssey.knowledge.truth.SpellTruthificationKey;
import com.swvague.ars_odyssey.knowledge.truth.TruthificationInsightRules;
import com.swvague.ars_odyssey.network.ClearTruthifiedProjectilesPacket;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentPierce;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(MethodProjectile.class)
public abstract class MethodProjectileMixin {
    private static final Logger ARS_ODYSSEY$LOG = LogManager.getLogger("Ars Odyssey Orbit");
    private static final int ARS_ODYSSEY$BASE_PROJECTILES_PER_RING = 1;
    private static final int ARS_ODYSSEY$COMPLETE_INSIGHT_PROJECTILES_PER_RING = 2;
    private static final int ARS_ODYSSEY$PRACTICALLY_INFINITE_EXTEND_TIMES = Integer.MAX_VALUE / 600 - 2;

    @Inject(method = "summonProjectiles(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void ars_odyssey$summonOrbitingProjectiles(Level world, LivingEntity shooter, SpellStats stats, SpellResolver resolver, CallbackInfo ci) {
        int ringCount = stats.getBuffCount(AugmentOrbitSelf.INSTANCE);
        if (ringCount <= 0) {
            return;
        }

        PlayerKnowledgeData knowledgeData = shooter instanceof ServerPlayer player
                ? player.getData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE)
                : null;
        List<AbstractSpellPart> spellParts = resolver.spell == null ? List.of() : resolver.spell.unsafeList();
        java.util.Map<Integer, Double> truthifiedOrbitEntanglementByRing = OdysseyConfig.ENABLE_TRUTHIFICATION.get() && knowledgeData != null
                ? ars_odyssey$truthifiedOrbitEntanglementByRing(knowledgeData, spellParts, world)
                : java.util.Map.of();
        boolean truthified = !truthifiedOrbitEntanglementByRing.isEmpty();
        double totalTruthEntanglement = TruthEntanglementCalculator.total(knowledgeData, world);
        UUID spellGroupId = truthified ? UUID.randomUUID() : null;
        if (truthified && shooter instanceof ServerPlayer serverPlayer) {
            boolean completeInsight = TruthificationInsightRules.hasOrbitSelfCompleteInsight(totalTruthEntanglement);
            int maxGroups = completeInsight ? 2 : 1;
            List<UUID> expiredGroups = TruthifiedOrbitProjectileRegistry.register(
                    shooter.getUUID(),
                    spellGroupId,
                    maxGroups,
                    truthifiedOrbitEntanglementByRing.size());
            ARS_ODYSSEY$LOG.debug("[Orbit] register group: completeInsight={} maxGroups={} expiredGroups={} -> previous casts' projectiles for {} expired group(s) are CLEARED",
                    completeInsight, maxGroups, expiredGroups.size(), expiredGroups.size());
            ClearTruthifiedProjectilesPacket.clearTruthifiedProjectilesFor(serverPlayer, expiredGroups);
        }

        boolean completeInsightProjectiles = truthified && TruthificationInsightRules.hasOrbitSelfCompleteInsight(totalTruthEntanglement);
        int baseProjectilesPerRing = completeInsightProjectiles
                ? ARS_ODYSSEY$COMPLETE_INSIGHT_PROJECTILES_PER_RING
                : ARS_ODYSSEY$BASE_PROJECTILES_PER_RING;
        int projectilesPerRing = baseProjectilesPerRing + stats.getBuffCount(AugmentSplit.INSTANCE);
        int totalProjectiles = ringCount * projectilesPerRing;
        int projectileIndex = 0;
        int truthifiedProjectileCount = 0;

        for (int ring = 0; ring < ringCount; ring++) {
            Double modifiedEffectEntanglement = truthifiedOrbitEntanglementByRing.get(ring);
            boolean truthifiedRing = modifiedEffectEntanglement != null;
            double consumeChance = truthifiedRing
                    ? ars_odyssey$consumeChanceForOrbitSelf(totalTruthEntanglement, modifiedEffectEntanglement)
                    : 1.0D;
            for (int slot = 0; slot < projectilesPerRing; slot++) {
                EntityOrbitProjectile projectile = new EntityOrbitProjectile(world, resolver, shooter);
                projectile.setOffset(projectileIndex);
                projectile.setTotal(totalProjectiles);
                projectile.setAccelerates((int) stats.getAccMultiplier());
                projectile.setAoe(ars_odyssey$orbitAoeForRing(stats, ring));
                projectile.pierceLeft = stats.getBuffCount(AugmentPierce.INSTANCE);
                projectile.numSensitive = stats.getBuffCount(AugmentSensitive.INSTANCE);
                projectile.extendTimes = ARS_ODYSSEY$PRACTICALLY_INFINITE_EXTEND_TIMES;
                projectile.setColor(resolver.spellContext.getColors());
                if (truthifiedRing && projectile instanceof TruthifiedOrbitProjectile truthifiedProjectile) {
                    truthifiedProjectile.ars_odyssey$setTruthifiedOrbit(totalTruthEntanglement, consumeChance, spellGroupId);
                    truthifiedProjectileCount++;
                }
                world.addFreshEntity(projectile);
                projectileIndex++;
            }
        }

        if (truthified && spellGroupId != null) {
            TruthifiedOrbitProjectileRegistry.setInitialProjectileCount(spellGroupId, truthifiedProjectileCount);
        }

        ARS_ODYSSEY$LOG.debug("[Orbit] spawn: orbitBuffCount(rings)={} splitBuffCount={} completeInsightProjectiles={} baseProjectilesPerRing={} projectilesPerRing={} totalProjectiles={} truthified={} truthifiedSpawned={} entanglementRingKeys={} totalEntanglement={}",
                ringCount,
                stats.getBuffCount(AugmentSplit.INSTANCE),
                completeInsightProjectiles,
                baseProjectilesPerRing,
                projectilesPerRing,
                totalProjectiles,
                truthified,
                truthifiedProjectileCount,
                truthifiedOrbitEntanglementByRing.keySet(),
                totalTruthEntanglement);

        ci.cancel();
    }

    private static float ars_odyssey$orbitAoeForRing(SpellStats stats, int ring) {
        double radiusBonus = ring * OdysseyConfig.ORBIT_RADIUS_BONUS_PER_RING.get();
        return (float) (stats.getAoeMultiplier() + radiusBonus / 0.5D);
    }

    private static double ars_odyssey$consumeChanceForOrbitSelf(double totalTruthEntanglement, double modifiedEffectEntanglement) {
        if (TruthificationInsightRules.hasOrbitSelfCompleteInsight(totalTruthEntanglement)) {
            return OdysseyConfig.ORBIT_COMPLETE_INSIGHT_CONSUME_CHANCE.get();
        }
        if (modifiedEffectEntanglement >= OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_THREE.get()) {
            return OdysseyConfig.ORBIT_CONSUME_CHANCE_THREE.get();
        }
        if (modifiedEffectEntanglement >= OdysseyConfig.ORBIT_CONSUME_CHANCE_TIER_TWO.get()) {
            return OdysseyConfig.ORBIT_CONSUME_CHANCE_TWO.get();
        }
        if (modifiedEffectEntanglement >= AugmentTruthificationResolver.orbitTierOneThreshold()) {
            return OdysseyConfig.ORBIT_CONSUME_CHANCE_ONE.get();
        }
        return 1.0D;
    }

    private static java.util.Map<Integer, Double> ars_odyssey$truthifiedOrbitEntanglementByRing(
            PlayerKnowledgeData data,
            List<AbstractSpellPart> spellParts,
            Level world
    ) {
        java.util.Map<Integer, Double> occurrences = new java.util.LinkedHashMap<>();
        int orbitOccurrence = 0;
        for (int slot = 0; slot < spellParts.size(); slot++) {
            AbstractSpellPart part = spellParts.get(slot);
            if (part == null || !AugmentOrbitSelf.INSTANCE.getRegistryName().equals(part.getRegistryName())) {
                continue;
            }
            String key = SpellTruthificationKey.forSlot(spellParts, slot, part.getRegistryName());
            if (data.isTruthifiedSpellSlot(key)) {
                double modifiedEffectEntanglement = AugmentTruthificationResolver.highestModifiedEffectEntanglement(
                        data,
                        spellParts,
                        slot,
                        world);
                if (modifiedEffectEntanglement >= OdysseyConfig.ORBIT_SELF_TRUTHIFICATION_REQUIRED_ENTANGLEMENT.get()) {
                    occurrences.put(orbitOccurrence, modifiedEffectEntanglement);
                }
            }
            orbitOccurrence++;
        }
        return java.util.Map.copyOf(occurrences);
    }
}
