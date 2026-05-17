package com.swvague.ars_odyssey.knowledge.discovery;

import com.swvague.ars_odyssey.knowledge.DiscoverySource;
import com.swvague.ars_odyssey.knowledge.GlyphRelation;
import com.swvague.ars_odyssey.knowledge.TruthDelta;

public record GlyphDiscoveryResult(
        GlyphRelation relation,
        TruthDelta truthDelta,
        DiscoverySource source
) {
    public GlyphDiscoveryResult {
        source = source == null ? DiscoverySource.STATIC_SOURCE_SCAN : source;
    }
}
