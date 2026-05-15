package com.example.ars_odyssey.client.tooltip;

import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@OnlyIn(Dist.CLIENT)
public final class GlyphItemTooltipHandler {
    private GlyphItemTooltipHandler() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof Glyph glyphItem)) {
            return;
        }
        // 只对效果类魔符显示，排除 Form / Augment
        if (!(glyphItem.spellPart instanceof AbstractEffect)) {
            return;
        }

        ResourceLocation glyphId = glyphItem.spellPart.getRegistryName();
        GlyphTooltipHelper.appendOdysseyTooltip(glyphId, event.getToolTip());
    }
}
