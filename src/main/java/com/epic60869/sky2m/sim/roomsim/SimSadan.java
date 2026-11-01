package com.epic60869.sky2m.sim.roomsim;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 6's boss, Sadan, in his tomb.
 *
 * <p>Three phases, the way the fight goes on Hypixel, tuned for practice rather than copied number for number:
 * <ul>
 * <li><b>Terracottas.</b> Terracotta soldiers rise along both sides of the tomb and come at you. Each one that
 * dies leaves a flower pot behind and rises again from it 15 seconds later (12 in Master Mode) - the pot is what
 * the Terracotta Timer counts down over - the pot turning back into terracotta two seconds before it does. The
 * phase is on a clock (60 seconds, 75 in Master Mode): killing them only buys you room until Sadan's "ENOUGH!".</li>
 * <li><b>Giants.</b> The four Giants wake in the corners and walk at you, each with its own attack:
 * <i>Bigfoot</i> leaps and stomps (get out of the ring of cloud round him), <i>L.A.S.R.</i> locks a red beam on a
 * player and fires down it (move off the line), the <i>Jolly Pink Giant</i> lobs pink bombs at your feet, and the
 * <i>Diamond Giant</i> winds up and swings very hard at anyone close. In Master Mode the Terracottas keep rising
 * through this phase too, and every Giant attacks faster. Kill all four.</li>
 * <li><b>Sadan.</b> Sadan himself, a Giant with the biggest pool: he swings like the Diamond Giant, stomps like
 * Bigfoot and calls Terracottas back up.</li>
 * </ul>
 *
 * <p>The phase lines are Hypixel's exact words where the mod reads them - the opening ("So you made it all the
 * way here...": Boss Phases' Terracottas, the dungeon's boss detection the Terracotta Timer waits on), "ENOUGH!"
 * (Giants) and "You did it. I understand now, you have earned my respect." (Sadan, after the Giants) - and his
 * death lines are Hypixel's; the lines in between are the sim's own.
 *
 * <p>The Giants and Sadan are vanilla {@link Giant}s with no AI, moved by the fight, their health in pools of
 * this fight's own ({@link SimBoss.Health}) like every boss. The Terracottas are ordinary chasing zombies with
 * room-mob pools ({@link SimHealth#TERRACOTTA}).
 *
 * <p><b>Health and damage are Hypixel's</b>, before the player's Defense:
 * <ul>
 * <li>Sadan 40,000,000 on F6 and 600,000,000 on M6, base damage 30,960 / 720,000
 * (https://hypixelskyblock.minecraft.wiki/w/Sadan). The wiki gives his last phase no attack numbers of its own, so his
 * swing and stomp deal his base damage (APPROXIMATE).</li>
 * <li>F6's Giants, 25,000,000 each: Bigfoot's stomp up to 88,000
 * (https://hypixelskyblock.minecraft.wiki/w/Bigfoot), one tick of L.A.S.R.'s laser 8,800
 * (https://hypixelskyblock.minecraft.wiki/w/L.A.S.R.; Hypixel's fires every half second for about five, the sim's
 * once), the Jolly Pink Giant's boulder up to 52,800 (https://hypixelskyblock.minecraft.wiki/w/Jolly_Pink_Giant), the
 * Diamond Giant's sword 25,160 (https://hypixelskyblock.minecraft.wiki/w/The_Diamond_Giant).</li>
 * <li>M6 replaces all four with Mutant Giants (https://hypixelskyblock.minecraft.wiki/w/Mutant_Giant), 600,000,000
 * each with the same attacks at stomp 4,000,000, laser 500,000, boulder 3,000,000 and sword 200,000; the sim keeps
 * the four F6 Giants' looks and gives each its Mutant Giant numbers.</li>
 * </ul>
 */
final class SimSadan extends SimBoss.Fight {

    /** Sadan's health, F6 then M6. */
    private static double sadanHealth() {
        return SimHealth.byMode(40_000_000, 600_000_000);
    }

    /** Sadan's base damage, which his swing and stomp deal. */
    private static double sadanDamage() {
        return SimHealth.byMode(30_960, 720_000);
    }

    /** A Giant's health: 25,000,000 on F6, a Mutant Giant's 600,000,000 on M6. */
    private static double giantHealth() {
        return SimHealth.byMode(25_000_000, 600_000_000);
    }

    /** The tomb, in the arena's own coordinates: the floor the fight is on. */
    private static final int[] TOMB_MIN = {-29, 69, 39};
    private static final int[] TOMB_MAX = {11, 69, 93};
    /** Where the Terracottas stand: both long walls, eight a side. */
    private static final int[] TERRACOTTA_X = {-26, 8};
    private static final int[] TERRACOTTA_Z = {45, 51, 57, 63, 69, 75, 81, 87};
    /** The four Giants' corners. */
    private static final int[][] GIANT_SPOTS = {{-22, 44}, {4, 44}, {-22, 88}, {4, 88}};

    private enum Phase { TERRACOTTAS, GIANTS_WAKING, GIANTS, SADAN_RISING, SADAN, DYING, DEAD }

    /** What a Giant does, which picks its attack. */
    private enum Kind {
        /** The stomp: up to 88,000 / 4,000,000. */
        BIGFOOT("Bigfoot", 88_000, 4_000_000),
        /** One tick of the laser: 8,800 / 500,000. */
        LASR("L.A.S.R.", 8_800, 500_000),
        /** The boulder: up to 52,800 / 3,000,000. */
        JOLLY("Jolly Pink Giant", 52_800, 3_000_000),
        /** The sword: 25,160 / 200,000. */
        DIAMOND("The Diamond Giant", 25_160, 200_000),
        SADAN("Sadan", 30_960, 720_000);

        final String display;
        final double attackF6;
        final double attackM6;

        Kind(String display, double attackF6, double attackM6) {
            this.display = display;
            this.attackF6 = attackF6;
            this.attackM6 = attackM6;
        }

        /** This Giant's attack on the current floor's mode, before Defense. */
        double attack() {
            return SimHealth.byMode(attackF6, attackM6);
        }
    }

    private Phase phase = Phase.TERRACOTTAS;
    private int phaseTicks;
    private int ticks;
    private final boolean master = SimState.floorLabel().toUpperCase(Locale.ROOT).startsWith("M");
    private final List<Slot> slots = new ArrayList<>();
    private final List<Big> giants = new ArrayList<>();
    private Big sadan;
    private final List<Bomb> bombs = new ArrayList<>();

    /** One Terracotta's place by the wall: its mob, or the pot it rises from again. */
    private static final class Slot {
        final BlockPos at;
        Terracotta mob;
        /** Tick it rises again, or -1 while it is up. */
        int risesAt = -1;
        boolean potPlaced;

        Slot(BlockPos at) {
            this.at = at;
        }
    }

    /** A Giant (or Sadan): its body, pool and attack timing. */
    private static final class Big {
        final Kind kind;
        final GiantBody body;
        final SimBoss.Health health;
        int cooldown;
        /** Ticks into a telegraphed attack, -1 when not winding up. */
        int windup = -1;
        Vec3 aim;
        /** The stomp has its own clock and wind-up, so Sadan can have it and his swing. */
        int stompCooldown;
        int stompWindup = -1;
        int summonCooldown;
        boolean dead;

        Big(Kind kind, GiantBody body, double max) {
            this.kind = kind;
            this.body = body;
            this.health = new SimBoss.Health(max);
        }
    }

    private record Bomb(Vec3 at, int landsAt) {
    }

    SimSadan(SimBossArena arena) {
        super(arena);
    }

    @Override
    String name() {
        return "Sadan";
    }

    @Override
    void start(ServerLevel level) {
        // Hypixel's first line, word for word: Boss Phases' Terracottas and the boss detection start on it.
        SimBoss.say(level.getServer(), "Sadan", "So you made it all the way here... Now you wish to defy me? Sadan?!");
        slots.clear();
        for (int x : TERRACOTTA_X) {
            for (int z : TERRACOTTA_Z) {
                BlockPos p = arena.toWorld(x, TOMB_MIN[1], z);
                slots.add(new Slot(new BlockPos(p.getX(), (int) Math.ceil(ground(level, p.getX() + 0.5,
                        p.getZ() + 0.5)), p.getZ())));
            }
        }
        // They rise a couple of seconds in, staggered, rather than all on the opening line.
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).risesAt = 40 + (i % 8) * 4;
        }
        toPhase(Phase.TERRACOTTAS);
    }

    private int terracottaTicks() {
        return master ? 1500 : 1200;
    }

    private int respawnTicks() {
        // Hypixel's: 15 s on F6, 12 s on M6 - the Terracotta Timer's numbers.
        return master ? 240 : 300;
    }

    @Override
    void tick(ServerLevel level, int t) {
        ticks = t;
        phaseTicks++;
        switch (phase) {
            case TERRACOTTAS -> {
                tickSlots(level, true);
                if (phaseTicks == 300) {
                    SimBoss.say(level.getServer(), "Sadan", "I am the bridge between this realm and the world below! You shall not pass!");
                } else if (phaseTicks == terracottaTicks() - 200) {
                    SimBoss.say(level.getServer(), "Sadan", "My Terracotta Army is endless. Give up!");
                }
                if (phaseTicks >= terracottaTicks()) {
                    // Hypixel's line: Boss Phases' Giants start on it.
                    SimBoss.say(level.getServer(), "Sadan", "ENOUGH!");
                    if (!master) {
                        clearTerracottas(level);
                    }
                    toPhase(Phase.GIANTS_WAKING);
                }
            }
            case GIANTS_WAKING -> {
                tickSlots(level, master);
                if (phaseTicks == 40) {
                    SimBoss.say(level.getServer(), "Sadan", "My giants! Unleashed!");
                    wakeGiants(level);
                    toPhase(Phase.GIANTS);
                }
            }
            case GIANTS -> {
                tickSlots(level, master);
                boolean anyAlive = false;
                for (Big g : giants) {
                    if (!g.dead) {
                        tickBig(level, g);
                        anyAlive |= !g.dead;
                    }
                }
                if (!anyAlive) {
                    // Hypixel's line: Boss Phases' Sadan phase starts on it.
                    SimBoss.say(level.getServer(), "Sadan", "You did it. I understand now, you have earned my respect.");
                    clearTerracottas(level);
                    toPhase(Phase.SADAN_RISING);
                }
            }
            case SADAN_RISING -> {
                if (phaseTicks == 60) {
                    SimBoss.say(level.getServer(), "Sadan", "But if you wish to go further, you must defeat me first!");
                    sadan = spawnBig(level, Kind.SADAN, arena.toWorld(arena.bossSpawn), sadanHealth());
                    sadan.cooldown = 60;
                    sadan.stompCooldown = 140;
                    sadan.summonCooldown = 200;
                    toPhase(Phase.SADAN);
                }
            }
            case SADAN -> {
                tickSlots(level, false);
                if (sadan != null && !sadan.dead) {
                    tickBig(level, sadan);
                }
                if (sadan == null || sadan.dead) {
                    // Hypixel's death lines.
                    SimBoss.say(level.getServer(), "Sadan", "NOOOOOOOOO!!! THIS IS IMPOSSIBLE!!");
                    clearTerracottas(level);
                    toPhase(Phase.DYING);
                }
            }
            case DYING -> {
                if (phaseTicks == 40) {
                    SimBoss.say(level.getServer(), "Sadan", "FATHER, FORGIVE ME!!!");
                    toPhase(Phase.DEAD);
                }
            }
            default -> {
            }
        }
        tickBombs(level);
        if (t % 20 == 0) {
            clearDrops(level);
        }
    }

    private void toPhase(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    // ------------------------------------------------------------------ Terracottas

    /**
     * Each slot: a dead Terracotta leaves a pot, the pot turns to terracotta two seconds before it rises, then it
     * rises. {@code rising} false lets the dead stay dead (the pots go too).
     */
    private void tickSlots(ServerLevel level, boolean rising) {
        for (Slot s : slots) {
            if (s.mob != null) {
                if (s.mob.isAlive() && !s.mob.isRemoved()) {
                    continue;
                }
                s.mob = null;
                if (!rising) {
                    continue;
                }
                // The pot the Terracotta Timer counts down over.
                level.setBlock(s.at, Blocks.FLOWER_POT.defaultBlockState(), 3);
                s.potPlaced = true;
                s.risesAt = ticks + respawnTicks();
                continue;
            }
            if (!rising) {
                if (s.potPlaced) {
                    level.setBlock(s.at, Blocks.AIR.defaultBlockState(), 3);
                    s.potPlaced = false;
                }
                continue;
            }
            if (s.risesAt < 0) {
                // Cleared at the end of a phase and rising again now (M6's Giants, Sadan's call).
                continue;
            }
            if (s.potPlaced && ticks == s.risesAt - 40) {
                level.setBlock(s.at, Blocks.TERRACOTTA.defaultBlockState(), 3);
            }
            if (ticks >= s.risesAt) {
                level.setBlock(s.at, Blocks.AIR.defaultBlockState(), 3);
                s.potPlaced = false;
                s.risesAt = -1;
                s.mob = spawnTerracotta(level, s.at);
            }
        }
    }

    private Terracotta spawnTerracotta(ServerLevel level, BlockPos at) {
        Terracotta t = new Terracotta(level, SimHealth.meleeDamage(SimState.floorLabel(), SimHealth.Role.STARRED));
        t.setPersistenceRequired();
        t.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TERRACOTTA));
        t.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, 0x8E5A3C));
        t.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, 0x8E5A3C));
        t.setDropChance(EquipmentSlot.HEAD, 0f);
        t.setDropChance(EquipmentSlot.CHEST, 0f);
        t.setDropChance(EquipmentSlot.LEGS, 0f);
        double x = at.getX() + 0.5;
        double z = at.getZ() + 0.5;
        BlockPos c = arena.toWorld(arena.centre);
        float yaw = (float) (Math.toDegrees(Math.atan2(c.getZ() - z, c.getX() - x)) - 90);
        t.snapTo(x, at.getY(), z, yaw, 0f);
        ServerPlayer target = nearest(playersInTomb(level), t.position());
        if (target != null) {
            t.setTarget(target);
        }
        level.addFreshEntity(t);
        // Registered by its Hypixel name, the pool takes the wiki's health and hit (SimHealth.statsFor).
        SimHealth.register(t, SimHealth.maxHealth(SimState.floorLabel(), SimHealth.Role.STARRED), "Terracotta",
                false, null);
        level.sendParticles(ParticleTypes.POOF, x, at.getY() + 0.5, z, 10, 0.3, 0.4, 0.3, 0.02);
        return t;
    }

    /** Every Terracotta down and every pot gone, for a phase that has none. */
    private void clearTerracottas(ServerLevel level) {
        for (Slot s : slots) {
            if (s.mob != null) {
                level.sendParticles(ParticleTypes.POOF, s.mob.getX(), s.mob.getY() + 1, s.mob.getZ(), 8, 0.3, 0.5,
                        0.3, 0.02);
                s.mob.discard();
                s.mob = null;
            }
            if (s.potPlaced) {
                level.setBlock(s.at, Blocks.AIR.defaultBlockState(), 3);
                s.potPlaced = false;
            }
            s.risesAt = -1;
        }
    }

    /** Sadan calls some of the fallen back up: they rise from their slots right away. */
    private void callTerracottas(ServerLevel level, int count) {
        List<Slot> empty = new ArrayList<>();
        for (Slot s : slots) {
            if (s.mob == null && s.risesAt < 0) {
                empty.add(s);
            }
        }
        java.util.Collections.shuffle(empty);
        for (int i = 0; i < Math.min(count, empty.size()); i++) {
            Slot s = empty.get(i);
            s.mob = spawnTerracotta(level, s.at);
        }
    }

    /** The Terracottas' zombie drops and experience, which the tomb would otherwise fill up with. */
    private void clearDrops(ServerLevel level) {
        AABB box = tombBox().inflate(4, 6, 4);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            item.discard();
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            orb.discard();
        }
    }

    // ------------------------------------------------------------------ Giants

    private void wakeGiants(ServerLevel level) {
        Kind[] kinds = {Kind.BIGFOOT, Kind.LASR, Kind.JOLLY, Kind.DIAMOND};
        // Shuffled corners, so the same Giant is not always in the same one.
        List<int[]> spots = new ArrayList<>(List.of(GIANT_SPOTS));
        java.util.Collections.shuffle(spots);
        for (int i = 0; i < kinds.length; i++) {
            int[] s = spots.get(i);
            BlockPos at = arena.toWorld(s[0], TOMB_MIN[1], s[1]);
            Big g = spawnBig(level, kinds[i], at, giantHealth());
            g.cooldown = 60 + i * 20;
            g.stompCooldown = 60 + i * 20;
            giants.add(g);
        }
        BlockPos c = arena.toWorld(arena.centre);
        level.playSound(null, c.getX(), c.getY(), c.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1f,
                0.6f);
    }

    private Big spawnBig(ServerLevel level, Kind kind, BlockPos at, double max) {
        GiantBody body = new GiantBody(level);
        body.setNoAi(true);
        body.setNoGravity(true);
        body.setPersistenceRequired();
        body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        body.setHealth(1000);
        dress(body, kind);
        double x = at.getX() + 0.5;
        double z = at.getZ() + 0.5;
        body.snapTo(x, ground(level, x, z), z, 0f, 0f);
        body.setCustomNameVisible(true);
        level.addFreshEntity(body);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, body.getY() + 1, z, 1, 0, 0, 0, 0);
        Big g = new Big(kind, body, max);
        nameTag(g);
        return g;
    }

    private static void dress(GiantBody body, Kind kind) {
        switch (kind) {
            case BIGFOOT -> {
                body.setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, 0x5C3A1E));
                body.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, 0x5C3A1E));
            }
            case LASR -> {
                body.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                body.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }
            case JOLLY -> {
                body.setItemSlot(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, 0xFF7FD0));
                body.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, 0xFF7FD0));
                body.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, 0xFF7FD0));
            }
            case DIAMOND -> {
                body.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                body.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                body.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                body.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                body.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            }
            case SADAN -> {
                body.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                body.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                body.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            }
        }
    }

    private static ItemStack dyed(Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        return stack;
    }

    /** One Giant's tick: walk, attack, die. */
    private void tickBig(ServerLevel level, Big g) {
        if (g.health.hp <= 0) {
            g.dead = true;
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, g.body.getX(), g.body.getY() + 4, g.body.getZ(),
                    2, 1, 2, 1, 0);
            level.playSound(null, g.body.getX(), g.body.getY(), g.body.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 2f, 0.6f);
            g.body.discard();
            return;
        }
        List<ServerPlayer> targets = playersInTomb(level);
        ServerPlayer target = nearest(targets, g.body.position());
        boolean winding = g.windup >= 0 || g.stompWindup >= 0;
        if (target != null && !winding) {
            walk(level, g, target);
        }
        if (g.cooldown > 0) {
            g.cooldown--;
        }
        switch (g.kind) {
            case BIGFOOT -> stomp(level, g, targets, false);
            case LASR -> laser(level, g, targets);
            case JOLLY -> lob(level, g, targets);
            case DIAMOND -> swing(level, g, targets, false);
            case SADAN -> {
                if (g.stompWindup < 0) {
                    swing(level, g, targets, true);
                }
                if (g.windup < 0) {
                    stomp(level, g, targets, true);
                }
                if (--g.summonCooldown <= 0) {
                    callTerracottas(level, master ? 4 : 2);
                    g.summonCooldown = master ? 200 : 300;
                }
            }
        }
        if (ticks % 5 == 0) {
            nameTag(g);
        }
    }

    /** A slow walk at the nearest player, on the tomb floor, facing him. */
    private void walk(ServerLevel level, Big g, ServerPlayer target) {
        GiantBody b = g.body;
        double dx = target.getX() - b.getX();
        double dz = target.getZ() - b.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        double x = b.getX();
        double z = b.getZ();
        double reach = g.kind == Kind.LASR || g.kind == Kind.JOLLY ? 12 : 4;
        if (len > reach) {
            double speed = (g.kind == Kind.SADAN ? 0.07 : 0.05) * (master ? 1.3 : 1.0);
            x += dx / len * speed;
            z += dz / len * speed;
        }
        AABB tomb = tombBox();
        x = Math.max(tomb.minX + 2, Math.min(tomb.maxX - 2, x));
        z = Math.max(tomb.minZ + 2, Math.min(tomb.maxZ - 2, z));
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        b.snapTo(x, ground(level, x, z), z, yaw, 0f);
        b.setYHeadRot(yaw);
        b.setYBodyRot(yaw);
    }

    /** Bigfoot's leap: a ring of cloud round him, then he lands and everything inside it is thrown up. */
    private void stomp(ServerLevel level, Big g, List<ServerPlayer> targets, boolean own) {
        double radius = 9;
        int telegraph = 30;
        if (g.stompWindup < 0) {
            boolean ready = --g.stompCooldown <= 0;
            if (!ready || nearest(targets, g.body.position()) == null
                    || g.body.distanceToSqr(nearest(targets, g.body.position())) > 14 * 14) {
                return;
            }
            g.stompWindup = 0;
        }
        g.stompWindup++;
        GiantBody b = g.body;
        if (g.stompWindup < telegraph) {
            if (g.stompWindup % 3 == 0) {
                for (int i = 0; i < 28; i++) {
                    double a = Math.PI * 2 * i / 28;
                    level.sendParticles(ParticleTypes.CLOUD, b.getX() + Math.cos(a) * radius, b.getY() + 0.2,
                            b.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
                }
            }
            // Up into the leap.
            b.snapTo(b.getX(), b.getY() + (g.stompWindup < telegraph / 2 ? 0.12 : -0.12), b.getZ(), b.getYRot(), 0f);
            return;
        }
        g.stompWindup = -1;
        int next = master ? 110 : 160;
        g.stompCooldown = own ? next + 40 : next;
        b.snapTo(b.getX(), ground(level, b.getX(), b.getZ()), b.getZ(), b.getYRot(), 0f);
        level.sendParticles(ParticleTypes.EXPLOSION, b.getX(), b.getY() + 0.5, b.getZ(), 8, radius / 2, 0.3,
                radius / 2, 0);
        level.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f,
                0.5f);
        for (ServerPlayer p : targets) {
            double dx = p.getX() - b.getX();
            double dz = p.getZ() - b.getZ();
            if (dx * dx + dz * dz <= radius * radius && p.getY() - b.getY() < 3) {
                hurtPlayer(level, p, own ? sadanDamage() : Kind.BIGFOOT.attack(), dx, dz, 0.9, 0.9);
            }
        }
    }

    /** L.A.S.R.'s beam: locks on a player, a red line for a second and a half, then fires down it. */
    private void laser(ServerLevel level, Big g, List<ServerPlayer> targets) {
        int track = 20;
        int lock = 12;
        GiantBody b = g.body;
        Vec3 eye = new Vec3(b.getX(), b.getY() + 10, b.getZ());
        if (g.windup < 0) {
            ServerPlayer t = nearest(targets, b.position());
            if (g.cooldown > 0 || t == null) {
                return;
            }
            g.windup = 0;
            g.aim = t.position();
        }
        g.windup++;
        ServerPlayer t = nearest(targets, b.position());
        if (g.windup <= track && t != null) {
            // It follows him for a second, then the aim freezes: that is the moment to move.
            g.aim = t.position();
        }
        Vec3 end = beamEnd(eye, g.aim);
        if (g.windup < track + lock) {
            if (g.windup % 2 == 0) {
                DustParticleOptions red = new DustParticleOptions(g.windup <= track ? 0xFF6060 : 0xFF0000,
                        g.windup <= track ? 0.8f : 1.4f);
                beam(level, red, eye, end);
            }
            return;
        }
        g.windup = -1;
        g.cooldown = master ? 80 : 120;
        level.playSound(null, b.getX(), b.getY() + 10, b.getZ(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE,
                2f, 1.4f);
        beam(level, ParticleTypes.FLAME, eye, end);
        for (ServerPlayer p : targets) {
            if (distanceToSegment(p.position().add(0, 1, 0), eye, end) < 1.6) {
                hurtPlayer(level, p, Kind.LASR.attack(), p.getX() - b.getX(), p.getZ() - b.getZ(), 0.4,
                        0.3);
            }
        }
    }

    /** The beam goes through the aim point and on to the floor beyond it. */
    private static Vec3 beamEnd(Vec3 eye, Vec3 aim) {
        Vec3 d = aim.subtract(eye);
        if (d.y >= -0.1) {
            return aim;
        }
        double k = (eye.y - aim.y) / -d.y;
        return eye.add(d.scale(Math.max(1, k)));
    }

    private static void beam(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, Vec3 from,
                             Vec3 to) {
        Vec3 d = to.subtract(from);
        int steps = (int) Math.max(4, d.length() * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(d.scale((double) i / steps));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** The Jolly Pink Giant's bombs: a pink ring at each player's feet (and one beside), then a blast. */
    private void lob(ServerLevel level, Big g, List<ServerPlayer> targets) {
        if (g.cooldown > 0 || targets.isEmpty()) {
            return;
        }
        g.cooldown = master ? 90 : 140;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (ServerPlayer p : targets) {
            bombs.add(new Bomb(p.position(), ticks + 30));
            double a = rnd.nextDouble() * Math.PI * 2;
            bombs.add(new Bomb(p.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4), ticks + 38));
        }
        g.body.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, g.body.getX(), g.body.getY() + 8, g.body.getZ(), SoundEvents.SNOWBALL_THROW,
                SoundSource.HOSTILE, 2f, 0.5f);
    }

    private void tickBombs(ServerLevel level) {
        if (bombs.isEmpty()) {
            return;
        }
        double radius = 3;
        DustParticleOptions pink = new DustParticleOptions(0xFF7FD0, 1.3f);
        List<Bomb> done = new ArrayList<>();
        for (Bomb b : bombs) {
            if (ticks < b.landsAt()) {
                if (ticks % 3 == 0) {
                    for (int i = 0; i < 16; i++) {
                        double a = Math.PI * 2 * i / 16;
                        level.sendParticles(pink, b.at().x + Math.cos(a) * radius, b.at().y + 0.1,
                                b.at().z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            done.add(b);
            level.sendParticles(ParticleTypes.EXPLOSION, b.at().x, b.at().y + 0.5, b.at().z, 2, 0.5, 0.3, 0.5, 0);
            level.playSound(null, b.at().x, b.at().y, b.at().z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                    1f, 1.3f);
            for (ServerPlayer p : playersInTomb(level)) {
                double dx = p.getX() - b.at().x;
                double dz = p.getZ() - b.at().z;
                if (dx * dx + dz * dz <= radius * radius && Math.abs(p.getY() - b.at().y) < 3) {
                    hurtPlayer(level, p, Kind.JOLLY.attack(), dx, dz, 0.7, 0.5);
                }
            }
        }
        bombs.removeAll(done);
    }

    /** The Diamond Giant's (and Sadan's) swing: a second's wind-up when you are close, then a very hard hit. */
    private void swing(ServerLevel level, Big g, List<ServerPlayer> targets, boolean own) {
        double reach = own ? 7 : 6.5;
        int telegraph = 20;
        GiantBody b = g.body;
        if (g.windup < 0) {
            if (g.cooldown > 0) {
                return;
            }
            ServerPlayer t = nearest(targets, b.position());
            if (t == null || horizontal(t, b) > reach) {
                return;
            }
            g.windup = 0;
        }
        g.windup++;
        if (g.windup < telegraph) {
            if (g.windup % 2 == 0) {
                level.sendParticles(ParticleTypes.CRIT, b.getX(), b.getY() + 7, b.getZ(), 6, 1.5, 1.5, 1.5, 0.1);
            }
            if (g.windup == 1) {
                level.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE,
                        1.5f, 0.6f);
            }
            return;
        }
        g.windup = -1;
        g.cooldown = own ? (master ? 50 : 70) : (master ? 40 : 60);
        b.swing(InteractionHand.MAIN_HAND);
        level.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE,
                2f, 0.5f);
        for (ServerPlayer p : targets) {
            if (horizontal(p, b) <= reach + 0.5 && p.getY() - b.getY() < 6) {
                hurtPlayer(level, p, own ? sadanDamage() : Kind.DIAMOND.attack(), p.getX() - b.getX(),
                        p.getZ() - b.getZ(), 1.4, 0.5);
            }
        }
    }

    private static double horizontal(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void nameTag(Big g) {
        Component tag = Component.literal(g.kind.display + " ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(SimBoss.shortHealth(g.health.hp)).withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("❤").withStyle(ChatFormatting.RED)));
        g.body.setCustomName(tag);
    }

    // ------------------------------------------------------------------ helpers

    private AABB tombBox() {
        BlockPos lo = arena.toWorld(TOMB_MIN);
        BlockPos hi = arena.toWorld(TOMB_MAX);
        return new AABB(lo.getX(), lo.getY() - 2, lo.getZ(), hi.getX() + 1, hi.getY() + 16, hi.getZ() + 1);
    }

    /**
     * Real players on the tomb floor. The tomb is a rectangle longer than the arena's ring, so this is used
     * instead of {@link SimBoss#playersInRing}, which would miss its far ends.
     */
    private List<ServerPlayer> playersInTomb(ServerLevel level) {
        AABB box = tombBox();
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : SimBoss.players(level.getServer())) {
            if (box.contains(p.position()) && !p.isSpectator() && !p.isCreative()) {
                out.add(p);
            }
        }
        return out;
    }

    /** The top of the floor under (x, z), searched down from a few blocks above the tomb floor. */
    private double ground(ServerLevel level, double x, double z) {
        BlockPos c = arena.toWorld(arena.centre);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos((int) Math.floor(x), c.getY() + 6,
                (int) Math.floor(z));
        for (int i = 0; i < 12; i++, p.move(Direction.DOWN)) {
            VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
            if (!shape.isEmpty()) {
                return p.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return c.getY();
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

    /** Damage plus knockback: {@code damage} is Hypixel damage before Defense ({@link SimVitals#dealHypixel}). */
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

    private Big big(Entity e) {
        if (sadan != null && sadan.body == e) {
            return sadan;
        }
        for (Big g : giants) {
            if (g.body == e) {
                return g;
            }
        }
        return null;
    }

    @Override
    boolean isBoss(Entity entity) {
        return entity instanceof GiantBody && big(entity) != null;
    }

    @Override
    boolean hit(ServerLevel level, LivingEntity body, float amount) {
        Big g = big(body);
        if (g == null || g.dead) {
            return false;
        }
        boolean landed = g.health.hit(amount);
        if (landed) {
            nameTag(g);
        }
        return landed;
    }

    @Override
    boolean defeated() {
        return phase == Phase.DEAD;
    }

    @Override
    void discard(ServerLevel level) {
        for (Slot s : slots) {
            if (s.mob != null) {
                s.mob.discard();
                s.mob = null;
            }
        }
        for (Big g : giants) {
            g.body.discard();
        }
        giants.clear();
        if (sadan != null) {
            sadan.body.discard();
            sadan = null;
        }
        bombs.clear();
    }

    @Override
    Component barName() {
        String part = switch (phase) {
            case TERRACOTTAS, GIANTS_WAKING -> " - Terracottas";
            case GIANTS -> " - Giants";
            default -> "";
        };
        return Component.literal("Sadan" + part).withStyle(ChatFormatting.RED);
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return switch (phase) {
            case TERRACOTTAS, GIANTS_WAKING -> BossEvent.BossBarColor.YELLOW;
            case GIANTS -> BossEvent.BossBarColor.PURPLE;
            default -> BossEvent.BossBarColor.RED;
        };
    }

    @Override
    float barProgress() {
        return switch (phase) {
            case TERRACOTTAS -> 1f - Math.min(1f, (float) phaseTicks / terracottaTicks());
            case GIANTS_WAKING -> 0f;
            case GIANTS -> {
                double hp = 0;
                double max = 0;
                for (Big g : giants) {
                    hp += g.health.hp;
                    max += g.health.max;
                }
                yield max <= 0 ? 1f : (float) (hp / max);
            }
            case SADAN_RISING -> 1f;
            case SADAN -> sadan == null ? 1f : sadan.health.fraction();
            default -> 0f;
        };
    }

    /** A Giant's body: no AI (the fight moves it), never despawns. */
    private static final class GiantBody extends Giant {
        GiantBody(Level level) {
            super(EntityTypes.GIANT, level);
        }

        @Override
        public void checkDespawn() {
        }
    }

    /** A Terracotta: a chasing zombie that never despawns, never burns and drops nothing. */
    private static final class Terracotta extends Zombie {
        Terracotta(Level level, float damage) {
            super(level);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
            setHealth(40);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
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
        protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        }

        @Override
        public boolean shouldDropExperience() {
            return false;
        }
    }
}
