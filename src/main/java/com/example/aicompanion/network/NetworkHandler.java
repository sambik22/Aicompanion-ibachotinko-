package com.example.aicompanion.network;

import com.example.aicompanion.AicompanionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel.VersionTest;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class NetworkHandler {

    public static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(AicompanionMod.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel()
                .play()
                    .clientbound()
                        .addMain(MorphSyncPacket.class, MorphSyncPacket.STREAM_CODEC, MorphSyncPacket::handle)
            .build();

    /** Пакети реєструються в статичному ініціалізаторі; виклик лише гарантує завантаження класу. */
    public static void register() {
    }

    public static void sendToAll(MorphSyncPacket packet) {
        INSTANCE.send(packet, PacketDistributor.ALL.noArg());
    }

    public static void sendTo(ServerPlayer player, MorphSyncPacket packet) {
        INSTANCE.send(packet, PacketDistributor.PLAYER.with(player));
    }
}
