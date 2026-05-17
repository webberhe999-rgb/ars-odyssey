package com.swvague.ars_odyssey.archive.cost;

import java.util.Comparator;
import java.util.List;

import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.swvague.ars_odyssey.archive.ArchiveCoreBinding;
import com.swvague.ars_odyssey.archive.ArchiveOperationCost;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.config.OdysseyConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ArsSourceCostSink implements ArchiveCostSink {
    @Override
    public ArchivePaymentResult pay(ServerPlayer player, PlayerArchiveData data, ArchiveOperationCost cost) {
        if (player.isCreative()) {
            return ArchivePaymentResult.success(0, cost.resonanceLoad(), cost.dissonancePressure());
        }
        if (!OdysseyConfig.ENABLE_ARCHIVE_SOURCE_PAYMENT.get()) {
            return ArchivePaymentResult.skipped(
                    "source_payment_disabled",
                    cost.resonanceLoad(),
                    cost.dissonancePressure()
            );
        }
        if (Double.isInfinite(cost.sourceCost())) {
            return ArchivePaymentResult.failure("source_cost_blocked", 0);
        }

        int sourceCost = (int) Math.ceil(cost.sourceCost());
        if (sourceCost <= 0) {
            return ArchivePaymentResult.success(0, cost.resonanceLoad(), cost.dissonancePressure());
        }

        ArchiveCoreBinding binding = data.coreBinding().orElse(null);
        if (binding == null) {
            return ArchivePaymentResult.failure("archive_unbound", 0);
        }
        if (!player.level().dimension().location().equals(binding.dimension()) || !(player.level() instanceof ServerLevel serverLevel)) {
            return ArchivePaymentResult.failure("archive_core_not_in_current_dimension", 0);
        }

        List<ISpecialSourceProvider> providers = SourceUtil.canTakeSource(
                binding.pos(),
                serverLevel,
                OdysseyConfig.ARCHIVE_SOURCE_RANGE.get()
        );
        providers.sort(Comparator.comparingInt(provider -> provider.getCurrentPos().distManhattan(binding.pos())));

        int available = 0;
        for (ISpecialSourceProvider provider : providers) {
            if (provider.isValid()) {
                available += Math.max(0, provider.getSource().getSource());
                if (available >= sourceCost) {
                    break;
                }
            }
        }
        if (available < sourceCost) {
            return ArchivePaymentResult.failure("insufficient_source", 0);
        }

        int remaining = sourceCost;
        int paid = 0;
        for (ISpecialSourceProvider provider : providers) {
            if (!provider.isValid() || remaining <= 0) {
                continue;
            }
            int removable = Math.min(remaining, Math.max(0, provider.getSource().getSource()));
            if (removable <= 0) {
                continue;
            }
            int removed = provider.getSource().removeSource(removable);
            paid += removed;
            remaining -= removed;
        }

        if (remaining > 0) {
            return ArchivePaymentResult.failure("source_network_changed", paid);
        }
        return ArchivePaymentResult.success(paid, cost.resonanceLoad(), cost.dissonancePressure());
    }
}
