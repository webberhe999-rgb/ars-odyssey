package com.swvague.ars_odyssey.mixin;

import com.swvague.ars_odyssey.glyph.TruthifiedOrbitProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityProjectileSpell.class)
public abstract class EntityProjectileSpellMixin {
    @Inject(method = "attemptRemoval", at = @At("HEAD"), cancellable = true, remap = false)
    private void ars_odyssey$truthifiedOrbitConsumeChance(CallbackInfo ci) {
        EntityProjectileSpell self = (EntityProjectileSpell) (Object) this;
        if (self instanceof TruthifiedOrbitProjectile truthifiedProjectile
                && truthifiedProjectile.ars_odyssey$isTruthifiedOrbit()
                && !truthifiedProjectile.ars_odyssey$shouldConsumeOnHit()) {
            ci.cancel();
        }
    }
}
