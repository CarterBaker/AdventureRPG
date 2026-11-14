package application.bootstrap.combatpipeline.weapon;

import engine.root.StructPackage;

public class AttackStruct extends StructPackage {

    /*
     * Immutable attack of a weapon from one CombatDirection, the same for
     * every body that wields it: how many seconds it winds up before the
     * blow lands and recovers after, how much harder and further it reaches
     * than the entity's plain strike, the arc in degrees its blow sweeps
     * across, zero for a straight thrust, and the damage type its blow lands
     * as. The clip each body plays it with lives on that body's
     * WeaponRigStruct.
     */

    // Timing
    private final float windup;
    private final float recovery;

    // Effect
    private final float damage;
    private final float reach;
    private final float arc;
    private final short damageTypeID;

    // Constructor \\

    public AttackStruct(
            float windup,
            float recovery,
            float damage,
            float reach,
            float arc,
            short damageTypeID) {

        // Timing
        this.windup = windup;
        this.recovery = recovery;

        // Effect
        this.damage = damage;
        this.reach = reach;
        this.arc = arc;
        this.damageTypeID = damageTypeID;
    }

    // Accessible \\

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

    public short getDamageTypeID() {
        return damageTypeID;
    }
}
