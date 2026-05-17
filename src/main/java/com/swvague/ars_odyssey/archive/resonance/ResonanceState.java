package com.swvague.ars_odyssey.archive.resonance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class ResonanceState {
    private double stability = 100.0D;
    private double load;
    private double dissonancePressure;
    private long lastUpdateGameTime = Long.MIN_VALUE;

    public double stability() {
        return stability;
    }

    public double load() {
        return load;
    }

    public double dissonancePressure() {
        return dissonancePressure;
    }

    public long lastUpdateGameTime() {
        return lastUpdateGameTime;
    }

    public boolean isOverloaded() {
        return load > stability;
    }

    public void applyOperation(double resonanceLoad, double pressure, long gameTime) {
        recover(gameTime);
        load += Math.max(0.0D, resonanceLoad);
        dissonancePressure += Math.max(0.0D, pressure);
        stability = clamp(stability - pressure * 0.05D, 0.0D, 100.0D);
        lastUpdateGameTime = gameTime;
    }

    public void recover(long gameTime) {
        if (lastUpdateGameTime == Long.MIN_VALUE) {
            lastUpdateGameTime = gameTime;
            return;
        }

        long elapsed = Math.max(0L, gameTime - lastUpdateGameTime);
        if (elapsed <= 0L) {
            return;
        }

        double seconds = elapsed / 20.0D;
        load = Math.max(0.0D, load - seconds * 2.0D);
        dissonancePressure = Math.max(0.0D, dissonancePressure - seconds * 0.35D);
        if (load < stability) {
            stability = clamp(stability + seconds * 0.25D, 0.0D, 100.0D);
        }
        lastUpdateGameTime = gameTime;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("stability", stability);
        tag.putDouble("load", load);
        tag.putDouble("dissonancePressure", dissonancePressure);
        tag.putLong("lastUpdateGameTime", lastUpdateGameTime);
        return tag;
    }

    public void load(CompoundTag tag) {
        stability = tag.contains("stability", Tag.TAG_DOUBLE) ? tag.getDouble("stability") : 100.0D;
        load = tag.contains("load", Tag.TAG_DOUBLE) ? tag.getDouble("load") : 0.0D;
        dissonancePressure = tag.contains("dissonancePressure", Tag.TAG_DOUBLE) ? tag.getDouble("dissonancePressure") : 0.0D;
        lastUpdateGameTime = tag.contains("lastUpdateGameTime", Tag.TAG_LONG) ? tag.getLong("lastUpdateGameTime") : Long.MIN_VALUE;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
