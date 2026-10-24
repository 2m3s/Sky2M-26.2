package com.epic60869.sky2m.gametest;

import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.features.FeatureConfigs;
import com.epic60869.sky2m.sim.roomsim.SimBuilder;
import com.epic60869.sky2m.sim.roomsim.SimState;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads a Dungeon Sim room from the title screen, as /s2 sim does, and checks the sim's own commands work inside it
 * (/simitem opens the toolkit). Then, in that singleplayer world, opens every Sky2M screen that works offline (the HUD
 * editor, which previews every HUD, the Item List over the inventory, /s2 rng, /s2 achievements, /s2 whatsnew and
 * more), screenshots each, fails on any exception or error logged meanwhile, and leaves the world. Run with
 * `gradlew runClientGameTest`; screenshots land in build/run/clientGameTest/screenshots.
 */
public class Sky2MScreensGameTest implements FabricClientGameTest {
    private static final String ROOM = "Mural";

    /** Screens that must open: a command that leaves no screen open fails the test. */
    private static final List<String> REQUIRED = List.of("s2 gui", "s2 rng", "s2 achievements", "s2 whatsnew", "s2 calendar");

    /** Other screens that open offline: a missing screen is only reported, an exception still fails. */
    private static final List<String> OPTIONAL = List.of("s2 huds", "s2 keybinds", "s2 runs", "s2 chesthistory",
        "s2 networth", "s2 notes", "s2 collections", "s2 deaths", "s2 trophyfish", "s2 feats",
        "s2 portfolio", "s2 errors", "s2 catacalc", "s2 screenshots", "s2 shopping",
        "s2 keys", "s2 search");

    /** ERROR log lines (and their exceptions) seen while the screens were open. */
    private static final List<String> ERRORS = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ErrorCatcher catcher = ErrorCatcher.attach();
        try {
            ctx.runOnClient(mc -> SimBuilder.buildSingleRoom(mc, ROOM));
            ctx.waitFor(mc -> SimState.canAct(mc), 2400);
            ctx.waitTicks(100);
            shot(ctx, "40-sim-room");

            ctx.runOnClient(mc -> mc.player.connection.sendCommand("simitem"));
            ctx.waitTicks(10);
            String screen = screenName(ctx);
            System.out.println("[Sky2M gametest] after /simitem the screen is " + screen);
            if (!"SimItemsScreen".equals(screen)) throw new AssertionError("/simitem didn't open the sim toolkit (screen: " + screen + ")");
            shot(ctx, "41-simitem");
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);

            // The same size as the settings screenshots, so text is checked at a normal GUI scale.
            ctx.getInput().resizeWindow(1920, 1057);
            ctx.runOnClient(mc -> mc.options.guiScale().set(2));
            ctx.waitTicks(10);
            List<String> failures = new ArrayList<>();
            int n = 50;
            // First: it waits for the item data that /s2 rng and others also show.
            itemList(ctx, n, failures);
            n += 3;
            for (String command : REQUIRED) openScreen(ctx, command, n++, true, failures);
            for (String command : OPTIONAL) openScreen(ctx, command, n++, false, failures);

            discordMessage(ctx, failures);
            removedMpCommand(ctx, failures);
            keybindTooltip(ctx, failures);
            importThenGui(ctx, failures);

