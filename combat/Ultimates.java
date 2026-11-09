package com.mayhem.combat;

import com.mayhem.config.MayhemConfig;
import com.mayhem.fx.Fx;
import com.mayhem.fx.MayhemSounds;
import com.mayhem.network.CinematicPacket;
import com.mayhem.network.ImpactPacket;
import com.mayhem.network.MayhemNetwork;
import com.mayhem.progression.PlayerProgression;
import com.mayhem.world.Destruction;
import com.mayhem.world.Dynamics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * The two full-meter ultimates, each a server-driven state machine that also scripts the client cinematic:
 * <ul>
 *   <li><b>Barrage</b>: flash-step to a target, 14-hit rush, launch, vanish-and-reappear above, spike into a crater.</li>
 *   <li><b>Meteor</b>: charge, rocket skyward, hang, dive and detonate in a huge crater.</li>
 * </ul>
 */
public final class Ultimates {
    // barrage phases
    private static final int B_OPEN = 0, B_RUSH = 1, B_LAUNCH = 2, B_DIVE = 3, B_RECOVER = 4;
    // meteor phases
    private static final int M_CHARGE = 0, M_ASCEND = 1, M_HANG = 2, M_DIVE = 3, M_RECOVER = 4;

    private static final int BARRAGE_HITS = 14;

    private Ultimates() {}

    // ================================================================== start / end

    public static void start(ServerPlayer p, boolean meteor, int lockId) {
        if (!MayhemConfig.ULTIMATES_ENABLED.get()) return;
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping() || p.isInWater()) return;
        CombatState s = CombatState.of(p);
        if (s.busy() || s.slam != CombatState.Slam.NONE) return;
        if (s.momentum < 99.5F) {
            p.displayClientMessage(Component.translatable("message.mayhem.ult_not_ready"), true);
            return;
        }
        int now = p.server.getTickCount();
        SpecialMoves.dropGuard(p, s);
        ServerLevel level = p.serverLevel();

