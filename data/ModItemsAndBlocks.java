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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
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

    // 6. Полублоки Земли, Дёрна и Вспаханной Пашни (вспахиваются мотыгой и поддерживают любые растения!)
    public static final DeferredBlock<FarmlandSlabBlock> FARMLAND_SLAB = registerBlock("farmland_slab",
            () -> new FarmlandSlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND).noOcclusion()));

    public static final DeferredBlock<TillableSlabBlock> DIRT_SLAB = registerBlock("dirt_slab",
            () -> new TillableSlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT), FARMLAND_SLAB::get));

    public static final DeferredBlock<TillableSlabBlock> GRASS_SLAB = registerBlock("grass_slab",
            () -> new TillableSlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK), FARMLAND_SLAB::get));

    // 7. Большие 3-блочные кровати под Орка (3x1) из всех 16 цветов шерсти
    public static final List<DeferredBlock<OrcGiantBedBlock>> ORC_GIANT_BEDS = new ArrayList<>();

    public static final DeferredBlock<OrcGiantBedBlock> ORC_WHITE_BED = registerOrcBed("orc_white_bed", DyeColor.WHITE);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_LIGHT_GRAY_BED = registerOrcBed("orc_light_gray_bed", DyeColor.LIGHT_GRAY);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_GRAY_BED = registerOrcBed("orc_gray_bed", DyeColor.GRAY);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_BLACK_BED = registerOrcBed("orc_black_bed", DyeColor.BLACK);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_BROWN_BED = registerOrcBed("orc_brown_bed", DyeColor.BROWN);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_RED_BED = registerOrcBed("orc_red_bed", DyeColor.RED);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_ORANGE_BED = registerOrcBed("orc_orange_bed", DyeColor.ORANGE);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_YELLOW_BED = registerOrcBed("orc_yellow_bed", DyeColor.YELLOW);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_LIME_BED = registerOrcBed("orc_lime_bed", DyeColor.LIME);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_GREEN_BED = registerOrcBed("orc_green_bed", DyeColor.GREEN);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_CYAN_BED = registerOrcBed("orc_cyan_bed", DyeColor.CYAN);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_LIGHT_BLUE_BED = registerOrcBed("orc_light_blue_bed", DyeColor.LIGHT_BLUE);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_BLUE_BED = registerOrcBed("orc_blue_bed", DyeColor.BLUE);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_PURPLE_BED = registerOrcBed("orc_purple_bed", DyeColor.PURPLE);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_MAGENTA_BED = registerOrcBed("orc_magenta_bed", DyeColor.MAGENTA);
    public static final DeferredBlock<OrcGiantBedBlock> ORC_PINK_BED = registerOrcBed("orc_pink_bed", DyeColor.PINK);

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

                        output.accept(GRASS_SLAB.get());
                        output.accept(DIRT_SLAB.get());
                        output.accept(FARMLAND_SLAB.get());

                        for (DeferredBlock<OrcGiantBedBlock> bed : ORC_GIANT_BEDS) {
                            output.accept(bed.get());
                        }

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

    private static DeferredBlock<OrcGiantBedBlock> registerOrcBed(String name, DyeColor color) {
        DeferredBlock<OrcGiantBedBlock> bed = BLOCKS.register(name,
                () -> new OrcGiantBedBlock(color, BlockBehaviour.Properties.ofFullCopy(Blocks.RED_BED).noOcclusion()));
        ITEMS.register(name, () -> new BlockItem(bed.get(), new Item.Properties().stacksTo(1)));
        ORC_GIANT_BEDS.add(bed);
        return bed;
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        for (DeferredBlock<OrcGiantBedBlock> bed : ORC_GIANT_BEDS) {
            event.modify(BlockEntityType.BED, bed.get());
        }
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        CREATIVE_TABS.register(eventBus);
        ENTITIES.register(eventBus);
        MENUS.register(eventBus);
        eventBus.addListener(ModItemsAndBlocks::onAddBlockEntityBlocks);
    }
}