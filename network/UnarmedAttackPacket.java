package com.mayhem.network;

import com.mayhem.combat.UnarmedCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request to strike. Carries only the (unverified) lock-on target id; the server decides everything. */
public class UnarmedAttackPacket {
    private final int lockId;

    public UnarmedAttackPacket(int lockId) {
        this.lockId = lockId;
    }

    public static void encode(UnarmedAttackPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.lockId + 1); // -1 (none) -> 0
    }

    public static UnarmedAttackPacket decode(FriendlyByteBuf buf) {
        return new UnarmedAttackPacket(buf.readVarInt() - 1);
    }

    public static void handle(UnarmedAttackPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            UnarmedCombat.tryAttack(sender, p.lockId);
        }
        context.setPacketHandled(true);
    }
}
