package com.mayhem.shinobi.client;

import com.mayhem.MayhemMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client movement for the traversal abilities (runs before the player's own movement each tick). */
@Mod.EventBusSubscriber(modid = MayhemMod.MOD_ID, value = Dist.CLIENT)
public final class ClientTraversal {
    private ClientTraversal() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !e.side.isClient()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || e.player != p || p.isSpectator() || p.getAbilities().flying || p.isPassenger()) return;
        Vec3 v = p.getDeltaMovement();

        // WATER: if you fall in (or the film is not there yet) rise back to the surface instead of sinking.
        if (ClientShinobi.waterWalk() && !p.isShiftKeyDown() && p.isInWater() && v.y < 0.16) {
            p.setDeltaMovement(v.x, 0.16, v.z);
            p.fallDistance = 0;
            v = p.getDeltaMovement();
        }

        // WALL: hold forward into a wall to climb; sneak to cling; jump to let go.
        if (ClientShinobi.wallWalk() && p.horizontalCollision && !p.onGround() && !p.isInWater()) {
            boolean forward = p.input.forwardImpulse > 0 || p.input.leftImpulse != 0;
            if (p.input.jumping) return;                              // detach
            if (p.isShiftKeyDown()) p.setDeltaMovement(v.x, 0.0, v.z); // cling
            else if (forward) p.setDeltaMovement(v.x, 0.22, v.z);      // climb
            else p.setDeltaMovement(v.x, Math.max(v.y, -0.04), v.z);   // slide slowly
            p.fallDistance = 0;
        }
    }
}
