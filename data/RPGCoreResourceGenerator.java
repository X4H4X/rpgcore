package com.rpgcore.data;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RPGCoreResourceGenerator {

    private record DoorDef(String id, String ruName, String vanillaDoor, int mainCol, int darkCol, int lightCol) {}

    private record GiantBedDef(String id, String vanillaColor, String ruName, int woolRgb, int darkRgb) {}

    private static final List<DoorDef> DOORS = List.of(
            new DoorDef("colossal_oak_door", "Огромная дубовая дверь (3x4)", "minecraft:oak_door", 0xFFA5844E, 0xFF6B512B, 0xFFBD9B61),
            new DoorDef("colossal_spruce_door", "Огромная еловая дверь (3x4)", "minecraft:spruce_door", 0xFF6E5233, 0xFF45301B, 0xFF856540),
            new DoorDef("colossal_birch_door", "Огромная берёзовая дверь (3x4)", "minecraft:birch_door", 0xFFD7C896, 0xFFA69665, 0xFFE8DCA8),
            new DoorDef("colossal_jungle_door", "Огромная дверь из тропического дерева (3x4)", "minecraft:jungle_door", 0xFFA87854, 0xFF734D30, 0xFFBF8D67),
            new DoorDef("colossal_acacia_door", "Огромная акациевая дверь (3x4)", "minecraft:acacia_door", 0xFFB86236, 0xFF803E1B, 0xFFCF7446),
            new DoorDef("colossal_dark_oak_door", "Огромная дверь из тёмного дуба (3x4)", "minecraft:dark_oak_door", 0xFF472E1B, 0xFF29190C, 0xFF5C3D25),
            new DoorDef("colossal_mangrove_door", "Огромная мангровая дверь (3x4)", "minecraft:mangrove_door", 0xFF7D362E, 0xFF521E18, 0xFF94443B),
            new DoorDef("colossal_cherry_door", "Огромная вишнёвая дверь (3x4)", "minecraft:cherry_door", 0xFFE2B2AD, 0xFFB37E79, 0xFFF2C6C2),
            new DoorDef("colossal_bamboo_door", "Огромная бамбуковая дверь (3x4)", "minecraft:bamboo_door", 0xFFC9B356, 0xFF968232, 0xFFE0CB6C),
            new DoorDef("colossal_crimson_door", "Огромная багровая дверь (3x4)", "minecraft:crimson_door", 0xFF75314C, 0xFF4A1B2E, 0xFF8F3F5F),
            new DoorDef("colossal_warped_door", "Огромная искажённая дверь (3x4)", "minecraft:warped_door", 0xFF2B7A78, 0xFF184C4A, 0xFF3B9693)
    );

    private static final List<GiantBedDef> GIANT_BEDS = List.of(
            new GiantBedDef("orc_white_bed", "white", "Большая белая кровать (3x1)", 0xFFE9ECEC, 0xFFB8BFC2),
            new GiantBedDef("orc_light_gray_bed", "light_gray", "Большая светло-серая кровать (3x1)", 0xFF8E8E86, 0xFF666660),
            new GiantBedDef("orc_gray_bed", "gray", "Большая серая кровать (3x1)", 0xFF3E4447, 0xFF262A2C),
            new GiantBedDef("orc_black_bed", "black", "Большая чёрная кровать (3x1)", 0xFF19191D, 0xFF0D0D10),
            new GiantBedDef("orc_brown_bed", "brown", "Большая коричневая кровать (3x1)", 0xFF724728, 0xFF4F301A),
            new GiantBedDef("orc_red_bed", "red", "Большая красная кровать (3x1)", 0xFFA12722, 0xFF731915),
            new GiantBedDef("orc_orange_bed", "orange", "Большая оранжевая кровать (3x1)", 0xFFF07613, 0xFFB5530A),
            new GiantBedDef("orc_yellow_bed", "yellow", "Большая жёлтая кровать (3x1)", 0xFFF8C627, 0xFFC49818),
            new GiantBedDef("orc_lime_bed", "lime", "Большая лаймовая кровать (3x1)", 0xFF70B919, 0xFF4E8510),
            new GiantBedDef("orc_green_bed", "green", "Большая зелёная кровать (3x1)", 0xFF546D1B, 0xFF3A4C11),
            new GiantBedDef("orc_cyan_bed", "cyan", "Большая бирюзовая кровать (3x1)", 0xFF158991, 0xFF0D5E63),
            new GiantBedDef("orc_light_blue_bed", "light_blue", "Большая голубая кровать (3x1)", 0xFF3AAFD9, 0xFF2482A6),
            new GiantBedDef("orc_blue_bed", "blue", "Большая синяя кровать (3x1)", 0xFF35399D, 0xFF22256E),
            new GiantBedDef("orc_purple_bed", "purple", "Большая фиолетовая кровать (3x1)", 0xFF792AAC, 0xFF531B78),
            new GiantBedDef("orc_magenta_bed", "magenta", "Большая пурпурная кровать (3x1)", 0xFFBD44B3, 0xFF8A2B82),
            new GiantBedDef("orc_pink_bed", "pink", "Большая розовая кровать (3x1)", 0xFFED8DAC, 0xFFB8627E)
    );

    public static void ensureResourcesGenerated() {
        try {
            File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
            File projectRoot = cwd.getName().equalsIgnoreCase("run") ? cwd.getParentFile() : cwd;
            if (projectRoot == null) return;

            patchEarlyLoadingColor(new File(cwd, "config/fml.toml"));
            patchEarlyLoadingColor(new File(projectRoot, "run/config/fml.toml"));

            patchOptionsTxtDarkBackground(new File(cwd, "options.txt"));
            patchOptionsTxtDarkBackground(new File(projectRoot, "run/options.txt"));

            List<Path> targets = new ArrayList<>();
            targets.add(projectRoot.toPath().resolve("src/main/resources"));
            Path buildRes = projectRoot.toPath().resolve("build/resources/main");
            if (Files.exists(buildRes.getParent())) {
                targets.add(buildRes);
            }

            for (Path root : targets) {
                generateInto(root);
            }
        } catch (Exception ignored) {
        }
    }

    private static void patchOptionsTxtDarkBackground(File optionsFile) {
        try {
            if (optionsFile.getParentFile() != null) {
                optionsFile.getParentFile().mkdirs();
            }
            if (optionsFile.exists()) {
                String content = Files.readString(optionsFile.toPath(), StandardCharsets.UTF_8);
                if (content.contains("darkMojangStudiosBackground:false")) {
                    content = content.replace("darkMojangStudiosBackground:false", "darkMojangStudiosBackground:true");
                    Files.writeString(optionsFile.toPath(), content, StandardCharsets.UTF_8);
                } else if (!content.contains("darkMojangStudiosBackground:")) {
                    content = content + "\ndarkMojangStudiosBackground:true\n";
                    Files.writeString(optionsFile.toPath(), content, StandardCharsets.UTF_8);
                }
            } else {
                Files.writeString(optionsFile.toPath(), "darkMojangStudiosBackground:true\n", StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    private static void patchEarlyLoadingColor(File fmlToml) {
        try {
            if (fmlToml.getParentFile() != null) {
                fmlToml.getParentFile().mkdirs();
            }
            String defaultToml = """
                    earlyWindowControl = true
                    earlyWindowProvider = "fmlearlywindow"
                    earlyWindowWidth = 854
                    earlyWindowHeight = 480
                    earlyWindowMaximized = false
                    earlyWindowSkipGLVersions = []
                    earlyWindowSquir = false
                    earlyWindowShowCPU = false
                    """;
            Files.writeString(fmlToml.toPath(), defaultToml, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static void generateInto(Path resRoot) throws Exception {
        Path assets = resRoot.resolve("assets/rpgcore");
        Path data = resRoot.resolve("data/rpgcore");

        Files.createDirectories(assets.resolve("lang"));
        Files.createDirectories(assets.resolve("blockstates"));
        Files.createDirectories(assets.resolve("models/block"));
        Files.createDirectories(assets.resolve("models/item"));
        Files.createDirectories(assets.resolve("textures/item"));
        Files.createDirectories(assets.resolve("textures/block"));
        Files.createDirectories(data.resolve("recipe"));

        StringBuilder ru = new StringBuilder("{\n");
        StringBuilder en = new StringBuilder("{\n");
        ru.append("  \"itemGroup.rpgcore\": \"RPG Core: Одежда, Рюкзаки и Блоки\",\n");
        ru.append("  \"item.rpgcore.straw_hat\": \"Соломенная шляпа крестьянина\",\n");
        ru.append("  \"item.rpgcore.peasant_shirt\": \"Летняя рубаха крестьянина\",\n");
        ru.append("  \"item.rpgcore.peasant_pants\": \"Летние портки крестьянина\",\n");
        ru.append("  \"item.rpgcore.peasant_shoes\": \"Летние башмаки крестьянина\",\n");
        ru.append("  \"item.rpgcore.hooded_cloak\": \"Походный плащ с капюшоном\",\n");
        ru.append("  \"item.rpgcore.leather_backpack\": \"Кожаный походный рюкзак\",\n");
        ru.append("  \"item.rpgcore.iron_backpack\": \"Укреплённый железный рюкзак\",\n");
        ru.append("  \"item.rpgcore.gold_backpack\": \"Позолоченный походный рюкзак\",\n");
        ru.append("  \"item.rpgcore.diamond_backpack\": \"Алмазный походный рюкзак\",\n");
        ru.append("  \"item.rpgcore.netherite_backpack\": \"Незеритовый походный рюкзак\",\n");
        ru.append("  \"block.rpgcore.dirt_slab\": \"Полублок земли\",\n");
        ru.append("  \"block.rpgcore.grass_slab\": \"Полублок дёрна\",\n");
        ru.append("  \"block.rpgcore.farmland_slab\": \"Вспаханный полублок пашни\",\n");

        en.append("  \"itemGroup.rpgcore\": \"RPG Core: Apparel, Backpacks & Blocks\",\n");
        en.append("  \"item.rpgcore.straw_hat\": \"Peasant Straw Hat\",\n");
        en.append("  \"item.rpgcore.peasant_shirt\": \"Peasant Summer Shirt\",\n");
        en.append("  \"item.rpgcore.peasant_pants\": \"Peasant Summer Pants\",\n");
        en.append("  \"item.rpgcore.peasant_shoes\": \"Peasant Summer Shoes\",\n");
        en.append("  \"item.rpgcore.hooded_cloak\": \"Traveler's Hooded Cloak\",\n");
        en.append("  \"item.rpgcore.leather_backpack\": \"Leather Backpack\",\n");
        en.append("  \"item.rpgcore.iron_backpack\": \"Iron Reinforced Backpack\",\n");
        en.append("  \"item.rpgcore.gold_backpack\": \"Golden Backpack\",\n");
        en.append("  \"item.rpgcore.diamond_backpack\": \"Diamond Backpack\",\n");
        en.append("  \"item.rpgcore.netherite_backpack\": \"Netherite Backpack\",\n");
        en.append("  \"block.rpgcore.dirt_slab\": \"Dirt Slab\",\n");
        en.append("  \"block.rpgcore.grass_slab\": \"Grass Slab\",\n");
        en.append("  \"block.rpgcore.farmland_slab\": \"Farmland Slab\",\n");

        for (GiantBedDef b : GIANT_BEDS) {
            ru.append("  \"block.rpgcore.").append(b.id).append("\": \"").append(b.ruName).append("\",\n");
            en.append("  \"block.rpgcore.").append(b.id).append("\": \"").append(b.ruName).append("\",\n");
        }

        for (int i = 0; i < DOORS.size(); i++) {
            DoorDef d = DOORS.get(i);
            String comma = (i == DOORS.size() - 1) ? "\n" : ",\n";
            ru.append("  \"block.rpgcore.").append(d.id).append("\": \"").append(d.ruName).append("\"").append(comma);
            en.append("  \"block.rpgcore.").append(d.id).append("\": \"").append(d.ruName).append("\"").append(comma);
        }
        ru.append("}\n");
        en.append("}\n");
        writeString(assets.resolve("lang/ru_ru.json"), ru.toString());
        writeString(assets.resolve("lang/en_us.json"), en.toString());

        String[] clothesIds = {"straw_hat", "peasant_shirt", "peasant_pants", "peasant_shoes", "hooded_cloak"};
        for (String cid : clothesIds) {
            writeString(assets.resolve("models/item/" + cid + ".json"),
                    "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"rpgcore:item/" + cid + "\"\n  }\n}\n");
            drawClothingIcon(assets.resolve("textures/item/" + cid + ".png"), cid);
        }

        String[] backpackIds = {"leather_backpack", "iron_backpack", "gold_backpack", "diamond_backpack", "netherite_backpack"};
        BackpackItem.Tier[] tiers = BackpackItem.Tier.values();
        for (int i = 0; i < backpackIds.length; i++) {
            String bid = backpackIds[i];
            writeString(assets.resolve("models/item/" + bid + ".json"),
                    "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"rpgcore:item/" + bid + "\"\n  }\n}\n");
            drawBackpackIcon(assets.resolve("textures/item/" + bid + ".png"), tiers[i]);
        }

        writeString(data.resolve("recipe/straw_hat.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    " W ",
                    "WWW"
                  ],
                  "key": {
                    "W": { "item": "minecraft:wheat" }
                  },
                  "result": {
                    "id": "rpgcore:straw_hat",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/peasant_shirt.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "P P",
                    "PLP",
                    "PLP"
                  ],
                  "key": {
                    "P": { "item": "minecraft:paper" },
                    "L": { "item": "minecraft:leather" }
                  },
                  "result": {
                    "id": "rpgcore:peasant_shirt",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/peasant_pants.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "PLP",
                    "P P",
                    "P P"
                  ],
                  "key": {
                    "P": { "item": "minecraft:paper" },
                    "L": { "item": "minecraft:leather" }
                  },
                  "result": {
                    "id": "rpgcore:peasant_pants",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/peasant_shoes.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "P P",
                    "L L"
                  ],
                  "key": {
                    "P": { "item": "minecraft:paper" },
                    "L": { "item": "minecraft:leather" }
                  },
                  "result": {
                    "id": "rpgcore:peasant_shoes",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/hooded_cloak.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "L L",
                    "LLL",
                    " L "
                  ],
                  "key": {
                    "L": { "item": "minecraft:leather" }
                  },
                  "result": {
                    "id": "rpgcore:hooded_cloak",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/leather_backpack.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "L L",
                    "LSL",
                    "SSS"
                  ],
                  "key": {
                    "L": { "item": "minecraft:leather" },
                    "S": { "item": "minecraft:string" }
                  },
                  "result": {
                    "id": "rpgcore:leather_backpack",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/iron_backpack.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "III",
                    "IBI",
                    "III"
                  ],
                  "key": {
                    "I": { "item": "minecraft:iron_ingot" },
                    "B": { "item": "rpgcore:leather_backpack" }
                  },
                  "result": {
                    "id": "rpgcore:iron_backpack",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/gold_backpack.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "GGG",
                    "GBG",
                    "GGG"
                  ],
                  "key": {
                    "G": { "item": "minecraft:gold_ingot" },
                    "B": { "item": "rpgcore:iron_backpack" }
                  },
                  "result": {
                    "id": "rpgcore:gold_backpack",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/diamond_backpack.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "DDD",
                    "DBD",
                    "DDD"
                  ],
                  "key": {
                    "D": { "item": "minecraft:diamond" },
                    "B": { "item": "rpgcore:gold_backpack" }
                  },
                  "result": {
                    "id": "rpgcore:diamond_backpack",
                    "count": 1
                  }
                }
                """);

        writeString(data.resolve("recipe/netherite_backpack.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "equipment",
                  "pattern": [
                    "NNN",
                    "NBN",
                    "NNN"
                  ],
                  "key": {
                    "N": { "item": "minecraft:netherite_ingot" },
                    "B": { "item": "rpgcore:diamond_backpack" }
                  },
                  "result": {
                    "id": "rpgcore:netherite_backpack",
                    "count": 1
                  }
                }
                """);

        generateSlabsModelsAndRecipes(assets, data);
        generateGiantOrcBeds(assets, data);

        String[] partSuffixes = {"_bottom", "_mid", "_handle", "_window", "_top"};
        for (DoorDef d : DOORS) {
            drawHighResDoorSection(assets.resolve("textures/block/" + d.id + "_bottom.png"), d, 0);
            drawHighResDoorSection(assets.resolve("textures/block/" + d.id + "_mid.png"), d, 1);
            drawHighResDoorSection(assets.resolve("textures/block/" + d.id + "_handle.png"), d, 2);
            drawHighResDoorSection(assets.resolve("textures/block/" + d.id + "_window.png"), d, 3);
            drawHighResDoorSection(assets.resolve("textures/block/" + d.id + "_top.png"), d, 4);
            drawDoorItemIcon(assets.resolve("textures/item/" + d.id + ".png"), d);

            writeString(assets.resolve("models/item/" + d.id + ".json"),
                    "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"rpgcore:item/" + d.id + "\"\n  }\n}\n");

            for (String suf : partSuffixes) {
                String texName = d.id + suf;
                writeString(assets.resolve("models/block/" + texName + "_closed.json"), """
                        {
                          "render_type": "minecraft:cutout",
                          "textures": {
                            "particle": "rpgcore:block/%1$s",
                            "tex": "rpgcore:block/%1$s"
                          },
                          "elements": [
                            {
                              "from": [0, 0, 13],
                              "to": [16, 16, 16],
                              "faces": {
                                "north": {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                "south": {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                "west":  {"uv": [0, 0, 3, 16], "texture": "#tex"},
                                "east":  {"uv": [13, 0, 16, 16], "texture": "#tex"},
                                "up":    {"uv": [0, 0, 16, 3], "texture": "#tex"},
                                "down":  {"uv": [0, 13, 16, 16], "texture": "#tex"}
                              }
                            }
                          ]
                        }
                        """.formatted(texName));

                writeString(assets.resolve("models/block/" + texName + "_open_left.json"), """
                        {
                          "render_type": "minecraft:cutout",
                          "textures": {
                            "particle": "rpgcore:block/%1$s",
                            "tex": "rpgcore:block/%1$s"
                          },
                          "elements": [
                            {
                              "from": [0, 0, 0],
                              "to": [3, 16, 16],
                              "faces": {
                                "north": {"uv": [0, 0, 3, 16], "texture": "#tex"},
                                "south": {"uv": [13, 0, 16, 16], "texture": "#tex"},
                                "west":  {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                "east":  {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                "up":    {"uv": [0, 0, 3, 16], "texture": "#tex"},
                                "down":  {"uv": [0, 0, 3, 16], "texture": "#tex"}
                              }
                            }
                          ]
                        }
                        """.formatted(texName));

                writeString(assets.resolve("models/block/" + texName + "_open_right.json"), """
                        {
                          "render_type": "minecraft:cutout",
                          "textures": {
                            "particle": "rpgcore:block/%1$s",
                            "tex": "rpgcore:block/%1$s"
                          },
                          "elements": [
                            {
                              "from": [13, 0, 0],
                              "to": [16, 16, 16],
                              "faces": {
                                "north": {"uv": [13, 0, 16, 16], "texture": "#tex"},
                                "south": {"uv": [0, 0, 3, 16], "texture": "#tex"},
                                "west":  {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                "east":  {"uv": [0, 0, 16, 16], "texture": "#tex"},
                                "up":    {"uv": [13, 0, 16, 16], "texture": "#tex"},
                                "down":  {"uv": [13, 0, 16, 16], "texture": "#tex"}
                              }
                            }
                          ]
                        }
                        """.formatted(texName));
            }

            StringBuilder bs = new StringBuilder("{\n  \"variants\": {\n");
            String[] facings = {"north", "east", "south", "west"};
            int[] yRots = {0, 90, 180, 270};
            boolean first = true;

            for (int fi = 0; fi < 4; fi++) {
                for (boolean open : new boolean[]{false, true}) {
                    for (String hinge : new String[]{"left", "right"}) {
                        for (int ly = 0; ly <= 3; ly++) {
                            for (int cx = 0; cx <= 2; cx++) {
                                String suf;
                                if (ly == 0) {
                                    suf = "_bottom";
                                } else if (ly == 1) {
                                    suf = (cx == 2) ? "_handle" : "_mid";
                                } else if (ly == 2) {
                                    suf = "_window";
                                } else {
                                    suf = "_top";
                                }

                                String stateModel = d.id + suf + (open ? ("_open_" + hinge) : "_closed");
                                if (!first) bs.append(",\n");
                                first = false;
                                bs.append("    \"col_x=").append(cx)
                                        .append(",facing=").append(facings[fi])
                                        .append(",hinge=").append(hinge)
                                        .append(",layer_y=").append(ly)
                                        .append(",open=").append(open)
                                        .append("\": { \"model\": \"rpgcore:block/").append(stateModel).append("\"");
                                if (yRots[fi] != 0) bs.append(", \"y\": ").append(yRots[fi]);
                                bs.append(" }");
                            }
                        }
                    }
                }
            }
            bs.append("\n  }\n}\n");
            writeString(assets.resolve("blockstates/" + d.id + ".json"), bs.toString());

            writeString(data.resolve("recipe/" + d.id + ".json"), """
                    {
                      "type": "minecraft:crafting_shaped",
                      "category": "building",
                      "pattern": [
                        "DD",
                        "DD",
                        "DD"
                      ],
                      "key": {
                        "D": { "item": "%s" }
                      },
                      "result": {
                        "id": "rpgcore:%s",
                        "count": 1
                      }
                    }
                    """.formatted(d.vanillaDoor, d.id));
        }

        String[] logSides = {
                "minecraft:block/stripped_oak_log", "minecraft:block/stripped_spruce_log",
                "minecraft:block/stripped_birch_log", "minecraft:block/stripped_jungle_log",
                "minecraft:block/stripped_acacia_log", "minecraft:block/stripped_dark_oak_log",
                "minecraft:block/stripped_mangrove_log", "minecraft:block/stripped_cherry_log",
                "minecraft:block/stripped_crimson_stem", "minecraft:block/stripped_warped_stem"
        };
        String[] logTops = {
                "minecraft:block/oak_log_top", "minecraft:block/spruce_log_top",
                "minecraft:block/birch_log_top", "minecraft:block/jungle_log_top",
                "minecraft:block/acacia_log_top", "minecraft:block/dark_oak_log_top",
                "minecraft:block/mangrove_log_top", "minecraft:block/cherry_log_top",
                "minecraft:block/crimson_stem_top", "minecraft:block/warped_stem_top"
        };

        StringBuilder choppedBs = new StringBuilder("{\n  \"variants\": {\n");
        boolean firstChop = true;
        for (int wt = 0; wt < 10; wt++) {
            for (int c = 1; c <= 7; c++) {
                int min = c;
                int max = 16 - c;
                String modelName = "chopped_log_w" + wt + "_c" + c;
                writeString(assets.resolve("models/block/" + modelName + ".json"), """
                        {
                          "textures": {
                            "particle": "%1$s",
                            "side": "%1$s",
                            "top": "%2$s"
                          },
                          "elements": [
                            {
                              "from": [%3$d, 0, %3$d],
                              "to": [%4$d, 16, %4$d],
                              "faces": {
                                "north": {"uv": [%3$d, 0, %4$d, 16], "texture": "#side"},
                                "south": {"uv": [%3$d, 0, %4$d, 16], "texture": "#side"},
                                "west":  {"uv": [%3$d, 0, %4$d, 16], "texture": "#side"},
                                "east":  {"uv": [%3$d, 0, %4$d, 16], "texture": "#side"},
                                "up":    {"uv": [%3$d, %3$d, %4$d, %4$d], "texture": "#top"},
                                "down":  {"uv": [%3$d, %3$d, %4$d, %4$d], "texture": "#top"}
                              }
                            }
                          ]
                        }
                        """.formatted(logSides[wt], logTops[wt], min, max));

                if (!firstChop) choppedBs.append(",\n");
                firstChop = false;
                choppedBs.append("    \"chops=").append(c).append(",wood_type=").append(wt)
                        .append("\": { \"model\": \"rpgcore:block/").append(modelName).append("\" }");
            }
        }
        choppedBs.append("\n  }\n}\n");
        writeString(assets.resolve("blockstates/chopped_log.json"), choppedBs.toString());
    }

    private static void generateSlabsModelsAndRecipes(Path assets, Path data) throws Exception {
        writeString(assets.resolve("models/block/dirt_slab.json"), """
                {
                  "parent": "minecraft:block/slab",
                  "textures": {
                    "bottom": "minecraft:block/dirt",
                    "top": "minecraft:block/dirt",
                    "side": "minecraft:block/dirt"
                  }
                }
                """);
        writeString(assets.resolve("models/block/dirt_slab_top.json"), """
                {
                  "parent": "minecraft:block/slab_top",
                  "textures": {
                    "bottom": "minecraft:block/dirt",
                    "top": "minecraft:block/dirt",
                    "side": "minecraft:block/dirt"
                  }
                }
                """);
        writeString(assets.resolve("blockstates/dirt_slab.json"), """
                {
                  "variants": {
                    "type=bottom": { "model": "rpgcore:block/dirt_slab" },
                    "type=top": { "model": "rpgcore:block/dirt_slab_top" },
                    "type=double": { "model": "minecraft:block/dirt" }
                  }
                }
                """);
        writeString(assets.resolve("models/item/dirt_slab.json"), "{\n  \"parent\": \"rpgcore:block/dirt_slab\"\n}\n");

        writeString(data.resolve("recipe/dirt_slab.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "building",
                  "pattern": [
                    "DDD"
                  ],
                  "key": {
                    "D": { "item": "minecraft:dirt" }
                  },
                  "result": {
                    "id": "rpgcore:dirt_slab",
                    "count": 6
                  }
                }
                """);

        writeString(assets.resolve("models/block/grass_slab.json"), """
                {
                  "render_type": "minecraft:cutout_mipped",
                  "textures": {
                    "particle": "minecraft:block/dirt",
                    "bottom": "minecraft:block/dirt",
                    "top": "minecraft:block/grass_block_top",
                    "side": "minecraft:block/grass_block_side",
                    "overlay": "minecraft:block/grass_block_side_overlay"
                  },
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [16, 8, 16],
                      "faces": {
                        "down":  {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
                        "up":    {"uv": [0, 0, 16, 16], "texture": "#top", "tintindex": 0},
                        "north": {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "north"},
                        "south": {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "south"},
                        "west":  {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "west"},
                        "east":  {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "east"}
                      }
                    },
                    {
                      "from": [0, 0, 0],
                      "to": [16, 8, 16],
                      "faces": {
                        "north": {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "north"},
                        "south": {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "south"},
                        "west":  {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "west"},
                        "east":  {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "east"}
                      }
                    }
                  ]
                }
                """);
        writeString(assets.resolve("models/block/grass_slab_top.json"), """
                {
                  "render_type": "minecraft:cutout_mipped",
                  "textures": {
                    "particle": "minecraft:block/dirt",
                    "bottom": "minecraft:block/dirt",
                    "top": "minecraft:block/grass_block_top",
                    "side": "minecraft:block/grass_block_side",
                    "overlay": "minecraft:block/grass_block_side_overlay"
                  },
                  "elements": [
                    {
                      "from": [0, 8, 0],
                      "to": [16, 16, 16],
                      "faces": {
                        "down":  {"uv": [0, 0, 16, 16], "texture": "#bottom"},
                        "up":    {"uv": [0, 0, 16, 16], "texture": "#top", "tintindex": 0, "cullface": "up"},
                        "north": {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "north"},
                        "south": {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "south"},
                        "west":  {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "west"},
                        "east":  {"uv": [0, 0, 16, 8], "texture": "#side", "cullface": "east"}
                      }
                    },
                    {
                      "from": [0, 8, 0],
                      "to": [16, 16, 16],
                      "faces": {
                        "north": {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "north"},
                        "south": {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "south"},
                        "west":  {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "west"},
                        "east":  {"uv": [0, 0, 16, 8], "texture": "#overlay", "tintindex": 0, "cullface": "east"}
                      }
                    }
                  ]
                }
                """);
        writeString(assets.resolve("blockstates/grass_slab.json"), """
                {
                  "variants": {
                    "type=bottom": { "model": "rpgcore:block/grass_slab" },
                    "type=top": { "model": "rpgcore:block/grass_slab_top" },
                    "type=double": { "model": "minecraft:block/grass_block" }
                  }
                }
                """);
        writeString(assets.resolve("models/item/grass_slab.json"), "{\n  \"parent\": \"rpgcore:block/grass_slab\"\n}\n");

        writeString(data.resolve("recipe/grass_slab.json"), """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "building",
                  "pattern": [
                    "GGG"
                  ],
                  "key": {
                    "G": { "item": "minecraft:grass_block" }
                  },
                  "result": {
                    "id": "rpgcore:grass_slab",
                    "count": 6
                  }
                }
                """);

        for (boolean moist : new boolean[]{false, true}) {
            String suf = moist ? "_moist" : "";
            String topTex = moist ? "minecraft:block/farmland_moist" : "minecraft:block/farmland";
            writeString(assets.resolve("models/block/farmland_slab" + suf + ".json"), """
                    {
                      "textures": {
                        "particle": "minecraft:block/dirt",
                        "bottom": "minecraft:block/dirt",
                        "top": "%s",
                        "side": "minecraft:block/dirt"
                      },
                      "elements": [
                        {
                          "from": [0, 0, 0],
                          "to": [16, 7, 16],
                          "faces": {
                            "down":  {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
                            "up":    {"uv": [0, 0, 16, 16], "texture": "#top"},
                            "north": {"uv": [0, 9, 16, 16], "texture": "#side", "cullface": "north"},
                            "south": {"uv": [0, 9, 16, 16], "texture": "#side", "cullface": "south"},
                            "west":  {"uv": [0, 9, 16, 16], "texture": "#side", "cullface": "west"},
                            "east":  {"uv": [0, 9, 16, 16], "texture": "#side", "cullface": "east"}
                          }
                        }
                      ]
                    }
                    """.formatted(topTex));

            writeString(assets.resolve("models/block/farmland_slab_top" + suf + ".json"), """
                    {
                      "textures": {
                        "particle": "minecraft:block/dirt",
                        "bottom": "minecraft:block/dirt",
                        "top": "%s",
                        "side": "minecraft:block/dirt"
                      },
                      "elements": [
                        {
                          "from": [0, 8, 0],
                          "to": [16, 15, 16],
                          "faces": {
                            "down":  {"uv": [0, 0, 16, 16], "texture": "#bottom"},
                            "up":    {"uv": [0, 0, 16, 16], "texture": "#top"},
                            "north": {"uv": [0, 1, 16, 8], "texture": "#side", "cullface": "north"},
                            "south": {"uv": [0, 1, 16, 8], "texture": "#side", "cullface": "south"},
                            "west":  {"uv": [0, 1, 16, 8], "texture": "#side", "cullface": "west"},
                            "east":  {"uv": [0, 1, 16, 8], "texture": "#side", "cullface": "east"}
                          }
                        }
                      ]
                    }
                    """.formatted(topTex));
        }

        StringBuilder farmBs = new StringBuilder("{\n  \"variants\": {\n");
        boolean firstFarm = true;
        for (int m = 0; m <= 7; m++) {
            String suf = (m == 7) ? "_moist" : "";
            String doubleModel = (m == 7) ? "minecraft:block/farmland_moist" : "minecraft:block/farmland";
            for (String type : new String[]{"bottom", "top", "double"}) {
                String model = switch (type) {
                    case "bottom" -> "rpgcore:block/farmland_slab" + suf;
                    case "top" -> "rpgcore:block/farmland_slab_top" + suf;
                    default -> doubleModel;
                };
                if (!firstFarm) farmBs.append(",\n");
                firstFarm = false;
                farmBs.append("    \"moisture=").append(m).append(",type=").append(type)
                        .append("\": { \"model\": \"").append(model).append("\" }");
            }
        }
        farmBs.append("\n  }\n}\n");
        writeString(assets.resolve("blockstates/farmland_slab.json"), farmBs.toString());
        writeString(assets.resolve("models/item/farmland_slab.json"), "{\n  \"parent\": \"rpgcore:block/farmland_slab\"\n}\n");
    }

    /**
     * Генерирует чистые 3D-модели 3 секций кровати без наложения полигонов (деревянное основание 3..5, матрас 5..9)
     * И настоящую ванильную 3D-иконку кровати в изометрии для инвентаря, хотбара и JEI!
     */
    private static void generateGiantOrcBeds(Path assets, Path data) throws Exception {
        for (GiantBedDef b : GIANT_BEDS) {
            String woolTex = "minecraft:block/" + b.vanillaColor + "_wool";

            // 1. Модель изножья (FOOT): деревянная база (Y: 3..5), матрас (Y: 5..9) и две передние ножки (Y: 0..3)
            writeString(assets.resolve("models/block/" + b.id + "_foot.json"), """
                    {
                      "textures": {
                        "particle": "%1$s",
                        "wool": "%1$s",
                        "wood": "minecraft:block/oak_planks"
                      },
                      "elements": [
                        {
                          "from": [0, 3, 0],
                          "to": [16, 5, 16],
                          "faces": {
                            "down":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                            "north": {"uv": [0, 11, 16, 13], "texture": "#wood"},
                            "west":  {"uv": [0, 11, 16, 13], "texture": "#wood"},
                            "east":  {"uv": [0, 11, 16, 13], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [0, 5, 0],
                          "to": [16, 9, 16],
                          "faces": {
                            "up":    {"uv": [0, 0, 16, 16], "texture": "#wool"},
                            "north": {"uv": [0, 7, 16, 11], "texture": "#wool"},
                            "west":  {"uv": [0, 7, 16, 11], "texture": "#wool"},
                            "east":  {"uv": [0, 7, 16, 11], "texture": "#wool"}
                          }
                        },
                        {
                          "from": [0, 0, 0],
                          "to": [3, 3, 3],
                          "faces": {
                            "down":  {"uv": [0, 0, 3, 3], "texture": "#wood"},
                            "north": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "south": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "west":  {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "east":  {"uv": [0, 13, 3, 16], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [13, 0, 0],
                          "to": [16, 3, 3],
                          "faces": {
                            "down":  {"uv": [13, 0, 16, 3], "texture": "#wood"},
                            "north": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "south": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "west":  {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "east":  {"uv": [13, 13, 16, 16], "texture": "#wood"}
                          }
                        }
                      ]
                    }
                    """.formatted(woolTex));

            // 2. Модель центральной секции (MIDDLE): деревянная база (Y: 3..5) и матрас (Y: 5..9), без внутренних торцов!
            writeString(assets.resolve("models/block/" + b.id + "_middle.json"), """
                    {
                      "textures": {
                        "particle": "%1$s",
                        "wool": "%1$s",
                        "wood": "minecraft:block/oak_planks"
                      },
                      "elements": [
                        {
                          "from": [0, 3, 0],
                          "to": [16, 5, 16],
                          "faces": {
                            "down": {"uv": [0, 0, 16, 16], "texture": "#wood"},
                            "west": {"uv": [0, 11, 16, 13], "texture": "#wood"},
                            "east": {"uv": [0, 11, 16, 13], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [0, 5, 0],
                          "to": [16, 9, 16],
                          "faces": {
                            "up":   {"uv": [0, 0, 16, 16], "texture": "#wool"},
                            "west": {"uv": [0, 7, 16, 11], "texture": "#wool"},
                            "east": {"uv": [0, 7, 16, 11], "texture": "#wool"}
                          }
                        }
                      ]
                    }
                    """.formatted(woolTex));

            // 3. Модель изголовья (HEAD): деревянная база, матрас (Z: 0..10), белая простыня и подушка (Z: 10..16), задние ножки
            writeString(assets.resolve("models/block/" + b.id + "_head.json"), """
                    {
                      "textures": {
                        "particle": "%1$s",
                        "wool": "%1$s",
                        "pillow": "minecraft:block/white_wool",
                        "wood": "minecraft:block/oak_planks"
                      },
                      "elements": [
                        {
                          "from": [0, 3, 0],
                          "to": [16, 5, 16],
                          "faces": {
                            "down":  {"uv": [0, 0, 16, 16], "texture": "#wood"},
                            "south": {"uv": [0, 11, 16, 13], "texture": "#wood"},
                            "west":  {"uv": [0, 11, 16, 13], "texture": "#wood"},
                            "east":  {"uv": [0, 11, 16, 13], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [0, 5, 0],
                          "to": [16, 9, 10],
                          "faces": {
                            "up":   {"uv": [0, 0, 16, 10], "texture": "#wool"},
                            "west": {"uv": [0, 7, 10, 11], "texture": "#wool"},
                            "east": {"uv": [6, 7, 16, 11], "texture": "#wool"}
                          }
                        },
                        {
                          "from": [0, 5, 10],
                          "to": [16, 9, 16],
                          "faces": {
                            "up":    {"uv": [0, 10, 16, 16], "texture": "#pillow"},
                            "south": {"uv": [0, 7, 16, 11], "texture": "#pillow"},
                            "west":  {"uv": [10, 7, 16, 11], "texture": "#pillow"},
                            "east":  {"uv": [0, 7, 6, 11], "texture": "#pillow"}
                          }
                        },
                        {
                          "from": [1, 9, 9],
                          "to": [15, 10, 15],
                          "faces": {
                            "up":    {"uv": [1, 2, 15, 8], "texture": "#pillow"},
                            "north": {"uv": [1, 14, 15, 15], "texture": "#pillow"},
                            "south": {"uv": [1, 14, 15, 15], "texture": "#pillow"},
                            "west":  {"uv": [2, 14, 8, 15], "texture": "#pillow"},
                            "east":  {"uv": [2, 14, 8, 15], "texture": "#pillow"}
                          }
                        },
                        {
                          "from": [0, 0, 13],
                          "to": [3, 3, 16],
                          "faces": {
                            "down":  {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "north": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "south": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "west":  {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "east":  {"uv": [0, 13, 3, 16], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [13, 0, 13],
                          "to": [16, 3, 16],
                          "faces": {
                            "down":  {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "north": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "south": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "west":  {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "east":  {"uv": [13, 13, 16, 16], "texture": "#wood"}
                          }
                        }
                      ]
                    }
                    """.formatted(woolTex));

            // 4. Блокстейт для всех направлений и 3 секций (foot, middle, head)
            StringBuilder bs = new StringBuilder("{\n  \"variants\": {\n");
            String[] facings = {"south", "west", "north", "east"};
            int[] yRots = {0, 90, 180, 270};
            boolean first = true;
            for (int fi = 0; fi < 4; fi++) {
                for (String sec : new String[]{"foot", "middle", "head"}) {
                    if (!first) bs.append(",\n");
                    first = false;
                    bs.append("    \"facing=").append(facings[fi]).append(",section=").append(sec)
                            .append("\": { \"model\": \"rpgcore:block/").append(b.id).append("_").append(sec).append("\"");
                    if (yRots[fi] != 0) bs.append(", \"y\": ").append(yRots[fi]);
                    bs.append(" }");
                }
            }
            bs.append("\n  }\n}\n");
            writeString(assets.resolve("blockstates/" + b.id + ".json"), bs.toString());

            // 5. ИСПРАВЛЕНО (image_a5d264.png): Настоящая ванильная 3D-изометрическая модель иконки кровати!
            // Точно повторяет вид обычной ванильной кровати в слоте инвентаря, но чуть крупнее и длиннее!
            writeString(assets.resolve("models/item/" + b.id + ".json"), """
                    {
                      "gui_light": "side",
                      "textures": {
                        "particle": "%1$s",
                        "wool": "%1$s",
                        "pillow": "minecraft:block/white_wool",
                        "wood": "minecraft:block/oak_planks"
                      },
                      "elements": [
                        {
                          "from": [2, 3, -6],
                          "to": [14, 5, 22],
                          "faces": {
                            "down":  {"uv": [0, 0, 12, 16], "texture": "#wood"},
                            "north": {"uv": [2, 11, 14, 13], "texture": "#wood"},
                            "south": {"uv": [2, 11, 14, 13], "texture": "#wood"},
                            "west":  {"uv": [0, 11, 16, 13], "texture": "#wood"},
                            "east":  {"uv": [0, 11, 16, 13], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [2, 5, -6],
                          "to": [14, 9, 14],
                          "faces": {
                            "up":    {"uv": [2, 0, 14, 16], "texture": "#wool"},
                            "north": {"uv": [2, 7, 14, 11], "texture": "#wool"},
                            "west":  {"uv": [0, 7, 16, 11], "texture": "#wool"},
                            "east":  {"uv": [0, 7, 16, 11], "texture": "#wool"}
                          }
                        },
                        {
                          "from": [2, 5, 14],
                          "to": [14, 9, 22],
                          "faces": {
                            "up":    {"uv": [2, 8, 14, 16], "texture": "#pillow"},
                            "south": {"uv": [2, 7, 14, 11], "texture": "#pillow"},
                            "west":  {"uv": [8, 7, 16, 11], "texture": "#pillow"},
                            "east":  {"uv": [0, 7, 8, 11], "texture": "#pillow"}
                          }
                        },
                        {
                          "from": [2, 0, -6],
                          "to": [5, 3, -3],
                          "faces": {
                            "down":  {"uv": [0, 0, 3, 3], "texture": "#wood"},
                            "north": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "south": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "west":  {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "east":  {"uv": [0, 13, 3, 16], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [11, 0, -6],
                          "to": [14, 3, -3],
                          "faces": {
                            "down":  {"uv": [13, 0, 16, 3], "texture": "#wood"},
                            "north": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "south": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "west":  {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "east":  {"uv": [13, 13, 16, 16], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [2, 0, 19],
                          "to": [5, 3, 22],
                          "faces": {
                            "down":  {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "north": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "south": {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "west":  {"uv": [0, 13, 3, 16], "texture": "#wood"},
                            "east":  {"uv": [0, 13, 3, 16], "texture": "#wood"}
                          }
                        },
                        {
                          "from": [11, 0, 19],
                          "to": [14, 3, 22],
                          "faces": {
                            "down":  {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "north": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "south": {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "west":  {"uv": [13, 13, 16, 16], "texture": "#wood"},
                            "east":  {"uv": [13, 13, 16, 16], "texture": "#wood"}
                          }
                        }
                      ],
                      "display": {
                        "gui": {
                          "rotation": [30, 135, 0],
                          "translation": [0, 1.2, 0],
                          "scale": [0.58, 0.58, 0.58]
                        },
                        "ground": {
                          "translation": [0, 3, 0],
                          "scale": [0.28, 0.28, 0.28]
                        },
                        "fixed": {
                          "rotation": [-90, 0, 0],
                          "translation": [0, 0, -4],
                          "scale": [0.5, 0.5, 0.5]
                        },
                        "thirdperson_righthand": {
                          "rotation": [75, 45, 0],
                          "translation": [0, 2.5, 0],
                          "scale": [0.35, 0.35, 0.35]
                        },
                        "firstperson_righthand": {
                          "rotation": [0, 45, 0],
                          "translation": [0, 2.0, 0],
                          "scale": [0.4, 0.4, 0.4]
                        }
                      }
                    }
                    """.formatted(woolTex));

            // 6. Рецепт крафта для верстака и JEI: 2 обычные кровати в ряд -> 1 Большая кровать
            writeString(data.resolve("recipe/" + b.id + ".json"), """
                    {
                      "type": "minecraft:crafting_shaped",
                      "category": "misc",
                      "pattern": [
                        "BB"
                      ],
                      "key": {
                        "B": { "item": "minecraft:%s_bed" }
                      },
                      "result": {
                        "id": "rpgcore:%s",
                        "count": 1
                      }
                    }
                    """.formatted(b.vanillaColor, b.id));
        }
    }

    private static void drawBackpackIcon(Path file, BackpackItem.Tier tier) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int out = 0xFF2B190E;
        int leatherDark = 0xFF5C3820;
        int leatherMid = 0xFF7D4E2D;
        int leatherLight = 0xFF96613B;
        int strapCol = 0xFF3E2414;

        int trimLight = tier.getTrimLight();
        int trimDark = tier.getTrimDark();
        int lvl = tier.getLevel();

        for (int x = 6; x <= 9; x++) {
            img.setRGB(x, 1, (lvl >= 2) ? trimDark : out);
        }
        img.setRGB(5, 2, out);
        img.setRGB(10, 2, out);

        for (int y = 7; y <= 12; y++) {
            for (int x : new int[]{2, 3, 12, 13}) {
                boolean border = (x == 2 || x == 13 || y == 7 || y == 12);
                int c = border ? out : leatherDark;
                if (lvl >= 3 && y == 8 && (x == 3 || x == 12)) {
                    c = trimLight;
                }
                img.setRGB(x, y, c);
            }
        }

        for (int y = 3; y <= 14; y++) {
            for (int x = 4; x <= 11; x++) {
                boolean border = (x == 4 || x == 11 || y == 3 || y == 14);
                int c = ((x + y) % 2 == 0) ? leatherMid : leatherLight;

                if (border) {
                    if (lvl >= 1 && ((y <= 4 || y >= 13) && (x == 4 || x == 11))) {
                        c = trimDark;
                    } else if (lvl >= 4 && (y == 3 || y == 14)) {
                        c = trimDark;
                    } else {
                        c = out;
                    }
                } else if (y == 7) {
                    c = (lvl >= 2) ? trimLight : out;
                } else if (y < 7) {
                    c = ((x + y) % 2 == 0) ? leatherLight : leatherMid;
                }

                if ((x == 5 || x == 10) && y >= 4 && y <= 13) {
                    c = strapCol;
                }

                if ((x == 5 || x == 10) && (y == 8 || y == 9)) {
                    c = (y == 8) ? trimLight : trimDark;
                }

                if (lvl >= 1 && (x == 7 || x == 8) && (y == 5 || y == 6)) {
                    c = (y == 5) ? trimLight : trimDark;
                }

                img.setRGB(x, y, c);
            }
        }

        ImageIO.write(img, "PNG", file.toFile());
    }

    private static void drawHighResDoorSection(Path file, DoorDef d, int mode) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int woodVeryDark = adjustColor(d.darkCol, 0.78F);
        int woodDark = d.darkCol;
        int woodMid = d.mainCol;
        int woodLight = d.lightCol;
        int woodHighlight = adjustColor(d.lightCol, 1.08F);

        int ironOutline = 0xFF1F2126;
        int ironBase = 0xFF383C44;
        int ironMid = 0xFF505661;
        int ironRivet = 0xFF7D8594;

        int brassDark = 0xFF6B4C1B;
        int brassMid = 0xFFA87E2B;
        int brassBright = 0xFFDEC05A;
        int brassSpec = 0xFFF7E594;

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int modX = x % 4;
                int hash = (x * 37 + y * 13 + (x / 4) * 19) & 15;
                int col;
                if (modX == 0) col = (hash % 3 == 0) ? woodVeryDark : woodDark;
                else if (modX == 1) col = (hash < 4) ? woodHighlight : woodLight;
                else if (modX == 2) col = (hash < 5) ? woodLight : ((hash > 12) ? woodDark : woodMid);
                else col = (hash < 6) ? woodDark : woodMid;

                if (mode == 0) {
                    if (y == 15) col = woodVeryDark;
                    else if (y == 13 || y == 14) {
                        col = (y == 13) ? ironMid : ironBase;
                        if (x % 4 == 2 && y == 13) col = ironRivet;
                    } else if (y == 12) col = ironOutline;
                }

                if (mode == 4) {
                    if (y == 0) col = woodVeryDark;
                    else if (y == 1 || y == 2) {
                        col = (y == 1) ? ironMid : ironBase;
                        if (x % 4 == 2 && y == 2) col = ironRivet;
                    } else if (y == 3) col = ironOutline;
                }

                if (mode == 3 && x >= 2 && x <= 13 && y >= 1 && y <= 14) {
                    boolean cornerCut = (y == 1 && (x == 2 || x == 13));
                    if (!cornerCut) {
                        if (x == 2 || x == 13 || y == 1 || y == 14) col = woodVeryDark;
                        else if (x == 3 || x == 12 || y == 2 || y == 13) col = (x == 3 || y == 2) ? woodDark : woodHighlight;
                        else {
                            boolean vBar = (x == 7 || x == 8);
                            boolean hBar = (y == 7 || y == 8);
                            if (vBar && hBar) col = (x == 7 && y == 7) ? ironRivet : ironMid;
                            else if (vBar) col = (x == 7) ? ironMid : ironOutline;
                            else if (hBar) col = (y == 7) ? ironMid : ironOutline;
                            else {
                                img.setRGB(x, y, 0x00000000);
                                continue;
                            }
                        }
                    }
                }

                if (mode == 2) {
                    int pdx = Math.abs(x - 8);
                    int pdy = Math.abs(y - 5);
                    if (pdx + pdy <= 3) col = (pdx + pdy == 3) ? ironOutline : ((y < 5) ? ironMid : ironBase);
                    double sdx = x - 8.0, sdy = y - 10.2;
                    double sDist = Math.sqrt(sdx * sdx + sdy * sdy);
                    if (sDist >= 2.6 && sDist <= 4.3) col = woodVeryDark;
                    double rdx = x - 8.0, rdy = y - 9.2;
                    double rDist = Math.sqrt(rdx * rdx + rdy * rdy);
                    if (rDist >= 2.2 && rDist <= 3.8) {
                        if (rdy < -1.0 && rdx < 0) col = brassSpec;
                        else if (rdy < 0.5) col = brassBright;
                        else if (rdy < 2.2) col = brassMid;
                        else col = brassDark;
                    }
                    if ((x == 7 || x == 8 || x == 9) && (y == 5 || y == 6)) col = (y == 5) ? ironRivet : ironOutline;
                }

                img.setRGB(x, y, col | 0xFF000000);
            }
        }
        ImageIO.write(img, "PNG", file.toFile());
    }

    private static int adjustColor(int rgb, float factor) {
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * factor)));
        return (r << 16) | (g << 8) | b;
    }

    private static void drawDoorItemIcon(Path file, DoorDef d) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 1; y <= 14; y++) {
            for (int x = 3; x <= 12; x++) {
                if (y >= 3 && y <= 6 && ((x >= 5 && x <= 6) || (x >= 9 && x <= 10))) {
                    img.setRGB(x, y, 0x00000000);
                    continue;
                }
                boolean border = (x == 3 || x == 12 || y == 1 || y == 14 || x == 7 || x == 8 || y == 7);
                int col = border ? d.darkCol : ((x + y) % 2 == 0 ? d.mainCol : d.lightCol);
                if ((y == 8 || y == 9) && (x == 6 || x == 9)) col = 0xFFD4AF37;
                img.setRGB(x, y, col | 0xFF000000);
            }
        }
        ImageIO.write(img, "PNG", file.toFile());
    }

    private static void drawClothingIcon(Path file, String type) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        switch (type) {
            case "straw_hat" -> {
                int rimDark = 0xFF8C6928, straw = 0xFFD9B45B, strawLight = 0xFFF2D47E, ribbon = 0xFF962B22;
                for (int x = 4; x <= 11; x++) {
                    for (int y = 4; y <= 8; y++) {
                        if (y == 4 && (x == 4 || x == 11)) continue;
                        img.setRGB(x, y, (y == 4 || x == 4 || x == 11) ? rimDark : ((x + y) % 2 == 0 ? strawLight : straw));
                    }
                    img.setRGB(x, 8, ribbon);
                }
                for (int x = 1; x <= 14; x++) {
                    for (int y = 9; y <= 11; y++) {
                        if (y == 11 && (x <= 2 || x >= 13)) continue;
                        img.setRGB(x, y, (y == 11 || x == 1 || x == 14) ? rimDark : ((x + y) % 2 == 0 ? strawLight : straw));
                    }
                }
            }
            case "peasant_shirt" -> {
                int out = 0xFF6E6353, base = 0xFFD8CCB8, shade = 0xFFBFB29C, belt = 0xFF4A2E1B, buckle = 0xFFD4AF37;
                for (int x = 2; x <= 13; x++) {
                    for (int y = 2; y <= 14; y++) {
                        boolean sleeve = (y >= 3 && y <= 9) && (x <= 4 || x >= 11);
                        boolean body = (x >= 4 && x <= 11) && (y >= 2 && y <= 14);
                        if (!sleeve && !body) continue;
                        if (y == 2 && (x == 7 || x == 8)) continue;
                        if (y == 3 && (x == 7 || x == 8)) {
                            img.setRGB(x, y, out);
                            continue;
                        }
                        if (y == 11 && x >= 4 && x <= 11) {
                            img.setRGB(x, y, (x == 7 || x == 8) ? buckle : belt);
                            continue;
                        }
                        boolean border = (x == 2 || x == 13 || y == 2 || y == 14 || (sleeve && y == 9) || (body && !sleeve && (x == 4 || x == 11)));
                        img.setRGB(x, y, border ? out : ((x + y) % 3 == 0 ? shade : base));
                    }
                }
            }
            case "peasant_pants" -> {
                int out = 0xFF3E3328, cloth = 0xFF7A6652, shade = 0xFF635241, belt = 0xFF2E2218;
                for (int x = 4; x <= 11; x++) {
                    for (int y = 2; y <= 14; y++) {
                        if (y >= 7 && (x == 7 || x == 8)) continue;
                        if (y == 2 || y == 3) {
                            img.setRGB(x, y, belt);
                            continue;
                        }
                        boolean border = (x == 4 || x == 11 || y == 14 || (y >= 7 && (x == 6 || x == 9)));
                        img.setRGB(x, y, border ? out : ((x + y) % 2 == 0 ? cloth : shade));
                    }
                }
            }
            case "peasant_shoes" -> {
                int out = 0xFF2B180D, leather = 0xFF6B3E23, dark = 0xFF522E18, cuff = 0xFFC7B99E;
                for (int leg = 0; leg < 2; leg++) {
                    int sx = (leg == 0) ? 2 : 9;
                    for (int dx = 0; dx < 5; dx++) {
                        for (int y = 7; y <= 14; y++) {
                            if (y < 11 && (leg == 0 ? dx == 0 : dx == 4)) continue;
                            int x = sx + dx;
                            if (y == 7 || y == 8) {
                                img.setRGB(x, y, cuff);
                            } else if (y == 14 || dx == 0 || dx == 4) {
                                img.setRGB(x, y, out);
                            } else {
                                img.setRGB(x, y, (x + y) % 2 == 0 ? leather : dark);
                            }
                        }
                    }
                }
            }
            case "hooded_cloak" -> {
                int out = 0xFF1C1917, dark = 0xFF2B2724, mid = 0xFF3B3632, light = 0xFF4A443F, brooch = 0xFFD4AF37;
                for (int y = 1; y <= 5; y++) {
                    for (int x = 5; x <= 10; x++) {
                        if (y == 1 && (x == 5 || x == 10)) continue;
                        boolean border = (y == 1 || x == 5 || x == 10);
                        int c = border ? out : ((y >= 3 && x >= 7 && x <= 8) ? 0xFF12100E : ((x + y) % 2 == 0 ? light : mid));
                        img.setRGB(x, y, c);
                    }
                }
                for (int y = 5; y <= 14; y++) {
                    int halfW = (y <= 7) ? 5 : 6;
                    for (int x = 8 - halfW; x <= 7 + halfW; x++) {
                        boolean border = (x == 8 - halfW || x == 7 + halfW || y == 14);
                        int c = border ? out : ((x % 3 == 0) ? dark : ((x + y) % 2 == 0 ? mid : light));
                        if (y == 6 && (x == 7 || x == 8)) c = brooch;
                        if (y == 13 && !border) c = 0xFF8C6D1F;
                        img.setRGB(x, y, c);
                    }
                }
            }
        }
        ImageIO.write(img, "PNG", file.toFile());
    }

    private static void writeString(Path path, String content) throws Exception {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}