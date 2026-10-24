package com.epic60869.sky2m.gametest;

import com.epic60869.sky2m.Sky2MConfig;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens /s2's settings from the title screen at 1920x1057 with GUI scale 2 and screenshots every category (and
 * sub-category), then a few searches that show long option names and descriptions, to check each page is readable.
 * Screenshots land in build/run/clientGameTest/screenshots as "settings-NN" and "search-NN".
 */
public class Sky2MSettingsGameTest implements FabricClientGameTest {
    private static final List<String> SEARCHES = List.of("Waypoint Type", "Show What's New", "HUD Theme",
        "Highlight Blood Ichor", "Alert When Killed by a Fishing Boss", "Enter Confirms Signs", "Compact Tab List");

    /** Dropdown values wider than this are shrunk in the dropdown box. */
    private static final int DROPDOWN_MAX = 13;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> listLongDropdowns(Sky2MConfig.current(), "", new java.util.IdentityHashMap<>()));
        // Every setting in the shipped defaults must still exist after loading, or its default is silently lost.
        List<String> dropped = ctx.computeOnClient(mc -> droppedDefaults());
        for (String path : dropped) System.out.println("[Sky2M gametest] default setting not in the config: " + path);
        if (!dropped.isEmpty()) throw new AssertionError(dropped.size() + " settings in defaults/sky2m-mod.json aren't in the loaded config, e.g. " + dropped.get(0));
        ctx.getInput().resizeWindow(1920, 1057);
        ctx.runOnClient(mc -> mc.options.guiScale().set(2));
        ctx.waitTicks(10);
        ctx.runOnClient(mc -> Sky2MConfig.openGui());
        ctx.waitTicks(20);
        List<String> names = ctx.computeOnClient(mc -> new ArrayList<>(editor().getAllCategories().keySet()));
        for (int i = 0; i < names.size(); i++) {
            String key = names.get(i);
            ctx.runOnClient(mc -> editor().setSelectedCategory(editor().getAllCategories().get(key)));
            ctx.waitTicks(5);
            shot(ctx, String.format("settings-%02d", i), key);
        }
        for (int i = 0; i < SEARCHES.size(); i++) {
            String search = SEARCHES.get(i);
            ctx.runOnClient(mc -> editor().search(search));
            ctx.waitTicks(10);
            // A match inside a sub-category only shows once that sub-category is opened.
            List<String> found = ctx.computeOnClient(mc -> new ArrayList<>(editor().getCurrentlySearchedCategories().keySet()));
            for (int j = 0; j < found.size(); j++) {
                String key = found.get(j);
                ctx.runOnClient(mc -> editor().setSelectedCategory(editor().getAllCategories().get(key)));
                ctx.waitTicks(5);
                shot(ctx, String.format("search-%02d-%d", i, j), search + " in " + key);
                // Open the first section (e.g. Waypoints & Routes) so the options inside are in the screenshot too.
                ctx.getInput().setCursorPos(900, 275);
                ctx.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);
                ctx.waitTicks(5);
                shot(ctx, String.format("search-%02d-%d-open", i, j), search + " in " + key + " (opened)");
            }
        }
        // A long dropdown value, closed and then opened (click the dropdown box).
        ctx.runOnClient(mc -> Sky2MConfig.current().gui.hudTheme = com.epic60869.sky2m.features.core.Sky2MTheme.HudTheme.TRITANOPIA);
        ctx.runOnClient(mc -> editor().search("HUD Theme"));
        ctx.waitTicks(10);
        shot(ctx, "dropdown-closed", "HUD Theme");
        ctx.getInput().setCursorPos(890, 369);
        ctx.getInput().pressMouse(org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT);
        ctx.waitTicks(5);
        shot(ctx, "dropdown-open", "HUD Theme");
        ctx.runOnClient(mc -> Sky2MConfig.current().gui.hudTheme = com.epic60869.sky2m.features.core.Sky2MTheme.HudTheme.DEFAULT);
        ctx.runOnClient(mc -> editor().search(""));
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);
    }

    /** Paths in the bundled default settings that the loaded (migrated) config doesn't have. */
    private static List<String> droppedDefaults() {
        List<String> out = new ArrayList<>();
        try (var in = Sky2MConfig.class.getResourceAsStream("/assets/sky2m/defaults/sky2m-mod.json")) {
            if (in == null) throw new AssertionError("defaults/sky2m-mod.json is missing from the jar");
            var defaults = com.google.gson.JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var loaded = Sky2MConfig.exportJson();
            if (loaded == null) throw new AssertionError("Couldn't export the loaded config");
            compareKeys(defaults, loaded, "", out);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        return out;
    }

    private static void compareKeys(com.google.gson.JsonObject expected, com.google.gson.JsonObject actual, String path, List<String> out) {
        for (var entry : expected.entrySet()) {
            String key = path + entry.getKey();
            if (!actual.has(entry.getKey())) {
                out.add(key);
            } else if (entry.getValue().isJsonObject() && actual.get(entry.getKey()).isJsonObject()) {
                compareKeys(entry.getValue().getAsJsonObject(), actual.getAsJsonObject(entry.getKey()), key + ".", out);
            }
        }
    }

    /** Prints every dropdown setting whose values (as the dropdown shows them) are longer than {@link #DROPDOWN_MAX}. */
    private static void listLongDropdowns(Object node, String path, java.util.Map<Object, Boolean> seen) {
        if (node == null || seen.put(node, true) != null) return;
        for (Class<?> type = node.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(node);
                    if (field.isAnnotationPresent(io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown.class)
                        && field.getType().isEnum()) {
                        for (Object constant : field.getType().getEnumConstants()) {
                            String shown = net.minecraft.ChatFormatting.stripFormatting(constant.toString());
                            if (shown.length() > DROPDOWN_MAX) {
                                System.out.println("[Sky2M gametest] long dropdown " + path + field.getName() + " "
                                    + field.getType().getName() + "." + ((Enum<?>) constant).name() + " = \"" + shown + "\" (" + shown.length() + ")");
                            }
                        }
                    } else if (value != null && field.isAnnotationPresent(com.google.gson.annotations.Expose.class)
                        && value.getClass().getName().startsWith("com.epic60869")) {
                        listLongDropdowns(value, path + field.getName() + ".", seen);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {}
            }
        }
    }

    private static void shot(ClientGameTestContext ctx, String file, String what) {
        System.out.println("[Sky2M gametest] screenshot " + file + " (" + what.replaceAll(".*\\.", "") + ") -> " + ctx.takeScreenshot(file));
    }

    @SuppressWarnings("unchecked")
    private static MoulConfigEditor<Sky2MConfig> editor() {
        try {
            var field = Sky2MConfig.class.getDeclaredField("editor");
            field.setAccessible(true);
            return (MoulConfigEditor<Sky2MConfig>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Couldn't reach Sky2M's settings editor", e);
        }
    }
}
