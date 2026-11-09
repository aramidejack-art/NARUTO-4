package com.mayhem.network;

import com.mayhem.client.ClientProgressionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client snapshot of the player's progression, used by the HUD. */
public class ProgressionSyncPacket {
    private final int level;
    private final int xp;
    private final int xpToNext;
    private final int strength;
    private final int mastery;

    public ProgressionSyncPacket(int level, int xp, int xpToNext, int strength, int mastery) {
        this.level = level;
        this.xp = xp;
        this.xpToNext = xpToNext;
        this.strength = strength;
        this.mastery = mastery;
    }

    public static void encode(ProgressionSyncPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.level);
        buf.writeVarInt(p.xp);
        buf.writeVarInt(p.xpToNext);
        buf.writeVarInt(p.strength);
        buf.writeVarInt(p.mastery);
    }

    public static ProgressionSyncPacket decode(FriendlyByteBuf buf) {
        return new ProgressionSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ProgressionSyncPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        // ClientProgressionData is a plain holder with no client-only class references, so it is
        // safe to touch from a handler class that is also loaded on a dedicated server.
        ClientProgressionData.update(p.level, p.xp, p.xpToNext, p.strength, p.mastery);
        context.setPacketHandled(true);
    }
}
