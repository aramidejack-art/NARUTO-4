package com.mayhem.shinobi.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Server-simulated projectiles (no entities): swept block and entity collision, gravity, piercing, homing. */
public final class Projectiles {
    public enum Reason { ENTITY, BLOCK, EXPIRE }

    @FunctionalInterface public interface HitFn { void hit(CastContext c, LivingEntity e, Vec3 at); }
    @FunctionalInterface public interface EndFn { void end(CastContext c, Vec3 at, Reason why); }

    public static final class P {
        public final CastContext c; public Vec3 pos, vel; public double gravity, radius = 0.6, homing;
        public int life = 30; public boolean pierce; public Vis vis; public HitFn onHit; public EndFn onEnd;
        public int trailDensity = 2; com.mayhem.shinobi.fx.JutsuFxEntity fx; final Set<Integer> hit = new HashSet<>(); LivingEntity homingTarget;

        public P(CastContext c, Vec3 pos, Vec3 vel, Vis vis) { this.c = c; this.pos = pos; this.vel = vel; this.vis = vis; }
        public P gravity(double g) { gravity = g; return this; }
        public P radius(double r) { radius = r; return this; }
        public P life(int l) { life = l; return this; }
        public P pierce(boolean b) { pierce = b; return this; }
        public P homing(double h, LivingEntity target) { homing = h; homingTarget = target; return this; }
        public P onHit(HitFn f) { onHit = f; return this; }
        public P onEnd(EndFn f) { onEnd = f; return this; }
        public P trail(int d) { trailDensity = d; return this; }
    }

    private static final List<P> ACTIVE = new ArrayList<>();
    private static final int MAX = 600;

    private Projectiles() {}

    public static void spawn(P p) {
        if (ACTIVE.size() >= MAX) return;
        ACTIVE.add(p);
        try {
            com.mayhem.shinobi.fx.JutsuFxEntity e = new com.mayhem.shinobi.fx.JutsuFxEntity(com.mayhem.shinobi.registry.ShinobiRegistry.JUTSU_FX.get(), p.c.level);
            e.setLook(p.vis.id(), (float) Math.max(0.5, p.radius * 1.5));
            e.setPos(p.pos.x, p.pos.y - e.getBbHeight() * 0.5, p.pos.z);
            e.aim(p.vel.x, p.vel.y, p.vel.z);
            p.c.level.addFreshEntity(e);
            p.fx = e;
        } catch (RuntimeException ignored) { }
    }

    private static void dropFx(P p) { if (p.fx != null) { p.fx.discard(); p.fx = null; } }

    public static void clear() { for (P p : ACTIVE) dropFx(p); ACTIVE.clear(); }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (Iterator<P> it = ACTIVE.iterator(); it.hasNext(); ) {
            P p = it.next();
            if (p.c.p.isRemoved() || !p.c.p.isAlive() || p.c.p.level() != p.c.level) { dropFx(p); it.remove(); continue; }
            if (!step(p)) { dropFx(p); it.remove(); }
            else if (p.fx != null) { p.fx.setPos(p.pos.x, p.pos.y - p.fx.getBbHeight() * 0.5, p.pos.z); p.fx.aim(p.vel.x, p.vel.y, p.vel.z); }
        }
    }

    /** Advances one tick. Returns false when the projectile is finished. */
    private static boolean step(P p) {
        CastContext c = p.c;
        if (p.homing > 0 && p.homingTarget != null && p.homingTarget.isAlive()) {
            Vec3 want = p.homingTarget.position().add(0, p.homingTarget.getBbHeight() * 0.5, 0).subtract(p.pos).normalize()
                    .scale(p.vel.length());
            p.vel = p.vel.lerp(want, p.homing);
        }
        p.vel = p.vel.add(0, -p.gravity, 0);
        int steps = (int) Math.max(1, Math.ceil(p.vel.length() / 0.6));
        Vec3 stepVec = p.vel.scale(1.0 / steps);
        for (int i = 0; i < steps; i++) {
            Vec3 from = p.pos, to = from.add(stepVec);
            BlockHitResult bh = c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.p));
            boolean blocked = bh.getType() != HitResult.Type.MISS;
            Vec3 end = blocked ? bh.getLocation() : to;

            LivingEntity best = null; double bestD = Double.MAX_VALUE;
            for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(p.radius), c::enemy)) {
                if (p.hit.contains(e.getId())) continue;
                Vec3 ec = e.position().add(0, e.getBbHeight() * 0.5, 0);
                double d = ec.distanceToSqr(from);
                if (d < bestD) { bestD = d; best = e; }
            }
            if (best != null) {
                p.pos = best.position().add(0, best.getBbHeight() * 0.5, 0);
                p.hit.add(best.getId());
                if (p.onHit != null) p.onHit.hit(c, best, p.pos);
                if (!p.pierce) {
                    if (p.onEnd != null) p.onEnd.end(c, p.pos, Reason.ENTITY);
                    return false;
                }
            }
            p.pos = end;
            if (blocked) {
                if (p.onEnd != null) p.onEnd.end(c, p.pos, Reason.BLOCK);
                return false;
            }
        }
        for (int i = 0; i < p.trailDensity; i++) c.fx(p.vis.main(), p.pos, 1, 0.06, 0.01);
        c.fx(p.vis.dust(), p.pos, 1, 0.05, 0.0);
        if (--p.life <= 0) {
            if (p.onEnd != null) p.onEnd.end(c, p.pos, Reason.EXPIRE);
            return false;
        }
        return true;
    }
}
