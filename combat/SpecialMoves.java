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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Flash-step dodge, guard / parry, perfect-dodge counter and the leap-and-dive ground slam. */
public final class SpecialMoves {
    private static final int PERFECT_WINDOW = 5;

    private SpecialMoves() {}

    // ================================================================== dodge / flash step

    public static void dodge(ServerPlayer p, float fwd, float left, int lockId) {
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping()) return;
        CombatState s = CombatState.of(p);
        int now = p.server.getTickCount();
        if (now < s.dodgeReady || s.busy() || s.slam != CombatState.Slam.NONE) return;
        dropGuard(p, s);

        ServerLevel level = p.serverLevel();
        if (fwd == 0F && left == 0F) fwd = -1F; // no input: hop backwards

        s.dodgeReady = now + MayhemConfig.DODGE_COOLDOWN.get();
        s.dodgeStart = now;
        s.dodgeUntil = now + MayhemConfig.DODGE_IFRAMES.get();
        s.perfectUsed = false;

        boolean teleported = false;
        LivingEntity lock = TargetFinder.byId(p, lockId, 14.0);
        if (lock != null && TargetFinder.validForSpecials(p, lock, false)) {
            teleported = flashStepAround(p, lock, fwd, left);
        }

        if (!teleported) {
            double yaw = Math.toRadians(p.getYRot());
            double dx = left * Math.cos(yaw) - fwd * Math.sin(yaw);
            double dz = fwd * Math.cos(yaw) + left * Math.sin(yaw);
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-4) return;
            dx /= len;
            dz /= len;
            double speed = 1.05;
            p.setDeltaMovement(dx * speed, p.onGround() ? 0.14 : Math.max(0.0, p.getDeltaMovement().y), dz * speed);
            p.hurtMarked = true;
            p.fallDistance = 0F;

