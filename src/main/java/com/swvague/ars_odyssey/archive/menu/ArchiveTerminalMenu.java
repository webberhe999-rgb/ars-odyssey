package com.swvague.ars_odyssey.archive.menu;

import java.util.Optional;
import java.util.Map;
import java.util.List;

import com.swvague.ars_odyssey.archive.ArchiveItemKey;
import com.swvague.ars_odyssey.archive.ArchiveOperationResult;
import com.swvague.ars_odyssey.archive.ArchiveService;
import com.swvague.ars_odyssey.archive.PlayerArchiveAttachments;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.registry.ModRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.ItemStack;

public class ArchiveTerminalMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 14;
    public static final int INVENTORY_LABEL_Y = 170;
    public static final int INVENTORY_GRID_Y = 184;
    public static final int HOTBAR_Y = 248;
    public static final int CRAFT_GRID_X = 78;
    public static final int CRAFT_GRID_Y = 112;
    public static final int CRAFT_RESULT_X = 150;
    public static final int CRAFT_RESULT_Y = 130;
    public static final int CRAFT_SLOT_START = 0;
    public static final int CRAFT_SLOT_COUNT = 9;
    public static final int RESULT_SLOT_INDEX = 9;
    public static final int PLAYER_SLOT_START = 10;
    public static final int PLAYER_SLOT_COUNT = 36;

    private final InteractionHand returnHand;
    private final Player player;
    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();
    private boolean craftingPageVisible;

    public ArchiveTerminalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data == null ? InteractionHand.MAIN_HAND : data.readEnum(InteractionHand.class));
    }

    public ArchiveTerminalMenu(int containerId, Inventory inventory, InteractionHand returnHand) {
        super(ModRegistry.ARCHIVE_TERMINAL_MENU.get(), containerId);
        this.returnHand = returnHand;
        this.player = inventory.player;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        addCraftingGrid(inventory);
        addPlayerInventory(inventory);
        updateCraftingResult();
    }

    public InteractionHand returnHand() {
        return returnHand;
    }

    public void setCraftingPageVisible(boolean craftingPageVisible) {
        this.craftingPageVisible = craftingPageVisible;
    }

    public boolean fillCraftingGridFromRecipe(ServerPlayer player, RecipeHolder<CraftingRecipe> recipeHolder, boolean maxTransfer) {
        if (recipeHolder == null || !stillValid(player)) {
            return false;
        }
        List<Ingredient> ingredients = recipeHolder.value().getIngredients();
        if (ingredients.isEmpty()) {
            return false;
        }

        clearCraftingGridToInventory(player);
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        int perSlot = maxTransfer ? maxCraftingSets(player, data, ingredients) : 1;
        if (perSlot <= 0) {
            return false;
        }

        boolean changed = false;
        for (int slotIndex = 0; slotIndex < Math.min(CRAFT_SLOT_COUNT, ingredients.size()); slotIndex++) {
            Ingredient ingredient = ingredients.get(slotIndex);
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack ingredientStack = takeIngredient(player, data, ingredient, perSlot);
            if (!ingredientStack.isEmpty()) {
                craftSlots.setItem(slotIndex, ingredientStack);
                changed = true;
            }
        }
        if (changed) {
            slotsChanged(craftSlots);
            broadcastChanges();
        }
        return changed;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = index >= 0 && index < slots.size() ? slots.get(index) : null;
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        if (index == RESULT_SLOT_INDEX) {
            ItemStack copy = original.copy();
            if (!moveItemStackTo(original, PLAYER_SLOT_START, PLAYER_SLOT_START + PLAYER_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(original, copy);
            if (original.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, original);
            return copy;
        }
        if (index >= CRAFT_SLOT_START && index < CRAFT_SLOT_START + CRAFT_SLOT_COUNT) {
            ItemStack copy = original.copy();
            if (!moveItemStackTo(original, PLAYER_SLOT_START, PLAYER_SLOT_START + PLAYER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (original.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return copy;
        }
        if (ModRegistry.isOdysseySpellBook(original)) {
            serverPlayer.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.deposit.denied_spellbook"), true);
            return ItemStack.EMPTY;
        }

        ItemStack copy = original.copy();
        ArchiveOperationResult result = ArchiveService.depositStack(serverPlayer, original, original.getCount());
        if (!result.success()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("ars_odyssey.archive_terminal.deposit.failed", result.reason()),
                    true);
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        serverPlayer.displayClientMessage(
                Component.translatable("ars_odyssey.archive_terminal.deposit.success", result.changedCount()),
                true);
        return copy;
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        if (container == craftSlots) {
            updateCraftingResult();
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, craftSlots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return ModRegistry.isOdysseySpellBook(player.getMainHandItem())
                || ModRegistry.isOdysseySpellBook(player.getOffhandItem());
    }

    private void addCraftingGrid(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new CraftingPageSlot(craftSlots, col + row * 3, CRAFT_GRID_X + col * 18, CRAFT_GRID_Y + row * 18));
            }
        }
        addSlot(new CraftingPageResultSlot(inventory.player, craftSlots, resultSlots, 0, CRAFT_RESULT_X, CRAFT_RESULT_Y));
    }

    private class CraftingPageSlot extends Slot {
        private CraftingPageSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return !player.level().isClientSide || craftingPageVisible;
        }
    }

    private class CraftingPageResultSlot extends ResultSlot {
        private CraftingPageResultSlot(Player player, CraftingContainer craftSlots, ResultContainer resultSlots, int slot, int x, int y) {
            super(player, craftSlots, resultSlots, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return !player.level().isClientSide || craftingPageVisible;
        }
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, INVENTORY_X + col * 18, INVENTORY_GRID_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    private void updateCraftingResult() {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CraftingInput input = craftingInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe = player.level().getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level());
        ItemStack result = recipe
                .map(holder -> holder.value().assemble(input, serverPlayer.registryAccess()))
                .filter(stack -> !stack.isEmpty())
                .orElse(ItemStack.EMPTY);
        resultSlots.setItem(0, result);
        setRemoteSlot(RESULT_SLOT_INDEX, result);
        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                containerId,
                incrementStateId(),
                RESULT_SLOT_INDEX,
                result));
    }

    private CraftingInput craftingInput() {
        NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, craftSlots.getItem(i));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private int maxCraftingSets(ServerPlayer player, PlayerArchiveData data, List<Ingredient> ingredients) {
        int max = 64;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            int available = countIngredient(player, data, ingredient);
            if (available <= 0) {
                return 0;
            }
            max = Math.min(max, available);
        }
        return Math.max(1, max);
    }

    private int countIngredient(ServerPlayer player, PlayerArchiveData data, Ingredient ingredient) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                count += stack.getCount();
            }
        }
        for (Map.Entry<ArchiveItemKey, Long> entry : data.ledger().counts().entrySet()) {
            if (ingredient.test(data.ledger().prototype(entry.getKey()))) {
                count += (int) Math.min(Integer.MAX_VALUE - count, entry.getValue());
            }
        }
        return count;
    }

    private ItemStack takeIngredient(ServerPlayer player, PlayerArchiveData data, Ingredient ingredient, int count) {
        ItemStack fromInventory = takeIngredientFromInventory(player, ingredient, count);
        if (!fromInventory.isEmpty()) {
            return fromInventory;
        }
        return data.ledger()
                .findMatching(ingredient)
                .map(key -> ArchiveService.extractStackForInternalUse(player, key, count))
                .orElse(ItemStack.EMPTY);
    }

    private ItemStack takeIngredientFromInventory(ServerPlayer player, Ingredient ingredient, int requested) {
        ItemStack result = ItemStack.EMPTY;
        int remaining = requested;
        for (int slot = 0; slot < player.getInventory().items.size() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }
            if (result.isEmpty()) {
                int moved = Math.min(remaining, stack.getCount());
                result = stack.copyWithCount(moved);
                stack.shrink(moved);
                remaining -= moved;
            } else if (ItemStack.isSameItemSameComponents(result, stack)) {
                int moved = Math.min(remaining, stack.getCount());
                result.grow(moved);
                stack.shrink(moved);
                remaining -= moved;
            }
            if (stack.isEmpty()) {
                player.getInventory().items.set(slot, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
        return result;
    }

    private void clearCraftingGridToInventory(Player player) {
        for (int i = 0; i < CRAFT_SLOT_COUNT; i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                craftSlots.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
