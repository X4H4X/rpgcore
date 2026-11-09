package com.rpgcore.data;

import java.util.List;

public enum RaceType {
    HUMAN(
            "Человек",
            20.0D,
            1.0F,
            1.0F,
            new int[]{
                    0xFFE2BE9E, // Европеец / Европейка
                    0xFFE8CA9E, // Азиат / Азиатка
                    0xFF593825, // Афроамериканец / Афроамериканка
                    0xFFC89369  // Араб / Арабка
            },
            new String[]{"Европеец", "Азиат", "Афроамериканец", "Араб"},
            new String[]{"Европейка", "Азиатка", "Афроамериканка", "Арабка"},
            new int[]{
                    0xFF5282A8, 0xFF6B7B8C, 0xFF3D7A4D, 0xFF593A22,
                    0xFF2B1A0E, 0xFFC2822B, 0xFF7A633A, 0xFF2B547E
            },
            new String[]{
                    "Голубой", "Серый", "Зелёный", "Карий",
                    "Тёмно-карий", "Янтарный", "Болотный", "Синий"
            },
            List.of(
                    "+ Дипломат: цены у жителей",
                    "-15%",
                    "",
                    "+ Обучаемость: опыт x2"
            ),
            List.of(
                    "- Обычность: нет",
                    "сопротивлений"
            )
    ),

    ELF(
            "Эльф",
            20.0D,
            1.07F,
            0.92F,
            new int[]{
                    0xFFF0D8C4,
                    0xFFDCE2EA,
                    0xFFDCC0A3,
                    0xFFC89B74
            },
            new String[]{"Высший", "Лунный", "Лесной", "Солнечный"},
            new String[]{"Высшая", "Лунная", "Лесная", "Солнечная"},
            new int[]{
                    0xFF2EB865, 0xFF38B2B8, 0xFF3366CC,
                    0xFF8A4BC2, 0xFFD99B26, 0xFFB8C8D8
            },
            new String[]{
                    "Изумрудный", "Бирюзовый", "Сапфировый",
                    "Фиалковый", "Золотистый", "Серебряный"
            },
            List.of(
                    "+ Лесной стрелок: урон",
                    "из луков +25%, скорость +10%",
                    "",
                    "+ Легкий шаг: бесшумен для",
                    "скалка, ходит по рыхлому снегу",
                    "и в кустах ягод без урона"
            ),
            List.of(
                    "- Хрупкость к взрывам: урон",
                    "от взрывов увеличен на 20%"
            )
    ),

    MUTANT(
            "Мутант",
            30.0D,
            1.03F,
            1.04F,
            new int[]{
                    0xFFC2B8B0,
                    0xFF9EA69E,
                    0xFF8A7D72,
                    0xFF6B636B
            },
            new String[]{"Бледный", "Пепельный", "Землистый", "Сумрачный"},
            new String[]{"Бледная", "Пепельная", "Землистая", "Сумрачная"},
            new int[]{
                    0xFFE5B822, 0xFFEB7A1C, 0xFF5CD638,
                    0xFFC92828, 0xFFD8E0E8, 0xFF9B42C8
            },
            new String[]{
                    "Жёлтый (хищный)", "Янтарный", "Токсичный",
                    "Багровый", "Призрачный", "Пурпурный"
            },
            List.of(
                    "+ Мутаген: иммунитет к яду",
                    "и голоду, урон вблизи +2",
                    "",
                    "+ Кошачий глаз: зрение во тьме"
            ),
            List.of(
                    "- Изгой: цены у жителей",
                    "+15%"
            )
    ),

    DWARF(
            "Дворф",
            40.0D,
            0.84F,
            1.12F,
            new int[]{
                    0xFFD8B090,
                    0xFFC4926E,
                    0xFF9E7B66,
                    0xFF6E5A4F
            },
            new String[]{"Горный", "Медный", "Каменный", "Глубинный"},
            new String[]{"Горная", "Медная", "Каменная", "Глубинная"},
            new int[]{
                    0xFF4B729F, 0xFF5C6B73, 0xFF4A7856,
                    0xFF63412C, 0xFFB87D28, 0xFF2E2218
            },
            new String[]{
                    "Стальной синий", "Гранитный", "Малахитовый",
                    "Янтарно-карий", "Золотой", "Угольный"
            },
            List.of(
                    "+ Крепкий хребет: доп. ряд",
                    "слотов в основном инвентаре",
                    "",
                    "+ Рудокоп: скорость раскопок",
                    "увеличена на +25%"
            ),
            List.of(
                    "- Короткие ноги: меньше",
                    "рост (0.84x)"
            )
    ),

