package com.mayhem.client;

import com.mayhem.combat.MoveType;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.util.Mth;

/**
 * Premium procedural pose library: punches, kicks, flips, twists, spins.
 * t = normalised progress 0..1; age = ticks with partial for idle sway.
 */
final class MovePoses {
    static final class Pose {
        float side, up, fwd;
        float pitch, yaw, roll;
        ArmPose left, right;
    }

    private MovePoses() {}

    private static float clamp01(float t) { return Mth.clamp(t, 0F, 1F); }
    private static float ease(float t) { t = clamp01(t); return t * t * (3F - 2F * t); }
    private static float easeIn(float t) { t = clamp01(t); return t * t; }
    private static float easeOut(float t) { t = clamp01(t); return 1F - (1F - t) * (1F - t); }
    private static float tri(float t, float peak) {
        t = clamp01(t);
        return t < peak ? t / peak : (1F - t) / Math.max(0.001F, 1F - peak);
    }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static void both(Pose p, ArmPose a) { p.left = a; p.right = a; }

    static Pose guard() {
        Pose p = new Pose();
        p.left = ArmPose.BLOCK;
        p.right = ArmPose.BLOCK;
        p.pitch = 8F;
        p.fwd = -0.05F;
        p.up = 0.02F;
        return p;
    }

