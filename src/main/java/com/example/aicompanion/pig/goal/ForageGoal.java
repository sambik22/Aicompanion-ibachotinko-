package com.example.aicompanion.pig.goal;

import com.example.aicompanion.pig.PigBrain;
import com.example.aicompanion.pig.PigMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

/**
 * Режим FORAGE: свиня-"трюфелешукач". Шукає на землі овочі, яблука й гриби,
 * підбирає їх "у рот", несе власнику і кладе йому в інвентар.
 *
 * Це маленький скінченний автомат з двох станів:
 *   ПОШУК  — немає здобичі: йдемо до найближчого підходящого предмета;
 *   ДОСТАВКА — є здобич: йдемо до власника і віддаємо.
 */
public class ForageGoal extends Goal {

    private static final Set<Item> FORAGEABLE = Set.of(
            Items.CARROT, Items.POTATO, Items.BEETROOT, Items.WHEAT,
            Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SLICE,
            Items.APPLE, Items.SWEET_BERRIES, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM
    );
    private static final double SEARCH_RADIUS = 12.0;
    /** Предмети ближче за цю відстань до власника вважаються вже доставленими. */
    private static final double OWNER_SAFE_ZONE = 3.0;

    private final Pig pig;
    private final double speed;
    private ItemEntity targetItem;
    private ItemStack carried = ItemStack.EMPTY;
    private int searchCooldown;
    private int repathTimer;

    public ForageGoal(Pig pig, double speed) {
        this.pig = pig;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!PigBrain.isInMode(pig, PigMode.FORAGE)) return false;
        Player owner = PigBrain.getOwnerPlayer(pig);
        if (owner == null) return false;
        if (!carried.isEmpty()) return true;
        if (--searchCooldown > 0) return false;
        searchCooldown = adjustedTickDelay(20);
        targetItem = findItem(owner);
        return targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!PigBrain.isInMode(pig, PigMode.FORAGE) || PigBrain.getOwnerPlayer(pig) == null) return false;
        return !carried.isEmpty() || (targetItem != null && targetItem.isAlive());
    }

    @Override
    public void start() {
        repathTimer = 0;
    }

    @Override
    public void stop() {
        // режим змінили або свиню налякали — не "ковтаємо" здобич, а кидаємо на землю
        if (!carried.isEmpty() && pig.level() instanceof ServerLevel level) {
            pig.spawnAtLocation(level, carried, 0.5F);
        }
        carried = ItemStack.EMPTY;
        targetItem = null;
        pig.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player owner = PigBrain.getOwnerPlayer(pig);
        if (owner == null || !(pig.level() instanceof ServerLevel level)) return;

        if (carried.isEmpty()) {
            seekItem(level);
        } else {
            deliverTo(owner, level);
        }
    }

    private void seekItem(ServerLevel level) {
        if (targetItem == null || !targetItem.isAlive()) return;
        pig.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);

        if (pig.distanceToSqr(targetItem) < 2.0) {
            carried = targetItem.getItem().copy();
            targetItem.discard();
            targetItem = null;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pig.getX(), pig.getY() + 0.8, pig.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
            repathTimer = 0;
            return;
        }
        if (--repathTimer <= 0) {
            repathTimer = adjustedTickDelay(10);
            pig.getNavigation().moveTo(targetItem, speed);
        }
    }

    private void deliverTo(Player owner, ServerLevel level) {
        pig.getLookControl().setLookAt(owner, 30.0F, 30.0F);

        if (pig.distanceToSqr(owner) < 2.5 * 2.5) {
            owner.getInventory().add(carried); // зменшує stack на те, що влізло
            if (!carried.isEmpty()) {
                pig.spawnAtLocation(level, carried, 0.5F);
            }
            carried = ItemStack.EMPTY;
            level.sendParticles(ParticleTypes.HEART,
                    pig.getX(), pig.getY() + 1.0, pig.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
            return;
        }
        if (--repathTimer <= 0) {
            repathTimer = adjustedTickDelay(10);
            if (pig.distanceToSqr(owner) > 24 * 24) {
                FollowOwnerGoal.tryTeleportNear(pig, owner);
            } else {
                pig.getNavigation().moveTo(owner, speed);
            }
        }
    }

    private ItemEntity findItem(Player owner) {
        return pig.level().getEntitiesOfClass(ItemEntity.class,
                        pig.getBoundingBox().inflate(SEARCH_RADIUS, 4.0, SEARCH_RADIUS),
                        item -> item.isAlive()
                                && FORAGEABLE.contains(item.getItem().getItem())
                                && item.distanceToSqr(owner) > OWNER_SAFE_ZONE * OWNER_SAFE_ZONE)
                .stream()
                .min(Comparator.comparingDouble(pig::distanceToSqr))
                .orElse(null);
    }
}
