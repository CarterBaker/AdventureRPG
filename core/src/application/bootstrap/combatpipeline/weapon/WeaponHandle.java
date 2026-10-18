package application.bootstrap.combatpipeline.weapon;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.HandlePackage;

public class WeaponHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded weapon. Registered and owned by
     * WeaponManager and shared by every item and entity that names it.
     * Delegates all accessors through WeaponData.
     */

    // Internal
    private WeaponData weaponData;

    // Constructor \\

    public void constructor(WeaponData weaponData) {

        // Internal
        this.weaponData = weaponData;
    }

    // Accessible \\

    public WeaponData getWeaponData() {
        return weaponData;
    }

    public String getWeaponName() {
        return weaponData.getWeaponName();
    }

    public short getWeaponID() {
        return weaponData.getWeaponID();
    }

    public CombatDirection resolveAttackSide(CombatDirection direction) {
        return weaponData.resolveAttackSide(direction);
    }

    public AttackStruct getAttack(CombatDirection side) {
        return weaponData.getAttack(side);
    }

    public CombatDirection resolveGuardSide(CombatDirection direction) {
        return weaponData.resolveGuardSide(direction);
    }

    public float getGuardRaise() {
        return weaponData.getGuardRaise();
    }

    public float getGuardShift() {
        return weaponData.getGuardShift();
    }

    public float getGuardCatch() {
        return weaponData.getGuardCatch();
    }

    public float getMass() {
        return weaponData.getMass();
    }

    public boolean hasRig(RigHandle rigHandle) {
        return weaponData.hasRig(rigHandle);
    }

    public AnimationClipHandle getAttackClip(RigHandle rigHandle, CombatDirection side) {
        return weaponData.getAttackClip(rigHandle, side);
    }

    public float getAttackStrike(RigHandle rigHandle, CombatDirection side) {
        return weaponData.getAttackStrike(rigHandle, side);
    }

    public AnimationClipHandle getGuardClip(RigHandle rigHandle, CombatDirection side) {
        return weaponData.getGuardClip(rigHandle, side);
    }
}
