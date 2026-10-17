package com.epic60869.sky2m.features.misc.profit;

import com.epic60869.sky2m.custom.RepoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Editing one item of a profit tracker by hand: set how many there are, or remove it. Opened by clicking an item in
 * the tracker shown over a menu, or in /s2 session's drops. The change is applied to every list in {@code targets}
 * as the same difference (so Session, Today and Total stay in step), and Remove takes the item out of all of them.
 * Closing goes back to the screen it came from.
 */
final class ProfitItemEditScreen extends Screen {
    private static final int W = 240, H = 118;

    private final Screen parent;
    private final String id;
    private final long amount;
    private final List<ProfitTracker.Stats> targets;
    private final String note;
    private final Runnable changed;
    private EditBox box;
    private String error = "";

    /** {@code targets}' first entry is the list the player clicked in; {@code amount} is its count there. */
    ProfitItemEditScreen(Screen parent, String id, long amount, List<ProfitTracker.Stats> targets, String note, Runnable changed) {
        super(Component.literal("Edit Tracked Item"));
        this.parent = parent;
        this.id = id;
        this.amount = amount;
        this.targets = targets;
        this.note = note;
        this.changed = changed;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    protected void init() {
        int x = left() + 10, y = top() + 46;
        box = new EditBox(font, x + 60, y, W - 80, 18, Component.literal("Amount"));
        box.setMaxLength(13);
        box.setValue(Long.toString(amount));
        addRenderableWidget(box);
        setInitialFocus(box);
        int by = top() + H - 28, bw = (W - 28) / 3;
        addRenderableWidget(Button.builder(Component.literal("Save").withStyle(ChatFormatting.GREEN), b -> save())
            .bounds(x, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Remove").withStyle(ChatFormatting.RED), b -> remove())
            .bounds(x + bw + 4, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
            .bounds(x + (bw + 4) * 2, by, bw, 20).build());
    }

    private void save() {
        long wanted;
        try {
            wanted = Long.parseLong(box.getValue().replace(",", "").trim());
        } catch (NumberFormatException e) {
            error = "Type a number (0 removes it).";
            return;
        }
        long difference = wanted - amount;
        for (int i = 0; i < targets.size(); i++) {
            ProfitTracker.Stats stats = targets.get(i);
            if (stats == null) continue;
            long next = i == 0 ? wanted : Math.max(0, stats.items.getOrDefault(id, 0L) + difference);
            if (next <= 0) stats.items.remove(id);
            else stats.items.put(id, next);
        }
        changed.run();
        onClose();
    }

    private void remove() {
        for (ProfitTracker.Stats stats : targets) if (stats != null) stats.items.remove(id);
        changed.run();
        onClose();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || key == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
            save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = left(), y = top();
        g.fill(x, y, x + W, y + H, 0xF0181B21);
        g.outline(x, y, W, H, 0xFF3A3F48);
        g.fill(x, y, x + W, y + 2, 0xFFFFD35A);
        g.item(RepoItems.itemStack(id), x + 10, y + 10);
        g.text(font, font.plainSubstrByWidth(ProfitTrackerHud.name(id), W - 44), x + 32, y + 10, 0xFFFFFFFF, true);
        g.text(font, "Tracked: " + String.format(Locale.US, "%,d", amount), x + 32, y + 21, 0xFF9AA5B8, false);
        g.text(font, "Amount:", x + 10, y + 51, 0xFFE0E0E0, false);
        String line = error.isEmpty() ? note : error;
        g.text(font, font.plainSubstrByWidth(line, W - 20), x + 10, y + 70, error.isEmpty() ? 0xFF7A8290 : 0xFFFF5555, false);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
