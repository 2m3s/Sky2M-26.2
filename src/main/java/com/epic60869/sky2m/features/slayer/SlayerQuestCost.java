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
 * by around "SLAYER QUEST STARTED!" (up to 350k; Aatrox's discount and the Rift's motes come out right that way), or,
 * with auto-slayer, what "Took X coins from your bank for auto-slayer..." says. Worked out once per quest here so the
 * slayer profit trackers and the per-kill profit line agree on it.
 */
public final class SlayerQuestCost {
    /** One spawn cost paid, for {@code slayer}'s quest ("Revenant Horror"..., or null when no quest showed). */
    public record Charge(String slayer, long coins, boolean bank) {}

    /** SkyHanni's slayer.autoslayer.bank pattern: auto-slayer takes the spawn cost from your bank, not your purse. */
    private static final Pattern AUTO_SLAYER_BANK = Pattern.compile("^Took (.+) coins from your bank for auto-slayer\\.\\.\\.$");
    private static final Pattern NUMBER = Pattern.compile("[\\d,]+(?:\\.\\d+)?");
    /** How long after the quest started its slayer may take to show on the scoreboard. */
    private static final long QUEST_COST_WINDOW_MS = 3_000L;
    /** The highest cost that can be a spawn fee: anything more is some other purse change. */
    private static final long MAX_COST = 350_000L;
    /** A bank charge this close before "SLAYER QUEST STARTED!" is for that quest (Hypixel can send it first). */
    private static final long BANK_BEFORE_START_MS = 5_000L;

    /** The purse over the last 15 seconds: {time, coins}. Hypixel takes the fee before "SLAYER QUEST STARTED!". */
    private static final ArrayDeque<double[]> purseHistory = new ArrayDeque<>();
    private static final List<Consumer<Charge>> listeners = new ArrayList<>();
    private static double lastPurse = -1;
    private static long questStartedAt;
    private static String questSlayer;
    /** The current quest's cost from the purse and from the bank, or -1 when not seen. */
    private static long purseCost = -1, bankCost = -1;
    private static long bankAt;
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

    /** The current quest's spawn cost: the auto-slayer bank charge if one came, else the purse fee; -1 if unknown. */
    public static long cost() {
        return bankCost > 0 ? bankCost : purseCost;
    }

    private static void onChat(String text) {
        if (!Sky2MLocation.onSkyblock()) return;
        long now = System.currentTimeMillis();
        if (text.equals("SLAYER QUEST STARTED!")) {
            questStartedAt = now;
            questSlayer = null;
            purseCost = -1;
            if (now - bankAt > BANK_BEFORE_START_MS) bankCost = -1;
            return;
        }
        Matcher autoSlayer = AUTO_SLAYER_BANK.matcher(text);
        if (autoSlayer.matches()) {
            long cost = Math.round(parseCoins(autoSlayer.group(1)));
            if (cost <= 0 || cost > MAX_COST) return;
            bankCost = cost;
            bankAt = now;
            charge(new Charge(SlayerArea.questSlayer(), cost, true));
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
        if (questStartedAt == 0) return;
        if (questSlayer == null) questSlayer = SlayerArea.questSlayer();
        // Wait for the quest to show (up to 3 seconds), so the fee goes to the right slayer.
        if (now - questStartedAt < QUEST_COST_WINDOW_MS && questSlayer == null) return;
        long started = questStartedAt;
        questStartedAt = 0;
        double highest = -1;
        for (double[] entry : purseHistory) if (entry[0] >= started - 10_000L) highest = Math.max(highest, entry[1]);
        long cost = highest > 0 && lastPurse >= 0 ? Math.round(highest - lastPurse) : -1;
        if (cost <= 0 || cost > MAX_COST) return;
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
