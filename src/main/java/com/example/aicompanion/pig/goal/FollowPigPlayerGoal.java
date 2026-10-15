package com.example.aicompanion.pig.goal;

import com.example.aicompanion.morph.MorphData;
import com.example.aicompanion.pig.PigBrain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Зв'язок з морфом: дикі свині приймають гравця, перетвореного на свиню
 * (/morphplayer <гравець> minecraft:pig), за ватажка стада й ходять за ним.
 */
public class FollowPigPlayerGoal extends Goal {

    private static final ResourceLocation PIG_ID = ResourceLocation.withDefaultNamespace("pig");
    private static final double NOTICE_DIST = 12.0;
    private static final double KEEP_DIST = 3.0;

    private final Pig pig;
    private final double speed;
    private Player leader;
    private int searchCooldown;
    private int repathTimer;

    public FollowPigPlayerGoal(Pig pig, double speed) {
        this.pig = pig;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (PigBrain.isTamed(pig) || pig.isBaby()) return false;
        if (--searchCooldown > 0) return false;
        searchCooldown = adjustedTickDelay(20);

        Player nearest = pig.level().getNearestPlayer(pig, NOTICE_DIST);
        if (nearest == null || !isPigMorph(nearest)) return false;
        if (pig.distanceToSqr(nearest) < KEEP_DIST * KEEP_DIST) return false;
        leader = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (leader == null || !leader.isAlive() || !isPigMorph(leader)) return false;
        double dist = pig.distanceToSqr(leader);
        return dist > KEEP_DIST * KEEP_DIST && dist < (NOTICE_DIST + 4) * (NOTICE_DIST + 4);
    }

    @Override
    public void start() {
        repathTimer = 0;
    }

    @Override
    public void stop() {
        leader = null;
        pig.getNavigation().stop();
    }

    @Override
    public void tick() {
        pig.getLookControl().setLookAt(leader, 10.0F, pig.getMaxHeadXRot());
        if (--repathTimer <= 0) {
            repathTimer = adjustedTickDelay(10);
            pig.getNavigation().moveTo(leader, speed);
        }
    }

    private static boolean isPigMorph(Player player) {
        return PIG_ID.equals(MorphData.getMorphServer(player.getUUID()));
    }
}
