package com.epic60869.sky2m.gametest;

import com.epic60869.sky2m.mixin.Sky2MChatComponentAccessor;
import com.epic60869.sky2m.Sky2MConfig;
import com.epic60869.sky2m.features.core.Sky2MLocation;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.tree.CommandNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Test bridge for driving a dev client from scripts: started only with -Dsky2m.bridge=PORT (the bridgeClient run),
 * it listens on 127.0.0.1 and runs each request on the client thread. Not part of the mod jar.
 *
 * GET /state, /log?since=N, /cmd?c=, /chat?m=, /inject?m=&overlay=, /click?slot=&button=&type=, /close, /shot,
 * /hotbar?i=, /use, /connect?host=, /config?path=&value=, /booleans, /enableall?exclude=, /call?cls=&method=,
 * /commands?root=, /screen?cls=
 */
public final class TestBridge implements ClientModInitializer {
    private static final Gson GSON = new Gson();
    private static final List<String> chat = Collections.synchronizedList(new ArrayList<>());
    private static final Set<GuiMessage> seen = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void onInitializeClient() {
        String port = System.getProperty("sky2m.bridge");
        if (port == null) return;
        ClientTickEvents.END_CLIENT_TICK.register(TestBridge::captureChat);
        // Server resource packs are always accepted, so no prompt stops a scripted join (or makes the server wait 25s).
        ClientConfigurationConnectionEvents.INIT.register((handler, mc) -> {
            // Whether this connection goes to this PC: the current server entry is null while the proxy switches
            // servers, so it can't be read then.
            try {
                Field connection = net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl.class.getDeclaredField("connection");
                connection.setAccessible(true);
                localConnection = ((net.minecraft.network.Connection) connection.get(handler)).getRemoteAddress() instanceof InetSocketAddress address
                    && address.getAddress() != null && address.getAddress().isLoopbackAddress();
                // The entry the pack prompt checks is the listener's own copy.
                Field data = net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl.class.getDeclaredField("serverData");
                data.setAccessible(true);
                if (localConnection && data.get(handler) instanceof ServerData own) own.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            } catch (ReflectiveOperationException e) {
                localConnection = false;
            }
            acceptPacks(mc);
        });
        ClientTickEvents.END_CLIENT_TICK.register(TestBridge::acceptPacks);
        ClientTickEvents.END_CLIENT_TICK.register(TestBridge::keepMining);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), Integer.parseInt(port)), 0);
            server.createContext("/", TestBridge::handle);
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread thread = new Thread(r, "Sky2M bridge");
                thread.setDaemon(true);
                return thread;
            }));
            server.start();
            System.out.println("[Sky2M bridge] listening on " + port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static volatile boolean localConnection;

    /** The block /mine is breaking, and when it gives up; null when not mining. */
    private static volatile net.minecraft.core.BlockPos miningTarget;
    private static volatile long miningUntil;

    /** Holds left-click on the /mine block each tick, as a player does, until it breaks or the time runs out. */
    private static void keepMining(Minecraft mc) {
        net.minecraft.core.BlockPos pos = miningTarget;
        if (pos == null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (System.currentTimeMillis() > miningUntil || mc.level.getBlockState(pos).isAir()) {
            mc.gameMode.stopDestroyBlock();
            miningTarget = null;
            return;
        }
        mc.gameMode.continueDestroyBlock(pos, net.minecraft.core.Direction.UP);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void acceptPacks(Minecraft mc) {
        ServerData server = mc.getCurrentServer();
        if (server != null ? !isLocal(server) : !localConnection) return;
        if (server != null && server.getResourcePackStatus() != ServerData.ServerPackStatus.ENABLED) {
            server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        }
        // Joins that don't go through quick play keep their own copy of the server entry, so the prompt can still
        // appear: answer it "Proceed".
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen confirm
            && confirm.getClass().getName().contains("PackConfirmScreen")) {
            for (Class<?> c = confirm.getClass(); c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!it.unimi.dsi.fastutil.booleans.BooleanConsumer.class.isAssignableFrom(f.getType())) continue;
                    try {
                        f.setAccessible(true);
                        ((it.unimi.dsi.fastutil.booleans.BooleanConsumer) f.get(confirm)).accept(true);
                        return;
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        }
    }

    /** Only the local test server. */
    private static boolean isLocal(ServerData server) {
        String host = server.ip == null ? "" : server.ip.toLowerCase(java.util.Locale.ROOT);
        return host.startsWith("localhost") || host.startsWith("127.0.0.1");
    }

    private static void captureChat(Minecraft mc) {
        if (mc.gui.hud == null) return;
        List<GuiMessage> all = ((Sky2MChatComponentAccessor) mc.gui.hud.getChat()).sky2m$allMessages();
        for (int i = all.size() - 1; i >= 0; i--) {
            GuiMessage message = all.get(i);
            if (seen.add(message)) chat.add(message.content().getString());
        }
        if (seen.size() > 2 * all.size() + 500) seen.retainAll(all);
    }

    private static void handle(HttpExchange ex) throws IOException {
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        String path = ex.getRequestURI().getPath();
        Object result;
        int status = 200;
        try {
            if (path.equals("/log")) {
                result = log(Integer.parseInt(q.getOrDefault("since", "0")));
            } else {
                result = Minecraft.getInstance().submit(() -> {
                    try {
                        return route(path, q);
                    } catch (Throwable t) {
                        return error(t);
                    }
                }).get(20, TimeUnit.SECONDS);
            }
        } catch (Throwable t) {
            result = error(t);
            status = 500;
        }
        byte[] body = GSON.toJson(result).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    private static Object route(String path, Map<String, String> q) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        switch (path) {
            case "/state": return state(mc);
            case "/cmd": mc.player.connection.sendCommand(q.get("c")); return "ok";
            case "/chat": mc.player.connection.sendChat(q.get("m")); return "ok";
            case "/inject":
                mc.getConnection().handleSystemChat(new ClientboundSystemChatPacket(Component.literal(q.get("m")), "1".equals(q.get("overlay"))));
                return "ok";
            case "/click": {
                if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return "no container open";
                ContainerInput type = ContainerInput.valueOf(q.getOrDefault("type", "PICKUP"));
                int slotId = Integer.parseInt(q.get("slot"));
                int button = Integer.parseInt(q.getOrDefault("button", "0"));
                if ("1".equals(q.get("raw"))) {
                    mc.gameMode.handleContainerInput(screen.getMenu().containerId, slotId, button, type, mc.player);
                    return "ok";
                }
                // As a mouse click: through the screen's slotClicked, where mods (misclick prevention, slot locking)
                // hook in.
                Method clicked = AbstractContainerScreen.class.getDeclaredMethod("slotClicked", Slot.class, int.class, int.class, ContainerInput.class);
                clicked.setAccessible(true);
                clicked.invoke(screen, screen.getMenu().getSlot(slotId), slotId, button, type);
                return "ok";
            }
            case "/close": {
                // Like a player: Escape first (some screens, such as the storage overview, only close that way).
                if (mc.gui.screen() != null) mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, 0, 0));
                if (mc.gui.screen() instanceof AbstractContainerScreen<?> && mc.player != null) mc.player.closeContainer();
                else if (mc.gui.screen() != null) mc.gui.setScreen(null);
                return "ok";
            }
            case "/shot": Screenshot.grab(mc, false); return "ok";
            case "/hotbar": {
                // Select the slot and tell the server, as scrolling does (otherwise the server keeps the old item).
                int slot = Integer.parseInt(q.get("i"));
                mc.player.getInventory().setSelectedSlot(slot);
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
                return "ok";
            }
            case "/use": return String.valueOf(mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
            case "/useon": {
                // Right-clicks the top of a block with the held item (placing a minion, opening a chest...).
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(q.get("x")), Integer.parseInt(q.get("y")), Integer.parseInt(q.get("z")));
                var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), net.minecraft.core.Direction.UP, pos, false);
                return String.valueOf(mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit));
            }
            case "/dig": {
                // Left-clicks the top of a block (starts breaking it), like digging a Diana burrow.
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(q.get("x")), Integer.parseInt(q.get("y")), Integer.parseInt(q.get("z")));
                boolean started = mc.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.UP);
                mc.gameMode.stopDestroyBlock();
                return String.valueOf(started);
            }
            case "/mine": {
                // Holds left-click on a block (ms= how long at most, default 5000) until it breaks, like mining it.
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(q.get("x")), Integer.parseInt(q.get("y")), Integer.parseInt(q.get("z")));
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(pos));
                miningUntil = System.currentTimeMillis() + Long.parseLong(q.getOrDefault("ms", "5000"));
                boolean started = mc.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.UP);
                miningTarget = pos;
                return String.valueOf(started);
            }
            case "/attacknear": {
                // Hits the nearest living entity whose type id contains `type` (not armor stands) within 4 blocks.
                String type = q.getOrDefault("type", "zombie");
                net.minecraft.world.entity.LivingEntity best = null;
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (!(e instanceof net.minecraft.world.entity.LivingEntity living) || e == mc.player
                        || e instanceof net.minecraft.world.entity.decoration.ArmorStand || !living.isAlive()) continue;
                    if (e.distanceTo(mc.player) > Double.parseDouble(q.getOrDefault("range", "4"))) continue;
                    if (!net.minecraft.world.entity.EntityType.getKey(e.getType()).toString().contains(type)) continue;
                    if (best == null || e.distanceTo(mc.player) < best.distanceTo(mc.player)) best = living;
                }
                if (best == null) return "none";
                mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, best.getEyePosition());
                mc.gameMode.attack(mc.player, best);
                mc.player.swing(InteractionHand.MAIN_HAND);
                return best.getUUID() + " " + best.position();
            }
            case "/entities": {
                // Entities within r blocks: "type name @ x y z", nearest first.
                double r = Double.parseDouble(q.getOrDefault("r", "8"));
                List<net.minecraft.world.entity.Entity> near = new ArrayList<>();
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e != mc.player && e.distanceTo(mc.player) <= r) near.add(e);
                }
                near.sort(java.util.Comparator.comparingDouble(e -> e.distanceTo(mc.player)));
                List<String> out = new ArrayList<>();
                for (net.minecraft.world.entity.Entity e : near) {
                    out.add(net.minecraft.world.entity.EntityType.getKey(e.getType()).getPath() + " "
                        + (e.hasCustomName() ? e.getCustomName().getString() : "") + " @ "
                        + String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f", e.getX(), e.getY(), e.getZ()));
                }
                return out;
            }
            case "/block": {
                // The block id at a position, e.g. "minecraft:grass_block".
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(Integer.parseInt(q.get("x")), Integer.parseInt(q.get("y")), Integer.parseInt(q.get("z")));
                return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(pos).getBlock()).toString();
            }
            case "/nearestblock": {
                // The nearest block whose id contains `name` with air above it (e.g. a water surface to fish in).
                String name = q.getOrDefault("name", "water");
                int r = Integer.parseInt(q.getOrDefault("r", "48"));
                net.minecraft.core.BlockPos center = mc.player.blockPosition(), best = null;
                double bestDistance = Double.MAX_VALUE;
                for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(center.offset(-r, -12, -r), center.offset(r, 12, r))) {
                    var state = mc.level.getBlockState(pos);
                    if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains(name)) continue;
                    if (!mc.level.getBlockState(pos.above()).isAir()) continue;
                    // open=N: only a block with the same block N around it (open water to cast into, not a well).
                    int open = Integer.parseInt(q.getOrDefault("open", "0"));
                    boolean surrounded = true;
                    for (int dx = -open; dx <= open && surrounded; dx++) {
                        for (int dz = -open; dz <= open; dz++) {
                            if (mc.level.getBlockState(pos.offset(dx, 0, dz)).getBlock() != state.getBlock()) {
                                surrounded = false;
                                break;
                            }
                        }
                    }
                    if (!surrounded) continue;
                    double d = pos.distSqr(center);
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = pos.immutable();
                    }
                }
                return best == null ? "none" : best.getX() + " " + best.getY() + " " + best.getZ();
            }
            case "/interactnear": {
                // Right-clicks the nearest entity whose type id contains `type` (e.g. armor_stand) within 6 blocks.
                String type = q.getOrDefault("type", "armor_stand");
                net.minecraft.world.entity.Entity best = null;
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e == mc.player || e.distanceTo(mc.player) > 6) continue;
                    if (!net.minecraft.world.entity.EntityType.getKey(e.getType()).toString().contains(type)) continue;
                    if (best == null || e.distanceTo(mc.player) < best.distanceTo(mc.player)) best = e;
                }
                if (best == null) return "none";
                var hit = new net.minecraft.world.phys.EntityHitResult(best, best.position().add(0, 0.5, 0));
                return String.valueOf(mc.gameMode.interact(mc.player, best, hit, InteractionHand.MAIN_HAND));
            }
            case "/tp": {
                // Moves the player (the client sends the move like any other); for walking through test floors.
                mc.player.setPos(Double.parseDouble(q.get("x")), Double.parseDouble(q.get("y")), Double.parseDouble(q.get("z")));
                if (q.containsKey("yaw")) mc.player.setYRot(Float.parseFloat(q.get("yaw")));
                if (q.containsKey("pitch")) mc.player.setXRot(Float.parseFloat(q.get("pitch")));
                return "ok";
            }
            case "/connect": {
                ServerData data = new ServerData("Local", q.getOrDefault("host", "localhost:25565"), ServerData.Type.OTHER);
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(data.ip), data, false, null);
                return "ok";
            }
            case "/config": return setConfig(q.get("path"), q.get("value"));
            case "/booleans": return booleans();
            case "/enableall": return enableAll(q.getOrDefault("exclude", ""));
            case "/focus": return focus(q.getOrDefault("only", ""), q.getOrDefault("exclude", ""));
            case "/call": {
                Method m = Class.forName(q.get("cls")).getDeclaredMethod(q.get("method"));
                m.setAccessible(true);
                return String.valueOf(m.invoke(null));
            }
            case "/field": {
                // Reads a static field (e.g. a feature's detected waypoints) for checking what a feature saw.
                java.lang.reflect.Field f = Class.forName(q.get("cls")).getDeclaredField(q.get("name"));
                f.setAccessible(true);
                return describe(f.get(null), 3);
            }
            case "/commands": {
                CommandNode<?> root = ClientCommands.getActiveDispatcher().getRoot().getChild(q.get("root"));
                List<String> names = new ArrayList<>();
                if (root != null) for (CommandNode<?> child : root.getChildren()) names.add(child.getName() + (child.getCommand() != null ? "" : "*"));
                return names;
            }
            case "/screen": return mc.gui.screen() == null ? null : mc.gui.screen().getClass().getName();
            case "/simgen": {
                // Leaves any server and generates a Dungeon Sim floor into the sim world (saved like any world, so
                // it can be copied to a test server). Returns the floor's map code.
                var floor = com.epic60869.sky2m.sim.roomsim.SimFloorGen.Floor.valueOf(q.getOrDefault("floor", "F5"));
                var planned = com.epic60869.sky2m.sim.roomsim.SimFloorGen.plan(floor,
                    Integer.parseInt(q.getOrDefault("puzzles", "3")), Integer.parseInt(q.getOrDefault("blood", "6")));
                if (planned == null) return "plan failed";
                if (mc.getConnection() != null && mc.getSingleplayerServer() == null) mc.disconnectWithProgressScreen();
                com.epic60869.sky2m.sim.roomsim.SimState.setFloorLabel(floor.code);
                com.epic60869.sky2m.sim.roomsim.SimWorld.open(mc, planned.code(),
                    c -> com.epic60869.sky2m.sim.roomsim.SimBuilder.build(c, planned.code()), "Generating " + floor.label);
                return planned.code();
            }
            case "/simmap": {
                // A map code's 11x11 grid (rooms on even cells, connectors on odd) with room types, for a test server
                // to draw Hypixel's dungeon map item from.
                var decoded = com.epic60869.sky2m.sim.roomsim.MapCode.decode(q.get("code"));
                if (decoded == null) return "bad code";
                List<Map<String, Object>> cells = new ArrayList<>();
                for (int i = 0; i < decoded.cellRoom().length; i++) {
                    int room = decoded.cellRoom()[i];
                    Map<String, Object> cell = new HashMap<>();
                    cell.put("room", room);
                    cell.put("door", decoded.cellDoor()[i]);
                    if (room >= 0) {
                        String name = decoded.nameTable()[room];
                        cell.put("name", name);
                        cell.put("type", com.epic60869.sky2m.sim.roomsim.SimFloorGen.typeOf(name));
                    }
                    cells.add(cell);
                }
                return cells;
            }
            case "/solver": {
                // The active container solver and its fields (for debugging solvers against the test server).
                Object solver = com.epic60869.sky2m.sb.utils.container.ContainerSolverManager.getCurrentSolver();
                Map<String, Object> out = new HashMap<>();
                out.put("solver", solver == null ? null : solver.getClass().getSimpleName());
                for (Class<?> c = solver == null ? null : solver.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers())) continue;
                        f.setAccessible(true);
                        out.put(f.getName(), String.valueOf(f.get(solver)));
                    }
                }
                return out;
            }
            case "/parse": {
                // Dry run: which nodes a client command line resolves to, without executing it.
                var parse = ClientCommands.getActiveDispatcher().parse(q.get("c"),
                    (net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource) mc.player.connection.getSuggestionsProvider());
                Map<String, Object> out = new HashMap<>();
                List<String> nodes = new ArrayList<>();
                for (var node : parse.getContext().getLastChild().getNodes()) nodes.add(node.getNode().getName());
                out.put("nodes", nodes);
                out.put("executable", parse.getContext().getLastChild().getCommand() != null);
                out.put("unparsed", parse.getReader().getRemaining());
                out.put("errors", parse.getExceptions().toString());
                return out;
            }
            default: return "unknown " + path;
        }
    }

    private static JsonObject log(int since) {
        JsonObject out = new JsonObject();
        JsonArray lines = new JsonArray();
        synchronized (chat) {
            for (int i = Math.max(0, since); i < chat.size(); i++) lines.add(chat.get(i));
            out.addProperty("next", chat.size());
        }
        out.add("lines", lines);
        return out;
    }

    private static JsonObject state(Minecraft mc) {
        JsonObject out = new JsonObject();
        out.addProperty("screen", mc.gui.screen() == null ? null : mc.gui.screen().getClass().getName());
        out.addProperty("title", mc.gui.screen() == null ? null : mc.gui.screen().getTitle().getString());
        out.addProperty("onSkyblock", Sky2MLocation.onSkyblock());
        out.addProperty("area", Sky2MLocation.area());
        out.addProperty("server", mc.getCurrentServer() == null ? null : mc.getCurrentServer().ip);
        if (mc.player != null) {
            Vec3 pos = mc.player.position();
            out.addProperty("pos", String.format("%.1f %.1f %.1f", pos.x, pos.y, pos.z));
            out.addProperty("health", mc.player.getHealth());
            out.addProperty("slot", mc.player.getInventory().getSelectedSlot());
            out.add("held", item(mc.player.getInventory().getSelectedItem()));
            JsonArray tab = new JsonArray();
            for (PlayerInfo info : mc.player.connection.getListedOnlinePlayers()) {
                Component name = info.getTabListDisplayName();
                tab.add(name != null ? name.getString() : info.getProfile().name());
            }
            out.add("tab", tab);
        }
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            JsonArray slots = new JsonArray();
            for (Slot slot : screen.getMenu().slots) {
                if (slot.getItem().isEmpty()) continue;
                JsonObject s = item(slot.getItem());
                s.addProperty("i", slot.index);
                slots.add(s);
            }
            out.add("slots", slots);
        }
        return out;
    }

    private static JsonObject item(ItemStack stack) {
        JsonObject out = new JsonObject();
        out.addProperty("name", stack.getHoverName().getString());
        out.addProperty("id", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        out.addProperty("foil", stack.hasFoil());
        out.addProperty("count", stack.getCount());
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            JsonArray lines = new JsonArray();
            for (Component line : lore.lines()) lines.add(line.getString());
            out.add("lore", lines);
        }
        return out;
    }

    private static Map<String, Object> error(Throwable t) {
        Throwable cause = rootCause(t);
        Map<String, Object> out = new HashMap<>();
        out.put("error", cause.toString());
        List<String> trace = new ArrayList<>();
        for (StackTraceElement e : cause.getStackTrace()) {
            trace.add(e.toString());
            if (trace.size() >= 12) break;
        }
        out.put("trace", trace);
        return out;
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t;
    }

    private static String setConfig(String path, String value) throws Exception {
        Object target = Sky2MConfig.current();
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) target = field(target, parts[i]).get(target);
        Field f = field(target, parts[parts.length - 1]);
        Class<?> type = f.getType();
        Object parsed = type == boolean.class ? Boolean.parseBoolean(value) : type == int.class ? Integer.parseInt(value)
            : type == float.class ? Float.parseFloat(value) : type == double.class ? Double.parseDouble(value) : value;
        f.set(target, parsed);
        return path + " = " + parsed;
    }

    private static Field field(Object target, String name) throws NoSuchFieldException {
        Field f = target.getClass().getField(name);
        f.setAccessible(true);
        return f;
    }

    private static List<String> booleans() {
        List<String> out = new ArrayList<>();
        walk(Sky2MConfig.current(), "", 0, (p, f) -> { out.add(p); return null; });
        return out;
    }

    private static List<String> enableAll(String exclude) {
        List<String> skip = List.of(exclude.split(","));
        List<String> changed = new ArrayList<>();
        walk(Sky2MConfig.current(), "", 0, (p, target) -> {
            for (String s : skip) if (!s.isEmpty() && p.toLowerCase().contains(s.toLowerCase())) return null;
            changed.add(p);
            return Boolean.TRUE;
        });
        return changed;
    }

    /**
     * Puts every option back to its default, then turns on only the ones whose path contains one of {@code only}
     * (comma-separated, e.g. "diana,burrow"), minus {@code exclude}: just the features being tested.
     */
    private static List<String> focus(String only, String exclude) {
        List<String> want = List.of(only.toLowerCase().split(","));
        List<String> skip = List.of(exclude.toLowerCase().split(","));
        Map<Class<?>, Object> defaults = new HashMap<>();
        List<String> enabled = new ArrayList<>();
        walk(Sky2MConfig.current(), "", 0, (p, node) -> {
            String lower = p.toLowerCase();
            boolean match = want.stream().anyMatch(w -> !w.isEmpty() && lower.contains(w))
                && skip.stream().noneMatch(s -> !s.isEmpty() && lower.contains(s));
            if (match) {
                enabled.add(p);
                return Boolean.TRUE;
            }
            try {
                Object fresh = defaults.computeIfAbsent(node.getClass(), c -> {
                    try {
                        var ctor = c.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        return ctor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                });
                if (fresh == null) return null;
                return node.getClass().getField(p.substring(p.lastIndexOf('.') + 1)).getBoolean(fresh);
            } catch (ReflectiveOperationException e) {
                return null;
            }
        });
        return enabled;
    }

    /** Visits every public boolean option; a non-null result from {@code visit} is written to the field. */
    private static void walk(Object node, String prefix, int depth, java.util.function.BiFunction<String, Object, Object> visit) {
        if (node == null || depth > 8) return;
        for (Field f : node.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                String p = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
                if (f.getType() == boolean.class) {
                    Object v = visit.apply(p, node);
                    if (v != null) f.setBoolean(node, (Boolean) v);
                } else if (!f.getType().isPrimitive() && f.getType().getName().startsWith("com.epic60869")) {
                    walk(f.get(node), p, depth + 1, visit);
                }
            } catch (IllegalAccessException ignored) {}
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) out.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            else out.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    /** Sky2M's own objects field by field (most have no toString), collections and maps element by element. */
    private static String describe(Object o, int depth) {
        if (o == null) return "null";
        if (depth <= 0) return String.valueOf(o);
        if (o instanceof java.util.Map<?, ?> map) {
            List<String> parts = new ArrayList<>();
            map.forEach((k, v) -> parts.add(describe(k, depth - 1) + "=" + describe(v, depth - 1)));
            return "{" + String.join(", ", parts) + "}";
        }
        if (o instanceof java.util.Collection<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object e : list) parts.add(describe(e, depth - 1));
            return "[" + String.join(", ", parts) + "]";
        }
        if (!o.getClass().getName().startsWith("com.epic60869") || o.getClass().isEnum()) return String.valueOf(o);
        List<String> parts = new ArrayList<>();
        for (java.lang.reflect.Field f : o.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                parts.add(f.getName() + "=" + describe(f.get(o), depth - 1));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return o.getClass().getSimpleName() + "(" + String.join(", ", parts) + ")";
    }
}
