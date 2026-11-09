package com.mayhem.network;

import com.mayhem.MayhemMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class MayhemNetwork {
    private static final String PROTOCOL = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MayhemMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int packetId = 0;

    private MayhemNetwork() {}

    public static void register() {
        // Client -> server
        CHANNEL.messageBuilder(UnarmedAttackPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnarmedAttackPacket::encode).decoder(UnarmedAttackPacket::decode)
                .consumerMainThread(UnarmedAttackPacket::handle).add();
        CHANNEL.messageBuilder(StatusRequestPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StatusRequestPacket::encode).decoder(StatusRequestPacket::decode)
                .consumerMainThread(StatusRequestPacket::handle).add();
        CHANNEL.messageBuilder(ActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActionPacket::encode).decoder(ActionPacket::decode)
                .consumerMainThread(ActionPacket::handle).add();
        // Server -> client
        CHANNEL.messageBuilder(ProgressionSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProgressionSyncPacket::encode).decoder(ProgressionSyncPacket::decode)
                .consumerMainThread(ProgressionSyncPacket::handle).add();
        CHANNEL.messageBuilder(MomentumPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MomentumPacket::encode).decoder(MomentumPacket::decode)
                .consumerMainThread(MomentumPacket::handle).add();
        CHANNEL.messageBuilder(MoveAnimPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MoveAnimPacket::encode).decoder(MoveAnimPacket::decode)
                .consumerMainThread(MoveAnimPacket::handle).add();
        CHANNEL.messageBuilder(ImpactPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ImpactPacket::encode).decoder(ImpactPacket::decode)
                .consumerMainThread(ImpactPacket::handle).add();
        CHANNEL.messageBuilder(CinematicPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CinematicPacket::encode).decoder(CinematicPacket::decode)
                .consumerMainThread(CinematicPacket::handle).add();
        com.mayhem.shinobi.net.ShinobiNet.register(CHANNEL);
    }
}
