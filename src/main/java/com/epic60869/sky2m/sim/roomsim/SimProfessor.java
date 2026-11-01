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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 3's boss, The Professor, and his four Guardians.
 *
 * <p>The fight in the order Hypixel runs it, tuned for practice rather than copied number for number:
 * <ol>
 * <li><b>The Guardians.</b> Four Guardians sit on the four diamond pads round the middle and The Professor watches
 * from his pillar, untouchable until all four are dead. Each has its own trick:
 * <ul>
 * <li><b>Healthy Guardian</b> - slowly heals the Guardians still alive.</li>
 * <li><b>Reinforced Guardian</b> - high defense: only {@link #REINFORCED_TAKEN} of each hit lands.</li>
 * <li><b>Chaos Guardian</b> - a slow beam that throws whoever it catches across the arena.</li>
 * <li><b>Laser Guardian</b> - quick, hard-hitting lasers.</li>
 * </ul>
 * Every beam is telegraphed: a thin line tracks its target while it charges, then LOCKS for the last half second
 * (it turns bright) and fires down that line. Step off the line once it locks and it misses.</li>
 * <li><b>The Professor.</b> He comes down off his pillar, walks at the nearest player and hits whoever he
 * reaches, and throws spirit bolts (three at a time in Master Mode).</li>
 * <li><b>The Guardian.</b> At a quarter of his health (where Hypixel ends his second phase) he transforms - three
 * untouchable seconds - into a giant Elder Guardian
 * floating over the middle, which lasers every player at once. The four Guardians come back on their pads
 * (weaker; killing them again is optional, but they keep shooting). Master Mode adds a <b>shockwave</b>: a ring
 * round the middle that blows up and throws everyone inside it out.</li>
 * </ol>
 *
 * <p>The phase lines that the mod reads (the first one, "Oh? You found my Guardians' one weakness?" and the death
 * line) are Hypixel's exact words - they drive Boss Phases, the splits and Clean End. The transformation lines are
 * Hypixel's as remembered; nothing in the mod reads them. The rest is the sim's own.
 */
final class SimProfessor extends SimBoss.Fight {

    // ------------------------------------------------------------------ health

    /**
     * The Professor's health, 3,000,000 on F3 and 600,000,000 on M3, and his base damage, 5,976 / 135,000, which his
     * swing, his bolts and the Guardian form's lasers deal (https://hypixelskyblock.minecraft.wiki/w/The_Professor;
     * the wiki's "Professor Laser" is his base damage). Hypixel ends his second phase at a quarter of that health
     * (750,000 / 150,000,000) and gives the Guardian form no pool of its own, so the form fights on with that last
     * quarter ({@link #TRANSFORM_SHARE}). The Master Mode shockwave has no number on the wiki and deals his base
     * damage too (APPROXIMATE).
     */
    private static double professorHealth() {
        return SimHealth.byMode(3_000_000, 600_000_000);
    }

    private static double professorDamage() {
        return SimHealth.byMode(5_976, 135_000);
    }

    /** The share of his health at which he transforms into the Guardian. */
    private static final double TRANSFORM_SHARE = 0.25;

    /** The share of each hit the Reinforced Guardian takes: its defense. APPROXIMATE. */
    private static final double REINFORCED_TAKEN = 0.3;

    /** The Healthy Guardian heals each other living Guardian this share of its pool per second. APPROXIMATE. */
    private static final double HEALTHY_HEAL_PER_SECOND = 0.01;

    /** The Guardians' share of their pool when they come back for the last phase: F3, then M3. */
    private static final double REVIVE_SHARE_NORMAL = 0.25;
    private static final double REVIVE_SHARE_MASTER = 0.5;

    private static final int INTRO_TICKS = 60;
    private static final int TRANSFORM_TICKS = 60;
    private static final double BEAM_LENGTH = 40;
    private static final double BEAM_WIDTH = 1.1;
    private static final double SHOCKWAVE_RADIUS = 7;

    /** The four diamond pads round the middle, as offsets from the arena centre in source blocks (arena data). */
    private static final int[][] PADS = {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
    /** The arena's real middle (the 5x5 pillar top) is a block south-east of the JSON's centre. */
    private static final int MIDDLE_DX = 0;
    private static final int MIDDLE_DZ = 1;

    private enum Phase { INTRO, GUARDIANS, PROFESSOR, TRANSFORM, GUARDIAN_FORM, DEAD }

    /**
     * The four Guardians, with Hypixel's health and base damage on F3 and M3; each one's beam deals its base damage,
     * as the Laser Guardian's Default Laser does (https://hypixelskyblock.minecraft.wiki/w/The_Professor).
     */
    private enum Kind {
        HEALTHY("Healthy Guardian", 1_000_000, 120_000_000, 4_392, 81_000, 0xFF5555, 120, 30, 0.6, 0.3),
        REINFORCED("Reinforced Guardian", 1_200_000, 140_000_000, 5_040, 81_000, 0x55FF55, 110, 30, 0.6, 0.3),
        CHAOS("Chaos Guardian", 1_000_000, 120_000_000, 2_880, 54_000, 0xAA00AA, 140, 40, 2.4, 0.9),
        LASER("Laser Guardian", 1_000_000, 120_000_000, 4_320, 81_000, 0x55FFFF, 70, 25, 0.5, 0.3);

        final String display;
        final double healthF3;
        final double healthM3;
        final double damageF3;
        final double damageM3;
        final int colour;
        /** Ticks between beams (Master Mode: two thirds). */
        final int beamEvery;
        /** Ticks a beam charges before firing (Master Mode: a quarter less). */
        final int charge;
        final double push;
        final double lift;

        Kind(String display, double healthF3, double healthM3, double damageF3, double damageM3, int colour,
             int beamEvery, int charge, double push, double lift) {
            this.display = display;
            this.healthF3 = healthF3;
            this.healthM3 = healthM3;
            this.damageF3 = damageF3;
            this.damageM3 = damageM3;
            this.colour = colour;
            this.beamEvery = beamEvery;
            this.charge = charge;
            this.push = push;
            this.lift = lift;
        }
    }

    /** One Guardian on its pad. */
    private static final class Pad {
        final Kind kind;
        final Vec3 at;
        Guardian body;
        SimBoss.Health health;
        int nextBeam;

        Pad(Kind kind, Vec3 at) {
            this.kind = kind;
            this.at = at;
        }

        boolean alive() {
            return body != null && health != null && health.hp > 0;
        }
    }

    /**
     * A telegraphed beam: tracks {@code target} until {@code lockAt}, then fires along the locked line at
     * {@code fireAt}.
     */
    private static final class Beam {
        final Entity from;
        final ServerPlayer target;
        final int lockAt;
        final int fireAt;
        final double damage;
        final double push;
        final double lift;
        final int colour;
        Vec3 end;

        Beam(Entity from, ServerPlayer target, int lockAt, int fireAt, double damage, double push, double lift,
             int colour) {
            this.from = from;
            this.target = target;
            this.lockAt = lockAt;
            this.fireAt = fireAt;
            this.damage = damage;
            this.push = push;
            this.lift = lift;
            this.colour = colour;
        }
    }

    /** The Professor's thrown bolt. */
    private record Bolt(Vec3 pos, Vec3 vel, int dieAt, double damage) {
    }

    private Phase phase = Phase.INTRO;
    private int phaseTicks;
    /** Counts every fight tick, for beams and bolts that live across phases. */
    private int clock;
    private final List<Pad> pads = new ArrayList<>();
    private final List<Beam> beams = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private final SimBoss.Health health = new SimBoss.Health(professorHealth());
    private Zombie professor;
    private ElderGuardian guardianForm;
    private int nextBolt;
    private int nextMelee;
    private int nextFormBeams;
    private int nextShockwave = -1;
    private int shockwaveAt = -1;
    private int protectedNote;
    private double orbit;

    SimProfessor(SimBossArena arena) {
        super(arena);
    }

    private static boolean master() {
        String f = SimState.floorLabel();
        return f != null && f.toUpperCase(java.util.Locale.ROOT).startsWith("M");
    }

    @Override
    String name() {
        return "The Professor";
    }

    @Override
    void start(ServerLevel level) {
        // Hypixel's first line, word for word: Boss Phases opens "Guardians" on it.
        SimBoss.say(level.getServer(), "The Professor", "I was burdened with terrible news recently...");
        spawnProfessor(level);
        spawnGuardians(level, 1.0);
        for (Pad p : pads) {
            p.health.invulnerable = true;
        }
        health.invulnerable = true;
        phase = Phase.INTRO;
        phaseTicks = 0;
    }

    @Override
    void tick(ServerLevel level, int t) {
        phaseTicks++;
        clock++;
        boolean master = master();
        switch (phase) {
            case INTRO -> {
                if (phaseTicks == 30) {
                    SimBoss.say(level.getServer(), "The Professor",
                            "My Guardians will make short work of you. Nobody gets past them.");
                }
                if (phaseTicks >= INTRO_TICKS) {
                    for (Pad p : pads) {
                        p.health.invulnerable = false;
                        p.nextBeam = clock + 20 + ThreadLocalRandom.current().nextInt(40);
                    }
                    toPhase(Phase.GUARDIANS);
                }
            }
            case GUARDIANS -> {
                tickGuardians(level, master);
                if (livingGuardians() == 0) {
                    // Hypixel's line, word for word: Boss Phases moves to "Professor" on it.
                    SimBoss.say(level.getServer(), "The Professor", "Oh? You found my Guardians' one weakness?");
                    health.invulnerable = false;
                    if (professor != null) {
                        professor.setGlowingTag(false);
                    }
                    beams.clear();
                    nextBolt = 40;
                    toPhase(Phase.PROFESSOR);
                }
            }
            case PROFESSOR -> {
                if (professor == null || professor.isRemoved()) {
                    spawnProfessor(level);
                }
                chase(level, master);
                if (--nextBolt <= 0) {
                    throwBolts(level, master);
                    nextBolt = master ? 45 : 60;
                }
                if (health.hp <= health.max * TRANSFORM_SHARE) {
                    transform(level);
                }
            }
            case TRANSFORM -> {
                Vec3 m = middle(level);
                level.sendParticles(ParticleTypes.BUBBLE, m.x, m.y + 3, m.z, 20, 1.5, 1.5, 1.5, 0.1);
                if (phaseTicks == TRANSFORM_TICKS / 2) {
                    SimBoss.say(level.getServer(), "The Professor",
                            "The process is irreversible, but I'll be stronger than a Wither now!");
                }
                if (phaseTicks >= TRANSFORM_TICKS) {
                    spawnGuardianForm(level, master);
                    toPhase(Phase.GUARDIAN_FORM);
                }
            }
            case GUARDIAN_FORM -> {
                if (guardianForm == null || guardianForm.isRemoved()) {
                    spawnGuardianForm(level, master);
                }
                floatForm(level, master);
                tickGuardians(level, master);
                if (clock >= nextFormBeams) {
                    formBeams(level, master);
                    nextFormBeams = clock + (master ? 40 : 60);
                }
                if (master) {
                    shockwave(level);
                }
                if (health.hp <= 0) {
                    die(level);
                }
            }
            default -> {
            }
        }
        tickBeams(level);
        tickBolts(level);
        nameTags();
        if (protectedNote > 0) {
            protectedNote--;
        }
    }

    private void toPhase(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    // ------------------------------------------------------------------ spawning

    /** The middle of the arena, on top of the pillar. */
    private Vec3 middle(ServerLevel level) {
        BlockPos c = arena.toWorld(arena.centre[0] + MIDDLE_DX, arena.centre[1], arena.centre[2] + MIDDLE_DZ);
        int y = ground(level, c.getX(), c.getZ(), c.getY() + 2);
        return new Vec3(c.getX() + 0.5, y, c.getZ() + 0.5);
    }

    /** The feet height of the first spot at or below {@code fromY} with something to stand on. */
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

    private void spawnProfessor(ServerLevel level) {
        Vec3 m = middle(level);
        Zombie z = new ProfessorBody(level);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.setCustomNameVisible(true);
        z.setGlowingTag(phase == Phase.INTRO || phase == Phase.GUARDIANS);
        z.snapTo(m.x, m.y, m.z, 180f, 0f);
        level.addFreshEntity(z);
        professor = z;
    }

    /** Puts the four Guardians on their pads, at {@code share} of their pools. */
    private void spawnGuardians(ServerLevel level, double share) {
        discardGuardians();
        Kind[] kinds = Kind.values();
        for (int i = 0; i < kinds.length; i++) {
            int[] o = PADS[i];
            BlockPos at = arena.toWorld(arena.centre[0] + MIDDLE_DX + o[0], arena.centre[1],
                    arena.centre[2] + MIDDLE_DZ + o[1]);
            int y = ground(level, at.getX(), at.getZ(), at.getY());
            Pad pad = new Pad(kinds[i], new Vec3(at.getX() + 0.5, y + 0.4, at.getZ() + 0.5));
            Guardian g = newGuardian(level);
            g.setNoGravity(true);
            g.setPersistenceRequired();
            g.setCustomNameVisible(true);
            g.snapTo(pad.at.x, pad.at.y, pad.at.z, 0f, 0f);
            level.addFreshEntity(g);
            pad.body = g;
            pad.health = new SimBoss.Health(SimHealth.byMode(kinds[i].healthF3, kinds[i].healthM3));
            pad.health.hp = pad.health.max * share;
            pad.nextBeam = clock + 40 + ThreadLocalRandom.current().nextInt(40);
            level.sendParticles(ParticleTypes.BUBBLE, pad.at.x, pad.at.y + 0.5, pad.at.z, 30, 0.6, 0.6, 0.6, 0.1);
            pads.add(pad);
        }
    }

    private void spawnGuardianForm(ServerLevel level, boolean master) {
        Vec3 m = middle(level);
        ElderGuardian g = new ElderGuardian(EntityTypes.ELDER_GUARDIAN, level);
        g.setNoAi(true);
        g.setMoving(true);
        g.setNoGravity(true);
        g.setPersistenceRequired();
        g.setCustomNameVisible(true);
        g.getAttribute(Attributes.SCALE).setBaseValue(master ? 2.5 : 2.0);
        g.snapTo(m.x, m.y + 4, m.z, 0f, 0f);
        level.addFreshEntity(g);
        guardianForm = g;
        if (phase != Phase.GUARDIAN_FORM) {
            // First time in: the last quarter of his pool, the Guardians back, the shockwave's clock started.
            health.hp = Math.max(1, Math.min(health.hp, health.max * TRANSFORM_SHARE));
            health.invulnerable = false;
            spawnGuardians(level, master ? REVIVE_SHARE_MASTER : REVIVE_SHARE_NORMAL);
            nextFormBeams = clock + 40;
            nextShockwave = clock + 120;
            level.playSound(null, m.x, m.y, m.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1f, 0.8f);
        }
    }

    // ------------------------------------------------------------------ phase 1 and 3: the Guardians

    private int livingGuardians() {
        int n = 0;
        for (Pad p : pads) {
            if (p.alive()) {
                n++;
            }
        }
        return n;
    }

    private void tickGuardians(ServerLevel level, boolean master) {
        List<ServerPlayer> players = SimBoss.playersInRing(level);
        for (Pad p : pads) {
            if (!p.alive()) {
                continue;
            }
            if (p.body.isRemoved()) {
                respawnPad(level, p);
            }
            // Face the nearest player, the way a Guardian's eye follows you.
            ServerPlayer near = nearest(players, p.body.position());
            if (near != null) {
                face(p.body, near.getEyePosition());
            }
            if (p.kind == Kind.HEALTHY && clock % 20 == 0) {
                for (Pad other : pads) {
                    if (other != p && other.alive() && other.health.hp < other.health.max) {
                        other.health.hp = Math.min(other.health.max,
                                other.health.hp + other.health.max * HEALTHY_HEAL_PER_SECOND);
                        level.sendParticles(ParticleTypes.HEART, other.at.x, other.at.y + 1.2, other.at.z, 1,
                                0.3, 0.2, 0.3, 0);
                    }
                }
            }
            if (clock >= p.nextBeam && !players.isEmpty()) {
                ServerPlayer target = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                int charge = master ? p.kind.charge * 3 / 4 : p.kind.charge;
                // Master Mode's beams come faster; their damage is the Guardian's own M3 number.
                beams.add(new Beam(p.body, target, clock + charge - 10, clock + charge,
                        master ? p.kind.damageM3 : p.kind.damageF3,
                        p.kind.push, p.kind.lift, p.kind.colour));
                level.playSound(null, p.at.x, p.at.y, p.at.z, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE,
                        0.8f, 1.2f);
                p.nextBeam = clock + (master ? p.kind.beamEvery * 2 / 3 : p.kind.beamEvery)
                        + ThreadLocalRandom.current().nextInt(20);
            }
        }
    }

    private void respawnPad(ServerLevel level, Pad p) {
        Guardian g = newGuardian(level);
        g.setNoGravity(true);
        g.setPersistenceRequired();
        g.setCustomNameVisible(true);
        g.snapTo(p.at.x, p.at.y, p.at.z, 0f, 0f);
        level.addFreshEntity(g);
        p.body = g;
    }

    /**
     * A Guardian with no AI. Marked "moving" on purpose: vanilla's Guardian pricks whoever hits it with Thorns
     * unless it is moving, and that would be a second, unasked-for damage source on every swing.
     */
    private static Guardian newGuardian(ServerLevel level) {
        Guardian g = new Guardian(EntityTypes.GUARDIAN, level);
        g.setNoAi(true);
        g.setMoving(true);
        return g;
    }

    private void killGuardian(ServerLevel level, Pad p) {
        if (p.body != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, p.body.getX(), p.body.getY() + 0.5, p.body.getZ(), 2,
                    0.3, 0.3, 0.3, 0);
            level.playSound(null, p.body.getX(), p.body.getY(), p.body.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 0.7f, 1.4f);
            p.body.discard();
            p.body = null;
        }
        beams.removeIf(b -> b.from == null || b.from.isRemoved());
        int left = livingGuardians();
        if (phase == Phase.GUARDIANS && left > 0) {
            SimBoss.tell(level.getServer(), Component.literal(p.kind.display
                    + " down, " + left + " left.").withStyle(ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------------ phase 2: The Professor

    /** Walks at the nearest player and hits whoever he reaches. Faster in Master Mode. */
    private void chase(ServerLevel level, boolean master) {
        if (professor == null) {
            return;
        }
        ServerPlayer target = nearest(SimBoss.playersInRing(level), professor.position());
        if (target == null) {
            return;
        }
        Vec3 from = professor.position();
        double dx = target.getX() - from.x;
        double dz = target.getZ() - from.z;
        double d = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        if (d > 1.8) {
            double step = Math.min(d - 1.5, master ? 0.2 : 0.14);
            double nx = from.x + dx / d * step;
            double nz = from.z + dz / d * step;
            int y = ground(level, (int) Math.floor(nx), (int) Math.floor(nz), (int) Math.floor(from.y) + 1);
            if (Math.abs(y - from.y) <= 1.5) {
                professor.snapTo(nx, y, nz, yaw, 0f);
            } else {
                professor.snapTo(from.x, from.y, from.z, yaw, 0f);
            }
        } else {
            professor.snapTo(from.x, from.y, from.z, yaw, 0f);
        }
        professor.setYHeadRot(yaw);
        professor.setYBodyRot(yaw);
        if (nextMelee > 0) {
            nextMelee--;
        } else if (d < 2.6) {
            professor.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            hurtPlayer(level, target, professorDamage(), dx, dz, 0.5, 0.3);
            nextMelee = 20;
        }
    }

    /** One bolt at each player; three in a fan in Master Mode. */
    private void throwBolts(ServerLevel level, boolean master) {
        if (professor == null) {
            return;
        }
        Vec3 from = professor.position().add(0, 1.5, 0);
        double damage = professorDamage();
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            Vec3 dir = p.position().add(0, 1, 0).subtract(from).normalize();
            int n = master ? 3 : 1;
            for (int i = 0; i < n; i++) {
                double spread = (i - (n - 1) / 2.0) * 0.25;
                Vec3 d = new Vec3(dir.x * Math.cos(spread) - dir.z * Math.sin(spread), dir.y,
                        dir.x * Math.sin(spread) + dir.z * Math.cos(spread));
                bolts.add(new Bolt(from, d.scale(master ? 0.9 : 0.7), clock + 60, damage));
            }
        }
        professor.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1f, 1.2f);
    }

    private void transform(ServerLevel level) {
        SimBoss.say(level.getServer(), "The Professor", "I see. You have forced me to use my ultimate technique.");
        health.invulnerable = true;
        bolts.clear();
        beams.clear();
        if (professor != null) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, professor.getX(), professor.getY() + 1,
                    professor.getZ(), 30, 0.5, 1, 0.5, 0.05);
            professor.discard();
            professor = null;
        }
        level.playSound(null, middle(level).x, middle(level).y, middle(level).z, SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 0.6f, 1.4f);
        toPhase(Phase.TRANSFORM);
    }

    // ------------------------------------------------------------------ phase 3: the Guardian

    /** Floats over the middle in a slow circle, looking at the nearest player. */
    private void floatForm(ServerLevel level, boolean master) {
        if (guardianForm == null) {
            return;
        }
        Vec3 m = middle(level);
        orbit += master ? 0.03 : 0.02;
        double x = m.x + Math.cos(orbit) * 3;
        double z = m.z + Math.sin(orbit) * 3;
        double y = m.y + 4 + Math.sin(clock * 0.05) * 0.5;
        guardianForm.snapTo(x, y, z, guardianForm.getYRot(), 0f);
        ServerPlayer near = nearest(SimBoss.playersInRing(level), guardianForm.position());
        if (near != null) {
            face(guardianForm, near.getEyePosition());
        }
    }

    /** A laser at every player at once. */
    private void formBeams(ServerLevel level, boolean master) {
        if (guardianForm == null) {
            return;
        }
        int charge = master ? 25 : 35;
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            beams.add(new Beam(guardianForm, p, clock + charge - 10, clock + charge,
                    professorDamage(), 0.8, 0.4, 0xFF55FF));
        }
        level.playSound(null, guardianForm.getX(), guardianForm.getY(), guardianForm.getZ(),
                SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.7f);
    }

    /** Master Mode: a ring round the middle that blows up after a warning and throws everyone in it out. */
    private void shockwave(ServerLevel level) {
        Vec3 m = middle(level);
        if (shockwaveAt < 0 && clock >= nextShockwave) {
            shockwaveAt = clock + 40;
            level.playSound(null, m.x, m.y, m.z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1f, 0.6f);
        }
        if (shockwaveAt < 0) {
            return;
        }
        if (clock < shockwaveAt) {
            if (clock % 3 == 0) {
                DustParticleOptions purple = new DustParticleOptions(0xAA00FF, 1.5f);
                for (int i = 0; i < 40; i++) {
                    double a = Math.PI * 2 * i / 40;
                    level.sendParticles(purple, m.x + Math.cos(a) * SHOCKWAVE_RADIUS, m.y + 0.1,
                            m.z + Math.sin(a) * SHOCKWAVE_RADIUS, 1, 0, 0, 0, 0);
                }
            }
            return;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, m.x, m.y + 0.5, m.z, 12, SHOCKWAVE_RADIUS / 2, 0.3,
                SHOCKWAVE_RADIUS / 2, 0);
        level.playSound(null, m.x, m.y, m.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2f, 0.7f);
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            double dx = p.getX() - m.x;
            double dz = p.getZ() - m.z;
            if (dx * dx + dz * dz <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS) {
                hurtPlayer(level, p, professorDamage(), dx, dz, 2.0, 0.8);
            }
        }
        shockwaveAt = -1;
        nextShockwave = clock + 160;
    }

    private void die(ServerLevel level) {
        // Hypixel's last line, word for word: Boss Phases, the splits and Clean End read it as the boss down.
        SimBoss.say(level.getServer(), "The Professor", "What?! My Guardian power is unbeatable!");
        if (guardianForm != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, guardianForm.getX(), guardianForm.getY() + 1,
                    guardianForm.getZ(), 5, 1, 1, 1, 0);
            guardianForm.discard();
            guardianForm = null;
        }
        discardGuardians();
        beams.clear();
        bolts.clear();
        toPhase(Phase.DEAD);
    }

    // ------------------------------------------------------------------ beams and bolts

    private void tickBeams(ServerLevel level) {
        if (beams.isEmpty()) {
            return;
        }
        List<Beam> done = new ArrayList<>();
        for (Beam b : beams) {
            if (b.from == null || b.from.isRemoved() || b.target.isRemoved()) {
                done.add(b);
                continue;
            }
            Vec3 start = b.from.position().add(0, b.from.getBbHeight() / 2, 0);
            if (clock < b.lockAt || b.end == null) {
                // Tracking: the line follows him. Aimed through his chest and on past him.
                Vec3 aim = b.target.position().add(0, 1, 0).subtract(start);
                b.end = start.add(aim.normalize().scale(BEAM_LENGTH));
            }
            if (clock < b.fireAt) {
                if (clock % 2 == 0) {
                    boolean locked = clock >= b.lockAt;
                    drawLine(level, start, b.end, new DustParticleOptions(b.colour, locked ? 1.4f : 0.6f),
                            locked ? 0.5 : 1.2);
                }
                continue;
            }
            fire(level, b, start);
            done.add(b);
        }
        beams.removeAll(done);
    }

    private void fire(ServerLevel level, Beam b, Vec3 start) {
        drawLine(level, start, b.end, ParticleTypes.END_ROD, 0.6);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1f, 0.6f);
        for (ServerPlayer p : SimBoss.playersInRing(level)) {
            Vec3 chest = p.position().add(0, 1, 0);
            if (distanceToSegment(chest, start, b.end) <= BEAM_WIDTH) {
                Vec3 dir = b.end.subtract(start);
                hurtPlayer(level, p, b.damage, dir.x, dir.z, b.push, b.lift);
            }
        }
    }

    private void tickBolts(ServerLevel level) {
        if (bolts.isEmpty()) {
            return;
        }
        List<Bolt> next = new ArrayList<>();
        List<ServerPlayer> players = SimBoss.playersInRing(level);
        for (Bolt b : bolts) {
            Vec3 pos = b.pos().add(b.vel());
            BlockPos block = BlockPos.containing(pos);
            if (clock >= b.dieAt() || !level.getBlockState(block).getCollisionShape(level, block).isEmpty()) {
                level.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.02);
                continue;
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0);
            boolean hit = false;
            for (ServerPlayer p : players) {
                if (p.getBoundingBox().inflate(0.4).contains(pos)) {
                    hurtPlayer(level, p, b.damage(), b.vel().x, b.vel().z, 0.5, 0.3);
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

    private static void drawLine(ServerLevel level, Vec3 a, Vec3 b,
                                 net.minecraft.core.particles.ParticleOptions particle, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) {
            return;
        }
        Vec3 u = d.scale(1 / len);
        for (double s = 0; s <= len; s += step) {
            level.sendParticles(particle, a.x + u.x * s, a.y + u.y * s, a.z + u.z * s, 1, 0, 0, 0, 0);
        }
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /**
     * Damage plus knockback along {@code (dx, dz)}: {@code damage} is Hypixel damage before the player's Defense
     * ({@link SimVitals#dealHypixel}).
     */
    private static void hurtPlayer(ServerLevel level, ServerPlayer p, double damage, double dx, double dz,
                                   double push, double lift) {
        SimVitals.dealHypixel(level, p, level.damageSources().magic(), damage, false);
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

    private static void face(Mob mob, Vec3 at) {
        double dx = at.x - mob.getX();
        double dz = at.z - mob.getZ();
        double dy = at.y - mob.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        mob.setYRot(yaw);
        mob.setXRot(pitch);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
    }

    // ------------------------------------------------------------------ names and the bar

    private void nameTags() {
        for (Pad p : pads) {
            if (p.alive()) {
                p.body.setCustomName(tag(p.kind.display, p.health));
            }
        }
        if (professor != null) {
            professor.setCustomName(tag("The Professor", health));
        }
        if (guardianForm != null) {
            guardianForm.setCustomName(tag("The Professor", health));
        }
    }

    private static Component tag(String name, SimBoss.Health h) {
        return Component.literal(name + " ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(h.invulnerable ? "???" : SimBoss.shortHealth(h.hp))
                        .withStyle(ChatFormatting.GREEN).append(Component.literal("❤")
                                .withStyle(ChatFormatting.RED)));
    }

    @Override
    boolean isBoss(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity == professor || entity == guardianForm) {
            return true;
        }
        for (Pad p : pads) {
            if (p.body == entity) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean hit(ServerLevel level, LivingEntity body, float amount) {
        for (Pad p : pads) {
            if (p.body == body) {
                if (!p.alive()) {
                    return false;
                }
                float dealt = p.kind == Kind.REINFORCED ? (float) (amount * REINFORCED_TAKEN) : amount;
                boolean landed = p.health.hit(dealt);
                if (landed && p.health.hp <= 0) {
                    killGuardian(level, p);
                }
                return landed;
            }
        }
        if (body == professor && phase == Phase.GUARDIANS) {
            if (protectedNote == 0) {
                protectedNote = 40;
                for (ServerPlayer p : SimBoss.playersInRing(level)) {
                    p.sendSystemMessage(Component.literal("The Professor is protected by his Guardians!")
                            .withStyle(ChatFormatting.RED), true);
                }
            }
            return false;
        }
        return (phase == Phase.PROFESSOR || phase == Phase.GUARDIAN_FORM) && health.hit(amount);
    }

    @Override
    boolean defeated() {
        return phase == Phase.DEAD;
    }

    @Override
    void discard(ServerLevel level) {
        if (professor != null) {
            professor.discard();
            professor = null;
        }
        if (guardianForm != null) {
            guardianForm.discard();
            guardianForm = null;
        }
        discardGuardians();
        beams.clear();
        bolts.clear();
    }

    private void discardGuardians() {
        for (Pad p : pads) {
            if (p.body != null) {
                p.body.discard();
                p.body = null;
            }
        }
        pads.clear();
    }

    @Override
    Component barName() {
        return switch (phase) {
            case INTRO, GUARDIANS -> Component.literal("The Professor's Guardians (" + livingGuardians() + " left)")
                    .withStyle(ChatFormatting.AQUA);
            case GUARDIAN_FORM, TRANSFORM -> Component.literal("The Professor (Guardian)")
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            default -> Component.literal("The Professor").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return switch (phase) {
            case INTRO, GUARDIANS -> BossEvent.BossBarColor.BLUE;
            case GUARDIAN_FORM, TRANSFORM -> BossEvent.BossBarColor.PURPLE;
            default -> BossEvent.BossBarColor.RED;
        };
    }

    @Override
    float barProgress() {
        return switch (phase) {
            case INTRO -> 1f;
            case GUARDIANS -> {
                double hp = 0;
                double max = 0;
                for (Pad p : pads) {
                    hp += Math.max(0, p.health.hp);
                    max += p.health.max;
                }
                yield max <= 0 ? 0f : (float) (hp / max);
            }
            case TRANSFORM -> Math.min(1f, (float) phaseTicks / TRANSFORM_TICKS);
            default -> health.fraction();
        };
    }

    /** The Professor's body: no AI (the fight moves him), never despawns, never burns in the sim's daylight. */
    private static final class ProfessorBody extends Zombie {
        ProfessorBody(Level level) {
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
    }
}
