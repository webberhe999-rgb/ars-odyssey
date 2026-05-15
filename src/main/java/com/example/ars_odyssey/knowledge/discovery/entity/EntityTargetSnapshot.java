package com.example.ars_odyssey.knowledge.discovery.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record EntityTargetSnapshot(
        UUID entityUuid,
        ResourceLocation entityTypeId,
        boolean alive,
        float health,
        int remainingFireTicks,
        int ticksFrozen,
        Vec3 position,
        Vec3 deltaMovement,
        Set<ResourceLocation> activeMobEffects,
        boolean undead
) {
    public EntityTargetSnapshot {
        activeMobEffects = Set.copyOf(activeMobEffects);
    }

    public static EntityTargetSnapshot capture(Entity entity) {
        Set<ResourceLocation> activeMobEffects = Set.of();
        float health = -1.0F;
        boolean undead = false;

        if (entity instanceof LivingEntity living) {
            health = living.getHealth();
            undead = living.isInvertedHealAndHarm();
            activeMobEffects = living.getActiveEffects().stream()
                    .map(MobEffectInstance::getEffect)
                    .map(effect -> BuiltInRegistries.MOB_EFFECT.getKey(effect.value()))
                    .collect(Collectors.toUnmodifiableSet());
        }

        return new EntityTargetSnapshot(
                entity.getUUID(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                entity.isAlive(),
                health,
                entity.getRemainingFireTicks(),
                entity.getTicksFrozen(),
                entity.position(),
                entity.getDeltaMovement(),
                activeMobEffects,
                undead);
    }
}
