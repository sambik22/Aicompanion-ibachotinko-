package com.example.aicompanion.morph;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorphData {

    private static final Map<UUID, ResourceLocation> SERVER_MORPHS = new HashMap<>();
    private static final Map<UUID, ResourceLocation> CLIENT_MORPHS = new HashMap<>();

    // ===== СЕРВЕР =====
    public static void setMorphServer(UUID uuid, ResourceLocation entityId) {
        SERVER_MORPHS.put(uuid, entityId);
    }

    public static void clearMorphServer(UUID uuid) {
        SERVER_MORPHS.remove(uuid);
    }

    public static ResourceLocation getMorphServer(UUID uuid) {
        return SERVER_MORPHS.get(uuid);
    }

    public static boolean isMorphedServer(UUID uuid) {
        return SERVER_MORPHS.containsKey(uuid);
    }

    public static Map<UUID, ResourceLocation> getAllServer() {
        return Map.copyOf(SERVER_MORPHS);
    }

    // ===== КЛІЄНТ =====
    public static void setMorphClient(UUID uuid, ResourceLocation entityId) {
        if (entityId == null) {
            CLIENT_MORPHS.remove(uuid);
        } else {
            CLIENT_MORPHS.put(uuid, entityId);
        }
    }

    public static ResourceLocation getMorphClient(UUID uuid) {
        return CLIENT_MORPHS.get(uuid);
    }

    public static boolean isMorphedClient(UUID uuid) {
        return CLIENT_MORPHS.containsKey(uuid);
    }

    public static void clearAllClient() {
        CLIENT_MORPHS.clear();
    }
}
