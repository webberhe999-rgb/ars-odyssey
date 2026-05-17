package com.swvague.ars_odyssey.compat.jei;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;
import com.swvague.ars_odyssey.registry.ModRegistry;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class ArsOdysseyJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ArsOdyssey.prefix("jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ArchiveCraftingRecipeTransferHandler(), RecipeTypes.CRAFTING);
    }
}
