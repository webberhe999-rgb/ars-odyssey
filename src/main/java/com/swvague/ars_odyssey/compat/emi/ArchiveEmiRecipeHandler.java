package com.swvague.ars_odyssey.compat.emi;

import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;
import com.swvague.ars_odyssey.network.ModNetwork;

import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class ArchiveEmiRecipeHandler implements EmiRecipeHandler<ArchiveTerminalMenu> {
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<ArchiveTerminalMenu> screen) {
        return EmiPlayerInventory.of(Minecraft.getInstance().player);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe != null
                && recipe.getId() != null
                && VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory());
    }

    @Override
    public boolean alwaysDisplaySupport(EmiRecipe recipe) {
        return supportsRecipe(recipe);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ArchiveTerminalMenu> context) {
        return supportsRecipe(recipe);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<ArchiveTerminalMenu> context) {
        if (!supportsRecipe(recipe)) {
            return false;
        }
        ModNetwork.fillArchiveCraftingGrid(recipe.getId(), context.getAmount() > 1);
        return true;
    }
}
