package com.swvague.ars_odyssey.network;

import com.swvague.ars_odyssey.ArsOdyssey;
import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FillArchiveCraftingGridPacket(ResourceLocation recipeId, boolean maxTransfer) implements CustomPacketPayload {
    public static final Type<FillArchiveCraftingGridPacket> TYPE =
            new Type<>(ArsOdyssey.prefix("fill_archive_crafting_grid"));

    public static final StreamCodec<ByteBuf, FillArchiveCraftingGridPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            FillArchiveCraftingGridPacket::recipeId,
            ByteBufCodecs.BOOL,
            FillArchiveCraftingGridPacket::maxTransfer,
            FillArchiveCraftingGridPacket::new);

    public static void handle(FillArchiveCraftingGridPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof ArchiveTerminalMenu menu)
                    || packet.recipeId() == null) {
                return;
            }

            RecipeHolder<?> holder = player.server.getRecipeManager().byKey(packet.recipeId()).orElse(null);
            if (holder == null || !(holder.value() instanceof CraftingRecipe craftingRecipe)) {
                player.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.crafting_transfer.failed"), true);
                return;
            }

            RecipeHolder<CraftingRecipe> craftingHolder = new RecipeHolder<>(holder.id(), craftingRecipe);
            if (!menu.fillCraftingGridFromRecipe(player, craftingHolder, packet.maxTransfer())) {
                player.displayClientMessage(Component.translatable("ars_odyssey.archive_terminal.crafting_transfer.failed"), true);
            }
        });
    }

    @Override
    public Type<FillArchiveCraftingGridPacket> type() {
        return TYPE;
    }
}
