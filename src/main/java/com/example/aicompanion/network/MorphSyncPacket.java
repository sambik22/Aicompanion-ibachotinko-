package com.example.aicompanion.network;

import com.example.aicompanion.morph.MorphData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MorphSyncPacket {

    private final UUID playerId;
    private final ResourceLocation morphId; // null = скинути

    public MorphSyncPacket(UUID playerId, ResourceLocation morphId) {
        this.playerId = playerId;
        this.morphId = morphId;
    }

    public static void encode(MorphSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.morphId != null);
        if (msg.morphId != null) {
            buf.writeResourceLocation(msg.morphId);
        }
    }

    public static MorphSyncPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean hasMorph = buf.readBoolean();
        ResourceLocation morphId = hasMorph ? buf.readResourceLocation() : null;
        return new MorphSyncPacket(playerId, morphId);
    }

    public static void handle(MorphSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            MorphData.setMorphClient(msg.playerId, msg.morphId);
        });
        ctx.get().setPacketHandled(true);
    }
}