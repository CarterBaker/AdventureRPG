package application.bootstrap.combatpipeline.weapon;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.StructPackage;

public class WeaponRigStruct extends StructPackage {

    /*
     * Immutable animation of a weapon for one rig: the clip each of its
     * attacks plays and the fraction of that clip its blow lands on, and the
     * clip each side of its guard is held with. Every clip is made for the
     * rig, and only the sides the weapon has are filled.
     */

    // Rig
    private final RigHandle rigHandle;

    // Attacks — indexed by CombatDirection ordinal
    private final AnimationClipHandle[] attackClips;
    private final float[] attackStrikes;

    // Guard — indexed by CombatDirection ordinal
    private final AnimationClipHandle[] guardClips;

    // Constructor \\

    public WeaponRigStruct(
            RigHandle rigHandle,
            AnimationClipHandle[] attackClips,
            float[] attackStrikes,
            AnimationClipHandle[] guardClips) {

        // Rig
        this.rigHandle = rigHandle;

        // Attacks
        this.attackClips = attackClips;
        this.attackStrikes = attackStrikes;

        // Guard
        this.guardClips = guardClips;
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public AnimationClipHandle getAttackClip(CombatDirection side) {
        return attackClips[side.ordinal()];
    }

    public float getAttackStrike(CombatDirection side) {
        return attackStrikes[side.ordinal()];
    }

    public AnimationClipHandle getGuardClip(CombatDirection side) {
        return guardClips[side.ordinal()];
    }
}
