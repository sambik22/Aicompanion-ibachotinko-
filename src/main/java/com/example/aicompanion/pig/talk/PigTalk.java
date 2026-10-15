package com.example.aicompanion.pig.talk;

import com.example.aicompanion.pig.PigBrain;
import com.example.aicompanion.pig.PigMode;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Розмова зі свинями.
 *
 * Гравець пише в чат, починаючи зі звертання ("свинко, ...", "хрю ...", "pig, ...")
 * або командою /aipig say <текст>. Найближча свиня (своя — в пріоритеті) відповідає в чат
 * і за потреби виконує дію: іти за тобою, охороняти, шукати їжу, подружитися...
 *
 * Сам запит до Claude йде у фоні, а результат застосовується на головному потоці сервера.
 */
public final class PigTalk {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String[] PREFIXES = {"свин", "хрю", "pig", "@pig", "порося"};
    private static final double LISTEN_RADIUS = 12.0;
    private static final double HEAR_RADIUS = 32.0;
    private static final int HISTORY_TURNS = 8;
    private static final long PLAYER_COOLDOWN_MS = 2000;

    /** Пам'ять розмови: свиня → останні пари {гравець, свиня}. Живе до перезапуску сервера. */
    private static final Map<UUID, Deque<String[]>> HISTORY = new HashMap<>();
    private static final Set<UUID> THINKING = new HashSet<>();
    private static final Map<UUID, Long> LAST_TALK = new HashMap<>();

    private PigTalk() {}

    public static void register() {
        ServerChatEvent.BUS.addListener(PigTalk::onChat);
    }

    private static void onChat(ServerChatEvent event) {
        String text = event.getRawText().trim();
        String lower = text.toLowerCase(Locale.ROOT);
        boolean addressed = false;
        for (String prefix : PREFIXES) {
            if (lower.startsWith(prefix)) {
                addressed = true;
                break;
            }
        }
        if (!addressed) return;

        ServerPlayer player = event.getPlayer();
        // повідомлення гравця йде в чат як зазвичай; свиня відповідає вже на головному потоці
        ((ServerLevel) player.level()).getServer().execute(() -> talk(player, text));
    }

    /** @return false, якщо поруч немає свиней */
    public static boolean talk(ServerPlayer player, String message) {
        ServerLevel level = (ServerLevel) player.level();
        Pig pig = findListener(player, level);
        if (pig == null) return false;

        long now = System.currentTimeMillis();
        Long last = LAST_TALK.get(player.getUUID());
        if (last != null && now - last < PLAYER_COOLDOWN_MS) return true;
        LAST_TALK.put(player.getUUID(), now);

        if (!THINKING.add(pig.getUUID())) {
            player.sendSystemMessage(Component.literal(PigPersonality.name(pig) + " ще думає над минулою відповіддю..."), true);
            return true;
        }

        boolean isOwner = PigBrain.isOwnedBy(pig, player);
        pig.getLookControl().setLookAt(player, 30.0F, 30.0F);
        level.sendParticles(ParticleTypes.NOTE, pig.getX(), pig.getY() + 1.2, pig.getZ(), 1, 0.0, 0.0, 0.0, 0.0);

        if (!AiConfig.hasApiKey()) {
            finish(pig, player, message,
                    OfflinePigBrain.decide(message, isOwner, PigPersonality.trait(pig), pig.getRandom()));
            return true;
        }

        List<String[]> history = new ArrayList<>(HISTORY.getOrDefault(pig.getUUID(), new ArrayDeque<>()));
        String context = describe(pig, player, level);
        ClaudePigBrain.ask(history, context, message).whenComplete((aiDecision, error) ->
                level.getServer().execute(() -> {
                    PigDecision decision = aiDecision;
                    if (error != null) {
                        LOGGER.warn("Свиня не змогла подумати через Claude API, відповідаю правилами", error);
                        decision = OfflinePigBrain.decide(message, PigBrain.isOwnedBy(pig, player),
                                PigPersonality.trait(pig), pig.getRandom());
                    }
                    finish(pig, player, message, decision);
                }));
        return true;
    }

    /** Головний потік: озвучити відповідь і виконати дію. */
    private static void finish(Pig pig, ServerPlayer player, String message, PigDecision decision) {
        THINKING.remove(pig.getUUID());
        if (!pig.isAlive() || pig.isRemoved() || !(pig.level() instanceof ServerLevel level)) return;

        remember(pig, message, decision.reply());
        say(pig, level, decision.reply());
        apply(pig, player, level, decision.action());
    }

