package com.mayhem.world;

import com.mayhem.combat.TargetFinder;
import com.mayhem.combat.UnarmedCombat;
import com.mayhem.config.MayhemConfig;
import com.mayhem.fx.Fx;
import com.mayhem.fx.MayhemSounds;
import com.mayhem.network.ImpactPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Tracks entities that were launched, spiked, frozen (hit-stop) or held in the air (juggles), and
 * produces the follow-up impacts: wall slams, ground crashes and spike craters.
 */
public final class Dynamics {
    private static final class Launch {
        UUID id, owner;
        ResourceKey<Level> dim;
        int start;
        boolean up, spike, hadAir;
        double vx, vz, speed;
    }

    private static final class Hold {
        UUID id;
        ResourceKey<Level> dim;
        int end;
        Vec3 resume; // velocity restored when a freeze ends, so hit-stop never cancels a launch
    }

    private static final List<Launch> LAUNCHES = new ArrayList<>();
    private static final List<Hold> SUSPENDS = new ArrayList<>();
    private static final List<Hold> FREEZES = new ArrayList<>();

    private Dynamics() {}

    // ------------------------------------------------------------------ registration

    private static Launch begin(LivingEntity t, ServerPlayer owner, boolean up, boolean spike) {
        LAUNCHES.removeIf(l -> l.id.equals(t.getUUID()));
        if (LAUNCHES.size() > 200) LAUNCHES.remove(0);
        Launch l = new Launch();
        l.id = t.getUUID();
        l.owner = owner != null ? owner.getUUID() : null;
        l.dim = t.level().dimension();
        l.start = t.getServer() != null ? t.getServer().getTickCount() : 0;
        l.up = up;
        l.spike = spike;
        Vec3 d = t.getDeltaMovement();
        l.vx = d.x;
        l.vz = d.z;
        l.speed = Math.sqrt(d.x * d.x + d.z * d.z);
        LAUNCHES.add(l);
        return l;
    }

    /** Track a knocked-back entity for wall slams (and ground crashes if it was launched upward). */
    public static void launch(LivingEntity target, ServerPlayer owner, boolean up) {
        begin(target, owner, up, false);
    }

    /** Smash an airborne entity into the ground; produces a crater and shockwave when it lands. */
    public static void spike(LivingEntity target, ServerPlayer owner) {
        target.setDeltaMovement(0, -3.0, 0);
        target.hurtMarked = true;
        begin(target, owner, false, true);
    }

    private static void hold(List<Hold> list, LivingEntity e, int ticks, Vec3 resume) {
        if (e.getServer() == null) return;
        int end = e.getServer().getTickCount() + ticks;
        for (Hold h : list) {
            if (h.id.equals(e.getUUID())) {
                h.end = Math.max(h.end, end);
                if (resume != null && h.resume == null) h.resume = resume;
                return;
            }
        }
        if (list.size() > 256) return;
        Hold h = new Hold();
        h.id = e.getUUID();
        h.dim = e.level().dimension();
        h.end = end;
        h.resume = resume;
        list.add(h);
    }

    /** Hit-stop: hold the entity completely still for a few ticks. */
    public static void freeze(LivingEntity e, int ticks) {
        // Call this AFTER setting knockback / launch velocity: that velocity is restored when the freeze ends.
        hold(FREEZES, e, ticks, e.getDeltaMovement());
    }

    /** Float the entity in mid-air (no gravity, horizontal drag) so air combos can continue. */
    public static void suspend(LivingEntity e, int ticks) {
        hold(SUSPENDS, e, ticks, null);
    }

