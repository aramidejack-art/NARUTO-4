package com.mayhem.client;

import com.mayhem.config.MayhemClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

/** Client-side target lock: picks a target, keeps it valid, and gently pulls the view toward it. */
public final class LockOn {
    private static int lockedId = -1;
    private static int lostSight;

    private LockOn() {}

    public static int id() {
        return lockedId;
    }

    public static void clear() {
        lockedId = -1;
        lostSight = 0;
    }

    public static LivingEntity target() {
        Minecraft mc = Minecraft.getInstance();
        if (lockedId < 0 || mc.level == null) return null;
        Entity e = mc.level.getEntity(lockedId);
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    public static void toggle(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (lockedId >= 0) {
            clear();
            p.displayClientMessage(Component.translatable("message.mayhem.lock_off"), true);
            return;
        }
        Vec3 view = p.getViewVector(1F);
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Mob m) || !m.isAlive() || m.isInvisible()) continue;
            double dist = p.distanceTo(m);
            if (dist > 20.0 || !p.hasLineOfSight(m)) continue;
            Vec3 to = m.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize();
            double angle = Math.acos(Mth.clamp(view.dot(to), -1.0, 1.0));
            double score = (angle * 3.0 + dist * 0.05) * (m instanceof Enemy ? 0.7 : 1.0);
            if (score < bestScore) {
                bestScore = score;
                best = m;
            }
        }
        if (best == null) {
            p.displayClientMessage(Component.translatable("message.mayhem.no_target"), true);
            return;
        }
        lockedId = best.getId();
        lostSight = 0;
        p.displayClientMessage(Component.translatable("message.mayhem.lock_on", best.getDisplayName()), true);
    }

    /** Per-client-tick validation and aim assist. */
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (lockedId < 0) return;
        LivingEntity t = target();
        if (t == null || p.distanceTo(t) > 26F) {
            clear();
            return;
        }
        if (!p.hasLineOfSight(t)) {
            if (++lostSight > 60) clear();
        } else {
            lostSight = 0;
        }

        float assist = MayhemClientConfig.LOCK_ON_ASSIST.get().floatValue();
        if (assist > 0F && p.distanceTo(t) <= 12F && !CinematicDirector.isActive()) {
            faceEntity(mc, t, assist * 0.5F);
        }
    }

    /** Rotates the local player toward {@code e} by the given fraction (0..1). */
    public static void faceEntity(Minecraft mc, Entity e, float amount) {
        LocalPlayer p = mc.player;
        if (p == null || e == null) return;
        Vec3 to = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(to.y, flat) * (180.0 / Math.PI));
        // Never assist the view upward past the uppercut threshold: it would hijack move selection.
        pitch = Mth.clamp(pitch, -30F, 60F);
        p.setYRot(Mth.rotLerp(amount, p.getYRot(), yaw));
        p.setXRot(Mth.lerp(amount, p.getXRot(), pitch));
    }
}
