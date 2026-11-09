package com.mayhem.network;

import com.mayhem.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client impact feedback: camera trauma / FOV punch (attenuated by distance on the client),
 * plus optional screen flash, comic text pop and speed lines.
 */
public class ImpactPacket {
    public static final byte FLASH_NONE = 0;
    public static final byte FLASH_WHITE = 1;
    public static final byte FLASH_DARK = 2;
    public static final byte FLASH_RED = 3;

    public final double x, y, z;
    public final float trauma, fov, radius;
    public final byte flash, text, lines;

    public ImpactPacket(double x, double y, double z, float trauma, float fov, float radius,
                        byte flash, byte text, byte lines) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.trauma = trauma;
        this.fov = fov;
        this.radius = radius;
        this.flash = flash;
        this.text = text;
        this.lines = lines;
    }

    public static void encode(ImpactPacket p, FriendlyByteBuf buf) {
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeFloat(p.trauma);
        buf.writeFloat(p.fov);
        buf.writeFloat(p.radius);
        buf.writeByte(p.flash);
        buf.writeByte(p.text);
        buf.writeByte(p.lines);
    }

    public static ImpactPacket decode(FriendlyByteBuf buf) {
        return new ImpactPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readByte(), buf.readByte(), buf.readByte());
    }

    public static void handle(ImpactPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.impact(p));
        context.setPacketHandled(true);
    }
}
