package com.swvague.ars_odyssey.knowledge.discovery.entity;

import com.swvague.ars_odyssey.index.EvidenceConfidence;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public record EntityObservationResult(
        ResourceLocation glyphId,
        ResourceLocation entityTypeId,
        Set<EntityObservationSignal> signals,
        EvidenceConfidence confidence,
        String evidenceKey
) {
    public EntityObservationResult {
        signals = Set.copyOf(signals);
        confidence = confidence == null ? EvidenceConfidence.UNKNOWN : confidence;
        evidenceKey = evidenceKey == null ? "" : evidenceKey;
    }

    public static EntityObservationResult none(ResourceLocation glyphId, ResourceLocation entityTypeId) {
        return new EntityObservationResult(
                glyphId,
                entityTypeId,
                Set.of(EntityObservationSignal.NO_OBSERVABLE_CHANGE),
                EvidenceConfidence.UNKNOWN,
                "entity_snapshot_no_observable_change");
    }

    public static EntityObservationResult of(
            ResourceLocation glyphId,
            ResourceLocation entityTypeId,
            Set<EntityObservationSignal> signals,
            EvidenceConfidence confidence,
            String evidenceKey
    ) {
        return new EntityObservationResult(glyphId, entityTypeId, signals, confidence, evidenceKey);
    }
}
