package com.epic60869.sky2m.features.misc.profit;

import com.epic60869.sky2m.custom.RepoItems;
import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.FeatureConfigs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /s2 session: Total, Farming, Mining and Diana tabs for this game session (or a past one, with the arrows). The
 * green profit at the top is worked out again with the latest prices every Price Refresh seconds. The Total tab lists
 * the three activities; click one to open its tab.
 */
public final class SessionTrackerScreen extends Screen {
    private static final int W = 420, ROW = 18, BG = 0xC0121722, EDGE = 0xFF3B465B, GOLD = 0xFFFFD35A, TEXT = 0xFF9AA5B8,
        BUTTON = 0xFF2A3345, BUTTON_SELECTED = 0xFF3B5B8B, GREEN = 0xFF55FF55, RED = 0xFFFF5555;
    /** The tabs: the three activities (shown after Total), then Total. */
    private static final int TOTAL = SessionTracker.ACTIVITIES.length;

    /** -1 is this session, 0 the last game's, and so on. */
    private int index;
    private int tab = TOTAL;
    private int scroll;
    private boolean confirmDelete;
    private boolean showDays;
    private final List<int[]> buttons = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();

    /** The profit and item values, worked out again every Price Refresh seconds. */
    private long pricedAt;
    private final double[] profits = new double[TOTAL];
    private final boolean[] unknown = new boolean[TOTAL];
    private final List<List<Item>> items = new ArrayList<>();
    private final Map<Integer, Double> dayProfits = new java.util.HashMap<>();

    private record Item(String id, String name, long amount, double value) {}

    private SessionTrackerScreen(int index) {
        super(Component.literal("Session Tracker"));
        this.index = index;
        tab = busiestTab();
    }

    public static int open(int index) {
        Compat.queueOpenScreen(new SessionTrackerScreen(index));
        return 1;
    }

    private SessionTracker.Session session() {
        return SessionTracker.session(index);
    }

    private boolean live() {
        return index < 0;
    }

    /** The activity you did most, or Total if you did more than one (or none). */
    private int busiestTab() {
        SessionTracker.Session s = session();
        int found = -1;
        for (int i = 0; i < TOTAL; i++) {
            if (!s.has(SessionTracker.ACTIVITIES[i])) continue;
            if (found >= 0) return TOTAL;
            found = i;
        }
        return found < 0 ? TOTAL : found;
    }

    private void reprice(boolean force) {
        FeatureConfigs.SessionTrackerSettings c = SessionTracker.config();
        long every = (c == null ? 30 : Math.clamp(c.priceRefreshSeconds, 5, 300)) * 1000L;
        long now = System.currentTimeMillis();
        if (!force && now - pricedAt < every && items.size() == TOTAL) return;
        pricedAt = now;
        dayProfits.clear();
        FeatureConfigs.ProfitPriceSource source = c == null || c.priceSource == null ? FeatureConfigs.ProfitPriceSource.INSTANT_SELL : c.priceSource;
        items.clear();
        SessionTracker.Session s = session();
        for (int i = 0; i < TOTAL; i++) {
            ProfitTracker.Preset preset = SessionTracker.ACTIVITIES[i];
            ProfitTracker.Stats stats = s.get(preset);
            boolean[] u = {false};
            profits[i] = SessionTracker.profit(preset, stats, u);
            unknown[i] = u[0];
            List<Item> list = new ArrayList<>();
            if (stats != null) {
                for (Map.Entry<String, Long> e : stats.items.entrySet()) {
                    double unit = com.epic60869.sky2m.Sky2MPriceTooltip.price(e.getKey(), source);
                    list.add(new Item(e.getKey(), ProfitTrackerHud.name(e.getKey()), e.getValue(), unit > 0 ? unit * e.getValue() : -1));
                }
            }
            list.sort(Comparator.comparingDouble(Item::value).reversed());
            items.add(list);
        }
    }

    private double shownProfit() {
        if (tab < TOTAL) return profits[tab];
        double total = 0;
        for (double p : profits) total += p;
        return total;
    }

