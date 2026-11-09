package com.mayhem.shinobi.client;

import com.mayhem.MayhemMod;
import com.mayhem.shinobi.avatar.ChakraAvatarEntity;
import com.mayhem.shinobi.beast.Beasts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws the chakra avatar around its owner: a see-through, single-colour glowing tailed beast (all fours or on hind legs,
 * with punch / tackle / roar poses) or the Mirror Eye Guardian. Rendered additively so it reads as pure chakra and the
 * owner stays visible inside it.
 */
public class ChakraAvatarRenderer extends EntityRenderer<ChakraAvatarEntity> {
    private static final ResourceLocation SUSANOO = new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/susanoo.png");
    private final BeastModel beast;
    private final SusanooModel guardian;
    private final net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> skin;
    private final ModeOutfitModel outfit;

    public ChakraAvatarRenderer(EntityRendererProvider.Context c) {
        super(c);
        beast = new BeastModel(c.bakeLayer(BeastModel.LAYER));
        guardian = new SusanooModel(c.bakeLayer(SusanooModel.LAYER));
        skin = new net.minecraft.client.model.PlayerModel<>(c.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
        outfit = new ModeOutfitModel(c.bakeLayer(ModeOutfitModel.LAYER));
        shadowRadius = 0F;
    }

    @Override public boolean shouldRender(ChakraAvatarEntity e, Frustum f, double x, double y, double z) { return true; }

    @Override
    public ResourceLocation getTextureLocation(ChakraAvatarEntity e) {
        return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/beast/" + e.form() + "_avatar.png");
    }

    @Override
    public void render(ChakraAvatarEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Player o = e.owner();
        if (o == null || e.form().isEmpty()) return;
        ps.pushPose();
        // follow the owner's interpolated position exactly (the avatar entity itself only ticks at 20 Hz)
        double ox = Mth.lerp(pt, o.xo, o.getX()) - Mth.lerp(pt, e.xo, e.getX());
        double oy = Mth.lerp(pt, o.yo, o.getY()) - Mth.lerp(pt, e.yo, e.getY());
        double oz = Mth.lerp(pt, o.zo, o.getZ()) - Mth.lerp(pt, e.zo, e.getZ());
        ps.translate(ox, oy, oz);
        float bodyYaw = Mth.rotLerp(pt, o.yBodyRotO, o.yBodyRot);
        ps.mulPose(Axis.YP.rotationDegrees(180F - bodyYaw));
        ps.scale(-1F, -1F, 1F);
        float limbSwing = o.walkAnimation.position(pt);
        float limbAmt = o.walkAnimation.speed(pt);
        float age = o.tickCount + pt;
        float headYaw = Mth.rotLerp(pt, o.yHeadRotO, o.yHeadRot) - bodyYaw;
        float pitch = Mth.lerp(pt, o.xRotO, o.getXRot());
        int act = e.act();
        float actT = e.actProgress(pt);
        if (actT >= 1F) act = 0;

        if (e.isMode()) {
            renderMode(e, o, pt, ps, buf, light, limbSwing, limbAmt, age, headYaw, pitch);
        } else if (e.isGuardian()) {
            int stage = Math.max(1, Math.min(4, e.stage()));
            float s = 1.6F + stage * 0.35F;
            ps.translate(0, -1.501F, 0);
            ps.scale(s, s, s);
            ps.translate(0, -0.15F - stage * 0.02F, 0);
            guardian.setupStage(stage, age, 0F);
            guardian.setupAct(limbSwing * 0.8F, limbAmt, act, actT);
            // additive + low alpha: a ghostly giant the owner can be seen standing inside
            VertexConsumer vc = buf.getBuffer(RenderType.eyes(SUSANOO));
            float r = stage >= 3 ? 0.55F : 0.45F, g = stage >= 4 ? 0.35F : 0.55F;
            guardian.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, 1F, 0.20F + stage * 0.04F);
        } else {
            float stand = Mth.lerp(pt, e.standAmtO, e.standAmt);
            float s = 1.9F;
            ps.scale(s, s, s);
            ps.translate(0, -1.501F, 0);
            int bAct = switch (act) { case 1 -> BeastModel.ACT_PUNCH_L; case 2 -> BeastModel.ACT_PUNCH_R; case 3 -> BeastModel.ACT_TACKLE; case 4 -> BeastModel.ACT_ROAR; default -> BeastModel.ACT_NONE; };
            beast.animate(e.form(), false, false, stand, bAct, actT, limbSwing * 0.75F, limbAmt, age, headYaw, pitch);
            VertexConsumer vc = buf.getBuffer(RenderType.eyes(getTextureLocation(e)));
            // two passes: a dense core and a slightly inflated halo so the hollow body has a glowing rim
            // low alpha so the owner stays clearly visible inside the giant
            beast.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 0.30F);
            ps.scale(1.045F, 1.045F, 1.045F);
            beast.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.8F, 0.8F, 0.8F, 0.12F);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    /** Chakra Mode: a skin-tight beast-patterned suit with a wavy coat, worn over the player (face and hands stay bare). */
    private void renderMode(ChakraAvatarEntity e, Player o, float pt, PoseStack ps, MultiBufferSource buf, int light,
                            float limbSwing, float limbAmt, float age, float headYaw, float pitch) {
        if (!(o instanceof net.minecraft.client.player.AbstractClientPlayer cp)) return;
        String id = e.form().substring(5);
        skin.young = false;
        skin.attackTime = o.getAttackAnim(pt);
        skin.riding = o.isPassenger();
        skin.crouching = o.isCrouching();
        skin.setupAnim(cp, limbSwing, limbAmt, age, headYaw, pitch);
        outfit.animate(id, skin, age, limbAmt);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0, -1.501F, 0);
        ResourceLocation tex = new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/mode/" + id + ".png");
        ResourceLocation glow = new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/mode/" + id + "_glow.png");
        outfit.renderToBuffer(ps, buf.getBuffer(RenderType.entityTranslucent(tex)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.5F);
        outfit.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(glow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1F);
    }
}
