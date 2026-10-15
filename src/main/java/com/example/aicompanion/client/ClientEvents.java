package com.example.aicompanion.client;

import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

public final class ClientEvents {

    private ClientEvents() {}

    /** Ховаємо список гравців (TAB), щоб морф не можна було "розкусити" через таб. */
    static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addConditionTo(
                ForgeLayeredDraw.POST_SLEEP_STACK,
                ForgeLayeredDraw.TAB_LIST,
                () -> false
        );
    }
}
