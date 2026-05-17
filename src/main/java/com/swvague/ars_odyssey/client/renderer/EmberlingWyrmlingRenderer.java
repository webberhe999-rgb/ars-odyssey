package com.swvague.ars_odyssey.client.renderer;

import com.swvague.ars_odyssey.client.model.EmberlingWyrmlingModel;
import com.swvague.ars_odyssey.entity.EmberlingWyrmling;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class EmberlingWyrmlingRenderer extends GeoEntityRenderer<EmberlingWyrmling> {
    public EmberlingWyrmlingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EmberlingWyrmlingModel());
        this.shadowRadius = 0.45F;
    }
}
