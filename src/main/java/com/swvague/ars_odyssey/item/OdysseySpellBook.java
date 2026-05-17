package com.swvague.ars_odyssey.item;

import com.swvague.ars_odyssey.network.ModNetwork;
import com.swvague.ars_odyssey.network.ClearTruthifiedProjectilesPacket;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.api.util.StackUtil;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OdysseySpellBook extends SpellBook {

    public OdysseySpellBook(SpellTier tier) {
        super(tier);
    }

    public int complexityTierBonus() {
        if (tier == SpellTier.CREATIVE || tier.value >= SpellTier.CREATIVE.value) {
            return 6;
        }
        return switch (tier.value) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 4;
            default -> Math.max(0, (tier.value - 1) * 2);
        };
    }

    public static int complexityTierBonus(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof OdysseySpellBook book) {
            return book.complexityTierBonus();
        }
        return 0;
    }

    public static int activeComplexityTierBonus(Player player) {
        if (player == null) {
            return 0;
        }
        int main = complexityTierBonus(player.getMainHandItem());
        if (main > 0 || player.getMainHandItem().getItem() instanceof OdysseySpellBook) {
            return main;
        }
        return complexityTierBonus(player.getOffhandItem());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull TooltipContext context,
            @NotNull List<Component> tooltip,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ars_odyssey.odyssey_spell_book.complexity_tier_bonus", complexityTierBonus()));
    }

    /**
     * Shift + right-click clears all truthified Orbit Self projectiles belonging to this player.
     * The keybind alternative ({@link com.swvague.ars_odyssey.client.OdysseyKeyBindings}) also triggers this.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                ModNetwork.clearTruthifiedProjectiles();
            } else if (player instanceof ServerPlayer serverPlayer) {
                ClearTruthifiedProjectilesPacket.clearTruthifiedProjectilesFor(serverPlayer);
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        InteractionHand hand = StackUtil.getBookHand(player);
        if (hand == null) {
            return;
        }

        Minecraft.getInstance().setScreen(new GuiSpellBook(hand));
    }
}
