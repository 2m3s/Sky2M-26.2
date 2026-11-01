/*
 * From Killer560's Mod (https://github.com/Killer560-OG/killer560s-mod), part of the Dungeon Sim. MIT License,
 * Copyright (c) 2026 Killer560.
 */
package com.epic60869.sky2m.sim.roomsim;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Catacombs minibosses for the Dungeon Sim, each with its own fight.
 *
 * <p>Every one is a humanoid mob with real pathfinding that walks at you and swings, dressed the way Hypixel
 * dresses it, with Hypixel's health and damage for that miniboss on the floor ({@link SimHealth#statsFor})
 * and a "✯ Name 1.2M❤" stand that follows it. On top of the melee each has the mechanic that makes it that
 * miniboss, approximated from how the fights play on Hypixel:
 * <ul>
 * <li><b>Lost Adventurer</b> - 100 Defense (takes half of every hit) and, the first time it drops below 30%,
 * patches itself back up to 60%.</li>
 * <li><b>Frozen</b> (Hypixel's Frozen Adventurer; the user's name for it in the sim) - every hit slows you, and
 * every few seconds it winds up and sprays ice in a cone in front of it: damage and a heavy slow for anyone in
 * it. Step out of the cone during the wind-up.</li>
 * <li><b>Shadow Assassin</b> - vanishes (no body, no name tag), then reappears right behind you and strikes.</li>
 * <li><b>Angry Archaeologist</b> - fast, throws splash potions of harming at you, and splashes healing on itself
 * when it is hurt.</li>
 * <li><b>King Midas</b> - gold armour, slow, hits very hard and slams the ground when you stand next to him;
 * dies in a burst of gold.</li>
 * </ul>
 *
 * <p>All the fight logic runs in the entity's own server AI step, so a miniboss needs no tick of its own and goes
 * away cleanly with its entity. Nothing here is reachable without passing {@link SimState#canAct}: the only
 * caller is {@link SimMobs}'s already-gated spawn path.
 */
public final class SimMiniboss {

    /** The minibosses the sim can place. */
    public enum Name {
        LOST_ADVENTURER("Lost Adventurer"),
        FROZEN("Frozen"),
        SHADOW_ASSASSIN("Shadow Assassin"),
        ANGRY_ARCHAEOLOGIST("Angry Archaeologist"),
        KING_MIDAS("King Midas");

        private final String text;

        Name(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    private SimMiniboss() {
    }

    /** Builds (but does not add) a miniboss, dressed and tuned for the floor. Server thread. */
    static Zombie create(ServerLevel level, Name name) {
        Miniboss boss = new Miniboss(level, name, SimState.floorLabel());
        boss.dress();
        return boss;
    }

    /** Called by {@link SimMobs} once the miniboss's pool exists, to set the parts of its fight that live there. */
    static void tunePool(Entity boss, SimHealth.Pool pool) {
        if (boss instanceof Miniboss m && (m.name == Name.LOST_ADVENTURER || m.name == Name.FROZEN)) {
            // Both have 100 Defense on Hypixel (https://hypixelskyblock.minecraft.wiki/w/Lost_Adventurer,
            // https://hypixelskyblock.minecraft.wiki/w/Frozen_Adventurer): 1 - 100 / (100 + 100) of a hit lands.
            pool.taken = 0.5;
            pool.surviveOnce = m.name == Name.LOST_ADVENTURER;
        }
    }

    /** A sim miniboss. */
    private static final class Miniboss extends Zombie {

        final Name name;
        /**
         * A special attack's Hypixel damage before Defense: the miniboss's own base damage for the floor
         * ({@link SimHealth#damageOf}). APPROXIMATE as a per-attack number - the wiki gives the base damage but no
         * separate number for the ice spray, the backstab or Midas's gold.
         */
        private final double ability;
        private int cooldown;
        /** Ticks left of a wind-up or vanish, 0 when none is running. */
        private int windUp;
        private boolean healed;
        private int healCooldown;
        private Map<EquipmentSlot, ItemStack> stashed;

        Miniboss(Level level, Name name, String floor) {
            super(level);
            this.name = name;
            double base = SimHealth.damageOf(name.text(), floor);
            this.ability = Double.isNaN(base) ? 0 : base;
            float melee = SimHealth.meleeDamage(floor, SimHealth.Role.MINIBOSS);
            double speed = 0.25;
            switch (name) {
                case ANGRY_ARCHAEOLOGIST -> speed = 0.34;
                case SHADOW_ASSASSIN -> speed = 0.3;
                case KING_MIDAS -> {
                    speed = 0.21;
                    melee *= 1.3f;
                }
                default -> {
                }
            }
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(melee);
            getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(24);
            getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
            var reinforcements = getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
            if (reinforcements != null) {
                reinforcements.setBaseValue(0);
            }
            cooldown = 60 + ThreadLocalRandom.current().nextInt(40);
        }

        void dress() {
            switch (name) {
                case LOST_ADVENTURER -> wear(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS,
                        Items.IRON_BOOTS, Items.IRON_SWORD);
                case FROZEN -> wear(Items.PACKED_ICE, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS,
                        Items.CHAINMAIL_BOOTS, Items.DIAMOND_SWORD);
                case SHADOW_ASSASSIN -> wear(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
                        Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_SWORD);
                case ANGRY_ARCHAEOLOGIST -> wear(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
                        Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_PICKAXE);
                case KING_MIDAS -> wear(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS,
                        Items.GOLDEN_BOOTS, Items.GOLDEN_SWORD);
            }
        }

        private void wear(net.minecraft.world.item.Item head, net.minecraft.world.item.Item chest,
                          net.minecraft.world.item.Item legs, net.minecraft.world.item.Item feet,
                          net.minecraft.world.item.Item hand) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
            setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
            setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(hand));
        }

        @Override
        protected void customServerAiStep(ServerLevel level) {
            super.customServerAiStep(level);
            SimHealth.Pool pool = SimHealth.get(this);
            if (pool == null || pool.dying || !isAlive()) {
                return;
            }
            LivingEntity target = getTarget();
            ServerPlayer player = target instanceof ServerPlayer sp && sp.isAlive() ? sp : null;
            if (cooldown > 0) {
                cooldown--;
            }
            switch (name) {
                case LOST_ADVENTURER -> lostAdventurer(level, pool);
                case FROZEN -> frozen(level, player);
                case SHADOW_ASSASSIN -> shadowAssassin(level, pool, player);
                case ANGRY_ARCHAEOLOGIST -> archaeologist(level, pool, player);
                case KING_MIDAS -> midas(level, player);
            }
        }

        // ------------------------------------------------------------------------------------- Lost Adventurer

        /** Once, below 30%: back up to 60%. The pool's surviveOnce stops one big hit skipping past the heal. */
        private void lostAdventurer(ServerLevel level, SimHealth.Pool pool) {
            if (healed || pool.fraction() >= 0.3) {
                return;
            }
            healed = true;
            pool.surviveOnce = false;
            pool.heal(pool.max * 0.6 - pool.hp);
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.2, getZ(), 8, 0.5, 0.3, 0.5, 0.1);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 25, 0.5, 0.8, 0.5, 0.1);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.7f, 1.2f);
        }

        // ---------------------------------------------------------------------------------------------- Frozen

        private static final int SPRAY_WINDUP = 20;
        private static final double SPRAY_RANGE = 7.0;
        /** Cosine of the cone's half-angle: 35 degrees either side of where it faces. */
        private static final double SPRAY_CONE = Math.cos(Math.toRadians(35));

        private void frozen(ServerLevel level, ServerPlayer player) {
            if (windUp > 0) {
                getNavigation().stop();
                if (player != null) {
                    getLookControl().setLookAt(player, 30f, 30f);
                }
                level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.6, getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                if (--windUp == 0) {
                    spray(level);
                    cooldown = 100;
                }
                return;
            }
            if (cooldown == 0 && player != null && distanceToSqr(player) < SPRAY_RANGE * SPRAY_RANGE
                    && hasLineOfSight(player)) {
                windUp = SPRAY_WINDUP;
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE,
                        1.2f, 0.6f);
            }
        }

        /** The ice spray: a cone of snow in front of it, damaging and slowing anyone inside. */
        private void spray(ServerLevel level) {
            Vec3 look = getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0e-4) {
                flat = Vec3.directionFromRotation(0f, getYRot());
            }
            flat = flat.normalize();
            Vec3 eye = new Vec3(getX(), getY() + 1.4, getZ());
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < 60; i++) {
                double d = 1 + rnd.nextDouble() * (SPRAY_RANGE - 1);
                double a = (rnd.nextDouble() - 0.5) * Math.toRadians(70);
                double cx = flat.x * Math.cos(a) - flat.z * Math.sin(a);
                double cz = flat.x * Math.sin(a) + flat.z * Math.cos(a);
                level.sendParticles(i % 2 == 0 ? ParticleTypes.SNOWFLAKE : ParticleTypes.ITEM_SNOWBALL,
                        eye.x + cx * d, eye.y - 0.4 + rnd.nextDouble() * 0.8, eye.z + cz * d, 1, 0, 0, 0, 0);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1f, 1.6f);
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                Vec3 to = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
                double dist = to.length();
                if (dist > SPRAY_RANGE || Math.abs(p.getY() - getY()) > 3) {
                    continue;
                }
                if (dist > 0.8 && to.normalize().dot(flat) < SPRAY_CONE) {
                    continue;
                }
                SimVitals.dealHypixel(level, p, level.damageSources().indirectMagic(this, this), ability, false);
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2), this);
            }
        }

        /** Frozen's hits slow; everything else hits as a plain mob does. */
        @Override
        public boolean doHurtTarget(ServerLevel level, Entity target) {
            boolean hit = super.doHurtTarget(level, target);
            if (hit && name == Name.FROZEN && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 10,
                        0.3, 0.5, 0.3, 0.02);
            }
            return hit;
        }

        // ------------------------------------------------------------------------------------- Shadow Assassin

        private static final int VANISH_TICKS = 30;

        /** Vanish, then reappear behind the player and strike. */
        private void shadowAssassin(ServerLevel level, SimHealth.Pool pool, ServerPlayer player) {
            if (windUp > 0) {
                if (--windUp == 0) {
                    reappear(level, pool, player);
                }
                return;
            }
            if (cooldown == 0 && player != null && distanceToSqr(player) < 16 * 16) {
                vanish(level, pool);
            }
        }

        private void vanish(ServerLevel level, SimHealth.Pool pool) {
            windUp = VANISH_TICKS;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE,
                    1f, 1f);
            setInvisible(true);
            pool.hideLabel = true;
            // An invisible mob still draws what it wears and holds, so the gear comes off while it is gone.
            stashed = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
                stashed.put(slot, getItemBySlot(slot).copy());
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        private void reappear(ServerLevel level, SimHealth.Pool pool, ServerPlayer player) {
            if (player != null && player.isAlive() && distanceToSqr(player) < 24 * 24) {
                // Behind is opposite the way he is looking, on the flat.
                Vec3 look = Vec3.directionFromRotation(0f, player.getYRot());
                double x = player.getX() - look.x * 1.6;
                double z = player.getZ() - look.z * 1.6;
                AABB box = getDimensions(getPose()).makeBoundingBox(x, player.getY(), z);
                if (level.noCollision(this, box)) {
                    teleportTo(x, player.getY(), z);
                } else {
                    teleportTo(player.getX(), player.getY(), player.getZ());
                }
                getLookControl().setLookAt(player, 180f, 180f);
            }
            setInvisible(false);
            pool.hideLabel = false;
            if (stashed != null) {
                stashed.forEach(this::setItemSlot);
                stashed = null;
            }
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.02);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE,
                    1f, 0.7f);
            if (player != null && distanceToSqr(player) < 3.5 * 3.5) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                // The backstab: his normal swing plus the strike on top.
                doHurtTarget(level, player);
                SimVitals.dealHypixel(level, player, level.damageSources().indirectMagic(this, this), ability, false);
                level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 15,
                        0.3, 0.5, 0.3, 0.2);
            }
            cooldown = 120 + ThreadLocalRandom.current().nextInt(40);
        }

        // --------------------------------------------------------------------------------- Angry Archaeologist

        private void archaeologist(ServerLevel level, SimHealth.Pool pool, ServerPlayer player) {
            if (healCooldown > 0) {
                healCooldown--;
            } else if (pool.fraction() < 0.6) {
                // Splashes healing on himself. Applied to the pool, not as a real potion: he is undead, so a
                // vanilla Healing potion would hurt him.
                healCooldown = 200;
                pool.heal(pool.max * 0.12);
                level.levelEvent(2007, blockPosition(), 0xF82423);   // the instant-effect splash, healing red
                level.sendParticles(ParticleTypes.HEART, getX(), getY() + 2, getZ(), 5, 0.4, 0.3, 0.4, 0.1);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE,
                        1f, 1f);
            }
            if (cooldown == 0 && player != null && distanceToSqr(player) < 12 * 12 && hasLineOfSight(player)) {
                throwHarming(level, player);
                cooldown = 70 + ThreadLocalRandom.current().nextInt(30);
            }
        }

        /** A splash potion of Harming, aimed the way a witch leads her target. */
        private void throwHarming(ServerLevel level, ServerPlayer target) {
            Vec3 move = target.getDeltaMovement();
            double dx = target.getX() + move.x - getX();
            double dy = target.getEyeY() - 1.1 - getY();
            double dz = target.getZ() + move.z - getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            ItemStack stack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HARMING);
            Projectile.spawnProjectileUsingShoot(new ThrownSplashPotion(level, this, stack), level, stack, dx,
                    dy + dist * 0.2, dz, 0.75f, 8f);
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1f,
                    0.8f + getRandom().nextFloat() * 0.4f);
        }

        // ------------------------------------------------------------------------------------------ King Midas

        /** Stand next to him for a moment and he slams the ground: damage and a shove. */
        private void midas(ServerLevel level, ServerPlayer player) {
            if (cooldown > 0 || player == null || distanceToSqr(player) > 3.5 * 3.5) {
                return;
            }
            cooldown = 140;
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_BLOCK), getX(), getY() + 0.2,
                    getZ(), 40, 1.5, 0.1, 1.5, 0.15);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 0.8f);
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (p.distanceToSqr(this) > 4 * 4) {
                    continue;
                }
                SimVitals.dealHypixel(level, p, level.damageSources().indirectMagic(this, this), ability * 1.5, false);
                double dx = p.getX() - getX();
                double dz = p.getZ() - getZ();
                double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                p.setDeltaMovement(dx / len * 1.2, 0.5, dz / len * 1.2);
                p.hurtMarked = true;
            }
        }

        /** King Midas goes out in a burst of gold - coins you can see but not keep. */
        @Override
        public void die(DamageSource source) {
            super.die(source);
            if (name != Name.KING_MIDAS || !(level() instanceof ServerLevel level)) {
                return;
            }
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.GOLD_INGOT), getX(), getY() + 1,
                    getZ(), 60, 0.6, 0.8, 0.6, 0.25);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 1f, 0.6f);
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < 14; i++) {
                ItemEntity gold = new ItemEntity(level, getX(), getY() + 1, getZ(),
                        new ItemStack(i % 3 == 0 ? Items.GOLD_INGOT : Items.GOLD_NUGGET),
                        (rnd.nextDouble() - 0.5) * 0.5, 0.3 + rnd.nextDouble() * 0.3, (rnd.nextDouble() - 0.5) * 0.5);
                // Can't be picked up and is gone in a few seconds: the burst, not loot.
                gold.makeFakeItem();
                level.addFreshEntity(gold);
            }
        }

        // ------------------------------------------------------------------------------- plain-mob behaviour

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

        /** Hypixel's minibosses are player NPCs: no zombie groans. */
        @Override
        protected SoundEvent getAmbientSound() {
            return null;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.PLAYER_HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.PLAYER_DEATH;
        }
    }
}
