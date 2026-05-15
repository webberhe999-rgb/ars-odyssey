package com.example.ars_odyssey.registry;

import com.example.ars_odyssey.item.ExampleCosmetic;
import com.hollingsworth.arsnouveau.api.sound.SpellSound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import static com.example.ars_odyssey.ArsOdyssey.MODID;
import static com.example.ars_odyssey.ArsOdyssey.prefix;
import static net.minecraft.core.registries.Registries.SOUND_EVENT;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import com.example.ars_odyssey.item.OdysseySpellBook;
public class ModRegistry {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(SOUND_EVENT, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static void registerRegistries(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    public static final DeferredHolder<Item, ? extends Item> EXAMPLE =
            ITEMS.register("star_hat", () -> new ExampleCosmetic(new Item.Properties()));

    public static final DeferredHolder<Item, ? extends Item> ODYSSEY_SPELL_BOOK =
            ITEMS.register("odyssey_spell_book", () -> new OdysseySpellBook(SpellTier.CREATIVE));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARS_ODYSSEY_TAB =
            CREATIVE_TABS.register("ars_odyssey_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ars_odyssey"))
                    .icon(() -> new ItemStack(ODYSSEY_SPELL_BOOK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ODYSSEY_SPELL_BOOK.get());

                    })
                    .build());
    //this is an example of how to register a sound. You also need to add the sound to the sound.json file, referencing your ogg files, and a texture for the button under textures/sounds.
    //this example will use one of the existing sounds randomly
    public static DeferredHolder<SoundEvent, SoundEvent> EXAMPLE_FAMILY = SOUNDS.register("example_sound", () -> makeSound("example_sound"));
    public static SpellSound EXAMPLE_SPELL_SOUND = new SpellSound(ModRegistry.EXAMPLE_FAMILY, Component.literal("Example"), prefix("example_random_sound"));




    static SoundEvent makeSound(@NotNull String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, name));
    }
}
