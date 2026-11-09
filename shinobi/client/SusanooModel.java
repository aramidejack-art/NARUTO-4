package com.mayhem.shinobi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Staged Spectral Guardian / Susanoo model rendered around the player.
 * Stage 1: ribcage   Stage 2: + arms & blade   Stage 3: + armor plates & bow
 * Stage 4: full body, helmet, oversized blade
 */
public class SusanooModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("mayhem", "susanoo"), "main");

    private final ModelPart root, ribs, spine, armL, armR, blade, bow, helm, pauldronL, pauldronR, skirt, legL, legR;

    public SusanooModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        ribs = root.getChild("ribs");
        spine = root.getChild("spine");
        armL = root.getChild("arm_l");
        armR = root.getChild("arm_r");
        blade = armR.getChild("blade");
        bow = root.getChild("bow");
        helm = root.getChild("helm");
        pauldronL = root.getChild("pauldron_l");
        pauldronR = root.getChild("pauldron_r");
        skirt = root.getChild("skirt");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        CubeDeformation d = new CubeDeformation(0.2F);

        // Ribcage cage around torso
        PartDefinition ribs = r.addOrReplaceChild("ribs", CubeListBuilder.create(), PartPose.offset(0, 6, 0));
        for (int i = 0; i < 6; i++) {
            float y = -4 + i * 2.2F;
            float w = 10 - Math.abs(i - 2) * 0.8F;
            ribs.addOrReplaceChild("rib" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2, y, -6, w, 1.2F, 12, d),
                    PartPose.ZERO);
        }
        r.addOrReplaceChild("spine",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -8, 4, 3, 18, 3, d),
                PartPose.offset(0, 6, 0));

        PartDefinition armL = r.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(0, 20).addBox(-3, -2, -2.5F, 4, 16, 5, d),
                PartPose.offset(-9, 2, 0));
        PartDefinition armR = r.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(0, 20).addBox(-1, -2, -2.5F, 4, 16, 5, d),
                PartPose.offset(9, 2, 0));
        armR.addOrReplaceChild("blade",
                CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, 12, -1, 1.2F, 22, 2.5F, d),
                PartPose.ZERO);

        r.addOrReplaceChild("bow",
                CubeListBuilder.create().texOffs(20, 20).addBox(-1, -10, 0, 2, 20, 1.5F, d),
                PartPose.offset(-10, 4, 2));

        r.addOrReplaceChild("helm",
                CubeListBuilder.create().texOffs(0, 48).addBox(-5, -12, -5, 10, 8, 10, d),
                PartPose.offset(0, 0, 0));
        r.addOrReplaceChild("pauldron_l",
                CubeListBuilder.create().texOffs(40, 24).addBox(-5, -3, -4, 6, 5, 8, d),
                PartPose.offset(-8, 0, 0));
        r.addOrReplaceChild("pauldron_r",
                CubeListBuilder.create().texOffs(40, 24).addBox(-1, -3, -4, 6, 5, 8, d),
                PartPose.offset(8, 0, 0));
        r.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(0, 66).addBox(-7, 0, -4, 14, 10, 8, d),
                PartPose.offset(0, 12, 0));
        r.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(48, 48).addBox(-2.5F, 0, -2.5F, 5, 12, 5, d),
                PartPose.offset(-3.5F, 12, 0));
        r.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(48, 48).addBox(-2.5F, 0, -2.5F, 5, 12, 5, d),
                PartPose.offset(3.5F, 12, 0));

        return LayerDefinition.create(mesh, 64, 96);
    }

    /** Show/hide and pose parts for the active guardian stage (1-4). */
    public void setupStage(int stage, float age, float bodyYaw) {
        boolean s2 = stage >= 2, s3 = stage >= 3, s4 = stage >= 4;
        ribs.visible = stage >= 1;
        spine.visible = stage >= 1;
        armL.visible = s2; armR.visible = s2; blade.visible = s2;
        bow.visible = s3;
        helm.visible = s4;
        pauldronL.visible = s3; pauldronR.visible = s3;
        skirt.visible = s4; legL.visible = s4; legR.visible = s4;

        float pulse = Mth.sin(age * 0.08F) * 0.03F;
        ribs.yScale = 1F + pulse;
        ribs.xScale = 1F + pulse * 0.5F;
        armR.xRot = -0.4F + Mth.sin(age * 0.15F) * 0.1F;
        armL.xRot = -0.25F + Mth.cos(age * 0.12F) * 0.08F;
        if (s2) blade.zRot = 0.15F + Mth.sin(age * 0.2F) * 0.05F;
        if (s3) bow.zRot = -0.3F;
        root.yRot = bodyYaw * Mth.DEG_TO_RAD;
    }

    /** Adds walking, punches (act 1 left / 2 right = blade slash), tackle (3) and cast pose (4) on top of {@link #setupStage}. */
    public void setupAct(float walkPos, float walkAmt, int act, float actT) {
        float walk = walkPos * 0.6F, amt = Math.min(1F, walkAmt * 1.4F);
        float swing = act == 0 ? 0F : Mth.sin(Mth.clamp(actT, 0F, 1F) * Mth.PI);
        float hit = act == 0 ? 0F : Mth.sin(Mth.clamp(actT * 1.3F, 0F, 1F) * Mth.PI);
        armL.xRot += Mth.cos(walk) * 0.55F * amt;
        armR.xRot += -Mth.cos(walk) * 0.55F * amt;
        legL.xRot = -Mth.cos(walk) * 0.7F * amt;
        legR.xRot = Mth.cos(walk) * 0.7F * amt;
        root.xRot = 0F;
        if (act == 1) { armL.xRot += -hit * 2.0F; armL.zRot += hit * 0.2F; root.yRot += hit * 0.25F; }
        else if (act == 2) { armR.xRot += -hit * 2.4F; blade.zRot += -hit * 0.9F; root.yRot -= hit * 0.25F; }
        else if (act == 3) { root.xRot = 0.35F * swing; armL.xRot += swing * 0.9F; armR.xRot += swing * 0.9F; legL.xRot -= swing * 0.5F; legR.xRot += swing * 0.5F; }
        else if (act == 4) { armL.xRot += -swing * 1.4F; armR.xRot += -swing * 1.4F; armL.zRot += swing * 0.5F; armR.zRot -= swing * 0.5F; }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(ps, vc, light, overlay, r, g, b, a);
    }
}
