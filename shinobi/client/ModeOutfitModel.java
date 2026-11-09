package com.mayhem.shinobi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The nine Beast Chakra Mode outfits (generated geometry in {@link ModeDesigns}). The six body parts copy the player's own pose
 * every frame; hoods, horns, pauldrons, belts and gauntlets ride on them, while flowing cloaks, tails, tentacles and wings are
 * animated procedurally so the outfit ripples when you move.
 */
public class ModeOutfitModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("mayhem", "mode_outfit"), "main");

    private record Anim(ModelPart part, String kind, int k, int j, float sign, float seed) {}

    private final Map<String, ModelPart> tops = new HashMap<>();
    private final Map<String, List<Anim>> anims = new HashMap<>();
    private ModelPart active;
    private String activeId = "nine_fox";

    public ModeOutfitModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        for (String id : ModeDesigns.IDS) {
            ModelPart top = root.getChild("d_" + id);
            tops.put(id, top);
            List<Anim> list = new ArrayList<>();
            for (String[] row : ModeDesigns.anims(id)) {
                ModelPart p = top;
                for (String seg : row[0].split("/")) p = p.getChild(seg);
                String[] t = row[1].split(":");
                list.add(new Anim(p, t[0], t.length > 1 ? (int) Float.parseFloat(t[1]) : 0, t.length > 2 ? Integer.parseInt(t[2]) : 0,
                        row[0].endsWith("_r") ? -1F : 1F, (row[0].hashCode() & 255) * 0.1F));
            }
            anims.put(id, list);
        }
        active = tops.get(activeId);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (String id : ModeDesigns.IDS) {
            PartDefinition top = root.addOrReplaceChild("d_" + id, CubeListBuilder.create(), PartPose.ZERO);
            ModeDesigns.build(id, top);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** @param src the (already animated) player model whose pose the outfit copies */
    public void animate(String id, PlayerModel<?> src, float age, float walkAmt) {
        if (!tops.containsKey(id)) id = "nine_fox";
        activeId = id;
        active = tops.get(id);
        active.getAllParts().forEach(ModelPart::resetPose);
        active.getChild("head").copyFrom(src.head);
        active.getChild("body").copyFrom(src.body);
        active.getChild("arm_r").copyFrom(src.rightArm);
        active.getChild("arm_l").copyFrom(src.leftArm);
        active.getChild("leg_r").copyFrom(src.rightLeg);
        active.getChild("leg_l").copyFrom(src.leftLeg);
        float amt = Math.min(1F, walkAmt * 1.5F);
        for (Anim an : anims.get(id)) {
            ModelPart p = an.part();
            float k = an.k(), j = an.j();
            switch (an.kind()) {
                case "cloak" -> {
                    p.xRot += 0.05F + amt * 0.22F + Mth.sin(age * 0.14F + k * 0.7F - j * 0.85F) * (0.09F + amt * 0.07F);
                    p.yRot += Mth.cos(age * 0.11F + k * 0.5F - j * 0.6F) * 0.05F;
                    p.zRot += Mth.sin(age * 0.13F + k * 0.9F - j * 0.7F) * 0.04F;
                }
                case "tail" -> {
                    p.xRot += Mth.sin(age * 0.09F + k * 0.7F - j * 0.55F) * 0.09F + (j == 0 ? amt * 0.15F : 0F);
                    p.yRot += Mth.cos(age * 0.075F + k * 0.9F - j * 0.5F) * 0.12F;
                }
                case "tent" -> {
                    p.yRot += Mth.sin(age * 0.1F + k - j * 0.6F) * 0.22F;
                    p.xRot += Mth.cos(age * 0.08F + k * 1.3F - j * 0.5F) * 0.12F + amt * 0.1F;
                }
                case "flick" -> {
                    p.xRot += Mth.sin(age * 0.3F + an.seed()) * 0.08F;
                    p.zRot += Mth.cos(age * 0.27F + an.seed()) * 0.08F;
                }
                case "stalk" -> {
                    p.xRot += Mth.sin(age * 0.1F + an.seed()) * 0.15F;
                    p.zRot += an.sign() * Mth.cos(age * 0.08F + an.seed()) * 0.12F;
                }
                case "wing" -> p.zRot += an.sign() * -(0.04F + amt * 0.25F + Mth.sin(age * 0.5F) * 0.04F);
                case "wing2" -> p.zRot += an.sign() * -(0.1F + amt * 0.5F + Mth.sin(age * 0.9F) * 0.2F);
                default -> { }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        active.render(ps, vc, light, overlay, r, g, b, a);
    }
}
