package com.mayhem.shinobi.client;

import com.mayhem.MayhemMod;
import com.mayhem.shinobi.beast.BeastEntity;
import com.mayhem.shinobi.beast.Beasts;
import com.mayhem.shinobi.npc.NpcEntity;
import com.mayhem.shinobi.npc.TrainingDummyEntity;
import com.mayhem.shinobi.npc.EscortClientEntity;
import com.mayhem.shinobi.npc.RogueNinjaEntity;
import com.mayhem.shinobi.npc.WandererEntity;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Entity renderers: NPCs, detailed beasts, Susanoo/Guardian player layer. */
@Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ShinobiRenderers {
    private ShinobiRenderers() {}

    @SubscribeEvent
    public static void layerDefs(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(BeastModel.LAYER, BeastModel::createLayer);
        e.registerLayerDefinition(SusanooModel.LAYER, SusanooModel::createLayer);
        e.registerLayerDefinition(JutsuFxRenderer.LAYER, JutsuFxRenderer::createLayer);
        e.registerLayerDefinition(ModeOutfitModel.LAYER, ModeOutfitModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ShinobiRegistry.NPC.get(), NpcRenderer::new);
        e.registerEntityRenderer(ShinobiRegistry.ROGUE_NINJA.get(), c -> new HumanoidMobRenderer<>(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)), 0.5F) {
            @Override public ResourceLocation getTextureLocation(RogueNinjaEntity e) {
                return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/npc/rogue_ninja.png");
            }
        });
        e.registerEntityRenderer(ShinobiRegistry.WANDERER.get(), c -> new HumanoidMobRenderer<>(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)), 0.5F) {
            @Override public ResourceLocation getTextureLocation(WandererEntity e) {
                return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/npc/wanderer_" + e.rank().name().toLowerCase(java.util.Locale.ROOT) + ".png");
            }
        });
        e.registerEntityRenderer(ShinobiRegistry.BEAST.get(), BeastRenderer::new);
        e.registerEntityRenderer(ShinobiRegistry.AVATAR.get(), ChakraAvatarRenderer::new);
        e.registerEntityRenderer(ShinobiRegistry.JUTSU_FX.get(), JutsuFxRenderer::new);
        e.registerEntityRenderer(ShinobiRegistry.ESCORT_CLIENT.get(), c -> new HumanoidMobRenderer<>(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)), 0.5F) {
            @Override public ResourceLocation getTextureLocation(EscortClientEntity e) {
                return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/npc/escort_client.png");
            }
        });
        e.registerEntityRenderer(ShinobiRegistry.TRAINING_DUMMY.get(), c -> new HumanoidMobRenderer<>(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)), 0.5F) {
            @Override public ResourceLocation getTextureLocation(TrainingDummyEntity e) {
                return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/npc/training_dummy.png");
            }
        });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer pr) {
                pr.addLayer(new AppearanceLayer(pr));
            }
        }
    }

    static final class NpcRenderer extends HumanoidMobRenderer<NpcEntity, HumanoidModel<NpcEntity>> {
        NpcRenderer(EntityRendererProvider.Context c) { super(c, new HumanoidModel<>(c.bakeLayer(ModelLayers.PLAYER)), 0.5F); }
        @Override public ResourceLocation getTextureLocation(NpcEntity e) {
            return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/npc/" + e.role().tag + ".png");
        }
    }

    static final class BeastRenderer extends MobRenderer<BeastEntity, BeastModel> {
        BeastRenderer(EntityRendererProvider.Context c) {
            super(c, new BeastModel(c.bakeLayer(BeastModel.LAYER)), 2.6F);
            this.addLayer(new BeastGlowLayer(this));
        }
        static ResourceLocation tex(BeastEntity e, boolean glow) {
            String id = e.beastId().isEmpty() ? "sand_tanuki" : e.beastId();
            return new ResourceLocation(MayhemMod.MOD_ID, "textures/entity/beast/" + id + (glow ? "_glow.png" : ".png"));
        }
        @Override public ResourceLocation getTextureLocation(BeastEntity e) { return tex(e, false); }
        @Override protected void scale(BeastEntity e, PoseStack ps, float pt) {
            Beasts.Beast b = Beasts.get(e.beastId());
            float s = (float) (b == null ? 1.0 : (0.9 + 0.05 * b.tails()) * b.scale());
            ps.scale(s, s, s);
        }
    }

    /** Emissive layer: lava cracks, flame tails, eyes and slime spots glow in the dark. */
    static final class BeastGlowLayer extends RenderLayer<BeastEntity, BeastModel> {
        BeastGlowLayer(RenderLayerParent<BeastEntity, BeastModel> parent) { super(parent); }
        @Override public void render(PoseStack ps, MultiBufferSource buf, int light, BeastEntity e, float limbSwing, float limbSwingAmount,
                                     float partial, float age, float netHeadYaw, float headPitch) {
            if (e.isInvisible()) return;
            getParentModel().renderToBuffer(ps, buf.getBuffer(RenderType.eyes(BeastRenderer.tex(e, true))), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        }
    }
}
