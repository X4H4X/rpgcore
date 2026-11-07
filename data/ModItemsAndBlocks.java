package com.rpgcore.data;

import com.rpgcore.RPGCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItemsAndBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RPGCore.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RPGCore.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RPGCore.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, RPGCore.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RPGCore.MODID);

    // 1. Одежда крестьянина и Плащ с капюшоном
    public static final DeferredItem<PeasantClothesItem> STRAW_HAT = ITEMS.register("straw_hat",
            () -> new PeasantClothesItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, PeasantClothesItem.ApparelType.HAT,
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PeasantClothesItem> PEASANT_SHIRT = ITEMS.register("peasant_shirt",
            () -> new PeasantClothesItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, PeasantClothesItem.ApparelType.SHIRT,
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PeasantClothesItem> PEASANT_PANTS = ITEMS.register("peasant_pants",
            () -> new PeasantClothesItem(ArmorMaterials.LEATHER, ArmorItem.Type.LEGGINGS, PeasantClothesItem.ApparelType.PANTS,
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<PeasantClothesItem> PEASANT_SHOES = ITEMS.register("peasant_shoes",
            () -> new PeasantClothesItem(ArmorMaterials.LEATHER, ArmorItem.Type.BOOTS, PeasantClothesItem.ApparelType.SHOES,
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<HoodedCloakItem> HOODED_CLOAK = ITEMS.register("hooded_cloak",
            () -> new HoodedCloakItem(new Item.Properties().stacksTo(1)));

    // 2. 5 уровней прокачки Рюкзаков
    public static final DeferredItem<BackpackItem> LEATHER_BACKPACK = ITEMS.register("leather_backpack",
            () -> new BackpackItem(BackpackItem.Tier.LEATHER, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BackpackItem> IRON_BACKPACK = ITEMS.register("iron_backpack",
            () -> new BackpackItem(BackpackItem.Tier.IRON, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BackpackItem> GOLD_BACKPACK = ITEMS.register("gold_backpack",
            () -> new BackpackItem(BackpackItem.Tier.GOLD, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BackpackItem> DIAMOND_BACKPACK = ITEMS.register("diamond_backpack",
            () -> new BackpackItem(BackpackItem.Tier.DIAMOND, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BackpackItem> NETHERITE_BACKPACK = ITEMS.register("netherite_backpack",
            () -> new BackpackItem(BackpackItem.Tier.NETHERITE, new Item.Properties().stacksTo(1)));

    // 3. Блок подрубленного ствола дерева (HT's TreeChop)
    public static final DeferredBlock<ChoppedLogBlock> CHOPPED_LOG = BLOCKS.register("chopped_log",
            () -> new ChoppedLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));

    // 4. Сущность Трупа игрока и Меню инвентаря трупа
    public static final DeferredHolder<EntityType<?>, EntityType<PlayerCorpseEntity>> PLAYER_CORPSE =
            ENTITIES.register("player_corpse", () -> EntityType.Builder.<PlayerCorpseEntity>of(PlayerCorpseEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.45F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("player_corpse"));

    public static final DeferredHolder<MenuType<?>, MenuType<CorpseMenu>> CORPSE_MENU =
            MENUS.register("corpse_menu", () -> IMenuTypeExtension.create(CorpseMenu::new));

    // 5. Огромные ванильные двери 3x4
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_OAK_DOOR = registerDoor("colossal_oak_door", BlockSetType.OAK);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_SPRUCE_DOOR = registerDoor("colossal_spruce_door", BlockSetType.SPRUCE);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_BIRCH_DOOR = registerDoor("colossal_birch_door", BlockSetType.BIRCH);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_JUNGLE_DOOR = registerDoor("colossal_jungle_door", BlockSetType.JUNGLE);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_ACACIA_DOOR = registerDoor("colossal_acacia_door", BlockSetType.ACACIA);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_DARK_OAK_DOOR = registerDoor("colossal_dark_oak_door", BlockSetType.DARK_OAK);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_MANGROVE_DOOR = registerDoor("colossal_mangrove_door", BlockSetType.MANGROVE);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_CHERRY_DOOR = registerDoor("colossal_cherry_door", BlockSetType.CHERRY);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_BAMBOO_DOOR = registerDoor("colossal_bamboo_door", BlockSetType.BAMBOO);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_CRIMSON_DOOR = registerDoor("colossal_crimson_door", BlockSetType.CRIMSON);
    public static final DeferredBlock<ColossalDoorBlock> COLOSSAL_WARPED_DOOR = registerDoor("colossal_warped_door", BlockSetType.WARPED);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RPGCORE_TAB = CREATIVE_TABS.register("rpgcore_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rpgcore"))
                    .icon(() -> new ItemStack(HOODED_CLOAK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(STRAW_HAT.get());
                        output.accept(PEASANT_SHIRT.get());
                        output.accept(PEASANT_PANTS.get());
                        output.accept(PEASANT_SHOES.get());
                        output.accept(HOODED_CLOAK.get());

                        output.accept(LEATHER_BACKPACK.get());
                        output.accept(IRON_BACKPACK.get());
                        output.accept(GOLD_BACKPACK.get());
                        output.accept(DIAMOND_BACKPACK.get());
                        output.accept(NETHERITE_BACKPACK.get());

                        output.accept(COLOSSAL_OAK_DOOR.get());
                        output.accept(COLOSSAL_SPRUCE_DOOR.get());
                        output.accept(COLOSSAL_BIRCH_DOOR.get());
                        output.accept(COLOSSAL_JUNGLE_DOOR.get());
                        output.accept(COLOSSAL_ACACIA_DOOR.get());
                        output.accept(COLOSSAL_DARK_OAK_DOOR.get());
                        output.accept(COLOSSAL_MANGROVE_DOOR.get());
                        output.accept(COLOSSAL_CHERRY_DOOR.get());
                        output.accept(COLOSSAL_BAMBOO_DOOR.get());
                        output.accept(COLOSSAL_CRIMSON_DOOR.get());
                        output.accept(COLOSSAL_WARPED_DOOR.get());
                    })
                    .build());

    private static DeferredBlock<ColossalDoorBlock> registerDoor(String name, BlockSetType type) {
        return registerBlock(name, () -> new ColossalDoorBlock(type,
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).noOcclusion().noLootTable()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        CREATIVE_TABS.register(eventBus);
        ENTITIES.register(eventBus);
        MENUS.register(eventBus);
    }
}