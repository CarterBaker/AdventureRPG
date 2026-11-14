package application.bootstrap.combatpipeline.weapon;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;

public class WeaponData extends DataPackage {

    /*
     * Immutable weapon definition loaded from ARPG — how one kind of held
     * thing, or a body's own, fights. Holds the attack it makes from each
     * side it has and the sides its guard can be held toward, both resolved
     * once for every CombatDirection through its fallbacks, so a side the
     * weapon lacks plays the one it falls back to; how many seconds a guard
     * takes to raise from rest and to swing from one side to another, and the
     * share of a blow it catches before whatever it is held up with adds its
     * own; the mass a body's own weapon drives into a blow, as a held item
     * drives its weight; and the animation for every rig that can wield it —
     * a body whose rig it does not list cannot. Owned by WeaponHandle for the
     * engine lifetime.
     */

    // Identity
    private final String weaponName;
    private final short weaponID;

    // Attacks — indexed by CombatDirection ordinal
    private final AttackStruct[] attacks;
    private final CombatDirection[] attackSides;

    // Guard — sides indexed by CombatDirection ordinal
    private final CombatDirection[] guardSides;
    private final float guardRaise;
    private final float guardShift;
    private final float guardCatch;

    // Mass
    private final float mass;

    // Rigs
    private final WeaponRigStruct[] rigs;

    // Constructor \\

    public WeaponData(
            String weaponName,
            short weaponID,
            AttackStruct[] attacks,
            CombatDirection[] attackSides,
            CombatDirection[] guardSides,
            float guardRaise,
            float guardShift,
            float guardCatch,
            float mass,
            WeaponRigStruct[] rigs) {

        // Identity
        this.weaponName = weaponName;
        this.weaponID = weaponID;

        // Attacks
        this.attacks = attacks;
        this.attackSides = attackSides;

        // Guard
        this.guardSides = guardSides;
        this.guardRaise = guardRaise;
        this.guardShift = guardShift;
        this.guardCatch = guardCatch;

        // Mass
        this.mass = mass;

        // Rigs
        this.rigs = rigs;
    }

    // Accessible \\

    public String getWeaponName() {
        return weaponName;
    }

    public short getWeaponID() {
        return weaponID;
    }

    // The side the weapon attacks from when asked for one — the side itself, or the first fallback it has
    public CombatDirection resolveAttackSide(CombatDirection direction) {
        return attackSides[direction.ordinal()];
    }

    // The attack from a side the weapon has
    public AttackStruct getAttack(CombatDirection side) {
        return attacks[side.ordinal()];
    }

    // The side the guard is held toward when asked for one — the side itself, or the first fallback it has
    public CombatDirection resolveGuardSide(CombatDirection direction) {
        return guardSides[direction.ordinal()];
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

    public float getMass() {
        return mass;
    }

    // Rigs \\

    // The weapon's animation for a rig, null when it is not made for that rig
    public WeaponRigStruct getRig(RigHandle rigHandle) {

        for (int i = 0; i < rigs.length; i++)
            if (rigs[i].getRigHandle() == rigHandle)
                return rigs[i];

        return null;
    }

    public boolean hasRig(RigHandle rigHandle) {
        return getRig(rigHandle) != null;
    }

    // The clip an attack plays on a rig, null when the weapon has no animation for it
    public AnimationClipHandle getAttackClip(RigHandle rigHandle, CombatDirection side) {

        WeaponRigStruct rig = getRig(rigHandle);

        return rig != null ? rig.getAttackClip(side) : null;
    }

    // The fraction of its clip an attack's blow lands on for a rig
    public float getAttackStrike(RigHandle rigHandle, CombatDirection side) {

        WeaponRigStruct rig = getRig(rigHandle);

        return rig != null ? rig.getAttackStrike(side) : EngineSetting.DEFAULT_ATTACK_STRIKE;
    }

    // The clip a guard is held with on a rig, null when the weapon has no animation for it
    public AnimationClipHandle getGuardClip(RigHandle rigHandle, CombatDirection side) {

        WeaponRigStruct rig = getRig(rigHandle);

        return rig != null ? rig.getGuardClip(side) : null;
    }
}
