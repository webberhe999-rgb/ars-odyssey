package com.swvague.ars_odyssey.archive;

import com.swvague.ars_odyssey.archive.runtime.ArchiveDirtyReason;
import com.swvague.ars_odyssey.archive.cost.ArchivePaymentResult;
import com.swvague.ars_odyssey.archive.cost.ArchivePaymentService;
import com.swvague.ars_odyssey.archive.provision.ArchiveProvisionRule;
import com.swvague.ars_odyssey.archive.resonance.ResonanceRiskLevel;
import com.swvague.ars_odyssey.archive.resonance.ResonanceService;
import com.swvague.ars_odyssey.network.ModNetwork;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ArchiveService {
    private ArchiveService() {
    }

    public static ArchiveOperationResult depositHeld(ServerPlayer player, int requestedCount) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return ArchiveOperationResult.failure("empty_hand", 0L, null);
        }

        return depositStack(player, held, requestedCount);
    }

    public static ArchiveOperationResult depositCarried(ServerPlayer player, int requestedCount) {
        ItemStack carried = player.containerMenu.getCarried();
        ArchiveOperationResult result = depositStack(player, carried, requestedCount);
        if (result.success()) {
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            player.containerMenu.broadcastChanges();
        }
        return result;
    }

    public static ArchiveOperationResult depositStack(ServerPlayer player, ItemStack source, int requestedCount) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        if (source == null || source.isEmpty()) {
            return ArchiveOperationResult.failure("empty_stack", 0L, null);
        }

        int count = Math.max(1, Math.min(requestedCount, source.getCount()));
        ArchiveOperationCost cost = ArchiveCostPolicy.estimate(source, count, ArchiveOperationType.DEPOSIT);
        ArchiveAccessPolicy.ArchiveAccessCheck access = ArchiveAccessPolicy.check(data, cost);
        ArchiveItemKey key = ArchiveItemKey.from(source).orElse(null);
        long stored = key == null ? 0L : data.ledger().count(key);
        if (!access.allowed()) {
            return ArchiveOperationResult.failure(access.reason(), stored, cost);
        }
        ArchivePaymentResult payment = ArchivePaymentService.pay(player, data, cost);
        if (!payment.success()) {
            return ArchiveOperationResult.failure(payment.reason(), stored, cost, payment);
        }
        ResonanceRiskLevel riskLevel = ResonanceService.applyPayment(player, data, payment);
        if (key == null || !data.ledger().add(source, count)) {
            return ArchiveOperationResult.failure("unsupported_item", stored, cost);
        }

        source.shrink(count);
        data.runtimeState().markDirty(ArchiveDirtyReason.LEDGER_CHANGED);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        return ArchiveOperationResult.success(count, data.ledger().count(key), cost, payment, riskLevel);
    }

    public static ArchiveOperationResult extractToCursor(ServerPlayer player, ArchiveItemKey key, int requestedCount) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        if (key == null) {
            return ArchiveOperationResult.failure("unknown_item", 0L, null);
        }

        ItemStack prototype = data.ledger().prototype(key);
        if (prototype.isEmpty()) {
            return ArchiveOperationResult.failure("unsupported_item", 0L, null);
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, prototype)) {
            return ArchiveOperationResult.failure("cursor_incompatible", data.ledger().count(key), null);
        }

        int maxStackSize = Math.max(1, prototype.getMaxStackSize());
        int room = carried.isEmpty() ? maxStackSize : Math.max(0, maxStackSize - carried.getCount());
        if (room <= 0) {
            return ArchiveOperationResult.failure("cursor_full", data.ledger().count(key), null);
        }

        long stored = data.ledger().count(key);
        int count = (int) Math.max(1L, Math.min(Math.min(requestedCount, room), stored));
        ArchiveOperationCost cost = ArchiveCostPolicy.estimate(prototype, count, ArchiveOperationType.EXTRACT);
        ArchiveAccessPolicy.ArchiveAccessCheck access = ArchiveAccessPolicy.check(data, cost);
        if (!access.allowed()) {
            return ArchiveOperationResult.failure(access.reason(), stored, cost);
        }
        if (stored < count) {
            return ArchiveOperationResult.failure("insufficient_items", stored, cost);
        }

        ArchivePaymentResult payment = ArchivePaymentService.pay(player, data, cost);
        if (!payment.success()) {
            return ArchiveOperationResult.failure(payment.reason(), stored, cost, payment);
        }
        ResonanceRiskLevel riskLevel = ResonanceService.applyPayment(player, data, payment);
        if (!data.ledger().remove(key, count)) {
            return ArchiveOperationResult.failure("ledger_rejected", stored, cost);
        }

        if (carried.isEmpty()) {
            player.containerMenu.setCarried(prototype.copyWithCount(count));
        } else {
            carried.grow(count);
            player.containerMenu.setCarried(carried);
        }
        player.containerMenu.broadcastChanges();
        data.runtimeState().markDirty(ArchiveDirtyReason.LEDGER_CHANGED);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        return ArchiveOperationResult.success(count, data.ledger().count(key), cost, payment, riskLevel);
    }

    public static ItemStack extractStackForInternalUse(ServerPlayer player, ArchiveItemKey key, int requestedCount) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        if (key == null || requestedCount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack prototype = data.ledger().prototype(key);
        if (prototype.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int count = (int) Math.max(1L, Math.min(requestedCount, data.ledger().count(key)));
        ArchiveOperationCost cost = ArchiveCostPolicy.estimate(prototype, count, ArchiveOperationType.EXTRACT);
        ArchiveAccessPolicy.ArchiveAccessCheck access = ArchiveAccessPolicy.check(data, cost);
        if (!access.allowed()) {
            return ItemStack.EMPTY;
        }
        ArchivePaymentResult payment = ArchivePaymentService.pay(player, data, cost);
        if (!payment.success()) {
            return ItemStack.EMPTY;
        }
        ResonanceService.applyPayment(player, data, payment);
        if (!data.ledger().remove(key, count)) {
            return ItemStack.EMPTY;
        }

        data.runtimeState().markDirty(ArchiveDirtyReason.LEDGER_CHANGED);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        return prototype.copyWithCount(count);
    }

    public static ArchiveOperationResult extract(ServerPlayer player, ResourceLocation itemId, int requestedCount) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (!BuiltInRegistries.ITEM.getKey(item).equals(itemId)) {
            return ArchiveOperationResult.failure("unknown_item", 0L, null);
        }

        ItemStack prototype = item.getDefaultInstance();
        if (prototype.isEmpty()) {
            return ArchiveOperationResult.failure("unsupported_item", 0L, null);
        }

        return extract(player, new ArchiveItemKey(itemId), requestedCount);
    }

    public static ArchiveOperationResult extract(ServerPlayer player, ArchiveItemKey key, int requestedCount) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        if (key == null) {
            return ArchiveOperationResult.failure("unknown_item", 0L, null);
        }

        ItemStack prototype = data.ledger().prototype(key);
        if (prototype.isEmpty()) {
            return ArchiveOperationResult.failure("unsupported_item", 0L, null);
        }

        int count = Math.max(1, requestedCount);
        long stored = data.ledger().count(key);
        ArchiveOperationCost cost = ArchiveCostPolicy.estimate(prototype, count, ArchiveOperationType.EXTRACT);
        ArchiveAccessPolicy.ArchiveAccessCheck access = ArchiveAccessPolicy.check(data, cost);
        if (!access.allowed()) {
            return ArchiveOperationResult.failure(access.reason(), stored, cost);
        }
        if (stored < count) {
            return ArchiveOperationResult.failure("insufficient_items", stored, cost);
        }
        ArchivePaymentResult payment = ArchivePaymentService.pay(player, data, cost);
        if (!payment.success()) {
            return ArchiveOperationResult.failure(payment.reason(), stored, cost, payment);
        }
        ResonanceRiskLevel riskLevel = ResonanceService.applyPayment(player, data, payment);
        if (!data.ledger().remove(key, count)) {
            return ArchiveOperationResult.failure("ledger_rejected", stored, cost);
        }

        giveStack(player, prototype, count);
        data.runtimeState().markDirty(ArchiveDirtyReason.LEDGER_CHANGED);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        return ArchiveOperationResult.success(count, data.ledger().count(key), cost, payment, riskLevel);
    }

    public static ArchiveOperationResult provision(ServerPlayer player, ResourceLocation itemId) {
        PlayerArchiveData data = player.getData(PlayerArchiveAttachments.PLAYER_ARCHIVE);
        ArchiveProvisionRule rule = data.provisionRules().get(itemId).orElse(null);
        if (rule == null) {
            return ArchiveOperationResult.failure("provision_rule_missing", 0L, null);
        }
        if (!rule.enabled()) {
            return ArchiveOperationResult.failure("provision_rule_disabled", 0L, null);
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (!BuiltInRegistries.ITEM.getKey(item).equals(itemId)) {
            return ArchiveOperationResult.failure("unknown_item", 0L, null);
        }

        ItemStack prototype = item.getDefaultInstance();
        if (prototype.isEmpty()) {
            return ArchiveOperationResult.failure("unsupported_item", 0L, null);
        }

        int current = player.getInventory().countItem(item);
        int needed = Math.max(0, rule.targetCount() - current);
        ArchiveItemKey key = new ArchiveItemKey(itemId);
        long stored = data.ledger().count(key);
        if (needed <= 0) {
            return ArchiveOperationResult.success(0L, stored, null, null, ResonanceRiskLevel.STABLE);
        }
        if (stored < needed) {
            return ArchiveOperationResult.failure("insufficient_items", stored, null);
        }

        int capacity = insertionCapacity(player.getInventory(), prototype, needed);
        if (capacity <= 0) {
            return ArchiveOperationResult.failure("inventory_full", stored, null);
        }
        int count = Math.min(needed, capacity);
        ArchiveOperationCost cost = ArchiveCostPolicy.estimate(prototype, count, ArchiveOperationType.PROVISION);
        ArchiveAccessPolicy.ArchiveAccessCheck access = ArchiveAccessPolicy.check(data, cost);
        if (!access.allowed()) {
            return ArchiveOperationResult.failure(access.reason(), stored, cost);
        }

        ArchivePaymentResult payment = ArchivePaymentService.pay(player, data, cost);
        if (!payment.success()) {
            return ArchiveOperationResult.failure(payment.reason(), stored, cost, payment);
        }
        ResonanceRiskLevel riskLevel = ResonanceService.applyPayment(player, data, payment);
        if (!data.ledger().remove(key, count)) {
            return ArchiveOperationResult.failure("ledger_rejected", stored, cost);
        }

        int inserted = insertIntoInventory(player, item, count);
        if (inserted < count) {
            data.ledger().add(new ItemStack(item), count - inserted);
        }
        data.runtimeState().markDirty(ArchiveDirtyReason.LEDGER_CHANGED);
        player.setData(PlayerArchiveAttachments.PLAYER_ARCHIVE, data);
        ModNetwork.syncArchive(player, data);
        return ArchiveOperationResult.success(inserted, data.ledger().count(key), cost, payment, riskLevel);
    }

    public static long query(PlayerArchiveData data, ResourceLocation itemId) {
        return data.ledger().count(new ArchiveItemKey(itemId));
    }

    public static int inventoryCount(ServerPlayer player, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (!BuiltInRegistries.ITEM.getKey(item).equals(itemId)) {
            return 0;
        }
        return player.getInventory().countItem(item);
    }

    private static void giveItem(ServerPlayer player, Item item, int count) {
        giveStack(player, item.getDefaultInstance(), count);
    }

    private static void giveStack(ServerPlayer player, ItemStack prototype, int count) {
        int remaining = count;
        int maxStackSize = Math.max(1, prototype.getMaxStackSize());
        while (remaining > 0) {
            int chunk = Math.min(maxStackSize, remaining);
            ItemStack stack = prototype.copyWithCount(chunk);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= chunk;
        }
    }

    private static int insertIntoInventory(ServerPlayer player, Item item, int count) {
        int remaining = count;
        int maxStackSize = Math.max(1, item.getDefaultInstance().getMaxStackSize());
        while (remaining > 0) {
            int chunk = Math.min(maxStackSize, remaining);
            ItemStack stack = new ItemStack(item, chunk);
            if (!player.getInventory().add(stack)) {
                break;
            }
            remaining -= chunk;
        }
        return count - remaining;
    }

    private static int insertionCapacity(Container inventory, ItemStack prototype, int limit) {
        int capacity = 0;
        int maxStackSize = Math.max(1, prototype.getMaxStackSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                capacity += maxStackSize;
            } else if (ItemStack.isSameItemSameComponents(stack, prototype)) {
                capacity += Math.max(0, Math.min(maxStackSize, stack.getMaxStackSize()) - stack.getCount());
            }
            if (capacity >= limit) {
                return limit;
            }
        }
        return Math.min(capacity, limit);
    }
}