            int variant = Math.abs(fwd) >= Math.abs(left) ? (fwd > 0 ? 0 : 1) : (left > 0 ? 2 : 3);
            Fx.anim(p, MoveType.DODGE, variant);
            Fx.trail(level, p.position(), p.position().add(dx * 3.5, 0, dz * 3.5));
            Fx.sound(level, p.position(), MayhemSounds.WHOOSH_LIGHT.get(), 0.8F, 1.2F);
            Fx.impactTo(p, p.position(), 0.05F, 7F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 6);
        }
    }

    private static boolean flashStepAround(ServerPlayer p, LivingEntity lock, float fwd, float left) {
        ServerLevel level = p.serverLevel();
        double dist = TargetFinder.distance(p, lock);
        double base = Teleports.angleToPlayer(p, lock);
        Vec3 spot = null;

        if (Math.abs(left) > Math.abs(fwd)) {
            // sidestep: orbit around the target (angle increases toward the player's left when facing it)
            double off = left > 0 ? 75.0 : -75.0;
            double r = Mth.clamp(dist, 2.5, 5.0);
            spot = Teleports.findSpot(level, p, lock, base, new double[]{off, off * 0.7, off * 1.2, off * 0.4},
                    new double[]{r});
        } else if (fwd > 0 && dist <= 10.0) {
            // forward: vanish and reappear behind the target
            Vec3 look = lock.getLookAngle();
            double behind = Mth.atan2(look.z, look.x) + Math.PI;
            spot = Teleports.findSpot(level, p, lock, behind, new double[]{0, 25, -25, 50, -50, 90, -90},
                    new double[]{1.9, 2.5});
        } else if (fwd < 0 && dist <= 9.0) {
            // backward: leap away along the line from the target
            spot = Teleports.findSpot(level, p, lock, base, new double[]{0, 20, -20, 40, -40},
                    new double[]{Mth.clamp(dist + 4.0, 5.0, 9.0)});
        }
        if (spot == null) return false;

        Teleports.blink(p, spot, lock.getBoundingBox().getCenter());
        Fx.anim(p, MoveType.FLASH_STEP, 0);
        Fx.impactTo(p, spot, 0.1F, 8F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 8);
        return true;
    }

    /** Called when an incoming hit was negated by dodge invulnerability. */
    static void onDodged(ServerPlayer p, CombatState s, DamageSource source, int now) {
        if (s.perfectUsed || now - s.dodgeStart > PERFECT_WINDOW) return;
        s.perfectUsed = true;

        ServerLevel level = p.serverLevel();
        CombatState.addMomentum(p, s, 25F);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0, false, false, false));
        p.displayClientMessage(Component.translatable("message.mayhem.perfect_dodge"), true);
        Fx.particle(level, ParticleTypes.END_ROD, p.position().add(0, 1, 0), 18, 0.4, 0.08);
        Fx.sound(level, p.position(), MayhemSounds.PARRY.get(), 0.7F, 1.5F);

        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity a && TargetFinder.validForSpecials(p, a, false)
                && p.distanceTo(a) <= 12.0F) {
            flashCounter(p, s, a, now);
        }
    }

    private static void flashCounter(ServerPlayer p, CombatState s, LivingEntity a, int now) {
        ServerLevel level = p.serverLevel();
        Vec3 look = a.getLookAngle();
        double behind = Mth.atan2(look.z, look.x) + Math.PI;
        Vec3 spot = Teleports.findSpot(level, p, a, behind,
                new double[]{0, 25, -25, 60, -60, 120, -120, 180}, new double[]{1.8, 2.4});
        if (spot == null) return;

        Teleports.blink(p, spot, a.getBoundingBox().getCenter());
        s.dodgeUntil = Math.max(s.dodgeUntil, now + 10);

        float damage = UnarmedCombat.baseDamage(p) * 3.2F * (1F + s.momentum / 100F * 0.25F)
                * MayhemConfig.DAMAGE_SCALE.get().floatValue();
        a.invulnerableTime = 0;
        a.hurt(p.damageSources().playerAttack(p), damage);
        a.knockback(1.1 * MayhemConfig.KNOCKBACK_SCALE.get(), p.getX() - a.getX(), p.getZ() - a.getZ());
        a.setDeltaMovement(a.getDeltaMovement().add(0, 0.7, 0));
        a.hurtMarked = true;
        Dynamics.launch(a, p, true);
        Fx.anim(a, MoveType.TUMBLE, 0);
        Fx.anim(p, MoveType.COUNTER, 0);

        Vec3 c = a.position().add(0, a.getBbHeight() * 0.55, 0);
        Fx.sound(level, c, MayhemSounds.HIT_HEAVY.get(), 1.0F, 1.1F);
        Fx.particle(level, ParticleTypes.EXPLOSION, c, 1, 0, 0);
        Fx.particle(level, ParticleTypes.CRIT, c, 20, 0.4, 0.3);
        Fx.impact(level, c, 0.55F, 6F, 24F, ImpactPacket.FLASH_DARK, Fx.TXT_ZAP, 9);
        MayhemNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new CinematicPacket(CinematicPacket.START, CinematicPacket.COUNTER, p.getId(), a.getId(),
                        CinematicPacket.OPEN));
        CombatState.addMomentum(p, s, 15F);
        if (a instanceof Enemy) {
            PlayerProgression.awardCombat(p, Math.max(2, (int) damage));
            if (a.isDeadOrDying()) PlayerProgression.awardDefeat(p, a);
        }
        if (MayhemConfig.HIT_STOP.get() && !a.isDeadOrDying()) Dynamics.freeze(a, 4);
    }

    // ================================================================== guard / parry

    public static void guard(ServerPlayer p, boolean down) {
        CombatState s = CombatState.of(p);
        int now = p.server.getTickCount();
        if (!down) {
            dropGuard(p, s);
            return;
        }
        if (s.guarding || !p.isAlive() || p.isSpectator() || s.busy() || s.slam != CombatState.Slam.NONE) return;
        s.guarding = true;
        // A perfect-parry window opens on each press, but spamming the key is rate limited.
        s.parryUntil = now < s.parryReady ? -1 : now + MayhemConfig.PARRY_WINDOW.get();
        s.parryReady = now + 30;
        Fx.anim(p, MoveType.GUARD_ON, 0);
    }

    static void dropGuard(ServerPlayer p, CombatState s) {
        if (s.guarding) {
            s.guarding = false;
            Fx.anim(p, MoveType.GUARD_OFF, 0);
        }
    }

    /** Whether the damage source comes from in front of the player (within roughly 80 degrees). */
    static boolean isFrontal(ServerPlayer p, DamageSource src) {
        Vec3 sp = src.getSourcePosition();
        if (sp == null) return false;
        Vec3 to = new Vec3(sp.x - p.getX(), 0, sp.z - p.getZ());
        if (to.lengthSqr() < 1.0E-4) return true;
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) return false;
        return flat.normalize().dot(to.normalize()) > 0.17;
    }

    static void parry(ServerPlayer p, CombatState s, DamageSource src) {
        ServerLevel level = p.serverLevel();
        int now = p.server.getTickCount();
        s.counterUntil = now + 50;
        CombatState.addMomentum(p, s, 25F);
        Fx.anim(p, MoveType.PARRY, 0);

        Entity attacker = src.getEntity();
        if (attacker instanceof LivingEntity a && a != p) {
            a.knockback(0.9 * MayhemConfig.KNOCKBACK_SCALE.get(), p.getX() - a.getX(), p.getZ() - a.getZ());
            a.hurtMarked = true;
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            a.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
            Dynamics.freeze(a, 6);
        }
        Vec3 look = p.getLookAngle();
        Vec3 c = p.position().add(0, 1.3, 0).add(look.scale(0.8));
        Fx.particle(level, ParticleTypes.CRIT, c, 18, 0.35, 0.3);
        Fx.particle(level, ParticleTypes.ELECTRIC_SPARK, c, 14, 0.3, 0.25);
        Fx.sound(level, c, MayhemSounds.PARRY.get(), 1.0F, 1.0F);
        Fx.impact(level, c, 0.5F, 3F, 20F, ImpactPacket.FLASH_DARK, Fx.TXT_ZAP, 6);
        p.displayClientMessage(Component.translatable("message.mayhem.parry"), true);
    }

    // ================================================================== ground slam

    public static void slam(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping() || p.isInWater()
                || p.getAbilities().flying) return;
        CombatState s = CombatState.of(p);
        int now = p.server.getTickCount();
        if (now < s.slamReady || s.busy() || s.slam != CombatState.Slam.NONE) return;
        dropGuard(p, s);

        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();

        s.slamReady = now + MayhemConfig.SLAM_COOLDOWN.get();
        s.slamStart = now;
        s.lastY = p.getY();
        s.noFallUntil = now + 200;

        if (p.onGround()) {
            s.slam = CombatState.Slam.LEAP;
            p.setDeltaMovement(flat.x * 0.35, 1.0, flat.z * 0.35);
            p.hurtMarked = true;
            Fx.anim(p, MoveType.SLAM_LEAP, 0);
            Fx.sound(p.serverLevel(), p.position(), MayhemSounds.WHOOSH_HEAVY.get(), 0.8F, 0.8F);
        } else {
            beginDive(p, s, now);
        }
    }

    private static void beginDive(ServerPlayer p, CombatState s, int now) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
        s.slam = CombatState.Slam.DIVE;
        s.diveStart = now;
        p.setDeltaMovement(flat.x * 0.25, -2.0, flat.z * 0.25);
        p.hurtMarked = true;
        Fx.anim(p, MoveType.SLAM_DIVE, 0);
        Fx.sound(p.serverLevel(), p.position(), MayhemSounds.WHOOSH_HEAVY.get(), 1.0F, 0.6F);
        Fx.impactTo(p, p.position(), 0.1F, 10F, ImpactPacket.FLASH_NONE, Fx.TXT_NONE, 14);
    }

    private static void slamImpact(ServerPlayer p, CombatState s, int now) {
        ServerLevel level = p.serverLevel();
        float scale = 1F + s.momentum / 100F * 0.6F;
        double radius = 3.6 * scale;
        Vec3 c = p.position();
        float base = UnarmedCombat.baseDamage(p);
        AABB box = new AABB(c, c).inflate(radius, 2.0, radius);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                o -> TargetFinder.valid(p, o) && (!(o instanceof Player) || MayhemConfig.PVP_SPECIALS.get())
                        && o.distanceToSqr(c) <= radius * radius)) {
            double d = Math.sqrt(e.distanceToSqr(c));
            float falloff = (float) (1.0 - 0.5 * d / radius);
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p),
                    base * 2.2F * falloff * scale * MayhemConfig.DAMAGE_SCALE.get().floatValue());
            e.knockback(1.0 * falloff * MayhemConfig.KNOCKBACK_SCALE.get(), c.x - e.getX(), c.z - e.getZ());
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7 * falloff, 0));
            e.hurtMarked = true;
            Dynamics.launch(e, p, true);
            Fx.anim(e, MoveType.TUMBLE, 0);
            if (e instanceof Enemy) {
                PlayerProgression.awardCombat(p, Math.max(2, (int) (base * 2)));
                if (e.isDeadOrDying()) PlayerProgression.awardDefeat(p, e);
            }
        }

        Destruction.shockCrater(p, c, radius, MayhemConfig.CRATER_DEPTH.get(), MayhemConfig.SLAM_HARDNESS.get());
        Fx.ring(level, ParticleTypes.CLOUD, c.add(0, 0.2, 0), 1.0, 36, 0.5, 0.02);
        Fx.ring(level, ParticleTypes.CRIT, c.add(0, 0.4, 0), 0.6, 24, 0.45, 0.1);
        Fx.particle(level, ParticleTypes.EXPLOSION, c.add(0, 0.5, 0), 3, 0.8, 0);
        Fx.sound(level, c, MayhemSounds.BOOM.get(), 1.0F, 1.0F);
        Fx.impact(level, c, 1.0F, 8F, 30F, ImpactPacket.FLASH_WHITE, Fx.TXT_DOGO, 8);
        Fx.anim(p, MoveType.SLAM_LAND, 0);

        s.slam = CombatState.Slam.NONE;
        s.noFallUntil = now + 10;
        p.fallDistance = 0F;
        CombatState.addMomentum(p, s, 12F);
        PlayerProgression.recordFinisher(p);
    }

    // ================================================================== per-tick

    public static void tick(ServerPlayer p) {
        CombatState s = CombatState.peek(p);
        if (s == null) return;
        int now = p.server.getTickCount();

        if (s.guarding) {
            if (p.isSprinting()) p.setSprinting(false);
            if (now % 4 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 1, false, false, false));
            }
        }

        if (s.ult != CombatState.Ult.NONE) {
            Ultimates.tick(p, s, now);
        }

        switch (s.slam) {
            case LEAP -> {
                if (now - s.slamStart > 40) {
                    s.slam = CombatState.Slam.NONE;
                } else if (now - s.slamStart >= 7 && p.getY() <= s.lastY + 0.0005) {
                    beginDive(p, s, now);
                }
            }
            case DIVE -> {
                p.fallDistance = 0F;
                if (now - s.slamStart > 90) {
                    s.slam = CombatState.Slam.NONE;
                } else if (p.onGround() && now - s.diveStart >= 2) {
                    slamImpact(p, s, now);
                } else if (now % 2 == 0) {
                    Fx.particle(p.serverLevel(), ParticleTypes.CLOUD, p.position().add(0, 1, 0), 3, 0.2, 0.02);
                }
            }
            default -> { }
        }

        if (s.momentum > 0F && s.ult == CombatState.Ult.NONE && now - s.lastMomentumGain > 80 && now % 10 == 0) {
            s.momentum = Math.max(0F, s.momentum - 2F);
            CombatState.syncMomentum(p, s);
        }
        s.lastY = p.getY();
    }

    /** True while fall damage should be suppressed (slam / ultimates). */
    static boolean noFall(CombatState s, int now) {
        return s.slam != CombatState.Slam.NONE || s.ult != CombatState.Ult.NONE || now <= s.noFallUntil;
    }

    /** Plays a short guard impact when a blocked hit reduces damage. */
    static void guardBlockFx(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0, 1.3, 0).add(p.getLookAngle().scale(0.7));
        Fx.particle(level, ParticleTypes.CRIT, c, 6, 0.25, 0.15);
        Fx.sound(level, c, SoundEvents.SHIELD_BLOCK, 0.9F, 1.1F);
    }
}
