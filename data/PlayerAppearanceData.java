package com.rpgcore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlayerAppearanceData(
        boolean isCreated,
        String firstName,
        String lastName,
        RaceType race,
        Gender gender,
        int skinColorIndex,
        int faceVariant,
        int eyeColorIndex,
        int hairStyle,
        int hairColorIndex,
        int beardStyle
) {

    public static final String[] MALE_HAIR_NAMES = {
            "Лысый",          // 0
            "Ирокез",         // 1
            "Короткая",       // 2
            "Средняя",        // 3
            "Хвост воина",    // 4
            "Длинные",        // 5
            "Височные пряди"  // 6
    };

    public static final String[] FEMALE_HAIR_NAMES = {
            "Длинные локоны", // 0
            "Высокий хвост",  // 1
            "Пышное каре",    // 2
            "Две косы",       // 3
            "Пучок с прядями" // 4
    };

    public static final int[] HAIR_COLORS = {
            0xFF6B442A, // Каштановый
            0xFF1C1815, // Чёрный
            0xFF3B271A, // Тёмно-русый
            0xFFD6B265, // Блонд
            0xFFA65224, // Рыжий
            0xFF8C6239, // Светло-русый
            0xFF523428, // Шоколадный
            0xFFD8D4CE  // Пепельно-седой
    };

    public static final String[] HAIR_COLOR_NAMES = {
            "Каштановый", "Чёрный", "Тёмно-русый", "Блонд",
            "Рыжий", "Светло-русый", "Шоколадный", "Пепельный"
    };

    public static final String[] BEARD_NAMES = {
            "Нет",               // 0
            "Лёгкая щетина",     // 1
            "Густая щетина",     // 2
            "Усы",               // 3
            "Эспаньолка",        // 4
            "Короткая борода",   // 5
            "Шкиперская",        // 6
            "Густая борода",     // 7
            "Длинная борода",    // 8
            "Окладистая длинная" // 9
    };

    public static final PlayerAppearanceData DEFAULT = new PlayerAppearanceData(
            false, "", "", RaceType.HUMAN, Gender.MALE, 0, 0, 0, 2, 0, 0
    );

    public String getFullRpName() {
        String f = firstName == null ? "" : firstName.trim();
        String l = lastName == null ? "" : lastName.trim();
        if (f.isEmpty() && l.isEmpty()) return "Неизвестный";
        if (l.isEmpty()) return f;
        return f + " " + l;
    }

    public int getResolvedSkinColor() {
        int[] cols = race.getSkinColors();
        return cols[Math.floorMod(skinColorIndex, cols.length)];
    }

    public int getResolvedEyeColor() {
        int[] cols = race.getEyeColors();
        return cols[Math.floorMod(eyeColorIndex, cols.length)];
    }

    public int getResolvedHairColor() {
        return HAIR_COLORS[Math.floorMod(hairColorIndex, HAIR_COLORS.length)];
    }

    public static final Codec<PlayerAppearanceData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("isCreated").forGetter(PlayerAppearanceData::isCreated),
            Codec.STRING.fieldOf("firstName").forGetter(PlayerAppearanceData::firstName),
            Codec.STRING.fieldOf("lastName").forGetter(PlayerAppearanceData::lastName),
            Codec.STRING.xmap(RaceType::valueOf, Enum::name).fieldOf("race").forGetter(PlayerAppearanceData::race),
            Codec.STRING.xmap(Gender::valueOf, Enum::name).fieldOf("gender").forGetter(PlayerAppearanceData::gender),
            Codec.INT.fieldOf("skinColorIndex").forGetter(PlayerAppearanceData::skinColorIndex),
            Codec.INT.fieldOf("faceVariant").forGetter(PlayerAppearanceData::faceVariant),
            Codec.INT.fieldOf("eyeColorIndex").forGetter(PlayerAppearanceData::eyeColorIndex),
            Codec.INT.fieldOf("hairStyle").forGetter(PlayerAppearanceData::hairStyle),
            Codec.INT.fieldOf("hairColorIndex").forGetter(PlayerAppearanceData::hairColorIndex),
            Codec.INT.fieldOf("beardStyle").forGetter(PlayerAppearanceData::beardStyle)
    ).apply(inst, PlayerAppearanceData::new));

    public static final StreamCodec<ByteBuf, PlayerAppearanceData> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.BOOL.encode(buf, val.isCreated());
                ByteBufCodecs.STRING_UTF8.encode(buf, val.firstName());
                ByteBufCodecs.STRING_UTF8.encode(buf, val.lastName());
                ByteBufCodecs.STRING_UTF8.encode(buf, val.race().name());
                ByteBufCodecs.STRING_UTF8.encode(buf, val.gender().name());
                ByteBufCodecs.VAR_INT.encode(buf, val.skinColorIndex());
                ByteBufCodecs.VAR_INT.encode(buf, val.faceVariant());
                ByteBufCodecs.VAR_INT.encode(buf, val.eyeColorIndex());
                ByteBufCodecs.VAR_INT.encode(buf, val.hairStyle());
                ByteBufCodecs.VAR_INT.encode(buf, val.hairColorIndex());
                ByteBufCodecs.VAR_INT.encode(buf, val.beardStyle());
            },
            buf -> new PlayerAppearanceData(
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    RaceType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf)),
                    Gender.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf)),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );
}