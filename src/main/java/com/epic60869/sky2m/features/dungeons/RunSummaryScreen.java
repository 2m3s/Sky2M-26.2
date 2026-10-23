package com.epic60869.sky2m.features.dungeons;

import com.epic60869.sky2m.features.dungeons.RunHistory.Run;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Run Summary card (/s2 runsummary or [View Summary] in chat): the run's floor, time, score, secrets, deaths,
 * splits, party and chest profit, with buttons to send a text version to party, guild or Sky2M chat.
 */
final class RunSummaryScreen extends Screen {
    private static final int W = 380, BG = 0xE0121722, EDGE = 0xFF3B465B, GOLD = 0xFFFFD35A, TEXT = 0xFF9AA5B8,
        BUTTON = 0xFF2A3345, WHITE = 0xFFFFFFFF;

    private final Run run;
    private final List<int[]> buttons = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();

    RunSummaryScreen(Run run) {
        super(Component.literal("Run Summary"));
        this.run = run;
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, String label, int mouseX, int mouseY, Runnable action) {
        int h = 16;
        boolean over = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        g.fill(x, y, x + w, y + h, over ? 0xFF3B5B8B : BUTTON);
        g.outline(x, y, w, h, EDGE);
        g.centeredText(font, label, x + w / 2, y + 4, WHITE);
        buttons.add(new int[]{x, y, w, h});
        actions.add(action);
    }

    private int stat(GuiGraphicsExtractor g, int x, int y, String label, String value, int colour) {
        g.text(font, label + ": ", x, y, TEXT, true);
        g.text(font, value, x + font.width(label + ": "), y, colour, true);
        return y + 10;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        buttons.clear();
        actions.clear();
        int left = (width - W) / 2, top = 20, bottom = height - 20;
        g.fill(left, top, left + W, bottom, BG);
        g.outline(left, top, W, bottom - top, EDGE);
        g.centeredText(font, "Run Summary", width / 2, top + 6, GOLD);
        int x = left + 10, half = left + W / 2 + 6, y = top + 22;

        String floor = run.floor == null ? "?" : run.floor;
        g.text(font, floor + (run.completed ? " cleared" : " run") + "  " + run.date(), x, y, floor.startsWith("M") ? 0xFFFF5555 : 0xFF55FF55, true);
        y += 14;
        int ly = y;
        ly = stat(g, x, ly, "Time", RunHistory.time(run.totalMs) + (run.bestTime ? " (PB)" : ""), WHITE);
        if (run.effectiveScore() >= 0) ly = stat(g, x, ly, "Score", run.effectiveScore() + " (" + run.effectiveRank() + ")" + (run.bestScore ? " (PB)" : ""), 0xFF55FFFF);
        if (run.secrets >= 0) ly = stat(g, x, ly, "Secrets", run.secrets + String.format(Locale.ROOT, " (%.0f%%)", Math.max(0, run.secretPercent)), WHITE);
        if (run.crypts >= 0) ly = stat(g, x, ly, "Crypts", String.valueOf(run.crypts), WHITE);
        int deaths = run.deathLog != null ? run.deathLog.size() : Math.max(0, run.deaths);
        ly = stat(g, x, ly, "Deaths", String.valueOf(deaths), deaths > 0 ? 0xFFFF5555 : WHITE);
        if (RunSummary.hasChests(run)) {
            double profit = RunSummary.profit(run);
            ly = stat(g, x, ly, "Chest profit", RunSummary.coins(profit), profit >= 0 ? 0xFF55FF55 : 0xFFFF5555);
        }
        ly += 6;
        g.text(font, "Party", x, ly, GOLD, true);
        ly += 11;
        for (RunHistory.Member m : run.party) {
            g.text(font, m.name() + (m.dungeonClass() == null ? "" : " §7(" + m.dungeonClass() + ")"), x + 4, ly, WHITE, true);
            ly += 10;
        }

        int ry = y;
        g.text(font, "Splits", half, ry, GOLD, true);
        ry += 11;
        if (run.splits.isEmpty()) {
            g.text(font, "None recorded", half + 4, ry, TEXT, true);
            ry += 10;
        }
        for (RunHistory.Split s : run.splits) {
            if (ry > bottom - 60) break;
            ry = stat(g, half + 4, ry, s.name(), RunHistory.time(s.ms()), WHITE);
        }
        if (run.deathLog != null && !run.deathLog.isEmpty()) {
            ry += 6;
            g.text(font, "Deaths", half, ry, GOLD, true);
            ry += 11;
            for (DeathRecap.DeathEntry d : run.deathLog) {
                if (ry > bottom - 36) break;
                g.text(font, font.plainSubstrByWidth(DeathRecap.line(d), W / 2 - 20), half + 4, ry, 0xFFFF8888, true);
                ry += 10;
            }
        }

        int by = bottom - 24, bw = (W - 20 - 12) / 4;
        button(g, left + 10, by, bw, "Share: Party", mouseX, mouseY, () -> RunSummary.share(run, "party"));
        button(g, left + 10 + (bw + 4), by, bw, "Share: Guild", mouseX, mouseY, () -> RunSummary.share(run, "guild"));
        button(g, left + 10 + 2 * (bw + 4), by, bw, "Share: /s2c", mouseX, mouseY, () -> RunSummary.share(run, "s2c"));
        button(g, left + 10 + 3 * (bw + 4), by, bw, "Close", mouseX, mouseY, this::onClose);
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
    public boolean isPauseScreen() {
        return false;
    }
}
