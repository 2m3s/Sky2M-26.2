package com.epic60869.sky2m.sim.roomsim;

import com.epic60869.sky2m.sim.compat.McEntities;
import com.epic60869.sky2m.sim.util.ModChat;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 2's boss, Scarf, the Apprentice Necromancer.
 *
 * <p>The fight goes the way it does on Hypixel, in two parts:
 * <ol>
 * <li><b>His Undead.</b> Scarf hangs back over the far end of the arena, untouchable, while four guardians fight
 * for him:
 * <ul>
 * <li><b>Undead Warrior</b> - the melee one: quick, hard-hitting, the most health.</li>
 * <li><b>Undead Priest</b> - heals the other guardians every few seconds and brings a fallen one back a while
 * after it dies. Kill him first, or kill the others and then race the revive. He blows up when he dies, so do
 * it from range or step away.</li>
 * <li><b>Undead Mage</b> - stands off and throws fire bolts at the nearest player.</li>
 * <li><b>Undead Archer</b> - a skeleton with a bow.</li>
 * </ul>
 * </li>
 * <li><b>Scarf himself.</b> With the four down he drops into the arena and fights: he teleports next to a player
 * every few seconds, throws fire bolts, and slams the ground round himself (a ring telegraph to step out of). At
 * half health he calls his Undead back once - the Warrior and the Archer on a Normal floor, all four on Master
 * Mode.</li>
 * </ol>
 *
 * <p><b>Health and damage are Hypixel's</b> (https://hypixelskyblock.minecraft.wiki/w/Scarf): Scarf 1,000,000 on F2
 * and 375,000,000 on M2 with a base damage of 3,456 / 81,000; his Undead with their own pages' numbers
 * ({@link SimHealth#UNDEAD_WARRIOR} and the rest), the Mage's bolts 1,094 / 29,640 (its Wither Skulls). The wiki has no
 * number for Scarf's own bolts and slam or the Priest's death blast, so those deal the caster's base damage
 * (APPROXIMATE). Every hit is before the player's Defense ({@link SimVitals#dealHypixel}).
 *
 * <p><b>Master Mode (M2)</b> is the same fight with its pace turned up (the sim's own tuning): a faster heal and
 * revive from the Priest, a
 * Mage that throws three bolts at a time, Scarf throwing bolts at you even while he hangs back, and once he is
 * down, more teleports, triple bolts, bigger and quicker slams, and the half-health wave makes him untouchable
 * again until it is dead.
 *
 * <p>The opening line and the death line are Hypixel's exact words: Boss Phases, the splits and the boss-dead
 * clutter all read them ({@code BossPhases}, {@code DungeonFeatures}, {@code DungeonClutter}). The lines between
 * them are nothing the mod parses.
 */
final class SimScarf extends SimBoss.Fight {

    /** Scarf's health: 1,000,000 on F2, 375,000,000 on M2 (https://hypixelskyblock.minecraft.wiki/w/Scarf). */
    private static double bossHealth() {
        return SimHealth.byMode(1_000_000, 375_000_000);
    }

    /** Scarf's base damage, 3,456 / 81,000, which his bolts and slam deal (the wiki gives them no own number). */
    private static double scarfDamage() {
        return SimHealth.byMode(3_456, 81_000);
    }

    /** The Undead Mage's Wither Skulls: 1,094 on F2, 29,640 on M2. */
    private static double mageBolt() {
        return SimHealth.byMode(1_094, 29_640);
    }

    /**
     * His four guardians. Their health and hit are Hypixel's, looked up by name when their pools are registered
     * ({@link SimHealth#statsFor}); the factors here only size the vanilla attack the mob carries.
     */
    private enum Guardian {
        WARRIOR("Undead Warrior", 1.2, 1.4, 0, -3),
        PRIEST("Undead Priest", 0.8, 0.8, 0, -8),
        MAGE("Undead Mage", 1.0, 0.8, -6, -6),
        ARCHER("Undead Archer", 0.9, 1.0, 6, -6);

        final String display;
        final double healthFactor;
        final float damageFactor;
        /** Where it rises, relative to the arena's centre in source coordinates. */
        final int dx;
        final int dz;

        Guardian(String display, double healthFactor, double damageFactor, int dx, int dz) {
            this.display = display;
            this.healthFactor = healthFactor;
            this.damageFactor = (float) damageFactor;
            this.dx = dx;
            this.dz = dz;
        }
    }

    private enum Phase { INTRO, MINIONS, DESCEND, FIGHT, DEAD }

    private static final int INTRO_TICKS = 50;
    private static final int DESCEND_TICKS = 40;
    private static final double BOLT_SPEED = 0.7;
    private static final int BOLT_LIFE = 70;
    private static final double BOLT_HIT_RADIUS = 1.1;
    /** How high over his spawn point he hangs while his Undead fight. */
    private static final double PERCH_HEIGHT = 4;

    private final boolean master = SimState.floorLabel().toUpperCase(Locale.ROOT).startsWith("M");
    private Phase phase = Phase.INTRO;
    private int phaseTicks;
    /** Ticks since the fight started; the bolts and telegraphs count on this, it never resets. */
    private int now;
    private final SimBoss.Health health = new SimBoss.Health(bossHealth());
    private Zombie body;

    private final Map<Guardian, UUID> guardians = new EnumMap<>(Guardian.class);
    /** A fallen guardian's last spot and the tick the Priest brings it back, while the Priest lives. */
    private final Map<Guardian, Vec3> fallenAt = new EnumMap<>(Guardian.class);
    private final Map<Guardian, Integer> reviveAt = new EnumMap<>(Guardian.class);
    /** Health of the wave that is up, for the bar while they fight. */
    private double waveMaxHealth;
    private boolean secondWave;
    private int nextHeal;

    private final List<Bolt> bolts = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();
    private int nextBolt;
    private int nextTeleport;
    private int nextSlam;
    private int lastImmuneNote = -1000;

    /** A fire bolt in flight: drawn with particles, hurts the first player it touches. */
    private static final class Bolt {
        Vec3 pos;
        final Vec3 vel;
        int life;
        final double damage;

        Bolt(Vec3 pos, Vec3 vel, double damage) {
            this.pos = pos;
            this.vel = vel;
            this.life = BOLT_LIFE;
            this.damage = damage;
        }
    }

    /** A ring on the ground that blows up at {@code explodeAt} (the slam, the Priest's death). */
    private record Blast(Vec3 at, double radius, int explodeAt, double damage) {
    }

    SimScarf(SimBossArena arena) {
        super(arena);
    }

    @Override
    String name() {
        return "Scarf";
    }

    @Override
    void start(ServerLevel level) {
        // Hypixel's first line, word for word: Boss Phases opens "Minions" on it, the splits "Scarf's minions".
        SimBoss.say(level.getServer(), "Scarf", "This is where the journey ends for you, Adventurers.");
        health.invulnerable = true;
        spawnBody(level);
        toPhase(Phase.INTRO);
    }

    @Override
    void tick(ServerLevel level, int t) {
        now = t;
        phaseTicks++;
        if (body == null || body.isRemoved()) {
            if (phase != Phase.DEAD) {
                spawnBody(level);
            }
        }
        switch (phase) {
            case INTRO -> {
                perch(level);
                if (phaseTicks >= INTRO_TICKS) {
                    SimBoss.say(level.getServer(), "Scarf", "ARISE, MY CREATIONS!");
                    for (Guardian g : Guardian.values()) {
                        raise(level, g, spawnSpot(g));
                    }
                    waveMaxHealth = waveHealthNow(level);
                    nextHeal = healInterval();
                    nextBolt = 100;
                    toPhase(Phase.MINIONS);
                }
            }
            case MINIONS -> {
                perch(level);
                tickGuardians(level);
                // Master Mode: he does not just watch.
                if (master && --nextBolt <= 0) {
                    ServerPlayer target = randomPlayer(level);
                    if (target != null && body != null) {
                        throwBolts(level, body.position().add(0, 1.6, 0), target, 1, scarfDamage());
                    }
                    nextBolt = 100;
                }
                if (allDown()) {
                    SimBoss.say(level.getServer(), "Scarf", "Those toys are not strong enough I see.");
                    toPhase(Phase.DESCEND);
                }
            }
            case DESCEND -> {
                if (body != null) {
                    // Floats down from his perch onto the arena floor.
                    BlockPos spawn = arena.toWorld(arena.bossSpawn);
                    double y = spawn.getY() + PERCH_HEIGHT * Math.max(0, 1 - (double) phaseTicks / DESCEND_TICKS);
                    body.snapTo(spawn.getX() + 0.5, y, spawn.getZ() + 0.5, body.getYRot(), 0f);
                    level.sendParticles(ParticleTypes.SOUL, body.getX(), body.getY() + 1, body.getZ(), 2,
                            0.3, 0.5, 0.3, 0.01);
                }
                if (phaseTicks >= DESCEND_TICKS) {
                    SimBoss.say(level.getServer(), "Scarf", "Don't get too excited though.");
                    health.invulnerable = false;
                    if (body != null) {
                        body.setGlowingTag(false);
                    }
                    nextBolt = 30;
                    nextTeleport = 60;
                    nextSlam = 120;
                    toPhase(Phase.FIGHT);
                }
            }
            case FIGHT -> fight(level);
            default -> {
            }
        }
        tickBolts(level);
        tickBlasts(level);
        nameTag();
    }

    private void toPhase(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    // ------------------------------------------------------------------ Scarf

    private void spawnBody(ServerLevel level) {
        BlockPos at = arena.toWorld(arena.bossSpawn);
        Zombie z = new BossBody(level);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        z.setNoAi(true);
        z.setNoGravity(true);
        z.setPersistenceRequired();
        z.setCustomNameVisible(true);
        z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
        double y = phase == Phase.FIGHT ? at.getY() : at.getY() + PERCH_HEIGHT;
        z.snapTo(at.getX() + 0.5, y, at.getZ() + 0.5, 0f, 0f);
        z.setGlowingTag(health.invulnerable);
        level.addFreshEntity(z);
        body = z;
        nameTag();
    }

    /** Hangs over his spawn point facing the nearest player, glowing while he cannot be hurt. */
    private void perch(ServerLevel level) {
        if (body == null) {
            return;
        }
        BlockPos at = arena.toWorld(arena.bossSpawn);
        double x = at.getX() + 0.5;
        double y = at.getY() + PERCH_HEIGHT + Math.sin(now * 0.08) * 0.3;
        double z = at.getZ() + 0.5;
        float yaw = face(level, x, z, body.getYRot());
        body.snapTo(x, y, z, yaw, 0f);
        body.setYHeadRot(yaw);
        body.setYBodyRot(yaw);
        body.setGlowingTag(true);
        if (now % 10 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.2, z, 4, 0.4, 0.1, 0.4, 0.01);
        }
    }

    /** His own part of the fight: teleports, bolts, slams, and the half-health wave. */
    private void fight(ServerLevel level) {
        if (body == null) {
            return;
        }
        tickGuardians(level);
        // Master Mode's wave shields him; a Normal floor's only gets in the way.
        boolean shielded = master && secondWave && !allDown();
        health.invulnerable = shielded;
        body.setGlowingTag(shielded);
        float yaw = face(level, body.getX(), body.getZ(), body.getYRot());
        body.setYRot(yaw);
        body.setYHeadRot(yaw);
        body.setYBodyRot(yaw);

        if (--nextTeleport <= 0) {
            teleport(level);
            nextTeleport = master ? 60 : 100;
        }
        if (--nextBolt <= 0) {
            ServerPlayer target = nearest(level, body.position());
            if (target != null) {
                body.swing(InteractionHand.MAIN_HAND);
                throwBolts(level, body.position().add(0, 1.6, 0), target, master ? 3 : 1, scarfDamage());
            }
            nextBolt = master ? 25 : 40;
        }
        if (--nextSlam <= 0) {
            slam(level);
            nextSlam = master ? 110 : 160;
        }
        if (!secondWave && health.hp <= health.max / 2) {
            secondWave = true;
            SimBoss.say(level.getServer(), "Scarf", "ARISE, MY CREATIONS!");
            List<Guardian> wave = master ? List.of(Guardian.values()) : List.of(Guardian.WARRIOR, Guardian.ARCHER);
            for (Guardian g : wave) {
                raise(level, g, spawnSpotNear(body.position(), g));
            }
            waveMaxHealth = waveHealthNow(level);
            nextHeal = healInterval();
        }
        if (health.hp <= 0) {
            die(level);
        }
    }

    private void die(ServerLevel level) {
        // Hypixel's last line, word for word: the splits and the boss-dead clutter read it as Scarf being down.
        SimBoss.say(level.getServer(), "Scarf", "Did you forget? I was taught by the best! Let's dance.");
        if (body != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, body.getX(), body.getY() + 1, body.getZ(), 3,
                    0.5, 0.5, 0.5, 0);
            body.discard();
            body = null;
        }
        discardGuardians(level);
        bolts.clear();
        blasts.clear();
        toPhase(Phase.DEAD);
    }

    /** Blinks to a spot a few blocks from a random player, inside the arena and on open floor. */
    private void teleport(ServerLevel level) {
        ServerPlayer target = randomPlayer(level);
        if (target == null || body == null) {
            return;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        BlockPos c = arena.toWorld(arena.centre);
        BlockPos lo = arena.toWorld(-30, 70, -29);
        BlockPos hi = arena.toWorld(14, 70, 11);
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = 4 + rnd.nextDouble() * 3;
            double x = target.getX() + Math.cos(a) * d;
            double z = target.getZ() + Math.sin(a) * d;
            if (x < Math.min(lo.getX(), hi.getX()) || x > Math.max(lo.getX(), hi.getX()) + 1
                    || z < Math.min(lo.getZ(), hi.getZ()) || z > Math.max(lo.getZ(), hi.getZ()) + 1) {
                continue;
            }
            BlockPos feet = BlockPos.containing(x, c.getY(), z);
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()
                    || level.getBlockState(feet.below()).isAir()) {
                continue;
            }
            level.sendParticles(ParticleTypes.PORTAL, body.getX(), body.getY() + 1, body.getZ(), 30, 0.4, 0.8, 0.4,
                    0.3);
            body.snapTo(x, c.getY(), z, body.getYRot(), 0f);
            level.sendParticles(ParticleTypes.PORTAL, x, c.getY() + 1, z, 30, 0.4, 0.8, 0.4, 0.3);
            level.playSound(null, x, c.getY(), z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.8f);
            return;
        }
    }

    /** A ring round where he stands that blows up a moment later: step out of it. */
    private void slam(ServerLevel level) {
        if (body == null) {
            return;
        }
        double r = master ? 5.5 : 4.5;
        int warn = master ? 18 : 25;
        blasts.add(new Blast(body.position(), r, now + warn, scarfDamage()));
        body.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE,
                1f, 0.6f);
    }

    /** Fire bolts from {@code from} at a player: one straight at him, or three in a narrow fan. */
    private void throwBolts(ServerLevel level, Vec3 from, ServerPlayer target, int count, double damage) {
        Vec3 aim = target.position().add(0, 1.0, 0).subtract(from);
        if (aim.lengthSqr() < 0.01) {
            return;
        }
        aim = aim.normalize();
        for (int i = 0; i < count; i++) {
            double spread = count == 1 ? 0 : Math.toRadians(-15 + 30.0 * i / (count - 1));
            double cos = Math.cos(spread);
            double sin = Math.sin(spread);
            Vec3 dir = new Vec3(aim.x * cos - aim.z * sin, aim.y, aim.x * sin + aim.z * cos).normalize();
            bolts.add(new Bolt(from, dir.scale(BOLT_SPEED), damage));
        }
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8f, 1.2f);
    }

    private void tickBolts(ServerLevel level) {
        if (bolts.isEmpty()) {
            return;
        }
        List<ServerPlayer> players = SimBoss.playersInRing(level);
        List<Bolt> done = new ArrayList<>();
        for (Bolt b : bolts) {
            boolean gone = --b.life <= 0;
            // Two half steps a tick, so a bolt cannot skip over a player or through a thin wall.
            for (int step = 0; step < 2 && !gone; step++) {
                b.pos = b.pos.add(b.vel.scale(0.5));
                BlockPos bp = BlockPos.containing(b.pos);
                if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                    level.sendParticles(ParticleTypes.SMOKE, b.pos.x, b.pos.y, b.pos.z, 6, 0.1, 0.1, 0.1, 0.02);
                    gone = true;
                    break;
                }
                for (ServerPlayer p : players) {
                    if (p.position().add(0, 1.0, 0).distanceToSqr(b.pos) <= BOLT_HIT_RADIUS * BOLT_HIT_RADIUS) {
                        hurtPlayer(level, p, b.damage, b.vel.x, b.vel.z, 0.5, 0.3);
                        level.sendParticles(ParticleTypes.LAVA, b.pos.x, b.pos.y, b.pos.z, 4, 0.2, 0.2, 0.2, 0);
                        gone = true;
                        break;
                    }
                }
            }
            if (gone) {
                done.add(b);
                continue;
            }
            level.sendParticles(ParticleTypes.FLAME, b.pos.x, b.pos.y, b.pos.z, 2, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.SMOKE, b.pos.x, b.pos.y, b.pos.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        bolts.removeAll(done);
    }

    private void tickBlasts(ServerLevel level) {
        if (blasts.isEmpty()) {
            return;
        }
        DustParticleOptions orange = new DustParticleOptions(0xFF8020, 1.2f);
        List<Blast> done = new ArrayList<>();
        for (Blast b : blasts) {
            if (b.explodeAt() > now) {
                if (now % 3 == 0) {
                    int n = (int) Math.max(16, b.radius() * 5);
                    for (int i = 0; i < n; i++) {
                        double a = Math.PI * 2 * i / n;
                        level.sendParticles(orange, b.at().x + Math.cos(a) * b.radius(), b.at().y + 0.1,
                                b.at().z + Math.sin(a) * b.radius(), 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            Vec3 at = b.at();
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, b.radius() / 3, 0.3,
                    b.radius() / 3, 0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1f, 1f);
            for (ServerPlayer p : SimBoss.playersInRing(level)) {
                double dx = p.getX() - at.x;
                double dz = p.getZ() - at.z;
                if (dx * dx + dz * dz > b.radius() * b.radius() || Math.abs(p.getY() - at.y) > 3) {
                    continue;
                }
                hurtPlayer(level, p, b.damage(), dx, dz, 1.2, 0.6);
            }
            done.add(b);
        }
        blasts.removeAll(done);
    }

    // ------------------------------------------------------------------ the Undead

    private BlockPos spawnSpot(Guardian g) {
        BlockPos c = arena.toWorld(arena.centre);
        return c.offset(g.dx, 0, g.dz);
    }

    /** The half-health wave rises round Scarf, wherever he has got to. */
    private BlockPos spawnSpotNear(Vec3 at, Guardian g) {
        BlockPos c = arena.toWorld(arena.centre);
        double a = Math.PI / 2 * g.ordinal() + ThreadLocalRandom.current().nextDouble() * 0.5;
        return BlockPos.containing(at.x + Math.cos(a) * 3, c.getY(), at.z + Math.sin(a) * 3);
    }

    /** Raises one guardian with its pool and its kit. */
    private void raise(ServerLevel level, Guardian g, BlockPos at) {
        String floor = SimState.floorLabel();
        Mob mob = switch (g) {
            case ARCHER -> {
                Archer s = new Archer(McEntities.SKELETON, level);
                s.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                yield s;
            }
            case WARRIOR -> {
                UndeadZombie z = new UndeadZombie(level);
                z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(master ? 0.32 : 0.29);
                yield z;
            }
            case PRIEST -> {
                UndeadZombie z = new UndeadZombie(level);
                z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_HOE));
                z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
                yield z;
            }
            case MAGE -> {
                UndeadZombie z = new UndeadZombie(level);
                z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BLAZE_ROD));
                z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.18);
                yield z;
            }
        };
        mob.setPersistenceRequired();
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(SimHealth.meleeDamage(floor, SimHealth.Role.STARRED) * g.damageFactor);
        }
        var range = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) {
            range.setBaseValue(32);
        }
        var reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) {
            reinforcements.setBaseValue(0);
        }
        if (mob instanceof Archer s) {
            s.reassessWeaponGoal();
        }
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
        ServerPlayer target = nearest(level, mob.position());
        if (target != null) {
            mob.setTarget(target);
        }
        if (!level.addFreshEntity(mob)) {
            return;
        }
        // Registered by its Hypixel name, the pool takes the wiki's health and hit (SimHealth.statsFor).
        SimHealth.register(mob, SimHealth.maxHealth(floor, SimHealth.Role.STARRED) * g.healthFactor, g.display,
                false, null);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 1, mob.getZ(), 15, 0.3, 0.6, 0.3,
                0.02);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE,
                SoundSource.HOSTILE, 0.5f, 1.4f);
        guardians.put(g, mob.getUUID());
        fallenAt.remove(g);
        reviveAt.remove(g);
    }

    private Mob guardian(ServerLevel level, Guardian g) {
        UUID id = guardians.get(g);
        return id != null && level.getEntity(id) instanceof Mob m && m.isAlive() ? m : null;
    }

    /** Deaths, the Priest's heal and revives, the Mage's bolts. */
    private void tickGuardians(ServerLevel level) {
        for (Guardian g : Guardian.values()) {
            UUID id = guardians.get(g);
            if (id == null) {
                continue;
            }
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) {
                if (e instanceof Mob m && m.getTarget() == null) {
                    ServerPlayer target = nearest(level, m.position());
                    if (target != null) {
                        m.setTarget(target);
                    }
                }
                continue;
            }
            guardians.remove(g);
            Vec3 at = e != null ? e.position() : Vec3.atBottomCenterOf(spawnSpot(g));
            if (g == Guardian.PRIEST) {
                // He goes out with a bang: a short ring round where he fell.
                blasts.add(new Blast(at, master ? 5 : 4, now + 15, SimHealth.byMode(1_920, 36_000)));
                fallenAt.clear();
                reviveAt.clear();
            } else if (guardian(level, Guardian.PRIEST) != null) {
                fallenAt.put(g, at);
                reviveAt.put(g, now + (master ? 140 : 200));
                SimBoss.tell(level.getServer(), ModChat.text("The Undead Priest will raise the " + g.display
                        + " again - kill him!"));
            }
        }

        Mob priest = guardian(level, Guardian.PRIEST);
        if (priest != null) {
            if (--nextHeal <= 0) {
                nextHeal = healInterval();
                priestHeal(level, priest);
            }
            for (Guardian g : List.copyOf(reviveAt.keySet())) {
                Vec3 at = fallenAt.get(g);
                int left = reviveAt.get(g) - now;
                if (at != null && left > 0 && left <= 40 && now % 4 == 0) {
                    level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.3, at.z, 6, 0.4, 0.2, 0.4, 0.02);
                }
                if (left <= 0) {
                    raise(level, g, at == null ? spawnSpot(g) : BlockPos.containing(at));
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, priest.getX(), priest.getY() + 2,
                            priest.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
                }
            }
        }

        Mob mage = guardian(level, Guardian.MAGE);
        if (mage != null && now % (master ? 40 : 60) == 0) {
            ServerPlayer target = nearest(level, mage.position());
            if (target != null && target.distanceToSqr(mage) < 24 * 24) {
                mage.swing(InteractionHand.MAIN_HAND);
                throwBolts(level, mage.position().add(0, 1.5, 0), target, master ? 3 : 1, mageBolt());
            }
        }
    }

    /** Heals every living guardian (himself too) by a share of its pool. */
    private void priestHeal(ServerLevel level, Mob priest) {
        double share = master ? 0.2 : 0.12;
        for (Guardian g : Guardian.values()) {
            Mob m = guardian(level, g);
            SimHealth.Pool pool = m == null ? null : SimHealth.get(m);
            if (pool == null || pool.hp >= pool.max) {
                continue;
            }
            pool.heal(pool.max * share);
            level.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + 2, m.getZ(), 4, 0.4, 0.3, 0.4, 0.1);
        }
        priest.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, priest.getX(), priest.getY() + 1, priest.getZ(), 20,
                1.5, 0.8, 1.5, 0.1);
    }

    private int healInterval() {
        return master ? 70 : 100;
    }

    /** No guardian standing and none waiting on a revive. */
    private boolean allDown() {
        return guardians.isEmpty() && reviveAt.isEmpty();
    }

    /** The living guardians' health, summed; their max when {@code max}. */
    private double waveHealth(ServerLevel level, boolean max) {
        double sum = 0;
        for (Guardian g : Guardian.values()) {
            Mob m = guardian(level, g);
            SimHealth.Pool pool = m == null ? null : SimHealth.get(m);
            if (pool != null) {
                sum += max ? pool.max : Math.max(0, pool.hp);
            }
        }
        return sum;
    }

    private double waveHealthNow(ServerLevel level) {
        return waveHealth(level, true);
    }

    private void discardGuardians(ServerLevel level) {
        for (UUID id : guardians.values()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        guardians.clear();
        fallenAt.clear();
        reviveAt.clear();
    }

    // ------------------------------------------------------------------ shared bits

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

    private float face(ServerLevel level, double x, double z, float fallback) {
        ServerPlayer target = nearest(level, new Vec3(x, 0, z));
        if (target == null) {
            return fallback;
        }
        return (float) (Math.toDegrees(Math.atan2(target.getZ() - z, target.getX() - x)) - 90);
    }

    private static ServerPlayer nearest(ServerLevel level, Vec3 from) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            double dx = p.getX() - from.x;
            double dz = p.getZ() - from.z;
            double d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private static ServerPlayer randomPlayer(ServerLevel level) {
        List<ServerPlayer> in = SimBoss.playersInRing(level);
        return in.isEmpty() ? null : in.get(ThreadLocalRandom.current().nextInt(in.size()));
    }

    private void nameTag() {
        if (body == null) {
            return;
        }
        Component tag = Component.literal("Scarf ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
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
        if (phase == Phase.FIGHT && health.hit(amount)) {
            return true;
        }
        if (phase != Phase.DEAD && now - lastImmuneNote > 60 && health.invulnerable) {
            lastImmuneNote = now;
            SimBoss.tell(level.getServer(), ModChat.text("Scarf can't be hurt while his Undead stand."));
        }
        return false;
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
        discardGuardians(level);
        bolts.clear();
        blasts.clear();
    }

    /** While a wave is up the bar is theirs: "Scarf's Undead (3/4)", their summed health. */
    private boolean waveBar() {
        return phase == Phase.MINIONS || (phase == Phase.FIGHT && health.invulnerable);
    }

    @Override
    Component barName() {
        if (waveBar()) {
            int up = guardians.size();
            int total = secondWave && !master ? 2 : 4;
            return Component.literal("Scarf's Undead (" + up + "/" + total + ")").withStyle(ChatFormatting.GOLD);
        }
        return Component.literal("Scarf").withStyle(ChatFormatting.RED);
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return waveBar() ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED;
    }

    @Override
    float barProgress() {
        if (phase == Phase.INTRO || phase == Phase.DESCEND) {
            return 1f;
        }
        if (waveBar() && body != null && body.level() instanceof ServerLevel level) {
            return waveMaxHealth <= 0 ? 0f : (float) (waveHealth(level, false) / waveMaxHealth);
        }
        return health.fraction();
    }

    /** Scarf's body: no AI (the fight moves him), never despawns, never burns in the sim's daylight. */
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
     * The Warrior, the Priest and the Mage: zombies that never despawn or burn, and drop nothing they are holding
     * (an Undead Warrior's iron sword is not loot).
     */
    private static final class UndeadZombie extends Zombie {
        UndeadZombie(Level level) {
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

        @Override
        public void die(DamageSource source) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                setItemSlot(slot, ItemStack.EMPTY);
            }
            super.die(source);
        }
    }

    /** The Archer: a skeleton with a bow whose arrows hit for the floor, like the room skeletons. */
    private static final class Archer extends Skeleton {
        Archer(EntityType<? extends Skeleton> type, Level level) {
            super(type, level);
        }

        /** Half the attack damage times the bow's speed lands about where a zombie's swing does (see SimMobs). */
        @Override
        protected net.minecraft.world.entity.projectile.arrow.AbstractArrow getArrow(ItemStack arrow, float power,
                                                                                    ItemStack weapon) {
            var shot = super.getArrow(arrow, power, weapon);
            shot.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE) / 2.0);
            return shot;
        }

        @Override
        public void checkDespawn() {
        }

        @Override
        public void igniteForTicks(int ticks) {
        }

        @Override
        public boolean fireImmune() {
            return true;
        }

        @Override
        public void die(DamageSource source) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            super.die(source);
        }
    }
}
