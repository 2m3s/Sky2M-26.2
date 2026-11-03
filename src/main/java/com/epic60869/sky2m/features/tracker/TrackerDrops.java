package com.epic60869.sky2m.features.tracker;

import com.epic60869.sky2m.custom.RepoItems;
import com.epic60869.sky2m.price.Sky2MPriceTooltip;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The other tabs of /s2 tracker ({@link DungeonDropScreen}): what Sky2M's own trackers have counted, read from their
 * files when a tab opens. Slayer, Fishing, Mining, Farming and Foraging come from the profit trackers
 * (config/sky2m/profit-trackers.json, all time), Diana from the Diana tracker (diana-tracker.json, all time). These
 * keep counts, not prices, so items are priced now.
 */
public final class TrackerDrops {
    /** The tabs after Dungeons, each with its parts (the left box) as {label, where its counts are}. */
    public static final String[] CATEGORIES = {"Slayer", "Fishing", "Diana", "Mining", "Farming", "Foraging"};
    private static final String[][] SLAYERS = {{"Revenant Horror", "ZOMBIE"}, {"Tarantula Broodfather", "SPIDER"},
        {"Sven Packmaster", "WOLF"}, {"Voidgloom Seraph", "ENDERMAN"}, {"Inferno Demonlord", "BLAZE"}, {"Riftstalker Bloodfiend", "VAMPIRE"}};

    /** One part of a category: its name and what was counted, id (or name) -> how many. */
    public record Part(String name, Map<String, Long> counts) {}

    private TrackerDrops() {}

    /** A category's parts. Slayer has one per boss; the others have one. */
    public static List<Part> load(String category) {
        Path dir = DungeonProfitHour.DIR.resolve("sky2m");
        List<Part> out = new ArrayList<>();
        if (category.equals("Diana")) {
            JsonObject diana = object(dir.resolve("diana-tracker.json"));
            // The Diana tracker keeps its own drop names, lootshared ones as NAME_LS: both count, under the item's id.
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Map.Entry<String, Long> e : counts(child(child(diana, "allTime"), "drops")).entrySet()) {
                counts.merge(dianaId(e.getKey()), e.getValue(), Long::sum);
            }
            out.add(new Part("Diana", counts));
            return out;
        }
        JsonObject profit = object(dir.resolve("profit-trackers.json"));
        if (category.equals("Slayer")) {
            for (String[] s : SLAYERS) out.add(new Part(s[0], counts(child(child(child(profit, s[1]), "total"), "items"))));
            return out;
        }
        out.add(new Part(category, counts(child(child(child(profit, category.toUpperCase(Locale.ROOT)), "total"), "items"))));
        return out;
    }

    /**
     * One part's rows (all parts when {@code part} is null), the most expensive first. Priced now; an item whose price
     * isn't known is worth 0 and sorts by count among the rest.
     */
    public static List<DungeonDropTracker.Row> rows(List<Part> parts, String part, String search) {
        Map<String, Long> all = new LinkedHashMap<>();
        for (Part p : parts) {
            if (part != null && !part.equals(p.name())) continue;
            for (Map.Entry<String, Long> e : p.counts().entrySet()) all.merge(e.getKey(), e.getValue(), Long::sum);
        }
        List<DungeonDropTracker.Drop> drops = new ArrayList<>();
        for (Map.Entry<String, Long> e : all.entrySet()) {
            if (e.getValue() <= 0) continue;
            DungeonDropTracker.Drop d = new DungeonDropTracker.Drop();
            String key = e.getKey();
            boolean isId = key.equals(key.toUpperCase(Locale.ROOT)) && !key.contains(" ");
            d.id = isId ? key : idFor(key);
            d.name = isId ? nameFor(key) : key;
            d.count = (int) Math.min(Integer.MAX_VALUE, e.getValue());
            d.value = d.id.isEmpty() ? 0 : price(d.id) * e.getValue();
            drops.add(d);
        }
        return DungeonDropTracker.rows(drops, null, search);
    }

    /** "ENCHANTMENT_CRITICAL_6" -> "Critical VI", else the item's name, else the id tidied up. */
    static String nameFor(String id) {
        if (id.startsWith("ENCHANTMENT_")) return DungeonDropTracker.displayName("Enchanted Book", id);
        String name = RepoItems.itemsLoaded() ? RepoItems.displayName(id.replace(':', '-')) : null;
        if (name != null && !name.isBlank()) return TrackLines.strip(name);
        StringBuilder out = new StringBuilder();
        for (String word : id.split("[_:;-]")) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    /** "CHIMERA" / "CHIMERA_LS" -> "ENCHANTMENT_ULTIMATE_CHIMERA_1" (Sky2M's Diana drop list); anything else as it is. */
    private static String dianaId(String key) {
        String name = key.endsWith("_LS") ? key.substring(0, key.length() - 3) : key;
        for (com.epic60869.sky2m.features.combat.DianaTracker.Drop d : com.epic60869.sky2m.features.combat.DianaTracker.Drop.values()) {
            if (d.name().equals(name)) return d.itemId;
        }
        return name;
    }

    /** A price id for a counted name: "Sphinx Shard" -> "SHARD_SPHINX", else the item by that name, or "". */
    private static String idFor(String name) {
        if (name.endsWith(" Shard")) return "SHARD_" + name.substring(0, name.length() - 6).trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        String id = RepoItems.itemsLoaded() ? RepoItems.idByLooseName(name) : null;
        return id == null ? "" : id;
    }

    /** What one is worth now; fishing ids like "RAW_FISH:1" are tried as "RAW_FISH-1" too. 0 if unknown. */
    private static double price(String id) {
        double p = Sky2MPriceTooltip.unitPrice(id);
        if (p <= 0 && id.contains(":")) p = Sky2MPriceTooltip.unitPrice(id.replace(':', '-'));
        return Math.max(0, p);
    }

    private static Map<String, Long> counts(JsonObject json) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (json == null) return out;
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) out.put(e.getKey(), e.getValue().getAsLong());
        }
        return out;
    }

    private static JsonObject child(JsonObject json, String key) {
        return json != null && json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : null;
    }

    private static JsonObject object(Path file) {
        try {
            if (Files.exists(file)) return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (Exception e) {
            TrackLines.LOG.warn("Couldn't read {}: {}", file, e.toString());
        }
        return null;
    }
}
