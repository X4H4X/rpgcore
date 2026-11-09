package com.rpgcore.data;

import com.mojang.serialization.Codec;
import com.rpgcore.RPGCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RPGCore.MODID);

    public static final Supplier<AttachmentType<PlayerAppearanceData>> APPEARANCE =
            ATTACHMENT_TYPES.register("appearance", () -> AttachmentType.builder(() -> PlayerAppearanceData.DEFAULT)
                    .serialize(PlayerAppearanceData.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<ExtraEquipmentData>> EXTRA_EQUIPMENT =
            ATTACHMENT_TYPES.register("extra_equipment", () -> AttachmentType.builder(() -> new ExtraEquipmentData())
                    .serialize(ExtraEquipmentData.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<Boolean>> CRAWLING =
            ATTACHMENT_TYPES.register("crawling", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());

    public static final Supplier<AttachmentType<Boolean>> HOOD_UP =
            ATTACHMENT_TYPES.register("hood_up", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<CharacterSlotsData>> CHARACTER_SLOTS =
            ATTACHMENT_TYPES.register("character_slots", () -> AttachmentType.builder(() -> new CharacterSlotsData())
                    .serialize(CharacterSlotsData.CODEC)
                    .copyOnDeath()
                    .build());

    public static final Supplier<AttachmentType<Boolean>> STAND_CLOAK =
            ATTACHMENT_TYPES.register("stand_cloak", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .build());

    // Таймер нокаута (в тиках: 1200 тиков = 60 секунд). 0 = игрок не в нокауте.
    public static final Supplier<AttachmentType<Integer>> DOWNED_TICKS =
            ATTACHMENT_TYPES.register("downed_ticks", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}