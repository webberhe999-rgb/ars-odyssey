package com.swvague.ars_odyssey.archive.cost;

import com.swvague.ars_odyssey.archive.ArchiveOperationCost;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;

import net.minecraft.server.level.ServerPlayer;

public final class ArchivePaymentService {
    private static final ArchiveCostSink SOURCE_COST_SINK = new ArsSourceCostSink();

    private ArchivePaymentService() {
    }

    public static ArchivePaymentResult pay(ServerPlayer player, PlayerArchiveData data, ArchiveOperationCost cost) {
        return SOURCE_COST_SINK.pay(player, data, cost);
    }
}
