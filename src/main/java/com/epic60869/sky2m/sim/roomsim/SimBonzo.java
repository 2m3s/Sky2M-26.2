package com.epic60869.sky2m.sim.roomsim;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 1's boss, Bonzo, in his circus ring.
 *
 * <p>Two health bars, the way the fight goes on Hypixel: he "dies", comes back, and has to be killed again.
 * The mechanics are the ones a run has to deal with, tuned for practice rather than copied number for number:
 * <ul>
 * <li><b>Balloon volleys.</b> Every few seconds he marks the ground under each player with a red ring and the
 * spot blows up a second and a half later - move out of it. In his second life the volleys come faster
 * and also land on extra spots around you.</li>
 * <li><b>Undead.</b> He calls zombie adds into the ring now and then, which chase and hit. Kill them or kite
 * them while you burst Bonzo.</li>
 * <li><b>The revive.</b> At 0 health in his first life he is untouchable for three seconds, then returns with a
 * fresh bar.</li>
 * <li><b>Pounce</b> (second life). Stand next to him for a second and he knocks everyone near him away.</li>
 * </ul>
 *
 * <p><b>Health and damage are Hypixel's</b> (https://hypixelskyblock.minecraft.wiki/w/Bonzo): each life 250,000 on
 * F1 and 200,000,000 on M1 (the second life is the first healed back to full), a balloon 1,598 / 22,500, his own
 * hit (the pounce) his base damage of 2,664 / 45,000, and the Undead 45,000 / 15,000,000 health with a 1,440 /
 * 36,000 hit ({@link SimHealth#BONZO_UNDEAD}); all before the player's Defense ({@link SimVitals#dealHypixel}).
 *
 * <p><b>Master Mode (M1)</b> is the same fight turned up in its pace (the sim's own tuning, not Hypixel's): the
 * balloons give less warning, come more often and land on extra spots from the first life on, the Undead come in
 * bigger groups, the second life pounces sooner, and every so often rains a ring of balloons round the middle of
 * the ring that has to be walked out of.
 *
 * <p>The opening line and the death line are Hypixel's exact words, which is what starts and ends the Boss
 * Phases splits; the lines in between are the sim's own.
 */
final class SimBonzo extends SimBoss.Fight {

    /**
     * A life's health: 250,000 on F1, 200,000,000 on M1 (https://hypixelskyblock.minecraft.wiki/w/Bonzo). The second
     * life is the same bar again - Hypixel's Bonzo "heals himself back to his max health". /boss bonzo from another
     * floor uses the number for that floor's mode.
     */
    private static double lifeHealth() {
        return SimHealth.byMode(250_000, 200_000_000);
    }

    /** A balloon's blast: 1,598 on F1, 22,500 on M1 (the Bonzo page's Attacks section). */
    private static double balloonDamage() {
        return SimHealth.byMode(1_598, 22_500);
    }

    /** His base damage, which the pounce deals: 2,664 on F1, 45,000 on M1. */
    private static double baseDamage() {
        return SimHealth.byMode(2_664, 45_000);
    }
    private static final int REVIVE_TICKS = 60;
    private static final double BALLOON_RADIUS = 3.0;

    private enum Phase { INTRO, LIFE_ONE, REVIVING, LIFE_TWO, DEAD }

    /** Fixed when the fight is made: the floor does not change mid-fight. */
    private final boolean master = SimState.floorLabel().toUpperCase(java.util.Locale.ROOT).startsWith("M");
    /** A balloon's warning before it blows: a second and a half, a little over one on Master Mode. */
    private final int telegraphTicks = master ? 22 : 30;
    private final int maxUndead = master ? 9 : 6;
    private Phase phase = Phase.INTRO;
    private int phaseTicks;
    private final SimBoss.Health health = new SimBoss.Health(lifeHealth());
    private int nextRain;
    private Zombie body;
    private final List<UUID> undead = new ArrayList<>();
    private final List<Balloon> balloons = new ArrayList<>();
    private int nextVolley;
    private int nextUndead;
    private int closeTicks;
    private int pounceCooldown;
    /** Where he is circling, in radians round the ring. */
    private double orbit;

    private record Balloon(Vec3 at, int explodeAt, double damage) {
    }

    SimBonzo(SimBossArena arena) {
        super(arena);
    }

    @Override
    String name() {
        return "Bonzo";
    }

    @Override
    void start(ServerLevel level) {
        // Hypixel's first line, word for word: Boss Phases opens "Bonzo 1" on it.
        SimBoss.say(level.getServer(), "Bonzo", "Gratz for making it this far, but I'm basically unbeatable.");
        phase = Phase.INTRO;
        phaseTicks = 0;
    }

    @Override
    void tick(ServerLevel level, int t) {
        phaseTicks++;
        switch (phase) {
            case INTRO -> {
                if (phaseTicks == 40) {
                    spawnBody(level);
                    SimBoss.say(level.getServer(), "Bonzo", "Let's see how long you last in my ring!");
                    toPhase(Phase.LIFE_ONE);
                    nextVolley = 40;
                    nextUndead = 100;
                }
            }
            case LIFE_ONE, LIFE_TWO -> {
                if (body == null || body.isRemoved()) {
                    spawnBody(level);
                }
                boolean second = phase == Phase.LIFE_TWO;
                move(level, second);
                if (--nextVolley <= 0) {
                    volley(level, second);
                    nextVolley = master ? (second ? 35 : 60) : (second ? 50 : 80);
                }
                if (--nextUndead <= 0) {
                    summonUndead(level, (second ? 3 : 2) + (master ? 1 : 0));
                    nextUndead = master ? (second ? 200 : 240) : (second ? 240 : 300);
                }
                if (second) {
                    pounce(level);
                    if (master && --nextRain <= 0) {
                        balloonRain(level);
                        nextRain = 200;
                    }
                }
                if (health.hp <= 0) {
                    if (second) {
                        die(level);
                    } else {
                        revive(level);
                    }
                }
            }
            case REVIVING -> {
                if (body != null) {
                    level.sendParticles(ParticleTypes.WITCH, body.getX(), body.getY() + 1, body.getZ(), 8,
                            0.4, 0.8, 0.4, 0.05);
                }
                if (phaseTicks >= REVIVE_TICKS) {
                    health.max = lifeHealth();
                    health.hp = health.max;
                    health.invulnerable = false;
                    if (body != null) {
                        body.setGlowingTag(false);
                    }
                    SimBoss.say(level.getServer(), "Bonzo", "Round two! Did you really think that was it?");
                    toPhase(Phase.LIFE_TWO);
                    nextVolley = 20;
                    nextUndead = 60;
                    nextRain = 120;
                }
            }
            default -> {
            }
        }
        tickBalloons(level);
        nameTag();
    }

    private void toPhase(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    private void revive(ServerLevel level) {
        SimBoss.say(level.getServer(), "Bonzo", "Oh no, you got me... or did you?");
        health.invulnerable = true;
        if (body != null) {
            body.setGlowingTag(true);
            level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.WITHER_SPAWN,
                    SoundSource.HOSTILE, 0.6f, 1.6f);
        }
        balloons.clear();
        toPhase(Phase.REVIVING);
    }

    private void die(ServerLevel level) {
        // Hypixel's last line, word for word: the splits and the score read it as the boss being down.
        SimBoss.say(level.getServer(), "Bonzo", "Oh I'm dead!");
        if (body != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, body.getX(), body.getY() + 1, body.getZ(), 3,
                    0.5, 0.5, 0.5, 0);
            body.discard();
            body = null;
        }
        discardUndead(level);
        balloons.clear();
        toPhase(Phase.DEAD);
    }

    private void spawnBody(ServerLevel level) {
        BlockPos at = arena.toWorld(arena.bossSpawn);
        Zombie z = new BossBody(level);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        z.setNoAi(true);
        z.setNoGravity(true);
        z.setPersistenceRequired();
        z.setCustomNameVisible(true);
        z.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(z);
        body = z;
        nameTag();
    }

    /** Circles the middle of the ring, facing the nearest player. Faster in his second life. */
    private void move(ServerLevel level, boolean second) {
        if (body == null) {
            return;
        }
        BlockPos c = arena.toWorld(arena.centre);
        orbit += second ? 0.035 : 0.02;
        double r = 7;
        double x = c.getX() + 0.5 + Math.cos(orbit) * r;
        double z = c.getZ() + 0.5 + Math.sin(orbit) * r;
        ServerPlayer target = nearest(level, body.position());
        float yaw = body.getYRot();
        if (target != null) {
            yaw = (float) (Math.toDegrees(Math.atan2(target.getZ() - z, target.getX() - x)) - 90);
        }
        body.snapTo(x, c.getY(), z, yaw, 0f);
        body.setYHeadRot(yaw);
        body.setYBodyRot(yaw);
    }

    /**
     * Marks a spot under each player (and round them: two more in his second life, and on Master Mode one more in
     * the first and four in the second) to blow up.
     */
    private void volley(ServerLevel level, boolean second) {
        List<ServerPlayer> targets = SimBoss.playersInRing(level);
        if (targets.isEmpty()) {
            return;
        }
        int now = phaseTicks;
        double damage = balloonDamage();
        int extra = master ? (second ? 4 : 1) : (second ? 2 : 0);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (ServerPlayer p : targets) {
            balloons.add(new Balloon(new Vec3(p.getX(), p.getY(), p.getZ()), now + telegraphTicks, damage));
            for (int i = 0; i < extra; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double d = 3 + rnd.nextDouble() * 3;
                balloons.add(new Balloon(new Vec3(p.getX() + Math.cos(a) * d, p.getY(),
                        p.getZ() + Math.sin(a) * d), now + telegraphTicks + 5 * (i + 1), damage));
            }
        }
        if (body != null) {
            body.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.HOSTILE, 1f, 0.8f);
        }
    }

    /**
     * Master Mode, second life: two rings of balloons round the middle of the ring, the inner and outer offset so
     * the safe spots are the gaps between them. A little longer warning than a volley - it covers much more ground.
     */
    private void balloonRain(ServerLevel level) {
        BlockPos c = arena.toWorld(arena.centre);
        double off = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        int at = phaseTicks + telegraphTicks + 10;
        for (int ring = 0; ring < 2; ring++) {
            double r = ring == 0 ? 5 : 11;
            int n = ring == 0 ? 6 : 10;
            for (int i = 0; i < n; i++) {
                double a = off + Math.PI * 2 * (i + ring * 0.5) / n;
                balloons.add(new Balloon(new Vec3(c.getX() + 0.5 + Math.cos(a) * r, c.getY(),
                        c.getZ() + 0.5 + Math.sin(a) * r), at, balloonDamage()));
            }
        }
        SimBoss.say(level.getServer(), "Bonzo", "Balloons for everyone!");
        if (body != null) {
            level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                    SoundSource.HOSTILE, 1f, 0.7f);
        }
    }

    private void tickBalloons(ServerLevel level) {
        if (balloons.isEmpty()) {
            return;
        }
        DustParticleOptions red = new DustParticleOptions(0xFF3030, 1.2f);
        List<Balloon> done = new ArrayList<>();
        for (Balloon b : balloons) {
            int left = b.explodeAt() - phaseTicks;
            if (left > 0) {
                if (phaseTicks % 3 == 0) {
                    // The warning ring on the ground, the thing to step out of.
                    for (int i = 0; i < 16; i++) {
                        double a = Math.PI * 2 * i / 16;
                        level.sendParticles(red, b.at().x + Math.cos(a) * BALLOON_RADIUS, b.at().y + 0.1,
                                b.at().z + Math.sin(a) * BALLOON_RADIUS, 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            explode(level, b);
            done.add(b);
        }
        balloons.removeAll(done);
    }

    private void explode(ServerLevel level, Balloon b) {
        Vec3 at = b.at();
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 2, 0.5, 0.3, 0.5, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1f, 1.2f);
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            double dx = p.getX() - at.x;
            double dz = p.getZ() - at.z;
            if (dx * dx + dz * dz > BALLOON_RADIUS * BALLOON_RADIUS || Math.abs(p.getY() - at.y) > 3) {
                continue;
            }
            hurtPlayer(level, p, b.damage(), dx, dz, 0.7, 0.6);
        }
    }

    /** Second life: stay next to him for a second and he throws everyone near him back. */
    private void pounce(ServerLevel level) {
        if (body == null) {
            return;
        }
        if (pounceCooldown > 0) {
            pounceCooldown--;
            return;
        }
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            if (p.distanceToSqr(body) < 3.5 * 3.5) {
                near.add(p);
            }
        }
        closeTicks = near.isEmpty() ? 0 : closeTicks + 1;
        if (closeTicks < (master ? 12 : 20)) {
            return;
        }
        closeTicks = 0;
        pounceCooldown = master ? 70 : 100;
        level.sendParticles(ParticleTypes.CLOUD, body.getX(), body.getY() + 0.2, body.getZ(), 30, 1.5, 0.1, 1.5,
                0.05);
        level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 0.7f, 0.7f);
        for (ServerPlayer p : near) {
            hurtPlayer(level, p, baseDamage(), p.getX() - body.getX(), p.getZ() - body.getZ(), 1.6, 0.7);
        }
    }

    /**
     * Damage plus knockback away from a point: {@code damage} is Hypixel damage before the player's Defense
     * ({@link SimVitals#dealHypixel}).
     */
    private static void hurtPlayer(ServerLevel level, ServerPlayer p, double damage, double dx, double dz,
                                   double push, double lift) {
        DamageSource src = level.damageSources().magic();
        SimVitals.dealHypixel(level, p, src, damage, false);
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) {
            double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            dx = Math.cos(a);
            dz = Math.sin(a);
            len = 1;
        }
        p.setDeltaMovement(dx / len * push, lift, dz / len * push);
        p.hurtMarked = true;
    }

    private void summonUndead(ServerLevel level, int count) {
        undead.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        BlockPos c = arena.toWorld(arena.centre);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // Hypixel's Undead: registered as "Undead", the pool takes the wiki's health and hit for F1/M1
        // (SimHealth.BONZO_UNDEAD) whatever is asked for here.
        String floor = SimState.floorLabel();
        SimHealth.Role role = master ? SimHealth.Role.STARRED : SimHealth.Role.REGULAR;
        for (int i = 0; i < count && undead.size() < maxUndead; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = 8 + rnd.nextDouble() * (arena.radius - 12);
            Undead u = new Undead(level);
            u.setPersistenceRequired();
            u.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(SimHealth.meleeDamage(floor, role));
            if (master) {
                u.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
            }
            u.snapTo(c.getX() + 0.5 + Math.cos(a) * d, c.getY(), c.getZ() + 0.5 + Math.sin(a) * d,
                    rnd.nextFloat() * 360, 0f);
            ServerPlayer target = nearest(level, u.position());
            if (target != null) {
                u.setTarget(target);
            }
            if (!level.addFreshEntity(u)) {
                continue;
            }
            // The pool writes "Undead 3k❤" into the zombie's own name.
            SimHealth.register(u, SimHealth.maxHealth(floor, role), "Undead", false, null);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, u.getX(), u.getY() + 1, u.getZ(), 10, 0.3, 0.5, 0.3,
                    0.02);
            undead.add(u.getUUID());
        }
    }

    private void discardUndead(ServerLevel level) {
        for (UUID id : undead) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        undead.clear();
    }

    private static ServerPlayer nearest(ServerLevel level, Vec3 from) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            double d = p.position().distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private void nameTag() {
        if (body == null) {
            return;
        }
        Component tag = Component.literal("Bonzo ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(health.invulnerable ? "???" : SimBoss.shortHealth(health.hp))
                        .withStyle(ChatFormatting.GREEN).append(Component.literal("❤")
                                .withStyle(ChatFormatting.RED)));
        body.setCustomName(tag);
    }

    @Override
    boolean isBoss(Entity entity) {
        return entity == body;
    }

    @Override
    boolean hit(ServerLevel level, LivingEntity hitBody, float amount) {
        return (phase == Phase.LIFE_ONE || phase == Phase.LIFE_TWO) && health.hit(amount);
    }

    @Override
    boolean defeated() {
        return phase == Phase.DEAD;
    }

    @Override
    void discard(ServerLevel level) {
        if (body != null) {
            body.discard();
            body = null;
        }
        discardUndead(level);
        balloons.clear();
    }

    @Override
    Component barName() {
        String life = phase == Phase.LIFE_TWO || phase == Phase.REVIVING ? " (2nd life)" : "";
        return Component.literal("Bonzo" + life).withStyle(ChatFormatting.RED);
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return phase == Phase.LIFE_TWO || phase == Phase.REVIVING
                ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED;
    }

    @Override
    float barProgress() {
        return phase == Phase.INTRO ? 1f : phase == Phase.REVIVING
                ? Math.min(1f, (float) phaseTicks / REVIVE_TICKS) : health.fraction();
    }

    /** Bonzo's body: no AI (the fight moves him), never despawns, never burns in the sim's daylight. */
    private static final class BossBody extends Zombie {
        BossBody(Level level) {
            super(level);
        }

        @Override
        public void checkDespawn() {
        }

        @Override
        protected boolean isSunSensitive() {
            return false;
        }

        @Override
        protected boolean convertsInWater() {
            return false;
        }

        @Override
        public void igniteForTicks(int ticks) {
        }

        @Override
        public boolean fireImmune() {
            return true;
        }
    }

    /**
     * Bonzo's adds: ordinary chasing zombies that never despawn and never burn, with a {@link SimHealth} pool.
     * Daylight burning comes from an entity tag rather than {@code isSunSensitive} in 26.2, so the fire itself is
     * refused too, the way the room mobs refuse it.
     */
    private static final class Undead extends Zombie {
        Undead(Level level) {
            super(level);
        }

        @Override
        public void checkDespawn() {
        }

        @Override
        protected boolean isSunSensitive() {
            return false;
        }

        @Override
        protected boolean convertsInWater() {
            return false;
        }

        @Override
        public void igniteForTicks(int ticks) {
        }

        @Override
        public boolean fireImmune() {
            return true;
        }
    }
}
