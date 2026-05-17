package com.swvague.ars_odyssey.client.gui.spellbook;

import com.swvague.ars_odyssey.network.ModNetwork;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;

public class SpellBookArchiveUi {
    private static final int TAB_W = 23;
    private static final int TAB_H = 20;
    private static final int GOLD = 0xFFE9B833;
    private static final int TEAL = 0xFF2FAEAA;

    public void reset() {
    }

    public boolean isOpen() {
        return false;
    }

    public void close() {
    }

    public boolean handleClick(double mouseX, double mouseY, int button, int bookLeft, int bookTop, InteractionHand returnHand) {
        if (button == 0 && inside(mouseX, mouseY, tabX(bookLeft), tabY(bookTop), TAB_W, TAB_H)) {
            ModNetwork.openArchiveTerminal(returnHand);
            return true;
        }
        return false;
    }

    public void render(GuiGraphics graphics, Font font, int bookLeft, int bookTop, int mouseX, int mouseY) {
        int x = tabX(bookLeft);
        int y = tabY(bookTop);
        graphics.fill(x, y, x + TAB_W, y + TAB_H, 0xFF4D2369);
        graphics.fill(x + 2, y + 2, x + TAB_W - 2, y + TAB_H - 2, 0xFF653083);
        graphics.fill(x + TAB_W - 2, y + 4, x + TAB_W, y + TAB_H - 4, GOLD);
        graphics.fill(x + 6, y + 7, x + 16, y + 17, 0xFFEBD67B);
        graphics.fill(x + 8, y + 5, x + 14, y + 19, TEAL);
        graphics.drawString(font, "A", x + 8, y + 8, 0xFFFFFFFF, false);
    }

    private int tabX(int bookLeft) {
        return bookLeft - 15;
    }

    private int tabY(int bookTop) {
        return bookTop + 22;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