    DRAGONBORN(
            "Драконорожденный",
            30.0D,
            1.08F,
            1.05F,
            new int[]{
                    0xFF9E2420, // Красный
                    0xFF24222B, // Черный (Обсидиановый)
                    0xFFF5E1E6, // Белый с оттенком розового (Жемчужно-розовый)
                    0xFF1B3664, // Темно-синий
                    0xFF1A472A, // Темно-зеленый
                    0xFFC89D3C  // Золотой
            },
            new String[]{"Красный", "Обсидиановый", "Бело-розовый", "Тёмно-синий", "Тёмно-зелёный", "Золотой"},
            new String[]{"Красная", "Обсидиановая", "Бело-розовая", "Тёмно-синяя", "Тёмно-зелёная", "Золотая"},
            new int[]{
                    0xFFFFB300, 0xFFFF5722, 0xFF00E676,
                    0xFF00B0FF, 0xFFE040FB, 0xFFFFF59D
            },
            new String[]{
                    "Огненное золото", "Пламенный", "Изумруд",
                    "Ледяной", "Аметист", "Сияющий"
            },
            List.of(
                    "+ Кровь дракона: полный",
                    "иммунитет к огню и лаве",
                    "",
                    "+ Драконья чешуя: броня +4,",
                    "урон в ближнем бою +1.5"
            ),
            List.of(
                    "- Хищник: наедается",
                    "нормально только мясом"
            )
    ),

    ORC(
            "Орк",
            40.0D,
            1.62F,
            1.38F,
            new int[]{
                    0xFF5D7A46,
                    0xFF475E38,
                    0xFF6E756A,
                    0xFF7A4E3B
            },
            new String[]{"Степной", "Болотный", "Пепельный", "Бурый"},
            new String[]{"Степная", "Болотная", "Пепельная", "Бурая"},
            new int[]{
                    0xFFD32F2F,
                    0xFFFF8F00,
                    0xFFFBC02D,
                    0xFF5D4037,
                    0xFFCFD8DC,
                    0xFF388E3C
            },
            new String[]{
                    "Кровавый", "Янтарный", "Жёлтый",
                    "Тёмно-бурый", "Стальной", "Дикий зелёный"
            },
            List.of(
                    "+ Ярость берсерка: урон",
                    "вблизи +3, стойкость +40%",
                    "",
                    "+ Клич войны: защита при",
                    "низком здоровье (<40%)"
            ),
            List.of(
                    "- Гигант: рост 3 блока",
                    "(не проходит в низкие двери)"
            )
    );

    private final String title;
    private final double maxHealth;
    private final float baseHeight;
    private final float baseWidth;
    private final int[] skinColors;
    private final String[] maleSkinNames;
    private final String[] femaleSkinNames;
    private final int[] eyeColors;
    private final String[] eyeColorNames;
    private final List<String> buffs;
    private final List<String> debuffs;

    RaceType(String title, double maxHealth, float baseHeight, float baseWidth,
             int[] skinColors, String[] maleSkinNames, String[] femaleSkinNames,
             int[] eyeColors, String[] eyeColorNames,
             List<String> buffs, List<String> debuffs) {
        this.title = title;
        this.maxHealth = maxHealth;
        this.baseHeight = baseHeight;
        this.baseWidth = baseWidth;
        this.skinColors = skinColors;
        this.maleSkinNames = maleSkinNames;
        this.femaleSkinNames = femaleSkinNames;
        this.eyeColors = eyeColors;
        this.eyeColorNames = eyeColorNames;
        this.buffs = buffs;
        this.debuffs = debuffs;
    }

    public String getTitle() { return title; }
    public double getMaxHealth() { return maxHealth; }
    public float getBaseHeight() { return baseHeight; }
    public float getBaseWidth() { return baseWidth; }
    public int[] getSkinColors() { return skinColors; }

    public String getSkinName(Gender gender, int index) {
        String[] arr = (gender == Gender.FEMALE) ? femaleSkinNames : maleSkinNames;
        return arr[Math.floorMod(index, arr.length)];
    }

    public int[] getEyeColors() { return eyeColors; }
    public String getEyeColorName(int index) {
        return eyeColorNames[Math.floorMod(index, eyeColorNames.length)];
    }

    public List<String> getBuffs() { return buffs; }
    public List<String> getDebuffs() { return debuffs; }
}