package com.epic60869.sky2m.features.combat;

import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.features.FeatureConfigs;
import com.epic60869.sky2m.features.itemlist.ItemListData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * /s2 rng: a searchable table of every drop that Magic Find, Pet Luck or Overbloom changes (all mob drops with a chance
 * in the NEU item list, plus {@link RngSupplement} for Garden pests), with the chance and average tries after your
 * stats. Each row is badged with the stats that affect it and the formula that applied is in its tooltip (see
 * {@link RngCommand.Formula}). Values and the filter are remembered in the config.
 */
public final class RngScreen extends Screen {
    private static final int ROW = 12;
    private static final String[] FILTERS = {"All", "Magic Find", "Pet Luck", "Overbloom"};

    /** {@code tags}: {Magic Find, Pet Luck, Overbloom}. */
    private record Row(String name, String source, RngCommand.Formula formula, double base, boolean[] tags) {}

    private final String initialSearch;
    private final List<Row> all = new ArrayList<>();
    private boolean built;
    private EditBox magicFind;
    private EditBox petLuck;
    private EditBox overbloom;
    private EditBox looting;
    private EditBox luck;
    private EditBox search;
    private Button[] filterButtons;
    private Button unaffectedButton;
    private int filter;
    private boolean showUnaffected;
    private int scroll;

    /** {@code search} null keeps the remembered search text. */
    public RngScreen(String search) {
        super(Component.literal("RNG Drop Chances"));
        this.initialSearch = search;
    }

    private static FeatureConfigs.RareDrops store() {
        Sky2MConfig c = Sky2MConfig.current();
        return c == null ? null : c.combat.rareDrops;
    }

    private int panelW() {
        return Math.min(520, width - 20);
    }

    private int left() {
        return (width - panelW()) / 2;
    }

    private int tableTop() {
        return 122;
    }

    private int tableBottom() {
        return height - 30;
    }

    private EditBox box(String label, int x, int w, String value) {
        EditBox b = new EditBox(font, x, 36, w, 18, Component.literal(label));
        b.setMaxLength(12);
        b.setValue(value);
        b.setHint(Component.literal("0").withStyle(ChatFormatting.DARK_GRAY));
        b.setResponder(v -> scroll = 0);
        addRenderableWidget(b);
        return b;
    }

    private void refreshButtons() {
        for (int i = 0; i < filterButtons.length; i++) {
            filterButtons[i].setMessage(Component.literal(i == filter ? "> " + FILTERS[i] + " <" : FILTERS[i]));
        }
        unaffectedButton.setMessage(Component.literal("Unaffected: " + (showUnaffected ? "on" : "off")));
    }

    @Override
    protected void init() {
        clearWidgets();
        FeatureConfigs.RareDrops s = store();
        int l = left();
        int w = panelW();
        // Keep what was typed on a resize.
        String mf = magicFind != null ? magicFind.getValue() : s == null ? "" : s.rngMagicFind;
        String pl = petLuck != null ? petLuck.getValue() : s == null ? "" : s.rngPetLuck;
        String ob = overbloom != null ? overbloom.getValue() : s == null ? "" : s.rngOverbloom;
        String lo = looting != null ? looting.getValue() : s == null ? "" : s.rngLooting;
        String lu = luck != null ? luck.getValue() : s == null ? "" : s.rngLuck;
        String q = search != null ? search.getValue() : initialSearch != null ? initialSearch : s == null ? "" : s.rngSearch;
        int bw = (w - 4 * 6) / 5;
        magicFind = box("Magic Find", l, bw, mf);
        petLuck = box("Pet Luck", l + (bw + 6), bw, pl);
        overbloom = box("Overbloom", l + (bw + 6) * 2, bw, ob);
        looting = box("Looting", l + (bw + 6) * 3, bw, lo);
        luck = box("Luck", l + (bw + 6) * 4, bw, lu);
        search = new EditBox(font, l, 62, w, 18, Component.literal("Search"));
        search.setMaxLength(64);
        search.setValue(q);
        search.setHint(Component.literal("Search drops or sources...").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(v -> scroll = 0);
        addRenderableWidget(search);
        filterButtons = new Button[FILTERS.length];
        for (int i = 0; i < FILTERS.length; i++) {
            final int index = i;
            filterButtons[i] = Button.builder(Component.literal(FILTERS[i]), b -> {
                filter = index;
                scroll = 0;
                refreshButtons();
            }).bounds(l + (bw + 6) * i, 86, bw, 18).build();
            addRenderableWidget(filterButtons[i]);
        }
        unaffectedButton = Button.builder(Component.literal("Unaffected"), b -> {
            showUnaffected = !showUnaffected;
            scroll = 0;
            refreshButtons();
        }).bounds(l + (bw + 6) * 4, 86, bw, 18).build();
        addRenderableWidget(unaffectedButton);
        refreshButtons();
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(width / 2 - 50, height - 26, 100, 20).build());
        setInitialFocus(search);
    }

