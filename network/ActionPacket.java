package com.mayhem.network;

import com.mayhem.combat.SpecialMoves;
import com.mayhem.combat.Ultimates;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Generic client -> server action request (dodge, guard, slam, ultimate). */
public class ActionPacket {
    public enum Type { DODGE, GUARD_DOWN, GUARD_UP, SLAM, ULTIMATE }

    private final Type type;
    private final float a;
    private final float b;
    private final int lockId;

    public ActionPacket(Type type, float a, float b, int lockId) {
        this.type = type;
        this.a = a;
        this.b = b;
        this.lockId = lockId;
    }

    public static void encode(ActionPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.type);
        buf.writeFloat(p.a);
        buf.writeFloat(p.b);
        buf.writeVarInt(p.lockId + 1);
    }

    public static ActionPacket decode(FriendlyByteBuf buf) {
        return new ActionPacket(buf.readEnum(Type.class), buf.readFloat(), buf.readFloat(), buf.readVarInt() - 1);
    }

    private static float clean(float v) {
        return Float.isNaN(v) || Float.isInfinite(v) ? 0F : Math.max(-1F, Math.min(1F, v));
    }

    public static void handle(ActionPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            switch (p.type) {
                case DODGE -> SpecialMoves.dodge(sender, clean(p.a), clean(p.b), p.lockId);
                case GUARD_DOWN -> SpecialMoves.guard(sender, true);
                case GUARD_UP -> SpecialMoves.guard(sender, false);
                case SLAM -> SpecialMoves.slam(sender);
                case ULTIMATE -> Ultimates.start(sender, p.a > 0.5F, p.lockId);
            }
        }
        context.setPacketHandled(true);
    }
}
