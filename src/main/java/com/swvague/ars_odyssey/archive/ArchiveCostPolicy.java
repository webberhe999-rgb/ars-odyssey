package com.swvague.ars_odyssey.archive;

import com.swvague.ars_odyssey.value.ItemValueResolver;
import com.swvague.ars_odyssey.value.ItemValueTier;

import net.minecraft.world.item.ItemStack;

public final class ArchiveCostPolicy {
    private static final double BASE_SOURCE_PER_ITEM = 4.0D;

    private ArchiveCostPolicy() {
    }

    public static ArchiveOperationCost estimate(ItemStack stack, long count, ArchiveOperationType operationType) {
        ItemValueTier tier = ItemValueResolver.tierOf(stack);
        long clampedCount = Math.max(1L, count);
        double tierMultiplier = tier.sourceCostMultiplier();
        double sourceCost = Double.isInfinite(tierMultiplier)
                ? Double.POSITIVE_INFINITY
                : BASE_SOURCE_PER_ITEM * clampedCount * tierMultiplier * operationType.sourceMultiplier();
        double resonanceLoad = tier.level() * operationType.resonanceLoadMultiplier() * Math.sqrt(clampedCount);
        double dissonancePressure = tier.level() >= 10
                ? resonanceLoad * 0.25D
                : resonanceLoad * 0.05D;

        return new ArchiveOperationCost(
                operationType,
                operationType.requiredAccessMode(),
                tier,
                clampedCount,
                sourceCost,
                resonanceLoad,
                dissonancePressure
        );
    }
}
