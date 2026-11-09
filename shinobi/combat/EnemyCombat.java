package com.mayhem.shinobi.combat;

import com.mayhem.combat.MoveType;
import com.mayhem.fx.Fx;
import com.mayhem.progression.MobLevelHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Combat brain for hostiles and rogue ninjas: telegraphed strikes + optional jinchūriki bursts. */
public final class EnemyCombat {
    private static final Map<UUID, Integer> CD = new HashMap<>();
    private static final List<Telegraph> PENDING = new ArrayList<>();

    private record Telegraph(UUID mob, UUID target, MoveType move, int fireAt, boolean jinchuriki) {}

    public static final String TAG_ROGUE = "mayhem_rogue_ninja";
    public static final String TAG_JIN = "mayhem_enemy_jin";
    public static final String NBT_BEAST = "mayhemEnemyBeast";

    private EnemyCombat() {}

    public static boolean isCombatant(Mob m) {
        if (m instanceof Enemy) return true;
        return m.getTags().contains(TAG_ROGUE) || m.getPersistentData().getBoolean(TAG_ROGUE);
    }

    public static boolean isJinchuriki(Mob m) {
        return m.getTags().contains(TAG_JIN) || m.getPersistentData().contains(NBT_BEAST);
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % 12 == 0) scan(server, now);
        tickPending(server, now);
        if (now % 200 == 0) CD.entrySet().removeIf(e -> e.getValue() < now - 100);
        if (now % 10 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), EnemyCombat::isJinchuriki)) {
                    if (m.isAlive()) {
                        p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + 1.2, m.getZ(), 2, 0.35, 0.5, 0.35, 0.01);
                    }
                }
            }
        }
    }

    private static void scan(MinecraftServer server, int now) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            ServerLevel level = p.serverLevel();
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(14),
                    mob -> mob.isAlive() && isCombatant(mob) && mob.getTarget() instanceof ServerPlayer)) {
                if (!(m.getTarget() instanceof ServerPlayer target)) continue;
                if (CD.getOrDefault(m.getUUID(), 0) > now) continue;
                double d = m.distanceTo(target);
                if (d > 12 || d < 1.2) continue;
                MoveType move = pickMove(m, target, d);
                int delay = move.isKick() || move.isAerial() ? 12 : 8;
                PENDING.add(new Telegraph(m.getUUID(), target.getUUID(), move, now + delay, isJinchuriki(m)));
                CD.put(m.getUUID(), now + delay + move.ticks + 15 + (isJinchuriki(m) ? 5 : 10));
                level.sendParticles(ParticleTypes.CRIT, m.getX(), m.getY() + 1.0, m.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                if (isJinchuriki(m)) {
                    level.sendParticles(ParticleTypes.FLAME, m.getX(), m.getY() + 1.2, m.getZ(), 8, 0.4, 0.5, 0.4, 0.02);
                }
            }
        }
    }

    private static MoveType pickMove(Mob m, ServerPlayer target, double dist) {
        boolean jin = isJinchuriki(m);
        boolean rogue = m.getTags().contains(TAG_ROGUE) || m.getPersistentData().getBoolean(TAG_ROGUE);
        int roll = m.getRandom().nextInt(100);
        if (dist > 6.5) return (rogue || jin) ? MoveType.DASH_STRIKE : MoveType.FLASH_STEP;
        if (!m.onGround() || (!target.onGround() && roll < 40)) return roll < 50 ? MoveType.FLYING_KICK : MoveType.SPIKE;
        if (roll < 12) return MoveType.SPIN_KICK;
        if (roll < 22) return MoveType.ROUNDHOUSE;
        if (roll < 32) return MoveType.HOOK;
        if (roll < 42) return MoveType.UPPERCUT;
        if (roll < 52) return MoveType.KNEE;
        if (roll < 60 && (rogue || jin)) return MoveType.WHIRLWIND;
        if (roll < 70 && jin) return MoveType.DOUBLE_PALM;
        if (roll < 80) return MoveType.CROSS;
        return MoveType.JAB;
    }

    private static void tickPending(MinecraftServer server, int now) {
        Iterator<Telegraph> it = PENDING.iterator();
        while (it.hasNext()) {
            Telegraph t = it.next();
            if (now < t.fireAt()) continue;
            it.remove();
            for (ServerLevel level : server.getAllLevels()) {
                if (!(level.getEntity(t.mob()) instanceof Mob mob) || !mob.isAlive()) break;
                if (!(level.getEntity(t.target()) instanceof ServerPlayer target) || !target.isAlive()) break;
                execute(level, mob, target, t.move(), t.jinchuriki());
                break;
            }
        }
    }

    private static void execute(ServerLevel level, Mob mob, ServerPlayer target, MoveType move, boolean jin) {
        Fx.anim(mob, move, mob.getRandom().nextInt(2));
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mob.position().add(0, mob.getBbHeight() * 0.5, 0));
        double dist = to.length();
        Vec3 dir = dist < 0.001 ? mob.getLookAngle() : to.normalize();
        if (move == MoveType.DASH_STRIKE || move == MoveType.FLASH_STEP || move == MoveType.FLYING_KICK) {
            mob.setDeltaMovement(dir.scale(1.4).add(0, 0.15, 0));
            mob.hurtMarked = true;
        }
        if (move == MoveType.UPPERCUT || move == MoveType.SPIN_KICK) {
            mob.setDeltaMovement(mob.getDeltaMovement().add(0, 0.35, 0));
            mob.hurtMarked = true;
        }
        double reach = move == MoveType.DASH_STRIKE || move == MoveType.FLYING_KICK ? 4.5 : 3.2;
        float dmg = baseDamage(mob, move, jin);
        AABB box = mob.getBoundingBox().inflate(reach, 1.2, reach);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x == target)) {
            e.hurt(mob.damageSources().mobAttack(mob), dmg);
            e.knockback(move.isKick() ? 0.7 : 0.4, -dir.x, -dir.z);
            if (move == MoveType.UPPERCUT) e.setDeltaMovement(e.getDeltaMovement().add(0, 0.45, 0));
            if (jin && mob.getRandom().nextInt(3) == 0) {
                e.setSecondsOnFire(2);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.9f, 0.9f + mob.getRandom().nextFloat() * 0.2f);
        if (jin) level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 1, mob.getZ(), 12, 0.5, 0.6, 0.5, 0.03);
    }

    private static float baseDamage(Mob mob, MoveType move, boolean jin) {
        if (mob instanceof com.mayhem.shinobi.npc.WandererEntity w) return w.strikeDamage(move);
        float base = 3.0F;
        try { base += Math.min(12F, MobLevelHandler.getLevel(mob) * 0.35F); } catch (Throwable ignored) {}
        if (move.isKick() || move == MoveType.UPPERCUT || move == MoveType.WHIRLWIND) base *= 1.35F;
        if (move == MoveType.JAB) base *= 0.85F;
        if (jin) base *= 1.25F;
        return base;
    }
}
