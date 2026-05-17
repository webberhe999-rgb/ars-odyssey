package com.swvague.ars_odyssey.client;

import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.registry.ModRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
public final class OdysseyKeyBindings {

    public static final KeyMapping CLEAR_TRUTHIFIED = new KeyMapping(
            "key.ars_odyssey.clear_truthified",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.ars_odyssey"
    );
    public static final KeyMapping OPEN_ARCHIVE = new KeyMapping(
            "key.ars_odyssey.open_archive",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.ars_odyssey"
    );

    private OdysseyKeyBindings() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CLEAR_TRUTHIFIED);
        event.register(OPEN_ARCHIVE);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (CLEAR_TRUTHIFIED.consumeClick()) {
            if (bookHand(mc) != null) {
                ModNetwork.clearTruthifiedProjectiles();
            }
        }
        while (OPEN_ARCHIVE.consumeClick()) {
            InteractionHand hand = bookHand(mc);
            if (hand != null) {
                ModNetwork.openArchiveTerminal(hand);
            }
        }
    }

    private static InteractionHand bookHand(Minecraft mc) {
        if (mc.player == null) {
            return null;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = mc.player.getItemInHand(hand);
            if (ModRegistry.isOdysseySpellBook(stack)) {
                return hand;
            }
        }
        return null;
    }
}