    private static boolean pet(ItemListData.Entry item) {
        return item != null && item.category() == ItemListData.Category.PET;
    }

    private static double chanceOf(ItemListData.Drop d) {
        double base = RngCommand.parseChance(d.chance());
        if (base <= 0 && d.extra() != null) {
            for (String e : d.extra()) {
                base = RngCommand.parseChance(e);
                if (base > 0) break;
            }
        }
        return base;
    }

    private void build() {
        all.clear();
        Set<String> seen = new HashSet<>();
        for (ItemListData.Entry mob : ItemListData.entries()) {
            if (mob.category() != ItemListData.Category.MOB) continue;
            for (ItemListData.Recipe r : ItemListData.usages(mob.id())) {
                if (!(r instanceof ItemListData.Drops drops) || !mob.id().equals(drops.owner())) continue;
                String source = ItemListData.strip(drops.name() == null ? mob.name() : drops.name());
                for (ItemListData.Drop d : drops.drops()) {
                    String id = d.item().id();
                    if (id.isEmpty() || id.equals("SKYBLOCK_COIN")) continue;
                    double base = chanceOf(d);
                    if (base <= 0) continue;
                    ItemListData.Entry item = ItemListData.entry(id);
                    String name = item == null ? id : ItemListData.strip(item.name());
                    if (!seen.add((name + "|" + source + "|" + base).toLowerCase(Locale.ROOT))) continue;
                    RngCommand.Formula formula = RngCommand.formulaFor(name, source, pet(item));
                    // Farming shows only the rare pest drops (the 15% in-season rare crops come from RngSupplement).
                    if ((formula == RngCommand.Formula.PEST || formula == RngCommand.Formula.PEST_PET) && base > 0.05) continue;
                    all.add(new Row(name, source, formula, base, RngCommand.affectedBy(formula, base)));
                }
            }
        }
        for (RngSupplement.Drop d : RngSupplement.drops()) {
            // The item list may already have it (same drop from the same pest).
            boolean have = false;
            for (Row row : all) {
                if (row.name().equalsIgnoreCase(d.name()) && row.source().toLowerCase(Locale.ROOT).contains(d.source().substring(d.source().indexOf(' ') + 1).toLowerCase(Locale.ROOT))) {
                    have = true;
                    break;
                }
            }
            if (!have) all.add(new Row(d.name(), d.source(), d.formula(), d.chance(), RngCommand.affectedBy(d.formula(), d.chance())));
        }
        all.sort(Comparator.comparing((Row r) -> r.name().toLowerCase(Locale.ROOT)).thenComparing(r -> r.source().toLowerCase(Locale.ROOT)));
        built = true;
    }

