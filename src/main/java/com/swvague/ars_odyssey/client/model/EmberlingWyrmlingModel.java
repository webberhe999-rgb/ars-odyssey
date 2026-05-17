package com.swvague.ars_odyssey.client.model;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.entity.EmberlingWyrmling;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class EmberlingWyrmlingModel extends GeoModel<EmberlingWyrmling> {
    private static final ResourceLocation MODEL =
            ArsOdyssey.prefix("geo/emberling_wyrmling.geo.json");
    private static final ResourceLocation TEXTURE =
            ArsOdyssey.prefix("textures/entity/emberling_wyrmling/emberling_wyrmling.png");
    private static final ResourceLocation ANIMATIONS =
            ArsOdyssey.prefix("animations/emberling_wyrmling.animation.json");

    @Override
    public ResourceLocation getModelResource(EmberlingWyrmling animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EmberlingWyrmling animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(EmberlingWyrmling animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(EmberlingWyrmling animatable, long instanceId,
                                    AnimationState<EmberlingWyrmling> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        getBone("saddle").ifPresent(saddle -> saddle.setHidden(!animatable.isSaddled()));
        getBone("rider_mount_anchor").ifPresent(anchor -> anchor.setHidden(true));
    }
}
