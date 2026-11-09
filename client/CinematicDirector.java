package com.mayhem.client;

import com.mayhem.config.MayhemClientConfig;
import com.mayhem.network.CinematicPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Takes over the camera for scripted shots. The camera is a client-only marker entity that is never added to
 * the world; the game camera is pointed at it, then it is repositioned every frame by the shot for the current
 * "beat". The server drives beats (see {@link CinematicPacket}), so the camera always matches the action.
 */
public final class CinematicDirector {
    private static boolean active;
    private static byte type, beat;
    private static int attackerId, targetId;
    private static float startTime, beatStart, autoEnd = -1F;

    private static ArmorStand cam;
    private static CameraType savedType;

    private static boolean hasCur;
    private static Vec3 curPos = Vec3.ZERO;
    private static float curYaw, curPitch, curFov;
    private static double fov = -1.0;
    private static float bars;

    private static Vec3 anchor = Vec3.ZERO, anchorDir = new Vec3(0, 0, 1);
    private static Vec3 impactPos = Vec3.ZERO, impactSide = new Vec3(1, 0, 0), lastTarget = Vec3.ZERO;

    private CinematicDirector() {}

    public static boolean isActive() { return active; }
    public static boolean isInputLocked() { return active && (type == CinematicPacket.BARRAGE || type == CinematicPacket.METEOR); }
    public static double fovOverride() { return active ? fov : -1.0; }
    public static float bars() { return bars; }

    private static float ease(float t) {
        t = Mth.clamp(t, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    // ------------------------------------------------------------------ packets

    public static void handle(CinematicPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        switch (p.kind) {
            case CinematicPacket.START -> {
                if (MayhemClientConfig.CINEMATICS.get()) begin(mc, p);
            }
            case CinematicPacket.BEAT -> {
                if (active) setBeat(mc, p.beat, p.targetId);
            }
            case CinematicPacket.END -> finish(mc);
            default -> { }
        }
    }

    private static float now(Minecraft mc) {
        return mc.level.getGameTime() + mc.getFrameTime();
    }

    private static void begin(Minecraft mc, CinematicPacket p) {
        if (active) finish(mc);
        Entity a = mc.level.getEntity(p.attackerId);
        if (a == null) return;

        type = p.type;
        beat = p.beat;
        attackerId = p.attackerId;
        targetId = p.targetId;
        startTime = beatStart = now(mc);
        autoEnd = type == CinematicPacket.COUNTER ? 18F : type == CinematicPacket.FINISHER ? 26F : -1F;

        Vec3 f = a.getLookAngle();
        Vec3 flat = new Vec3(f.x, 0, f.z);
        anchorDir = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        anchor = a.position();
        impactPos = anchor;
        lastTarget = Vec3.ZERO;

        if (cam == null || cam.level() != mc.level) {
            cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            cam.setNoGravity(true);
        }
        savedType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(cam);
        hasCur = false;
        active = true;
    }

    private static void setBeat(Minecraft mc, byte newBeat, int newTarget) {
        beat = newBeat;
        beatStart = now(mc);
        if (newTarget >= 0) targetId = newTarget;
        if (newBeat == CinematicPacket.IMPACT) {
            Entity t = targetId >= 0 ? mc.level.getEntity(targetId) : null;
            Entity a = mc.level.getEntity(attackerId);
            Entity ref = t != null ? t : a;
            if (ref != null) impactPos = ref.position();
            Vec3 side = type == CinematicPacket.METEOR
                    ? new Vec3(-anchorDir.z, 0, anchorDir.x)
                    : new Vec3(-anchorDir.z, 0, anchorDir.x).scale(-1.0);
            impactSide = side;
        }
        // hard cuts feel cinematic: snap on the opening and impact shots, glide between the rest
        if (newBeat == CinematicPacket.IMPACT || newBeat == CinematicPacket.OPEN || newBeat == CinematicPacket.LAUNCH) {
            hasCur = false;
        }
    }

    public static void finish(Minecraft mc) {
        if (!active) return;
        active = false;
        fov = -1.0;
        try {
            if (mc.player != null) mc.setCameraEntity(mc.player);
            if (savedType != null) mc.options.setCameraType(savedType);
        } catch (RuntimeException ignored) {
            // never let a camera restore failure crash the game
        }
    }

    // ------------------------------------------------------------------ per-tick / per-frame

    /** Client tick: keep the attacker facing the target during the rush. */
    public static void tick(Minecraft mc) {
        if (!active || mc.player == null || mc.level == null) return;
        if (type == CinematicPacket.BARRAGE && beat <= CinematicPacket.ORBIT && targetId >= 0) {
            Entity t = mc.level.getEntity(targetId);
            if (t != null) LockOn.faceEntity(mc, t, 0.7F);
        }
    }

    /** Render-frame update: letterbox smoothing and camera placement. */
    public static void frame(float partial) {
        Minecraft mc = Minecraft.getInstance();
        float barTarget = active && MayhemClientConfig.LETTERBOX.get() ? 1F : 0F;
        bars += (barTarget - bars) * 0.18F;
        if (!active && bars < 0.01F) bars = 0F;
        if (!active) return;

        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) {
            finish(mc);
            return;
        }
        float now = mc.level.getGameTime() + partial;
        if (now - startTime > 420F || (autoEnd > 0F && now - startTime > autoEnd)) {
            finish(mc);
            return;
        }
        Entity a = mc.level.getEntity(attackerId);
        if (a == null) {
            finish(mc);
            return;
        }
        Entity t = targetId >= 0 ? mc.level.getEntity(targetId) : null;

        Vec3 A = a.getPosition(partial).add(0, a.getBbHeight() * 0.5, 0);
        Vec3 T;
        if (t != null && t.isAlive()) {
            T = t.getPosition(partial).add(0, t.getBbHeight() * 0.5, 0);
            lastTarget = T;
        } else {
            T = lastTarget.equals(Vec3.ZERO) ? A.add(anchorDir.scale(2.0)) : lastTarget;
        }

        float bt = now - beatStart;
        Vec3[] shot = new Vec3[2];
        float shotFov = shot(shot, bt, A, T);
        if (shot[0] == null) {
            finish(mc);
            return;
        }
        Vec3 desired = shot[0];
        Vec3 look = shot[1];

        // keep the camera out of walls
        BlockHitResult hit = mc.level.clip(new ClipContext(look, desired,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cam));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 back = look.subtract(hit.getLocation());
            desired = hit.getLocation().add(back.lengthSqr() < 1.0E-6 ? Vec3.ZERO : back.normalize().scale(0.25));
        }

        Vec3 to = look.subtract(desired);
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(to.y, flat) * (180.0 / Math.PI));

