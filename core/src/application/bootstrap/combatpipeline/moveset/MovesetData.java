package application.bootstrap.combatpipeline.moveset;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import engine.root.DataPackage;

public class MovesetData extends DataPackage {

    /*
     * Immutable moveset definition loaded from ARPG — how one kind of weapon
     * fights. Holds an attack for every CombatDirection and a guard clip for
     * every CombatDirection, how many seconds a guard takes to raise from
     * rest and to swing from one side to another, and the share of a blow it
     * catches before whatever it is held up with adds its own. Owned by
     * MovesetHandle for the engine lifetime.
     */

    // Identity
    private final String movesetName;
    private final short movesetID;

    // Attacks — indexed by CombatDirection ordinal
    private final AttackStruct[] attacks;

    // Guard — clips indexed by CombatDirection ordinal
    private final AnimationClipHandle[] guardClips;
    private final float guardRaise;
    private final float guardShift;
    private final float guardCatch;

    // Constructor \\

    public MovesetData(
            String movesetName,
            short movesetID,
            AttackStruct[] attacks,
            AnimationClipHandle[] guardClips,
            float guardRaise,
            float guardShift,
            float guardCatch) {

        // Identity
        this.movesetName = movesetName;
        this.movesetID = movesetID;

        // Attacks
        this.attacks = attacks;

        // Guard
        this.guardClips = guardClips;
        this.guardRaise = guardRaise;
        this.guardShift = guardShift;
        this.guardCatch = guardCatch;
    }

    // Accessible \\

    public String getMovesetName() {
        return movesetName;
    }

    public short getMovesetID() {
        return movesetID;
    }

    public AttackStruct getAttack(CombatDirection direction) {
        return attacks[direction.ordinal()];
    }

    public AnimationClipHandle getGuardClip(CombatDirection direction) {
        return guardClips[direction.ordinal()];
    }

    public float getGuardRaise() {
        return guardRaise;
    }

    public float getGuardShift() {
        return guardShift;
    }

    public float getGuardCatch() {
        return guardCatch;
    }
}