    private static void apply(Pig pig, ServerPlayer player, ServerLevel level, PigAction action) {
        boolean isOwner = PigBrain.isOwnedBy(pig, player);
        switch (action) {
            case FOLLOW, STAY, GUARD, FORAGE, WANDER -> {
                if (isOwner) PigBrain.setMode(pig, PigMode.byId(action.id()));
            }
            case LEAVE -> {
                if (isOwner) PigBrain.release(pig);
            }
            case BEFRIEND -> {
                if (!PigBrain.isTamed(pig) && !pig.isBaby()) {
                    PigBrain.tame(pig, player);
                    level.sendParticles(ParticleTypes.HEART, pig.getX(), pig.getY() + 1.0, pig.getZ(),
                            7, 0.4, 0.4, 0.4, 0.0);
                }
            }
            case COME -> pig.getNavigation().moveTo(player, 1.3);
            case JUMP -> {
                if (pig.onGround()) pig.getJumpControl().jump();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pig.getX(), pig.getY() + 0.8, pig.getZ(),
                        6, 0.3, 0.3, 0.3, 0.0);
            }
            case NONE -> {
            }
        }
    }

    private static void say(Pig pig, ServerLevel level, String text) {
        Component line = Component.literal("<" + PigPersonality.name(pig) + "> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(pig) <= HEAR_RADIUS * HEAR_RADIUS) {
                p.sendSystemMessage(line);
            }
        }
    }

    private static void remember(Pig pig, String playerMessage, String reply) {
        if (HISTORY.size() > 500) HISTORY.clear(); // проста межа пам'яті
        Deque<String[]> turns = HISTORY.computeIfAbsent(pig.getUUID(), k -> new ArrayDeque<>());
        turns.addLast(new String[]{playerMessage, reply});
        while (turns.size() > HISTORY_TURNS) turns.removeFirst();
    }

    /** Найближча свиня поруч; своя свиня важливіша за чужу. */
    private static Pig findListener(Player player, ServerLevel level) {
        List<Pig> pigs = level.getEntitiesOfClass(Pig.class, player.getBoundingBox().inflate(LISTEN_RADIUS));
        return pigs.stream()
                .min(Comparator.<Pig>comparingInt(p -> PigBrain.isOwnedBy(p, player) ? 0 : 1)
                        .thenComparingDouble(p -> p.distanceToSqr(player)))
                .orElse(null);
    }

    /** Що свиня "знає" про себе і світ — передається моделі як дані. */
    private static String describe(Pig pig, ServerPlayer speaker, ServerLevel level) {
        Player owner = PigBrain.getOwnerPlayer(pig);
        PigPersonality.Trait trait = PigPersonality.trait(pig);

        JsonObject ctx = new JsonObject();
        ctx.addProperty("pig_name", PigPersonality.name(pig));
        ctx.addProperty("personality", trait.description);
        ctx.addProperty("speaker", speaker.getName().getString());
        ctx.addProperty("speaker_is_owner", PigBrain.isOwnedBy(pig, speaker));
        ctx.addProperty("has_owner", PigBrain.isTamed(pig));
        if (owner != null) ctx.addProperty("owner_name", owner.getName().getString());
        if (PigBrain.isTamed(pig)) ctx.addProperty("current_mode", PigBrain.getMode(pig).id());
        ctx.addProperty("health", Math.round(pig.getHealth()) + "/" + Math.round(pig.getMaxHealth()));
        ctx.addProperty("is_baby", pig.isBaby());
        ctx.addProperty("speaker_holds", speaker.getMainHandItem().isEmpty()
                ? "nothing" : speaker.getMainHandItem().getHoverName().getString());
        ctx.addProperty("raining", level.isRaining());
        ctx.addProperty("monsters_nearby",
                level.getEntitiesOfClass(Monster.class, pig.getBoundingBox().inflate(16.0)).size());
        ctx.addProperty("pigs_nearby",
                level.getEntitiesOfClass(Pig.class, pig.getBoundingBox().inflate(16.0)).size() - 1);
        ctx.addProperty("distance_to_speaker_blocks", Math.round(pig.distanceTo(speaker)));
        return ctx.toString();
    }
}
