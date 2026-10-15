package com.example.aicompanion.pig.goal;

import com.example.aicompanion.pig.PigBrain;
import com.example.aicompanion.pig.PigMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Режим GUARD: свиня таранить того, хто напав на власника, на кого напав власник,
 * або хто вдарив саму свиню.
 *
 * У ванільної свині немає атрибута ATTACK_DAMAGE, тому стандартний MeleeAttackGoal
 * не підходить — удар робимо вручну: шкода + відкидання.
 */
public class GuardOwnerGoal extends Goal {

    /** Наскільки "свіжою" має бути бійка власника, щоб свиня втрутилась (тіки). */
    private static final int MEMORY_TICKS = 100;
    private static final double SEARCH_DIST = 20.0;
    private static final double GIVE_UP_DIST = 24.0;
    private static final float DAMAGE = 3.0F;
    private static final double KNOCKBACK = 0.6;

    private final Pig pig;
    private final double speed;
    private LivingEntity target;
    private int attackCooldown;
    private int repathTimer;

    public GuardOwnerGoal(Pig pig, double speed) {
        this.pig = pig;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!PigBrain.isInMode(pig, PigMode.GUARD)) return false;
        target = findTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null
                && isValidTarget(target, PigBrain.getOwnerPlayer(pig))
                && PigBrain.isInMode(pig, PigMode.GUARD)
                && pig.distanceToSqr(target) < GIVE_UP_DIST * GIVE_UP_DIST;
    }

    @Override
    public void start() {
        attackCooldown = 0;
        repathTimer = 0;
    }

    @Override
    public void stop() {
        target = null;
        pig.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        pig.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repathTimer <= 0) {
            repathTimer = 5;
            pig.getNavigation().moveTo(target, speed);
        }
        if (attackCooldown > 0) attackCooldown--;

        double reach = pig.getBbWidth() * 1.5 + target.getBbWidth() * 0.5;
        if (attackCooldown == 0 && pig.distanceToSqr(target) <= reach * reach + 1.0) {
            ram(target);
            attackCooldown = 20;
        }
    }

    private void ram(LivingEntity victim) {
        if (!(pig.level() instanceof ServerLevel level)) return;
        victim.hurtServer(level, pig.damageSources().mobAttack(pig), DAMAGE);
        victim.knockback(KNOCKBACK, pig.getX() - victim.getX(), pig.getZ() - victim.getZ());
        level.sendParticles(ParticleTypes.CRIT,
                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                8, 0.3, 0.3, 0.3, 0.1);
    }

    private LivingEntity findTarget() {
        Player owner = PigBrain.getOwnerPlayer(pig);
        if (owner != null) {
            LivingEntity attacker = owner.getLastHurtByMob();
            if (attacker != null
                    && owner.tickCount - owner.getLastHurtByMobTimestamp() < MEMORY_TICKS
                    && isValidTarget(attacker, owner)) {
                return attacker;
            }
            LivingEntity victim = owner.getLastHurtMob();
            if (victim != null
                    && owner.tickCount - owner.getLastHurtMobTimestamp() < MEMORY_TICKS
                    && isValidTarget(victim, owner)) {
                return victim;
            }
        }
        LivingEntity selfAttacker = pig.getLastHurtByMob();
        if (selfAttacker != null && isValidTarget(selfAttacker, owner)) {
            return selfAttacker;
        }
        return null;
    }

    private boolean isValidTarget(LivingEntity candidate, Player owner) {
        if (!candidate.isAlive() || candidate == pig || candidate == owner) return false;
        if (candidate instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        // своїх не чіпаємо: інші свині того ж власника
        if (owner != null && candidate instanceof Pig other && PigBrain.isOwnedBy(other, owner)) return false;
        return pig.distanceToSqr(candidate) < SEARCH_DIST * SEARCH_DIST;
    }
}
