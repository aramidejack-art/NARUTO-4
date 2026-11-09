package com.mayhem.shinobi.client;

import com.mayhem.shinobi.appearance.Appearance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Draws hair, eyes and face marks as flat-coloured boxes on the head (no custom textures needed). Reads Appearance.CACHE. */
public class AppearanceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public AppearanceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limb, float limbSwing,
                       float pt, float age, float yaw, float pitch) {
        if (!Appearance.has(p.getUUID()) || p.isInvisible()) return;
        CompoundTag a = Appearance.of(p.getUUID());
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        // Skin tint overlay (skinAmt 0..0.6 mixes skin tone onto the head)
        float amt = a.getFloat("skinAmt");
        if (amt > 0.02F) {
            int sc = a.getInt("skin");
            int sr = (sc >> 16) & 255, sg = (sc >> 8) & 255, sb = sc & 255;
            int alpha = (int) (amt * 180);
            int tint = (Math.min(255, sr) << 16) | (Math.min(255, sg) << 8) | Math.min(255, sb);
            // translucent head shell
            boxAlpha(vc, ps, -4.2F, -8.2F, -4.2F, 4.2F, 0.2F, 4.2F, tint, light, alpha);
        }
        int hc = a.getInt("hairColor");
        int style = a.getInt("hairStyle");
        float e = 0.5F; // pixels of inflate over the head
        switch (style) {
            case 1 -> box(vc, ps, -4 - e, -8.6F, -4 - e, 4 + e, -6.6F, 4 + e, hc, light);          // short cap
            case 2 -> { box(vc, ps, -4 - e, -8.6F, -4 - e, 4 + e, -6.6F, 4 + e, hc, light);          // spiky
                for (int i = -3; i <= 3; i += 3) box(vc, ps, i - 1, -11, -1, i + 1, -8.6F, 1, hc, light); }
            case 3 -> { box(vc, ps, -4 - e, -8.6F, -4 - e, 4 + e, -6.6F, 4 + e, hc, light);          // long
                box(vc, ps, -4 - e, -8.6F, 3.5F, 4 + e, 4, 4.5F, hc, light); }
            case 4 -> { box(vc, ps, -4 - e, -8.6F, -4 - e, 4 + e, -6.6F, 4 + e, hc, light);          // ponytail
                box(vc, ps, -1, -7, 4, 1, 2, 6, hc, light); }
            case 5 -> { box(vc, ps, -4 - e, -8.6F, -4 - e, 4 + e, -6.6F, 4 + e, hc, light);          // bun
                box(vc, ps, -2, -11.5F, -2, 2, -8.6F, 2, hc, light); }
            case 6 -> box(vc, ps, -1, -10, -4.5F, 1, -8.4F, 4.5F, hc, light);                        // mohawk
            case 7 -> { box(vc, ps, -4 - e, -8.6F, -4 - e, 4 + e, -6.6F, 4 + e, hc, light);          // bob
                box(vc, ps, -4.5F, -8, -4.5F, -3.5F, -1, 4.5F, hc, light);
                box(vc, ps, 3.5F, -8, -4.5F, 4.5F, -1, 4.5F, hc, light); }
            default -> { }
        }
        // eyes (front face is -Z)
        int ec = a.getInt("eyeColor");
        int es = a.getInt("eyeStyle");
        float eh = es == 1 ? 1.0F : es == 3 ? 2.0F : 1.5F;
        box(vc, ps, -3, -4.2F, -4.15F, -1, -4.2F + eh, -4.05F, ec, light);
        box(vc, ps, 1, -4.2F, -4.15F, 3, -4.2F + eh, -4.05F, ec, light);
        // marks
        int marks = a.getInt("marks");
        if (marks == 1) { for (int s = -1; s <= 1; s += 2) for (int i = 0; i < 3; i++) box(vc, ps, s * 3.5F - 0.4F, -2.8F + i * 0.7F, -4.12F, s * 3.5F + 0.4F + s * 0.4F, -2.4F + i * 0.7F, -4.05F, 0x2A1A12, light); }
        if (marks == 2) box(vc, ps, 1.5F, -6.5F, -4.12F, 2.1F, -2.5F, -4.05F, 0x9E3B3B, light);
        if (marks == 3) box(vc, ps, -4.2F, -7F, -4.2F, 4.2F, -6F, 4.2F, 0x2B3A55, light);
        if (marks == 4) { box(vc, ps, -3, -2.5F, -4.12F, -2, -1.5F, -4.05F, 0xC0392B, light); box(vc, ps, 2, -2.5F, -4.12F, 3, -1.5F, -4.05F, 0xC0392B, light); }
        ps.popPose();
    }

    /** Axis-aligned box in model pixels (1/16 block) relative to the current pose. */
    private static void box(VertexConsumer vc, PoseStack ps, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        PoseStack.Pose pose = ps.last();
        x0 /= 16; y0 /= 16; z0 /= 16; x1 /= 16; y1 /= 16; z1 /= 16;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        quad(vc, pose, r, g, b, light, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(vc, pose, r, g, b, light, 0, 0, 1, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
        quad(vc, pose, r, g, b, light, -1, 0, 0, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
        quad(vc, pose, r, g, b, light, 1, 0, 0, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(vc, pose, r, g, b, light, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
        quad(vc, pose, r, g, b, light, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int r, int g, int b, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        v(vc, pose, ax, ay, az, 0, 1, r, g, b, light, nx, ny, nz);
        v(vc, pose, bx, by, bz, 1, 1, r, g, b, light, nx, ny, nz);
        v(vc, pose, cx, cy, cz, 1, 0, r, g, b, light, nx, ny, nz);
        v(vc, pose, dx, dy, dz, 0, 0, r, g, b, light, nx, ny, nz);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float vv, int r, int g, int b, int light, float nx, float ny, float nz) {
        vc.vertex(pose.pose(), x, y, z).color(r, g, b, 255).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
