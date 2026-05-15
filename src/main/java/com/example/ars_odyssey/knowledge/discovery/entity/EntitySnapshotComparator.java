package com.example.ars_odyssey.knowledge.discovery.entity;

import com.example.ars_odyssey.index.EvidenceConfidence;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.Set;

public final class EntitySnapshotComparator {
    private EntitySnapshotComparator() {
    }

    public static EntityObservationResult compare(
            ResourceLocation glyphId,
            EntityTargetSnapshot before,
            EntityTargetSnapshot after
    ) {
        if (before == null || after == null || !before.entityUuid().equals(after.entityUuid())) {
            ResourceLocation entityTypeId = before != null ? before.entityTypeId() : null;
            return EntityObservationResult.none(glyphId, entityTypeId);
        }

        EnumSet<EntityObservationSignal> signals = EnumSet.noneOf(EntityObservationSignal.class);

        if (before.alive() && !after.alive()) {
            signals.add(EntityObservationSignal.ENTITY_DIED);
        }
        if (before.health() >= 0.0F && after.health() >= 0.0F) {
            if (after.health() < before.health()) {
                signals.add(EntityObservationSignal.HEALTH_DECREASED);
            } else if (after.health() > before.health()) {
                signals.add(EntityObservationSignal.HEALTH_INCREASED);
            }
        }
        if (after.remainingFireTicks() > before.remainingFireTicks()) {
            signals.add(EntityObservationSignal.FIRE_TICKS_INCREASED);
        }
        if (after.ticksFrozen() > before.ticksFrozen()) {
            signals.add(EntityObservationSignal.FROZEN_TICKS_INCREASED);
        }
        if (!before.activeMobEffects().containsAll(after.activeMobEffects())) {
            signals.add(EntityObservationSignal.MOB_EFFECT_ADDED);
        }
        if (!after.activeMobEffects().containsAll(before.activeMobEffects())) {
            signals.add(EntityObservationSignal.MOB_EFFECT_REMOVED);
        }
        if (before.position().distanceTo(after.position()) > 0.25D) {
            signals.add(EntityObservationSignal.POSITION_CHANGED);
        }
        if (before.deltaMovement().distanceTo(after.deltaMovement()) > 0.05D) {
            signals.add(EntityObservationSignal.VELOCITY_CHANGED);
        }

        if (signals.isEmpty()) {
            signals.add(EntityObservationSignal.NO_OBSERVABLE_CHANGE);
            return EntityObservationResult.of(
                    glyphId,
                    after.entityTypeId(),
                    signals,
                    EvidenceConfidence.UNKNOWN,
                    "entity_snapshot_no_observable_change");
        }

        return EntityObservationResult.of(
                glyphId,
                after.entityTypeId(),
                Set.copyOf(signals),
                EvidenceConfidence.MEDIUM,
                "entity_snapshot_changed");
    }
}
