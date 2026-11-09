package com.mayhem.shinobi.ability;

import com.mayhem.combat.MoveType;
import com.mayhem.combat.Teleports;
import com.mayhem.fx.Fx;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.data.Resources;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Support, defence, summon, sensing and movement mechanics. */
public final class Utility {
    private Utility() {}

    /** Finds a safe landing spot along {@code dir}, shrinking the distance until the path and spot are clear. */
    static Vec3 dashTo(CastContext c, Vec3 dir, double dist) {
        Vec3 start = c.p.position();
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? c.flatLook() : dir.normalize();
        for (double s = dist; s >= 1.0; s -= 1.0) {
            Vec3 target = start.add(d.scale(s));
            Vec3 eyeTo = c.eye().add(d.scale(s));
            BlockHitResult h = c.level.clip(new ClipContext(c.eye(), eyeTo, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.p));
            if (h.getType() != HitResult.Type.MISS) continue;
            if (Teleports.validSpot(c.level, c.p, target, true)) return target;
        }
        return null;
    }

    static void shieldFx(CastContext c, Vis vis) {
        Vec3 pos = c.p.position();
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            c.fx(vis.main(), pos.add(Math.cos(a) * 1.2, 0.3 + (i % 4) * 0.45, Math.sin(a) * 1.2), 1, 0.05, 0.01);
        }
        c.sound(vis.cast(), pos, 0.9F, 0.8F);
    }

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }

    // ------------------------------------------------------------------ defence
    /** Damage-reducing field: reduction 0..0.9 plus optional absorption and a push-back pulse. */
    public static Behavior shield(Vis vis, int duration, double reduction, int absorbAmp, double pushRadius) {
        return c -> {
            c.swing(MoveType.PARRY);
            ShinobiState st = ShinobiState.of(c.p.getUUID());
            long now = c.p.server.getTickCount();
            st.guardUntil = now + (c.flag == Flag.AREA ? duration + 40 : duration);
            st.guardReduction = Math.min(0.9, reduction * (c.flag == Flag.FOCUSED ? 1.15 : 1.0));
            if (absorbAmp >= 0) c.p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, absorbAmp, false, true));
            shieldFx(c, vis);
            if (pushRadius > 0) for (LivingEntity e : c.inRadius(c.p.position(), pushRadius)) c.push(e, e.position().subtract(c.p.position()), 0.7, 0.2);
            Tasks.every(c.p.server, 5, duration / 5, i -> { if (c.p.isAlive()) c.fx(vis.main(), c.p.position().add(0, 1, 0), 6, 0.7, 0.01); });
        };
    }

    /** Counter stance: the next hit taken inside the window is negated and answered with a teleport strike. */
    public static Behavior counter(Vis vis, int ticks, double mult) {
        return c -> {
            c.swing(MoveType.PARRY);
            ShinobiState st = ShinobiState.of(c.p.getUUID());
            st.counterUntil = c.p.server.getTickCount() + ticks;
            st.counterDamage = c.dmg * mult;
            shieldFx(c, vis);
            msg(c.p, "Counter stance ready");
        };
    }

    /** Called by the hurt hook when a counter stance is active. */
    public static void triggerCounter(ServerPlayer p, LivingEntity attacker, double damage, Vis vis) {
        ShinobiState st = ShinobiState.of(p.getUUID());
        st.counterUntil = 0;
        if (attacker == null || !attacker.isAlive()) return;
        Vec3 behind = attacker.position().subtract(attacker.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        Teleports.blink(p, behind, attacker.position());
        attacker.invulnerableTime = 0;
        attacker.hurt(p.damageSources().playerAttack(p), (float) Math.max(1.0, damage));
        Vec3 away = attacker.position().subtract(p.position());
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(away.normalize().scale(0.9)).add(0, 0.35, 0));
        attacker.hurtMarked = true;
        Fx.particle(p.serverLevel(), vis.accent(), attacker.position().add(0, 1, 0), 18, 0.4, 0.15);
        Fx.impact(p.serverLevel(), attacker.position(), 0.6F, 0.04F, 24F);
    }

    /** Leaves an afterimage, drops mob aggro for a while and grants a burst of speed. */
    public static Behavior decoy(Vis vis, int ticks) {
        return c -> {
            c.swing(MoveType.FLASH_STEP);
            Vec3 spot = c.p.position();
            c.fx(vis.main(), spot.add(0, 1, 0), 30, 0.4, 0.06);
            c.sound(vis.cast(), spot, 1.0F, 1.5F);
            c.p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1, false, false));
            c.p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.min(ticks, 60), 0, false, false));
            Tasks.every(c.p.server, 5, ticks / 5, i -> {
                if (!c.p.isAlive()) return;
                c.fx(vis.accent(), spot.add(0, 1, 0), 3, 0.3, 0.02);
                for (Mob m : c.level.getEntitiesOfClass(Mob.class, c.p.getBoundingBox().inflate(14))) {
                    if (m.getTarget() == c.p) m.setTarget(null);
                }
            });
        };
    }

    /** Illusion: blinds and confuses everything nearby and breaks mob aggro. */
    public static Behavior genjutsu(Vis vis, double radius, int ticks) {
        return c -> {
            c.swing(MoveType.JOLT);
            c.castSound(vis);
            Fx.ring(c.level, vis.main(), c.p.position().add(0, 1, 0), 0.5, 26, 0.4, 0.02);
            for (LivingEntity e : c.inRadius(c.p.position(), radius)) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
                if (e instanceof Mob m) {
                    Tasks.every(c.p.server, 10, ticks / 10, i -> { if (m.isAlive()) m.setTarget(null); });
                }
            }
        };
    }

    /** Reveals nearby living things and grants night vision. */
    public static Behavior sense(Vis vis, double radius, int ticks) {
        return c -> {
            c.swing(MoveType.JOLT);
            c.castSound(vis);
            c.p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0, false, false));
            for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, c.p.getBoundingBox().inflate(radius), x -> x != c.p)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
            }
            Fx.ring(c.level, vis.main(), c.p.position().add(0, 0.2, 0), 1.0, 30, 0.6, 0.01);
        };
    }

    // ------------------------------------------------------------------ support
    /** Heal over pulses; limited by max health fraction per pulse. Optionally cleanses harmful effects. */
    public static Behavior heal(Vis vis, double pctPerPulse, double radius, boolean cleanse, int pulses) {
        return c -> {
            c.swing(MoveType.JOLT);
            c.castSound(vis);
            Tasks.every(c.p.server, 10, pulses, i -> {
                if (!c.p.isAlive()) return;
                List<ServerPlayer> group = new ArrayList<>();
                group.add(c.p);
                for (ServerPlayer o : c.level.getEntitiesOfClass(ServerPlayer.class, c.p.getBoundingBox().inflate(radius))) if (o != c.p && o.isAlive()) group.add(o);
                for (ServerPlayer o : group) {
                    o.heal((float) (o.getMaxHealth() * pctPerPulse * (c.flag == Flag.AREA ? 1.0 : 1.2)));
                    if (cleanse) {
                        List<MobEffectInstance> bad = new ArrayList<>();
                        for (MobEffectInstance m : o.getActiveEffects()) if (!m.getEffect().isBeneficial()) bad.add(m);
                        for (MobEffectInstance m : bad) o.removeEffect(m.getEffect());
                    }
                    c.fx(vis.accent(), o.position().add(0, 1.2, 0), 8, 0.4, 0.03);
                }
            });
        };
    }

    /** Self buff: temporary damage multiplier plus potion effects. */
    public static Behavior empower(Vis vis, int ticks, double dmgMul, int speed, int strength, int resist, int regen) {
        return c -> {
            c.swing(MoveType.CHARGE);
            ShinobiState st = ShinobiState.of(c.p.getUUID());
            st.boostUntil = c.p.server.getTickCount() + ticks;
            st.boostMul = dmgMul;
            if (speed >= 0) c.p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, speed, false, false));
            if (strength >= 0) c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, strength, false, false));
            if (resist >= 0) c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, resist, false, false));
            if (regen >= 0) c.p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, regen, false, false));
            Fx.ring(c.level, vis.main(), c.p.position().add(0, 0.2, 0), 0.4, 20, 0.35, 0.12);
            c.sound(vis.cast(), c.p.position(), 1.0F, 0.7F);
            Tasks.every(c.p.server, 6, ticks / 6, i -> { if (c.p.isAlive()) c.fx(vis.main(), c.p.position().add(0, 1, 0), 4, 0.5, 0.02); });
        };
    }

    /** Siphons chakra from the target into the caster. */
    public static Behavior siphon(Vis vis, double reach, double mult, float chakra) {
        return c -> {
            c.swing(MoveType.CROSS);
            c.castSound(vis);
            LivingEntity e = c.primaryTarget(reach);
            if (e == null) return;
            if (c.hurt(e, mult)) {
                Resources.addChakra(c.p, c.t, chakra);
                c.fx(vis.accent(), e.position().add(0, 1, 0), 12, 0.3, 0.1);
            }
        };
    }

    /** Short-range precise strike that weakens the target and drains a player target's chakra. */
    public static Behavior palm(Vis vis, double reach, double mult, int weakTicks, float drain) {
        return c -> {
            c.swing(MoveType.UPPERCUT);
            c.castSound(vis);
            LivingEntity e = c.primaryTarget(reach);
            if (e == null) return;
            if (c.hurt(e, mult)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakTicks, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, weakTicks / 2, 0));
                if (e instanceof ServerPlayer sp && drain > 0) {
                    CompoundTag tt = ShinobiData.of(sp);
                    tt.putFloat("chakra", Math.max(0F, tt.getFloat("chakra") - drain));
                }
                c.push(e, c.look(), 0.15, 0.0);
                c.fx(vis.accent(), e.position().add(0, 1.1, 0), 14, 0.25, 0.1);
            }
        };
    }

    // ------------------------------------------------------------------ summons
    public static Behavior summon(Supplier<EntityType<? extends Mob>> type, int count, int duration, String name, Vis vis) {
        return c -> {
            c.swing(MoveType.SLAM_LAND);
            c.castSound(vis);
            ShinobiState st = ShinobiState.of(c.p.getUUID());
            int cap = ShinobiConfig.MAX_SUMMONS.get();
            if (st.summons + count > cap) { msg(c.p, "Too many summons active"); return; }
            LivingEntity tgt = c.primaryTarget(c.range);
            for (int i = 0; i < count; i++) {
                Mob m = type.get().create(c.level);
                if (m == null) continue;
                double a = Math.PI * 2 * i / Math.max(1, count);
                m.moveTo(c.p.getX() + Math.cos(a) * 1.8, c.p.getY(), c.p.getZ() + Math.sin(a) * 1.8, c.p.getYRot(), 0);
                m.addTag("mayhem_friendly");
                m.setCustomName(Component.literal(name));
                if (m instanceof TamableAnimal ta) ta.tame(c.p);
                if (m instanceof IronGolem g) g.setPlayerCreated(true);
                if (tgt != null) m.setTarget(tgt);
                c.level.addFreshEntity(m);
                c.fx(vis.main(), m.position().add(0, 0.6, 0), 14, 0.4, 0.05);
                st.summons++;
                Tasks.later(c.p.server, duration, () -> {
                    if (m.isAlive()) { c.fx(vis.accent(), m.position().add(0, 0.6, 0), 10, 0.4, 0.05); m.discard(); }
                    st.summons = Math.max(0, st.summons - 1);
                });
            }
        };
    }

    // ------------------------------------------------------------------ movement
    public static Behavior blink(Vis vis, double dist) {
        return c -> {
            c.swing(MoveType.FLASH_STEP);
            Vec3 start = c.p.position();
            Vec3 land = dashTo(c, c.look(), c.flag == Flag.CHARGED ? dist * 1.4 : dist);
            if (land == null) { msg(c.p, "Path blocked"); return; }
            Fx.trail(c.level, start, land);
            c.sound(vis.cast(), start, 1.0F, 1.3F);
            Teleports.blink(c.p, land, land.add(c.look()));
            c.p.fallDistance = 0;
            c.fx(vis.main(), land.add(0, 1, 0), 16, 0.4, 0.06);
        };
    }

    /** Teleports behind the target and strikes; bonus damage from behind. */
    public static Behavior behindStrike(Vis vis, double reach, double mult) {
        return c -> {
            c.swing(MoveType.FLASH_STEP);
            LivingEntity e = c.primaryTarget(reach);
            if (e == null) { msg(c.p, "No target"); return; }
            Vec3 from = c.p.position();
            Vec3 face = e.getLookAngle().multiply(1, 0, 1);
            Vec3 behind = e.position().subtract((face.lengthSqr() < 1.0E-4 ? c.flatLook() : face.normalize()).scale(1.6));
            if (!Teleports.validSpot(c.level, c.p, behind, true)) behind = e.position().subtract(c.flatLook().scale(1.6));
            Fx.trail(c.level, from, behind);
            Teleports.blink(c.p, behind, e.position());
            c.sound(vis.cast(), behind, 1.0F, 1.2F);
            double back = e.getLookAngle().dot(c.flatLook()) > 0.3 ? 1.5 : 1.0;
            if (c.hurt(e, mult * back)) c.push(e, c.flatLook(), 0.5, 0.2);
        };
    }

    public static Behavior airDash(Vis vis, double power, double lift) {
        return c -> {
            c.swing(MoveType.DASH_STRIKE);
            Vec3 d = c.look();
            double p = c.flag == Flag.CHARGED ? power * 1.35 : power;
            c.p.setDeltaMovement(d.x * p, Math.max(lift, d.y * p + lift * 0.5), d.z * p);
            c.p.hurtMarked = true;
            c.p.fallDistance = 0;
            c.sound(vis.cast(), c.p.position(), 1.0F, 1.0F);
            Tasks.every(c.p.server, 1, 8, i -> { if (c.p.isAlive()) c.fx(vis.main(), c.p.position().add(0, 0.8, 0), 4, 0.25, 0.02); });
        };
    }

    public static Behavior glide(Vis vis, int ticks) {
        return c -> {
            c.p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false));
            c.p.setDeltaMovement(c.p.getDeltaMovement().add(0, 0.55, 0));
            c.p.hurtMarked = true;
            c.p.fallDistance = 0;
            c.sound(vis.cast(), c.p.position(), 0.8F, 1.3F);
            Fx.ring(c.level, vis.main(), c.p.position().add(0, 0.3, 0), 0.4, 18, 0.3, 0.08);
        };
    }

    /** Toggles a traversal ability flag stored in player data ("waterWalk", "wallWalk"). */
    public static Behavior toggleFlag(String key, String label) {
        return c -> {
            boolean on = !c.t.getBoolean(key);
            c.t.putBoolean(key, on);
            msg(c.p, label + (on ? " ON" : " OFF"));
            c.fx(Vis.CHAKRA.main(), c.p.position().add(0, 0.2, 0), 14, 0.5, 0.03);
        };
    }
}
