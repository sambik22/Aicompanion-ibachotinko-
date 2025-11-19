package com.example.aicompanion.client;

import com.example.aicompanion.AicompanionMod;
import com.example.aicompanion.morph.MorphData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = AicompanionMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class MorphRenderClient {

    private static final Map<UUID, Entity> MORPH_ENTITIES = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        UUID id = player.getUUID();

        ResourceLocation morphId = MorphData.getMorphClient(id);
        if (morphId == null) return;

        event.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        Entity entity = MORPH_ENTITIES.get(id);
        if (entity == null) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(morphId);
            if (type == null) return;
            entity = type.create(player.level());
            if (entity == null) return;
            MORPH_ENTITIES.put(id, entity);
        }

        entity.setPos(player.getX(), player.getY(), player.getZ());
        entity.setYRot(player.getYRot());
        entity.setXRot(player.getXRot());

        var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        double x = entity.getX() - cameraPos.x;
        double y = entity.getY() - cameraPos.y;
        double z = entity.getZ() - cameraPos.z;

        mc.getEntityRenderDispatcher().render(
                entity,
                x, y, z,
                entity.getYRot(),
                event.getPartialTick(),
                poseStack,
                buffer,
                mc.getEntityRenderDispatcher().getPackedLightCoords(entity, event.getPartialTick())
        );
    }
}