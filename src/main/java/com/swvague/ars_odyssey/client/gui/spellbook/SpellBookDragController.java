package com.swvague.ars_odyssey.client.gui.spellbook;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class SpellBookDragController {
    private static final double DRAG_THRESHOLD_SQ = 9.0D;
    private static final double SNAP_CUTOFF_SQ = 900.0D;

    private int draggedSpellIndex = -1;
    private int hoveredSpellIndex = -1;
    private boolean draggingSpellPart = false;
    private double dragStartX;
    private double dragStartY;
    private double dragCurrentX;
    private double dragCurrentY;

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            boolean enabled,
            List<CraftingButton> craftingCells,
            List<AbstractSpellPart> spell
    ) {
        if (button != 0 || !enabled || craftingCells == null || spell == null) {
            return false;
        }

        int index = getSpellPartIndexAt(mouseX, mouseY, craftingCells);
        if (!hasSpellPartAt(index, spell)) {
            return false;
        }

        draggedSpellIndex = index;
        hoveredSpellIndex = index;
        draggingSpellPart = true;
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragCurrentX = mouseX;
        dragCurrentY = mouseY;
        hideDraggedSlot(index, craftingCells);
        return true;
    }

    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            List<CraftingButton> craftingCells
    ) {
        if (!draggingSpellPart) {
            return false;
        }

        dragCurrentX = mouseX;
        dragCurrentY = mouseY;
        hoveredSpellIndex = getNearestSpellPartIndex(mouseX, mouseY, craftingCells, SNAP_CUTOFF_SQ);
        return true;
    }

    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button,
            List<CraftingButton> craftingCells,
            List<AbstractSpellPart> spell,
            Runnable updateNextGlyphArrow,
            Runnable validate
    ) {
        if (!draggingSpellPart) {
            return false;
        }

        int sourceIndex = draggedSpellIndex;
        int clickedIndex = getSpellPartIndexAt(mouseX, mouseY, craftingCells);
        int targetIndex = getNearestSpellPartIndex(mouseX, mouseY, craftingCells, SNAP_CUTOFF_SQ);
        boolean shouldDelete = button == 0
                && sourceIndex == clickedIndex
                && !hasDraggedFarEnough(mouseX, mouseY);
        boolean recipeChanged = false;

        if (shouldDelete) {
            clearSpellPartAt(sourceIndex, spell, updateNextGlyphArrow, validate);
            recipeChanged = true;
        } else if (button == 0 && targetIndex >= 0 && targetIndex != sourceIndex) {
            moveSpellPart(sourceIndex, targetIndex, spell, updateNextGlyphArrow, validate);
            recipeChanged = true;
        }

        if (!recipeChanged && validate != null) {
            validate.run();
        }

        clearState();
        return true;
    }

    public void renderDraggedSpellPart(GuiGraphics graphics, List<AbstractSpellPart> spell) {
        if (!draggingSpellPart || !hasSpellPartAt(draggedSpellIndex, spell)) {
            return;
        }

        AbstractSpellPart draggedPart = spell.get(draggedSpellIndex);
        RenderUtils.drawSpellPart(
                draggedPart,
                graphics,
                (int) dragCurrentX - 8,
                (int) dragCurrentY - 8,
                16,
                false,
                200
        );
    }

    public boolean isDragging() {
        return draggingSpellPart;
    }

    public void clearState() {
        draggedSpellIndex = -1;
        hoveredSpellIndex = -1;
        draggingSpellPart = false;
        dragStartX = 0.0D;
        dragStartY = 0.0D;
        dragCurrentX = 0.0D;
        dragCurrentY = 0.0D;
    }

    private static int getSpellPartIndexAt(double mouseX, double mouseY, List<CraftingButton> craftingCells) {
        if (craftingCells == null) {
            return -1;
        }

        for (CraftingButton craftingButton : craftingCells) {
            if (craftingButton.visible && craftingButton.isMouseOver(mouseX, mouseY)) {
                return craftingButton.slotNum;
            }
        }
        return -1;
    }

    private static int getNearestSpellPartIndex(
            double mouseX,
            double mouseY,
            List<CraftingButton> craftingCells,
            double cutoffDistanceSq
    ) {
        if (craftingCells == null) {
            return -1;
        }

        int nearestIndex = -1;
        double nearestDistanceSq = cutoffDistanceSq;

        for (CraftingButton craftingButton : craftingCells) {
            if (!craftingButton.visible) {
                continue;
            }

            double centerX = craftingButton.getX() + craftingButton.getWidth() / 2.0D;
            double centerY = craftingButton.getY() + craftingButton.getHeight() / 2.0D;
            double dx = mouseX - centerX;
            double dy = mouseY - centerY;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq <= nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearestIndex = craftingButton.slotNum;
            }
        }

        return nearestIndex;
    }

    private static boolean hasSpellPartAt(int index, List<AbstractSpellPart> spell) {
        return index >= 0
                && spell != null
                && index < spell.size()
                && spell.get(index) != null;
    }

    private static void hideDraggedSlot(int index, List<CraftingButton> craftingCells) {
        if (craftingCells == null) {
            return;
        }

        for (CraftingButton craftingButton : craftingCells) {
            if (craftingButton.slotNum == index) {
                craftingButton.setAbstractSpellPart(null);
                return;
            }
        }
    }

    private static void clearSpellPartAt(
            int index,
            List<AbstractSpellPart> spell,
            Runnable updateNextGlyphArrow,
            Runnable validate
    ) {
        if (!hasSpellPartAt(index, spell)) {
            return;
        }

        spell.set(index, null);
        if (spell.stream().allMatch(part -> part == null)) {
            spell.clear();
        }
        refreshSpellRecipe(updateNextGlyphArrow, validate);
    }

    private static void moveSpellPart(
            int sourceIndex,
            int targetIndex,
            List<AbstractSpellPart> spell,
            Runnable updateNextGlyphArrow,
            Runnable validate
    ) {
        if (!hasSpellPartAt(sourceIndex, spell)) {
            return;
        }

        AbstractSpellPart movedPart = spell.remove(sourceIndex);
        int insertionIndex;
        if (targetIndex >= spell.size()) {
            insertionIndex = spell.size();
        } else if (targetIndex > sourceIndex) {
            insertionIndex = targetIndex;
        } else {
            insertionIndex = Math.min(targetIndex + 1, spell.size());
        }

        spell.add(insertionIndex, movedPart);
        refreshSpellRecipe(updateNextGlyphArrow, validate);
    }

    private static void refreshSpellRecipe(Runnable updateNextGlyphArrow, Runnable validate) {
        if (updateNextGlyphArrow != null) {
            updateNextGlyphArrow.run();
        }
        if (validate != null) {
            validate.run();
        }
    }

    private boolean hasDraggedFarEnough(double mouseX, double mouseY) {
        double dx = mouseX - dragStartX;
        double dy = mouseY - dragStartY;
        return dx * dx + dy * dy > DRAG_THRESHOLD_SQ;
    }
}
