package com.swvague.ars_odyssey.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.swvague.ars_odyssey.archive.PlayerArchiveData;
import com.swvague.ars_odyssey.knowledge.PlayerKnowledgeData;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncPlayerKnowledgePacket.TYPE,
                SyncPlayerKnowledgePacket.STREAM_CODEC,
                SyncPlayerKnowledgePacket::handle);
        registrar.playToClient(
                SyncPlayerArchivePacket.TYPE,
                SyncPlayerArchivePacket.STREAM_CODEC,
                SyncPlayerArchivePacket::handle);
        registrar.playToServer(
                SetTruthifiedGlyphPacket.TYPE,
                SetTruthifiedGlyphPacket.STREAM_CODEC,
                SetTruthifiedGlyphPacket::handle);
        registrar.playToServer(
                ClearTruthifiedProjectilesPacket.TYPE,
                ClearTruthifiedProjectilesPacket.STREAM_CODEC,
                ClearTruthifiedProjectilesPacket::handle);
        registrar.playToServer(
                SetArchiveProvisionRulePacket.TYPE,
                SetArchiveProvisionRulePacket.STREAM_CODEC,
                SetArchiveProvisionRulePacket::handle);
        registrar.playToServer(
                RemoveArchiveProvisionRulePacket.TYPE,
                RemoveArchiveProvisionRulePacket.STREAM_CODEC,
                RemoveArchiveProvisionRulePacket::handle);
        registrar.playToServer(
                RunArchiveProvisionPacket.TYPE,
                RunArchiveProvisionPacket.STREAM_CODEC,
                RunArchiveProvisionPacket::handle);
        registrar.playToServer(
                OpenArchiveTerminalPacket.TYPE,
                OpenArchiveTerminalPacket.STREAM_CODEC,
                OpenArchiveTerminalPacket::handle);
        registrar.playToServer(
                ExtractArchiveItemPacket.TYPE,
                ExtractArchiveItemPacket.STREAM_CODEC,
                ExtractArchiveItemPacket::handle);
        registrar.playToServer(
                ExtractArchiveItemToCursorPacket.TYPE,
                ExtractArchiveItemToCursorPacket.STREAM_CODEC,
                ExtractArchiveItemToCursorPacket::handle);
        registrar.playToServer(
                DepositCarriedArchiveItemPacket.TYPE,
                DepositCarriedArchiveItemPacket.STREAM_CODEC,
                DepositCarriedArchiveItemPacket::handle);
        registrar.playToServer(
                FillArchiveCraftingGridPacket.TYPE,
                FillArchiveCraftingGridPacket.STREAM_CODEC,
                FillArchiveCraftingGridPacket::handle);
        registrar.playToServer(
                UpdateNexusConfigPacket.TYPE,
                UpdateNexusConfigPacket.STREAM_CODEC,
                UpdateNexusConfigPacket::handle);
    }

    public static void syncKnowledge(ServerPlayer player, PlayerKnowledgeData data) {
        PacketDistributor.sendToPlayer(player, SyncPlayerKnowledgePacket.from(data));
    }

    public static void syncArchive(ServerPlayer player, PlayerArchiveData data) {
        PacketDistributor.sendToPlayer(player, SyncPlayerArchivePacket.from(player, data));
    }

    public static void setTruthifiedGlyph(ResourceLocation glyphId, boolean enabled) {
        PacketDistributor.sendToServer(new SetTruthifiedGlyphPacket(glyphId, "", enabled));
    }

    public static void setTruthifiedSpellSlot(ResourceLocation glyphId, String spellSlotKey, boolean enabled) {
        PacketDistributor.sendToServer(new SetTruthifiedGlyphPacket(glyphId, spellSlotKey, enabled));
    }

    public static void clearTruthifiedProjectiles() {
        PacketDistributor.sendToServer(new ClearTruthifiedProjectilesPacket());
    }

    public static void setArchiveProvisionRule(ResourceLocation itemId, int targetCount) {
        PacketDistributor.sendToServer(new SetArchiveProvisionRulePacket(itemId, targetCount));
    }

    public static void removeArchiveProvisionRule(ResourceLocation itemId) {
        PacketDistributor.sendToServer(new RemoveArchiveProvisionRulePacket(itemId));
    }

    public static void runArchiveProvision(ResourceLocation itemId) {
        PacketDistributor.sendToServer(new RunArchiveProvisionPacket(itemId, false));
    }

    public static void runAllArchiveProvision() {
        PacketDistributor.sendToServer(RunArchiveProvisionPacket.allRules());
    }

    public static void openArchiveTerminal(net.minecraft.world.InteractionHand hand) {
        PacketDistributor.sendToServer(OpenArchiveTerminalPacket.forHand(hand));
    }

    public static void extractArchiveItem(com.swvague.ars_odyssey.archive.ArchiveItemKey key, int count) {
        PacketDistributor.sendToServer(new ExtractArchiveItemPacket(key.itemId(), key.componentKey(), count));
    }

    public static void extractArchiveItemToCursor(com.swvague.ars_odyssey.archive.ArchiveItemKey key, int count) {
        PacketDistributor.sendToServer(new ExtractArchiveItemToCursorPacket(key.itemId(), key.componentKey(), count));
    }

    public static void depositCarriedArchiveItem(int count) {
        PacketDistributor.sendToServer(new DepositCarriedArchiveItemPacket(count));
    }

    public static void fillArchiveCraftingGrid(ResourceLocation recipeId, boolean maxTransfer) {
        PacketDistributor.sendToServer(new FillArchiveCraftingGridPacket(recipeId, maxTransfer));
    }

    public static void updateNexusConfig(net.minecraft.core.BlockPos center, int tier, boolean particles) {
        PacketDistributor.sendToServer(UpdateNexusConfigPacket.of(center, tier, particles));
    }
}
