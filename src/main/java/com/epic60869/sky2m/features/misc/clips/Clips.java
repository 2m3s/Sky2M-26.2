package com.epic60869.sky2m.features.misc.clips;

import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.custom.util.Compat;
import com.epic60869.sky2m.features.core.Sky2MAlerts;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Clips: Sky2M keeps the last few seconds of the game recorded and saves them as a video on a key press. The recording
 * itself is done by Sky2M Clips (github.com/2m3s/Sky2m-Clipping), a separate Windows program, so the mod stays small
 * for everyone who doesn't use it: it is downloaded only when the player agrees, checked against the SHA-256 this
 * version of Sky2M expects, and kept in config/sky2m/clips.
 * <p>
 * The mod starts it while Clips is on and talks to it with one JSON object per line over its stdin/stdout: the settings
 * ({@code config}, sent again whenever they change), {@code save} with a name, and {@code quit}. It answers with
 * {@code ready}, {@code status}, {@code saved} (the file) and {@code error}. It stops by itself when the game exits.
 */
public final class Clips {
    private static final Logger LOGGER = LoggerFactory.getLogger("sky2m-clips");

    /** The Sky2M Clips release this version of Sky2M uses, and the SHA-256 of its Sky2MClips.exe. */
    public static final String HELPER_VERSION = "1.0.0";
    private static final String HELPER_URL = "https://github.com/2m3s/Sky2m-Clipping/releases/download/v" + HELPER_VERSION + "/Sky2MClips.exe";
    private static final String HELPER_SHA256 = "cb27d779fa97453baec13b21d8b9c793bfad034094242c0ceb2bc93fece10310";
    private static final long MAX_DOWNLOAD = 100L * 1024 * 1024;

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("sky2m").resolve("clips");
    private static final Path EXE = DIR.resolve("Sky2MClips-" + HELPER_VERSION + ".exe");
    private static final Gson GSON = new Gson();

    public enum Quality {
        P720("720p", 720), P1080("1080p", 1080), P1440("1440p", 1440), WINDOW("Window size", 0);

        private final String label;
        final int height;

