package com.example.aicompanion.pig;

import com.example.aicompanion.pig.goal.FollowOwnerGoal;
import com.example.aicompanion.pig.goal.FollowPigPlayerGoal;
import com.example.aicompanion.pig.goal.ForageGoal;
import com.example.aicompanion.pig.goal.GuardOwnerGoal;
import com.example.aicompanion.pig.goal.HerdAlertGoal;
import com.example.aicompanion.pig.goal.StayGoal;
import com.example.aicompanion.pig.talk.PigTalk;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Точка входу "мізків" свиней.
 *
 * Новий тип моба не реєструємо: кожна ванільна свиня при появі у світі отримує додаткові
 * AI-цілі (Goal). Так працюють і вже існуючі свині, і не потрібні власні моделі/текстури.
 *
 * Пріоритети цілей (менше число = важливіше; у ванільної свині 0..7):
 *   0 GuardOwnerGoal     — охорона сильніша за паніку
 *   1 HerdAlertGoal      — тікати разом зі стадом (поряд з ванільною PanicGoal)
 *   1 StayGoal           — стояти
 *   2 ForageGoal         — збирати їжу
 *   2 FollowOwnerGoal    — іти за власником (важливіше за ванільне розмноження і "морквинку")
 *   4 FollowPigPlayerGoal — дикі свині йдуть за гравцем-свинею
 */
public final class PigEvents {

    private static final Set<Pig> UPGRADED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final double HERD_RADIUS = 10.0;

    private PigEvents() {}

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(PigEvents::onJoinLevel);
        Predicate<PlayerInteractEvent.EntityInteract> interact = PigEvents::onInteract;
        PlayerInteractEvent.EntityInteract.BUS.addListener(interact);
        LivingHurtEvent.BUS.addListener(PigEvents::onHurt);
        PigTalk.register();
    }

    private static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Pig pig) || !(event.getLevel() instanceof ServerLevel)) return;
        if (!UPGRADED.add(pig)) return;

        pig.goalSelector.addGoal(0, new GuardOwnerGoal(pig, 1.4));
        pig.goalSelector.addGoal(1, new HerdAlertGoal(pig, 1.5));
        pig.goalSelector.addGoal(1, new StayGoal(pig));
        pig.goalSelector.addGoal(2, new ForageGoal(pig, 1.2));
        pig.goalSelector.addGoal(2, new FollowOwnerGoal(pig, 1.25));
        pig.goalSelector.addGoal(4, new FollowPigPlayerGoal(pig, 1.1));
    }

    /** ПКМ золотою морквою — приручити; Shift+ПКМ порожньою рукою — змінити режим. */
    private static boolean onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Pig pig) || event.getHand() != InteractionHand.MAIN_HAND) return false;

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(pig.level() instanceof ServerLevel level)) {
            // Клієнт не знає, чия це свиня, тому лише гасить клік золотою морквою
            // (інакше гравець почав би її їсти), а вирішує все сервер.
            if (stack.is(Items.GOLDEN_CARROT)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                return true;
            }
            return false;
        }

        if (!PigBrain.isTamed(pig) && stack.is(Items.GOLDEN_CARROT) && !pig.isBaby()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            PigBrain.tame(pig, player);
            level.sendParticles(ParticleTypes.HEART, pig.getX(), pig.getY() + 1.0, pig.getZ(),
                    7, 0.4, 0.4, 0.4, 0.0);
            notify(player, "Свиня тепер твоя! Режим: " + PigMode.FOLLOW.title()
                    + ". Shift+ПКМ порожньою рукою — змінити режим.");
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }

        if (PigBrain.isOwnedBy(pig, player) && player.isShiftKeyDown() && stack.isEmpty()) {
            PigMode mode = PigBrain.getMode(pig).next();
            PigBrain.setMode(pig, mode);
            level.sendParticles(ParticleTypes.NOTE, pig.getX(), pig.getY() + 1.2, pig.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            notify(player, "Режим свині: " + mode.title());
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }
        return false;
    }

    /** Коли б'ють свиню — тривога для всього стада поруч. */
    private static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Pig victim) || !(victim.level() instanceof ServerLevel level)) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker instanceof Pig) return;

        for (Pig pig : level.getEntitiesOfClass(Pig.class, victim.getBoundingBox().inflate(HERD_RADIUS))) {
            // господар може штовхнути свою свиню — його вона не боїться
            if (attacker instanceof Player p && PigBrain.isOwnedBy(pig, p)) continue;
            PigBrain.alert(pig, attacker);
        }
    }

    private static void notify(Player player, String text) {
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(text), true);
        }
    }
}
