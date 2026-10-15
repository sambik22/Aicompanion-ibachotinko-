package com.example.aicompanion.pig.goal;

import com.example.aicompanion.pig.PigBrain;
import com.example.aicompanion.pig.PigMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Стадна поведінка: коли поруч б'ють свиню, тікає не лише вона (ванільна паніка),
 * а все стадо — геть від нападника. Тривогу виставляє PigEvents.
 * Свині-охоронці (GUARD) не тікають.
 */
public class HerdAlertGoal extends Goal {

    private final Pig pig;
    private final double speed;

    public HerdAlertGoal(Pig pig, double speed) {
        this.pig = pig;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (PigBrain.isInMode(pig, PigMode.GUARD)) return false;
        Entity threat = PigBrain.getThreat(pig);
        if (threat == null || pig.distanceToSqr(threat) > 16 * 16) return false;

        Vec3 away = DefaultRandomPos.getPosAway(pig, 16, 7, threat.position());
        if (away == null) return false;
        return pig.getNavigation().moveTo(away.x, away.y, away.z, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return !pig.getNavigation().isDone() && PigBrain.getThreat(pig) != null;
    }

    @Override
    public void stop() {
        pig.getNavigation().stop();
    }
}