        Quality(String label, int height) {
            this.label = label;
            this.height = height;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Fps {
        FPS30("30", 30), FPS60("60", 60);

        private final String label;
        final int fps;

        Fps(String label, int fps) {
            this.label = label;
            this.fps = fps;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Sound {
        GAME("Game only", "game"), GAME_DISCORD("Game + Discord", "game_discord"), DESKTOP("Everything you hear", "desktop"),
        NONE("No sound", "none");

        private final String label;
        final String wire;

        Sound(String label, String wire) {
            this.label = label;
            this.wire = wire;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static Process process;
    private static BufferedWriter toHelper;
    private static String lastConfig;
    private static boolean askedThisSession;
    private static boolean promptShown;
    private static volatile boolean downloading;
    private static int restarts;
    private static boolean keyWasDown;
    private static int tick;
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static volatile String encoder = "";
    private static volatile boolean recording;

    private Clips() {
    }

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
                    .executes(c -> {
                        save("");
                        return 1;
                    })
                    .then(ClientCommands.literal("download").executes(c -> {
                        download();
                        return 1;
                    }))
                    .then(ClientCommands.literal("folder").executes(c -> {
                        openFolder();
                        return 1;
                    }))
                    .then(ClientCommands.literal("status").executes(c -> {
                        status();
                        return 1;
                    }))
                    .then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                        save(StringArgumentType.getString(c, "name"));
                        return 1;
                    }))));
            }
        });
    }

    // ---------------------------------------------------------------- every tick

    private static void tick() {
        Sky2MConfig.Clips cfg = config();
        if (cfg == null) return;
        if (!cfg.askedDownload) askOnFirstLaunch(cfg);
        if (!cfg.enabled) {
            if (process != null) stop();
            return;
        }
        if (!windows()) {
            if (!askedThisSession) {
                askedThisSession = true;
                say(Component.literal("Clips only works on Windows.").withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (++tick % 20 == 0) keepRunning(cfg);
        boolean down = cfg.saveKey > 0 && keyDown(cfg.saveKey);
        if (down && !keyWasDown && !(Minecraft.getInstance().gui.screen() instanceof ChatScreen)) save("");
        keyWasDown = down;
    }

    /**
     * On the title screen, asks whether to download Sky2M Clips - every player once, including ones who used Sky2M
     * before Clips existed. Yes downloads it and turns Clips on; No leaves it (it can still be downloaded any time from
     * Clips' settings or /s2 clip download). Either answer is final: it is only asked again if the game closed before
     * they answered.
     */
    private static void askOnFirstLaunch(Sky2MConfig.Clips cfg) {
        Minecraft mc = Minecraft.getInstance();
        if (promptShown || !(mc.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen title)) return;
        if (!windows() || installed()) {
            answered(cfg);
            return;
        }
        promptShown = true;
        mc.gui.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(yes -> {
            answered(cfg);
            mc.gui.setScreen(title);
            if (yes) download();
        }, Component.literal("Sky2M Clips"),
            Component.literal("Would you like to automatically download the clipping software?\n\n"
                + "Sky2M Clips saves the last few seconds of the game as a video with one key, with the sound you choose. "
                + "It's a separate program (about 42 MB). If you say no, you can download it later in /s2 > Clips."),
            Component.literal("Yes, download"), Component.literal("No")));
    }

    private static void answered(Sky2MConfig.Clips cfg) {
        cfg.askedDownload = true;
        Sky2MConfig c = Sky2MConfig.current();
        if (c != null) Sky2MConfig.saveCurrent(c);
    }

    private static boolean keyDown(int key) {
        long window = Minecraft.getInstance().getWindow().handle();
        if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) return GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
        return key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    /** Once a second: start the helper (or ask to download it), and send the settings when they changed. */
    private static void keepRunning(Sky2MConfig.Clips cfg) {
        if (process != null && !process.isAlive()) {
            LOGGER.warn("Sky2M Clips stopped (exit code {})", process.exitValue());
            process = null;
            toHelper = null;
            lastConfig = null;
            recording = false;
            if (++restarts > 3) {
                cfg.enabled = false;
                say(Component.literal("Sky2M Clips keeps stopping, so Clips was turned off. Its log is in %LOCALAPPDATA%\\Sky2MClips\\log.txt.").withStyle(ChatFormatting.RED));
                return;
            }
        }
        if (process == null) {
            if (!installed()) {
                if (!askedThisSession && !downloading) {
                    askedThisSession = true;
                    askToDownload();
                }
                return;
            }
            start();
        }
        String json = configJson(cfg);
        if (!json.equals(lastConfig) && send(json)) lastConfig = json;
    }

    private static String configJson(Sky2MConfig.Clips cfg) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "config");
        o.addProperty("pid", ProcessHandle.current().pid());
        o.addProperty("seconds", cfg.seconds);
        o.addProperty("fps", cfg.fps.fps);
        o.addProperty("height", cfg.quality.height);
        o.addProperty("audio", cfg.sound.wire);
        o.addProperty("mic", cfg.mic);
        o.addProperty("gameVolume", cfg.gameVolume);
        o.addProperty("discordVolume", cfg.discordVolume);
        o.addProperty("desktopVolume", cfg.desktopVolume);
        o.addProperty("micVolume", cfg.micVolume);
        o.addProperty("folder", folder().toString());
        return GSON.toJson(o);
    }

    private static Path folder() {
        String home = System.getProperty("user.home", ".");
        return Path.of(home, "Videos", "Sky2M Clips");
    }

    // ---------------------------------------------------------------- the helper process

    private static boolean installed() {
        return Files.isRegularFile(EXE);
    }

    private static synchronized void start() {
        try {
            Process p = new ProcessBuilder(EXE.toString()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            process = p;
            toHelper = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8));
            Thread reader = new Thread(() -> read(p), "Sky2M clips reader");
            reader.setDaemon(true);
            reader.start();
            LOGGER.info("started Sky2M Clips {}", HELPER_VERSION);
        } catch (Exception e) {
            LOGGER.error("couldn't start Sky2M Clips", e);
            say(Component.literal("Couldn't start Sky2M Clips: " + e.getMessage()).withStyle(ChatFormatting.RED));
            Sky2MConfig.Clips cfg = config();
            if (cfg != null) cfg.enabled = false;
        }
    }

    private static synchronized boolean send(String json) {
        if (toHelper == null) return false;
        try {
            toHelper.write(json);
            toHelper.write('\n');
            toHelper.flush();
            return true;
        } catch (Exception e) {
            LOGGER.warn("couldn't talk to Sky2M Clips: {}", e.toString());
            return false;
        }
    }

    private static synchronized void stop() {
        Process p = process;
        if (p == null) return;
        send("{\"type\":\"quit\"}");
        try {
            if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) p.destroy();
        } catch (InterruptedException ignored) {
            p.destroy();
        }
        process = null;
        toHelper = null;
        lastConfig = null;
        recording = false;
    }

    private static void read(Process p) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handle(JsonParser.parseString(line).getAsJsonObject());
                } catch (Exception e) {
                    LOGGER.debug("Sky2M Clips said: {}", line);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void handle(JsonObject m) {
        String type = m.has("type") ? m.get("type").getAsString() : "";
        switch (type) {
            case "status" -> {
                if (m.has("recording")) recording = m.get("recording").getAsBoolean();
                if (m.has("encoder")) encoder = m.get("encoder").getAsString();
            }
            case "saved" -> {
                Path file = Path.of(m.get("path").getAsString());
                double seconds = m.has("seconds") ? m.get("seconds").getAsDouble() : 0;
                MutableComponent line = Component.literal(String.format(Locale.ROOT, "Clip saved (%.0f s) ", seconds)).withStyle(ChatFormatting.GREEN)
                    .append(link("[Watch]", ChatFormatting.AQUA, new ClickEvent.OpenFile(file.toAbsolutePath()), file.getFileName().toString()))
                    .append(Component.literal(" "))
                    .append(link("[Folder]", ChatFormatting.YELLOW, new ClickEvent.OpenFile(file.toAbsolutePath().getParent()), "Videos\\Sky2M Clips"));
                say(line);
            }
            case "error" -> say(Component.literal("Clip failed: " + (m.has("message") ? m.get("message").getAsString() : "unknown error"))
                .withStyle(ChatFormatting.RED));
            default -> {
            }
        }
    }

    private static MutableComponent link(String text, ChatFormatting colour, ClickEvent click, String hover) {
        return Component.literal(text).withStyle(s -> s.withColor(colour).withClickEvent(click)
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    // ---------------------------------------------------------------- commands

    public static void save(String name) {
        Sky2MConfig.Clips cfg = config();
        if (cfg == null || !cfg.enabled) {
            say(Component.literal("Clips are off: turn them on in /s2 > Clips.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (process == null) {
            say(Component.literal(installed() ? "Sky2M Clips is starting, try again in a moment." : "Sky2M Clips isn't downloaded yet.")
                .withStyle(ChatFormatting.YELLOW));
            if (!installed()) askToDownload();
            return;
        }
        JsonObject o = new JsonObject();
        o.addProperty("type", "save");
        o.addProperty("id", String.valueOf(NEXT_ID.incrementAndGet()));
        o.addProperty("name", name == null || name.isBlank() ? "" : name.trim());
        if (send(GSON.toJson(o))) say(Component.literal("Saving clip...").withStyle(ChatFormatting.GRAY));
    }

    public static void openFolder() {
        try {
            Files.createDirectories(folder());
        } catch (Exception ignored) {
        }
        net.minecraft.util.Util.getPlatform().openPath(folder());
    }

    private static void status() {
        Sky2MConfig.Clips cfg = config();
        String text;
        if (cfg == null || !cfg.enabled) text = "Clips are off.";
        else if (!installed()) text = "Sky2M Clips isn't downloaded.";
        else if (process == null) text = "Sky2M Clips is starting.";
        else text = recording ? "Recording" + (encoder.isEmpty() ? "" : " with " + encoder) + "." : "Running, waiting for the game window.";
        say(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    private static void askToDownload() {
        MutableComponent line = Component.literal("Clips need Sky2M Clips, a separate program (about 42 MB, from Sky2M's GitHub). ")
            .withStyle(ChatFormatting.YELLOW)
            .append(link("[Download]", ChatFormatting.GREEN, new ClickEvent.RunCommand("/s2 clip download"),
                "Download Sky2M Clips " + HELPER_VERSION + " into config/sky2m/clips"));
        say(line);
    }

    /** Downloads the helper this version of Sky2M expects, checks its SHA-256, and starts it. */
    public static void download() {
        if (downloading) return;
        if (!windows()) {
            say(Component.literal("Clips only works on Windows.").withStyle(ChatFormatting.RED));
            return;
        }
        downloading = true;
        say(Component.literal("Downloading Sky2M Clips " + HELPER_VERSION + "...").withStyle(ChatFormatting.GRAY));
        Thread t = new Thread(() -> {
            Path part = DIR.resolve("Sky2MClips-" + HELPER_VERSION + ".exe.part");
            try {
                Files.createDirectories(DIR);
                HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(HELPER_URL)).timeout(Duration.ofMinutes(5))
                    .header("User-Agent", "Sky2M").GET().build();
                HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                long total = 0;
                try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(part)) {
                    byte[] buffer = new byte[65536];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        total += n;
                        if (total > MAX_DOWNLOAD) throw new IllegalStateException("the download is far bigger than expected");
                        sha.update(buffer, 0, n);
                        out.write(buffer, 0, n);
                    }
                }
                String hash = HexFormat.of().formatHex(sha.digest());
                if (!HELPER_SHA256.isEmpty() && !hash.equalsIgnoreCase(HELPER_SHA256)) {
                    throw new IllegalStateException("the download isn't the file this version of Sky2M expects");
                }
                Files.move(part, EXE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                say(Component.literal("Sky2M Clips downloaded. Press your Save Clip key to save the last few seconds.").withStyle(ChatFormatting.GREEN));
                Sky2MConfig.Clips cfg = config();
                if (cfg != null && !cfg.enabled) {
                    cfg.enabled = true;
                    Sky2MConfig c = Sky2MConfig.current();
                    if (c != null) Sky2MConfig.saveCurrent(c);
                }
            } catch (Exception e) {
                LOGGER.error("Sky2M Clips download failed", e);
                say(Component.literal("Couldn't download Sky2M Clips: " + e.getMessage()).withStyle(ChatFormatting.RED));
                try {
                    Files.deleteIfExists(part);
                } catch (Exception ignored) {
                }
            } finally {
                downloading = false;
            }
        }, "Sky2M clips download");
        t.setDaemon(true);
        t.start();
    }

    private static void say(Component message) {
        Sky2MAlerts.chat(message);
    }
}
