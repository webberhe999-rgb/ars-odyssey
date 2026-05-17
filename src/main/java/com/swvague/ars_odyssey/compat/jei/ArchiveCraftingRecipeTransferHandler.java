package com.swvague.ars_odyssey.compat.jei;

import java.util.Optional;

import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;
import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.registry.ModRegistry;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class ArchiveCraftingRecipeTransferHandler implements IRecipeTransferHandler<ArchiveTerminalMenu, RecipeHolder<CraftingRecipe>> {
    @Override
    public Class<? extends ArchiveTerminalMenu> getContainerClass() {
        return ArchiveTerminalMenu.class;
    }

    @Override
    public Optional<MenuType<ArchiveTerminalMenu>> getMenuType() {
        return Optional.of(ModRegistry.ARCHIVE_TERMINAL_MENU.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(
            ArchiveTerminalMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer) {
        if (doTransfer && recipe != null) {
            ModNetwork.fillArchiveCraftingGrid(recipe.id(), maxTransfer);
        }
        return null;
    }
}
