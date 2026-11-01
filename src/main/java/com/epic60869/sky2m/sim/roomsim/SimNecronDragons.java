package com.epic60869.sky2m.sim.roomsim;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * M7's last phase, the Wither King ({@link SimNecron}'s P5, at the bottom of the room).
 *
 * <p>Shaped to what the mod reads (Wither Dragons, Dragon Splits, Boss Phases, Rag Axe):
 * <ul>
 * <li>The Wither King's lines: "You... again?" opens the phase (Boss Phases' "Wither King", Dungeon Features),
 * "I no longer wish to fight, but I know that will not stop you." comes before the dragons (Rag Axe's prompt).
 * His boss bar is named "Wither King" (the relic place timer starts on it).</li>
 * <li>The five dragons spawn one after another, as since the Minister update: each one is announced by Hypixel's
 * flame particles (20 flames at y 19 over its statue, which Wither Dragons' spawn alerts and timers read) and
 * appears five seconds later at its statue, an ender dragon with the relic "collar" armour stand Wither Dragons
 * matches it by. It drifts between its statue and the middle of the room, breathes fire at players and hits
 * whoever comes close. Killed inside its statue's box (Wither Dragons' kill area) it is done; killed outside it
 * escapes and comes back later. When it is gone, Dragon Splits times it.</li>
 * <li>The five relics lie at the statues once Necron dies: walk into one (or click it) and "Name picked the
 * Corrupted Red Relic!" (the relic place timer's line), then walk it to its cauldron by the throne. A placed relic
 * weakens its dragon by a quarter (APPROXIMATE: the sim's own stand-in for what relics do).</li>
 * </ul>
 * All five dragons down at their statues and the Wither King gives up; the run ends.
 *
 * <p>The dragons have no AI (the fight flies them), so the vanilla ender dragon's own flight, block breaking and
 * End fight never run; their body parts, which are what arrows hit, are placed by the fight on the server.
 */
final class SimNecronDragons {

    /** Ticks from the flames to the dragon (Wither Dragons' 100-tick spawn timer). */
    private static final int SPAWN_TICKS = 100;
    private static final Vec3 MIDDLE = new Vec3(54.5, 15, 76.5);

    /** One dragon: Wither Dragons' spawn spot, kill box and relic cauldron (NoammAddons' coordinates). */
    private record Def(String name, String relicId, ChatFormatting colour, int rgb, Vec3 spawn, AABB area,
                       int[] cauldron) {
    }

    private static final List<Def> DEFS = List.of(
            new Def("Red", "RED_KING_RELIC", ChatFormatting.RED, 0xFF0000, new Vec3(27, 14, 59),
                    new AABB(14.5, 5, 45.5, 39.5, 28, 70.5), new int[]{51, 7, 42}),
            new Def("Orange", "ORANGE_KING_RELIC", ChatFormatting.GOLD, 0xFFAA00, new Vec3(85, 14, 56),
                    new AABB(72, 5, 47, 102, 28, 77), new int[]{57, 7, 42}),
            new Def("Green", "GREEN_KING_RELIC", ChatFormatting.GREEN, 0x00FF00, new Vec3(27, 14, 94),
                    new AABB(7, 5, 80, 37, 28, 110), new int[]{49, 7, 44}),
            new Def("Blue", "BLUE_KING_RELIC", ChatFormatting.AQUA, 0x55FFFF, new Vec3(84, 14, 94),
                    new AABB(71.5, 5, 82.5, 96.5, 26, 107.5), new int[]{59, 7, 44}),
            new Def("Purple", "PURPLE_KING_RELIC", ChatFormatting.DARK_PURPLE, 0xAA00AA, new Vec3(56, 14, 125),
                    new AABB(45.5, 6, 113.5, 68.5, 23, 136.5), new int[]{54, 7, 41}));

    private final SimNecron fight;
    private final double maxHealth;
    private int start;
    private final List<Def> queue = new ArrayList<>();
    private final List<Dragon> dragons = new ArrayList<>();
    private final List<Relic> relics = new ArrayList<>();
    private final boolean[] relicPlaced = new boolean[DEFS.size()];
    private int slain;
    private int nextSpawnAt;
    private int endAt = -1;
    private boolean finished;

    private static final class Dragon {
        final Def def;
        final int startedAt;
        DragonBody body;
        ArmorStand collar;
        double hp;
        int age;
        double angle;
        int nextBreath;
        int touchCooldown;

        Dragon(Def def, int startedAt) {
            this.def = def;
            this.startedAt = startedAt;
        }
    }

    private static final class Relic {
        final Def def;
        ArmorStand stand;

        Relic(Def def) {
            this.def = def;
        }
    }

    SimNecronDragons(SimNecron fight, ServerLevel level) {
        this.fight = fight;
        this.maxHealth = dragonHealth();
        this.start = fight.now();
        queue.addAll(DEFS);
        Collections.shuffle(queue);
        nextSpawnAt = start + 100;
        // Since Hypixel's 2026 dungeon update this is the first line of P5 (Boss Phases, Dungeon Features).
        SimBoss.say(level.getServer(), "Wither King", "You... again?");
        for (Def d : DEFS) {
            Relic r = new Relic(d);
            r.stand = relicStand(level, d, new Vec3(d.spawn().x + 0.5,
                    SimNecron.ground(level, (int) d.spawn().x, (int) d.spawn().z, 12, 1), d.spawn().z + 0.5));
            relics.add(r);
        }
    }

    /**
     * A dragon's health: 1,000,000,000 for each of the five (raised from 900M in 0.19.4), base damage 720,000 on M7
     * (https://hypixelskyblock.minecraft.wiki/w/Power_Dragon and the Flame, Apex, Ice and Soul Dragon pages). The
     * dragons are M7's only; elsewhere (/boss from another floor) they keep that health and hit for the F7 withers'
     * base damage ({@link SimNecron#witherDamage}, APPROXIMATE). The wiki gives the breath and the touch no number
     * of their own, so both deal the base damage. The Wither King himself has no health: Hypixel's cannot be
     * damaged, only beaten through his dragons (https://hypixelskyblock.minecraft.wiki/w/Wither_King).
     */
    private static double dragonHealth() {
        return 1_000_000_000;
    }

    boolean finished() {
        return finished;
    }

    /** The Wither King's bar: the share of statues still standing. */
    float progress() {
        return (DEFS.size() - slain) / (float) DEFS.size();
    }

    boolean isDragon(Entity e) {
        for (Dragon d : dragons) {
            if (d.body != null && d.body == e) {
                return true;
            }
        }
        return false;
    }

    boolean hit(LivingEntity e, float amount) {
        for (Dragon d : dragons) {
            if (d.body == e && d.hp > 0) {
                d.hp = Math.max(0, d.hp - amount);
                mirror(d);
                return true;
            }
        }
        return false;
    }

    void tick(ServerLevel level) {
        int now = fight.now();
        MinecraftServer server = level.getServer();
        if (now - start == 60) {
            // Hypixel's line, word for word: Rag Axe's dragon prompt.
            SimBoss.say(server, "Wither King", "I no longer wish to fight, but I know that will not stop you.");
        }
        if (endAt >= 0) {
            if (now >= endAt) {
                finished = true;
            }
            return;
        }
        tickRelics(level);
        if (now >= nextSpawnAt && !queue.isEmpty() && readyForNext(now)) {
            Dragon d = new Dragon(queue.removeFirst(), now);
            dragons.add(d);
            nextSpawnAt = now + 40;
        }
        for (Dragon d : new ArrayList<>(dragons)) {
            tickDragon(level, d, now);
        }
        if (slain >= DEFS.size()) {
            SimBoss.say(server, "Wither King", "Incredible. You did what I couldn't do myself.");
            endAt = now + 60;
        }
    }

    /** The next dragon waits until none is spawning and the ones up have been up a while (or there are none). */
    private boolean readyForNext(int now) {
        for (Dragon d : dragons) {
            if (d.body == null || d.age < 300) {
                return false;
            }
        }
        return true;
    }

    private void tickDragon(ServerLevel level, Dragon d, int now) {
        int since = now - d.startedAt;
        if (d.body == null) {
            if (since % 10 == 0) {
                flames(level, d.def);
            }
            if (since >= SPAWN_TICKS) {
                spawn(level, d);
            }
            return;
        }
        if (d.body.isRemoved()) {
            dragons.remove(d);
            return;
        }
        d.age++;
        fly(d);
        if (d.collar != null) {
            d.collar.snapTo(d.body.getX(), d.body.getY() + 1.5, d.body.getZ());
        }
        if (now >= d.nextBreath) {
            ServerPlayer target = fight.nearest(level, d.body.position());
            if (target != null) {
                SimNecron.beam(level, ParticleTypes.FLAME, d.body.position().add(0, 1, 0), target.position(), 1.0);
                fight.blast(target.position(), 3.0, 25, SimNecron.witherDamage(), d.def.rgb());
                level.playSound(null, d.body.getX(), d.body.getY(), d.body.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 2f, 1f);
            }
            d.nextBreath = now + 70;
        }
        if (--d.touchCooldown <= 0) {
            for (ServerPlayer p : fight.players(level)) {
                if (p.distanceToSqr(d.body) < 5 * 5) {
                    SimNecron.hurtPlayer(level, p, SimNecron.witherDamage(), p.getX() - d.body.getX(), p.getZ() - d.body.getZ(), 1.0, 0.6);
                    d.touchCooldown = 20;
                }
            }
        }
        if (d.hp <= 0) {
            die(level, d, now);
        }
    }

    /**
     * Hypixel's spawn warning, as Wither Dragons reads it: FLAME, 20 of them, at y 19 over the statue, spread
     * 2/3/2, speed 0, on whole x and z. Sent to every player whatever the distance (vanilla's 32-block cut-off
     * would drop it for half the room).
     */
    private static void flames(ServerLevel level, Def def) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            level.sendParticles(p, ParticleTypes.FLAME, true, false, Math.floor(def.spawn().x), 19, Math.floor(def.spawn().z),
                    20, 2, 3, 2, 0);
        }
    }

    private void spawn(ServerLevel level, Dragon d) {
        DragonBody body = new DragonBody(level);
        body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        body.setHealth(1000);
        body.setPersistenceRequired();
        body.addTag(SimNecron.TAG);
        body.setCustomName(Component.literal(d.def.name() + " Dragon").withStyle(d.def.colour(), ChatFormatting.BOLD));
        body.snapTo(d.def.spawn().x + 0.5, d.def.spawn().y, d.def.spawn().z + 0.5, 0f, 0f);
        if (!level.addFreshEntity(body)) {
            return;
        }
        d.body = body;
        d.hp = maxHealth * (relicPlaced[DEFS.indexOf(d.def)] ? 0.75 : 1.0);
        d.nextBreath = fight.now() + 60;
        d.angle = Math.random() * Math.PI * 2;
        mirror(d);
        // The collar Wither Dragons tells the dragons apart by: an armour stand over it wearing its relic.
        ArmorStand collar = new ArmorStand(level, body.getX(), body.getY() + 1.5, body.getZ());
        collar.setInvisible(true);
        collar.setNoGravity(true);
        collar.setInvulnerable(true);
        collar.addTag(SimNecron.TAG);
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", d.def.relicId());
        head.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        collar.setItemSlot(EquipmentSlot.HEAD, head);
        if (level.addFreshEntity(collar)) {
            d.collar = collar;
        }
        level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4f, 1f);
    }

    /**
     * Circles a point that drifts from the statue toward the middle of the room and back (about twenty seconds a
     * round), so a dragon spends part of its time outside its kill box.
     */
    private static void fly(Dragon d) {
        Vec3 statue = new Vec3(d.def.spawn().x + 0.5, d.def.spawn().y, d.def.spawn().z + 0.5);
        double w = 0.5 - 0.5 * Math.cos(Math.PI * 2 * d.age / 400.0);
        Vec3 anchor = statue.lerp(MIDDLE, w * 0.8);
        d.angle += 0.05;
        Vec3 to = new Vec3(anchor.x + Math.cos(d.angle) * 5, anchor.y + 2 * Math.sin(d.age / 20.0), anchor.z + Math.sin(d.angle) * 5);
        Vec3 from = d.body.position();
        Vec3 v = to.subtract(from);
        // The dragon's model (and its head part) faces (sin yaw, -cos yaw).
        float yaw = v.horizontalDistanceSqr() < 1e-4 ? d.body.getYRot() : (float) Math.toDegrees(Math.atan2(v.x, -v.z));
        d.body.snapTo(to.x, to.y, to.z, yaw, 0f);
    }

    /** The vanilla health, for Wither Dragons' health display: the pool's share of 1000, never 0 (that is death). */
    private void mirror(Dragon d) {
        if (d.body != null) {
            d.body.setHealth((float) Math.max(1, 1000 * d.hp / Math.max(1, maxHealth)));
        }
    }

    private void die(ServerLevel level, Dragon d, int now) {
        MinecraftServer server = level.getServer();
        boolean inArea = d.def.area().contains(d.body.position());
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, d.body.getX(), d.body.getY(), d.body.getZ(), 2, 1, 1, 1, 0);
        level.playSound(null, d.body.getX(), d.body.getY(), d.body.getZ(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 3f, 1.2f);
        d.body.discard();
        if (d.collar != null) {
            d.collar.discard();
        }
        dragons.remove(d);
        Component name = Component.literal("The " + d.def.name().toUpperCase(Locale.ROOT) + " Dragon").withStyle(d.def.colour());
        if (inArea) {
            slain++;
            SimNecron.broadcast(server, name.copy().append(Component.literal(" was slain at its statue! (" + slain + "/"
                    + DEFS.size() + ")").withStyle(ChatFormatting.GREEN)));
        } else {
            // Killed away from its statue: it comes back (APPROXIMATE, the sim's version of a missed kill area).
            queue.add(d.def);
            SimNecron.broadcast(server, name.copy().append(Component.literal(" died away from its statue and will be back!")
                    .withStyle(ChatFormatting.RED)));
        }
        nextSpawnAt = Math.max(nextSpawnAt, now + 40);
    }

    // ---------------------------------------------------------------------------------------------- relics

    private static ArmorStand relicStand(ServerLevel level, Def d, Vec3 at) {
        ArmorStand s = new ArmorStand(level, at.x, at.y, at.z);
        s.setInvisible(true);
        s.setNoGravity(true);
        s.setInvulnerable(true);
        s.setGlowingTag(true);
        s.addTag(SimNecron.TAG);
        s.setCustomName(Component.literal("Corrupted " + d.name() + " Relic").withStyle(d.colour()));
        s.setCustomNameVisible(true);
        s.setItemSlot(EquipmentSlot.HEAD, relicItem(d));
        level.addFreshEntity(s);
        return s;
    }

    private static ItemStack relicItem(Def d) {
        ItemStack item = new ItemStack(Items.PLAYER_HEAD);
        // The name is what Wither Dragons' relic place timer checks a placed relic's stand by.
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Corrupted " + d.name() + " Relic").withStyle(d.colour()));
        return item;
    }

    private void tickRelics(ServerLevel level) {
        for (ServerPlayer p : fight.players(level)) {
            String held = fight.carried(p);
            if (held == null) {
                for (Relic r : relics) {
                    if (r.stand != null && p.distanceToSqr(r.stand) < 2.0 * 2.0) {
                        pick(level, p, r);
                        break;
                    }
                }
                continue;
            }
            for (int i = 0; i < DEFS.size(); i++) {
                Def d = DEFS.get(i);
                int[] c = d.cauldron();
                double dx = p.getX() - (c[0] + 0.5);
                double dz = p.getZ() - (c[2] + 0.5);
                if (d.name().equals(held) && dx * dx + dz * dz < 2.2 * 2.2 && Math.abs(p.getY() - c[1]) < 3) {
                    place(level, p, i);
                    break;
                }
            }
        }
    }

    private void pick(ServerLevel level, ServerPlayer p, Relic r) {
        r.stand.discard();
        r.stand = null;
        fight.carry(p, r.def.name(), relicItem(r.def));
        // Hypixel's line, word for word: Wither Dragons' relic place timer waits for this relic from here.
        SimNecron.broadcast(level.getServer(), Component.literal(p.getGameProfile().name() + " picked the Corrupted "
                + r.def.name() + " Relic!"));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f);
    }

    private void place(ServerLevel level, ServerPlayer p, int i) {
        Def d = DEFS.get(i);
        fight.uncarry(p);
        relicPlaced[i] = true;
        int[] c = d.cauldron();
        // Where NoammAddons' place timer looks for a placed relic: one block over from the cauldron on x and z.
        ArmorStand s = new ArmorStand(level, c[0] + 1.0, c[1] - 1, c[2] + 1.0);
        s.setInvisible(true);
        s.setNoGravity(true);
        s.setInvulnerable(true);
        s.addTag(SimNecron.TAG);
        s.setItemSlot(EquipmentSlot.HEAD, relicItem(d));
        level.addFreshEntity(s);
        level.playSound(null, c[0] + 0.5, c[1], c[2] + 0.5, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.5f, 1f);
        SimNecron.broadcast(level.getServer(), Component.literal("The Corrupted " + d.name() + " Relic was placed! ")
                .withStyle(d.colour()).append(Component.literal("Its dragon is weakened.").withStyle(ChatFormatting.GRAY)));
        for (Dragon dr : dragons) {
            if (dr.def == d && dr.body != null && dr.hp > 0) {
                dr.hp = Math.max(1, dr.hp - maxHealth * 0.25);
                mirror(dr);
            }
        }
    }

    /** A click on a relic (the fight's use/attack callbacks). */
    void touch(ServerPlayer p, Entity e) {
        if (fight.carried(p) != null) {
            return;
        }
        for (Relic r : relics) {
            if (r.stand == e && p.distanceToSqr(e) < 5 * 5) {
                pick((ServerLevel) p.level(), p, r);
                return;
            }
        }
    }

    void discard() {
        for (Dragon d : dragons) {
            if (d.body != null) {
                d.body.discard();
            }
            if (d.collar != null) {
                d.collar.discard();
            }
        }
        dragons.clear();
        for (Relic r : relics) {
            if (r.stand != null) {
                r.stand.discard();
                r.stand = null;
            }
        }
    }

    /**
     * A dragon's body: no AI, so none of the vanilla dragon's flight, block breaking or End fight. That also stops
     * vanilla moving its parts, so they are put round the body here (the hit boxes arrows find), roughly where the
     * vanilla dragon has them: head and neck ahead, wings to the sides, tail behind.
     */
    static final class DragonBody extends EnderDragon {
        DragonBody(Level level) {
            super(EntityTypes.ENDER_DRAGON, level);
            setNoAi(true);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide()) {
                return;
            }
            double s = Math.sin(Math.toRadians(getYRot()));
            double c = Math.cos(Math.toRadians(getYRot()));
            EnderDragonPart[] parts = getSubEntities();
            // Vanilla's order: head, neck, body, tail x3, wing x2.
            double[][] at = {
                {s * 6.5, 1, -c * 6.5}, {s * 5.5, 1, -c * 5.5}, {s * 0.5, 0, -c * 0.5},
                {-s * 2.5, 0, c * 2.5}, {-s * 4.5, 0, c * 4.5}, {-s * 6.5, 0, c * 6.5},
                {c * 4.5, 2, s * 4.5}, {-c * 4.5, 2, -s * 4.5},
            };
            for (int i = 0; i < parts.length && i < at.length; i++) {
                parts[i].setPos(getX() + at[i][0], getY() + at[i][1], getZ() + at[i][2]);
            }
        }

        @Override
        public void checkDespawn() {
        }
    }
}
