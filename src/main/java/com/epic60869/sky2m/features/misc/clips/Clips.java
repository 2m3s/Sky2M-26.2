package com.epic60869.sky2m.features.misc.clips;

import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.core.Sky2MAlerts;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sky2M owns the Clips settings and save key. Recording is provided by the separate Sky2M Clipping Fabric mod,
 * discovered through the "sky2m-clipping" Fabric entrypoint. No external executable is launched or downloaded.
 *
 * Clipping entrypoint contract:
 *   configure(Map<String, Object> settings)
 *   save(String name) -> CompletableFuture<Map<String, Object>> (result includes path and optional seconds)
 *   status() -> Map<String, Object> (recording and optional encoder)
 *   shutdown()
 */
public final class Clips {
    private static final Logger LOGGER = LoggerFactory.getLogger("sky2m-clips");
    private static final String CLIPPING_ENTRYPOINT = "sky2m-clipping";
    private static final String RELEASES_URL = "https://github.com/2m3s/Sky2m-Clipping/releases";
    private static final Path DEFAULT_FOLDER = Path.of(
        System.getProperty("user.home", "."), "Videos", "Sky2M Clips"
    );

    public enum Quality {
        P720("720p", 720), P1080("1080p", 1080), P1440("1440p", 1440), WINDOW("Window size", 0);
        private final String label;
        final int height;
        Quality(String label, int height) { this.label = label; this.height = height; }
        @Override public String toString() { return label; }
    }

    public enum Fps {
        FPS30("30", 30), FPS60("60", 60);
        private final String label;
        final int fps;
        Fps(String label, int fps) { this.label = label; this.fps = fps; }
        @Override public String toString() { return label; }
    }

    public enum Sound {
        GAME("Game only", "game"),
        GAME_DISCORD("Game + Discord", "game_discord"),
        DESKTOP("Everything you hear", "desktop"),
        NONE("No sound", "none");
        private final String label;
        final String wire;
        Sound(String label, String wire) { this.label = label; this.wire = wire; }
        @Override public String toString() { return label; }
    }

    private static volatile Object clipping;
    private static boolean askedThisSession;
    private static boolean promptShown;
    private static boolean keyWasDown;
    private static int tick;
    private static String lastConfig = "";
    private static volatile boolean recording;
    private static volatile String encoder = "";

    private Clips() {}

