package com.epic60869.sky2m.features.slayer;

import com.epic60869.sky2m.features.core.Sky2MChat;
import com.epic60869.sky2m.features.core.Sky2MLocation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the current slayer quest cost, as SkyHanni's SlayerProfitTracker works it out: what the purse actually went down
 * by around "SLAYER QUEST STARTED!", or around the boss spawning when Hypixel takes it then (up to 350k; Aatrox's discount and the Rift's motes come out right that way), or,
 * with auto-slayer, what "Took X coins from your bank for auto-slayer..." says. Worked out once per quest here so the
 * slayer profit trackers and the per-kill profit line agree on it.
 */
public final class SlayerQuestCost {
    /** One spawn cost paid, for {@code slayer}'s quest ("Revenant Horror"..., or null when no quest showed). */
    public record Charge(String slayer, long coins, boolean bank) {}

    /**
     * The spawn fee taken from the bank: SkyHanni's slayer.autoslayer.bank ("Took 1.9k coins from your bank for
     * auto-slayer..."), and the same message without "for auto-slayer" when Hypixel takes a normal quest's fee from the
     * bank because the purse is short.
     */
    private static final Pattern BANK_CHARGE = Pattern.compile("^Took (.+?) coins from your bank\\b(.*)$");
    /** A bank charge that doesn't say it's for auto-slayer only counts this close to a quest start (either side). */
    private static final long BANK_NEAR_START_MS = 10_000L;
    private static final Pattern NUMBER = Pattern.compile("[\\d,]+(?:\\.\\d+)?");
    /** How long after the quest started its slayer may take to show on the scoreboard. */
    private static final long QUEST_COST_WINDOW_MS = 3_000L;
    /** The highest cost that can be a spawn fee: anything more is some other purse change. */
    private static final long MAX_COST = 350_000L;

    /** The purse over the last 15 seconds: {time, coins}. Hypixel takes the fee before "SLAYER QUEST STARTED!". */
    private static final ArrayDeque<double[]> purseHistory = new ArrayDeque<>();
    private static final List<Consumer<Charge>> listeners = new ArrayList<>();
    private static double lastPurse = -1;
    private static long questStartedAt;
    private static String questSlayer;
    /** The current quest's cost from the purse and from the bank, or -1 when not seen. */
    private static long purseCost = -1, bankCost = -1, previousCost = -1;
    /** When to look at the purse for the fee (the quest start, then the boss spawn if needed), and which it was. */
    private static long measureAt;
    private static String measureWhat = "";
    private static long bankAt, lastStartAt;
    /** A bank charge that came before its quest started, waiting for the start to confirm it, or -1. */
    private static long pendingBank = -1;
    private static boolean initialized;