    static Pose sample(MoveType move, int v, float t, float age) {
        Pose p = new Pose();
        t = clamp01(t);
        float sgn = (v & 1) == 0 ? 1F : -1F;
        float k, s, wind, hit;

        switch (move) {
            case JAB -> {
                k = tri(t, 0.32F);
                p.fwd = 0.22F * k;
                p.pitch = 10F * k;
                p.yaw = -14F * sgn * k;
                p.side = 0.04F * sgn * k;
                if (t > 0.04F && t < 0.7F) { p.right = ArmPose.CROSSBOW_HOLD; p.left = ArmPose.EMPTY; }
            }
            case CROSS -> {
                k = tri(t, 0.38F);
                p.fwd = 0.28F * k;
                p.yaw = 36F * sgn * k;
                p.pitch = 12F * k;
                p.roll = 8F * sgn * k;
                if (t > 0.04F && t < 0.75F) both(p, ArmPose.CROSSBOW_HOLD);
            }
            case HOOK -> {
                // Wide twisting hook
                p.yaw = lerp(-70F * sgn, 85F * sgn, ease(t));
                p.roll = 22F * tri(t, 0.5F) * sgn;
                p.fwd = 0.14F * tri(t, 0.5F);
                p.side = 0.12F * sgn * tri(t, 0.45F);
                both(p, ArmPose.BOW_AND_ARROW);
            }
            case KNEE -> {
                k = tri(t, 0.42F);
                p.up = 0.32F * k;
                p.pitch = -18F * k;
                p.fwd = 0.16F * k;
                p.yaw = 10F * sgn * k;
                both(p, ArmPose.EMPTY);
            }
            case ROUNDHOUSE -> {
                // Full spinning roundhouse
                wind = clamp01(t / 0.25F);
                hit = tri(Mth.clamp((t - 0.2F) / 0.55F, 0F, 1F), 0.45F);
                p.yaw = lerp(0F, 340F * sgn, ease(t));
                p.roll = 25F * hit * sgn;
                p.up = 0.2F * hit;
                p.fwd = 0.1F * hit;
                p.pitch = -8F * hit;
                both(p, ArmPose.EMPTY);
            }
            case UPPERCUT -> {
                k = tri(t, 0.4F);
                p.up = 0.35F * k;
                p.pitch = -45F * k;
                p.fwd = 0.18F * easeIn(k);
                p.yaw = -8F * sgn * k;
                if (t < 0.85F) both(p, ArmPose.CROSSBOW_HOLD);
            }
            case SWEEP -> {
                k = tri(t, 0.5F);
                p.up = -0.25F * k;
                p.yaw = 120F * sgn * ease(t);
                p.roll = 30F * k * sgn;
                p.fwd = 0.08F * k;
                both(p, ArmPose.EMPTY);
            }
            case DASH_STRIKE -> {
                k = tri(t, 0.35F);
                p.fwd = 0.55F * k;
                p.pitch = 15F * k;
                p.side = 0.06F * sgn * k;
                both(p, ArmPose.CROSSBOW_HOLD);
            }
            case SPIKE -> {
                k = tri(t, 0.4F);
                p.pitch = 70F * k;
                p.up = 0.15F * (1F - k);
                p.fwd = -0.05F;
                both(p, ArmPose.EMPTY);
            }
            case BARRAGE_L, BARRAGE_R -> {
                k = tri(t, 0.45F);
                float side = move == MoveType.BARRAGE_L ? -1F : 1F;
                p.fwd = 0.2F * k;
                p.yaw = 20F * side * k;
                p.pitch = 6F * k;
                p.right = side > 0 ? ArmPose.CROSSBOW_HOLD : ArmPose.EMPTY;
                p.left = side < 0 ? ArmPose.CROSSBOW_HOLD : ArmPose.EMPTY;
            }
            case SLAM_LEAP -> {
                k = easeOut(t);
                p.up = 0.9F * k;
                p.pitch = -25F * k;
                both(p, ArmPose.THROW_SPEAR);
            }
            case SLAM_DIVE -> {
                k = easeIn(t);
                p.pitch = 80F * k;
                p.up = lerp(0.6F, -0.1F, k);
                both(p, ArmPose.THROW_SPEAR);
            }
            case SLAM_LAND -> {
                k = tri(t, 0.3F);
                p.up = -0.15F * k;
                p.pitch = 20F * k;
                both(p, ArmPose.BLOCK);
            }
            case DODGE -> {
                k = tri(t, 0.4F);
                p.side = 0.65F * sgn * k;
                p.up = 0.12F * k;
                p.roll = 35F * sgn * k;
                p.yaw = -25F * sgn * k;
            }
            case FLASH_STEP -> {
                k = tri(t, 0.3F);
                p.fwd = 0.8F * k;
                p.pitch = 5F;
            }
            case PARRY -> {
                k = tri(t, 0.35F);
                p.fwd = -0.08F * k;
                p.pitch = 12F * k;
                both(p, ArmPose.BLOCK);
            }
            case COUNTER -> {
                k = tri(t, 0.4F);
                p.fwd = 0.3F * k;
                p.yaw = 40F * sgn * k;
                p.roll = 15F * sgn * k;
                both(p, ArmPose.CROSSBOW_HOLD);
            }
            case CHARGE -> {
                p.fwd = -0.12F * ease(t);
                p.pitch = 18F * ease(t);
                both(p, ArmPose.BOW_AND_ARROW);
            }
            case JOLT -> {
                k = tri(t, 0.35F);
                p.side = 0.08F * sgn * k;
                p.pitch = 12F * k;
                p.roll = 18F * sgn * k;
            }
            case TUMBLE -> {
                p.pitch = 360F * ease(t);
                p.roll = 40F * Mth.sin(t * Mth.PI) * sgn;
                p.up = 0.25F * Mth.sin(t * Mth.PI);
            }

            // ---- Premium kit ----
            case SPIN_KICK -> {
                // 360° spinning kick with lift
                p.yaw = 360F * sgn * ease(t);
                k = tri(t, 0.5F);
                p.up = 0.35F * k;
                p.roll = 40F * k * sgn;
                p.pitch = -15F * k;
                p.fwd = 0.12F * k;
                both(p, ArmPose.EMPTY);
            }
            case AXE_KICK -> {
                // Leg up then smash down
                if (t < 0.45F) {
                    k = easeOut(t / 0.45F);
                    p.up = 0.4F * k;
                    p.pitch = -50F * k;
                } else {
                    k = easeIn((t - 0.45F) / 0.55F);
                    p.up = lerp(0.4F, -0.1F, k);
                    p.pitch = lerp(-50F, 40F, k);
                    p.fwd = 0.15F * k;
                }
                both(p, ArmPose.EMPTY);
            }
            case TWIST_PUNCH -> {
                // Torso twist + snap punch
                p.yaw = lerp(-90F * sgn, 50F * sgn, ease(t));
                k = tri(t, 0.55F);
                p.fwd = 0.25F * k;
                p.roll = 28F * Mth.sin(t * Mth.PI) * sgn;
                p.pitch = 8F * k;
                both(p, ArmPose.CROSSBOW_HOLD);
            }
            case BACKFLIP -> {
                p.pitch = -360F * ease(t);
                p.up = 0.7F * Mth.sin(t * Mth.PI);
                p.fwd = -0.2F * Mth.sin(t * Mth.PI);
                both(p, ArmPose.EMPTY);
            }
            case FRONTFLIP -> {
                p.pitch = 360F * ease(t);
                p.up = 0.65F * Mth.sin(t * Mth.PI);
                p.fwd = 0.25F * Mth.sin(t * Mth.PI);
                both(p, ArmPose.EMPTY);
            }
            case CARTWHEEL -> {
                p.roll = 360F * sgn * ease(t);
                p.side = 0.5F * sgn * Mth.sin(t * Mth.PI);
                p.up = 0.35F * Mth.sin(t * Mth.PI);
                both(p, ArmPose.EMPTY);
            }
            case WHIRLWIND -> {
                // Multi-spin with arms out
                p.yaw = 720F * sgn * ease(t);
                k = tri(t, 0.5F);
                p.up = 0.25F * k;
                p.roll = 15F * Mth.sin(t * Mth.TWO_PI * 2F);
                both(p, ArmPose.THROW_SPEAR);
            }
            case FLYING_KICK -> {
                k = tri(t, 0.4F);
                p.fwd = 0.7F * k;
                p.up = 0.3F * k;
                p.pitch = -25F * k;
                p.roll = 12F * sgn * k;
                both(p, ArmPose.EMPTY);
            }
            case ELBOW -> {
                k = tri(t, 0.35F);
                p.fwd = 0.15F * k;
                p.yaw = 25F * sgn * k;
                p.pitch = 15F * k;
                both(p, ArmPose.BLOCK);
            }
            case DOUBLE_PALM -> {
                k = tri(t, 0.4F);
                p.fwd = 0.3F * k;
                p.pitch = 5F * k;
                both(p, ArmPose.CROSSBOW_HOLD);
            }
            case SIDE_KICK -> {
                k = tri(t, 0.4F);
                p.side = 0.35F * sgn * k;
                p.yaw = 60F * sgn * k;
                p.pitch = -10F * k;
                p.up = 0.1F * k;
                both(p, ArmPose.EMPTY);
            }
            case HEEL_STOMP -> {
                if (t < 0.4F) {
                    k = easeOut(t / 0.4F);
                    p.up = 0.45F * k;
                    p.pitch = -30F * k;
                } else {
                    k = easeIn((t - 0.4F) / 0.6F);
                    p.up = lerp(0.45F, -0.12F, k);
                    p.pitch = lerp(-30F, 25F, k);
                }
                both(p, ArmPose.EMPTY);
            }
            case SPINNING_BACKFIST -> {
                p.yaw = 300F * sgn * ease(t);
                k = tri(t, 0.55F);
                p.fwd = 0.12F * k;
                p.roll = 20F * k * sgn;
                both(p, ArmPose.CROSSBOW_HOLD);
            }
            case AIR_TWIST -> {
                p.yaw = 360F * sgn * ease(t);
                p.pitch = 20F * Mth.sin(t * Mth.PI);
                p.up = 0.5F * Mth.sin(t * Mth.PI);
                p.roll = 25F * Mth.sin(t * Mth.TWO_PI) * sgn;
                both(p, ArmPose.EMPTY);
            }
            default -> { }
        }
        return p;
    }
}
