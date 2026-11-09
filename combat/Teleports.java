package com.mayhem.combat;

import com.mayhem.fx.Fx;
import com.mayhem.fx.MayhemSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Flash-step ("shunpo") teleports: safe destination search and the actual blink. */
public final class Teleports {
    private Teleports() {}

    /** Yaw/pitch (degrees) that make something at {@code from} look at {@code to}. */
    public static float[] aim(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, flat) * (180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    /** True if the player fits at {@code pos}, the path is clear, and it is not lava / a long drop. */
    public static boolean validSpot(ServerLevel level, ServerPlayer p, Vec3 pos, boolean allowAir) {
        AABB box = p.getBoundingBox().move(pos.x - p.getX(), pos.y - p.getY(), pos.z - p.getZ());
        if (!level.noCollision(p, box)) return false;
        if (!level.getWorldBorder().isWithinBounds(box)) return false;

        Vec3 from = p.getEyePosition();
        Vec3 to = pos.add(0, p.getEyeHeight(), 0);
        BlockHitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.MISS) return false;

        BlockPos feet = BlockPos.containing(pos);
        if (!level.getFluidState(feet).isEmpty()) return false;
        if (!allowAir) {
            boolean ground = false;
            for (int i = 1; i <= 4 && !ground; i++) {
                ground = !level.getBlockState(feet.below(i)).getCollisionShape(level, feet.below(i)).isEmpty();
            }
            if (!ground) return false;
        }
        return true;
    }

    /**
     * First valid spot on a ring around {@code target}. {@code baseAngle} is the direction (radians,
     * measured from the target) to prefer; offsets/radii are tried in order.
     */
    public static Vec3 findSpot(ServerLevel level, ServerPlayer p, LivingEntity target, double baseAngle,
                                double[] offsetsDeg, double[] radii) {
        boolean air = !target.onGround();
        for (double r : radii) {
            for (double off : offsetsDeg) {
                double a = baseAngle + Math.toRadians(off);
                Vec3 pos = new Vec3(target.getX() + Math.cos(a) * r, target.getY(), target.getZ() + Math.sin(a) * r);
                if (validSpot(level, p, pos, air)) return pos;
            }
        }
        return null;
    }

    /** Angle (radians) of the vector pointing from {@code target} toward {@code p}. */
    public static double angleToPlayer(ServerPlayer p, LivingEntity target) {
        return Mth.atan2(p.getZ() - target.getZ(), p.getX() - target.getX());
    }

    /** Teleports the player (afterimage trail + sound), facing {@code lookAt} if given. */
    public static void blink(ServerPlayer p, Vec3 dest, Vec3 lookAt) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        Fx.trail(level, from, dest);
        Fx.sound(level, from, MayhemSounds.FLASH_STEP.get(), 0.9F, 1.0F);
        float yaw = p.getYRot();
        float pitch = p.getXRot();
        if (lookAt != null) {
            float[] a = aim(dest.add(0, p.getEyeHeight(), 0), lookAt);
            yaw = a[0];
            pitch = Mth.clamp(a[1], -60F, 60F);
        }
        p.connection.teleport(dest.x, dest.y, dest.z, yaw, pitch);
        p.fallDistance = 0F;
        Fx.particle(level, net.minecraft.core.particles.ParticleTypes.CLOUD, dest.add(0, 0.9, 0), 10, 0.3, 0.05);
        Fx.sound(level, dest, MayhemSounds.WHOOSH_LIGHT.get(), 0.7F, 1.3F);
    }

    /** Convenience: is {@code level} the player's current level (teleport safety). */
    public static boolean sameLevel(ServerPlayer p, Level level) {
        return p.level() == level;
    }
}
