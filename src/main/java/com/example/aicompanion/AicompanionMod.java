package com.example.aicompanion;

import com.example.aicompanion.morph.MorphCommands;
import com.example.aicompanion.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AicompanionMod.MOD_ID)
public class AicompanionMod {
    public static final String MOD_ID = "aicompanion";

    public AicompanionMod() {
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MorphCommands.register(event.getDispatcher(), event.getBuildContext());
    }
}