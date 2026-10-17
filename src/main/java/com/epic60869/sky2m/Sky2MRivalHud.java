package com.epic60869.sky2m;

import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.core.Sky2MHuds;
import com.epic60869.sky2m.features.core.Sky2MLocation;
import com.epic60869.sky2m.features.misc.CollectionTracker;
import com.epic60869.sky2m.features.sbc.SbcNet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rival HUD (Misc > Collections > Rival HUD): the Sky2M collection board for what you're doing right now, against
 * your rivals on it — both amounts, who's ahead and by how much. Farming Wheat shows the Wheat board; switch to Cactus
 * and it shows Cactus. Hidden when you have no rival on that board.
 *
 * <p>What you're doing, freshest first: the crop you're breaking, the crop your farming tool is for, then the
 * collection the Collection Tracker saw you gather (when that's on). The last one is kept for a while after you
 * stop, so the HUD doesn't blink out between crops.
 *
 * <p>The standings are the ones {@link Sky2MLeaderboards} already caches (the server only refreshes them every 15
 * minutes); while the HUD is on and has a rival board it asks for that board when it has none, then every
 * {@link #REFRESH_MS}, never while another board request is out or a screen is open (so it can't take the
 * leaderboard screen's answer). The lines are built once per tick, not per frame.
 */
public final class Sky2MRivalHud {
    private static final String ID = "rival_hud";
    private static final long REFRESH_MS = 5 * 60_000L;
    /** Don't ask again sooner than this when an answer didn't come (offline, or the server had none). */
    private static final long RETRY_MS = 30_000L;
    /** Breaking a crop counts as "what you're doing" this long after the last hit. */
    private static final long BREAKING_MS = 5_000L;
    /** The board stays up this long after you stop gathering. */
    private static final long KEEP_MS = 90_000L;

    private static String breaking = "";
    private static long breakingAt;
    private static String board = "";
    private static long boardAt;
    private static String requested = "";
    private static long requestedAt;
    private static List<Component> lines = List.of();

    private static final List<Component> PREVIEW = List.of(
        Component.literal("⚔ Wheat").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
        Component.literal("You #12 ").withStyle(ChatFormatting.GREEN).append(Component.literal("12,345,678").withStyle(ChatFormatting.YELLOW)),
        Component.literal("Rival #10 ").withStyle(ChatFormatting.GOLD).append(Component.literal("13,000,000").withStyle(ChatFormatting.YELLOW)),
        Component.literal("654,322 behind").withStyle(ChatFormatting.RED));

    private Sky2MRivalHud() {}

    private static boolean turnedOn() {
        Sky2MConfig c = Sky2MConfig.current();
        return c != null && c.misc.rivalHud;
    }

    static void init() {
        Sky2MHuds.setting(ID, Sky2MRivalHud::turnedOn);
        Sky2MHuds.setSettingsSearch(ID, "Rival HUD");
        Sky2MHuds.register(ID, "Rival HUD", () -> turnedOn() && !lines.isEmpty(), () -> lines, PREVIEW, 8, 190);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick(mc));
        // The crop you break, the way the Farming profit tracker sees it.
        ClientPlayerBlockBreakEvents.AFTER.register((level, player, pos, state) -> {
            String crop = turnedOn() ? cropOf(state.getBlock(), level.getGameTime()) : null;
            if (crop != null) {
                breaking = crop;
                breakingAt = System.currentTimeMillis();
            }
        });
    }

    private static void tick(Minecraft mc) {
        if (!turnedOn() || mc.player == null || mc.level == null || !Sky2MLocation.onSkyblock()) {
            lines = List.of();
            return;
        }
        long now = System.currentTimeMillis();
        String collection = current(mc, now);
        if (!collection.isEmpty()) {
            board = "collection|" + collection;
            boardAt = now;
        } else if (now - boardAt > KEEP_MS) {
            board = "";
        }
        if (board.isEmpty() || !Sky2MRivals.hasRivalsOn(board)) {
            lines = List.of();
            return;
        }
        Sky2MLeaderboards.StatResult result = Sky2MLeaderboards.cachedStat(board);
        boolean stale = result == null || now - result.at() > REFRESH_MS;
        boolean waited = !board.equals(requested) || now - requestedAt > RETRY_MS;
        if (stale && waited && mc.gui.screen() == null && SbcNet.online() && !Sky2MLeaderboards.statBusy()) {
            int bar = board.indexOf('|');
            if (Sky2MLeaderboards.requestStat(board.substring(0, bar), board.substring(bar + 1))) {
                requested = board;
                requestedAt = now;
            }
        }
        lines = build(result);
    }

    /** The collection you're working on right now ("WHEAT"), or "". */
    private static String current(Minecraft mc, long now) {
        if (!breaking.isEmpty() && now - breakingAt < BREAKING_MS) return breaking;
        String tool = toolCrop(Compat.neuName(mc.player.getMainHandItem()), mc.level.getGameTime());
        if (tool != null) return tool;
        return CollectionTracker.gathering();
    }

    /** A crop block's collection (Hypixel IDs, as the boards use), or null. */
    private static String cropOf(Block block, long gameTime) {
        if (block == Blocks.WHEAT) return "WHEAT";
        if (block == Blocks.CARROTS) return "CARROT_ITEM";
        if (block == Blocks.POTATOES) return "POTATO_ITEM";
        if (block == Blocks.NETHER_WART) return "NETHER_STALK";
        if (block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN) return "PUMPKIN";
        if (block == Blocks.MELON) return "MELON";
        if (block == Blocks.COCOA) return "INK_SACK:3";
        if (block == Blocks.SUGAR_CANE) return "SUGAR_CANE";
        if (block == Blocks.CACTUS) return "CACTUS";
        if (block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM
            || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.BROWN_MUSHROOM_BLOCK) return "MUSHROOM_COLLECTION";
        if (block == Blocks.ROSE_BUSH) return "WILD_ROSE";
        if (block == Blocks.SUNFLOWER) return sunflower(gameTime);
        return null;
    }

    /** The Garden's sunflowers are Moonflowers at night (as the Farming profit tracker counts them). */
    private static String sunflower(long gameTime) {
        return gameTime % 24_000L >= 12_000L ? "MOONFLOWER" : "DOUBLE_PLANT";
    }

    /** The crop a farming tool is for (Sky2MMouseLock's tool list), or null for other items and the generic ones. */
    private static String toolCrop(String id, long gameTime) {
        if (id == null || id.isEmpty()) return null;
        if (id.startsWith("THEORETICAL_HOE_")) {
            String crop = id.substring("THEORETICAL_HOE_".length()).replaceFirst("_\\d+$", "");
            return switch (crop) {
                case "WHEAT" -> "WHEAT";
                case "CARROT" -> "CARROT_ITEM";
                case "POTATO" -> "POTATO_ITEM";
                case "CANE" -> "SUGAR_CANE";
                case "WARTS" -> "NETHER_STALK";
                case "WILD_ROSE" -> "WILD_ROSE";
                case "SUNFLOWER" -> sunflower(gameTime);
                default -> null;
            };
        }
        if (id.startsWith("FUNGI_CUTTER")) return "MUSHROOM_COLLECTION";
        if (id.startsWith("CACTUS_KNIFE")) return "CACTUS";
        if (id.startsWith("MELON_DICER")) return "MELON";
        if (id.startsWith("PUMPKIN_DICER")) return "PUMPKIN";
        if (id.startsWith("COCO_CHOPPER")) return "INK_SACK:3";
        return null;
    }

    /** "⚔ Wheat", your line, then each rival's line and the gap to them. */
    private static List<Component> build(Sky2MLeaderboards.StatResult result) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal("⚔ " + Sky2MRivals.boardName(board)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (result == null) {
            out.add(Component.literal(SbcNet.online() ? "loading..." : "Can't reach the Sky2M server").withStyle(ChatFormatting.DARK_GRAY));
            return out;
        }
        String self = Minecraft.getInstance().getUser().getName();
        Sky2MLeaderboards.StatEntry you = null;
        for (Sky2MLeaderboards.StatEntry e : result.entries()) if (e.username().equalsIgnoreCase(self)) you = e;
        out.add(you == null ? Component.literal("You: not in the top " + result.entries().size()).withStyle(ChatFormatting.GRAY)
            : Component.literal("You #" + you.rank() + " ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(fmt(you.value())).withStyle(ChatFormatting.YELLOW)));
        for (String rival : Sky2MRivals.rivalsOn(board)) {
            Sky2MLeaderboards.StatEntry them = null;
            for (Sky2MLeaderboards.StatEntry e : result.entries()) if (e.username().equalsIgnoreCase(rival)) them = e;
            if (them == null) {
                // Rivals on every board (from before per-board rivals) who aren't on this one aren't worth a line.
                if (!Sky2MRivals.isRivalEverywhere(rival)) {
                    out.add(Component.literal(rival + ": not in the top " + result.entries().size()).withStyle(ChatFormatting.GRAY));
                }
                continue;
            }
            out.add(Component.literal(them.username() + " #" + them.rank() + " ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fmt(them.value())).withStyle(ChatFormatting.YELLOW)));
            if (you != null) {
                long gap = Math.abs(them.value() - you.value());
                out.add(them.value() == you.value() ? Component.literal("  tied").withStyle(ChatFormatting.YELLOW)
                    : them.value() > you.value() ? Component.literal("  " + fmt(gap) + " behind").withStyle(ChatFormatting.RED)
                    : Component.literal("  " + fmt(gap) + " ahead").withStyle(ChatFormatting.GREEN));
            }
        }
        // Only rivals on every board, none of them on this one: nothing to race here.
        return out.size() <= 2 && Sky2MRivals.rivalsOn(board).stream().allMatch(Sky2MRivals::isRivalEverywhere) ? List.of() : out;
    }

    private static String fmt(long n) {
        return String.format(Locale.US, "%,d", n);
    }
}
