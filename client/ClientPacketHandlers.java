package com.mayhem.client;

import com.mayhem.combat.MoveType;
import com.mayhem.network.CinematicPacket;
import com.mayhem.network.ImpactPacket;

/** Client-side entry points for server packets. Only invoked via DistExecutor on the client. */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void playAnimation(int entityId, MoveType move, int variant) {
        ClientAnimations.play(entityId, move, variant);
    }

    public static void impact(ImpactPacket packet) {
        CameraEffects.impact(packet);
    }

    public static void cinematic(CinematicPacket packet) {
        CinematicDirector.handle(packet);
    }
}
