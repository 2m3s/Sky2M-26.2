package com.epic60869.sky2m.features.dungeons;

import com.epic60869.sky2m.Sky2MGlobalChat;
import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.core.Sky2MAlerts;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

/**
 * Dungeons > Run Recap > Run Summary Card: builds a summary of the run that just ended from its Run History record
 * (floor, time, score, secrets, crypts, deaths, splits, party) and the reward chests opened for it (Chest History
 * profit). {@link RunSummaryScreen} draws it; {@link #textLines} is the plain text version that /s2 runsummary share
 * sends to party, guild or Sky2M chat, one message a second.
 */
final class RunSummary {
    private static final Queue<Runnable> outbox = new ArrayDeque<>();
    private static int cooldown;

    private RunSummary() {}

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (outbox.isEmpty()) return;
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            outbox.poll().run();
            cooldown = 20;
        });
    }

    static void onRunFinished(RunHistory.Run run) {
        DungeonRecapExtras.Config c = DungeonRecapExtras.config();
        if (c == null || !c.runSummary.enabled) return;
        if (c.runSummary.chatLink) {
            Sky2MAlerts.chat(Component.literal("Run summary ready: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("[View Summary]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                    .withClickEvent(new ClickEvent.RunCommand("/s2 runsummary")))));
        }
    }

    private static RunHistory.Run current() {
        List<RunHistory.Run> runs = RunHistory.runs();
        return runs.isEmpty() ? null : runs.getFirst();
    }

    static void open() {
        RunHistory.Run run = current();
        if (run == null) {
            Sky2MAlerts.chat(Component.literal("No finished run yet. Turn on Run History (Dungeons > Timers and Alerts) and finish a run.").withStyle(ChatFormatting.GRAY));
            return;
        }
        Compat.queueOpenScreen(new RunSummaryScreen(run));
    }

    /** Coins from the reward chests opened for this run: Chest History entries after it started and before the next run. */
    static double profit(RunHistory.Run run) {
        List<RunHistory.Run> runs = RunHistory.runs();
        int i = runs.indexOf(run);
        long until = i > 0 ? runs.get(i - 1).startedAt : Long.MAX_VALUE;
        double sum = 0;
        for (ChestHistory.Chest chest : ChestHistory.chests()) {
            if (chest.time < run.startedAt || chest.time >= until) continue;
            if (chest.floor != null && run.floor != null && !chest.floor.equals(run.floor)) continue;
            sum += chest.profit;
        }
        return sum;
    }

    static boolean hasChests(RunHistory.Run run) {
        List<RunHistory.Run> runs = RunHistory.runs();
        int i = runs.indexOf(run);
        long until = i > 0 ? runs.get(i - 1).startedAt : Long.MAX_VALUE;
        for (ChestHistory.Chest chest : ChestHistory.chests()) {
            if (chest.time >= run.startedAt && chest.time < until && (chest.floor == null || run.floor == null || chest.floor.equals(run.floor))) return true;
        }
        return false;
    }

    static String coins(double v) {
        double a = Math.abs(v);
        String s = a >= 1_000_000 ? String.format(Locale.ROOT, "%.2fM", a / 1_000_000) : a >= 1_000 ? String.format(Locale.ROOT, "%.1fK", a / 1_000) : String.format(Locale.ROOT, "%.0f", a);
        return (v < 0 ? "-" : "+") + s;
    }

    /** Plain text, short enough for a chat message each. */
    static List<String> textLines(RunHistory.Run run) {
        List<String> out = new ArrayList<>();
        StringBuilder head = new StringBuilder("Sky2M " + (run.floor == null ? "?" : run.floor) + (run.completed ? " clear " : " run ") + RunHistory.time(run.totalMs));
        if (run.effectiveScore() >= 0) head.append(" | Score ").append(run.effectiveScore()).append(" (").append(run.effectiveRank()).append(")");
        if (run.secrets >= 0) head.append(" | Secrets ").append(run.secrets);
        if (run.crypts >= 0) head.append(" | Crypts ").append(run.crypts);
        int deaths = run.deathLog != null ? run.deathLog.size() : Math.max(0, run.deaths);
        head.append(" | Deaths ").append(deaths);
        if (hasChests(run)) head.append(" | Profit ").append(coins(profit(run)));
        out.add(head.toString());
        if (!run.splits.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (RunHistory.Split s : run.splits) parts.add(s.name() + " " + RunHistory.time(s.ms()));
            wrap(out, "Splits: ", parts);
        }
        if (run.deathLog != null && !run.deathLog.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (DeathRecap.DeathEntry d : run.deathLog) parts.add(d.name() + " (" + d.cause() + (d.ms() >= 0 ? ", " + RunHistory.time(d.ms()) : "") + ")");
            wrap(out, "Deaths: ", parts);
        }
        if (!run.party.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (RunHistory.Member m : run.party) parts.add(m.name() + (m.dungeonClass() == null ? "" : " " + m.dungeonClass()));
            wrap(out, "Party: ", parts);
        }
        return out;
    }

    private static void wrap(List<String> out, String prefix, List<String> parts) {
        StringBuilder line = new StringBuilder(prefix);
        boolean first = true;
        for (String part : parts) {
            if (line.length() + part.length() + 2 > 230 && !first) {
                out.add(line.toString());
                line = new StringBuilder(prefix);
                first = true;
            }
            if (!first) line.append(", ");
            line.append(part);
            first = false;
        }
        out.add(line.toString());
    }

    /** /s2 runsummary share party|guild|s2c. */
    static void share(String target) {
        RunHistory.Run run = current();
        if (run == null) {
            Sky2MAlerts.chat(Component.literal("No finished run to share yet.").withStyle(ChatFormatting.GRAY));
            return;
        }
        share(run, target);
    }

    static void share(RunHistory.Run run, String target) {
        String t = target.toLowerCase(Locale.ROOT);
        String command;
        switch (t) {
            case "party", "p", "pc" -> command = "pc ";
            case "guild", "g", "gc" -> command = "gc ";
            case "s2c", "sky2m", "chat" -> command = null;
            default -> {
                Sky2MAlerts.chat(Component.literal("Share to party, guild or s2c.").withStyle(ChatFormatting.RED));
                return;
            }
        }
        for (String line : textLines(run)) {
            outbox.add(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (command == null) Sky2MGlobalChat.send(line);
                else if (mc.getConnection() != null) mc.getConnection().sendCommand(command + line);
            });
        }
        Sky2MAlerts.chat(Component.literal("Sharing the run summary to " + t + "...").withStyle(ChatFormatting.GRAY));
    }
}
