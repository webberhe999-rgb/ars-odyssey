package com.swvague.ars_odyssey.client.renderer;

import com.swvague.ars_odyssey.registry.ModRegistry;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class OdysseyEntityRenderers {
    private OdysseyEntityRenderers() {
    }

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.EMBERLING_WYRMLING.get(), EmberlingWyrmlingRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.RESONANCE_NEXUS_CORE_BLOCK_ENTITY.get(), ResonanceNexusCoreRenderer::new);
    }
}
