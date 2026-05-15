package com.example.ars_odyssey.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public class OdysseySpellBookScreen extends Screen {

    private final InteractionHand hand;

    public OdysseySpellBookScreen(InteractionHand hand) {
        super(Component.translatable("screen.ars_odyssey.odyssey_spell_book"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Odyssey Spell Book UI"),
                this.width / 2,
                40,
                0xFFFFFF
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Hand: " + hand.name()),
                this.width / 2,
                60,
                0xAAAAAA
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}