    // ------------------------------------------------------------------ per-tick processing

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (!FREEZES.isEmpty()) tickHolds(server, FREEZES, now, true);
        if (!SUSPENDS.isEmpty()) tickHolds(server, SUSPENDS, now, false);
        if (!LAUNCHES.isEmpty()) tickLaunches(server, now);
    }

    private static void tickHolds(MinecraftServer server, List<Hold> list, int now, boolean freeze) {
        Iterator<Hold> it = list.iterator();
        while (it.hasNext()) {
            Hold h = it.next();
            ServerLevel level = server.getLevel(h.dim);
            Entity e = level == null ? null : level.getEntity(h.id);
            if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                it.remove();
                continue;
            }
            if (now >= h.end) {
                if (freeze && h.resume != null) {
                    le.setDeltaMovement(h.resume);
                    le.hurtMarked = true;
                }
                it.remove();
                continue;
            }
            if (freeze) {
                le.setDeltaMovement(Vec3.ZERO);
                if (le instanceof Mob m) m.getNavigation().stop();
            } else {
                Vec3 d = le.getDeltaMovement();
                le.setDeltaMovement(d.x * 0.8, 0.0, d.z * 0.8);
                le.fallDistance = 0F;
            }
            le.hurtMarked = true;
        }
    }

    private static void tickLaunches(MinecraftServer server, int now) {
        Iterator<Launch> it = LAUNCHES.iterator();
        while (it.hasNext()) {
            Launch l = it.next();
            ServerLevel level = server.getLevel(l.dim);
            Entity e = level == null ? null : level.getEntity(l.id);
            int age = now - l.start;
            if (!(e instanceof LivingEntity t) || !t.isAlive() || age > 60) {
                it.remove();
                continue;
            }
            if (!t.onGround()) l.hadAir = true;

            if (l.spike) {
                if (age >= 2 && t.onGround()) {
                    spikeImpact(server, level, t, l);
                    it.remove();
                }
                continue;
            }
            if (age >= 2 && age <= 40 && t.horizontalCollision && l.speed > 0.28) {
                wallSlam(server, level, t, l);
                it.remove();
                continue;
            }
            if (l.up && l.hadAir && age >= 6 && t.onGround()) {
                groundCrash(server, level, t, l);
                it.remove();
                continue;
            }
            Vec3 d = t.getDeltaMovement();
            double sp = Math.sqrt(d.x * d.x + d.z * d.z);
            if (sp > 0.02) {
                l.vx = d.x;
                l.vz = d.z;
            }
            l.speed = sp;
        }
    }

    private static DamageSource source(MinecraftServer server, LivingEntity t, UUID owner) {
        ServerPlayer p = owner == null ? null : server.getPlayerList().getPlayer(owner);
        return p != null ? t.damageSources().playerAttack(p) : t.damageSources().flyIntoWall();
    }

    private static void wallSlam(MinecraftServer server, ServerLevel level, LivingEntity t, Launch l) {
        ServerPlayer owner = l.owner == null ? null : server.getPlayerList().getPlayer(l.owner);
        float damage = 3.0F + (float) (l.speed * 9.0);
        t.invulnerableTime = 0;
        t.hurt(source(server, t, l.owner), damage);

        Vec3 dir = new Vec3(l.vx, 0, l.vz);
        dir = dir.lengthSqr() < 1.0E-4 ? Vec3.ZERO : dir.normalize();
        double reach = t.getBbWidth() * 0.5 + 0.3;
        BlockPos wall = BlockPos.containing(t.getX() + dir.x * reach, t.getY() + t.getBbHeight() * 0.5,
                t.getZ() + dir.z * reach);

        if (owner != null && Destruction.enabled()) {
            Destruction.Budget budget = new Destruction.Budget();
            Vec3 vel = new Vec3(dir.x * 0.5, 0.3, dir.z * 0.5);
            double hardness = MayhemConfig.WEAK_HARDNESS.get();
            Destruction.breakAt(owner, wall, hardness, vel, budget);
            Destruction.breakAt(owner, wall.above(), hardness, vel, budget);
            Destruction.breakAt(owner, wall.below(), hardness, vel, budget);
        }

        Vec3 c = t.position().add(dir.scale(reach)).add(0, t.getBbHeight() * 0.5, 0);
        Fx.sound(level, c, MayhemSounds.HIT_HEAVY.get(), 1.0F, 0.8F);
        Fx.particle(level, ParticleTypes.EXPLOSION, c, 1, 0, 0);
        Fx.particle(level, ParticleTypes.CRIT, c, 18, 0.4, 0.2);
        Fx.impact(level, c, 0.6F, 4F, 20F, ImpactPacket.FLASH_NONE, Fx.TXT_CRACK, 0);
    }

    private static void groundCrash(MinecraftServer server, ServerLevel level, LivingEntity t, Launch l) {
        float damage = 3.0F + (float) Math.min(6.0, Math.abs(t.getDeltaMovement().y) * 4.0 + 2.0);
        t.invulnerableTime = 0;
        t.hurt(source(server, t, l.owner), damage);
        Vec3 c = t.position().add(0, 0.2, 0);
        Fx.ring(level, ParticleTypes.CLOUD, c, 0.5, 14, 0.25, 0.03);
        Fx.blockDust(level, level.getBlockState(BlockPos.containing(c).below()), c, 14);
        Fx.sound(level, c, MayhemSounds.HIT_HEAVY.get(), 0.8F, 0.9F);
        Fx.impact(level, c, 0.35F, 2F, 16F);
    }

    private static void spikeImpact(MinecraftServer server, ServerLevel level, LivingEntity t, Launch l) {
        ServerPlayer owner = l.owner == null ? null : server.getPlayerList().getPlayer(l.owner);
        Vec3 pos = t.position();
        float base = owner != null ? UnarmedCombat.baseDamage(owner) : 4F;
        float damage = base * 2.6F * MayhemConfig.DAMAGE_SCALE.get().floatValue();
        t.invulnerableTime = 0;
        t.hurt(source(server, t, l.owner), damage);

        double radius = 3.5;
        if (owner != null) {
            AABB box = new AABB(pos, pos).inflate(radius, 2.0, radius);
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, box,
                    o -> o != t && TargetFinder.valid(owner, o) && (!(o instanceof net.minecraft.world.entity.player.Player)
                            || MayhemConfig.PVP_SPECIALS.get()))) {
                other.invulnerableTime = 0;
                other.hurt(level.damageSources().playerAttack(owner), damage * 0.4F);
                other.knockback(0.9, pos.x - other.getX(), pos.z - other.getZ());
                other.setDeltaMovement(other.getDeltaMovement().add(0, 0.45, 0));
                other.hurtMarked = true;
            }
            Destruction.shockCrater(owner, pos, radius, MayhemConfig.CRATER_DEPTH.get(),
                    MayhemConfig.SLAM_HARDNESS.get());
        }
        Fx.sound(level, pos, MayhemSounds.BOOM.get(), 1.0F, 1.1F);
        Fx.particle(level, ParticleTypes.EXPLOSION, pos.add(0, 0.5, 0), 3, 0.6, 0);
        Fx.impact(level, pos, 0.85F, 6F, 32F, ImpactPacket.FLASH_WHITE, Fx.TXT_DOGO, 8);
        if (MayhemConfig.HIT_STOP.get()) freeze(t, 4);
    }

    public static void clear() {
        LAUNCHES.clear();
        SUSPENDS.clear();
        FREEZES.clear();
    }
}
