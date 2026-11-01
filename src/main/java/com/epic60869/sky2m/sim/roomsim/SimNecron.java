package com.epic60869.sky2m.sim.roomsim;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floor 7's boss room, phase by phase: Maxor, Storm, Goldor (the terminals), Necron, and on Master Mode the
 * Wither King's dragons ({@link SimNecronDragons}).
 *
 * <p>The room is the real one at Hypixel's own coordinates ({@link SimBossArena#isNative}), so the mod's F7 features
 * read the sim the way they read Hypixel: the lines below that the mod parses are Hypixel's exact wording (Boss
 * Phases, F7 Extras, Dungeon Features, Rag Axe, Wither Dragons), the crystal pads, terminals, levers, devices,
 * gates and dragon spawn boxes are where those features look for them, and the phase-by-height works because
 * every phase is fought at its own level of the room. Lines nothing reads are the sim's own words.
 *
 * <h2>The phases</h2>
 * <ul>
 * <li><b>P1 Maxor</b> (y 221). Two Energy Crystals spawn on the floor; pick one up (walk into it or click it, it
 * goes to the last hotbar slot as Hypixel's does) and walk it to one of the two pads by the walls. With both pads
 * lit the Energy Laser in the middle charges and fires: if Maxor is over it he is stunned and can be hurt for a
 * few seconds, otherwise the crystals are wasted. The crystals come back 34 ticks after each laser. Two stuns at
 * least: a stun can only take him down to half. He chases the closest player and drops blasts on everyone.</li>
 * <li><b>P2 Storm</b> (y 165). Storm can only be hurt after a pillar crushes him: stand on one of the four
 * coloured pads to arm its pillar, and it drops when Storm floats under it. Two crushes on F7, three on M7. He
 * calls lightning on everyone, and at 60% (and on M7 again at 30%) the big storm: a countdown title, a strike on
 * everyone off the pads at each number.</li>
 * <li><b>P3 Goldor</b> (y 107-135). The four sections with their terminals, levers and devices
 * ({@link SimTerms#startInArena}: the drill's boards and devices, at the real room's spots), each gate opened
 * when its section is done, then "The Core entrance is opening!" and Goldor fought in the core. Until then he
 * walks the current section, hits hard up close and cannot be hurt, and every 60 ticks from Storm's death (the
 * timer Boss Phases shows) his death tick hits everyone not in the current section or the core.</li>
 * <li><b>P4 Necron</b> (y 64). On the platforms over the lava (which throws you back up, as on Hypixel): he
 * circles, sweeps a flamethrower along a player, charges at one, and on M7 rains fireballs too.</li>
 * <li><b>P5 the Wither King</b> (M7 only, y 5): see {@link SimNecronDragons}.</li>
 * </ul>
 *
 * <p>Between phases the players are moved down to the next level (Hypixel drops them through the floor), and death
 * sends them to the start of the phase they are in.
 *
 * <h2>Health and damage</h2>
 * Hypixel's, before the player's Defense unless marked true damage:
 * <ul>
 * <li>Maxor 100,000,000 on F7 / 800,000,000 on M7 (https://hypixelskyblock.minecraft.wiki/w/Maxor), Storm
 * 400,000,000 / 1,000,000,000 (https://hypixelskyblock.minecraft.wiki/w/Storm) and Goldor 750,000,000 /
 * 1,200,000,000 (https://hypixelskyblock.minecraft.wiki/w/Goldor). Their pages give one "min-max" range; the low end
 * is taken as F7 and the high as M7, the wiki's convention (its damage changelogs confirm it for damage). Necron
 * 1,000,000,000 / 1,400,000,000, stated per floor (https://hypixelskyblock.minecraft.wiki/w/Necron).</li>
 * <li>Base damage 7,200 on F7 and 720,000 on M7 for Maxor, Storm and Goldor; Necron's M7 base is 300,000 (the 2026
 * values). Their touch, their blast volleys, Necron's charge and his M7 fireballs deal it - the wiki gives those no
 * number of their own (APPROXIMATE as per-attack numbers).</li>
 * <li>Storm's big storm: each strike is 75% of the player's max health, true damage (Storm's page, for F7; the
 * sim uses it on M7 too).</li>
 * <li>Necron's beam (the sim's flamethrower) is 25% of the player's max health, true damage (Necron's page), once
 * a second while you stand in it.</li>
 * <li>Goldor's death tick has no number on the wiki: the sim's is half the player's max health, true damage
 * (APPROXIMATE).</li>
 * <li>The lava does no damage at all in the sim, by request; Hypixel's takes 20% of max health per bounce (the
 * Storm and Goldor pages).</li>
 * </ul>
 */
final class SimNecron extends SimBoss.Fight {

    /** On everything the fight spawns: crystals, relics, the dragons' collars. */
    static final String TAG = "sky2m_simnecron";

    // ---------------------------------------------------------------------------------------------- numbers

    /** The withers' health, F7 then M7 (see the class comment for the sources). */
    private static final double[] MAXOR_HEALTH = {100_000_000, 800_000_000};
    private static final double[] STORM_HEALTH = {400_000_000, 1_000_000_000};
    private static final double[] GOLDOR_HEALTH = {750_000_000, 1_200_000_000};
    private static final double[] NECRON_HEALTH = {1_000_000_000, 1_400_000_000};
    /** Maxor's, Storm's and Goldor's base damage, F7 then M7. */
    private static final double[] WITHER_DAMAGE = {7_200, 720_000};
    /** Necron's base damage: his M7 number was cut to 300,000 in 2026. */
    private static final double[] NECRON_DAMAGE = {7_200, 300_000};
    /** Storm's big storm strike, Necron's beam and Goldor's death tick: shares of max health, true damage. */
    private static final double STORM_STRIKE_SHARE = 0.75;
    private static final double NECRON_BEAM_SHARE = 0.25;
    private static final double DEATH_TICK_SHARE = 0.5;
    /** How long a dead wither's empty bar stays up before the next phase (F7 Extras' "Dead!" titles read it). */
    private static final int DEAD_HOLD = 40;

    // ---------------------------------------------------------------------------------------------- P1

    /** The Energy Laser: the beacon in the middle of Maxor's room. */
    private static final Vec3 LASER = new Vec3(73.5, 221, 73.5);
    /** How close (horizontally) Maxor has to be to the laser to be caught by it. APPROXIMATE. */
    private static final double LASER_REACH = 8;
    /** The two crystal pads (the pressure plates on the pillars): a placed crystal stands at y 224, as F7 Extras expects. */
    private static final int[][] PADS = {{52, 224, 41}, {94, 224, 41}};
    /** Where the two Energy Crystals appear, on the floor of each wing (APPROXIMATE spots). */
    private static final int[][] CRYSTAL_SPOTS = {{42, 60}, {104, 60}};
    /** Hypixel's crystal respawn after a laser (F7 Extras' timer). */
    private static final int CRYSTAL_RESPAWN_TICKS = 34;
    private static final int LASER_CHARGE_TICKS = 50;

    // ---------------------------------------------------------------------------------------------- P2

    private static final Vec3 P2_LANDING = new Vec3(73.5, 165, 53.5);
    /** Storm floats this high: the pads' top is y 168 and the pillars hang from y 175. */
    private static final double STORM_Y = 170.5;
    /**
     * The four pads, {pillar centre x, z, pad x0, x1, z0, z1, colour}: green, yellow, red and blue terracotta at
     * y 168 with a polished diorite pillar over the middle (the fan copy of the room has the west pillars two
     * blocks off the pads' centre; the footprint is read from the blocks, not assumed).
     */
    private static final int[][] STORM_PADS = {
        {46, 41, 38, 58, 32, 46, 0x55FF55},
        {46, 65, 38, 58, 60, 74, 0xFFFF55},
        {100, 41, 90, 110, 32, 46, 0xFF5555},
        {100, 65, 90, 110, 60, 74, 0x5555FF},
    };
    private static final int PAD_TOP = 168;
    private static final int PILLAR_COOLDOWN = 160;

    // ---------------------------------------------------------------------------------------------- P3

    /** The start of section 1, where Hypixel drops the players from Storm's room. */
    private static final Vec3 P3_LANDING = new Vec3(102.5, 116, 36.5);
    /** The room's own levers, {x, y, z, section}: two per section, where the arena has them. */
    private static final int[][] P3_LEVERS = {
        {94, 124, 113, 1}, {106, 124, 113, 1},
        {27, 124, 127, 2}, {23, 132, 138, 2},
        {2, 122, 55, 3}, {14, 122, 55, 3},
        {84, 121, 34, 4}, {86, 128, 46, 4},
    };
    /** NoammAddons' four P3 sections (as F7 Extras boxes them). */
    private static final AABB[] P3_SECTIONS = {
        new AABB(90, 105, 32, 111, 158, 123),
        new AABB(16, 105, 122, 111, 158, 143),
        new AABB(-3, 106, 48, 19, 158, 142),
        new AABB(-3, 106, 30, 91, 158, 50),
    };
    /** The core, behind the gold door in the inner wall of section 4 (F7 Extras' "in the core" box). */
    private static final AABB CORE = new AABB(42, 106, 54, 68, 155, 119);
    private static final Vec3 GOLDOR_CORE_SPOT = new Vec3(54.5, 114, 85.5);
    /** Goldor's death tick: every 60 ticks from Storm's death line (Boss Phases' timer). */
    private static final int DEATH_TICK = 60;
    /**
     * The three gates, {plane axis (0 = x, 1 = z), plane, barrier plane, from, to}: Hypixel's portcullis is a sheet
     * of cobblestone, cobblestone walls and nether bricks one block thick, with a sheet of barriers behind it,
     * from y 115 to 134. Read off the arena's blocks.
     */
    private static final int[][] GATES = {{1, 123, 122, 96, 104}, {0, 17, 18, 128, 136}, {1, 49, 50, 4, 12}};
    private static final int GATE_Y0 = 115, GATE_Y1 = 134;

    // ---------------------------------------------------------------------------------------------- P4

    private static final Vec3 P4_LANDING = new Vec3(54.5, 65, 76.5);
    private static final Vec3 P4_CENTRE = new Vec3(54.5, 70, 76.5);
    static final Vec3 P5_LANDING = new Vec3(54.5, 5, 76.5);

    private enum Phase { MAXOR, STORM, GOLDOR, NECRON, WITHER_KING, DONE }

    /** The fight the use/attack callbacks hand clicks to. Server thread. */
    private static SimNecron live;

    final boolean master = SimState.floorLabel().toUpperCase(Locale.ROOT).startsWith("M");
    private Phase phase = Phase.MAXOR;
    private int phaseTicks;
    /** Fight ticks, from {@link #tick}. */
    int now;
    private ServerLevel level;

    /** The current wither's body and pool; null between phases (and in P5, whose pool is the dragons'). */
    private WitherBody body;
    private SimBoss.Health health = new SimBoss.Health(1);
    /** The pool cannot be hit below this (a stun's share; 0 when it may die). */
    private double floorHp;
    private int deadAt = -1;
    private String bodyName = "Maxor";
    private int contactCooldown;

    private final List<Blast> blasts = new ArrayList<>();
    /** What was in a player's last hotbar slot before he picked up a crystal or a relic. */
    private final Map<UUID, ItemStack> savedSlot = new HashMap<>();
    private final Map<UUID, String> carrying = new HashMap<>();
    private final Map<UUID, Integer> bounceUntil = new HashMap<>();

    // P1
    private final List<EndCrystal> pickups = new ArrayList<>();
    private final EndCrystal[] placed = new EndCrystal[PADS.length];
    private int crystalsAt = -1;
    private int laserAt = -1;
    private int stuns;
    private int stunEnd = -1;
    private boolean enraged;
    private int nextVolley;

    // P2
    private final List<Pillar> pillars = new ArrayList<>();
    private int crushes;
    private int pyStep;
    private int pyNext;
    private int pyDone;

    // P3
    private int deathTickBase = -1;

    // P4
    private int act;
    private int actAt;
    private int nextFlame;
    private int nextCharge;
    private Vec3 actFrom;
    private Vec3 actTarget;
    private double orbit;

    // P5
    private SimNecronDragons dragons;

    /** A telegraphed blast: a ring on the ground, then a hit on everyone inside it. */
    private record Blast(Vec3 at, double radius, int goesOff, double damage, int colour, boolean lightning) {
    }

    /** One of Storm's pillars and its pad. */
    private static final class Pillar {
        final int cx, cz;
        final AABB pad;
        final int colour;
        final List<int[]> cells = new ArrayList<>();
        int bottom = 175;
        /** 0 up, 1 dropping, 2 down. */
        int state;
        int layers;
        int ticks;
        int readyAt;

        Pillar(int[] d) {
            cx = d[0];
            cz = d[1];
            pad = new AABB(d[2], PAD_TOP + 0.5, d[4], d[3] + 1, PAD_TOP + 4, d[5] + 1);
            colour = d[6];
        }
    }

    SimNecron(SimBossArena arena) {
        super(arena);
    }

    /** Server-side clicks on the fight's crystals and relics. Called once from {@link SimBoss#register}. */
    static void register() {
        AttackEntityCallback.EVENT.register((player, lvl, hand, entity, hit) -> {
            if (!(entity instanceof EndCrystal || entity instanceof ArmorStand) || !entity.entityTags().contains(TAG)) {
                return InteractionResult.PASS;
            }
            if (!lvl.isClientSide() && player instanceof ServerPlayer sp && live != null) {
                live.touch(sp, entity);
            }
            // Never a vanilla hit: an End Crystal blows up when hit.
            return InteractionResult.FAIL;
        });
        UseEntityCallback.EVENT.register((player, lvl, hand, entity, hit) -> {
            if (!(entity instanceof EndCrystal || entity instanceof ArmorStand) || !entity.entityTags().contains(TAG)) {
                return InteractionResult.PASS;
            }
            if (!lvl.isClientSide() && player instanceof ServerPlayer sp && live != null) {
                live.touch(sp, entity);
            }
            return InteractionResult.SUCCESS;
        });
    }

    // ---------------------------------------------------------------------------------------------- the frame

    @Override
    String name() {
        // Hypixel's end line names every wither the run fought (and on M7 the Wither King).
        return master ? "Maxor, Storm, Goldor, Necron, and Wither King" : "Maxor, Storm, Goldor, and Necron";
    }

    @Override
    void start(ServerLevel level) {
        this.level = level;
        live = this;
        patchSection3(level);
        startMaxor(level);
    }

    @Override
    void tick(ServerLevel level, int t) {
        this.level = level;
        now = t;
        phaseTicks++;
        lavaBounce(level);
        switch (phase) {
            case MAXOR -> tickMaxor(level);
            case STORM -> tickStorm(level);
            case GOLDOR -> tickGoldor(level);
            case NECRON -> tickNecron(level);
            case WITHER_KING -> {
                dragons.tick(level);
                if (dragons.finished()) {
                    toPhase(Phase.DONE);
                    cleanUp(level);
                }
            }
            default -> {
            }
        }
        tickBlasts(level);
        nameTag();
    }

    private void toPhase(Phase p) {
        phase = p;
        phaseTicks = 0;
        deadAt = -1;
        blasts.clear();
    }

    @Override
    boolean isBoss(Entity entity) {
        return (body != null && entity == body) || (dragons != null && dragons.isDragon(entity));
    }

    @Override
    boolean hit(ServerLevel level, LivingEntity hitBody, float amount) {
        if (dragons != null && dragons.isDragon(hitBody)) {
            return dragons.hit(hitBody, amount);
        }
        if (hitBody != body || deadAt >= 0 || !health.hit(amount)) {
            return false;
        }
        health.hp = Math.max(health.hp, floorHp);
        return true;
    }

    @Override
    boolean defeated() {
        return phase == Phase.DONE;
    }

    @Override
    void discard(ServerLevel level) {
        cleanUp(level);
        if (live == this) {
            live = null;
        }
    }

    /** Removes everything the fight placed and gives back the players' last hotbar slots. */
    private void cleanUp(ServerLevel level) {
        removeBody();
        for (EndCrystal c : pickups) {
            c.discard();
        }
        pickups.clear();
        for (int i = 0; i < placed.length; i++) {
            if (placed[i] != null) {
                placed[i].discard();
                placed[i] = null;
            }
        }
        if (dragons != null) {
            dragons.discard();
        }
        for (Pillar p : pillars) {
            raise(level, p);
        }
        SimTerms.stopInArena(level.getServer());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            uncarry(p);
        }
        savedSlot.clear();
        carrying.clear();
        blasts.clear();
        List<Entity> doomed = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e.entityTags().contains(TAG)) {
                doomed.add(e);
            }
        }
        for (Entity e : doomed) {
            e.discard();
        }
    }

    @Override
    Component barName() {
        String name = phase == Phase.WITHER_KING || phase == Phase.DONE && master ? "Wither King" : bodyName;
        ChatFormatting colour = switch (phase) {
            case MAXOR -> ChatFormatting.DARK_PURPLE;
            case STORM -> ChatFormatting.AQUA;
            case GOLDOR -> ChatFormatting.GOLD;
            case NECRON -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_RED;
        };
        return Component.literal(name).withStyle(colour, ChatFormatting.BOLD);
    }

    @Override
    BossEvent.BossBarColor barColor() {
        return switch (phase) {
            case MAXOR -> BossEvent.BossBarColor.PURPLE;
            case STORM -> BossEvent.BossBarColor.BLUE;
            case GOLDOR -> BossEvent.BossBarColor.YELLOW;
            case NECRON -> BossEvent.BossBarColor.RED;
            default -> BossEvent.BossBarColor.PINK;
        };
    }

    @Override
    float barProgress() {
        if (phase == Phase.WITHER_KING && dragons != null) {
            return dragons.progress();
        }
        return deadAt >= 0 ? 0f : health.fraction();
    }

    // ---------------------------------------------------------------------------------------------- P1 Maxor

    private void startMaxor(ServerLevel level) {
        toPhase(Phase.MAXOR);
        newPool("Maxor", MAXOR_HEALTH);
        Vec3 at = Vec3.atBottomCenterOf(arena.toWorld(arena.bossSpawn));
        body = spawnWither(level, at);
        // Hypixel's first line, word for word: Boss Phases, Dungeon Features and F7 Extras start P1 on it.
        SimBoss.say(level.getServer(), "Maxor", "WELL! WELL! WELL! LOOK WHO'S HERE!");
        crystalsAt = now + 60;
        nextVolley = now + 80;
        SimSurvival.setHome(arena.toWorld(arena.entry));
    }

    private void tickMaxor(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (deadAt >= 0) {
            if (now - deadAt >= DEAD_HOLD) {
                startStorm(level);
            }
            return;
        }
        if (phaseTicks == 50) {
            SimBoss.say(server, "Maxor", "I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU.");
        } else if (phaseTicks == 110) {
            SimBoss.say(server, "Maxor", "DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE.");
        }
        boolean stunned = stunEnd > now;
        if (stunned) {
            glide(body, new Vec3(LASER.x, LASER.y + 5, LASER.z), 0.6);
            beam(level, ParticleTypes.END_ROD, new Vec3(LASER.x, LASER.y + 1, LASER.z), new Vec3(LASER.x, LASER.y + 20, LASER.z), 0.5);
        } else {
            chase(body, 226, enraged ? 0.24 : 0.18, 5);
        }
        if (crystalsAt >= 0 && now >= crystalsAt) {
            crystalsAt = -1;
            spawnCrystals(level);
        }
        tickCrystals(level);
        if (laserAt >= 0) {
            if (now % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, LASER.x, LASER.y + 2, LASER.z, 12, 0.6, 1.5, 0.6, 0.05);
            }
            if (now >= laserAt) {
                fireLaser(level);
            }
        }
        if (stunEnd >= 0 && now >= stunEnd) {
            stunEnd = -1;
            health.invulnerable = true;
            if (!enraged) {
                // F7 Extras turns this subtitle into its coloured "enraged" title.
                enraged = true;
                subtitleAll(server, Component.literal("⚠ Maxor is enraged! ⚠").withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        if (!stunned && now >= nextVolley) {
            volley(level, 0x8B008B, enraged ? 18 : 25, 3.0, false);
            nextVolley = now + (enraged ? 40 : 60) - (master ? 10 : 0);
        }
        contact(level);
        if (health.hp <= 0) {
            SimBoss.say(server, "Maxor", "I'M TOO YOUNG TO DIE AGAIN!");
            killBody(level);
        }
    }

    private void spawnCrystals(ServerLevel level) {
        for (EndCrystal c : pickups) {
            c.discard();
        }
        pickups.clear();
        for (int[] s : CRYSTAL_SPOTS) {
            double y = ground(level, s[0], s[1], 232, 214);
            EndCrystal c = new EndCrystal(level, s[0] + 0.5, y + 0.5, s[1] + 0.5);
            c.setShowBottom(false);
            c.setInvulnerable(true);
            c.addTag(TAG);
            if (level.addFreshEntity(c)) {
                pickups.add(c);
            }
        }
        level.playSound(null, LASER.x, LASER.y, LASER.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2f, 1.2f);
    }

    /** A pickup by walking into a crystal, a placement by walking a carried one up to a free pad. */
    private void tickCrystals(ServerLevel level) {
        for (ServerPlayer p : players(level)) {
            if (!carrying.containsKey(p.getUUID())) {
                for (EndCrystal c : new ArrayList<>(pickups)) {
                    if (p.distanceToSqr(c) < 2.2 * 2.2) {
                        pickCrystal(level, p, c);
                        break;
                    }
                }
                continue;
            }
            if (!"crystal".equals(carrying.get(p.getUUID()))) {
                continue;
            }
            for (int i = 0; i < PADS.length; i++) {
                int[] pad = PADS[i];
                double dx = p.getX() - (pad[0] + 0.5);
                double dz = p.getZ() - (pad[2] + 0.5);
                if (placed[i] == null && dx * dx + dz * dz < 2.8 * 2.8 && Math.abs(p.getY() - pad[1]) < 4.5) {
                    placeCrystal(level, p, i);
                    break;
                }
            }
        }
    }

    private void pickCrystal(ServerLevel level, ServerPlayer p, EndCrystal c) {
        pickups.remove(c);
        c.discard();
        ItemStack item = new ItemStack(Items.END_CRYSTAL);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Energy Crystal").withStyle(ChatFormatting.AQUA));
        carry(p, "crystal", item);
        // Hypixel's line: F7 Extras starts its crystal place timer on it.
        broadcast(level.getServer(), Component.literal(p.getGameProfile().name() + " picked up an Energy Crystal!"));
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f);
    }

    private void placeCrystal(ServerLevel level, ServerPlayer p, int i) {
        uncarry(p);
        int[] pad = PADS[i];
        EndCrystal c = new EndCrystal(level, pad[0] + 0.5, pad[1], pad[2] + 0.5);
        c.setShowBottom(false);
        c.setInvulnerable(true);
        c.setBeamTarget(BlockPos.containing(LASER.x, LASER.y + 6, LASER.z));
        c.addTag(TAG);
        level.addFreshEntity(c);
        placed[i] = c;
        int active = 0;
        for (EndCrystal e : placed) {
            if (e != null) {
                active++;
            }
        }
        MinecraftServer server = level.getServer();
        // Hypixel's subtitles: F7 Extras shows the first as its "Crystal (1/2)" title and hides the second.
        subtitleAll(server, Component.literal(active + "/" + PADS.length + " Energy Crystals are now active!")
                .withStyle(ChatFormatting.AQUA));
        if (active == PADS.length) {
            subtitleAll(server, Component.literal("The Energy Laser is charging up!").withStyle(ChatFormatting.AQUA));
            laserAt = now + LASER_CHARGE_TICKS;
            level.playSound(null, LASER.x, LASER.y, LASER.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3f, 1f);
        }
    }

    private void fireLaser(ServerLevel level) {
        MinecraftServer server = level.getServer();
        laserAt = -1;
        for (int i = 0; i < placed.length; i++) {
            if (placed[i] != null) {
                placed[i].discard();
                placed[i] = null;
            }
        }
        crystalsAt = now + CRYSTAL_RESPAWN_TICKS;
        level.playSound(null, LASER.x, LASER.y, LASER.z, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 3f, 0.6f);
        beam(level, ParticleTypes.END_ROD, new Vec3(LASER.x, LASER.y + 1, LASER.z), new Vec3(LASER.x, LASER.y + 24, LASER.z), 0.3);
        double dx = body.getX() - LASER.x;
        double dz = body.getZ() - LASER.z;
        if (dx * dx + dz * dz > LASER_REACH * LASER_REACH) {
            SimBoss.say(server, "Maxor", "HAH! MISSED ME!");
            return;
        }
        stuns++;
        // Hypixel's two stun lines: F7 Extras' crystal respawn timer and "Maxor Stunned!" start on them.
        SimBoss.say(server, "Maxor", stuns == 1 ? "THAT BEAM! IT HURTS! IT HURTS!!" : "YOU TRICKED ME!");
        stunEnd = now + (master ? 140 : 200);
        health.invulnerable = false;
        // A stun can take him down to half, so it is always two lasers at least.
        floorHp = Math.max(0, health.max * (1 - stuns / 2.0));
        level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 2f, 0.8f);
    }

    // ---------------------------------------------------------------------------------------------- P2 Storm

    private void startStorm(ServerLevel level) {
        removeBody();
        clearCarried(level);
        toPhase(Phase.STORM);
        moveAll(level, P2_LANDING, 180f);
        newPool("Storm", STORM_HEALTH);
        body = spawnWither(level, new Vec3(P2_LANDING.x, STORM_Y + 6, P2_LANDING.z + 12));
        // Hypixel's line, word for word: Storm's phase (and Rag Axe's prompt) start on it.
        SimBoss.say(level.getServer(), "Storm", "Pathetic Maxor, just like expected.");
        pillars.clear();
        for (int[] d : STORM_PADS) {
            Pillar p = new Pillar(d);
            for (int x = p.cx - 6; x <= p.cx + 6; x++) {
                for (int z = p.cz - 6; z <= p.cz + 6; z++) {
                    if (level.getBlockState(new BlockPos(x, 190, z)).is(Blocks.POLISHED_DIORITE)) {
                        p.cells.add(new int[]{x, z});
                    }
                }
            }
            for (int y = PAD_TOP + 1; y < 210; y++) {
                if (level.getBlockState(new BlockPos(p.cx, y, p.cz)).is(Blocks.POLISHED_DIORITE)) {
                    p.bottom = y;
                    break;
                }
            }
            pillars.add(p);
        }
        crushes = 0;
        enraged = false;
        pyStep = 0;
        pyDone = 0;
        nextVolley = now + 60;
    }

    private int crushesNeeded() {
        return master ? 3 : 2;
    }

    private void tickStorm(ServerLevel level) {
        MinecraftServer server = level.getServer();
        tickPillars(level);
        if (deadAt >= 0) {
            if (now - deadAt >= DEAD_HOLD) {
                startGoldor(level);
            }
            return;
        }
        if (phaseTicks == 60) {
            SimBoss.say(server, "Storm", "Lure me under a pillar if you can. You will not!");
        }
        boolean stunned = stunEnd > now;
        if (!stunned) {
            chase(body, STORM_Y, enraged ? 0.2 : 0.15, 2.5);
        }
        if (stunEnd >= 0 && now >= stunEnd) {
            stunEnd = -1;
            health.invulnerable = true;
            enraged = true;
            subtitleAll(server, Component.literal("⚠ Storm is enraged! ⚠").withStyle(ChatFormatting.AQUA));
        }
        // The big storm, once below 60% (and on M7 again below 30%): Hypixel's lines, which Boss Phases' lightning
        // timer and Dungeon Features start on.
        double frac = health.fraction();
        if (pyStep == 0 && (pyDone == 0 && frac <= 0.6 || master && pyDone == 1 && frac <= 0.3)) {
            SimBoss.say(server, "Storm", pyDone == 0 ? "ENERGY HEED MY CALL!" : "THUNDER LET ME BE YOUR CATALYST!");
            pyDone++;
            // F7 Extras reads the first number as the storm's length (1.35 s a number).
            pyStep = master ? 6 : 4;
            pyNext = now + 20;
        }
        if (pyStep > 0 && now >= pyNext) {
            stormStrike(level, pyStep);
            pyStep--;
            pyNext = now + 27;
        }
        if (pyStep == 0 && !stunned && now >= nextVolley) {
            volley(level, 0x55FFFF, 20, 2.5, true);
            nextVolley = now + (enraged ? 50 : 70) - (master ? 10 : 0);
        }
        contact(level);
        if (health.hp <= 0) {
            // Hypixel's death line, word for word: Goldor's death tick timer and Storm Dead! start on it.
            SimBoss.say(server, "Storm", "I should have known that I stood no chance.");
            deathTickBase = now;
            killBody(level);
        }
    }

    /** One number of the big storm: the title, and a strike on everyone who is not up on a pad. */
    private void stormStrike(ServerLevel level, int number) {
        for (ServerPlayer p : players(level)) {
            title(p, Component.literal(String.valueOf(number)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            if (onPad(p) != null) {
                continue;
            }
            lightning(level, p.position());
            hurtPlayerTrue(level, p, STORM_STRIKE_SHARE, 0.4);
        }
    }

    private Pillar onPad(ServerPlayer p) {
        for (Pillar pl : pillars) {
            if (pl.pad.contains(p.position())) {
                return pl;
            }
        }
        return null;
    }

    /** Arms a pillar with a player on its pad, drops it when Storm is under it, and crushes him if he still is. */
    private void tickPillars(ServerLevel level) {
        for (Pillar pl : pillars) {
            switch (pl.state) {
                case 0 -> {
                    if (deadAt >= 0 || body == null || now < pl.readyAt || stunEnd > now) {
                        continue;
                    }
                    boolean manned = false;
                    for (ServerPlayer p : players(level)) {
                        manned |= pl.pad.contains(p.position());
                    }
                    if (manned && under(pl)) {
                        pl.state = 1;
                        pl.layers = 0;
                        level.playSound(null, pl.cx, pl.bottom, pl.cz, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2f, 0.5f);
                    }
                }
                case 1 -> {
                    int y = pl.bottom - 1 - pl.layers;
                    if (y > PAD_TOP) {
                        fillLayer(level, pl, y, Blocks.POLISHED_DIORITE.defaultBlockState());
                        pl.layers++;
                        continue;
                    }
                    pl.state = 2;
                    pl.ticks = 0;
                    if (body != null && deadAt < 0 && under(pl)) {
                        crush(level, pl);
                    }
                }
                default -> {
                    if (++pl.ticks >= 30) {
                        raise(level, pl);
                    }
                }
            }
        }
    }

    private boolean under(Pillar pl) {
        double dx = body.getX() - (pl.cx + 0.5);
        double dz = body.getZ() - (pl.cz + 0.5);
        return dx * dx + dz * dz < 4.5 * 4.5;
    }

    /** One layer of a dropping pillar, leaving out any cell a player stands in (no one is buried alive). */
    private void fillLayer(ServerLevel level, Pillar pl, int y, BlockState state) {
        for (int[] c : pl.cells) {
            AABB cell = new AABB(c[0], y, c[1], c[0] + 1, y + 1, c[1] + 1);
            boolean taken = false;
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                taken |= p.getBoundingBox().intersects(cell);
            }
            if (!taken) {
                level.setBlock(new BlockPos(c[0], y, c[1]), state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private void raise(ServerLevel level, Pillar pl) {
        if (pl.state == 0) {
            return;
        }
        for (int y = PAD_TOP + 1; y < pl.bottom; y++) {
            for (int[] c : pl.cells) {
                BlockPos pos = new BlockPos(c[0], y, c[1]);
                if (level.getBlockState(pos).is(Blocks.POLISHED_DIORITE)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        pl.state = 0;
        pl.readyAt = now + PILLAR_COOLDOWN;
    }

    private void crush(ServerLevel level, Pillar pl) {
        MinecraftServer server = level.getServer();
        crushes++;
        // Hypixel's two crush lines: F7 Extras' "Storm Crushed!" title starts on them.
        SimBoss.say(server, "Storm", crushes % 2 == 1 ? "Oof" : "Ouch, that hurt!");
        level.sendParticles(ParticleTypes.EXPLOSION, body.getX(), body.getY() + 1, body.getZ(), 4, 1, 1, 1, 0);
        level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2f, 0.6f);
        // The crush itself takes a share, then a few seconds to finish the rest of this crush's share.
        floorHp = Math.max(0, health.max * (1 - (double) crushes / crushesNeeded()));
        health.hp = Math.max(floorHp, health.hp - health.max * 0.15);
        health.invulnerable = false;
        stunEnd = now + (master ? 100 : 140);
    }

    // ---------------------------------------------------------------------------------------------- P3 Goldor

    private void startGoldor(ServerLevel level) {
        for (Pillar pl : pillars) {
            raise(level, pl);
        }
        removeBody();
        toPhase(Phase.GOLDOR);
        moveAll(level, P3_LANDING, 0f);
        newPool("Goldor", GOLDOR_HEALTH);
        body = spawnWither(level, new Vec3(100.5, 117, 70.5));
        // Hypixel's line, word for word: Terminals 1, the death tick's resync and Rag Axe start on it.
        SimBoss.say(level.getServer(), "Goldor", "Who dares trespass into my domain?");
        // Boss Phases and Dungeon Features restart the death tick timer on this line, so the fight does too.
        deathTickBase = now;
        SimTerms.startInArena(level, new SimTerms.Arena() {
            @Override
            public void openGate(ServerLevel l, int section) {
                SimNecron.this.openGate(l, section);
            }

            @Override
            public void openCore(ServerLevel l) {
                SimNecron.this.openCore(l);
            }
        }, P3_LEVERS);
        SimBoss.tell(level.getServer(), com.epic60869.sky2m.sim.util.ModChat.text(
                "P3: each section's terminals, two levers and its device open the gate to the next."));
    }

    private void tickGoldor(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (deadAt >= 0) {
            if (now - deadAt >= DEAD_HOLD) {
                startNecron(level);
            }
            return;
        }
        boolean coreOpen = SimTerms.coreOpen() || !SimTerms.isActive();
        if (!coreOpen) {
            int section = Math.max(1, Math.min(4, SimTerms.section()));
            ServerPlayer target = nearestIn(level, P3_SECTIONS[section - 1]);
            if (target != null) {
                glide(body, new Vec3(target.getX(), Math.max(108, target.getY() + 0.5), target.getZ()), 0.12);
            }
            if (deathTickBase >= 0 && (now - deathTickBase) % DEATH_TICK == 0) {
                deathTick(level, section);
            }
            contact(level);
            return;
        }
        if (health.invulnerable && phaseTicks > 0 && deadAt < 0 && body.distanceToSqr(GOLDOR_CORE_SPOT) > 1) {
            // The core is open: Goldor goes in to make his stand, and can be hurt from now on.
            body.snapTo(GOLDOR_CORE_SPOT.x, GOLDOR_CORE_SPOT.y, GOLDOR_CORE_SPOT.z, 180f, 0f);
            health.invulnerable = false;
            floorHp = 0;
            nextVolley = now + 40;
            SimBoss.say(server, "Goldor", "You have done it, you destroyed the factory... but you will not leave the core alive!");
            SimSurvival.setHome(BlockPos.containing(54.5, 115, 57.5));
        }
        ServerPlayer target = nearestIn(level, CORE);
        if (target != null) {
            glide(body, new Vec3(target.getX(), Math.max(114, target.getY() + 0.5), target.getZ()), 0.1);
        }
        if (now >= nextVolley) {
            volley(level, 0xFFAA00, 25, 3.0, false);
            nextVolley = now + (master ? 50 : 70);
        }
        contact(level);
        if (health.hp <= 0) {
            SimBoss.say(server, "Goldor", "Necron, forgive me...");
            killBody(level);
            SimTerms.stopInArena(server);
        }
    }

    /**
     * Goldor's death tick: every 60 ticks from Storm's death, everyone in P3 who is not in the section being done
     * (or the core) takes {@link #DEATH_TICK_SHARE} of his max health as true damage (APPROXIMATE size - the wiki
     * has no number). On Hypixel it is what punishes being left behind.
     */
    private void deathTick(ServerLevel level, int section) {
        for (ServerPlayer p : players(level)) {
            Vec3 pos = p.position();
            if (pos.y < 100 || pos.y > 160 || P3_SECTIONS[section - 1].contains(pos) || CORE.contains(pos)) {
                continue;
            }
            hurtPlayerTrue(level, p, DEATH_TICK_SHARE, 0.3);
            p.sendSystemMessage(Component.literal("Goldor's death tick! Keep up with the section.")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /** Takes a section's gate down: the portcullis sheet and the barriers behind it. */
    private void openGate(ServerLevel level, int section) {
        int[] g = GATES[section - 1];
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = GATE_Y0; y <= GATE_Y1; y++) {
            for (int a = g[3]; a <= g[4]; a++) {
                BlockPos gate = g[0] == 1 ? new BlockPos(a, y, g[1]) : new BlockPos(g[1], y, a);
                BlockPos barrier = g[0] == 1 ? new BlockPos(a, y, g[2]) : new BlockPos(g[2], y, a);
                level.setBlock(gate, air, Block.UPDATE_ALL);
                if (level.getBlockState(barrier).is(Blocks.BARRIER)) {
                    level.setBlock(barrier, air, Block.UPDATE_ALL);
                }
            }
        }
        level.playSound(null, g[0] == 1 ? (g[3] + g[4]) / 2.0 : g[1], 120, g[0] == 1 ? g[1] : (g[3] + g[4]) / 2.0,
                SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3f, 0.5f);
    }

    /** Opens the core: the gold blocks in the inner wall of section 4. */
    private void openCore(ServerLevel level) {
        for (int x = 51; x <= 57; x++) {
            for (int y = 113; y <= 126; y++) {
                for (int z = 53; z <= 55; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.GOLD_BLOCK)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        level.playSound(null, 54.5, 117, 54.5, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 3f, 0.7f);
    }

    /**
     * Rebuilds section 3's west edge. The fan copy of the room was cut at x 0 and Hypixel's section 3 reaches x -3:
     * three of its terminals and the Arrow Align frames are at x -2, against the wall at x -3. So the corridor's
     * first column (x 1) is copied over x -2..0 and a wall is put at x -3, which gives the terminals somewhere to
     * stand and the frames something to hang on. APPROXIMATE: the corridor's real west side is not known.
     */
    private static void patchSection3(ServerLevel level) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockPos.MutableBlockPos from = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos to = new BlockPos.MutableBlockPos();
        for (int z = 48; z <= 124; z++) {
            for (int y = 100; y <= 158; y++) {
                BlockState copy = level.getBlockState(from.set(1, y, z));
                for (int x = -2; x <= 0; x++) {
                    if (level.getBlockState(to.set(x, y, z)) != copy) {
                        level.setBlock(to, copy, Block.UPDATE_CLIENTS);
                    }
                }
                if (!level.getBlockState(to.set(-3, y, z)).is(Blocks.STONE_BRICKS)) {
                    level.setBlock(to, wall, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------- P4 Necron

    private void startNecron(ServerLevel level) {
        removeBody();
        toPhase(Phase.NECRON);
        moveAll(level, P4_LANDING, 0f);
        newPool("Necron", NECRON_HEALTH);
        body = spawnWither(level, new Vec3(P4_CENTRE.x, P4_CENTRE.y + 4, P4_CENTRE.z + 20));
        // Hypixel's line, word for word: Necron's phase starts on it (and Goldor's timers stop).
        SimBoss.say(level.getServer(), "Necron", "You went further than any human before, congratulations.");
        act = 0;
        nextFlame = now + 120;
        nextCharge = now + 220;
        nextVolley = now + 160;
    }

    private void tickNecron(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (deadAt >= 0) {
            if (master) {
                if (now - deadAt == DEAD_HOLD) {
                    removeBody();
                }
                if (now - deadAt >= DEAD_HOLD + 20) {
                    startWitherKing(level);
                }
            } else if (now - deadAt >= DEAD_HOLD) {
                toPhase(Phase.DONE);
                cleanUp(level);
            }
            return;
        }
        if (phaseTicks == 60) {
            SimBoss.say(server, "Necron", "I'm afraid, your journey ends now.");
        }
        if (phaseTicks == 100) {
            // Hypixel's line: F7 Extras arms its Necron death checks on it.
            SimBoss.say(server, "Necron", "ARGH!");
            health.invulnerable = false;
        }
        if (phaseTicks < 100) {
            glide(body, new Vec3(P4_CENTRE.x, P4_CENTRE.y + 4, P4_CENTRE.z), 0.2);
            return;
        }
        tickNecronAttacks(level);
        contact(level);
        if (health.hp <= 0) {
            if (master) {
                // Since Hypixel's 2026 update Necron has no last line on M7; his death is a loud wither hurt
                // sound, which starts F7 Extras' relic spawn timer.
                level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 15f, 1f);
                deadAt = now;
                health.invulnerable = true;
            } else {
                // Hypixel's F7 last line: the end of the boss.
                SimBoss.say(server, "Necron", "All this, for nothing...");
                killBody(level);
            }
        }
    }

    /** 0 circling, 1 flamethrower aimed, 2 flamethrower on, 3 charge aimed, 4 charging. */
    private void tickNecronAttacks(ServerLevel level) {
        switch (act) {
            case 0 -> {
                orbit += 0.02;
                glide(body, new Vec3(P4_CENTRE.x + Math.cos(orbit) * 18, P4_CENTRE.y + 3, P4_CENTRE.z + Math.sin(orbit) * 18), 0.35);
                ServerPlayer target = nearest(level, body.position());
                if (target == null) {
                    return;
                }
                if (now >= nextCharge) {
                    act = 3;
                    actAt = now + 20;
                    actTarget = target.position();
                } else if (now >= nextFlame) {
                    act = 1;
                    actAt = now + 20;
                    actTarget = target.position();
                } else if (master && now >= nextVolley) {
                    volley(level, 0xFF4400, 22, 3.0, false);
                    nextVolley = now + 70;
                }
            }
            case 1, 2 -> {
                Vec3 eye = body.position().add(0, 2.5, 0);
                // The beam follows its target slowly, so it can be outrun but not stood in.
                ServerPlayer target = nearest(level, actTarget);
                if (target != null) {
                    actTarget = actTarget.lerp(target.position(), act == 1 ? 0.15 : 0.06);
                }
                Vec3 end = eye.add(actTarget.add(0, 0.5, 0).subtract(eye).normalize().scale(40));
                if (act == 1) {
                    if (now % 2 == 0) {
                        beam(level, ParticleTypes.SMOKE, eye, end, 1.0);
                    }
                    if (now >= actAt) {
                        act = 2;
                        actAt = now + 50;
                        level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3f, 0.5f);
                    }
                    return;
                }
                beam(level, ParticleTypes.FLAME, eye, end, 0.6);
                if (now % 20 == 0) {
                    for (ServerPlayer p : players(level)) {
                        if (distanceToSegment(p.position().add(0, 1, 0), eye, end) < 1.8) {
                            hurtPlayerTrue(level, p, NECRON_BEAM_SHARE, 0.1);
                        }
                    }
                }
                if (now >= actAt) {
                    act = 0;
                    nextFlame = now + (master ? 90 : 130);
                }
            }
            case 3 -> {
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, body.getX(), body.getY() + 2, body.getZ(), 3, 1, 1, 1, 0);
                circle(level, new DustParticleOptions(0xFF0000, 1.5f), actTarget, 4.0);
                if (now >= actAt) {
                    act = 4;
                    actAt = now + 12;
                    actFrom = body.position();
                    level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 3f, 0.6f);
                }
            }
            default -> {
                double f = 1 - Math.max(0, actAt - now) / 12.0;
                Vec3 to = new Vec3(actTarget.x, actTarget.y + 0.5, actTarget.z);
                Vec3 at = actFrom.lerp(to, f);
                body.snapTo(at.x, at.y, at.z, body.getYRot(), 0f);
                if (now >= actAt) {
                    level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 4, 1.5, 0.5, 1.5, 0);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f, 0.8f);
                    for (ServerPlayer p : players(level)) {
                        if (p.position().distanceToSqr(at) < 4.0 * 4.0) {
                            hurtPlayer(level, p, baseDamage(), p.getX() - at.x, p.getZ() - at.z, 1.4, 0.7);
                        }
                    }
                    act = 0;
                    nextCharge = now + (master ? 160 : 220);
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------- P5

    private void startWitherKing(ServerLevel level) {
        removeBody();
        toPhase(Phase.WITHER_KING);
        bodyName = "Wither King";
        moveAll(level, P5_LANDING, 0f);
        dragons = new SimNecronDragons(this, level);
    }

    // ---------------------------------------------------------------------------------------------- shared

    /** A wither's pool on this floor ({F7, M7} health), invulnerable until its phase opens it. */
    private void newPool(String name, double[] pool) {
        bodyName = name;
        health = new SimBoss.Health(SimHealth.byMode(pool[0], pool[1]));
        health.invulnerable = true;
        floorHp = 0;
        deadAt = -1;
        stunEnd = -1;
        laserAt = -1;
    }

    /** The current wither's base damage on this floor (Necron's own in P4 and after), before Defense. */
    double baseDamage() {
        double[] d = phase == Phase.NECRON || phase == Phase.WITHER_KING ? NECRON_DAMAGE : WITHER_DAMAGE;
        return SimHealth.byMode(d[0], d[1]);
    }

    /** Maxor's, Storm's and Goldor's base damage on this floor (the M7 dragons hit for it too). */
    static double witherDamage() {
        return SimHealth.byMode(WITHER_DAMAGE[0], WITHER_DAMAGE[1]);
    }

    /** The dying wither: empty bar kept up for {@link #DEAD_HOLD} ticks, then the next phase. */
    private void killBody(ServerLevel level) {
        deadAt = now;
        health.hp = 0;
        health.invulnerable = true;
        blasts.clear();
        if (body != null) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, body.getX(), body.getY() + 1.5, body.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, body.getX(), body.getY(), body.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2f, 1f);
            removeBody();
        }
    }

    private void removeBody() {
        if (body != null) {
            body.discard();
            body = null;
        }
    }

    private WitherBody spawnWither(ServerLevel level, Vec3 at) {
        WitherBody w = new WitherBody(level);
        w.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        w.setHealth(1000);
        w.setNoAi(true);
        w.setNoGravity(true);
        w.setPersistenceRequired();
        w.setCustomNameVisible(true);
        w.addTag(TAG);
        w.snapTo(at.x, at.y, at.z, 0f, 0f);
        level.addFreshEntity(w);
        return w;
    }

    private void nameTag() {
        if (body == null) {
            return;
        }
        Component tag = Component.literal("﴾ " + bodyName + " ﴿ ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(health.invulnerable ? "???" : SimBoss.shortHealth(health.hp))
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("❤").withStyle(ChatFormatting.RED)));
        body.setCustomName(tag);
    }

    /** Moves an entity a step toward a point, facing where it goes. */
    static void glide(Entity e, Vec3 to, double speed) {
        if (e == null) {
            return;
        }
        Vec3 from = e.position();
        Vec3 d = to.subtract(from);
        double len = d.length();
        Vec3 at = len <= speed ? to : from.add(d.scale(speed / len));
        float yaw = len < 0.01 ? e.getYRot() : (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
        e.snapTo(at.x, at.y, at.z, yaw, 0f);
        e.setYHeadRot(yaw);
        if (e instanceof LivingEntity le) {
            le.setYBodyRot(yaw);
        }
    }

    /** Floats the body toward the closest player at height {@code y}, stopping {@code keep} blocks short. */
    private void chase(Entity e, double y, double speed, double keep) {
        if (e == null) {
            return;
        }
        ServerPlayer target = nearest(level, e.position());
        if (target == null) {
            return;
        }
        double dx = target.getX() - e.getX();
        double dz = target.getZ() - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d <= keep) {
            glide(e, new Vec3(e.getX(), y, e.getZ()), speed);
            return;
        }
        glide(e, new Vec3(target.getX() - dx / d * keep, y, target.getZ() - dz / d * keep), speed);
    }

    /** A hit of the wither's base damage on anyone touching the body (once a second). */
    private void contact(ServerLevel level) {
        if (body == null || --contactCooldown > 0) {
            return;
        }
        for (ServerPlayer p : players(level)) {
            if (p.distanceToSqr(body) < 2.6 * 2.6) {
                hurtPlayer(level, p, baseDamage(), p.getX() - body.getX(), p.getZ() - body.getZ(), 1.0, 0.5);
                contactCooldown = 20;
            }
        }
    }

    /** Marks a blast of the wither's base damage under every player. */
    void volley(ServerLevel level, int colour, int warn, double radius, boolean lightning) {
        for (ServerPlayer p : players(level)) {
            blasts.add(new Blast(p.position(), radius, now + warn, baseDamage(), colour, lightning));
        }
    }

    /** Marks one blast; {@code damage} is Hypixel damage before Defense. */
    void blast(Vec3 at, double radius, int warn, double damage, int colour) {
        blasts.add(new Blast(at, radius, now + warn, damage, colour, false));
    }

    private void tickBlasts(ServerLevel level) {
        if (blasts.isEmpty()) {
            return;
        }
        List<Blast> done = new ArrayList<>();
        for (Blast b : blasts) {
            if (b.goesOff() > now) {
                if (now % 3 == 0) {
                    circle(level, new DustParticleOptions(b.colour(), 1.2f), b.at(), b.radius());
                }
                continue;
            }
            done.add(b);
            if (b.lightning()) {
                lightning(level, b.at());
            } else {
                level.sendParticles(ParticleTypes.EXPLOSION, b.at().x, b.at().y + 0.5, b.at().z, 2, 0.5, 0.3, 0.5, 0);
                level.playSound(null, b.at().x, b.at().y, b.at().z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1f, 1.2f);
            }
            for (ServerPlayer p : players(level)) {
                double dx = p.getX() - b.at().x;
                double dz = p.getZ() - b.at().z;
                if (dx * dx + dz * dz <= b.radius() * b.radius() && Math.abs(p.getY() - b.at().y) < 3) {
                    hurtPlayer(level, p, b.damage(), dx, dz, 0.6, 0.5);
                }
            }
        }
        blasts.removeAll(done);
    }

    static void circle(ServerLevel level, ParticleOptions particle, Vec3 at, double r) {
        int n = Math.max(12, (int) (r * 6));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(particle, at.x + Math.cos(a) * r, at.y + 0.1, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    static void beam(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        for (double s = 0; s <= len; s += step) {
            Vec3 p = from.add(d.scale(s / len));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** A lightning bolt for show (no fire, no vanilla damage: the hit is the fight's). */
    static void lightning(ServerLevel level, Vec3 at) {
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        bolt.setVisualOnly(true);
        bolt.snapTo(at.x, at.y, at.z);
        level.addFreshEntity(bolt);
    }

    /** True damage of {@code share} of the player's max health (Defense ignored), plus a lift. */
    private static void hurtPlayerTrue(ServerLevel level, ServerPlayer p, double share, double lift) {
        SimVitals.dealMaxHealthShare(level, p, level.damageSources().magic(), share, true);
        if (lift > 0) {
            p.setDeltaMovement(p.getDeltaMovement().x, lift, p.getDeltaMovement().z);
            p.hurtMarked = true;
        }
    }

    /** Damage plus knockback: {@code damage} is Hypixel damage before Defense ({@link SimVitals#dealHypixel}). */
    static void hurtPlayer(ServerLevel level, ServerPlayer p, double damage, double dx, double dz, double push, double lift) {
        DamageSource src = level.damageSources().magic();
        SimVitals.dealHypixel(level, p, src, damage, false);
        double len = Math.sqrt(dx * dx + dz * dz);
        if (push <= 0 && lift <= 0) {
            return;
        }
        if (len < 0.01) {
            double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            dx = Math.cos(a);
            dz = Math.sin(a);
            len = 1;
        }
        p.setDeltaMovement(dx / len * push, lift, dz / len * push);
        p.hurtMarked = true;
    }

    /**
     * The room's lava throws you back up rather than burning you, as Hypixel's boss lava does (the sim's lava does
     * no damage at all, by request).
     */
    private void lavaBounce(ServerLevel level) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (!p.isInLava() || !arena.contains(p.getX(), p.getY(), p.getZ())) {
                continue;
            }
            Integer until = bounceUntil.get(p.getUUID());
            if (until != null && now < until) {
                continue;
            }
            bounceUntil.put(p.getUUID(), now + 10);
            p.clearFire();
            p.setDeltaMovement(p.getDeltaMovement().x * 0.5, 1.4, p.getDeltaMovement().z * 0.5);
            p.hurtMarked = true;
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    /** The living players in the room (ghosts and spectators left out). */
    List<ServerPlayer> players(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (!p.isSpectator() && !SimDeath.isGhost(p) && p.isAlive() && arena.contains(p.getX(), p.getY(), p.getZ())) {
                out.add(p);
            }
        }
        return out;
    }

    ServerPlayer nearest(ServerLevel level, Vec3 from) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : players(level)) {
            double d = p.position().distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private ServerPlayer nearestIn(ServerLevel level, AABB box) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : players(level)) {
            double d = body == null ? 0 : p.distanceToSqr(body);
            if (box.contains(p.position()) && d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Every player (ghosts too) to the start of the next phase, which is also where death sends him now. */
    private void moveAll(ServerLevel level, Vec3 to, float yaw) {
        SimSurvival.setHome(BlockPos.containing(to));
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.teleportTo(level, to.x, to.y, to.z, Set.of(), yaw, 0f, false);
            p.fallDistance = 0;
            p.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** The first solid block's top going down from {@code fromY}, or {@code minY}. */
    static double ground(ServerLevel level, int x, int z, int fromY, int minY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = fromY; y > minY; y--) {
            if (!level.getBlockState(m.set(x, y, z)).isAir() && level.getBlockState(m.set(x, y + 1, z)).isAir()) {
                return y + 1;
            }
        }
        return minY;
    }

    /** A click on one of the fight's entities (a crystal to pick up, a relic). */
    private void touch(ServerPlayer p, Entity e) {
        if (phase == Phase.MAXOR && e instanceof EndCrystal c && pickups.contains(c)
                && !carrying.containsKey(p.getUUID()) && p.distanceToSqr(c) < 5 * 5) {
            pickCrystal((ServerLevel) p.level(), p, c);
        } else if (phase == Phase.WITHER_KING && dragons != null) {
            dragons.touch(p, e);
        }
    }

    /** Puts an item in the player's last hotbar slot (Hypixel's spot for a crystal or relic), keeping what was there. */
    void carry(ServerPlayer p, String kind, ItemStack stack) {
        savedSlot.putIfAbsent(p.getUUID(), p.getInventory().getItem(8).copy());
        carrying.put(p.getUUID(), kind);
        p.getInventory().setItem(8, stack);
    }

    /** What the player carries ("crystal", a relic's colour), or null. */
    String carried(ServerPlayer p) {
        return carrying.get(p.getUUID());
    }

    /** Gives back the player's last hotbar slot. */
    void uncarry(ServerPlayer p) {
        carrying.remove(p.getUUID());
        ItemStack old = savedSlot.remove(p.getUUID());
        if (old != null) {
            p.getInventory().setItem(8, old);
        }
    }

    private void clearCarried(ServerLevel level) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            uncarry(p);
        }
        for (EndCrystal c : pickups) {
            c.discard();
        }
        pickups.clear();
        for (int i = 0; i < placed.length; i++) {
            if (placed[i] != null) {
                placed[i].discard();
                placed[i] = null;
            }
        }
    }

    static void broadcast(MinecraftServer server, Component line) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(line);
        }
    }

    /** A subtitle (with an empty title, as Hypixel sends them) to everyone. */
    static void subtitleAll(MinecraftServer server, Component subtitle) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 40, 10));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    private static void title(ServerPlayer p, Component title) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    int now() {
        return now;
    }

    /**
     * A boss wither's body: no AI (the fight moves it), no vanilla boss bar (SimBoss shows the fight's), no
     * skulls or block breaking, never despawns.
     */
    static final class WitherBody extends WitherBoss {
        WitherBody(Level level) {
            super(EntityTypes.WITHER, level);
        }

        @Override
        public void startSeenByPlayer(ServerPlayer player) {
        }

        @Override
        public void stopSeenByPlayer(ServerPlayer player) {
        }

        @Override
        protected void customServerAiStep(ServerLevel level) {
        }

        @Override
        public void checkDespawn() {
        }
    }
}
