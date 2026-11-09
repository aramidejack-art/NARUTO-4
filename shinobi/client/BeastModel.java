package com.mayhem.shinobi.client;

import com.mayhem.shinobi.beast.BeastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven tailed-beast model. One baked layer holds all nine original beast designs
 * (see {@link BeastDesigns}, generated); only the design chosen by {@link #select(String)} is drawn.
 * Every part carries an animation tag ("legA", "tail:3:2", "tent:5:1", "wing", ...) that {@link #animate} interprets, so each
 * beast moves with its own anatomy. The same model serves the wild beast entity and the player's hollow chakra avatar,
 * which can walk on all fours or rise onto its hind legs ({@code stand}), throw punches and tackle.
 */
public class BeastModel extends EntityModel<BeastEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("mayhem", "tailed_beast"), "main");
    private static final int TEX = 256;

    public static final int ACT_NONE = 0, ACT_PUNCH_L = 1, ACT_PUNCH_R = 2, ACT_TACKLE = 3, ACT_ROAR = 4;

    /** front: part belongs to a fore-limb (arm when standing); side: +1 left, -1 right, 0 none. */
    private record Anim(ModelPart part, String kind, int a, int b, float f, float sign, float seed, boolean front, int side) {}

    private final Map<String, ModelPart> roots = new HashMap<>();
    private final Map<String, ModelPart> stands = new HashMap<>();
    private final Map<String, List<Anim>> anims = new HashMap<>();
    private ModelPart active;
    private ModelPart activeStand;
    private String activeId = "sand_tanuki";

    public BeastModel(ModelPart root) {
        for (String id : BeastDesigns.IDS) {
            ModelPart top = root.getChild("d_" + id);
            ModelPart stand = top.getChild("stand");
            ModelPart main = stand.getChild("main");
            roots.put(id, top);
            stands.put(id, stand);
            List<Anim> list = new ArrayList<>();
            for (String[] row : BeastDesigns.anims(id)) {
                ModelPart p = main;
                String last = "";
                for (String seg : row[0].split("/")) { p = p.getChild(seg); last = seg; }
                String[] t = row[1].split(":");
                int a = t.length > 1 ? (int) Float.parseFloat(t[1]) : 0;
                int b = t.length > 2 ? Integer.parseInt(t[2]) : 0;
                float f = t.length > 1 ? Float.parseFloat(t[1]) : 0F;
                float sign = row[0].endsWith("_r") ? -1F : 1F;
                boolean front = false;
                int side = 0;
                String[] path = row[0].split("/");
                String limb = path[path.length - 1];
                for (String seg : path) {
                    if (seg.startsWith("leg0") || seg.startsWith("arm")) {
                        front = true;
                        char sc = seg.charAt(seg.startsWith("arm") ? 3 : 4);
                        side = sc == 'l' ? 1 : sc == 'r' ? -1 : 0;
                        break;
                    }
                }
                list.add(new Anim(p, t[0], a, b, f, sign, (row[0].hashCode() & 255) * 0.1F, front, side));
            }
            anims.put(id, list);
        }
        select(activeId);
    }

    public void select(String id) {
        if (id == null || !roots.containsKey(id)) id = "sand_tanuki";
        activeId = id;
        active = roots.get(id);
        activeStand = stands.get(id);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (String id : BeastDesigns.IDS) {
            float hz = BeastDesigns.stand(id)[0];
            PartDefinition top = root.addOrReplaceChild("d_" + id, CubeListBuilder.create(), PartPose.offset(0F, 24F, 0F));
            PartDefinition stand = top.addOrReplaceChild("stand", CubeListBuilder.create(), PartPose.offset(0F, 0F, hz));
            PartDefinition main = stand.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0F, 0F, -hz));
            BeastDesigns.build(id, main);
        }
        return LayerDefinition.create(mesh, TEX, TEX);
    }

    @Override
    public void setupAnim(BeastEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        animate(e.beastId(), e.isWeakened(), e.isTelegraphing(), 0F, ACT_NONE, 0F, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
    }

    /**
     * Poses the selected design.
     * @param stand 0 = all fours, 1 = risen onto the hind legs (eased by the caller)
     * @param act   one of ACT_* (punch left/right, tackle, roar), {@code actT} its progress 0..1
     */
    public void animate(String beastId, boolean weak, boolean wind, float stand, int act, float actT,
                        float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        select(beastId);
        active.getAllParts().forEach(ModelPart::resetPose);
        float walk = limbSwing * 0.6F;
        float amt = Math.min(1F, limbSwingAmount * 1.4F);
        float yawR = netHeadYaw * Mth.DEG_TO_RAD;
        float pitR = headPitch * Mth.DEG_TO_RAD;
        float swing = act == ACT_NONE ? 0F : Mth.sin(Mth.clamp(actT, 0F, 1F) * Mth.PI);
        float hit = act == ACT_NONE ? 0F : Mth.sin(Mth.clamp(actT * 1.35F, 0F, 1F) * Mth.PI);

        for (Anim an : anims.get(activeId)) {
            ModelPart p = an.part();
            switch (an.kind()) {
                case "legA", "legB" -> {
                    float ph = an.kind().equals("legB") ? Mth.PI : 0F;
                    float k = an.front() ? 1F - stand : 1F;
                    p.xRot += Mth.cos(walk + ph) * 0.9F * amt * k;
                    if (an.front()) {
                        p.xRot += -stand * 0.75F + stand * Mth.sin(age * 0.07F) * 0.05F;
                        if (act == ACT_PUNCH_L || act == ACT_PUNCH_R) {
                            if (an.side() == (act == ACT_PUNCH_L ? 1 : -1)) { p.xRot += -hit * 2.0F; p.zRot += an.side() * -hit * 0.25F; }
                            else p.xRot += hit * 0.5F;
                        } else if (act == ACT_TACKLE) p.xRot += -swing * 1.1F;
                        else if (act == ACT_ROAR) p.xRot += -swing * 0.9F;
                    } else if (act == ACT_TACKLE) p.xRot += swing * 0.8F;
                }
                case "legAlo", "legBlo" -> {
                    float ph = an.kind().equals("legBlo") ? Mth.PI : 0F;
                    float k = an.front() ? 1F - stand : 1F;
                    p.xRot += Math.max(0F, -Mth.cos(walk + ph)) * 0.8F * amt * k;
                    if (an.front()) {
                        p.xRot += stand * 0.7F;
                        if (act == ACT_PUNCH_L || act == ACT_PUNCH_R) {
                            if (an.side() == (act == ACT_PUNCH_L ? 1 : -1)) p.xRot += -hit * 0.8F;
                        }
                    }
                }
                case "swA", "swB" -> {
                    float ph = an.kind().equals("swB") ? Mth.PI : 0F;
                    p.yRot += Mth.cos(walk + ph) * 0.35F * amt * an.sign();
                }
                case "head" -> {
                    p.yRot += yawR * an.f() * 0.5F;
                    p.xRot += pitR * an.f() * 0.5F + (weak ? 0.12F : 0F) + (wind ? -0.12F : 0F)
                            + stand * 0.5F * an.f() + (act == ACT_TACKLE ? swing * 0.5F * an.f() : 0F) - (act == ACT_ROAR ? swing * 0.6F * an.f() : 0F);
                }
                case "jaw" -> p.xRot += 0.04F + (Mth.sin(age * 0.11F) + 1F) * 0.04F + (wind ? 0.7F : 0F) + (weak ? 0.25F : 0F) + (act == ACT_ROAR ? swing * 0.9F : 0F) + (act == ACT_TACKLE ? swing * 0.5F : 0F);
                case "ear" -> p.zRot += an.sign() * (Mth.sin(age * 0.22F + an.seed()) * 0.12F + (wind ? 0.25F : 0F) + (act == ACT_TACKLE ? swing * 0.5F : 0F));
                case "tail" -> {
                    float k = an.a(), j = an.b();
                    float lash = (act == ACT_PUNCH_L || act == ACT_PUNCH_R) ? hit * 0.18F * (act == ACT_PUNCH_L ? 1 : -1) : 0F;
                    p.xRot += Mth.sin(age * 0.07F + k * 0.7F - j * 0.5F) * 0.07F + (j == 0 && wind ? 0.35F : 0F) + (j == 0 && weak ? -0.4F : 0F)
                            + (j == 0 ? stand * 0.25F : 0F) + (act == ACT_TACKLE ? swing * -0.18F : 0F) + (act == ACT_ROAR ? swing * 0.2F : 0F);
                    p.yRot += Mth.cos(age * 0.06F + k * 0.9F - j * 0.45F) * 0.09F - yawR * 0.02F + lash + Mth.sin(walk * 0.5F + j * 0.5F + k) * 0.05F * amt;
                }
                case "tent" -> {
                    float k = an.a(), j = an.b();
                    p.yRot += Mth.sin(age * 0.1F + k - j * 0.6F) * 0.2F + (act == ACT_PUNCH_L || act == ACT_PUNCH_R ? hit * 0.25F * (k % 2 == 0 ? 1 : -1) : 0F);
                    p.xRot += Mth.cos(age * 0.08F + k * 1.3F - j * 0.5F) * 0.1F + (wind ? -0.1F * (j + 1) : 0F) + (act == ACT_ROAR ? -swing * 0.12F * (j + 1) : 0F);
                }
                case "flick" -> {
                    p.xRot += Mth.sin(age * 0.3F + an.seed()) * 0.07F;
                    p.zRot += Mth.cos(age * 0.27F + an.seed()) * 0.07F;
                }
                case "float" -> {
                    p.y += Mth.sin(age * 0.1F + an.a()) * 1.6F;
                    p.x += Mth.cos(age * 0.07F + an.a() * 1.7F) * 1.2F;
                }
                case "slither" -> p.yRot += Mth.sin(age * 0.12F - an.a() * 1.0F + walk * 0.5F) * 0.12F * (0.5F + amt);
                case "stalk" -> {
                    p.xRot += Mth.sin(age * 0.1F + an.seed()) * 0.15F;
                    p.zRot += an.sign() * Mth.cos(age * 0.08F + an.seed()) * 0.12F;
                }
                case "wing" -> p.zRot += an.sign() * -(wind || act == ACT_TACKLE ? 0.55F : Mth.sin(age * 0.5F) * 0.03F);
                case "wing2" -> p.zRot += an.sign() * -(wind || act == ACT_TACKLE ? 0.9F : 0.08F + Mth.sin(age * 0.9F) * 0.22F);
                case "breath" -> p.xRot += Mth.sin(age * 0.06F) * 0.025F;
                default -> { }
            }
        }
        // rising onto the hind legs pivots the whole body about the hind feet
        float[] st = BeastDesigns.stand(activeId);
        activeStand.xRot = -st[1] * stand;
        activeStand.y -= stand * 1.5F;
        // whole-body pose: kneel when weakened, rear up when telegraphing, lean into a tackle, idle sway otherwise
        if (weak) {
            active.y += 5F;
            active.xRot = 0.12F;
        } else if (wind) {
            active.xRot = -0.18F;
            active.y -= 2F;
        } else if (act == ACT_TACKLE) {
            active.xRot = 0.32F * swing - 0.1F * (1F - swing);
            active.yRot = 0F;
        } else if (act == ACT_PUNCH_L || act == ACT_PUNCH_R) {
            active.yRot = (act == ACT_PUNCH_L ? -1F : 1F) * hit * 0.28F;
            active.xRot = Mth.sin(age * 0.05F) * 0.015F;
        } else {
            active.xRot = Mth.sin(age * 0.05F) * 0.015F + Mth.sin(walk * 2F) * 0.02F * amt;
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        active.render(ps, vc, light, overlay, r, g, b, a);
    }
}
