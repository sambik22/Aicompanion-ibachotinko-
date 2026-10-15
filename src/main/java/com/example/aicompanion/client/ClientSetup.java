package com.example.aicompanion.client;

import com.example.aicompanion.morph.MorphData;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderAvatarEvent;

import java.util.function.Predicate;

/**
 * Реєстрація клієнтських слухачів. Викликається лише на фізичному клієнті,
 * щоб на виділеному сервері не завантажувались клієнтські класи.
 */
public final class ClientSetup {

    private ClientSetup() {}

    public static void register() {
        Predicate<RenderAvatarEvent.Pre> morphRender = MorphRenderClient::onRenderAvatar;
        RenderAvatarEvent.Pre.BUS.addListener(morphRender);

        AddGuiOverlayLayersEvent.BUS.addListener(ClientEvents::onAddGuiLayers);

        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
            MorphData.clearAllClient();
            MorphRenderClient.clear();
        });
    }
}