            if (!ERRORS.isEmpty()) {
                for (String error : ERRORS) System.out.println("[Sky2M gametest] logged error: " + error);
                failures.add(ERRORS.size() + " error(s) logged while the screens were open (see above)");
            }
            if (!failures.isEmpty()) throw new AssertionError("Sky2M screens failed:\n  " + String.join("\n  ", failures));
        } finally {
            catcher.detach();
            leaveWorld(ctx);
        }
    }

    /** Runs {@code command} as typed in chat, then screenshots whatever it opened. */
    private static void openScreen(ClientGameTestContext ctx, String command, int index, boolean required, List<String> failures) {
        String error = ctx.computeOnClient(mc -> {
            try {
                ClientCommands.getActiveDispatcher().execute(command, (FabricClientCommandSource) mc.getConnection().getSuggestionsProvider());
                return null;
            } catch (Throwable e) {
                e.printStackTrace();
                return e.toString();
            }
        });
        ctx.waitTicks(10);
        String screen = screenName(ctx);
        System.out.println("[Sky2M gametest] /" + command + " -> " + screen);
        if (error != null) failures.add("/" + command + " threw " + error);
        else if (required && "none".equals(screen)) failures.add("/" + command + " didn't open a screen");
        if (!"none".equals(screen)) shot(ctx, String.format("%02d-%s", index, command.replace(' ', '-')));
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);
    }

    /**
     * A chat message relayed from Discord, as the chat server sends it, through Sky2M's real socket handler: the line
     * must read "[S2M] [Discord] [Bob]: ..." (the server's own "[Discord] " name prefix not shown twice), Bob in blue.
     */
    private static void discordMessage(ClientGameTestContext ctx, List<String> failures) {
        String packet = "{\"type\":\"message\",\"id\":1,\"at\":" + System.currentTimeMillis() + ",\"source\":\"discord\","
            + "\"username\":\"[Discord] Bob\",\"minecraftUuid\":\"\",\"nickname\":\"[Discord] Bob\",\"nicknameEnabled\":false,"
            + "\"nicknameMode\":\"Plain\",\"nicknameHex\":\"\",\"nicknameFont\":\"Default\",\"message\":\"hello from discord\",\"mentions\":[]}";
        try {
            Class<?> listener = Class.forName("com.epic60869.sky2m.Sky2MGlobalChat$Listener");
            var constructor = listener.getDeclaredConstructor();
            constructor.setAccessible(true);
            var handle = listener.getDeclaredMethod("handle", String.class);
            handle.setAccessible(true);
            // The socket thread calls it (the gametest thread may not touch Minecraft); the line reaches chat
            // through Minecraft.execute.
            Object instance = constructor.newInstance();
            Throwable[] thrown = {null};
            Thread socket = new Thread(() -> {
                try {
                    handle.invoke(instance, packet);
                } catch (Throwable e) {
                    thrown[0] = e;
                }
            }, "Sky2M gametest socket");
            socket.start();
            socket.join(10_000);
            if (thrown[0] != null) throw new java.lang.reflect.InvocationTargetException(thrown[0]);
        } catch (ReflectiveOperationException | InterruptedException e) {
            failures.add("couldn't call Sky2MGlobalChat's message handler: " + e);
            return;
        }
        ctx.waitTicks(5);
        String result = ctx.computeOnClient(mc -> {
            var all = ((com.epic60869.sky2m.mixin.Sky2MChatComponentAccessor) mc.gui.hud.getChat()).sky2m$allMessages();
            for (var message : all) {
                String text = message.content().getString();
                if (!text.contains("hello from discord")) continue;
                System.out.println("[Sky2M gametest] Discord chat line: \"" + text + "\"");
                // The reply arrow " ↩" may follow the message.
                if (!text.replaceFirst(" ↩$", "").equals("[S2M] [Discord] [Bob]: hello from discord")) return "the Discord line reads \"" + text + "\"";
                boolean[] blue = {false};
                message.content().visit((style, part) -> {
                    if (part.equals("Bob") && style.getColor() != null
                        && style.getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.BLUE))) blue[0] = true;
                    return java.util.Optional.empty();
                }, net.minecraft.network.chat.Style.EMPTY);
                return blue[0] ? null : "the Discord sender \"Bob\" isn't blue";
            }
            return "the Discord message didn't reach chat";
        });
        System.out.println("[Sky2M gametest] Discord message check: " + (result == null ? "ok" : result));
        if (result != null) failures.add(result);
        ctx.setScreen(() -> new net.minecraft.client.gui.screens.ChatScreen("", false));
        ctx.waitTicks(5);
        shot(ctx, "90-discord-chat");
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);
    }

    /** /s2 mp (Magical Power) was removed: it must be an unknown command now. */
    private static void removedMpCommand(ClientGameTestContext ctx, List<String> failures) {
        String outcome = ctx.computeOnClient(mc -> {
            try {
                ClientCommands.getActiveDispatcher().execute("s2 mp", (FabricClientCommandSource) mc.getConnection().getSuggestionsProvider());
                return "ran";
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                return "unknown: " + e.getMessage();
            } catch (Throwable e) {
                return "threw " + e;
            }
        });
        System.out.println("[Sky2M gametest] /s2 mp -> " + outcome);
        if (!outcome.startsWith("unknown")) failures.add("/s2 mp should be an unknown command, but it " + outcome);
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);
    }

    /** /s2 keybinds with the cursor on the first row: its tooltip (one line per component, no box symbol). */
    private static void keybindTooltip(ClientGameTestContext ctx, List<String> failures) {
        var rows = ctx.computeOnClient(mc -> com.epic60869.sky2m.features.misc.KeyConflicts.sheet());
        // A Settings row: its tooltip has two lines (the path, then "Click to open it in /s2").
        int row = 0;
        while (row < rows.size() - 1 && !rows.get(row).where().equals("Settings")) row++;
        System.out.println("[Sky2M gametest] /s2 keybinds has " + rows.size() + " rows; hovering row " + row);
        if (row > 30) failures.add("/s2 keybinds: no Settings row on the first page to hover");
        ctx.setScreen(com.epic60869.sky2m.features.misc.KeybindSheetScreen::new);
        ctx.waitTicks(5);
        // GUI scale 2: row n is at GUI y 76 + 13n (window y twice that).
        ctx.getInput().setCursorPos(300, (76 + 13 * row) * 2 + 6);
        ctx.waitTicks(5);
        shot(ctx, "91-keybinds-hover");
        ctx.getInput().setCursorPos(5, 5);
        ctx.setScreen(() -> null);
        ctx.waitTicks(5);
    }

    /** Settings replaced in game (cloud sync, profiles), then /s2 gui: this overflowed the stack before. */
    private static void importThenGui(ClientGameTestContext ctx, List<String> failures) {
        com.google.gson.JsonObject saved = ctx.computeOnClient(mc -> Sky2MConfig.exportJson());
        try {
            boolean imported = ctx.computeOnClient(mc -> Sky2MConfig.importJson(new com.google.gson.JsonObject()));
            if (!imported) failures.add("Sky2MConfig.importJson({}) returned false");
            String error = ctx.computeOnClient(mc -> {
                try {
                    ClientCommands.getActiveDispatcher().execute("s2 gui", (FabricClientCommandSource) mc.getConnection().getSuggestionsProvider());
                    return null;
                } catch (Throwable e) {
                    e.printStackTrace();
                    return e.toString();
                }
            });
            ctx.waitTicks(10);
            String screen = screenName(ctx);
            System.out.println("[Sky2M gametest] /s2 gui after importJson -> " + screen + (error == null ? "" : " (" + error + ")"));
            if (error != null) failures.add("/s2 gui after importJson threw " + error);
            else if ("none".equals(screen)) failures.add("/s2 gui after importJson didn't open a screen");
            else shot(ctx, "92-gui-after-import");
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);
            // And the settings themselves, rebuilt for the new config.
            String settingsError = ctx.computeOnClient(mc -> {
                try {
                    Sky2MConfig.openGui();
                    return null;
                } catch (Throwable e) {
                    e.printStackTrace();
                    return e.toString();
                }
            });
            ctx.waitTicks(10);
            String settings = screenName(ctx);
            System.out.println("[Sky2M gametest] settings after importJson -> " + settings + (settingsError == null ? "" : " (" + settingsError + ")"));
            if (settingsError != null) failures.add("the settings after importJson threw " + settingsError);
            else if ("none".equals(settings)) failures.add("the settings after importJson didn't open");
            else shot(ctx, "93-settings-after-import");
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);
        } finally {
            if (saved != null) ctx.runOnClient(mc -> Sky2MConfig.importJson(saved));
            ctx.waitTicks(2);
        }
    }

    /** The Item List over the inventory: a search, then a sum for its calculator. */
    private static void itemList(ClientGameTestContext ctx, int index, List<String> failures) {
        FeatureConfigs.ItemList c = Sky2MConfig.current().inventory.itemList;
        boolean enabled = c.enabled, outside = c.outsideSkyblock;
        try {
            ctx.runOnClient(mc -> {
                c.enabled = true;
                c.outsideSkyblock = true;
            });
            ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            ctx.waitTicks(10);
            // The NEU repo download can take a while on the first run.
            for (int i = 0; i < 60 && !ctx.computeOnClient(mc -> com.epic60869.sky2m.features.itemlist.ItemListData.loaded()); i++) ctx.waitTicks(20);
            if (searchBox() == null) {
                failures.add("the Item List didn't attach to the inventory");
                return;
            }
            ctx.runOnClient(mc -> searchBox().setValue("hyperion"));
            ctx.waitTicks(10);
            shot(ctx, String.format("%02d-item-list-search", index));
            ctx.runOnClient(mc -> searchBox().setValue("5*5"));
            ctx.waitTicks(5);
            shot(ctx, String.format("%02d-item-list-calculator", index + 1));
            ctx.runOnClient(mc -> searchBox().setValue("type:armor"));
            ctx.waitTicks(10);
            shot(ctx, String.format("%02d-item-list-armor", index + 2));
            ctx.runOnClient(mc -> searchBox().setValue(""));
        } finally {
            ctx.setScreen(() -> null);
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> {
                c.enabled = enabled;
                c.outsideSkyblock = outside;
            });
        }
    }

    private static EditBox searchBox() {
        try {
            var field = com.epic60869.sky2m.features.itemlist.ItemList.class.getDeclaredField("box");
            field.setAccessible(true);
            return (EditBox) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Couldn't reach the Item List's search box", e);
        }
    }

    /** Leaves the sim world and waits for its integrated server to stop, so the next test starts on the title screen. */
    private static void leaveWorld(ClientGameTestContext ctx) {
        ctx.setScreen(() -> null);
        // As Fabric's own TestSingleplayerContext.close() does.
        ctx.runOnClient(mc -> {
            if (mc.level != null) mc.level.disconnect(net.minecraft.network.chat.Component.translatable("menu.savingLevel"));
        });
        ctx.waitFor(mc -> mc.level == null && mc.getSingleplayerServer() == null, 1200);
        ctx.waitTicks(5);
        // Leaving shows the "disconnected" screen; gametests must end on the title screen.
        ctx.setScreen(net.minecraft.client.gui.screens.TitleScreen::new);
        ctx.waitTicks(5);
    }

    private static String screenName(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
    }

    private static void shot(ClientGameTestContext ctx, String name) {
        System.out.println("[Sky2M gametest] screenshot " + name + " -> " + ctx.takeScreenshot(name));
    }

    /** Collects every ERROR (or worse) line logged while attached. */
    private static final class ErrorCatcher extends AbstractAppender {
        private ErrorCatcher() {
            super("Sky2MGameTestErrors", null, null, true, Property.EMPTY_ARRAY);
        }

        static ErrorCatcher attach() {
            ErrorCatcher catcher = new ErrorCatcher();
            catcher.start();
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.getConfiguration().getRootLogger().addAppender(catcher, Level.ERROR, null);
            context.updateLoggers();
            return catcher;
        }

        void detach() {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.getConfiguration().getRootLogger().removeAppender(getName());
            stop();
        }

        @Override
        public void append(LogEvent event) {
            if (!event.getLevel().isMoreSpecificThan(Level.ERROR)) return;
            // The test client's offline account: Mojang refuses its profile keys. Not Sky2M's.
            if (event.getLoggerName().endsWith("AccountProfileKeyPairManager")) return;
            Throwable thrown = event.getThrown();
            ERRORS.add("[" + event.getLoggerName() + "] " + event.getMessage().getFormattedMessage() + (thrown == null ? "" : " - " + thrown));
        }
    }
}
