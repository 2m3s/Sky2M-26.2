package com.epic60869.sky2m.sim.roomsim;

import com.epic60869.sky2m.sim.util.SimItemIds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 4's boss, Thorn, in his ring of spirit trees.
 *
 * <p>Thorn floats over the middle and cannot be hurt by anything but the <b>Spirit Bow</b>. The loop, as on
 * Hypixel and tuned for practice rather than copied number for number:
 * <ol>
 * <li><b>Spirit animals</b> run round the ring - Spirit Wolves (which bite), Sheep, Chickens, Rabbits and Bats -
 * and Thorn keeps sending more. Each one killed lights the next coal block of the ring round the arena into a
 * sea lantern (the same blocks, in the same order, that the mod's Spirit Bear HUD watches on Hypixel), and each
 * leaves something behind ({@link Animal}).</li>
 * <li>When this life's share of the ring is lit, the <b>Spirit Bear</b> appears {@link #BEAR_DELAY_TICKS} ticks
 * later (Hypixel's 68, Odin's number). Killing it drops the Spirit Bow.</li>
 * <li>Pick the bow up and <b>shoot Thorn</b> with it: one hit takes one of his lives. The bow is good for one
 * arrow and crumbles if it is held too long; a miss or a crumbled bow calls a new bear, without the kills.</li>
 * </ol>
 * Thorn shoots spikes at everyone the whole time, and in Master Mode also grows roots under each player's feet
 * that snare and hurt whoever is still standing there when they close.
 *
 * <p><b>Lives.</b> F4 takes {@link #LIVES_NORMAL} hits. Master Mode takes {@link #LIVES_MASTER}. Hypixel's Thorn has
 * "4-6" health, one taken per Spirit Bow hit (https://hypixelskyblock.minecraft.wiki/w/Thorn), with no F4/M4 split
 * on the wiki; one extra life here fills the M4 ring's 30 blocks in five even shares of 6, as F4's 25 fill in four.
 *
 * <p><b>Health and damage.</b> The spirit animals and the Spirit Bear have Hypixel's health and hit ({@link Animal};
 * https://hypixelskyblock.minecraft.wiki/w/Thorn, https://hypixelskyblock.minecraft.wiki/w/Spirit_Bear), before the
 * player's Defense. The wiki has no number for Thorn's own spikes or the M4 roots, so those stay the sim's
 * ({@link #mobHits}: a few of the floor's zombie hits, APPROXIMATE).
 *
 * <p>Only the opening line is Hypixel's (Boss Phases opens "Thorn" on it, word for word). Thorn has no death
 * line the mod reads - Clean End and the splits end F4 on the chests line - so the rest are the sim's own.
 */
final class SimThorn extends SimBoss.Fight {

    private static final int LIVES_NORMAL = 4;
    private static final int LIVES_MASTER = 5;
    private static final int BEAR_DELAY_TICKS = 68;
    /** A new bear after a miss or a crumbled bow. APPROXIMATE. */
    private static final int BEAR_AGAIN_TICKS = 100;
    /** How long a picked-up Spirit Bow lasts: F4, then M4. APPROXIMATE. */
    private static final int BOW_TICKS_NORMAL = 600;
    private static final int BOW_TICKS_MASTER = 400;
    /** How long a dropped bow lies on the ground. */
    private static final int DROP_TICKS = 600;
    private static final double PICKUP_RANGE = 1.8;
    private static final int MAX_SPIRITS_NORMAL = 8;
    private static final int MAX_SPIRITS_MASTER = 12;
    private static final double ROOT_RADIUS = 2.5;

    /** The sim's own id for the boss's Spirit Bow. Not Hypixel's: nothing reads it but this class. */
    static final String SPIRIT_BOW_ID = "SIM_THORN_SPIRIT_BOW";

    /**
     * The ring of coal blocks round the arena, in the order they light, source coordinates (y 77). Odin's lists
     * ({@code SpiritBear}, F4 and M4), which are also exactly where this arena's coal blocks are.
     */
    private static final int[][] RING_F4 = {
            {-3, 33}, {-9, 31}, {-16, 26}, {-20, 20}, {-23, 13}, {-24, 6}, {-24, 0}, {-22, -7}, {-18, -13},
            {-12, -19}, {-5, -22}, {1, -24}, {8, -24}, {14, -23}, {21, -19}, {27, -14}, {31, -8}, {33, -1}, {34, 5},
            {33, 12}, {31, 19}, {27, 25}, {20, 30}, {14, 33}, {7, 34}};
    private static final int[][] RING_M4 = {
            {-2, 33}, {-7, 32}, {-13, 28}, {-17, 24}, {-21, 18}, {-23, 13}, {-24, 7}, {-24, 2}, {-23, -4}, {-21, -9},
            {-17, -14}, {-12, -19}, {-6, -22}, {-1, -23}, {5, -24}, {10, -24}, {16, -22}, {21, -19}, {27, -15},
            {30, -10}, {32, -5}, {34, 1}, {34, 7}, {33, 12}, {31, 18}, {28, 23}, {23, 28}, {18, 31}, {12, 33},
            {7, 34}};
    private static final int RING_Y = 77;

    /**
     * The spirit animals and what each leaves when it dies. The effects are APPROXIMATE - the sim's reading of
     * Hypixel's, kept small: none of them is worth chasing on its own. Health and hit are Hypixel's for F4 and M4:
     * the Thorn page lists each animal as a "min-max" range, read as F4-M4 the way the wiki writes every boss's
     * Normal and Master numbers (https://hypixelskyblock.minecraft.wiki/w/Thorn); the bear has its own page
     * (https://hypixelskyblock.minecraft.wiki/w/Spirit_Bear).
     */
    private enum Animal {
        /** Bites. Leaves nothing. */
        WOLF("Spirit Wolf", 450_000, 11_250_000, 3_000, 100_000),
        /** Heals everyone in the ring a little. */
        SHEEP("Spirit Sheep", 400_000, 10_000_000, 19_040, 666_400),
        /** Speed for whoever got it. */
        CHICKEN("Spirit Chicken", 120_000, 3_000_000, 928, 26_880),
        /** Jump Boost for whoever got it. */
        RABBIT("Spirit Rabbit", 260_000, 6_500_000, 3_162, 60_000),
        /** Mana for whoever got it. */
        BAT("Spirit Bat", 50_000, 1_250_000, 2_380, 59_500),
        /** Drops the Spirit Bow. Spawned by the ring, never by Thorn. */
        BEAR("Spirit Bear", 5_000_000, 180_000_000, 8_704, 154_700);

        final String display;
        final double healthF4;
        final double healthM4;
        /** The animal's hit on a player when it attacks (only the wolves and the bear do), before Defense. */
        final double damageF4;
        final double damageM4;

        Animal(String display, double healthF4, double healthM4, double damageF4, double damageM4) {
            this.display = display;
            this.healthF4 = healthF4;
            this.healthM4 = healthM4;
            this.damageF4 = damageF4;
            this.damageM4 = damageM4;
        }
    }

    private static final Animal[] SUMMONS = {Animal.WOLF, Animal.WOLF, Animal.SHEEP, Animal.CHICKEN, Animal.RABBIT,
            Animal.BAT};

    private static final class Spirit {
        final Animal kind;
        final Mob body;
        final SimBoss.Health health;

        Spirit(Animal kind, Mob body, double hp) {
            this.kind = kind;
            this.body = body;
            this.health = new SimBoss.Health(hp);
        }
    }

    private enum Phase { INTRO, FIGHT, DEAD }

    /** Where the bow loop stands. */
    private enum Bow { GATHER, BEAR_SOON, BEAR, DROPPED, HELD, SHOT }

    private record Bolt(Vec3 pos, Vec3 vel, int dieAt, float damage) {
    }

    private record Root(Vec3 at, int closeAt) {
    }

    private Phase phase = Phase.INTRO;
    private int phaseTicks;
    private int clock;
    private Ghast thorn;
    private final int livesMax = master() ? LIVES_MASTER : LIVES_NORMAL;
    private int lives = livesMax;
    private final int[][] ring = master() ? RING_M4 : RING_F4;
    private int lit;
    private final List<Spirit> spirits = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private final List<Root> roots = new ArrayList<>();
    private Bow bow = Bow.GATHER;
    private int bowTimer;
    private Spirit bear;
    private ItemEntity drop;
    private UUID holder;
    private boolean arrowGiven;
    private AbstractArrow spiritArrow;
    private int nextSummon;
    private int nextSpikes;
    private int nextRoots;
    private int hurtTicks;
    private int onlyBowNote;
    private double orbit;

    SimThorn(SimBossArena arena) {
        super(arena);
    }

    private static boolean master() {
        String f = SimState.floorLabel();
        return f != null && f.toUpperCase(Locale.ROOT).startsWith("M");
    }

    /**
     * {@code k} of the floor's regular-mob hits, as the vanilla amount {@link SimVitals#hypixelDamage} scales back up
     * (it divides by a regular mob's swing after EASY's scaling and multiplies by the floor's zombie hit). For the
     * spikes and roots, which have no Hypixel number. APPROXIMATE.
     */
    private static float mobHits(double k) {
        float ref = SimHealth.meleeDamage(SimState.floorLabel(), SimHealth.Role.REGULAR);
        return (float) (k * Math.min(ref, ref / 2f + 1f));
    }

    /** The ring blocks this life needs lit before its bear: an even share of the ring, the last life the rest. */
    private int threshold() {
        int life = livesMax - lives;
        return (int) Math.round((double) ring.length * (life + 1) / livesMax);
    }

    @Override
    String name() {
        return "Thorn";
    }

    @Override
    void start(ServerLevel level) {
        // Hypixel's first line, word for word: Boss Phases opens "Thorn" on it, and Dungeon Boss reads it.
        SimBoss.say(level.getServer(), "Thorn",
                "Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!");
        phase = Phase.INTRO;
        phaseTicks = 0;
        spawnThorn(level);
    }

    @Override
    void tick(ServerLevel level, int t) {
        phaseTicks++;
        clock++;
        boolean master = master();
        if (phase == Phase.INTRO) {
            if (phaseTicks == 40) {
                SimBoss.say(level.getServer(), "Thorn", "Only my Spirit Bow can touch me. Earn it from my animals!");
            }
            if (phaseTicks >= 60) {
                for (int i = 0; i < (master ? 6 : 4); i++) {
                    summon(level, SUMMONS[i % SUMMONS.length]);
                }
                nextSummon = 100;
                nextSpikes = 40;
                nextRoots = 160;
                phase = Phase.FIGHT;
                phaseTicks = 0;
            }
        } else if (phase == Phase.FIGHT) {
            if (thorn == null || thorn.isRemoved()) {
                spawnThorn(level);
            }
            hover(level, master);
            tickSpirits(level);
            if (--nextSummon <= 0) {
                for (int i = 0; i < (master ? 3 : 2); i++) {
                    summon(level, SUMMONS[ThreadLocalRandom.current().nextInt(SUMMONS.length)]);
                }
                nextSummon = master ? 70 : 100;
            }
            if (--nextSpikes <= 0) {
                spikes(level, master);
                nextSpikes = master ? 40 : 60;
            }
            if (master && --nextRoots <= 0) {
                plantRoots(level);
                nextRoots = 200;
            }
            tickBow(level, master);
            if (hurtTicks > 0) {
                hurtTicks--;
            }
        }
        tickBolts(level);
        tickRoots(level);
        if (onlyBowNote > 0) {
            onlyBowNote--;
        }
        nameTags();
    }

    // ------------------------------------------------------------------ Thorn

    private Vec3 middle(ServerLevel level) {
        BlockPos c = arena.toWorld(arena.centre);
        return new Vec3(c.getX() + 0.5, ground(level, c.getX(), c.getZ(), c.getY() + 2), c.getZ() + 0.5);
    }

    private static int ground(ServerLevel level, int x, int z, int fromY) {
        for (int y = fromY; y > fromY - 12; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                return y;
            }
        }
        return fromY;
    }

    private void spawnThorn(ServerLevel level) {
        Vec3 m = middle(level);
        Ghast g = new Ghast(EntityTypes.GHAST, level);
        g.setNoAi(true);
        g.setNoGravity(true);
        g.setPersistenceRequired();
        g.setCustomNameVisible(true);
        g.snapTo(m.x, m.y + 5, m.z, 180f, 0f);
        level.addFreshEntity(g);
        thorn = g;
    }

    /** A slow circle over the middle, a little faster in Master Mode, bobbing so a shot has to be aimed. */
    private void hover(ServerLevel level, boolean master) {
        if (thorn == null) {
            return;
        }
        Vec3 m = middle(level);
        orbit += master ? 0.025 : 0.015;
        double x = m.x + Math.cos(orbit) * 4;
        double z = m.z + Math.sin(orbit) * 4;
        double y = m.y + 5 + Math.sin(clock * 0.06) * 1.0;
        ServerPlayer near = nearest(SimBoss.playersInRing(level), thorn.position());
        float yaw = thorn.getYRot();
        if (near != null) {
            yaw = (float) (Math.toDegrees(Math.atan2(near.getZ() - z, near.getX() - x)) - 90);
        }
        thorn.snapTo(x, y, z, yaw, 0f);
        thorn.setYHeadRot(yaw);
        thorn.setYBodyRot(yaw);
    }

    /** A spike at every player. */
    private void spikes(ServerLevel level, boolean master) {
        if (thorn == null) {
            return;
        }
        Vec3 from = thorn.position().add(0, 2, 0);
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            Vec3 dir = p.position().add(0, 1, 0).subtract(from).normalize();
            bolts.add(new Bolt(from, dir.scale(master ? 0.9 : 0.7), clock + 80, mobHits(master ? 2.0 : 1.5)));
        }
        level.playSound(null, from.x, from.y, from.z, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1f, 1.3f);
    }

    /** Master Mode: roots under each player that close a second and a half later. */
    private void plantRoots(ServerLevel level) {
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            roots.add(new Root(p.position(), clock + 30));
        }
    }

    private void tickRoots(ServerLevel level) {
        if (roots.isEmpty()) {
            return;
        }
        DustParticleOptions green = new DustParticleOptions(0x2E7D32, 1.3f);
        List<Root> done = new ArrayList<>();
        for (Root r : roots) {
            if (clock < r.closeAt()) {
                if (clock % 3 == 0) {
                    for (int i = 0; i < 14; i++) {
                        double a = Math.PI * 2 * i / 14;
                        level.sendParticles(green, r.at().x + Math.cos(a) * ROOT_RADIUS, r.at().y + 0.1,
                                r.at().z + Math.sin(a) * ROOT_RADIUS, 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            level.sendParticles(ParticleTypes.COMPOSTER, r.at().x, r.at().y + 0.5, r.at().z, 30, 1.2, 0.4, 1.2, 0);
            for (ServerPlayer p : SimBoss.playersInRing(level)) {
                double dx = p.getX() - r.at().x;
                double dz = p.getZ() - r.at().z;
                if (dx * dx + dz * dz <= ROOT_RADIUS * ROOT_RADIUS && Math.abs(p.getY() - r.at().y) < 2.5) {
                    p.hurtServer(level, level.damageSources().magic(), mobHits(3.0));
                    p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                }
            }
            done.add(r);
        }
        roots.removeAll(done);
    }

    private void tickBolts(ServerLevel level) {
        if (bolts.isEmpty()) {
            return;
        }
        DustParticleOptions thornGreen = new DustParticleOptions(0x7CB342, 1.0f);
        List<Bolt> next = new ArrayList<>();
        List<ServerPlayer> players = SimBoss.playersInRing(level);
        for (Bolt b : bolts) {
            Vec3 pos = b.pos().add(b.vel());
            BlockPos block = BlockPos.containing(pos);
            if (clock >= b.dieAt() || !level.getBlockState(block).getCollisionShape(level, block).isEmpty()) {
                level.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.02);
                continue;
            }
            level.sendParticles(thornGreen, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            boolean hit = false;
            for (ServerPlayer p : players) {
                if (p.getBoundingBox().inflate(0.4).contains(pos)) {
                    p.hurtServer(level, level.damageSources().magic(), b.damage());
                    double len = Math.max(0.01, Math.sqrt(b.vel().x * b.vel().x + b.vel().z * b.vel().z));
                    p.setDeltaMovement(b.vel().x / len * 0.4, 0.25, b.vel().z / len * 0.4);
                    p.hurtMarked = true;
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                next.add(new Bolt(pos, b.vel(), b.dieAt(), b.damage()));
            }
        }
        bolts.clear();
        bolts.addAll(next);
    }

    // ------------------------------------------------------------------ the spirit animals

    private int maxSpirits() {
        return master() ? MAX_SPIRITS_MASTER : MAX_SPIRITS_NORMAL;
    }

    /** A random standable spot in the ring, away from the middle. */
    private Vec3 ringSpot(ServerLevel level) {
        BlockPos c = arena.toWorld(arena.centre);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double a = rnd.nextDouble() * Math.PI * 2;
        double d = 8 + rnd.nextDouble() * Math.max(1, arena.radius - 12);
        int x = (int) Math.floor(c.getX() + 0.5 + Math.cos(a) * d);
        int z = (int) Math.floor(c.getZ() + 0.5 + Math.sin(a) * d);
        return new Vec3(x + 0.5, ground(level, x, z, c.getY() + 4), z + 0.5);
    }

    private void summon(ServerLevel level, Animal kind) {
        int living = 0;
        for (Spirit s : spirits) {
            if (s.kind != Animal.BEAR) {
                living++;
            }
        }
        if (kind != Animal.BEAR && living >= maxSpirits()) {
            return;
        }
        Mob body = switch (kind) {
            case WOLF -> new Wolf(EntityTypes.WOLF, level);
            case SHEEP -> new Sheep(EntityTypes.SHEEP, level);
            case CHICKEN -> new Chicken(EntityTypes.CHICKEN, level);
            case RABBIT -> new Rabbit(EntityTypes.RABBIT, level);
            case BAT -> new Bat(EntityTypes.BAT, level);
            case BEAR -> new PolarBear(EntityTypes.POLAR_BEAR, level);
        };
        Vec3 at = ringSpot(level);
        body.setPersistenceRequired();
        body.setCustomNameVisible(true);
        body.snapTo(at.x, at.y + (kind == Animal.BAT ? 2 : 0), at.z, ThreadLocalRandom.current().nextFloat() * 360,
                0f);
        level.addFreshEntity(body);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.8, at.z, 12, 0.3, 0.5, 0.3, 0.02);
        double hp = SimHealth.byMode(kind.healthF4, kind.healthM4);
        SimHealth.setBossHit(body, SimHealth.byMode(kind.damageF4, kind.damageM4));
        Spirit s = new Spirit(kind, body, hp);
        spirits.add(s);
        if (kind == Animal.BEAR) {
            bear = s;
        }
    }

    /** Keeps the biters on a player and every animal inside the ring. */
    private void tickSpirits(ServerLevel level) {
        BlockPos c = arena.toWorld(arena.centre);
        double limit = (arena.radius + 2.0) * (arena.radius + 2.0);
        List<ServerPlayer> players = SimBoss.playersInRing(level);
        for (Spirit s : new ArrayList<>(spirits)) {
            if (s.body.isRemoved() || !s.body.isAlive()) {
                // Gone some other way (the void, /kill): forget it without counting it.
                spirits.remove(s);
                if (s == bear) {
                    bear = null;
                    bow = Bow.BEAR_SOON;
                    bowTimer = BEAR_AGAIN_TICKS;
                }
                continue;
            }
            double dx = s.body.getX() - (c.getX() + 0.5);
            double dz = s.body.getZ() - (c.getZ() + 0.5);
            if (dx * dx + dz * dz > limit || Math.abs(s.body.getY() - c.getY()) > 12) {
                Vec3 back = ringSpot(level);
                s.body.snapTo(back.x, back.y + (s.kind == Animal.BAT ? 2 : 0), back.z);
            }
            if ((s.kind == Animal.WOLF || s.kind == Animal.BEAR) && clock % 20 == 0) {
                ServerPlayer target = nearest(players, s.body.position());
                if (target != null) {
                    s.body.setTarget(target);
                }
            }
        }
    }

    private void killSpirit(ServerLevel level, Spirit s) {
        Vec3 at = s.body.position();
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.6, at.z, 15, 0.3, 0.4, 0.3, 0.05);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.6, at.z, 8, 0.3, 0.3, 0.3, 0.02);
        s.body.discard();
        spirits.remove(s);
        ServerPlayer near = nearest(SimBoss.playersInRing(level), at);
        switch (s.kind) {
            case SHEEP -> {
                for (ServerPlayer p : SimBoss.playersInRing(level)) {
                    SimVitals.healFraction(p, 0.15);
                    level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2, p.getZ(), 3, 0.3, 0.2, 0.3, 0);
                }
            }
            case CHICKEN -> {
                if (near != null) {
                    near.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
                }
            }
            case RABBIT -> {
                if (near != null) {
                    near.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 100, 1));
                }
            }
            case BAT -> {
                if (near != null) {
                    SimVitals.refund(near, SimVitals.of(near).maxMana * 0.25);
                }
            }
            case BEAR -> {
                bear = null;
                dropBow(level, at);
                return;
            }
            default -> {
            }
        }
        lightNext(level);
    }

    /** One more ring block lit for a kill, while this life still needs it. */
    private void lightNext(ServerLevel level) {
        if (bow != Bow.GATHER || lit >= threshold() || lit >= ring.length) {
            return;
        }
        int[] r = ring[lit++];
        BlockPos pos = arena.toWorld(r[0], RING_Y, r[1]);
        level.setBlock(pos, Blocks.SEA_LANTERN.defaultBlockState(), 3);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6,
                0.3, 0.3, 0.3, 0.02);
        if (lit >= threshold()) {
            bow = Bow.BEAR_SOON;
            bowTimer = BEAR_DELAY_TICKS;
        }
    }

    // ------------------------------------------------------------------ the Spirit Bear and the Spirit Bow

    private void tickBow(ServerLevel level, boolean master) {
        switch (bow) {
            case BEAR_SOON -> {
                if (--bowTimer <= 0) {
                    summon(level, Animal.BEAR);
                    bow = Bow.BEAR;
                    SimBoss.tell(level.getServer(), Component.literal("A Spirit Bear has appeared!")
                            .withStyle(ChatFormatting.GOLD));
                    level.playSound(null, bear.body.getX(), bear.body.getY(), bear.body.getZ(),
                            SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 1.2f, 0.8f);
                }
            }
            case DROPPED -> {
                if (drop == null || drop.isRemoved() || --bowTimer <= 0) {
                    if (drop != null) {
                        drop.discard();
                        drop = null;
                    }
                    lost(level, "The Spirit Bow faded away.");
                    return;
                }
                level.sendParticles(ParticleTypes.END_ROD, drop.getX(), drop.getY() + 0.5, drop.getZ(), 1,
                        0.2, 0.2, 0.2, 0);
                for (ServerPlayer p : SimBoss.playersInRing(level)) {
                    if (p.distanceToSqr(drop) <= PICKUP_RANGE * PICKUP_RANGE) {
                        pickUp(level, p, master);
                        break;
                    }
                }
            }
            case HELD -> {
                ServerPlayer p = holderPlayer(level);
                if (p == null || --bowTimer <= 0) {
                    if (p != null) {
                        takeBow(p);
                        p.sendSystemMessage(Component.literal("The Spirit Bow crumbled in your hands!")
                                .withStyle(ChatFormatting.RED));
                    }
                    lost(level, p == null ? "The Spirit Bow was lost." : null);
                    return;
                }
                if (bowTimer % 20 == 0) {
                    p.sendSystemMessage(Component.literal("Spirit Bow: " + bowTimer / 20 + "s - shoot Thorn!")
                            .withStyle(ChatFormatting.GOLD), true);
                }
                watchForShot(level, p);
            }
            case SHOT -> {
                AbstractArrow a = spiritArrow;
                if (a != null && thorn != null && !a.isRemoved()
                        && thorn.getBoundingBox().inflate(1.0).intersects(a.getBoundingBox())) {
                    thornHit(level);
                } else if (a == null || a.isRemoved() || a.tickCount > 100
                        || a.tickCount > 5 && a.getDeltaMovement().lengthSqr() < 0.01) {
                    if (a != null) {
                        a.discard();
                    }
                    spiritArrow = null;
                    lost(level, "The Spirit Bow's arrow missed Thorn!");
                }
            }
            default -> {
            }
        }
    }

    private void dropBow(ServerLevel level, Vec3 at) {
        ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, spiritBow());
        // Picked up by hand ({@link #pickUp}), so the bow and its arrow arrive together.
        item.setNeverPickUp();
        item.setGlowingTag(true);
        level.addFreshEntity(item);
        drop = item;
        bow = Bow.DROPPED;
        bowTimer = DROP_TICKS;
        SimBoss.tell(level.getServer(), Component.literal("The Spirit Bear dropped the Spirit Bow!")
                .withStyle(ChatFormatting.GOLD));
    }

    private void pickUp(ServerLevel level, ServerPlayer p, boolean master) {
        if (drop != null) {
            drop.discard();
            drop = null;
        }
        p.getInventory().add(spiritBow());
        // A vanilla bow needs an arrow to draw; the sim's other bows do not use vanilla arrows.
        arrowGiven = p.getInventory().add(new ItemStack(Items.ARROW));
        holder = p.getUUID();
        bow = Bow.HELD;
        bowTimer = master ? BOW_TICKS_MASTER : BOW_TICKS_NORMAL;
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f,
                1.6f);
        p.sendSystemMessage(Component.literal("You picked up the Spirit Bow! One arrow - make it count.")
                .withStyle(ChatFormatting.GOLD));
    }

    /** The first arrow he looses while holding the Spirit Bow is the spirit arrow; the bow goes with it. */
    private void watchForShot(ServerLevel level, ServerPlayer p) {
        if (!holdsBow(p)) {
            return;
        }
        AABB box = p.getBoundingBox().inflate(4);
        for (AbstractArrow a : level.getEntitiesOfClass(AbstractArrow.class, box)) {
            if (a.getOwner() == p && a.tickCount <= 3) {
                spiritArrow = a;
                a.pickup = AbstractArrow.Pickup.DISALLOWED;
                a.setGlowingTag(true);
                arrowGiven = false;
                takeBow(p);
                bow = Bow.SHOT;
                return;
            }
        }
    }

    private void thornHit(ServerLevel level) {
        if (spiritArrow != null) {
            spiritArrow.discard();
            spiritArrow = null;
        }
        lives--;
        hurtTicks = 30;
        if (thorn != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, thorn.getX(), thorn.getY() + 2, thorn.getZ(), 3, 1, 1, 1,
                    0);
            level.playSound(null, thorn.getX(), thorn.getY(), thorn.getZ(), SoundEvents.GHAST_HURT,
                    SoundSource.HOSTILE, 1.5f, 0.8f);
        }
        if (lives <= 0) {
            die(level);
            return;
        }
        SimBoss.say(level.getServer(), "Thorn", lives == 1 ? "No! My last spirit... you will not take it!"
                : "Ouch! My animals will avenge me!");
        // His answer: a wave of animals, and the next share of the ring to light.
        for (int i = 0; i < (master() ? 5 : 3); i++) {
            summon(level, SUMMONS[ThreadLocalRandom.current().nextInt(SUMMONS.length)]);
        }
        bow = Bow.GATHER;
        if (lit >= threshold()) {
            bow = Bow.BEAR_SOON;
            bowTimer = BEAR_DELAY_TICKS;
        }
    }

    /** A miss, a crumbled bow or a lost drop: the bear comes back on its own after a short wait. */
    private void lost(ServerLevel level, String why) {
        holder = null;
        if (why != null) {
            SimBoss.tell(level.getServer(), Component.literal(why + " A new Spirit Bear is coming.")
                    .withStyle(ChatFormatting.RED));
        }
        bow = Bow.BEAR_SOON;
        bowTimer = BEAR_AGAIN_TICKS;
    }

    private void die(ServerLevel level) {
        SimBoss.say(level.getServer(), "Thorn", "This is it... where I belong.");
        if (thorn != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, thorn.getX(), thorn.getY() + 2, thorn.getZ(), 6, 1.5, 1.5,
                    1.5, 0);
            level.playSound(null, thorn.getX(), thorn.getY(), thorn.getZ(), SoundEvents.GHAST_DEATH,
                    SoundSource.HOSTILE, 1.5f, 0.8f);
        }
        discard(level);
        phase = Phase.DEAD;
    }

    private ServerPlayer holderPlayer(ServerLevel level) {
        if (holder == null) {
            return null;
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(holder);
        return p != null && p.isAlive() && holdsAnywhere(p) ? p : null;
    }

    static ItemStack spiritBow() {
        ItemStack stack = new ItemStack(Items.BOW);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Spirit Bow").withStyle(ChatFormatting.GOLD));
        CompoundTag tag = new CompoundTag();
        tag.putString("id", SPIRIT_BOW_ID);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static boolean isBow(ItemStack stack) {
        return SPIRIT_BOW_ID.equals(SimItemIds.skyblockId(stack));
    }

    private static boolean holdsBow(ServerPlayer p) {
        return isBow(p.getMainHandItem()) || isBow(p.getOffhandItem());
    }

    private static boolean holdsAnywhere(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (isBow(p.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    /** Takes every Spirit Bow off him, and the arrow he was given if it was never shot. */
    private void takeBow(ServerPlayer p) {
        var inv = p.getInventory();
        boolean arrow = arrowGiven;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isBow(s)) {
                inv.setItem(i, ItemStack.EMPTY);
            } else if (arrow && s.is(Items.ARROW) && SimItemIds.skyblockId(s) == null) {
                s.shrink(1);
                arrow = false;
            }
        }
        arrowGiven = false;
    }

    // ------------------------------------------------------------------ shared

    private static ServerPlayer nearest(List<ServerPlayer> players, Vec3 from) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : players) {
            double d = p.position().distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private void nameTags() {
        if (thorn != null) {
            thorn.setCustomName(Component.literal("Thorn ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(Component.literal(lives + "/" + livesMax + " lives").withStyle(ChatFormatting.GREEN)));
        }
        for (Spirit s : spirits) {
            s.body.setCustomName(Component.literal(s.kind.display + " ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(SimBoss.shortHealth(s.health.hp)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("❤").withStyle(ChatFormatting.RED)));
        }
    }

    @Override
    boolean isBoss(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity == thorn) {
            return true;
        }
        for (Spirit s : spirits) {
            if (s.body == entity) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean hit(ServerLevel level, LivingEntity body, float amount) {
        if (phase != Phase.FIGHT) {
            return false;
        }
        if (body == thorn) {
            // The spirit arrow, caught as it touches him (it bounces off, since the vanilla hit is refused).
            if (bow == Bow.SHOT && spiritArrow != null && !spiritArrow.isRemoved()
                    && thorn.getBoundingBox().inflate(2.5).intersects(spiritArrow.getBoundingBox())) {
                thornHit(level);
                return true;
            }
            if (onlyBowNote == 0) {
                onlyBowNote = 40;
                for (ServerPlayer p : SimBoss.playersInRing(level)) {
                    p.sendSystemMessage(Component.literal("Only the Spirit Bow can hurt Thorn!")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
            return false;
        }
        for (Spirit s : new ArrayList<>(spirits)) {
            if (s.body == body) {
                boolean landed = s.health.hit(amount);
                if (landed && s.health.hp <= 0) {
                    killSpirit(level, s);
                }
                return landed;
            }
        }
        return false;
    }

    @Override
    boolean defeated() {
        return phase == Phase.DEAD;
    }

    @Override
    void discard(ServerLevel level) {
        if (thorn != null) {
            thorn.discard();
            thorn = null;
        }
        for (Spirit s : spirits) {
            s.body.discard();
        }
        spirits.clear();
        bear = null;
        if (drop != null) {
            drop.discard();
            drop = null;
        }
        if (spiritArrow != null) {
            spiritArrow.discard();
            spiritArrow = null;
        }
        ServerPlayer p = holder == null ? null : level.getServer().getPlayerList().getPlayer(holder);
        if (p != null) {
            takeBow(p);
        }
        holder = null;
        bolts.clear();
        roots.clear();
    }

    @Override
    Component barName() {
        String loop = switch (bow) {
            case GATHER -> "spirits " + lit + "/" + threshold();
            case BEAR_SOON, BEAR -> "Spirit Bear";
            case DROPPED -> "pick up the Spirit Bow";
            case HELD, SHOT -> "shoot Thorn";
        };
        return Component.literal("Thorn - " + lives + "/" + livesMax + " lives - " + loop)
                .withStyle(ChatFormatting.RED);
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return hurtTicks > 0 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.GREEN;
    }

    @Override
    float barProgress() {
        return (float) lives / livesMax;
    }
}
