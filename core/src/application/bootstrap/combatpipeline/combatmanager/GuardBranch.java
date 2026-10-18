package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.weapon.WeaponHandle;
import application.bootstrap.combatpipeline.weaponmanager.WeaponManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import engine.root.BranchPackage;

class GuardBranch extends BranchPackage {

    /*
     * Holds a weapon's guard for CombatManager. raise() lifts the guard
     * toward a side over the weapon's raise time; control() keeps it up for
     * as long as the entity holds it, swings it over the weapon's shift time
     * to whatever side the entity turns it toward, and lowers it once let
     * go. A guard is held with the weapon the entity fights with, toward the
     * side asked for or the one it falls back to when the weapon cannot
     * guard that side, playing the clip the weapon has for the entity's rig.
     * catches() is the one place that decides whether a guard meets a blow
     * from a side.
     */

    // Internal
    private WeaponManager weaponManager;
    private CombatManager combatManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.weaponManager = get(WeaponManager.class);
        this.combatManager = get(CombatManager.class);
    }

    // Guard \\

    void raise(EntityInstance entity, CombatDirection direction) {

        short weaponID = combatManager.resolveWeapon(entity);
        WeaponHandle weaponHandle = weaponManager.getWeaponHandleFromWeaponID(weaponID);
        CombatDirection side = weaponHandle.resolveGuardSide(direction);

        entity.getEntityActionHandle().holdMove(
                EntityAction.BLOCK,
                side,
                weaponID,
                weaponHandle.getGuardClip(entity.getEntityData().getRigHandle(), side),
                weaponHandle.getGuardRaise());
    }

    void control(EntityInstance entity, EntityInputHandle input) {

        EntityActionHandle actionHandle = entity.getEntityActionHandle();

        if (!input.isBlockAction()) {
            actionHandle.release();
            return;
        }

        WeaponHandle weaponHandle = weaponManager.getWeaponHandleFromWeaponID(actionHandle.getWeaponID());
        CombatDirection side = weaponHandle.resolveGuardSide(input.getGuardDirection());

        if (side == actionHandle.getDirection())
            return;

        actionHandle.shift(
                side,
                weaponHandle.getGuardClip(entity.getEntityData().getRigHandle(), side),
                weaponHandle.getGuardShift());
    }

    // Catch \\

    // A guard meets a blow from the attacker's side when it is held toward that side's mirror
    boolean catches(EntityInstance target, CombatDirection side) {
        return target.getEntityActionHandle().getDirection() == side.mirror();
    }

    // The share of a blow the raised guard's weapon catches on its own
    float resolveCatch(EntityInstance target) {
        return weaponManager.getWeaponHandleFromWeaponID(target.getEntityActionHandle().getWeaponID())
                .getGuardCatch();
    }
}
