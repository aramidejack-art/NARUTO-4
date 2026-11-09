package com.mayhem.client;

import com.mayhem.MayhemMod;
import com.mayhem.combat.MoveType;
import com.mayhem.config.MayhemClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Applies procedural poses to rendered entities (third person / other players / mobs) and punch motion to the
 * first-person hand. Uses only Forge render events: no mixins, no animation libraries.
 */
@Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT)
public final class AnimationRenderer {
    /** Entities whose PoseStack we pushed in Pre and must pop in Post. */
    private static final Set<Integer> PUSHED = new HashSet<>();

    private AnimationRenderer() {}

    @SubscribeEvent
    public static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!MayhemClientConfig.ANIMATIONS.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LivingEntity e = event.getEntity();
        ClientAnimations.Active a = ClientAnimations.active(e);
        boolean guarding = e instanceof Player && ClientAnimations.isGuarding(e.getId());
        if (a == null && !guarding) return;

        MovePoses.Pose pose;
        if (a != null) {
            float age = (mc.level.getGameTime() - a.start) + event.getPartialTick();
            float t = a.move.ticks > 0 ? age / a.move.ticks : 1F;
            pose = MovePoses.sample(a.move, a.variant, t, age);
        } else {
            pose = MovePoses.guard();
        }

        // arm poses (players only); the model is shared, so these are re-set every render
        if (event.getRenderer().getModel() instanceof PlayerModel<?> pm) {
            if (pose.left != null) pm.leftArmPose = pose.left;
            if (pose.right != null) pm.rightArmPose = pose.right;
        }

        boolean moves = pose.side != 0F || pose.up != 0F || pose.fwd != 0F
                || pose.pitch != 0F || pose.yaw != 0F || pose.roll != 0F;
        if (!moves) return;

        PoseStack ps = event.getPoseStack();
        float yaw = Mth.rotLerp(event.getPartialTick(), e.yBodyRotO, e.yBodyRot);
        float pivot = e.getBbHeight() * 0.5F;

        ps.pushPose();
        PUSHED.add(e.getId());
        ps.translate(0.0, pivot, 0.0);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));           // into the entity's local frame
        ps.translate(pose.side, pose.up, pose.fwd);
        ps.mulPose(Axis.XP.rotationDegrees(pose.pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(pose.roll));
        ps.mulPose(Axis.YP.rotationDegrees(pose.yaw));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));            // back out; the renderer applies its own yaw
        ps.translate(0.0, -pivot, 0.0);
    }

    @SubscribeEvent
    public static void onPost(RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    // ------------------------------------------------------------------ first person

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (CinematicDirector.isActive()) {
            event.setCanceled(true); // the player is visible on screen during cinematics
            return;
        }
        if (!MayhemClientConfig.FIRST_PERSON_FX.get() || mc.player == null || mc.level == null) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        ClientAnimations.Active a = ClientAnimations.active(mc.player);
        if (a == null) return;
        float age = (mc.level.getGameTime() - a.start) + event.getPartialTick();
        float t = Mth.clamp(a.move.ticks > 0 ? age / a.move.ticks : 1F, 0F, 1F);
        PoseStack ps = event.getPoseStack();

        float tri = t < 0.4F ? t / 0.4F : (1F - t) / 0.6F;
        float side = (a.variant & 1) == 0 ? 1F : -1F;
        switch (a.move) {
            case JAB, CROSS, BARRAGE_L, BARRAGE_R -> {
                ps.translate(0.06F * side * tri, -0.02F, -0.55F * tri);
                ps.mulPose(Axis.XP.rotationDegrees(-8F * tri));
            }
            case HOOK -> {
                float s = Mth.sin(t * Mth.PI);
                ps.translate(-0.55F * s + 0.2F * t, -0.1F * s, -0.25F * s);
                ps.mulPose(Axis.YP.rotationDegrees(40F * s));
                ps.mulPose(Axis.ZP.rotationDegrees(-25F * s));
            }
            case UPPERCUT -> {
                ps.translate(0.1F * tri, 0.45F * tri, -0.3F * tri);
                ps.mulPose(Axis.XP.rotationDegrees(-55F * tri));
            }
            case DASH_STRIKE, FLASH_STEP -> ps.translate(0.0F, -0.05F, -0.75F * tri);
            case KNEE, ROUNDHOUSE, SWEEP, COUNTER, SLAM_LAND, SIDE_KICK, SPIN_KICK -> {
                float s = Mth.sin(t * Mth.PI);
                ps.translate(0.0F, -0.35F * s, 0.05F * s);
                ps.mulPose(Axis.ZP.rotationDegrees(22F * s * side));
                ps.mulPose(Axis.YP.rotationDegrees(30F * s * side));
            }
            case SPIKE, SLAM_DIVE, SLAM_LEAP, AXE_KICK, HEEL_STOMP -> {
                ps.translate(0.0F, -0.1F, -0.5F);
                ps.mulPose(Axis.XP.rotationDegrees(-40F * Mth.sin(t * Mth.PI)));
            }
            case TWIST_PUNCH, SPINNING_BACKFIST, WHIRLWIND -> {
                float s = Mth.sin(t * Mth.PI);
                ps.mulPose(Axis.YP.rotationDegrees(90F * s * side));
                ps.translate(0.15F * side * s, 0.0F, -0.4F * s);
            }
            case BACKFLIP, FRONTFLIP, CARTWHEEL, AIR_TWIST -> {
                float s = Mth.sin(t * Mth.PI);
                ps.translate(0.0F, 0.4F * s, -0.2F);
                ps.mulPose(Axis.XP.rotationDegrees(moveFlipPitch(a.move, t)));
                ps.mulPose(Axis.ZP.rotationDegrees(a.move == MoveType.CARTWHEEL ? 360F * t * side : 15F * s * side));
            }
            case FLYING_KICK -> {
                float s = Mth.sin(t * Mth.PI);
                ps.translate(0.0F, 0.15F * s, -0.7F * s);
                ps.mulPose(Axis.XP.rotationDegrees(-30F * s));
            }
            case DOUBLE_PALM, ELBOW -> {
                float s = Mth.sin(t * Mth.PI);
                ps.translate(0.0F, 0.0F, -0.45F * s);
                ps.mulPose(Axis.XP.rotationDegrees(-12F * s));
            }
            default -> { }
        }
    }

    public static boolean isMoveHeavy(MoveType m) {
        return m == MoveType.ROUNDHOUSE || m == MoveType.UPPERCUT || m == MoveType.DASH_STRIKE;
    }
}
