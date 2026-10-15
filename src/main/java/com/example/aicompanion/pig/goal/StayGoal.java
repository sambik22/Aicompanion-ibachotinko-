package com.example.aicompanion.pig.goal;

import com.example.aicompanion.pig.PigBrain;
import com.example.aicompanion.pig.PigMode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;

import java.util.EnumSet;

/** Режим STAY: свиня стоїть на місці, але може крутити головою. */
public class StayGoal extends Goal {

    private final Pig pig;

    public StayGoal(Pig pig) {
        this.pig = pig;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return PigBrain.isInMode(pig, PigMode.STAY) && !pig.isInWater();
    }

    @Override
    public void start() {
        pig.getNavigation().stop();
    }
}
