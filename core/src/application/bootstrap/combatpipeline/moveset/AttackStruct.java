package application.bootstrap.combatpipeline.moveset;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.StructPackage;

public class AttackStruct extends StructPackage {

    /*
     * Immutable attack of a moveset from one CombatDirection: the clip it
     * plays and the fraction of that clip its blow lands on, how many seconds
     * it winds up before the blow lands and recovers after, how much harder
     * and further it reaches than the entity's plain strike, and the arc in
     * degrees its blow sweeps across, zero for a straight thrust.
     */

    // Animation
    private final AnimationClipHandle clip;
    private final float strike;

    // Timing
    private final float windup;
    private final float recovery;

    // Effect
    private final float damage;
    private final float reach;
    private final float arc;

    // Constructor \\

    public AttackStruct(
            AnimationClipHandle clip,
            float strike,
            float windup,
            float recovery,
            float damage,
            float reach,
            float arc) {

        // Animation
        this.clip = clip;
        this.strike = strike;

        // Timing
        this.windup = windup;
        this.recovery = recovery;

        // Effect
        this.damage = damage;
        this.reach = reach;
        this.arc = arc;
    }

    // Accessible \\

    public AnimationClipHandle getClip() {
        return clip;
    }

    public float getStrike() {
        return strike;
    }

    public float getWindup() {
        return windup;
    }

    public float getRecovery() {
        return recovery;
    }

    public float getDuration() {
        return windup + recovery;
    }

    // The fraction of the attack's duration at which its blow lands
    public float getImpact() {
        return windup / (windup + recovery);
    }

    public float getDamage() {
        return damage;
    }

    public float getReach() {
        return reach;
    }

    public float getArc() {
        return arc;
    }
}
