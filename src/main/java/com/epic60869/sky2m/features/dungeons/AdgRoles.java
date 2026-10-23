package com.epic60869.sky2m.features.dungeons;

import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.FeatureConfigs;
import com.epic60869.sky2m.features.core.Sky2MAlerts;
import com.epic60869.sky2m.features.core.Sky2MChat;
import com.epic60869.sky2m.features.core.Sky2MHuds;
import com.epic60869.sky2m.features.core.Sky2MLocation;
import com.epic60869.sky2m.sb.skyblock.dungeon.DungeonClass;
import com.google.gson.Gson;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The ADG M7 role guide (Ascent's "ADG M7 Roles"): for your class, a title and a short checklist at the start of each
 * F7/M7 phase: the clear, Maxor, Storm, terminal sections 1-4, Goldor, Necron, relics and dragons.
 * <p>
 * The guides are data, in {@code assets/sky2m/adg/<class>.json} (archer, bers, healer, and mage/tank once added), so a
 * role is edited without touching code. {@code /s2 adg [class] [phase]} prints a guide in chat.
 */
public final class AdgRoles {
    private static final Gson GSON = new Gson();
    private static final String DRAGONS_LINE = "[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you";
    private static final List<String> PHASES = List.of("clear", "maxor", "storm", "s1", "s2", "s3", "s4", "goldor", "necron", "relics", "dragons");

    /** One guide: its class name and the sections in phase order. */
    private static final class Guide {
        String name;
        List<Section> sections;
    }

    private static final class Section {
        String id;
        String title;
        String subtitle;
        List<String> lines;
    }

    private static final Map<String, Guide> GUIDES = new HashMap<>();
    private static final Set<String> SHOWN = new HashSet<>();
    /** The section of the phase you are in, for the HUD; cleared when you change world. */
    private static volatile Section current;
    private static final int WRAP = 58;

    private AdgRoles() {}

    private static FeatureConfigs.PositionalMessages config() {
        Sky2MConfig c = Sky2MConfig.current();
        return c == null ? null : c.dungeons.positionalMessages;
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> {
            SHOWN.clear();
            current = null;
        });
        Sky2MLocation.onAreaChange(area -> {
            SHOWN.clear();
            current = null;
        });
        Sky2MHuds.setting("adg_role", () -> {
            FeatureConfigs.PositionalMessages c = config();
            return c != null && c.adgRoles && c.adgHud;
        });
        Sky2MHuds.register("adg_role", "ADG Role",
            () -> {
                FeatureConfigs.PositionalMessages c = config();
                return c != null && c.adgRoles && c.adgHud && current != null && Sky2MLocation.inDungeon();
            },
            AdgRoles::hudLines,
            List.of(Component.literal("Storm: pad yellow 37.1-37.9").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Spirit mask for lightning at purple").withStyle(ChatFormatting.YELLOW)),
            8, 120);
        Sky2MChat.onChat(message -> {
            String text = message.text();
            if (text.equals("Starting in 1 second.")) {
                if (Sky2MLocation.dungeonFloor().endsWith("7")) show("clear");
            } else if (text.startsWith(DRAGONS_LINE)) {
                show("dragons");
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("adg")
                    .executes(c -> print(null, null))
                    .then(ClientCommands.argument("class", StringArgumentType.word())
                        .executes(c -> print(StringArgumentType.getString(c, "class"), null))
                        .then(ClientCommands.argument("phase", StringArgumentType.word())
                            .executes(c -> print(StringArgumentType.getString(c, "class"), StringArgumentType.getString(c, "phase")))))));
            }
        });
    }

    /** Whether the guide is on, so {@link BossPhases} keeps tracking F7/M7 phases for it. */
    static boolean wantsPhases() {
        FeatureConfigs.PositionalMessages c = config();
        return c != null && c.adgRoles;
    }

    /** From {@link BossPhases} when the fight moves to {@code name} on {@code floor} ("F7", "M7"). */
    static void onPhase(String floor, String name) {
        if (floor == null || !floor.endsWith("7")) return;
        String id = switch (name) {
            case "Maxor" -> "maxor";
            case "Storm" -> "storm";
            case "Terminals 1" -> "s1";
            case "Terminals 2" -> "s2";
            case "Terminals 3" -> "s3";
            case "Terminals 4" -> "s4";
            case "Goldor" -> "goldor";
            case "Necron" -> "necron";
            case "Wither King" -> "relics";
            default -> null;
        };
        if (id != null) show(id);
    }

    /** The guide key for a dungeon class, or null. */
    private static String key(DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case HEALER -> "healer";
            case MAGE -> "mage";
            case BERSERK -> "bers";
            case ARCHER -> "archer";
            case TANK -> "tank";
            default -> null;
        };
    }

    private static String normalise(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return switch (n) {
            case "berserk", "berserker", "berz" -> "bers";
            case "arch" -> "archer";
            case "heal" -> "healer";
            default -> n;
        };
    }

    private static synchronized Guide guide(String key) {
        if (key == null) return null;
        if (GUIDES.containsKey(key)) return GUIDES.get(key);
        Guide guide = null;
        try (InputStream in = AdgRoles.class.getResourceAsStream("/assets/sky2m/adg/" + key + ".json")) {
            if (in != null) guide = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Guide.class);
        } catch (Exception e) {
            System.err.println("[Sky2M] Could not read the ADG guide for " + key + ": " + e);
        }
        GUIDES.put(key, guide);
        return guide;
    }

    private static Section section(Guide guide, String id) {
        if (guide == null || guide.sections == null) return null;
        for (Section s : guide.sections) if (id.equals(s.id)) return s;
        return null;
    }

    private static void show(String id) {
        FeatureConfigs.PositionalMessages c = config();
        if (c == null || !c.adgRoles || !Sky2MLocation.inDungeon()) return;
        if (!SHOWN.add(id)) return;
        Guide guide = guide(key(SelfClass.get()));
        Section section = section(guide, id);
        if (section == null) return;
        current = section;
        if (c.adgTitles && section.title != null) {
            Sky2MAlerts.title(Component.literal(section.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                section.subtitle == null ? null : Component.literal(section.subtitle).withStyle(ChatFormatting.YELLOW));
        }
        if (c.adgChat) chat(guide, section);
    }

    /** The HUD: the phase's title and key action, and (optionally) its steps wrapped to a readable width. */
    private static List<Component> hudLines() {
        Section section = current;
        FeatureConfigs.PositionalMessages c = config();
        if (section == null || c == null) return List.of();
        List<Component> out = new ArrayList<>();
        if (section.title != null) out.add(Component.literal(section.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (section.subtitle != null) out.add(Component.literal(section.subtitle).withStyle(ChatFormatting.YELLOW));
        if (c.adgHudSteps && section.lines != null) {
            for (String line : section.lines) {
                boolean first = true;
                for (String part : wrap(line, WRAP)) {
                    out.add(Component.literal((first ? "- " : "  ") + part).withStyle(ChatFormatting.GRAY));
                    first = false;
                }
            }
        }
        return out;
    }

    private static List<String> wrap(String text, int width) {
        List<String> parts = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (String word : text.split(" ")) {
            if (row.length() > 0 && row.length() + 1 + word.length() > width) {
                parts.add(row.toString());
                row.setLength(0);
            }
            if (row.length() > 0) row.append(' ');
            row.append(word);
        }
        if (row.length() > 0) parts.add(row.toString());
        return parts;
    }

    private static void chat(Guide guide, Section section) {
        Sky2MAlerts.chat(Component.literal("ADG " + guide.name + " - " + section.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (section.lines == null) return;
        for (String line : section.lines) Sky2MAlerts.chat(Component.literal("- " + line).withStyle(ChatFormatting.GRAY));
    }

    private static int print(String classArg, String phaseArg) {
        String key = classArg == null ? key(SelfClass.get()) : normalise(classArg);
        Guide guide = guide(key);
        if (guide == null) {
            Sky2MAlerts.chat(Component.literal(key == null ? "Your class isn't known yet. Use /s2 adg <healer|mage|bers|archer|tank> [phase]."
                : "There is no ADG guide for " + key + " yet.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (phaseArg == null) {
            for (Section s : guide.sections) chat(guide, s);
            return 1;
        }
        String phase = phaseArg.toLowerCase(Locale.ROOT);
        Section section = section(guide, phase);
        if (section == null) {
            Sky2MAlerts.chat(Component.literal("Phases: " + String.join(", ", PHASES)).withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        chat(guide, section);
        return 1;
    }
}
