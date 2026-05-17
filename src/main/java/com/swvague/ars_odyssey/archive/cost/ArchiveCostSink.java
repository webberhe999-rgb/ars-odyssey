package com.swvague.ars_odyssey.archive.cost;

import com.swvague.ars_odyssey.archive.ArchiveOperationCost;
import com.swvague.ars_odyssey.archive.PlayerArchiveData;

import net.minecraft.server.level.ServerPlayer;

public interface ArchiveCostSink {
    ArchivePaymentResult pay(ServerPlayer player, PlayerArchiveData data, ArchiveOperationCost cost);
}
