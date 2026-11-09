package com.mayhem.shinobi.client;

import com.mayhem.MayhemMod;
import com.mayhem.client.CombatMode;
import com.mayhem.shinobi.data.ShinobiData;
import com.mayhem.shinobi.net.ShinobiNet.Op;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shinobi input. While Combat Mode is on, digits 1-9/0 cast slots 1-10 and the wheel cycles the selected slot. */
public final class ShinobiClientEvents {
    private ShinobiClientEvents() {}

    @Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(ShinobiKeys.MENU); e.register(ShinobiKeys.STYLE); e.register(ShinobiKeys.CHARGE); e.register(ShinobiKeys.MODE);
            e.register(ShinobiKeys.CLOAK); e.register(ShinobiKeys.AWAKEN); e.register(ShinobiKeys.GUARDIAN); e.register(ShinobiKeys.STANCE); e.register(ShinobiKeys.TACKLE); e.register(ShinobiKeys.BEAST_MODE); e.register(ShinobiKeys.SKILL1); e.register(ShinobiKeys.SKILL2);
            e.register(ShinobiKeys.CAST_SELECTED); e.register(ShinobiKeys.WEAPON); e.register(ShinobiKeys.SUPPORT); e.register(ShinobiKeys.SHINOBI_ULT);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("mayhem_shinobi_hud", ShinobiHud::render);
        }
    }

    @Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private static LocalPlayer last;
        private static boolean chargeSent;

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void tick(TickEvent.ClientTickEvent e) {
            Minecraft mc = Minecraft.getInstance();
            if (e.phase == TickEvent.Phase.START) {
                // runs before vanilla reads hotbar keys, so combat-mode digits never switch the held item
                if (mc.player != null && mc.screen == null && CombatMode.active()) {
                    for (int i = 0; i < 9; i++) while (mc.options.keyHotbarSlots[i].consumeClick()) cast(i);
                }
                return;
            }
            ClientShinobi.clientTicks++;
            LocalPlayer p = mc.player;
            if (p != last) { last = p; ClientShinobi.reset(); chargeSent = false; if (p != null) ClientShinobi.send(Op.REQUEST_SYNC); }
            if (p == null) return;
            ClientShinobi.BANNERS.removeIf(b -> ClientShinobi.clientTicks - b.born() > 100);
            if (mc.screen != null) return;
            while (ShinobiKeys.MENU.consumeClick()) mc.setScreen(new ShinobiScreen());
            while (ShinobiKeys.STYLE.consumeClick()) ClientShinobi.send(Op.SWITCH_STYLE);
            while (ShinobiKeys.MODE.consumeClick()) ClientShinobi.send(Op.TOGGLE_MODE);
            while (ShinobiKeys.CLOAK.consumeClick()) ClientShinobi.send(Op.TOGGLE_CLOAK);
            while (ShinobiKeys.AWAKEN.consumeClick()) ClientShinobi.send(Op.AWAKEN);
            while (ShinobiKeys.GUARDIAN.consumeClick()) ClientShinobi.send(Op.GUARDIAN_TOGGLE);
            while (ShinobiKeys.STANCE.consumeClick()) ClientShinobi.send(Op.AVATAR_STANCE);
            while (ShinobiKeys.TACKLE.consumeClick()) ClientShinobi.send(Op.AVATAR_TACKLE);
            while (ShinobiKeys.BEAST_MODE.consumeClick()) ClientShinobi.send(Op.BEAST_MODE);
            while (ShinobiKeys.SKILL1.consumeClick()) ClientShinobi.send(Op.MODE_SKILL, "", 0, 0);
            while (ShinobiKeys.SKILL2.consumeClick()) ClientShinobi.send(Op.MODE_SKILL, "", 1, 0);
            // holding attack keeps punching while an avatar is up
            if (mc.options.keyAttack.isDown() && ClientShinobi.clientTicks % 3 == 0 && hasAvatar(p)) ClientShinobi.send(p.isSprinting() ? Op.AVATAR_TACKLE : Op.AVATAR_PUNCH);
            while (ShinobiKeys.CAST_SELECTED.consumeClick()) cast(ClientShinobi.selectedSlot);
            while (ShinobiKeys.WEAPON.consumeClick()) cast(ShinobiData.SLOT_WEAPON);
            while (ShinobiKeys.SUPPORT.consumeClick()) cast(ShinobiData.SLOT_SUPPORT);
            while (ShinobiKeys.SHINOBI_ULT.consumeClick()) cast(ShinobiData.SLOT_ULTIMATE);
            // 10th slot on the 0 key is not a vanilla hotbar key; use raw key state with edge detection
            boolean held = ShinobiKeys.CHARGE.isDown();
            if (held != chargeSent) { chargeSent = held; ClientShinobi.send(held ? Op.CHARGE_ON : Op.CHARGE_OFF); }
        }

        static boolean hasAvatar(LocalPlayer p) {
            return !p.level().getEntitiesOfClass(com.mayhem.shinobi.avatar.ChakraAvatarEntity.class, p.getBoundingBox().inflate(8),
                    a -> p.getUUID().equals(a.ownerId())).isEmpty();
        }

        /** While an avatar is up, attack = punch (sprint + attack = tackle) instead of vanilla mining/swinging. */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
            Minecraft mc = Minecraft.getInstance();
            if (!e.isAttack() || mc.player == null || mc.screen != null || !hasAvatar(mc.player)) return;
            e.setCanceled(true);
            e.setSwingHand(false);
            ClientShinobi.send(mc.player.isSprinting() ? Op.AVATAR_TACKLE : Op.AVATAR_PUNCH);
        }

        private static void cast(int slot) {
            if (slot < 0 || slot >= ShinobiData.ALL_SLOTS) return;
            int lock = com.mayhem.client.LockOn.id();
            ClientShinobi.send(Op.CAST, "", slot, lock);
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void scroll(InputEvent.MouseScrollingEvent e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null || !CombatMode.active()) return;
            int dir = e.getScrollDelta() > 0 ? -1 : 1;
            ClientShinobi.selectedSlot = Math.floorMod(ClientShinobi.selectedSlot + dir, ShinobiData.SLOTS);
            e.setCanceled(true);
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { ClientShinobi.reset(); }
    }
}
