package com.example.aicompanion.pig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * "Пам'ять" свині.
 *
 * Власник і режим зберігаються в persistent data сутності (Forge сам пише їх у сейв світу),
 * тому приручена свиня пам'ятає господаря після перезаходу.
 * Тривога стада живе лише в пам'яті сервера — вона й так триває кілька секунд.
 */
public final class PigBrain {

    private static final String OWNER_KEY = "aicompanion_owner";
    private static final String MODE_KEY = "aicompanion_mode";

    /** Скільки тіків свиня пам'ятає, від кого тікати (100 тіків = 5 секунд). */
    private static final int ALERT_TICKS = 100;

    private static final Map<Pig, Alert> ALERTS = new WeakHashMap<>();

    private record Alert(Entity threat, long untilGameTime) {}

    private PigBrain() {}

    // ===== Власник =====

    public static UUID getOwner(Pig pig) {
        String raw = pig.getPersistentData().getStringOr(OWNER_KEY, "");
        if (raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isTamed(Pig pig) {
        return getOwner(pig) != null;
    }

    public static boolean isOwnedBy(Pig pig, Player player) {
        return player.getUUID().equals(getOwner(pig));
    }

    /** Власник, якщо він онлайн і в тому ж вимірі, що й свиня. */
    public static Player getOwnerPlayer(Pig pig) {
        UUID owner = getOwner(pig);
        return owner == null ? null : pig.level().getPlayerByUUID(owner);
    }

    public static void tame(Pig pig, Player owner) {
        CompoundTag data = pig.getPersistentData();
        data.putString(OWNER_KEY, owner.getUUID().toString());
        data.putString(MODE_KEY, PigMode.FOLLOW.id());
        pig.setPersistenceRequired();
        if (!pig.hasCustomName()) {
            pig.setCustomName(Component.literal("Свинка " + owner.getName().getString()));
        }
    }

    public static void release(Pig pig) {
        CompoundTag data = pig.getPersistentData();
        data.remove(OWNER_KEY);
        data.remove(MODE_KEY);
    }

    // ===== Режим =====

    public static PigMode getMode(Pig pig) {
        PigMode mode = PigMode.byId(pig.getPersistentData().getStringOr(MODE_KEY, ""));
        return mode == null ? PigMode.FOLLOW : mode;
    }

    public static void setMode(Pig pig, PigMode mode) {
        pig.getPersistentData().putString(MODE_KEY, mode.id());
        pig.getNavigation().stop();
    }

    public static boolean isInMode(Pig pig, PigMode mode) {
        return isTamed(pig) && getMode(pig) == mode;
    }

    // ===== Тривога стада =====

    public static void alert(Pig pig, Entity threat) {
        ALERTS.put(pig, new Alert(threat, pig.level().getGameTime() + ALERT_TICKS));
    }

    public static Entity getThreat(Pig pig) {
        Alert alert = ALERTS.get(pig);
        if (alert == null) return null;
        if (pig.level().getGameTime() > alert.untilGameTime() || !alert.threat().isAlive()) {
            ALERTS.remove(pig);
            return null;
        }
        return alert.threat();
    }
}
