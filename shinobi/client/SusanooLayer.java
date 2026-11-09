package com.mayhem.shinobi.client;

import com.mayhem.MayhemMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;

/**
 * Renders the Spectral Guardian / Susanoo model around a player when guardTicks > 0.
 * Stage is read from the synced shinobi tag (guardStage / guardTicks).
 */
public class SusanooLayer extends RenderLayer<AbstractClientPlayer, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEX = new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/susanoo.png");
    private final SusanooModel model;

    public SusanooLayer(RenderLayerParent<AbstractClientPlayer, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        model = new SusanooModel(models.bakeLayer(SusanooModel.LAYER));
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float pt, float age, float netHeadYaw, float headPitch) {
        CompoundTag tag = ClientShinobi.tag;
        // Only for local player with synced tag; other players would need per-player sync (future).
        if (player != net.minecraft.client.Minecraft.getInstance().player) return;
        if (tag == null || tag.getInt("guardTicks") <= 0) return;
        int stage = Math.max(1, Math.min(4, tag.getInt("guardStage")));

        ps.pushPose();
        // Grow around the player
        float scale = 1.6F + stage * 0.35F;
        ps.scale(scale, scale, scale);
        ps.translate(0, -0.15F - stage * 0.02F, 0);

        model.setupStage(stage, age + pt, player.yBodyRot);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX));
        // Spectral blue/purple tint
        float r = stage >= 3 ? 0.55F : 0.45F;
        float g = stage >= 4 ? 0.35F : 0.55F;
        float b = 1.0F;
        float a = 0.55F + stage * 0.08F;
        model.renderToBuffer(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, a);
        ps.popPose();
    }
}
