package com.mayhem.network;

import com.mayhem.client.ClientPacketHandlers;
import com.mayhem.combat.MoveType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: play a procedural animation on an entity. */
public class MoveAnimPacket {
    private final int entityId;
    private final byte move;
    private final byte variant;

    public MoveAnimPacket(int entityId, MoveType move, int variant) {
        this.entityId = entityId;
        this.move = (byte) move.ordinal();
        this.variant = (byte) variant;
    }

    private MoveAnimPacket(int entityId, byte move, byte variant) {
        this.entityId = entityId;
        this.move = move;
        this.variant = variant;
    }

    public static void encode(MoveAnimPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        buf.writeByte(p.move);
        buf.writeByte(p.variant);
    }

    public static MoveAnimPacket decode(FriendlyByteBuf buf) {
        return new MoveAnimPacket(buf.readVarInt(), buf.readByte(), buf.readByte());
    }

    public static void handle(MoveAnimPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientPacketHandlers.playAnimation(p.entityId, MoveType.byId(p.move), p.variant));
        context.setPacketHandled(true);
    }
}