    private SlayerQuestCost() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(SlayerQuestCost::tick);
        Sky2MChat.onChat(message -> onChat(message.text().trim()));
    }

    /** Called with each spawn cost as it's worked out (the purse fee a moment after the quest starts, or the bank charge). */
    public static void onCharge(Consumer<Charge> listener) {
        listeners.add(listener);
    }

    /** The current quest's spawn cost: the bank charge if one came, else the purse fee; -1 if unknown. */
    public static long cost() {
        return bankCost > 0 ? bankCost : purseCost;
    }

    /**
     * The spawn cost of the boss that just died: right after the next quest started (an instant restart, in the same
     * tick as the kill), that's the previous quest's cost, not the new one's.
     */
    public static long costOfLastBoss() {
        return lastStartAt > 0 && System.currentTimeMillis() - lastStartAt < 5_000L ? previousCost : cost();
    }

    private static void onChat(String text) {
        if (!Sky2MLocation.onSkyblock()) return;
        long now = System.currentTimeMillis();
        if (text.equals("SLAYER QUEST STARTED!")) {
            // The boss that just died belongs to the quest that's ending: keep its cost for the kill's profit line.
            previousCost = cost();
            questStartedAt = now;
            lastStartAt = now;
            questSlayer = null;
            purseCost = -1;
            bankCost = -1;
            // The fee was taken from the bank just before the quest started: it's this quest's.
            if (pendingBank > 0 && now - bankAt <= BANK_NEAR_START_MS) {
                bankCost = pendingBank;
                charge(new Charge(SlayerArea.questSlayer(), pendingBank, true));
            }
            pendingBank = -1;
            measureAt = now;
            measureWhat = "quest start";
            return;
        }
        // Hypixel can take the fee when the boss spawns instead (an instant restart shows no purse change at the
        // start): measure again there if the start didn't find it.
        if (text.startsWith("SLAYER BOSS!") && text.contains("spawned") && cost() < 0) {
            measureAt = now;
            measureWhat = "boss spawn";
            return;
        }
        Matcher bank = BANK_CHARGE.matcher(text);
        if (bank.matches()) {
            long cost = Math.round(parseCoins(bank.group(1)));
            if (cost <= 0 || cost > MAX_COST) return;
            bankAt = now;
            boolean autoSlayer = bank.group(2).toLowerCase(java.util.Locale.ROOT).contains("slayer");
            if (autoSlayer || now - lastStartAt <= BANK_NEAR_START_MS) {
                bankCost = cost;
                pendingBank = -1;
                charge(new Charge(SlayerArea.questSlayer(), cost, true));
            } else {
                // Maybe for a quest about to start: only counted if one starts within 10 seconds.
                pendingBank = cost;
            }
        }
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null) {
            lastPurse = -1;
            return;
        }
        long now = System.currentTimeMillis();
        double purse = purse();
        lastPurse = purse;
        if (purse >= 0) {
            purseHistory.addLast(new double[]{now, purse});
            while (!purseHistory.isEmpty() && now - purseHistory.getFirst()[0] > 15_000L) purseHistory.removeFirst();
        }
        if (measureAt == 0) return;
        if (questSlayer == null) questSlayer = SlayerArea.questSlayer();
        // Always wait 3 seconds: the scoreboard's purse shows the fee a moment after the message, and with an instant
        // restart the old quest's slayer is already on the sidebar, so knowing the slayer isn't enough.
        if (now - measureAt < QUEST_COST_WINDOW_MS) return;
        long at = measureAt;
        measureAt = 0;
        questStartedAt = 0;
        if (cost() > 0) return;
        // The fee: the highest purse in the 10 s before minus the lowest since (mob kill coins coming in after the fee
        // can't hide it that way).
        double highest = -1, lowest = -1;
        for (double[] entry : purseHistory) {
            if (entry[0] >= at - 10_000L && entry[0] <= at) highest = Math.max(highest, entry[1]);
            else if (entry[0] > at) lowest = lowest < 0 ? entry[1] : Math.min(lowest, entry[1]);
        }
        long cost = highest > 0 && lowest >= 0 ? Math.round(highest - lowest) : -1;
        boolean found = cost > 0 && cost <= MAX_COST;
        // One line per check, so a log shows why a cost came out "unknown".
        System.out.println("[Sky2M] Slayer quest cost at " + measureWhat + ": " + (questSlayer == null ? "no quest seen" : questSlayer)
            + ", purse " + (highest < 0 ? "not readable" : Math.round(highest) + " -> " + Math.round(lowest))
            + (bankCost > 0 ? ", bank charge " + bankCost : "")
            + " -> " + (found ? cost : bankCost > 0 ? "bank charge" : measureWhat.equals("quest start") ? "not yet, checking at boss spawn" : "unknown"));
        if (!found) return;
        purseCost = cost;
        if (questSlayer != null) charge(new Charge(questSlayer, cost, false));
    }

    private static void charge(Charge charge) {
        for (Consumer<Charge> listener : listeners) listener.accept(charge);
    }

    /** The purse (or Piggy) from the scoreboard, or -1. */
    public static double purse() {
        String value = Sky2MLocation.scoreboardValue("Purse:");
        if (value == null) value = Sky2MLocation.scoreboardValue("Piggy:");
        if (value == null) return -1;
        Matcher m = NUMBER.matcher(value);
        if (!m.find()) return -1;
        try {
            return Double.parseDouble(m.group().replace(",", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** "1.9k", "250,000", "1.2M" -> coins (SkyHanni's formatDouble). */
    private static double parseCoins(String text) {
        String t = text.trim().replace(",", "").toLowerCase(java.util.Locale.ROOT);
        double scale = t.endsWith("k") ? 1e3 : t.endsWith("m") ? 1e6 : t.endsWith("b") ? 1e9 : 1;
        if (scale != 1) t = t.substring(0, t.length() - 1);
        try {
            return Double.parseDouble(t) * scale;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