    private static Sky2MConfig.Clips config() {
        Sky2MConfig c = Sky2MConfig.current();
        return c == null ? null : c.clips;
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick());
        Runtime.getRuntime().addShutdownHook(new Thread(Clips::stop, "Sky2M clips stop"));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("clip")
                    .executes(c -> { save(""); return 1; })
                    .then(ClientCommands.literal("download").executes(c -> { download(); return 1; }))
                    .then(ClientCommands.literal("folder").executes(c -> { openFolder(); return 1; }))
                    .then(ClientCommands.literal("status").executes(c -> { status(); return 1; }))
                    .then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                        save(StringArgumentType.getString(c, "name"));
                        return 1;
                    }))));
            }
        });
    }

    private static void tick() {
        Sky2MConfig.Clips cfg = config();
        if (cfg == null) return;
        if (!cfg.enabled) {
            if (clipping != null) stop();
            return;
        }
        if (!windows()) {
            if (!askedThisSession) {
                askedThisSession = true;
                say(Component.literal("Sky2M Clips currently supports Windows only.").withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (++tick % 20 == 0) {
            Object api = findClipping();
            if (api == null) {
                if (!askedThisSession) {
                    askedThisSession = true;
                    say(Component.literal("Sky2M Clipping mod isn't installed. Install the Sky2M-Clipping.jar from GitHub Releases, then restart Minecraft. ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(link("[Open releases]", ChatFormatting.AQUA, new ClickEvent.OpenUrl(java.net.URI.create(RELEASES_URL)), RELEASES_URL)));
                }
            } else {
                if (clipping != api) {
                    clipping = api;
                    lastConfig = "";
                    LOGGER.info("Connected to Sky2M Clipping Fabric mod");
                }
                applyConfig(cfg);
                refreshStatus();
            }
        }
        boolean down = cfg.saveKey > 0 && keyDown(cfg.saveKey);
        if (down && !keyWasDown && !(Minecraft.getInstance().gui.screen() instanceof ChatScreen)) save("");
        keyWasDown = down;
    }

    private static Object findClipping() {
        try {
            List<Object> entries = FabricLoader.getInstance().getEntrypoints(CLIPPING_ENTRYPOINT, Object.class);
            return entries.isEmpty() ? null : entries.get(0);
        } catch (Throwable e) {
            LOGGER.debug("Sky2M Clipping entrypoint isn't available", e);
            return null;
        }
    }

    private static boolean keyDown(int key) {
        long window = Minecraft.getInstance().getWindow().handle();
        if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST)
            return GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
        return key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST
            && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    private static Map<String, Object> configMap(Sky2MConfig.Clips cfg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pid", ProcessHandle.current().pid());
        m.put("seconds", cfg.seconds);
        m.put("fps", cfg.fps.fps);
        m.put("height", cfg.quality.height);
        m.put("audio", cfg.sound.wire);
        m.put("mic", cfg.mic);
        m.put("gameVolume", cfg.gameVolume);
        m.put("discordVolume", cfg.discordVolume);
        m.put("desktopVolume", cfg.desktopVolume);
        m.put("micVolume", cfg.micVolume);
        m.put("folder", folder().toString());
        return m;
    }

    private static void applyConfig(Sky2MConfig.Clips cfg) {
        Object api = clipping;
        if (api == null) return;
        Map<String, Object> settings = configMap(cfg);
        String signature = settings.toString();
        if (signature.equals(lastConfig)) return;
        try {
            invoke(api, "configure", new Class<?>[]{Map.class}, settings);
            lastConfig = signature;
        } catch (Exception e) {
            LOGGER.error("Could not send Sky2M Clips settings to the clipping mod", e);
            say(Component.literal("Couldn't configure Sky2M Clipping: " + rootMessage(e)).withStyle(ChatFormatting.RED));
        }
    }

    private static void refreshStatus() {
        Object api = clipping;
        if (api == null) return;
        try {
            Object result = invoke(api, "status", new Class<?>[0]);
            if (result instanceof Map<?, ?> map) {
                Object r = map.get("recording");
                if (r instanceof Boolean b) recording = b;
                Object e = map.get("encoder");
                if (e != null) encoder = e.toString();
            }
        } catch (Exception e) {
            LOGGER.debug("Couldn't read clipping status", e);
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(name, parameterTypes);
        return method.invoke(target, args);
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    private static Path folder() {
        return DEFAULT_FOLDER;
    }

    private static void stop() {
        Object api = clipping;
        clipping = null;
        lastConfig = "";
        recording = false;
        if (api == null) return;
        try {
            invoke(api, "shutdown", new Class<?>[0]);
        } catch (Exception e) {
            LOGGER.debug("Couldn't stop Sky2M Clipping cleanly", e);
        }
    }

    private static MutableComponent link(String text, ChatFormatting colour, ClickEvent click, String hover) {
        return Component.literal(text).withStyle(s -> s.withColor(colour).withClickEvent(click)
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    public static void save(String name) {
        Sky2MConfig.Clips cfg = config();
        if (cfg == null || !cfg.enabled) {
            say(Component.literal("Clips are off: turn them on in /s2 > Clips.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        Object api = clipping != null ? clipping : findClipping();
        if (api == null) {
            say(Component.literal("Sky2M Clipping mod isn't installed. Install its JAR from GitHub Releases and restart Minecraft.")
                .withStyle(ChatFormatting.YELLOW));
            download();
            return;
        }
        try {
            Object result = invoke(api, "save", new Class<?>[]{String.class}, name == null ? "" : name.trim());
            if (result instanceof CompletableFuture<?> future) {
                say(Component.literal("Saving clip...").withStyle(ChatFormatting.GRAY));
                future.whenComplete((value, error) -> Minecraft.getInstance().execute(() -> {
                    if (error != null) {
                        say(Component.literal("Clip failed: " + rootMessage(error)).withStyle(ChatFormatting.RED));
                        return;
                    }
                    savedMessage(value);
                }));
            } else {
                savedMessage(result);
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't request a clip from Sky2M Clipping", e);
            say(Component.literal("Couldn't save clip: " + rootMessage(e)).withStyle(ChatFormatting.RED));
        }
    }

    private static void savedMessage(Object value) {
        String pathString = null;
        double seconds = 0;
        if (value instanceof Map<?, ?> map) {
            Object path = map.get("path");
            if (path != null) pathString = path.toString();
            Object duration = map.get("seconds");
            if (duration instanceof Number n) seconds = n.doubleValue();
        } else if (value != null) {
            pathString = value.toString();
        }
        if (pathString == null || pathString.isBlank()) {
            say(Component.literal("Clip saved.").withStyle(ChatFormatting.GREEN));
            return;
        }
        Path file = Path.of(pathString);
        MutableComponent line = Component.literal(String.format(Locale.ROOT, "Clip saved%s ", seconds > 0 ? String.format(Locale.ROOT, " (%.0f s)", seconds) : ""))
            .withStyle(ChatFormatting.GREEN)
            .append(link("[Open]", ChatFormatting.AQUA, new ClickEvent.OpenFile(file.toAbsolutePath()), file.getFileName().toString()))
            .append(Component.literal(" "))
            .append(link("[Folder]", ChatFormatting.YELLOW, new ClickEvent.OpenFile(file.toAbsolutePath().getParent()), "Sky2M Clips folder"));
        say(line);
    }

    public static void openFolder() {
        try {
            Files.createDirectories(folder());
        } catch (Exception e) {
            LOGGER.debug("Couldn't create clips folder", e);
        }
        net.minecraft.util.Util.getPlatform().openPath(folder());
    }

    private static void status() {
        Sky2MConfig.Clips cfg = config();
        String text;
        if (cfg == null || !cfg.enabled) text = "Clips are off.";
        else if (clipping == null && findClipping() == null) text = "Sky2M Clipping mod isn't installed.";
        else if (!recording) text = "Sky2M Clipping is loaded" + (encoder.isBlank() ? "." : " (" + encoder + ").");
        else text = "Recording" + (encoder.isBlank() ? "" : " with " + encoder) + ".";
        say(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    /** Opens the JAR release page; downloading an executable is intentionally no longer supported. */
    public static void download() {
        net.minecraft.util.Util.getPlatform().openUri(RELEASES_URL);
        say(Component.literal("Install Sky2M-Clipping.jar from GitHub Releases into your Minecraft mods folder, then restart the game.")
            .withStyle(ChatFormatting.YELLOW));
    }

    private static void say(Component message) {
        Sky2MAlerts.chat(message);
    }
}
