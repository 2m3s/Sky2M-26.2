package com.epic60869.sky2m.features.sbc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared, server-relayed name-tag lines for Sky2M users. */
public final class SbcNameTags {
    private record Entry(String username, String line) {}
    private static final Map<UUID, Entry> LINES = new ConcurrentHashMap<>();
    private static long lastSent;
    private static String lastLine = "";
    private static String lastBoard = "";
    private SbcNameTags() {}

    public static void onConnected() {
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "nameTagGet");
        SbcNet.sendQuietly(packet);
        lastSent = 0;
    }

    /** Publish at most every 30 seconds; the backend adds selected collection standings. */
    public static void publish(String line, String collectionId) {
        if (!SbcNet.online()) return;
        long now = System.currentTimeMillis();
        String clean = ChatFormatting.stripFormatting(line == null ? "" : line);
        if (clean == null) clean = "";
        clean = clean.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.length() > 100) clean = clean.substring(0, 100);
        String board = collectionId == null ? "" : collectionId.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "");
        if (now - lastSent < 30_000L && clean.equals(lastLine) && board.equals(lastBoard)) return;
        lastSent = now; lastLine = clean; lastBoard = board;
        JsonObject packet = new JsonObject();
        packet.addProperty("type", "nameTagPublish");
        packet.addProperty("line", clean);
        packet.addProperty("collectionId", board);
        SbcNet.sendQuietly(packet);
    }

    public static void handle(String type, JsonObject packet) {
        if ("nameTagTable".equals(type)) {
            LINES.clear();
            JsonArray entries = Sbc.arr(packet, "entries");
            for (var element : entries) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                UUID uuid = Sbc.uuid(Sbc.str(item, "uuid"));
                if (uuid == null) continue;
                LINES.put(uuid, new Entry(Sbc.str(item, "username"), Sbc.str(item, "line")));
            }
            return;
        }
        UUID uuid = Sbc.uuid(Sbc.str(packet, "uuid"));
        if (uuid == null) return;
        String line = Sbc.str(packet, "line");
        String username = Sbc.str(packet, "username");
        if (line.isBlank()) LINES.remove(uuid);
        else LINES.put(uuid, new Entry(username, line));
    }

    public static Component line(UUID uuid) {
        Entry entry = LINES.get(uuid);
        return entry == null || entry.line().isBlank() ? null : Component.literal(entry.line());
    }
}