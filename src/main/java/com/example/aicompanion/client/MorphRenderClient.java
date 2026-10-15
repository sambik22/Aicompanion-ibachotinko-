package com.example.aicompanion.client;

import com.example.aicompanion.morph.MorphData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderAvatarEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Малює замість гравця моба, в якого він "перетворений".
 *
 * З 1.21.9 рендер працює через render state: у подію приходить лише знімок стану гравця,
 * а не сам гравець, тому гравця шукаємо за позицією зі стану.
 */
public final class MorphRenderClient {

    private static final Map<UUID, Entity> MORPH_ENTITIES = new HashMap<>();

    private MorphRenderClient() {}

    /** @return true — скасувати стандартний рендер гравця */
    static boolean onRenderAvatar(RenderAvatarEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Player player = findPlayer(mc, event.getState(), partialTick);
        if (player == null) return false;

        ResourceLocation morphId = MorphData.getMorphClient(player.getUUID());
        if (morphId == null) {
            MORPH_ENTITIES.remove(player.getUUID());
            return false;
        }

        Entity entity = getOrCreateMorph(mc, player.getUUID(), morphId);
        if (entity == null) return false;

        copyPose(player, entity, partialTick);
        submit(mc.getEntityRenderDispatcher().getRenderer(entity), entity, partialTick, event);
        return true;
    }

    static void clear() {
        MORPH_ENTITIES.clear();
    }

    private static Player findPlayer(Minecraft mc, AvatarRenderState state, float partialTick) {
        Player best = null;
        double bestDist = 1.0; // не далі ніж 1 блок від точки рендеру
        for (AbstractClientPlayer p : mc.level.players()) {
            double d = p.getPosition(partialTick).distanceToSqr(state.x, state.y, state.z);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private static Entity getOrCreateMorph(Minecraft mc, UUID playerId, ResourceLocation morphId) {
        Entity entity = MORPH_ENTITIES.get(playerId);
        if (entity != null
                && entity.level() == mc.level
                && EntityType.getKey(entity.getType()).equals(morphId)) {
            return entity;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(morphId).orElse(null);
        if (type == null) return null;
        entity = type.create(mc.level, EntitySpawnReason.LOAD);
        if (entity == null) return null;
        MORPH_ENTITIES.put(playerId, entity);
        return entity;
    }

    private static void copyPose(Player player, Entity entity, float partialTick) {
        Vec3 pos = player.getPosition(partialTick);
        entity.snapTo(pos.x, pos.y, pos.z, player.getYRot(partialTick), player.getViewXRot(partialTick));
        entity.tickCount = player.tickCount;
        entity.setInvisible(player.isInvisible());

        if (entity instanceof LivingEntity living) {
            float body = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            float head = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
            living.yBodyRot = living.yBodyRotO = body;
            living.yHeadRot = living.yHeadRotO = head;
        }
    }

    private static <E extends Entity, S extends EntityRenderState> void submit(
            EntityRenderer<? super E, S> renderer, E entity, float partialTick, RenderAvatarEvent.Pre event) {
        S state = renderer.createRenderState(entity, partialTick);
        renderer.submit(state, event.getPoseStack(), event.getNodeCollector(), event.getCameraState());
    }
}
