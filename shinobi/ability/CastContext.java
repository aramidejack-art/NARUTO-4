package com.mayhem.shinobi.ability;

import com.mayhem.config.MayhemConfig;
import com.mayhem.combat.TargetFinder;
import com.mayhem.fx.Fx;
import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.data.ShinobiState;
import com.mayhem.shinobi.identity.Passives;
import com.mayhem.world.Destruction;
import com.mayhem.world.Dynamics;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Everything a behaviour needs: the caster, resolved numbers, targeting, damage and effect helpers. */
public final class CastContext {
    public final ServerPlayer p;
    public final ServerLevel level;
    public final AbilityDef def;
    public final Variant v;
    public final Flag flag;
    public final LivingEntity lock;
    public final CompoundTag t;
    public final double range, dmg;
    public final int mastery;
    public int hits;

    public CastContext(ServerPlayer p, AbilityDef def, Variant v, LivingEntity lock) {
        this.p = p; this.level = p.serverLevel(); this.def = def; this.v = v; this.lock = lock;
        this.flag = v == null ? Flag.NONE : v.flag();
        this.t = ShinobiData.of(p);
        this.mastery = ShinobiData.mastery(t, def.id);
        this.range = def.range * (v == null ? 1.0 : v.rangeMul());
        ShinobiState st = ShinobiState.of(p.getUUID());
        long now = p.server.getTickCount();
        double boost = now < st.boostUntil ? st.boostMul : 1.0;
        double lv = 1.0 + ShinobiData.stat(t, "power") * 0.04 + (PlayerProgression.getLevel(p) - 1) * 0.012;
        double mast = 1.0 + Math.min(mastery, 200) / 200.0 * 0.15;
        this.dmg = def.damage * (v == null ? 1.0 : v.dmgMul()) * lv * mast * Passives.damageMul(t, def)
                * ShinobiConfig.JUTSU_DAMAGE_SCALE.get() * boost;
    }

