package com.mayhem.client;

import com.mayhem.MayhemMod;
import com.mayhem.config.MayhemClientConfig;
import com.mayhem.network.ActionPacket;
import com.mayhem.network.MayhemNetwork;
import com.mayhem.network.StatusRequestPacket;
import com.mayhem.network.UnarmedAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only wiring: key registration, input handling, lock-on, cinematic locks and the overlay. */
public final class MayhemClientEvents {
    private MayhemClientEvents() {}

    @Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(MayhemKeys.ATTACK);
            event.register(MayhemKeys.DODGE);
            event.register(MayhemKeys.GUARD);
            event.register(MayhemKeys.SLAM);
            event.register(MayhemKeys.ULTIMATE);
            event.register(MayhemKeys.LOCK_ON);
            event.register(MayhemKeys.STATUS);
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("mayhem_hud", HudRenderer::render);
        }
    }

    @Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private static LocalPlayer lastPlayer;
        private static boolean guardSent;
        private static long lastAttackSend = -100;

        private ForgeBus() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (event.phase == TickEvent.Phase.START) {
                if (mc.player != null && mc.level != null) CombatMode.stealAttackInput(mc);
                return;
            }
            LocalPlayer pl = mc.player;

            if (pl != lastPlayer) { // joined, respawned or changed world: reset transient client state
                lastPlayer = pl;
                guardSent = false;
                CombatMode.reset();
                LockOn.clear();
                ClientAnimations.clear();
            }
            if (pl == null || mc.level == null) {
                drain();
                return;
            }

            CinematicDirector.tick(mc);
            LockOn.tick(mc);

            boolean canAct = mc.screen == null && !pl.isSpectator() && !pl.isDeadOrDying()
                    && !CinematicDirector.isInputLocked();
            long now = mc.level.getGameTime();

            // ---- combat mode toggle (R) ----
            while (MayhemKeys.ATTACK.consumeClick()) {
                if (mc.screen == null && !CinematicDirector.isActive()) {
                    CombatMode.toggle();
                    pl.displayClientMessage(Component.translatable(CombatMode.active()
                            ? "message.mayhem.mode_on" : "message.mayhem.mode_off"), true);
                }
            }

            // ---- strike: in combat mode M1 is the anime strike (tap, or hold to repeat), held items ignored ----
            boolean tapped = CombatMode.takeTap();
            if (canAct && CombatMode.active()) {
                boolean held = MayhemClientConfig.HOLD_TO_ATTACK.get() && CombatMode.isHeld();
                if ((tapped || held) && now - lastAttackSend >= (tapped ? 1 : 4)) {
                    MayhemNetwork.CHANNEL.sendToServer(new UnarmedAttackPacket(LockOn.id()));
                    lastAttackSend = now;
                }
            }

            // ---- dodge / flash step ----
            while (MayhemKeys.DODGE.consumeClick()) {
                if (canAct) {
                    Input in = pl.input;
                    MayhemNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.Type.DODGE,
                            Math.signum(in.forwardImpulse), Math.signum(in.leftImpulse), LockOn.id()));
                }
            }

            // ---- guard (hold) ----
            boolean guardDown = canAct && (MayhemKeys.GUARD.isDown() || (CombatMode.active() && mc.options.keyUse.isDown()));
            while (MayhemKeys.GUARD.consumeClick()) { /* state is polled via isDown */ }
            if (guardDown != guardSent) {
                guardSent = guardDown;
                MayhemNetwork.CHANNEL.sendToServer(new ActionPacket(
                        guardDown ? ActionPacket.Type.GUARD_DOWN : ActionPacket.Type.GUARD_UP, 0F, 0F, -1));
            }

            // ---- ground slam ----
            while (MayhemKeys.SLAM.consumeClick()) {
                if (canAct) {
                    MayhemNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.Type.SLAM, 0F, 0F, -1));
                }
            }

            // ---- ultimate: X = Barrage on your target, Shift+X = Meteor ----
            while (MayhemKeys.ULTIMATE.consumeClick()) {
                if (canAct) {
                    MayhemNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.Type.ULTIMATE,
                            pl.isShiftKeyDown() ? 1F : 0F, 0F, LockOn.id()));
                }
            }

            // ---- lock-on toggle / status ----
            while (MayhemKeys.LOCK_ON.consumeClick()) {
                if (mc.screen == null && !CinematicDirector.isActive()) LockOn.toggle(mc);
            }
            while (MayhemKeys.STATUS.consumeClick()) {
                if (canAct) MayhemNetwork.CHANNEL.sendToServer(new StatusRequestPacket());
            }
        }

        private static void drain() {
            while (MayhemKeys.ATTACK.consumeClick()) { }
            while (MayhemKeys.DODGE.consumeClick()) { }
            while (MayhemKeys.GUARD.consumeClick()) { }
            while (MayhemKeys.SLAM.consumeClick()) { }
            while (MayhemKeys.ULTIMATE.consumeClick()) { }
            while (MayhemKeys.LOCK_ON.consumeClick()) { }
            while (MayhemKeys.STATUS.consumeClick()) { }
        }

        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                CinematicDirector.frame(event.renderTickTime);
            }
        }

        /** While an ultimate plays, the player is a passenger of the script: no walking, jumping or sneaking. */
        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            if (!CinematicDirector.isInputLocked()) return;
            Input in = event.getInput();
            in.up = false;
            in.down = false;
            in.left = false;
            in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
            in.forwardImpulse = 0F;
            in.leftImpulse = 0F;
        }

        @SubscribeEvent
        public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
            if (CinematicDirector.isActive() || (CombatMode.active() && (event.isAttack() || event.isUseItem()))) event.setCanceled(true);
        }

        /** Hide every vanilla HUD element during a cinematic; our own overlay still draws the bars/effects. */
        @SubscribeEvent
        public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
            if (CinematicDirector.isActive() && "minecraft".equals(event.getOverlay().id().getNamespace())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            CinematicDirector.finish(Minecraft.getInstance());
            ClientProgressionData.reset();
            ClientAnimations.clear();
            CombatMode.reset();
            LockOn.clear();
        }
    }
}
