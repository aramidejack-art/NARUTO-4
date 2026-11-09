package com.mayhem.shinobi.client;

import com.mayhem.MayhemMod;
import com.mayhem.shinobi.fx.JutsuFxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 3D jutsu bodies. Style order matches {@code Vis.id}: FIRE, WATER, WIND, LIGHTNING, EARTH, SHADOW, CHAKRA, BODY, BEAST, ICE, WOOD, LAVA.
 * Every style is built from the same parts (core, orbiting shards, tumbling chunks, ring, tail) but shaped, coloured and animated differently.
 */
public class JutsuFxRenderer extends EntityRenderer<JutsuFxEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(MayhemMod.MOD_ID, "jutsu_fx"), "main");
    private static final ResourceLocation TEX = new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/fx/orb.png");
    /** r,g,b per style. Additive rendering, so every colour is kept bright. */
    private static final float[][] COL = {
            {1.0F, 0.45F, 0.1F}, {0.25F, 0.55F, 1F}, {0.8F, 1F, 0.9F}, {0.75F, 0.85F, 1F}, {0.75F, 0.55F, 0.3F}, {0.55F, 0.2F, 0.9F},
            {0.4F, 0.75F, 1F}, {1F, 1F, 1F}, {1F, 0.3F, 0.15F}, {0.7F, 0.95F, 1F}, {0.3F, 0.85F, 0.3F}, {1F, 0.3F, 0.05F}};

    private final Fx model;

    public JutsuFxRenderer(EntityRendererProvider.Context c) {
        super(c);
        model = new Fx(c.bakeLayer(LAYER));
        shadowRadius = 0F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.ZERO);
        PartDefinition ring = r.addOrReplaceChild("ring", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float a = (float) (Math.PI * 2 * i / 6);
            ring.addOrReplaceChild("shard" + i, CubeListBuilder.create().texOffs(0, 16).addBox(-1, -1, -7, 2, 2, 14),
                    PartPose.offsetAndRotation(Mth.cos(a) * 7, Mth.sin(a) * 7, 0, 0, 0, a));
        }
        PartDefinition chunks = r.addOrReplaceChild("chunks", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 5; i++) {
            float a = (float) (Math.PI * 2 * i / 5);
            chunks.addOrReplaceChild("chunk" + i, CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, -2.5F, -2.5F, 5, 5, 5),
                    PartPose.offset(Mth.cos(a) * 9, Mth.sin(a * 2) * 4, Mth.sin(a) * 9));
        }
        PartDefinition tail = r.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 5; i++) {
            float w = 7 - i * 1.2F;
            tail.addOrReplaceChild("t" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2, -w / 2, 0, w, w, 6),
                    PartPose.offset(0, 0, 4 + i * 6));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override public boolean shouldRender(JutsuFxEntity e, Frustum f, double x, double y, double z) { return true; }
    @Override public ResourceLocation getTextureLocation(JutsuFxEntity e) { return TEX; }

    @Override
    public void render(JutsuFxEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        int st = Mth.clamp(e.style(), 0, COL.length - 1);
        float age = e.tickCount + pt;
        float size = e.size();
        ps.pushPose();
        ps.translate(0, e.getBbHeight() * 0.5, 0);
        ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.lerp(pt, e.yRotO, e.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        float s = size * 0.09F;                       // model units are 1/16 block; size 1.0 ~ one block across
        ps.scale(-s, -s, s);
        model.pose(st, age);
        float[] c = COL[st];
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.7F);
        model.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(TEX)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c[0] * pulse, c[1] * pulse, c[2] * pulse, 1F);
        ps.scale(1.6F, 1.6F, 1.6F);                   // soft halo
        model.renderToBuffer(ps, buf.getBuffer(RenderType.eyes(TEX)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c[0] * 0.25F, c[1] * 0.25F, c[2] * 0.25F, 1F);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    private static final class Fx extends Model {
        private final ModelPart root, core, ring, chunks, tail;

        Fx(ModelPart root) {
            super(RenderType::eyes);
            this.root = root;
            core = root.getChild("core");
            ring = root.getChild("ring");
            chunks = root.getChild("chunks");
            tail = root.getChild("tail");
        }

        void pose(int st, float age) {
            root.getAllParts().forEach(ModelPart::resetPose);
            boolean fire = st == 0 || st == 8 || st == 11, water = st == 1 || st == 9, wind = st == 2, bolt = st == 3,
                    earth = st == 4 || st == 10, dark = st == 5, chakra = st == 6 || st == 7;
            core.visible = !wind && !bolt;
            ring.visible = true;
            chunks.visible = earth || water;
            tail.visible = fire || wind || bolt || dark;
            core.xScale = core.yScale = core.zScale = earth ? 1.1F : chakra ? 1.25F : 0.85F;
            core.yRot = age * 0.2F; core.xRot = age * 0.13F;
            ring.zRot = age * (wind ? 0.9F : 0.35F);
            ring.yRot = wind ? 1.2F : 0F;
            float len = bolt ? 1.8F : wind ? 0.5F : water ? 1.2F : 1F;
            ring.xScale = ring.yScale = wind ? 1.4F : 1F;
            ring.zScale = len;
            chunks.yRot = age * 0.3F; chunks.xRot = age * 0.1F;
            tail.zScale = bolt ? 2.2F : 1F;
            for (int i = 0; i < 5; i++) {
                ModelPart t = tail.getChild("t" + i);
                t.xRot = Mth.sin(age * 0.5F - i * 0.9F) * 0.18F * (bolt ? 2.5F : 1F);
                t.yRot = Mth.cos(age * 0.43F - i * 0.8F) * 0.18F * (bolt ? 2.5F : 1F);
            }
        }

        @Override
        public void renderToBuffer(PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            root.render(ps, vc, light, overlay, r, g, b, a);
        }
    }
}
