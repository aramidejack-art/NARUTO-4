package com.mayhem.combat;

import com.mayhem.config.MayhemConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Cone / radius target selection with line-of-sight checks. Used instead of a thin ray for a forgiving feel. */
public final class TargetFinder {
    private TargetFinder() {}

    /** Whether {@code e} may be hit by the player's normal strikes. */
    public static boolean valid(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e == p.getVehicle()) return false;
        if (e.isAlliedTo(p)) return false;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return false;
        return e.isPickable();
    }

    /** Whether {@code e} may be targeted by lock-on special moves (flash steps, launches, ultimates). */
    public static boolean validForSpecials(ServerPlayer p, LivingEntity e, boolean ultimate) {
        if (!valid(p, e)) return false;
        if (e instanceof Player) {
            return ultimate ? MayhemConfig.ULTIMATES_HIT_PLAYERS.get() : MayhemConfig.PVP_SPECIALS.get();
        }
        return e instanceof Mob;
    }

    private static double distanceToBox(Vec3 from, AABB bb) {
        double cx = Mth.clamp(from.x, bb.minX, bb.maxX);
        double cy = Mth.clamp(from.y, bb.minY, bb.maxY);
        double cz = Mth.clamp(from.z, bb.minZ, bb.maxZ);
        return from.distanceTo(new Vec3(cx, cy, cz));
    }

    public static double distance(ServerPlayer p, LivingEntity e) {
        return distanceToBox(p.getEyePosition(), e.getBoundingBox());
    }

    public static boolean hasLineOfSight(ServerPlayer p, LivingEntity e) {
        Vec3 from = p.getEyePosition();
        Vec3 to = e.getBoundingBox().getCenter();
        BlockHitResult hit = p.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS) return true;
        return from.distanceToSqr(hit.getLocation()) >= from.distanceToSqr(to) - 0.5;
    }

    /** The entity with the given client id if it is a sensible lock-on target within range, else null. */
    public static LivingEntity byId(ServerPlayer p, int id, double maxDistance) {
        if (id < 0) return null;
        Entity e = p.level().getEntity(id);
        if (!(e instanceof LivingEntity le) || !valid(p, le)) return null;
        if (p.distanceTo(le) > maxDistance) return null;
        return le;
    }

    /** Targets inside a forward cone, best first. */
    public static List<LivingEntity> find(ServerPlayer p, double reach, double halfAngleDeg, int max) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double cosLimit = Math.cos(Math.toRadians(halfAngleDeg));
        AABB box = p.getBoundingBox().inflate(reach, reach * 0.6, reach);

        List<LivingEntity> candidates = p.level().getEntitiesOfClass(LivingEntity.class, box, e -> valid(p, e));
        List<double[]> scored = new ArrayList<>();
        List<LivingEntity> kept = new ArrayList<>();
        for (LivingEntity e : candidates) {
            AABB bb = e.getBoundingBox();
            double dist = distanceToBox(eye, bb);
            if (dist > reach) continue;
            double dot = 1.0;
            if (dist > 0.05) {
                Vec3 cp = new Vec3(Mth.clamp(eye.x, bb.minX, bb.maxX), Mth.clamp(eye.y, bb.minY, bb.maxY),
                        Mth.clamp(eye.z, bb.minZ, bb.maxZ));
                dot = look.dot(cp.subtract(eye).normalize());
            }
            if (dot < cosLimit) continue;
            if (!hasLineOfSight(p, e)) continue;
            scored.add(new double[]{kept.size(), Math.acos(Math.min(1.0, dot)) * 2.0 + dist * 0.08});
            kept.add(e);
        }
        scored.sort(Comparator.comparingDouble(a -> a[1]));
        List<LivingEntity> out = new ArrayList<>();
        for (double[] s : scored) {
            if (out.size() >= max) break;
            out.add(kept.get((int) s[0]));
        }
        return out;
    }

    /** Nearest valid special-move target within {@code radius}, ignoring facing. */
    public static LivingEntity nearest(ServerPlayer p, double radius, boolean ultimate) {
        AABB box = p.getBoundingBox().inflate(radius);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> validForSpecials(p, e, ultimate))) {
            double d = p.distanceToSqr(e);
            if (d < bestDist && hasLineOfSight(p, e)) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }
}