        if (!hasCur) {
            curPos = desired;
            curYaw = yaw;
            curPitch = pitch;
            curFov = shotFov;
            hasCur = true;
        } else {
            float k = 0.30F;
            curPos = curPos.lerp(desired, k);
            curYaw = Mth.rotLerp(k, curYaw, yaw);
            curPitch = Mth.lerp(k, curPitch, pitch);
            curFov = Mth.lerp(k, curFov, shotFov);
        }

        cam.setPos(curPos.x, curPos.y - cam.getEyeHeight(), curPos.z);
        cam.xo = cam.xOld = curPos.x;
        cam.yo = cam.yOld = curPos.y - cam.getEyeHeight();
        cam.zo = cam.zOld = curPos.z;
        cam.setYRot(curYaw);
        cam.setXRot(curPitch);
        cam.yRotO = curYaw;
        cam.xRotO = curPitch;
        fov = curFov;
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    // ------------------------------------------------------------------ the shots

    /**
     * Fills out[0] = camera position, out[1] = look-at point; returns the FOV (degrees).
     * out[0] stays null when the current type/beat has no shot (the cinematic then ends).
     */
    private static float shot(Vec3[] out, float bt, Vec3 A, Vec3 T) {
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 dirH = new Vec3(T.x - A.x, 0, T.z - A.z);
        dirH = dirH.lengthSqr() < 1.0E-4 ? anchorDir : dirH.normalize();
        double ang0 = Math.atan2(dirH.z, dirH.x);
        Vec3 side = new Vec3(-dirH.z, 0, dirH.x);
        Vec3 M = A.add(T).scale(0.5);

        if (type == CinematicPacket.BARRAGE) {
            switch (beat) {
                case CinematicPacket.OPEN -> {
                    double ang = ang0 + Math.PI + 0.5;
                    float r = 3.4F - 1.2F * ease(bt / 10F);
                    out[0] = A.add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
                    out[1] = T.add(0, 0.2, 0);
                    return Mth.lerp(ease(bt / 10F), 72F, 48F);
                }
                case CinematicPacket.ORBIT -> {
                    double ang = ang0 + Math.PI + 0.6 + 0.11 * bt;
                    double h = 0.45 + 0.35 * Math.sin(bt * 0.2);
                    out[0] = M.add(Math.cos(ang) * 3.7, h, Math.sin(ang) * 3.7);
                    out[1] = T.add(0, 0.15, 0);
                    return 54F;
                }
                case CinematicPacket.LAUNCH, CinematicPacket.SKY -> {
                    if (bt < 8F) {
                        out[0] = A.subtract(dirH.scale(1.8)).add(0, -0.6, 0);
                        out[1] = T;
                        return Mth.lerp(bt / 8F, 78F, 88F);
                    }
                    out[0] = T.add(side.scale(14.0)).add(0, -1.5, 0);
                    out[1] = T;
                    return 62F;
                }
                case CinematicPacket.DIVE -> {
                    out[0] = T.add(side.scale(9.0)).add(0, 1.5, 0);
                    out[1] = T;
                    return Mth.lerp(Mth.clamp(bt / 8F, 0F, 1F), 62F, 78F);
                }
                case CinematicPacket.IMPACT -> {
                    out[0] = impactPos.add(impactSide.scale(7.5 + 0.2 * bt)).add(0, 1.6, 0);
                    out[1] = impactPos.add(0, 1.2, 0);
                    return 52F;
                }
                default -> { return 60F; }
            }
        }

        if (type == CinematicPacket.METEOR) {
            Vec3 f = anchorDir;
            Vec3 fSide = new Vec3(-f.z, 0, f.x);
            switch (beat) {
                case CinematicPacket.CHARGE -> {
                    out[0] = anchor.add(f.scale(5.0 - 1.8 * ease(bt / 26F))).add(0, 0.4, 0);
                    out[1] = A.add(0, 0.5, 0);
                    return Mth.lerp(ease(bt / 26F), 72F, 50F);
                }
                case CinematicPacket.ASCEND -> {
                    double y = Math.max(anchor.y + 0.5, A.y - 7.0);
                    out[0] = new Vec3(anchor.x - f.x * 6.5, y, anchor.z - f.z * 6.5);
                    out[1] = A;
                    return 88F;
                }
                case CinematicPacket.HANG -> {
                    double ang = Math.atan2(f.z, f.x) + Math.PI + 0.10 * bt;
                    out[0] = A.add(Math.cos(ang) * 7.5, 1.0, Math.sin(ang) * 7.5);
                    out[1] = A;
                    return 58F;
                }
                case CinematicPacket.DIVE -> {
                    out[0] = A.add(fSide.scale(10.0)).add(0, 0.5, 0);
                    out[1] = A;
                    return Mth.lerp(Mth.clamp(bt / 6F, 0F, 1F), 66F, 82F);
                }
                case CinematicPacket.IMPACT -> {
                    out[0] = impactPos.add(impactSide.scale(11.0 + 0.2 * bt)).add(0, 2.5, 0);
                    out[1] = impactPos.add(0, 2.0, 0);
                    return 56F;
                }
                default -> { return 60F; }
            }
        }

        if (type == CinematicPacket.COUNTER) {
            double ang = ang0 + Math.PI + 0.18 * bt;
            out[0] = M.add(Math.cos(ang) * 3.0, 0.6, Math.sin(ang) * 3.0);
            out[1] = M.add(0, 0.2, 0);
            return Mth.lerp(Mth.clamp(bt / 18F, 0F, 1F), 62F, 46F);
        }

        if (type == CinematicPacket.FINISHER) {
            double ang = ang0 + Math.PI + 0.8 - 0.02 * bt;
            float r = 4.2F - 2.2F * ease(bt / 24F);
            out[0] = T.add(Math.cos(ang) * r, 0.5, Math.sin(ang) * r);
            out[1] = T;
            return Mth.lerp(ease(bt / 24F), 62F, 34F);
        }
        return 60F;
    }
}
