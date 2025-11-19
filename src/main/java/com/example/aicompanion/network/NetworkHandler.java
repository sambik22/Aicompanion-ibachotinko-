package com.example.aicompanion.network;

import com.example.aicompanion.AicompanionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.SimpleChannel;

public class NetworkHandler {

    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(AicompanionMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(
                packetId++,
                MorphSyncPacket.class,
                MorphSyncPacket::encode,
                MorphSyncPacket::decode,
                MorphSyncPacket::handle
        );
    }
}