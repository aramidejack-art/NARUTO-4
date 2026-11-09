package com.mayhem.network;

import com.mayhem.combat.UnarmedCombat;
import com.mayhem.progression.PlayerProgression;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request to print the player's progression status in chat. Rate-limited server side. */
public class StatusRequestPacket {
    public StatusRequestPacket() {}

    public static void encode(StatusRequestPacket packet, FriendlyByteBuf buf) {}

    public static StatusRequestPacket decode(FriendlyByteBuf buf) {
        return new StatusRequestPacket();
    }

    public static void handle(StatusRequestPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null && UnarmedCombat.allowStatusRequest(sender)) {
            PlayerProgression.sendStatus(sender);
            PlayerProgression.sync(sender);
        }
        context.setPacketHandled(true);
    }
}
