package com.mayhem.network;

import com.mayhem.client.ClientProgressionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: current momentum meter (0..100). */
public class MomentumPacket {
    private final int value;

    public MomentumPacket(int value) {
        this.value = value;
    }

    public static void encode(MomentumPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.value);
    }

    public static MomentumPacket decode(FriendlyByteBuf buf) {
        return new MomentumPacket(buf.readVarInt());
    }

    public static void handle(MomentumPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ClientProgressionData.setMomentum(p.value);
        context.setPacketHandled(true);
    }
}