    // ------------------------------------------------------------------ geometry
    public Vec3 eye() { return p.getEyePosition(); }
    public Vec3 look() { return p.getLookAngle(); }
    public Vec3 flatLook() {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** Where the player is aiming: locked target if any, else the first block hit within {@code max}. */
    public Vec3 aimPoint(double max) {
        if (lock != null && lock.isAlive() && p.distanceTo(lock) <= max + 2) return lock.position().add(0, lock.getBbHeight() * 0.5, 0);
        Vec3 from = eye();
        Vec3 to = from.add(look().scale(max));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** First free end of a straight run from the eye along {@code dir}, stopping before blocks. */
    public Vec3 rayEnd(Vec3 dir, double max) {
        Vec3 from = eye();
        Vec3 to = from.add(dir.normalize().scale(max));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    public Vec3 groundBelow(Vec3 pos) {
        Vec3 to = pos.add(0, -24, 0);
        BlockHitResult hit = level.clip(new ClipContext(pos, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    // ------------------------------------------------------------------ targeting
    public boolean enemy(LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (e.getTags().contains("mayhem_friendly")) return false;
        if (e instanceof Player) {
            if (!ShinobiConfig.PVP_JUTSU.get() || !p.server.isPvpAllowed()) return false;
        }
        return TargetFinder.validForSpecials(p, e, false);
    }

    public List<LivingEntity> inRadius(Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.7, r), this::enemy)) {
            if (e.position().add(0, e.getBbHeight() * 0.5, 0).distanceTo(c) <= r + e.getBbWidth() * 0.5) out.add(e);
        }
        return out;
    }

    public List<LivingEntity> inCone(double reach, double halfDeg) {
        Vec3 eye = eye(), look = look();
        double cos = Math.cos(Math.toRadians(halfDeg));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(reach, reach * 0.6, reach), this::enemy)) {
            Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
            Vec3 d = c.subtract(eye);
            double len = d.length();
            if (len > reach + e.getBbWidth() * 0.5) continue;
            if (len > 0.1 && look.dot(d.scale(1.0 / len)) < cos) continue;
            out.add(e);
        }
        return out;
    }

    public List<LivingEntity> inLine(Vec3 a, Vec3 b, double width) {
        List<LivingEntity> out = new ArrayList<>();
        AABB box = new AABB(a, b).inflate(width + 1.0);
        Vec3 ab = b.subtract(a);
        double l2 = Math.max(1.0E-6, ab.lengthSqr());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, this::enemy)) {
            Vec3 c = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double tt = Math.max(0, Math.min(1, c.subtract(a).dot(ab) / l2));
            if (a.add(ab.scale(tt)).distanceTo(c) <= width + e.getBbWidth() * 0.5) out.add(e);
        }
        return out;
    }

    public LivingEntity primaryTarget(double reach) {
        if (lock != null && lock.isAlive() && enemy(lock) && p.distanceTo(lock) <= reach + 2) return lock;
        List<LivingEntity> c = inCone(reach, 25);
        return c.isEmpty() ? null : c.get(0);
    }

    // ------------------------------------------------------------------ damage + force
    public boolean hurt(LivingEntity e, double mult) { return hurt(e, mult, false); }

    public boolean hurt(LivingEntity e, double mult, boolean multiHit) {
        if (!enemy(e)) return false;
        if (multiHit) e.invulnerableTime = 0;
        boolean ok = e.hurt(p.damageSources().playerAttack(p), (float) Math.max(0.5, dmg * mult));
        if (!ok) return false;
        hits++;
        String clan = ShinobiData.clan(t);
        if ("gentleveil".equals(clan)) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        if ("thunderfang".equals(clan) && "lightning".equals(def.element)) Dynamics.freeze(e, 8);
        if ("emberfang".equals(clan) && "fire".equals(def.element)) e.setSecondsOnFire(5);
        if ("bonecrest".equals(clan) && def.category == Category.TAIJUTSU && mult >= 1.0) e.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 0));
        if ("mistbane".equals(clan) && e.isInWaterOrRain()) e.invulnerableTime = 0;
        return true;
    }

    public void push(LivingEntity e, Vec3 dir, double strength, double up) {
        double resist = e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? Vec3.ZERO : dir.normalize();
        Vec3 add = d.scale(strength * (1.0 - Math.min(0.9, resist))).add(0, up, 0);
        e.setDeltaMovement(e.getDeltaMovement().add(add));
        e.hurtMarked = true;
        if (strength >= 1.0 || up >= 0.6) Dynamics.launch(e, p, up >= 0.6);
    }

    public void freeze(LivingEntity e, int ticks) {
        Dynamics.freeze(e, ticks);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
    }

    /** Area blast: damage with falloff, knockback away from the centre, particles, sound and optional terrain damage. */
    public int blast(Vec3 c, double radius, double mult, double knock, double lift, Vis vis, boolean crater) {
        int n = 0;
        for (LivingEntity e : inRadius(c, radius)) {
            double fall = 1.0 - 0.45 * Math.min(1.0, e.position().distanceTo(c) / Math.max(0.1, radius));
            if (hurt(e, mult * fall, true)) {
                push(e, e.position().subtract(c).add(0, 0.2, 0), knock, lift);
                n++;
            }
        }
        fx(vis.main(), c, (int) (14 + radius * 6), radius * 0.4, 0.15);
        fx(vis.accent(), c, (int) (8 + radius * 3), radius * 0.35, 0.1);
        Fx.ring(level, vis.main(), c.add(0, 0.2, 0), 0.4, (int) (14 + radius * 4), 0.25 + radius * 0.04, 0.02);
        sound(vis.hit(), c, 1.2F, 0.9F);
        Fx.impact(level, c, (float) Math.min(1.2, 0.3 + radius * 0.08), 0.03F, (float) (radius * 3 + 12));
        if (crater && Destruction.enabled()) {
            Destruction.shockCrater(p, c, Math.max(1.5, radius * 0.7), 1, MayhemConfig.SLAM_HARDNESS.get());
        }
        return n;
    }

    // ------------------------------------------------------------------ effects
    public void fx(ParticleOptions o, Vec3 pos, int count, double spread, double speed) { Fx.particle(level, o, pos, count, spread, speed); }
    public void sound(SoundEvent s, Vec3 pos, float vol, float pitch) { Fx.sound(level, pos, s, vol, pitch); }
    public void castSound(Vis v) { sound(v.cast(), p.position(), 1.0F, 1.0F); }

    public void telegraphRing(Vec3 c, double radius, Vis vis) {
        Fx.ring(level, vis.dust(), c.add(0, 0.15, 0), radius, (int) Math.max(16, radius * 10), 0.0, 0.0);
    }

    public void swing(com.mayhem.combat.MoveType m) { Fx.anim(p, m, 0); }
}