        if (!meteor) {
            LivingEntity t = TargetFinder.byId(p, lockId, 16.0);
            if (t == null || !TargetFinder.validForSpecials(p, t, true)) {
                t = TargetFinder.nearest(p, 10.0, true);
            }
            if (t == null) {
                p.displayClientMessage(Component.translatable("message.mayhem.no_target"), true);
                return;
            }
            Vec3 spot = Teleports.findSpot(level, p, t, Teleports.angleToPlayer(p, t),
                    new double[]{0, 25, -25, 60, -60, 120, -120, 180}, new double[]{2.2, 2.8, 3.4});
            if (spot == null && TargetFinder.distance(p, t) > 3.4) {
                p.displayClientMessage(Component.translatable("message.mayhem.no_target"), true);
                return;
            }
            if (spot != null) Teleports.blink(p, spot, t.getBoundingBox().getCenter());
            s.ult = CombatState.Ult.BARRAGE;
            s.ultTarget = t.getUUID();
            s.ultAnchor = p.position();
            setPhase(s, B_OPEN, now);
            send(p, CinematicPacket.START, CinematicPacket.BARRAGE, t.getId(), CinematicPacket.OPEN);
        } else {
            s.ult = CombatState.Ult.METEOR;
            s.ultTarget = null;
            s.ultAnchor = p.position();
            setPhase(s, M_CHARGE, now);
            send(p, CinematicPacket.START, CinematicPacket.METEOR, -1, CinematicPacket.CHARGE);
        }
        s.ultStart = now;
        s.momentum = 0F;
        CombatState.syncMomentum(p, s);
        s.noFallUntil = now + 400;
        Fx.anim(p, MoveType.CHARGE, 0);
        Fx.sound(level, p.position(), MayhemSounds.CHARGE.get(), 1.0F, 1.0F);
        Fx.impactTo(p, p.position(), 0.3F, 0F, ImpactPacket.FLASH_DARK, Fx.TXT_RUMBLE, 10);
    }

    public static boolean active(CombatState s) {
        return s.ult != CombatState.Ult.NONE;
    }

    /** Ends the ultimate (also used when the player dies or logs out). */
    public static void end(ServerPlayer p, CombatState s) {
        if (s.ult == CombatState.Ult.NONE) return;
        s.ult = CombatState.Ult.NONE;
        s.ultTarget = null;
        p.setNoGravity(false);
        s.noFallUntil = p.server.getTickCount() + 30;
        send(p, CinematicPacket.END, (byte) 0, -1, (byte) 0);
    }

    public static void abort(ServerPlayer p) {
        CombatState s = CombatState.peek(p);
        if (s != null) end(p, s);
    }

    private static void send(ServerPlayer p, byte kind, byte type, int targetId, byte beat) {
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new CinematicPacket(kind, type, p.getId(), targetId, beat));
    }

    private static void setPhase(CombatState s, int phase, int now) {
        s.ultPhase = phase;
        s.ultPhaseStart = now;
    }

    // ================================================================== tick

    public static void tick(ServerPlayer p, CombatState s, int now) {
        if (!p.isAlive() || p.isSpectator()) {
            end(p, s);
            return;
        }
        if (now - s.ultStart > 300) { // hard safety timeout
            end(p, s);
            return;
        }
        if (s.ult == CombatState.Ult.BARRAGE) tickBarrage(p, s, now);
        else if (s.ult == CombatState.Ult.METEOR) tickMeteor(p, s, now);
    }

    private static float damage(ServerPlayer p, float mult) {
        return UnarmedCombat.baseDamage(p) * mult * MayhemConfig.ULTIMATE_DAMAGE_SCALE.get().floatValue()
                * MayhemConfig.DAMAGE_SCALE.get().floatValue();
    }

    private static void aura(ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions type,
                             int count, double radius, double rise) {
        for (int i = 0; i < count; i++) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            level.sendParticles(type, pos.x + Math.cos(a) * radius, pos.y + 0.1 + level.random.nextDouble() * 1.6,
                    pos.z + Math.sin(a) * radius, 0, 0, 1, 0, rise);
        }
    }

    // ================================================================== BARRAGE

    private static void tickBarrage(ServerPlayer p, CombatState s, int now) {
        ServerLevel level = p.serverLevel();
        Entity found = s.ultTarget == null ? null : level.getEntity(s.ultTarget);
        if (!(found instanceof LivingEntity t) || !t.isAlive()) {
            if (s.ultPhase < B_DIVE) {
                // target died or vanished early: end with a flourish
                Fx.sound(level, p.position(), MayhemSounds.BOOM.get(), 0.8F, 1.2F);
                Fx.impact(level, p.position(), 0.6F, 6F, 30F, ImpactPacket.FLASH_WHITE, Fx.TXT_BOOM, 8);
                end(p, s);
            } else if (now - s.ultPhaseStart > 6) {
                end(p, s);
            }
            return;
        }
        int j = now - s.ultPhaseStart;

        if (s.ultPhase <= B_RUSH) {
            holdAttacker(p, s, t);
            t.setDeltaMovement(Vec3.ZERO);
            t.hurtMarked = true;
            if (t instanceof Mob m) m.getNavigation().stop();
        }

        switch (s.ultPhase) {
            case B_OPEN -> {
                aura(level, p.position(), ParticleTypes.END_ROD, 4, 1.1, 0.14);
                if (j % 4 == 0) Fx.impactTo(p, p.position(), 0.12F, 0F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 0);
                if (j >= 10) {
                    setPhase(s, B_RUSH, now);
                    send(p, CinematicPacket.BEAT, CinematicPacket.BARRAGE, t.getId(), CinematicPacket.ORBIT);
                }
            }
            case B_RUSH -> {
                if (j % 2 == 0 && j / 2 < BARRAGE_HITS) barrageHit(p, t, j / 2);
                if (j >= BARRAGE_HITS * 2) launch(p, s, t, now);
            }
            case B_LAUNCH -> {
                if (j % 3 == 0) Fx.particle(level, ParticleTypes.CLOUD, t.position().add(0, 0.6, 0), 3, 0.25, 0.01);
                boolean apex = j >= 5 && t.getDeltaMovement().y <= 0.05;
                if (apex || j >= 26) beginDive(p, s, t, now);
            }
            case B_DIVE -> {
                Fx.particle(level, ParticleTypes.FLAME, t.position().add(0, 0.8, 0), 4, 0.25, 0.02);
                Fx.particle(level, ParticleTypes.CLOUD, p.position().add(0, 0.8, 0), 3, 0.25, 0.02);
                t.setDeltaMovement(0, Math.min(t.getDeltaMovement().y, -2.4), 0);
                t.hurtMarked = true;
                if ((t.onGround() && j >= 2) || j >= 40) barrageImpact(p, s, t, now);
            }
            case B_RECOVER -> {
                if (j >= 24) end(p, s);
            }
            default -> end(p, s);
        }
    }

    private static void holdAttacker(ServerPlayer p, CombatState s, LivingEntity t) {
        if (p.position().distanceToSqr(s.ultAnchor) > 0.64) {
            float[] a = Teleports.aim(s.ultAnchor.add(0, p.getEyeHeight(), 0), t.getBoundingBox().getCenter());
            p.connection.teleport(s.ultAnchor.x, s.ultAnchor.y, s.ultAnchor.z, a[0], a[1]);
        }
    }

    private static void barrageHit(ServerPlayer p, LivingEntity t, int i) {
        ServerLevel level = p.serverLevel();
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage(p, 0.6F + i * 0.02F));
        p.swing(InteractionHand.MAIN_HAND, true);
        Fx.anim(p, (i & 1) == 0 ? MoveType.BARRAGE_L : MoveType.BARRAGE_R, i & 1);
        Fx.anim(t, MoveType.JOLT, i & 1);

        Vec3 c = t.position().add((level.random.nextDouble() - 0.5) * 0.7,
                t.getBbHeight() * 0.55 + (level.random.nextDouble() - 0.5) * 0.6,
                (level.random.nextDouble() - 0.5) * 0.7);
        Fx.sound(level, c, MayhemSounds.BARRAGE_HIT.get(), 0.9F, 0.9F + level.random.nextFloat() * 0.4F);
        Fx.particle(level, ParticleTypes.CRIT, c, 6, 0.25, 0.2);
        if (i % 3 == 0) Fx.particle(level, ParticleTypes.EXPLOSION, c, 1, 0, 0);
        byte flash = i % 5 == 4 ? ImpactPacket.FLASH_WHITE : ImpactPacket.FLASH_NONE;
        Fx.impactTo(p, c, 0.16F, 2.5F + (i % 4 == 0 ? 3F : 0F), flash, i % 3 == 0 ? Fx.TXT_ORA : Fx.TXT_NONE, 3);
    }

    private static void launch(ServerPlayer p, CombatState s, LivingEntity t, int now) {
        ServerLevel level = p.serverLevel();
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage(p, 2.0F));
        t.setDeltaMovement(0, 1.55, 0);
        t.hurtMarked = true;
        Fx.anim(p, MoveType.UPPERCUT, 0);
        p.swing(InteractionHand.MAIN_HAND, true);
        Fx.anim(t, MoveType.TUMBLE, 0);
        Vec3 c = t.position().add(0, t.getBbHeight() * 0.5, 0);
        Fx.sound(level, c, MayhemSounds.HIT_HEAVY.get(), 1.0F, 0.7F);
        Fx.sound(level, c, MayhemSounds.WHOOSH_HEAVY.get(), 1.0F, 0.7F);
        Fx.particle(level, ParticleTypes.EXPLOSION, c, 2, 0.4, 0);
        Fx.ring(level, ParticleTypes.CLOUD, t.position().add(0, 0.2, 0), 0.5, 20, 0.35, 0.05);
        Fx.impact(level, c, 0.7F, 8F, 32F, ImpactPacket.FLASH_WHITE, Fx.TXT_BAM, 10);
        setPhase(s, B_LAUNCH, now);
        send(p, CinematicPacket.BEAT, CinematicPacket.BARRAGE, t.getId(), CinematicPacket.LAUNCH);
    }

    private static void beginDive(ServerPlayer p, CombatState s, LivingEntity t, int now) {
        ServerLevel level = p.serverLevel();
        Vec3 above = t.position().add(0, t.getBbHeight() + 0.7, 0);
        if (Teleports.validSpot(level, p, above, true)) {
            Teleports.blink(p, above, t.getBoundingBox().getCenter());
        }
        p.setDeltaMovement(0, -2.6, 0);
        p.hurtMarked = true;
        t.setDeltaMovement(0, -2.8, 0);
        t.hurtMarked = true;
        Fx.anim(p, MoveType.SLAM_DIVE, 0);
        Fx.sound(level, p.position(), MayhemSounds.WHOOSH_HEAVY.get(), 1.0F, 0.6F);
        Fx.impactTo(p, p.position(), 0.2F, 12F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 14);
        setPhase(s, B_DIVE, now);
        send(p, CinematicPacket.BEAT, CinematicPacket.BARRAGE, t.getId(), CinematicPacket.DIVE);
    }

    private static void barrageImpact(ServerPlayer p, CombatState s, LivingEntity t, int now) {
        ServerLevel level = p.serverLevel();
        Vec3 c = t.position();
        double radius = 7.0;

        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage(p, 4.5F));
        shockwave(p, c, radius, t, 2.0F);
        Destruction.shockCrater(p, c, 6.0, Math.min(6, MayhemConfig.CRATER_DEPTH.get() + 2),
                MayhemConfig.ULTIMATE_HARDNESS.get());
        bigBlast(level, c, 1.0F);
        Fx.impact(level, c, 1.0F, 12F, 56F, ImpactPacket.FLASH_WHITE, Fx.TXT_DOGO, 14);
        Fx.anim(p, MoveType.SLAM_LAND, 0);

        if (t instanceof Enemy) {
            PlayerProgression.awardCombat(p, 40);
            if (t.isDeadOrDying()) PlayerProgression.awardDefeat(p, t);
        }
        PlayerProgression.recordFinisher(p);
        if (MayhemConfig.HIT_STOP.get() && !t.isDeadOrDying()) Dynamics.freeze(t, 8);
        p.fallDistance = 0F;
        setPhase(s, B_RECOVER, now);
        send(p, CinematicPacket.BEAT, CinematicPacket.BARRAGE, t.getId(), CinematicPacket.IMPACT);
    }

    // ================================================================== METEOR

    private static void tickMeteor(ServerPlayer p, CombatState s, int now) {
        ServerLevel level = p.serverLevel();
        int j = now - s.ultPhaseStart;

        switch (s.ultPhase) {
            case M_CHARGE -> {
                aura(level, p.position(), ParticleTypes.FLAME, 5, 1.3, 0.16);
                aura(level, p.position(), ParticleTypes.END_ROD, 2, 0.9, 0.2);
                if (j % 6 == 0) {
                    Fx.ring(level, ParticleTypes.CLOUD, p.position().add(0, 0.1, 0), 0.5, 16, 0.3, 0.0);
                    Fx.blockDust(level, level.getBlockState(p.blockPosition().below()), p.position().add(0, 0.2, 0), 8);
                    Fx.impactTo(p, p.position(), 0.2F, 0F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 0);
                }
                if (j >= 26) {
                    p.setDeltaMovement(0, 1.7, 0);
                    p.hurtMarked = true;
                    Fx.anim(p, MoveType.SLAM_LEAP, 0);
                    Fx.sound(level, p.position(), MayhemSounds.WHOOSH_HEAVY.get(), 1.0F, 0.5F);
                    Fx.ring(level, ParticleTypes.CLOUD, p.position().add(0, 0.2, 0), 0.6, 24, 0.5, 0.05);
                    Fx.impact(level, p.position(), 0.7F, 10F, 40F, ImpactPacket.FLASH_WHITE, Fx.TXT_NONE, 14);
                    setPhase(s, M_ASCEND, now);
                    send(p, CinematicPacket.BEAT, CinematicPacket.METEOR, -1, CinematicPacket.ASCEND);
                }
            }
            case M_ASCEND -> {
                p.fallDistance = 0F;
                Fx.particle(level, ParticleTypes.FLAME, p.position().add(0, 0.2, 0), 4, 0.25, 0.03);
                Fx.particle(level, ParticleTypes.CLOUD, p.position().add(0, 0.2, 0), 2, 0.2, 0.02);
                if ((j >= 8 && p.getY() <= s.lastY + 0.0005) || j >= 40) {
                    p.setNoGravity(true);
                    p.setDeltaMovement(Vec3.ZERO);
                    p.hurtMarked = true;
                    Fx.sound(level, p.position(), MayhemSounds.CHARGE.get(), 0.8F, 1.2F);
                    setPhase(s, M_HANG, now);
                    send(p, CinematicPacket.BEAT, CinematicPacket.METEOR, -1, CinematicPacket.HANG);
                }
            }
            case M_HANG -> {
                p.setDeltaMovement(Vec3.ZERO);
                p.hurtMarked = true;
                p.fallDistance = 0F;
                aura(level, p.position(), ParticleTypes.FLAME, 5, 1.2, 0.1);
                if (j % 3 == 0) Fx.ring(level, ParticleTypes.FLAME, p.position().add(0, 0.9, 0), 0.8, 18, 0.25, 0.0);
                if (j == 2) Fx.impactTo(p, p.position(), 0.35F, 0F, ImpactPacket.FLASH_DARK, Fx.TXT_RUMBLE, 12);
                if (j >= 16) {
                    p.setNoGravity(false);
                    Vec3 look = p.getLookAngle();
                    Vec3 flat = new Vec3(look.x, 0, look.z);
                    flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
                    p.setDeltaMovement(flat.x * 0.2, -3.2, flat.z * 0.2);
                    p.hurtMarked = true;
                    Fx.anim(p, MoveType.SLAM_DIVE, 0);
                    Fx.sound(level, p.position(), MayhemSounds.WHOOSH_HEAVY.get(), 1.0F, 0.5F);
                    Fx.impactTo(p, p.position(), 0.3F, 14F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 16);
                    setPhase(s, M_DIVE, now);
                    send(p, CinematicPacket.BEAT, CinematicPacket.METEOR, -1, CinematicPacket.DIVE);
                }
            }
            case M_DIVE -> {
                p.fallDistance = 0F;
                Vec3 at = p.position().add(0, 1.0, 0);
                Fx.particle(level, ParticleTypes.FLAME, at, 8, 0.4, 0.03);
                Fx.particle(level, ParticleTypes.LAVA, at, 2, 0.4, 0.0);
                Fx.particle(level, ParticleTypes.LARGE_SMOKE, at.add(0, 1.2, 0), 4, 0.35, 0.02);
                if ((p.onGround() && j >= 3) || j >= 60) meteorImpact(p, s, now);
            }
            case M_RECOVER -> {
                if (j >= 28) end(p, s);
            }
            default -> end(p, s);
        }
    }

    private static void meteorImpact(ServerPlayer p, CombatState s, int now) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        double radius = 8.5;

        shockwave(p, c, radius, null, 6.0F);
        Destruction.shockCrater(p, c, 8.0, Math.min(6, MayhemConfig.CRATER_DEPTH.get() + 3),
                MayhemConfig.ULTIMATE_HARDNESS.get());
        bigBlast(level, c, 1.5F);
        Fx.ring(level, ParticleTypes.FLAME, c.add(0, 0.3, 0), 1.0, 40, 0.6, 0.04);
        Fx.ring(level, ParticleTypes.LARGE_SMOKE, c.add(0, 0.5, 0), 2.0, 32, 0.4, 0.06);
        Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, c.add(0, 1, 0), 1, 0, 0);
        Fx.impact(level, c, 1.0F, 14F, 64F, ImpactPacket.FLASH_WHITE, Fx.TXT_BOOM, 16);
        Fx.anim(p, MoveType.SLAM_LAND, 0);

        PlayerProgression.recordFinisher(p);
        p.fallDistance = 0F;
        setPhase(s, M_RECOVER, now);
        send(p, CinematicPacket.BEAT, CinematicPacket.METEOR, -1, CinematicPacket.IMPACT);
    }

    // ================================================================== shared effects

    /** Damages and throws everything around {@code center} (except {@code exclude}); strength scales damage. */
    private static void shockwave(ServerPlayer p, Vec3 center, double radius, LivingEntity exclude, float mult) {
        ServerLevel level = p.serverLevel();
        AABB box = new AABB(center, center).inflate(radius, 3.0, radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                o -> o != exclude && TargetFinder.valid(p, o)
                        && (!(o instanceof Player) || MayhemConfig.ULTIMATES_HIT_PLAYERS.get())
                        && o.distanceToSqr(center) <= radius * radius)) {
            double d = Math.sqrt(e.distanceToSqr(center));
            float falloff = (float) (1.0 - 0.6 * d / radius);
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), damage(p, mult * falloff));
            e.knockback(1.3 * falloff * MayhemConfig.KNOCKBACK_SCALE.get(), center.x - e.getX(), center.z - e.getZ());
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9 * falloff, 0));
            e.hurtMarked = true;
            Dynamics.launch(e, p, true);
            Fx.anim(e, MoveType.TUMBLE, 0);
            if (e instanceof Enemy) {
                PlayerProgression.awardCombat(p, 20);
                if (e.isDeadOrDying()) PlayerProgression.awardDefeat(p, e);
            }
        }
    }

    private static void bigBlast(ServerLevel level, Vec3 c, float size) {
        Fx.sound(level, c, MayhemSounds.BOOM.get(), 1.0F, 0.85F);
        Fx.sound(level, c, MayhemSounds.HIT_HEAVY.get(), 1.0F, 0.6F);
        Fx.particle(level, ParticleTypes.EXPLOSION, c.add(0, 0.8, 0), (int) (6 * size), 1.2 * size, 0);
        Fx.ring(level, ParticleTypes.CLOUD, c.add(0, 0.2, 0), 1.0, 40, 0.7 * size, 0.02);
        Fx.ring(level, ParticleTypes.CRIT, c.add(0, 0.6, 0), 0.8, 32, 0.55 * size, 0.12);
        Fx.ring(level, ParticleTypes.END_ROD, c.add(0, 0.4, 0), 0.6, 24, 0.4 * size, 0.2);
    }
}
