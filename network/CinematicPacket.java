package com.mayhem.network;

import com.mayhem.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: start / advance / end a cinematic camera sequence. */
public class CinematicPacket {
    public static final byte START = 0;
    public static final byte BEAT = 1;
    public static final byte END = 2;

    // cinematic types
    public static final byte BARRAGE = 0;
    public static final byte METEOR = 1;
    public static final byte COUNTER = 2;
    public static final byte FINISHER = 3;

    // beats (shots) inside a cinematic
    public static final byte OPEN = 0;
    public static final byte ORBIT = 1;
    public static final byte LAUNCH = 2;
    public static final byte SKY = 3;
    public static final byte DIVE = 4;
    public static final byte IMPACT = 5;
    public static final byte CHARGE = 7;
    public static final byte ASCEND = 8;
    public static final byte HANG = 9;

    public final byte kind, type, beat;
    public final int attackerId, targetId;

    public CinematicPacket(byte kind, byte type, int attackerId, int targetId, byte beat) {
        this.kind = kind;
        this.type = type;
        this.attackerId = attackerId;
        this.targetId = targetId;
        this.beat = beat;
    }

    public static void encode(CinematicPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.kind);
        buf.writeByte(p.type);
        buf.writeVarInt(p.attackerId);
        buf.writeVarInt(p.targetId + 1);
        buf.writeByte(p.beat);
    }

    public static CinematicPacket decode(FriendlyByteBuf buf) {
        return new CinematicPacket(buf.readByte(), buf.readByte(), buf.readVarInt(),
                buf.readVarInt() - 1, buf.readByte());
    }

    public static void handle(CinematicPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.cinematic(p));
        context.setPacketHandled(true);
    }
}
