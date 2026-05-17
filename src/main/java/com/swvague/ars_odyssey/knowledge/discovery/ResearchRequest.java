package com.swvague.ars_odyssey.knowledge.discovery;

import com.swvague.ars_odyssey.knowledge.DiscoverySource;
import com.swvague.ars_odyssey.knowledge.TargetDescriptor;
import net.minecraft.resources.ResourceLocation;

public record ResearchRequest(
        ResourceLocation glyphId,
        TargetDescriptor target,
        DiscoverySource source,
        int expectedCost,
        int durationTicks,
        String researchKey
) {
    public ResearchRequest {
        source = source == null ? DiscoverySource.AUTOMATED_RESEARCH : source;
        expectedCost = Math.max(0, expectedCost);
        durationTicks = Math.max(0, durationTicks);
        researchKey = researchKey == null ? "" : researchKey;
    }
}
