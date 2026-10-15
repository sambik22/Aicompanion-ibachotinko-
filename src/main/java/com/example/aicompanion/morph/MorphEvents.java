package com.example.aicompanion.morph;

import com.example.aicompanion.network.MorphSyncPacket;
import com.example.aicompanion.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Тримає морфи синхронізованими: гравець, що щойно зайшов, бачить уже існуючі морфи,
 * а морф гравця, що вийшов, скидається для всіх.
 */
public final class MorphEvents {

    private MorphEvents() {}

    public static void register() {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(MorphEvents::onLogin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(MorphEvents::onLogout);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (Map.Entry<UUID, ResourceLocation> e : MorphData.getAllServer().entrySet()) {
            NetworkHandler.sendTo(player, new MorphSyncPacket(e.getKey(), e.getValue()));
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (MorphData.isMorphedServer(id)) {
            MorphData.clearMorphServer(id);
            NetworkHandler.sendToAll(new MorphSyncPacket(id, null));
        }
    }
}