    private static double num(EditBox b) {
        try {
            String t = b.getValue();
            return t.isEmpty() || t.equals(".") ? 0 : Math.min(100000, Double.parseDouble(t));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<Row> cachedRows = List.of();
    private String cachedKey;

    /** The rows for the current search and filter; recomputed only when those change, not every frame. */
    private List<Row> filtered() {
        String key = search.getValue() + '\u0000' + filter + '\u0000' + showUnaffected + '\u0000' + all.size();
        if (!key.equals(cachedKey)) {
            cachedRows = computeFiltered();
            cachedKey = key;
        }
        return cachedRows;
    }

    private List<Row> computeFiltered() {
        String f = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Row> out = new ArrayList<>();
        for (Row r : all) {
            boolean any = r.tags()[0] || r.tags()[1] || r.tags()[2];
            if (filter == 0 ? !any && !showUnaffected : !r.tags()[filter - 1]) continue;
            if (!f.isEmpty() && !r.name().toLowerCase(Locale.ROOT).contains(f) && !r.source().toLowerCase(Locale.ROOT).contains(f)
                && !r.formula().label.toLowerCase(Locale.ROOT).contains(f)) continue;
            out.add(r);
        }
        return out;
    }

    private String clip(String s, int w) {
        if (font.width(s) <= w) return s;
        while (s.length() > 1 && font.width(s + "..") > w) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    @Override
    public void tick() {
        if (!built) {
            if (ItemListData.loaded()) build();
            else ItemListData.ensureLoaded();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int l = left();
        int w = panelW();
        int r = l + w;
        int bw = (w - 4 * 6) / 5;
        List<Row> rows = built ? filtered() : List.of();
        g.centeredText(font, "RNG Drop Chances", width / 2, 6, 0xFFFFD35A);
        g.centeredText(font, built ? rows.size() + " of " + all.size() + " drops" : "Loading...", width / 2, 17, 0xFF9AA5B8);
        String[] labels = {"Magic Find", "Pet Luck", "Overbloom", "Looting lvl", "Luck lvl (armor)"};
        for (int i = 0; i < 5; i++) g.text(font, clip(labels[i], bw), l + (bw +6) * i, 27, 0xFFAAAAAA, false);

        int top = tableTop();
        int bottom = tableBottom();
        g.text(font, "Drop", l + 6, top - 12, 0xFFFFFFFF, false);
        g.text(font, "Source", l + 130, top - 12, 0xFFFFFFFF, false);
        g.text(font, "Stats", l + 232, top - 12, 0xFFFFFFFF, false);
        g.text(font, "Base", l + 285, top - 12, 0xFFFFFFFF, false);
        g.text(font, "Now", l + 345, top - 12, 0xFFFFFFFF, false);
        g.text(font, "Avg tries", l + 415, top - 12, 0xFFFFFFFF, false);
        g.fill(l, top, r, bottom, 0xC0121722);
        g.outline(l, top, w, bottom - top, 0xFF3B465B);

        if (!built) {
            String status = ItemListData.status();
            g.centeredText(font, status.isEmpty() ? "Loading the item list (it holds the drop chances)..." : status, width / 2, top + 20, 0xFFAAAAAA);
            g.centeredText(font, "This opens by itself once it's ready.", width / 2, top + 34, 0xFF666666);
            return;
        }
        if (rows.isEmpty()) {
            g.centeredText(font, all.isEmpty() ? "No drop chances found in the item list yet." : "No drop matches.", width / 2, top + 20, 0xFFFF5555);
            return;
        }
        double mf = num(magicFind), pl = num(petLuck), ob = num(overbloom), lo = num(looting), lu = num(luck);
        int visible = (bottom - top - 4) / ROW;
        scroll = Math.max(0, Math.min(scroll, rows.size() - visible));
        Row hovered = null;
        g.enableScissor(l + 1, top + 1, r - 1, bottom - 1);
        for (int i = scroll; i < Math.min(rows.size(), scroll + visible + 1); i++) {
            Row row = rows.get(i);
            int y = top + 2 + (i - scroll) * ROW;
            boolean hover = mouseX >= l && mouseX < r && mouseY >= y && mouseY < y + ROW && mouseY < bottom && mouseY >= top;
            if (hover) {
                g.fill(l + 1, y, r - 1, y + ROW, 0x20FFFFFF);
                hovered = row;
            }
            double now = RngCommand.chance(row.formula(), row.base(), mf, pl, ob, lo, lu);
            boolean boosted = now > row.base() * 1.0000001;
            g.text(font, clip(row.name(), 120), l + 6, y + 2, 0xFF55FFFF, false);
            g.text(font, clip(row.source(), 98), l + 130, y + 2, 0xFFAAAAAA, false);
            // Badges: M = Magic Find, P = Pet Luck, O = Overbloom (dim when it doesn't apply).
            g.text(font, "M", l + 232, y + 2, row.tags()[0] ? 0xFF55FF55 : 0xFF444444, false);
            g.text(font, "P", l + 241, y + 2, row.tags()[1] ? 0xFFFF55FF : 0xFF444444, false);
            g.text(font, "O", l + 250, y + 2, row.tags()[2] ? 0xFFFFAA00 : 0xFF444444, false);
            g.text(font, RngCommand.percent(row.base()), l + 285, y + 2, 0xFF888888, false);
            g.text(font, RngCommand.percent(now), l + 345, y + 2, boosted ? 0xFF55FF55 : 0xFFCCCCCC, false);
            g.text(font, clip("1 in " + String.format(Locale.US, "%,.0f", 1.0 / now), r - 4 - (l + 415)), l + 415, y + 2, 0xFFCCCCCC, false);
        }
        g.disableScissor();
        if (hovered != null && mouseX >= l && mouseX < r && mouseY >= top && mouseY < bottom) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hovered.name() + " (" + hovered.source() + ")").withStyle(ChatFormatting.AQUA));
            tip.add(Component.literal(hovered.formula().label + ": " + hovered.formula().formula).withStyle(ChatFormatting.YELLOW));
            tip.add(Component.literal(hovered.formula().note).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("M = Magic Find, P = Pet Luck, O = Overbloom").withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.literal("Base " + RngCommand.chanceText(hovered.base())).withStyle(ChatFormatting.DARK_GRAY));
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) Math.signum(scrollY) * 2;
        if (scroll < 0) scroll = 0;
        return true;
    }

    @Override
    public void removed() {
        FeatureConfigs.RareDrops s = store();
        if (s == null || magicFind == null) return;
        s.rngMagicFind = magicFind.getValue();
        s.rngPetLuck = petLuck.getValue();
        s.rngOverbloom = overbloom.getValue();
        s.rngLooting = looting.getValue();
        s.rngLuck = luck.getValue();
        s.rngSearch = search.getValue();
        Sky2MConfig.saveCurrent(Sky2MConfig.current());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
