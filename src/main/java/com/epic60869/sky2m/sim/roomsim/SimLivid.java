package com.epic60869.sky2m.sim.roomsim;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 5's boss, Livid, in her ring.
 *
 * <p>The fight as it goes on Hypixel, tuned for practice rather than copied number for number:
 * <ul>
 * <li><b>The opening.</b> Her first line (Hypixel's exact words: Boss Phases, the Livid Invulnerability HUD and
 * Rag Axe all start on it), then 340 ticks in which nothing can hurt her - the same 340 the HUD counts down.</li>
 * <li><b>The split.</b> A few seconds before she can be hit, she becomes nine Livids, one of each colour
 * (Vendetta, Crossed, Arcade, Smile, Hockey, Doctor, Frog, Scream, Purple), and the wool block above the ring
 * and the stained glass round it, red until then, turn the REAL one's colour. In Master Mode everyone is blinded
 * for five seconds as she splits.</li>
 * <li><b>Only the real one takes damage.</b> Hitting a fake does nothing to her, and the fake you hit strikes
 * straight back (harder in Master Mode). Every Livid chases and swings at you.</li>
 * <li><b>Livid Dagger.</b> Every few seconds the real one marks the ground under a player with a ring of crits
 * and her knife lands there a moment later - step out of it. Faster in Master Mode.</li>
 * <li><b>Shadow shuffle.</b> Now and then all nine vanish in smoke and come back somewhere else in the ring, so
 * you have to find her again. More often in Master Mode.</li>
 * </ul>
 *
 * <h2>What the mod's Livid features read, reproduced</h2>
 * The Livids are what Hypixel's are: NPC players (fake {@link ServerPlayer}s that are never in the player list,
 * with a version-3 UUID rather than a real player's version 4) named "Hockey Livid", "Smile Livid" and so on,
 * each under an armour stand reading "﴾ Livid 7M❤ ﴿" in its colour. That is exactly what Livid Finder sorts
 * by name, and what its Hide Wrong Livids hides by the stand's colour. The wool is the block Livid Finder
 * watches ({@code (6, 109, 43)} in the arena's own coordinates, red until the split); in the sim it sits at
 * {@link #woolCue()} instead, because the arena is pasted a thousand blocks away from Hypixel's coordinates.
 *
 * <p>Her death line is Hypixel's ("Impossible! How did you figure out which one I was?!", said by the real
 * Livid by name); the lines in between are the sim's own.
 */
public final class SimLivid extends SimBoss.Fight {

    /**
     * Livid's health and base damage, the real one's and every clone's alike: 7,000,000 and 9,360 on F5,
     * 600,000,000 and 216,000 on M5 (https://hypixelskyblock.minecraft.wiki/w/Livid, Variants table; the damage is
     * the 2026 value after the cut from 10,400 / 240,000). Every Livid's swing deals it, before the player's
     * Defense. The wiki gives no number for the dagger or for the sim's punishment for hitting a clone, so those
     * deal it too (APPROXIMATE).
     */
    private static double lividDamage() {
        return SimHealth.byMode(9_360, 216_000);
    }

    /** Hypixel's: 340 ticks from her first line until she can be damaged ({@code LividTimer}, Odin). */
    private static final int INVULNERABLE_TICKS = 340;
    /** When she splits and the wool turns, a few seconds before she can be hit. */
    private static final int SPLIT_TICK = 260;
    private static final int BLIND_TICKS = 100;
    private static final int DAGGER_TELEGRAPH = 18;
    private static final double DAGGER_RADIUS = 1.8;
    /** How far from the middle of the ring the Livids walk, in blocks: the raised inner floor. */
    private static final double ROAM_RADIUS = 17;

    /** The block Livid Finder watches, in the arena's own (Hypixel's) coordinates. */
    private static final int[] WOOL = {6, 109, 43};
    /** The box of ceiling glass round it that changes with the wool, in the arena's coordinates. */
    private static final int[] GLASS_MIN = {-6, 107, 30};
    private static final int[] GLASS_MAX = {16, 110, 54};

    /** Where the sim's Livid wool is while a Livid fight is built, or null. Read by Livid Finder on the client. */
    private static volatile BlockPos woolCue;

    /**
     * The nine Livids, each with the colour Livid Finder knows it by (SkyHanni's LividSolver names) and the dye
     * of the wool that colour reads back as (Livid Finder's {@code woolColor}).
     */
    private enum Colour {
        VENDETTA("Vendetta", ChatFormatting.WHITE, DyeColor.WHITE),
        CROSSED("Crossed", ChatFormatting.LIGHT_PURPLE, DyeColor.MAGENTA),
        ARCADE("Arcade", ChatFormatting.YELLOW, DyeColor.YELLOW),
        SMILE("Smile", ChatFormatting.GREEN, DyeColor.LIME),
        HOCKEY("Hockey", ChatFormatting.RED, DyeColor.RED),
        DOCTOR("Doctor", ChatFormatting.GRAY, DyeColor.GRAY),
        FROG("Frog", ChatFormatting.DARK_GREEN, DyeColor.GREEN),
        SCREAM("Scream", ChatFormatting.BLUE, DyeColor.BLUE),
        PURPLE("Purple", ChatFormatting.DARK_PURPLE, DyeColor.PURPLE);

        final String display;
        final ChatFormatting chat;
        final DyeColor dye;

        Colour(String display, ChatFormatting chat, DyeColor dye) {
            this.display = display;
            this.chat = chat;
            this.dye = dye;
        }

        String speaker() {
            return display + " Livid";
        }
    }

    private enum Phase { INTRO, FIGHT, DEAD }

    private Phase phase = Phase.INTRO;
    private final boolean master = SimState.floorLabel().toUpperCase(java.util.Locale.ROOT).startsWith("M");
    private final SimBoss.Health health = new SimBoss.Health(lividHealth());
    private final Colour real = Colour.values()[ThreadLocalRandom.current().nextInt(Colour.values().length)];
    private final List<Livid> livids = new ArrayList<>();
    private final List<BlockPos> glass = new ArrayList<>();
    private final List<Dagger> daggers = new ArrayList<>();
    /** When each player last got hit back for hitting a fake, so a Hyperion through all nine is one punishment. */
    private final Map<UUID, Integer> punished = new HashMap<>();
    private int ticks;
    private int nextDagger;
    private int nextShuffle;
    private int spawnCounter;

    /** One Livid: its NPC body and the name stand over it. */
    private static final class Livid {
        final Colour colour;
        final LividBody body;
        ArmorStand stand;
        int swingCooldown;
        /** Where round its target it closes in to, so nine Livids do not stand inside each other. */
        final double approachAngle;

        Livid(Colour colour, LividBody body, double approachAngle) {
            this.colour = colour;
            this.body = body;
            this.approachAngle = approachAngle;
        }
    }

    private record Dagger(Vec3 at, int landsAt) {
    }

    SimLivid(SimBossArena arena) {
        super(arena);
    }

    /**
     * The sim's Livid wool (the block Livid Finder reads at {@code (6, 109, 43)} on Hypixel) while a Livid room is
     * built, else null. Safe from the client thread.
     */
    public static BlockPos woolCue() {
        return woolCue;
    }

    private static double lividHealth() {
        return SimHealth.byMode(7_000_000, 600_000_000);
    }

    @Override
    String name() {
        return "Livid";
    }

    @Override
    void start(ServerLevel level) {
        // Hypixel's first line, word for word: Boss Phases, the Livid Invulnerability HUD, Rag Axe and the
        // dungeon's boss detection (which Livid Finder waits on) all start on it.
        SimBoss.say(level.getServer(), "Livid",
                "Welcome, you've arrived right on time. I am Livid, the Master of Shadows.");
        BlockPos wool = arena.toWorld(WOOL);
        woolCue = wool;
        // Red until the split, as on Hypixel (Livid Finder assumes red on the way in).
        level.setBlock(wool, Blocks.WOOL.pick(DyeColor.RED).defaultBlockState(), 3);
        findGlass(level);
        phase = Phase.INTRO;
        ticks = 0;
    }

    /** The red ceiling glass round the wool, which turns with it. */
    private void findGlass(ServerLevel level) {
        glass.clear();
        BlockPos lo = arena.toWorld(GLASS_MIN);
        BlockPos hi = arena.toWorld(GLASS_MAX);
        BlockState red = Blocks.STAINED_GLASS.pick(DyeColor.RED).defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            if (level.getBlockState(p) == red) {
                glass.add(p.immutable());
            }
        }
    }

    @Override
    void tick(ServerLevel level, int t) {
        ticks = t;
        switch (phase) {
            case INTRO -> intro(level, t);
            case FIGHT -> fight(level, t);
            default -> {
            }
        }
        tickDaggers(level);
        for (Livid l : livids) {
            moveStand(l);
        }
    }

    private void intro(ServerLevel level, int t) {
        if (t == 80) {
            SimBoss.say(level.getServer(), "Livid", "I respect you for making it this far, but I'll be your undoing.");
        } else if (t == 180) {
            SimBoss.say(level.getServer(), "Livid", "My shadows are everywhere, THEY WILL FIND YOU!!");
        } else if (t == SPLIT_TICK) {
            split(level);
        } else if (t >= INVULNERABLE_TICKS) {
            phase = Phase.FIGHT;
            nextDagger = t + 60;
            nextShuffle = t + shuffleInterval();
        }
    }

    /** Nine Livids where there was one, and the wool and glass turn the real one's colour. */
    private void split(ServerLevel level) {
        // The cue first, so Livid Finder knows the colour the moment the bodies show up.
        BlockPos wool = arena.toWorld(WOOL);
        level.setBlock(wool, Blocks.WOOL.pick(real.dye).defaultBlockState(), 3);
        BlockState tinted = Blocks.STAINED_GLASS.pick(real.dye).defaultBlockState();
        for (BlockPos p : glass) {
            level.setBlock(p, tinted, 3);
        }
        Colour[] all = Colour.values();
        BlockPos c = arena.toWorld(arena.centre);
        double offset = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        for (int i = 0; i < all.length; i++) {
            double a = offset + Math.PI * 2 * i / all.length;
            double x = c.getX() + 0.5 + Math.cos(a) * 8;
            double z = c.getZ() + 0.5 + Math.sin(a) * 8;
            spawn(level, all[i], x, z, Math.PI * 2 * i / all.length);
        }
        level.playSound(null, c.getX(), c.getY(), c.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE,
                2f, 0.8f);
        if (master) {
            // Master Mode's blindness as she splits; Livid Finder's box shows through it.
            for (ServerPlayer p : SimBoss.playersInRing(level)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0, false, false));
            }
        }
    }

    private void spawn(ServerLevel level, Colour colour, double x, double z, double approach) {
        UUID id = UUID.nameUUIDFromBytes(("sky2m-livid-" + colour.display + "-" + spawnCounter++)
                .getBytes(StandardCharsets.UTF_8));
        LividBody body = new LividBody(level, new GameProfile(id, colour.speaker()));
        double y = ground(level, x, z);
        body.snapTo(x, y, z, 0f, 0f);
        body.getAbilities().invulnerable = false;
        int rgb = colour.dye.getTextureDiffuseColor() & 0xFFFFFF;
        body.setItemSlot(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, rgb));
        body.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, rgb));
        body.setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, rgb));
        body.setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, rgb));
        body.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        // A client only builds a player body for a uuid it has player info for, so the info goes first (unlisted:
        // ADD_PLAYER alone leaves it out of the tab list, as Hypixel's NPCs are).
        level.getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), List.of(body)));
        level.addNewPlayer(body);
        Livid l = new Livid(colour, body, approach);
        l.stand = nameStand(level, body);
        livids.add(l);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z, 12, 0.3, 0.6, 0.3, 0.02);
        writeName(l);
    }

    private static ItemStack dyed(Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb));
        return stack;
    }

    /** The name stand: a marker, so it has no hitbox to catch hits meant for the Livid (as SimMobs' tags). */
    private static ArmorStand nameStand(ServerLevel level, LivingEntity body) {
        ArmorStand tag = new ArmorStand(level, body.getX(), body.getY() + body.getBbHeight() + 0.3, body.getZ());
        tag.setInvisible(true);
        tag.setNoGravity(true);
        tag.setNoBasePlate(true);
        tag.setInvulnerable(true);
        byte flags = tag.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS);
        tag.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte) (flags | ArmorStand.CLIENT_FLAG_MARKER));
        tag.setCustomNameVisible(true);
        level.addFreshEntity(tag);
        return tag;
    }

    /**
     * Hypixel's Livid stand, "§c﴾ §c§lLivid§r§r §a7M§c❤ §c﴿", in the Livid's own colour. Every stand shows the
     * real one's health, so the tag alone does not give her away.
     */
    private void writeName(Livid l) {
        if (l.stand == null) {
            return;
        }
        ChatFormatting c = l.colour.chat;
        MutableComponent name = Component.literal("﴾ ").withStyle(c)
                .append(Component.literal("Livid").withStyle(c, ChatFormatting.BOLD))
                .append(Component.literal(" "))
                .append(Component.literal(SimBoss.shortHealth(health.hp)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("❤").withStyle(ChatFormatting.RED))
                .append(Component.literal(" ﴿").withStyle(c));
        l.stand.setCustomName(name);
    }

    private static void moveStand(Livid l) {
        if (l.stand != null && !l.stand.isRemoved()) {
            l.stand.setPos(l.body.getX(), l.body.getY() + l.body.getBbHeight() + 0.3, l.body.getZ());
        }
    }

    private void fight(ServerLevel level, int t) {
        List<ServerPlayer> targets = SimBoss.playersInRing(level);
        for (Livid l : livids) {
            chase(level, l, targets);
        }
        if (t >= nextDagger) {
            dagger(level, targets);
            nextDagger = t + (master ? 90 : 140);
        }
        if (t >= nextShuffle) {
            shuffle(level);
            nextShuffle = t + shuffleInterval();
        }
        if (t % 10 == 0) {
            for (Livid l : livids) {
                writeName(l);
            }
        }
        if (health.hp <= 0) {
            die(level);
        }
    }

    private int shuffleInterval() {
        return master ? 240 : 400;
    }

    /** Walks a Livid at its target and swings when close. Every Livid does this, the real one included. */
    private void chase(ServerLevel level, Livid l, List<ServerPlayer> targets) {
        LividBody b = l.body;
        if (l.swingCooldown > 0) {
            l.swingCooldown--;
        }
        ServerPlayer target = nearest(targets, b.position());
        if (target == null) {
            return;
        }
        BlockPos c = arena.toWorld(arena.centre);
        double gx = target.getX() + Math.cos(l.approachAngle) * 1.6;
        double gz = target.getZ() + Math.sin(l.approachAngle) * 1.6;
        double dx = gx - b.getX();
        double dz = gz - b.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        double speed = master ? 0.26 : 0.2;
        double x = b.getX();
        double z = b.getZ();
        if (len > 0.3) {
            double step = Math.min(speed, len);
            x += dx / len * step;
            z += dz / len * step;
        }
        // Kept on the raised inner floor of the ring.
        double ox = x - (c.getX() + 0.5);
        double oz = z - (c.getZ() + 0.5);
        double r = Math.sqrt(ox * ox + oz * oz);
        if (r > ROAM_RADIUS) {
            x = c.getX() + 0.5 + ox / r * ROAM_RADIUS;
            z = c.getZ() + 0.5 + oz / r * ROAM_RADIUS;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(target.getZ() - z, target.getX() - x)) - 90);
        b.snapTo(x, ground(level, x, z), z, yaw, 0f);
        b.setYHeadRot(yaw);
        b.setYBodyRot(yaw);
        if (l.swingCooldown == 0 && b.distanceToSqr(target) < 2.8 * 2.8) {
            b.swing(InteractionHand.MAIN_HAND);
            hurtPlayer(level, target, lividDamage(), target.getX() - b.getX(),
                    target.getZ() - b.getZ(), 0.4, 0.25);
            l.swingCooldown = master ? 18 : 25;
        }
    }

    /** The real Livid marks a spot under a player; her knife lands there a moment later. */
    private void dagger(ServerLevel level, List<ServerPlayer> targets) {
        if (targets.isEmpty()) {
            return;
        }
        Livid her = realLivid();
        ServerPlayer p = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
        daggers.add(new Dagger(p.position(), ticks + DAGGER_TELEGRAPH));
        if (her != null) {
            her.body.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, her.body.getX(), her.body.getY(), her.body.getZ(), SoundEvents.TRIDENT_THROW,
                    SoundSource.HOSTILE, 1f, 1.4f);
        }
    }

    private void tickDaggers(ServerLevel level) {
        if (daggers.isEmpty()) {
            return;
        }
        DustParticleOptions mark = new DustParticleOptions(real.dye.getTextureDiffuseColor() & 0xFFFFFF, 1.1f);
        List<Dagger> landed = new ArrayList<>();
        for (Dagger d : daggers) {
            if (ticks < d.landsAt()) {
                if (ticks % 3 == 0) {
                    for (int i = 0; i < 14; i++) {
                        double a = Math.PI * 2 * i / 14;
                        level.sendParticles(mark, d.at().x + Math.cos(a) * DAGGER_RADIUS, d.at().y + 0.1,
                                d.at().z + Math.sin(a) * DAGGER_RADIUS, 1, 0, 0, 0, 0);
                    }
                }
                continue;
            }
            landed.add(d);
            level.sendParticles(ParticleTypes.CRIT, d.at().x, d.at().y + 0.3, d.at().z, 25, 0.6, 0.2, 0.6, 0.3);
            level.playSound(null, d.at().x, d.at().y, d.at().z, SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1f,
                    1.2f);
            for (ServerPlayer p : SimBoss.playersInRing(level)) {
                double dx = p.getX() - d.at().x;
                double dz = p.getZ() - d.at().z;
                if (dx * dx + dz * dz <= DAGGER_RADIUS * DAGGER_RADIUS && Math.abs(p.getY() - d.at().y) < 3) {
                    hurtPlayer(level, p, lividDamage(), dx, dz, 0.3, 0.3);
                }
            }
        }
        daggers.removeAll(landed);
    }

    /** All nine vanish in smoke and come back somewhere else in the ring. */
    private void shuffle(ServerLevel level) {
        BlockPos c = arena.toWorld(arena.centre);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Livid l : livids) {
            LividBody b = l.body;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, b.getX(), b.getY() + 1, b.getZ(), 15, 0.3, 0.7, 0.3,
                    0.02);
            double a = rnd.nextDouble() * Math.PI * 2;
            double d = 5 + rnd.nextDouble() * (ROAM_RADIUS - 6);
            double x = c.getX() + 0.5 + Math.cos(a) * d;
            double z = c.getZ() + 0.5 + Math.sin(a) * d;
            b.snapTo(x, ground(level, x, z), z, b.getYRot(), 0f);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, b.getY() + 1, z, 15, 0.3, 0.7, 0.3, 0.02);
        }
        level.playSound(null, c.getX(), c.getY(), c.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE,
                2f, 1.2f);
    }

    private void die(ServerLevel level) {
        // Hypixel's last line, said by the real Livid by name ("[BOSS] Hockey Livid: Impossible! ...").
        SimBoss.say(level.getServer(), real.speaker(), "Impossible! How did you figure out which one I was?!");
        Livid her = realLivid();
        if (her != null) {
            level.sendParticles(ParticleTypes.EXPLOSION, her.body.getX(), her.body.getY() + 1, her.body.getZ(), 3,
                    0.5, 0.5, 0.5, 0);
        }
        removeLivids(level);
        daggers.clear();
        phase = Phase.DEAD;
    }

    private Livid realLivid() {
        for (Livid l : livids) {
            if (l.colour == real) {
                return l;
            }
        }
        return null;
    }

    private Livid livid(Entity e) {
        for (Livid l : livids) {
            if (l.body == e) {
                return l;
            }
        }
        return null;
    }

    private void removeLivids(ServerLevel level) {
        List<UUID> ids = new ArrayList<>();
        for (Livid l : livids) {
            ids.add(l.body.getUUID());
            if (!l.body.isRemoved()) {
                level.removePlayerImmediately(l.body, Entity.RemovalReason.DISCARDED);
            }
            if (l.stand != null) {
                l.stand.discard();
            }
        }
        livids.clear();
        if (!ids.isEmpty()) {
            level.getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoRemovePacket(ids));
        }
    }

    /** The top of the floor under (x, z), searched down from a few blocks above the ring. */
    private double ground(ServerLevel level, double x, double z) {
        BlockPos c = arena.toWorld(arena.centre);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos((int) Math.floor(x), c.getY() + 3,
                (int) Math.floor(z));
        for (int i = 0; i < 10; i++, p.move(Direction.DOWN)) {
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
            return;
        }
        p.setDeltaMovement(dx / len * push, lift, dz / len * push);
        p.hurtMarked = true;
    }

    @Override
    boolean isBoss(Entity entity) {
        return entity instanceof LividBody && livid(entity) != null;
    }

    @Override
    boolean hit(ServerLevel level, LivingEntity body, float amount) {
        Livid l = livid(body);
        if (l == null || phase != Phase.FIGHT) {
            return false;
        }
        if (l.colour == real) {
            boolean landed = health.hit(amount);
            if (landed && ticks % 2 == 0) {
                for (Livid other : livids) {
                    writeName(other);
                }
            }
            return landed;
        }
        punish(level, l);
        return false;
    }

    /** Hitting a fake: it does nothing to Livid, and the fake strikes straight back. Once a second per player. */
    private void punish(ServerLevel level, Livid fake) {
        ServerPlayer p = nearest(SimBoss.playersInRing(level), fake.body.position());
        if (p == null) {
            return;
        }
        Integer last = punished.get(p.getUUID());
        if (last != null && ticks - last < 20) {
            return;
        }
        punished.put(p.getUUID(), ticks);
        fake.body.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, fake.body.getX(), fake.body.getY() + 2.2,
                fake.body.getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.playSound(null, fake.body.getX(), fake.body.getY(), fake.body.getZ(), SoundEvents.SHIELD_BLOCK,
                SoundSource.HOSTILE, 1f, 0.7f);
        hurtPlayer(level, p, lividDamage(), p.getX() - fake.body.getX(), p.getZ() - fake.body.getZ(),
                0.8, 0.35);
    }

    @Override
    boolean defeated() {
        return phase == Phase.DEAD;
    }

    @Override
    void discard(ServerLevel level) {
        removeLivids(level);
        daggers.clear();
        woolCue = null;
    }

    @Override
    Component barName() {
        return Component.literal("Livid").withStyle(ChatFormatting.RED);
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return phase == Phase.INTRO ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.RED;
    }

    @Override
    float barProgress() {
        return phase == Phase.INTRO ? Math.min(1f, (float) ticks / INVULNERABLE_TICKS) : health.fraction();
    }

    /**
     * A Livid's body: an NPC player, like Hypixel's. Fabric's {@link FakePlayer} refuses all damage and cannot be
     * hit by players; this one can, so the hit reaches {@link SimBoss#onDamage} (which refuses the vanilla hit and
     * puts it on the pool). It never ticks (FakePlayer), so it only moves where the fight puts it.
     */
    private static final class LividBody extends FakePlayer {
        LividBody(ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
            return false;
        }

        @Override
        public boolean canHarmPlayer(Player other) {
            return true;
        }
    }
}
