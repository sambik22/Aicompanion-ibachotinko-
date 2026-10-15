package com.example.aicompanion.pig.goal;

import com.example.aicompanion.pig.PigBrain;
import com.example.aicompanion.pig.PigMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Режим FOLLOW: свиня йде за власником. Якщо відстала надто далеко (застрягла, власник
 * побіг/полетів) — телепортується на безпечний блок поруч, як приручений вовк.
 */
public class FollowOwnerGoal extends Goal {

    private static final double START_DIST = 6.0;
    private static final double STOP_DIST = 2.5;
    private static final double TELEPORT_DIST = 16.0;

    private final Pig pig;
    private final double speed;
    private Player owner;
    private int repathTimer;

    public FollowOwnerGoal(Pig pig, double speed) {
        this.pig = pig;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!PigBrain.isInMode(pig, PigMode.FOLLOW) || pig.isPassenger() || pig.isLeashed()) return false;
        Player candidate = PigBrain.getOwnerPlayer(pig);
        if (candidate == null || candidate.isSpectator()) return false;
        if (pig.distanceToSqr(candidate) < START_DIST * START_DIST) return false;
        owner = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null
                && owner.isAlive()
                && PigBrain.isInMode(pig, PigMode.FOLLOW)
                && !pig.isLeashed()
                && pig.distanceToSqr(owner) > STOP_DIST * STOP_DIST;
    }

    @Override
    public void start() {
        repathTimer = 0;
    }

    @Override
    public void stop() {
        owner = null;
        pig.getNavigation().stop();
    }

    @Override
    public void tick() {
        pig.getLookControl().setLookAt(owner, 10.0F, pig.getMaxHeadXRot());
        if (--repathTimer > 0) return;
        repathTimer = adjustedTickDelay(10);

        if (pig.distanceToSqr(owner) >= TELEPORT_DIST * TELEPORT_DIST) {
            tryTeleportNear(pig, owner);
        } else {
            pig.getNavigation().moveTo(owner, speed);
        }
    }

    /** Шукає вільний блок з твердою підлогою в радіусі 2–3 блоків від власника. */
    static boolean tryTeleportNear(Pig pig, Player owner) {
        BlockPos base = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = pig.getRandom().nextInt(7) - 3;
            int dz = pig.getRandom().nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue; // не прямо під ноги
            int dy = pig.getRandom().nextInt(3) - 1;
            BlockPos target = base.offset(dx, dy, dz);
            if (canStandAt(pig, target)) {
                pig.snapTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, pig.getYRot(), pig.getXRot());
                pig.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    private static boolean canStandAt(Pig pig, BlockPos pos) {
        Level level = pig.level();
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        if (!level.getFluidState(pos).isEmpty()) return false;
        Vec3 offset = Vec3.atBottomCenterOf(pos).subtract(pig.position());
        return level.noCollision(pig, pig.getBoundingBox().move(offset));
    }
}
