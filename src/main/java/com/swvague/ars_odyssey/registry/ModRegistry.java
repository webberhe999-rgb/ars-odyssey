package com.swvague.ars_odyssey.registry;

import com.swvague.ars_odyssey.item.OdysseySpellBook;
import com.swvague.ars_odyssey.archive.menu.ArchiveTerminalMenu;
import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlock;
import com.swvague.ars_odyssey.block.ResonanceNexusCoreBlockEntity;
import com.swvague.ars_odyssey.block.menu.NexusConfigMenu;
import com.swvague.ars_odyssey.entity.EmberlingWyrmling;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.swvague.ars_odyssey.ArsOdyssey.MODID;

public class ModRegistry {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static void registerRegistries(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        ITEMS.register(bus);
        ENTITY_TYPES.register(bus);
        MENUS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    public static final DeferredHolder<Item, ? extends Item> ODYSSEY_SPELL_BOOK =
            ITEMS.register("odyssey_boundless_spell_book", () -> new OdysseySpellBook(SpellTier.CREATIVE));
    public static final DeferredHolder<Item, ? extends Item> ODYSSEY_INITIATE_SPELL_BOOK =
            ITEMS.register("odyssey_initiate_spell_book", () -> new OdysseySpellBook(SpellTier.ONE));
    public static final DeferredHolder<Item, ? extends Item> ODYSSEY_ADEPT_SPELL_BOOK =
            ITEMS.register("odyssey_adept_spell_book", () -> new OdysseySpellBook(SpellTier.TWO));
    public static final DeferredHolder<Item, ? extends Item> ODYSSEY_ARCHMAGE_SPELL_BOOK =
            ITEMS.register("odyssey_archmage_spell_book", () -> new OdysseySpellBook(SpellTier.THREE));
    public static final DeferredHolder<Block, ResonanceNexusCoreBlock> RESONANCE_ARCHIVE_CORE =
            BLOCKS.register("resonance_nexus_core", () -> new ResonanceNexusCoreBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 9.0F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(ResonanceNexusCoreBlock.PARTICLES)
                            ? Math.min(15, 4 + state.getValue(ResonanceNexusCoreBlock.TIER) * 2)
                            : 0)));
    public static final DeferredHolder<Item, ? extends Item> RESONANCE_ARCHIVE_CORE_ITEM =
            ITEMS.register("resonance_nexus_core", () -> new BlockItem(RESONANCE_ARCHIVE_CORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> RESONANCE_PRISM_SHARD_VISUAL =
            ITEMS.register("resonance_prism_shard_visual", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> RESONANCE_LAW_CUBE_VISUAL =
            ITEMS.register("resonance_law_cube_visual", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, ? extends Item> RESONANCE_LAW_CORE_VISUAL =
            ITEMS.register("resonance_law_core_visual", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResonanceNexusCoreBlockEntity>> RESONANCE_NEXUS_CORE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("resonance_nexus_core", () -> BlockEntityType.Builder.of(
                    ResonanceNexusCoreBlockEntity::new,
                    RESONANCE_ARCHIVE_CORE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ArchiveTerminalMenu>> ARCHIVE_TERMINAL_MENU =
            MENUS.register("archive_terminal", () -> IMenuTypeExtension.create(ArchiveTerminalMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<NexusConfigMenu>> NEXUS_CONFIG_MENU =
            MENUS.register("nexus_config", () -> IMenuTypeExtension.create(NexusConfigMenu::new));
    public static final DeferredHolder<EntityType<?>, EntityType<EmberlingWyrmling>> EMBERLING_WYRMLING =
            ENTITY_TYPES.register("emberling_wyrmling", () -> EntityType.Builder
                    .of(EmberlingWyrmling::new, MobCategory.CREATURE)
                    .sized(1.55F, 2.25F)
                    .eyeHeight(1.75F)
                    .passengerAttachments(2.05F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("emberling_wyrmling"));
    public static final DeferredHolder<Item, ? extends Item> EMBERLING_WYRMLING_SPAWN_EGG =
            ITEMS.register("emberling_wyrmling_spawn_egg", () -> new DeferredSpawnEggItem(
                    EMBERLING_WYRMLING,
                    0x9d2f22,
                    0x35d7d0,
                    new Item.Properties()));

    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(EMBERLING_WYRMLING.get(), EmberlingWyrmling.createAttributes().build());
    }

    public static boolean isOdysseySpellBook(ItemStack stack) {
        return stack != null
                && (stack.is(ODYSSEY_SPELL_BOOK.get())
                || stack.is(ODYSSEY_INITIATE_SPELL_BOOK.get())
                || stack.is(ODYSSEY_ADEPT_SPELL_BOOK.get())
                || stack.is(ODYSSEY_ARCHMAGE_SPELL_BOOK.get()));
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARS_ODYSSEY_TAB =
            CREATIVE_TABS.register("ars_odyssey_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ars_odyssey"))
                    .icon(() -> new ItemStack(ODYSSEY_SPELL_BOOK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ODYSSEY_INITIATE_SPELL_BOOK.get());
                        output.accept(ODYSSEY_ADEPT_SPELL_BOOK.get());
                        output.accept(ODYSSEY_ARCHMAGE_SPELL_BOOK.get());
                        output.accept(ODYSSEY_SPELL_BOOK.get());
                        output.accept(RESONANCE_ARCHIVE_CORE_ITEM.get());
                        output.accept(EMBERLING_WYRMLING_SPAWN_EGG.get());
                    })
                    .build());
}
