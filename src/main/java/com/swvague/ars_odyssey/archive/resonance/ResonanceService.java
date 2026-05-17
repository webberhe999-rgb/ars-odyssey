package com.swvague.ars_odyssey.archive.resonance;

import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.archive.cost.ArchivePaymentResult;
import com.swvague.ars_odyssey.archive.runtime.ArchiveDirtyReason;

import net.minecraft.server.level.ServerPlayer;

public final class ResonanceService {
    private ResonanceService() {
    }

    public static ResonanceRiskLevel applyPayment(ServerPlayer player, PlayerArchiveData data, ArchivePaymentResult payment) {
        ResonanceState state = data.resonanceState();
        state.applyOperation(
                payment.resonanceLoadAccepted(),
                payment.dissonancePressureAccepted(),
                player.serverLevel().getGameTime()
        );
        data.runtimeState().markDirty(ArchiveDirtyReason.RESONANCE_CHANGED);
        return ResonanceRiskLevel.from(state);
    }

    public static ResonanceRiskLevel refresh(ServerPlayer player, PlayerArchiveData data) {
        data.resonanceState().recover(player.serverLevel().getGameTime());
        return ResonanceRiskLevel.from(data.resonanceState());
    }
}
