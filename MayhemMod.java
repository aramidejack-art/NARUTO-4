package com.mayhem;

import com.mayhem.combat.CombatState;
import com.mayhem.combat.Ultimates;
import com.mayhem.config.MayhemClientConfig;
import com.mayhem.config.MayhemConfig;
import com.mayhem.fx.MayhemSounds;
import com.mayhem.network.MayhemNetwork;
import com.mayhem.progression.MobLevelHandler;
import com.mayhem.progression.PlayerProgression;
import com.mayhem.shinobi.ShinobiConfig;
import com.mayhem.shinobi.ability.Abilities;
import com.mayhem.shinobi.events.ShinobiEvents;
import com.mayhem.shinobi.combat.EnemyJinchuriki;
import com.mayhem.shinobi.registry.ShinobiRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MayhemMod.MOD_ID)
public final class MayhemMod {
    public static final String MOD_ID = "mayhem";

    public MayhemMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        MayhemSounds.SOUNDS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MayhemConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MayhemClientConfig.SPEC);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ShinobiConfig.SPEC, "mayhem-shinobi.toml");
        ShinobiRegistry.register(modBus);
        MinecraftForge.EVENT_BUS.register(new ShinobiEvents());
        MinecraftForge.EVENT_BUS.register(new MobLevelHandler());
        MinecraftForge.EVENT_BUS.register(new EnemyJinchuriki());
        MinecraftForge.EVENT_BUS.register(new PlayerEvents());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MayhemNetwork::register);
        event.enqueueWork(Abilities::ensureLoaded);
    }

    /** Keeps player progression alive across death/respawn and keeps the client HUD in sync. */
    public static final class PlayerEvents {
        @SubscribeEvent
        public void onClone(PlayerEvent.Clone event) {
            PlayerProgression.copy(event.getOriginal(), event.getEntity());
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                player.setNoGravity(false); // safety net if the server stopped mid-ultimate
                PlayerProgression.sync(player);
            }
        }

        @SubscribeEvent
        public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PlayerProgression.sync(player);
            }
        }

        @SubscribeEvent
        public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Ultimates.abort(player);
                PlayerProgression.sync(player);
            }
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Ultimates.abort(player);
                CombatState.remove(player.getUUID());
            }
        }
    }
}
