package com.epic60869.sky2m;

import com.epic60869.sky2m.custom.util.Compat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rivals: players you race on the leaderboards. /s2 rival add|remove <name> (or click them in /s2 leaderboard); they're
 * highlighted there with how far ahead or behind you they are, and when a board's standings show a rival has gone
 * from behind you to ahead of you, you're told in chat. Kept in config/sky2m/rivals.json.
 */
public final class Sky2MRivals {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Set<String> rivals;
    /** "board|rival" -> whether the rival was ahead of you the last time that board's standings came. */
    private static final Map<String, Boolean> AHEAD = new HashMap<>();

    private Sky2MRivals() {}

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("rival")
                    .executes(c -> {
                        list();
                        return 1;
                    })
                    .then(ClientCommands.literal("add").then(ClientCommands.argument("player", StringArgumentType.word())
                        .executes(c -> {
                            set(StringArgumentType.getString(c, "player"), true);
                            return 1;
                        })))
                    .then(ClientCommands.literal("remove").then(ClientCommands.argument("player", StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(names(), b))
                        .executes(c -> {
                            set(StringArgumentType.getString(c, "player"), false);
                            return 1;
                        })))));
            }
        });
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("sky2m").resolve("rivals.json");
    }

    private static synchronized Set<String> rivals() {
        if (rivals == null) {
            rivals = new LinkedHashSet<>();
            try {
                if (Files.exists(file())) {
                    List<String> loaded = GSON.fromJson(Files.readString(file(), StandardCharsets.UTF_8), new TypeToken<List<String>>() {}.getType());
                    if (loaded != null) for (String name : loaded) if (name != null && name.matches("\\w{1,16}")) rivals.add(name.toLowerCase(Locale.ROOT));
                }
            } catch (Exception e) {
                System.err.println("[Sky2M] Couldn't read rivals.json: " + e.getMessage());
            }
        }
        return rivals;
    }

    public static synchronized List<String> names() {
        return new ArrayList<>(rivals());
    }

    public static boolean isRival(String name) {
        return name != null && rivals().contains(name.toLowerCase(Locale.ROOT));
    }

    /** Adds or removes a rival and says so. */
    public static synchronized void set(String name, boolean add) {
        String clean = name == null ? "" : name.trim();
        if (!clean.matches("\\w{1,16}")) {
            say(Component.literal("That isn't a Minecraft name.").withStyle(ChatFormatting.RED));
            return;
        }
        if (clean.equalsIgnoreCase(Minecraft.getInstance().getUser().getName())) {
            say(Component.literal("You can't be your own rival.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        boolean changed = add ? rivals().add(clean.toLowerCase(Locale.ROOT)) : rivals().remove(clean.toLowerCase(Locale.ROOT));
        if (changed) save();
        say(Component.literal(add ? (changed ? clean + " is now your rival. They're highlighted on /s2 leaderboard and you'll hear when they pass you."
                : clean + " is already your rival.") : (changed ? clean + " isn't your rival any more." : clean + " wasn't your rival."))
            .withStyle(add ? ChatFormatting.GOLD : ChatFormatting.GRAY));
    }

    private static void list() {
        List<String> names = names();
        say(Component.literal(names.isEmpty() ? "No rivals yet: /s2 rival add <name>, or click a player in /s2 leaderboard."
            : "Your rivals: " + String.join(", ", names)).withStyle(ChatFormatting.GOLD));
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(new ArrayList<>(rivals)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[Sky2M] Couldn't save rivals.json: " + e.getMessage());
        }
    }

    /**
     * A board's standings arrived: for each rival on it, whether they've gone from behind you to ahead of you since
     * the last time this board came (only when both of you are on it).
     */
    static void onStandings(String board, String boardName, List<Sky2MLeaderboards.StatEntry> entries) {
        if (rivals().isEmpty() || board == null || board.isEmpty() || board.equals("|")) return;
        String self = Minecraft.getInstance().getUser().getName();
        int yourRank = -1;
        for (Sky2MLeaderboards.StatEntry e : entries) if (e.username().equalsIgnoreCase(self)) yourRank = e.rank();
        if (yourRank < 0) return;
        for (Sky2MLeaderboards.StatEntry e : entries) {
            if (!isRival(e.username())) continue;
            boolean ahead = e.rank() < yourRank;
            Boolean before = AHEAD.put(board + "|" + e.username().toLowerCase(Locale.ROOT), ahead);
            if (before != null && !before && ahead) {
                say(Component.literal(e.username() + " passed you on " + (boardName.isBlank() ? board : boardName)
                    + " (#" + e.rank() + ", you're #" + yourRank + ")!").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static void say(Component text) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(Compat.PREFIX.get().append(text));
        });
    }
}
