package com.mayhem.combat;

import com.mayhem.config.MayhemConfig;
import com.mayhem.fx.Fx;
import com.mayhem.progression.MobLevelHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gives level 2+ melee mobs a readable attack pattern: a ~0.7s glowing telegraph, then a fast lunge.
 * The lunge goes through normal damage events, so dodging, parrying and guarding all work on it.
 */
public final class MobBrain {
    private static final int TELEGRAPH_TICKS = 14;
    private static final int LUNGE_TICKS = 10;

    private static final class Pending {
        UUID mob, target;
        int fireTick;
    }

    private static final class Active {
        UUID mob, target;
        int end;
        boolean hit;
    }

    private static final Map<UUID, Integer> READY = new HashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final List<Active> ACTIVE = new ArrayList<>();

    private MobBrain() {}

    public static void tick(MinecraftServer server) {
        if (!MayhemConfig.MOB_LUNGES.get()) return;
        int now = server.getTickCount();
        if (now % 10 == 0) scan(server, now);
        if (!PENDING.isEmpty()) tickPending(server, now);
        if (!ACTIVE.isEmpty()) tickActive(server, now);
        if (now % 400 == 0) READY.values().removeIf(v -> v < now - 200);
    }

    private static void scan(MinecraftServer server, int now) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            ServerLevel level = p.serverLevel();
            int started = 0;
            AABB box = p.getBoundingBox().inflate(10.0);
            for (Mob m : level.getEntitiesOfClass(Mob.class, box, mob -> mob instanceof Enemy && mob.isAlive())) {
                if (started >= 2) break;
                if (m.getTarget() != p || m instanceof RangedAttackMob || m instanceof Creeper) continue;
                if (MobLevelHandler.getLevel(m) < 2 || !m.onGround()) continue;
                double dist = m.distanceTo(p);
                if (dist < 3.5 || dist > 9.0 || !m.hasLineOfSight(p)) continue;
                if (READY.getOrDefault(m.getUUID(), 0) > now) continue;

                READY.put(m.getUUID(), now + 140 + level.random.nextInt(80));
                Pending pend = new Pending();
                pend.mob = m.getUUID();
                pend.target = p.getUUID();
                pend.fireTick = now + TELEGRAPH_TICKS;
                PENDING.add(pend);
                started++;
                level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.ENDERMAN_STARE,
                        SoundSource.HOSTILE, 0.7F, 1.6F);
            }
        }
    }

    private static void tickPending(MinecraftServer server, int now) {
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pend = it.next();
            ServerPlayer target = server.getPlayerList().getPlayer(pend.target);
            Entity e = target == null ? null : target.serverLevel().getEntity(pend.mob);
            if (!(e instanceof Mob m) || !m.isAlive() || target == null || !target.isAlive()
                    || m.level() != target.level()) {
                it.remove();
                continue;
            }
            ServerLevel level = target.serverLevel();
            m.getNavigation().stop();
            m.getLookControl().setLookAt(target, 60F, 60F);

            if (now < pend.fireTick) {
                if ((pend.fireTick - now) % 2 == 0) {
                    Vec3 head = m.position().add(0, m.getBbHeight() + 0.3, 0);
                    Fx.particle(level, ParticleTypes.ANGRY_VILLAGER, head, 1, 0.2, 0);
                    Fx.ring(level, ParticleTypes.FLAME, m.position().add(0, 0.1, 0), 0.6, 8, 0.05, 0.02);
                }
                continue;
            }

            Vec3 dir = new Vec3(target.getX() - m.getX(), 0, target.getZ() - m.getZ());
            if (dir.lengthSqr() > 1.0E-4) {
                dir = dir.normalize();
                m.setDeltaMovement(dir.x * 1.15, 0.25, dir.z * 1.15);
                m.hurtMarked = true;
            }
            Active a = new Active();
            a.mob = pend.mob;
            a.target = pend.target;
            a.end = now + LUNGE_TICKS;
            ACTIVE.add(a);
            level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.PHANTOM_SWOOP,
                    SoundSource.HOSTILE, 0.9F, 0.8F);
            it.remove();
        }
    }

    private static void tickActive(MinecraftServer server, int now) {
        Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            ServerPlayer target = server.getPlayerList().getPlayer(a.target);
            Entity e = target == null ? null : target.serverLevel().getEntity(a.mob);
            if (now >= a.end || a.hit || !(e instanceof Mob m) || !m.isAlive() || target == null || !target.isAlive()
                    || m.level() != target.level()) {
                it.remove();
                continue;
            }
            if (m.distanceTo(target) <= m.getBbWidth() * 0.5 + 1.4) {
                a.hit = true; // one strike per lunge; goes through attack events (dodge / parry / guard)
                m.doHurtTarget((LivingEntity) target);
            }
        }
    }

    public static void clear() {
        READY.clear();
        PENDING.clear();
        ACTIVE.clear();
    }
}
