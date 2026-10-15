package com.example.aicompanion;

import com.example.aicompanion.client.ClientSetup;
import com.example.aicompanion.morph.MorphCommands;
import com.example.aicompanion.morph.MorphEvents;
import com.example.aicompanion.network.NetworkHandler;
import com.example.aicompanion.pig.PigCommands;
import com.example.aicompanion.pig.PigEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(AicompanionMod.MOD_ID)
public final class AicompanionMod {
    public static final String MOD_ID = "aicompanion";

    public AicompanionMod(FMLJavaModLoadingContext context) {
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(this::commonSetup);

        RegisterCommandsEvent.BUS.addListener(AicompanionMod::onRegisterCommands);
        MorphEvents.register();
        PigEvents.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.register();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        MorphCommands.register(event.getDispatcher(), event.getBuildContext());
        PigCommands.register(event.getDispatcher());
    }
}
