package com.swvague.ars_odyssey.mixin;

import com.swvague.ars_odyssey.config.OdysseyConfig;
import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectile;
import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectileRegistry;
import com.swvague.ars_odyssey.knowledge.truth.TruthificationInsightRules;
import com.swvague.ars_odyssey.network.ClearTruthifiedProjectilesPacket;
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(EntityOrbitProjectile.class)
public abstract class EntityOrbitProjectileMixin implements TruthifiedOrbitProjectile {
    @Unique
    private boolean ars_odyssey$truthifiedOrbit;
    @Unique
    private double ars_odyssey$totalTruthEntanglement;
    @Unique
    private double ars_odyssey$consumeChance = 1.0D;
    @Unique
    private UUID ars_odyssey$spellGroupId;
    @Unique
    private int ars_odyssey$lifetimeTicks = 0;

    @Override
    public void ars_odyssey$setTruthifiedOrbit(double totalTruthEntanglement, double consumeChance, UUID spellGroupId) {
        ars_odyssey$truthifiedOrbit = true;
        ars_odyssey$totalTruthEntanglement = Math.max(0.0D, totalTruthEntanglement);
        ars_odyssey$consumeChance = Math.max(0.0D, Math.min(1.0D, consumeChance));
        ars_odyssey$spellGroupId = spellGroupId;
    }

    @Override
    public boolean ars_odyssey$isTruthifiedOrbit() {
        return ars_odyssey$truthifiedOrbit;
    }

    @Override
    public double ars_odyssey$getConsumeChance() {
        return ars_odyssey$consumeChance;
    }

    @Override
    public UUID ars_odyssey$getSpellGroupId() {
        return ars_odyssey$spellGroupId;
    }

    @Override
    public boolean ars_odyssey$shouldConsumeOnHit() {
        EntityOrbitProjectile self = (EntityOrbitProjectile) (Object) this;
        return self.getRandom().nextDouble() < ars_odyssey$consumeChance;
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void ars_odyssey$drainTruthifiedOrbitMana(CallbackInfo ci) {
        if (!ars_odyssey$truthifiedOrbit) {
            return;
        }

        EntityOrbitProjectile self = (EntityOrbitProjectile) (Object) this;
        if (self.level().isClientSide) {
            return;
        }

        long gameTime = self.level().getGameTime();
        Entity owner = self.getOwner();
        if (!(owner instanceof LivingEntity livingOwner)) {
            return;
        }

        UUID ownerUuid = livingOwner.getUUID();
        if (!TruthifiedOrbitProjectileRegistry.markDrainIfDue(ownerUuid, gameTime, 20L)) {
            return;
        }

        IManaCap mana = CapabilityRegistry.getMana(livingOwner);
        if (mana == null) {
            return;
        }

        double currentMana = mana.getCurrentMana();
        boolean completeInsight = ars_odyssey$totalTruthEntanglement > TruthificationInsightRules.orbitSelfThirdCondition() * 15.0D;
        double drain = OdysseyConfig.ORBIT_MIN_MANA_DRAIN.get();
        if (completeInsight) {
            int percentStacks = Math.max(1, TruthifiedOrbitProjectileRegistry.activePercentStacks(ownerUuid));
            double stackedPercent = ars_odyssey$stackedDrainPercent(OdysseyConfig.ORBIT_MANA_DRAIN_PERCENT.get(), percentStacks);
            drain = Math.max(drain, currentMana * stackedPercent);
        }

        // Scale the per-second drain by the fraction of truthified orbit
        // projectiles still alive. As the initial projectiles (and any split
        // children) are consumed, the upkeep cost drops proportionally.
        int aliveCount = ars_odyssey$countAliveTruthifiedOrbits(self, livingOwner);
        if (aliveCount <= 0) {
            return;
        }
        int initialCount = TruthifiedOrbitProjectileRegistry.initialProjectileCount(ownerUuid);
        if (initialCount > 0 && aliveCount < initialCount) {
            drain *= (double) aliveCount / (double) initialCount;
        }

        if (currentMana < drain) {
            if (livingOwner instanceof ServerPlayer serverPlayer) {
                ClearTruthifiedProjectilesPacket.clearTruthifiedProjectilesFor(serverPlayer);
            } else {
                self.discard();
            }
            return;
        }
        mana.removeMana(drain);
    }

    @Unique
    private static int ars_odyssey$countAliveTruthifiedOrbits(EntityOrbitProjectile self, LivingEntity owner) {
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        UUID ownerUuid = owner.getUUID();
        List<EntityOrbitProjectile> alive = serverLevel.getEntitiesOfClass(
                EntityOrbitProjectile.class,
                owner.getBoundingBox().inflate(256.0D),
                p -> {
                    Entity o = p.getOwner();
                    return o != null
                            && o.getUUID().equals(ownerUuid)
                            && !p.isRemoved()
                            && p instanceof TruthifiedOrbitProjectile t
                            && t.ars_odyssey$isTruthifiedOrbit();
                });
        return alive.size();
    }

    @Unique
    private static double ars_odyssey$stackedDrainPercent(double basePercent, int stacks) {
        double safeBase = Math.max(0.0D, Math.min(1.0D, basePercent));
        int safeStacks = Math.max(1, stacks);
        return 1.0D - Math.pow(1.0D - safeBase, safeStacks);
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void ars_odyssey$checkOrbitLifetime(CallbackInfo ci) {
        EntityOrbitProjectile self = (EntityOrbitProjectile) (Object) this;
        if (self.level().isClientSide || self.isRemoved()) {
            return;
        }

        boolean shouldTickLifetime;
        if (!ars_odyssey$truthifiedOrbit) {
            // Non-truthified projectile: timer always counts
            shouldTickLifetime = true;
        } else {
            // Truthified projectile lasts until hit or mana cancellation clears it.
            shouldTickLifetime = false;
        }

        if (shouldTickLifetime) {
            ars_odyssey$lifetimeTicks++;
            if (ars_odyssey$lifetimeTicks >= OdysseyConfig.ORBIT_MAX_DURATION_TICKS.get()) {
                Entity owner = self.getOwner();
                if (owner != null) {
                    TruthifiedOrbitProjectileRegistry.remove(owner.getUUID(), ars_odyssey$spellGroupId);
                }
                self.discard();
            }
        }
    }
}
