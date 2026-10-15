package com.example.aicompanion.network;

import com.example.aicompanion.morph.MorphData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

/**
 * Сервер → клієнт: "гравець playerId тепер виглядає як morphId" (null = скинути морф).
 */
public record MorphSyncPacket(UUID playerId, ResourceLocation morphId) {

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(MorphSyncPacket::encode, MorphSyncPacket::decode);

    public static void encode(MorphSyncPacket msg, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.morphId != null);
        if (msg.morphId != null) {
            buf.writeResourceLocation(msg.morphId);
        }
    }

    public static MorphSyncPacket decode(RegistryFriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean hasMorph = buf.readBoolean();
        ResourceLocation morphId = hasMorph ? buf.readResourceLocation() : null;
        return new MorphSyncPacket(playerId, morphId);
    }

    // викликається вже на головному потоці клієнта (див. addMain у NetworkHandler)
    public static void handle(MorphSyncPacket msg, CustomPayloadEvent.Context ctx) {
        MorphData.setMorphClient(msg.playerId, msg.morphId);
    }
}