    private double dayProfit(int dayIndex, SessionTracker.Session session) {
        return dayProfits.computeIfAbsent(dayIndex, ignored -> {
            double total = 0;
            for (ProfitTracker.Preset preset : SessionTracker.ACTIVITIES) {
                total += SessionTracker.profit(preset, session.get(preset), null);
            }
            return total;
        });
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, boolean selected, int mouseX, int mouseY, Runnable action) {
        boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, selected ? BUTTON_SELECTED : over ? 0xFF33405A : BUTTON);
        g.outline(x, y, w, h, EDGE);
        g.centeredText(font, label, x + w / 2, y + (h - 8) / 2, selected ? GOLD : 0xFFFFFFFF);
        buttons.add(new int[]{x, y, w, h});
        actions.add(action);
    }

    private void go(int newIndex) {
        index = newIndex;
        scroll = 0;
        confirmDelete = false;
        tab = busiestTab();
        reprice(true);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        reprice(false);
        buttons.clear();
        actions.clear();
        int left = (width - W) / 2, top = 24, bottom = height - 16;
        SessionTracker.Session s = session();
        g.centeredText(font, "Daily Profit History", width / 2, 8, GOLD);
        g.fill(left, top, left + W, bottom, BG);
        g.outline(left, top, W, bottom - top, EDGE);
        button(g, left + W - 58, top + 4, 54, 14, showDays ? "Back" : "Days", false, mouseX, mouseY, () -> {
            showDays = !showDays;
            scroll = 0;
        });
        if (showDays) {
            g.centeredText(font, "Daily Profit History", width / 2, top + 28, GOLD);
            g.text(font, "Click a day to open its total and tracker breakdown.", left + 10, top + 44, TEXT, true);
            int rowY = top + 64;
            int maxRows = Math.max(1, (bottom - rowY - 8) / 22);
            int count = SessionTracker.pastSessions() + 1;
            scroll = Math.clamp(scroll, 0, Math.max(0, count - maxRows));
            for (int visible = 0; visible < maxRows && visible + scroll < count; visible++) {
                int dayIndex = visible + scroll == 0 ? -1 : visible + scroll - 1;
                SessionTracker.Session daySession = SessionTracker.session(dayIndex);
                int ry = rowY + visible * 22;
                boolean over = mouseX >= left + 6 && mouseX < left + W - 6 && mouseY >= ry && mouseY < ry + 19;
                g.fill(left + 6, ry, left + W - 6, ry + 19, over ? 0xFF33405A : BUTTON);
                double value = dayProfit(dayIndex, daySession);
                String label = (dayIndex < 0 ? "Today" : SessionTracker.date(daySession.started)) + "  ·  " + signed(value);
                g.text(font, font.plainSubstrByWidth(label, W - 32), left + 12, ry + 5, value >= 0 ? GREEN : RED, true);
                buttons.add(new int[]{left + 6, ry, W - 12, 19});
                actions.add(() -> {
                    index = dayIndex;
                    showDays = false;
                    scroll = 0;
                    tab = TOTAL;
                    reprice(true);
                });
            }
            return;
        }

        // Which session: this one, or a past one with the arrows (older to the right).
        int last = SessionTracker.pastSessions() - 1;
        if (index < last) button(g, left + W - 24, top + 4, 20, 14, "▶", false, mouseX, mouseY, () -> go(index + 1));
        if (index > -1) button(g, left + 4, top + 4, 20, 14, "◀", false, mouseX, mouseY, () -> go(index - 1));
        String which = live() ? "Today · " + SessionTracker.date(s.started)
            : SessionTracker.date(s.started) + " · Day " + (index + 1) + "/" + (last + 1);
        g.centeredText(font, which, width / 2, top + 7, TEXT);

        // The profit, big and green, kept up to date with the prices.
        double profit = shownProfit();
        String profitText = (profit >= 0 ? "+" : "-") + ProfitTrackerHud.coins(Math.abs(profit));
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, top + 24);
        g.pose().scale(2.5f, 2.5f);
        g.centeredText(font, profitText, 0, 0, profit >= 0 ? GREEN : RED);
        g.pose().popMatrix();
        boolean anyUnknown = tab < TOTAL ? unknown[tab] : anyUnknown();
        long ago = (System.currentTimeMillis() - pricedAt) / 1000;
        g.centeredText(font, (anyUnknown ? "Known profit" : "Profit") + " · prices updated " + ago + "s ago", width / 2, top + 50, TEXT);

        // Two compact rows keep all 11 profit presets accessible without tiny one-line tabs.
        int tabsY = top + 64, columns = 6, bw = (W - 8) / columns;
        for (int pos = 0; pos <= TOTAL; pos++) {
            int t = pos == 0 ? TOTAL : pos - 1;
            String label = t < TOTAL ? SessionTracker.name(SessionTracker.ACTIVITIES[t]) : "Total";
            button(g, left + 4 + (pos % columns) * bw, tabsY + (pos / columns) * 18, bw - 2, 16, label, tab == t, mouseX, mouseY, () -> {
                tab = t;
                scroll = 0;
            });
        }
        int y = tabsY + 40;
        if (!s.hasAny()) {
            boolean on = SessionTracker.on();
            g.centeredText(font, on ? "Nothing yet this session. Go mining, farming or do some Diana!"
                : "Turn on the Session Tracker (Profit Trackers > Session Tracker).", width / 2, y + 20, TEXT);
        } else if (tab == TOTAL) {
            total(g, left, y, bottom, mouseX, mouseY);
        } else {
            activity(g, left, y, bottom, SessionTracker.ACTIVITIES[tab]);
        }

        if (!live()) {
            button(g, left + W - 64, bottom - 18, 60, 14, confirmDelete ? "Sure?" : "Delete", confirmDelete, mouseX, mouseY, () -> {
                if (!confirmDelete) {
                    confirmDelete = true;
                    return;
                }
                SessionTracker.delete(index);
                go(Math.min(index, SessionTracker.pastSessions() - 1));
            });
        }
    }

    /** Total: the three activities as rows (click one for its tab), then everything added up. */
    private void total(GuiGraphicsExtractor g, int left, int y, int bottom, int mouseX, int mouseY) {
        SessionTracker.Session s = session();
        int[] cols = {left + 8, left + 100, left + 190, left + 290};
        String[] heads = {"Activity", "Started", "Playtime", "Profit"};
        for (int i = 0; i < heads.length; i++) g.text(font, heads[i], cols[i], y, GOLD, true);
        y += 14;
        for (int i = 0; i < TOTAL; i++) {
            ProfitTracker.Preset preset = SessionTracker.ACTIVITIES[i];
            int ry = y;
            boolean has = s.has(preset);
            boolean over = has && mouseX >= left + 2 && mouseX < left + W - 2 && mouseY >= ry - 4 && mouseY < ry + ROW - 4;
            if (over) g.fill(left + 1, ry - 4, left + W - 1, ry + ROW - 4, 0x20FFFFFF);
            int colour = 0xFF000000 | net.minecraft.network.chat.TextColor.fromLegacyFormat(SessionTracker.colour(preset)).getValue();
            String status = live() && SessionTracker.running(preset) ? " §a●" : "";
            g.text(font, SessionTracker.name(preset) + status, cols[0], ry, has ? colour : 0xFF555555, true);
            if (has) {
                ProfitTracker.Stats stats = s.get(preset);
                g.text(font, SessionTracker.date(s.firstActive(preset)).replaceFirst(" \\d{4}$", "") + " " + SessionTracker.time(s.firstActive(preset)), cols[1], ry, 0xFFFFFFFF, true);
                g.text(font, SessionTracker.duration(stats.activeMs), cols[2], ry, 0xFFFFFFFF, true);
                g.text(font, signed(profits[i]), cols[3], ry, profits[i] >= 0 ? GREEN : RED, true);
                int t = i;
                buttons.add(new int[]{left + 2, ry - 4, W - 4, ROW});
                actions.add(() -> {
                    tab = t;
                    scroll = 0;
                });
            } else {
                g.text(font, "-", cols[1], ry, 0xFF555555, true);
            }
            y += ROW;
        }
        y += 6;
        g.fill(left + 6, y, left + W - 6, y + 1, EDGE);
        y += 8;
        long playtime = s.playtime();
        double profit = shownProfit();
        y = stat(g, left + 8, y, "Date", SessionTracker.date(s.firstActiveAny()) + " at " + SessionTracker.time(s.firstActiveAny()));
        y = stat(g, left + 8, y, "Total playtime", SessionTracker.duration(playtime));
        y = stat(g, left + 8, y, "Total profit", signed(profit));
        stat(g, left + 8, y, "Profit per hour", playtime < 60_000 ? "-" : signed(profit * 3_600_000.0 / playtime));
        g.text(font, "Click an activity for its drops.", left + 8, bottom - 14, TEXT, false);
    }

    /** One activity: when, how long, its numbers and its drops. */
    private void activity(GuiGraphicsExtractor g, int left, int y, int bottom, ProfitTracker.Preset preset) {
        SessionTracker.Session s = session();
        ProfitTracker.Stats stats = s.get(preset);
        if (!s.has(preset)) {
            g.centeredText(font, "No " + SessionTracker.name(preset) + " this session.", width / 2, y + 20, TEXT);
            return;
        }
        int x = left + 8, half = left + W / 2 + 4;
        int ly = y;
        ly = stat(g, x, ly, "Date", SessionTracker.date(s.firstActive(preset)));
        ly = stat(g, x, ly, "Started", SessionTracker.time(s.firstActive(preset)));
        ly = stat(g, x, ly, "Playtime", SessionTracker.duration(stats.activeMs)
            + (live() ? SessionTracker.running(preset) ? " §a(active)" : " §7(paused)" : ""));
        int ry = y;
        ry = stat(g, half, ry, "Profit per hour", stats.activeMs < 60_000 ? "-" : signed(profits[tab] * 3_600_000.0 / stats.activeMs));
        if (stats.actions > 0) ry = stat(g, half, ry, preset.actionLabel, String.format(Locale.US, "%,d", stats.actions));
        if (stats.coins != 0) ry = stat(g, half, ry, preset.coinLabel, "§6" + ProfitTrackerHud.coins(stats.coins));
        if (stats.kernels > 0) ry = stat(g, half, ry, "Kernels", String.valueOf(stats.kernels));
        y = Math.max(ly, ry) + 6;
        g.fill(left + 6, y, left + W - 6, y + 1, EDGE);
        y += 6;

        List<Item> list = items.get(tab);
        if (list.isEmpty()) {
            g.text(font, "No drops counted yet.", x, y, TEXT, true);
            return;
        }
        int visible = Math.max(1, (bottom - 22 - y) / ROW);
        scroll = Math.clamp(scroll, 0, Math.max(0, list.size() - visible));
        for (int i = scroll; i < list.size() && i - scroll < visible; i++) {
            Item item = list.get(i);
            int iy = y + (i - scroll) * ROW;
            g.item(RepoItems.itemStack(item.id()), x, iy);
            g.text(font, font.plainSubstrByWidth(item.name(), 220), x + 20, iy + 4, 0xFFFFFFFF, true);
            g.text(font, "§7x" + String.format(Locale.US, "%,d", item.amount()), left + 250, iy + 4, 0xFFFFFFFF, true);
            String value = item.value() < 0 ? "§8Unknown" : "§6" + ProfitTrackerHud.coins(item.value());
            g.text(font, value, left + W - 10 - font.width(value), iy + 4, 0xFFFFFFFF, true);
        }
        if (list.size() > visible) {
            int trackH = visible * ROW, thumb = Math.max(10, trackH * visible / list.size());
            int thumbY = y + (trackH - thumb) * scroll / Math.max(1, list.size() - visible);
            g.fill(left + W - 4, y, left + W - 2, y + trackH, 0xFF0C0F16);
            g.fill(left + W - 4, thumbY, left + W - 2, thumbY + thumb, EDGE);
        }
    }

    private boolean anyUnknown() {
        for (boolean value : unknown) if (value) return true;
        return false;
    }

    private static String signed(double value) {
        return (value >= 0 ? "§a+" : "§c-") + ProfitTrackerHud.coins(Math.abs(value));
    }

    private int stat(GuiGraphicsExtractor g, int x, int y, String label, String value) {
        g.text(font, label + ": ", x, y, TEXT, true);
        g.text(font, value, x + font.width(label + ": "), y, 0xFFFFFFFF, true);
        return y + 11;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < buttons.size(); i++) {
            int[] b = buttons.get(i);
            if (event.x() >= b[0] && event.x() < b[0] + b[2] && event.y() >= b[1] && event.y() < b[1] + b[3]) {
                actions.get(i).run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 3);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
