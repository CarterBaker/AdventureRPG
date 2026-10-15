package application.bootstrap.combatpipeline.combatmanager;

import application.bootstrap.combatpipeline.moveset.MovesetHandle;
import application.bootstrap.combatpipeline.movesetmanager.MovesetManager;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import engine.root.BranchPackage;

class GuardBranch extends BranchPackage {

    /*
     * Holds a moveset's guard for CombatManager. raise() lifts the guard
     * toward a side over the moveset's raise time; control() keeps it up for
     * as long as the entity holds it, swings it over the moveset's shift
     * time to whatever side the entity turns it toward, and lowers it once
     * let go. A guard is held with the weapon's moveset, or bare hands' when
     * the main hand holds no weapon. catches() is the one place that decides
     * whether a guard meets a blow from a side.
     */

    // Internal
    private MovesetManager movesetManager;
    private CombatManager combatManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.movesetManager = get(MovesetManager.class);
        this.combatManager = get(CombatManager.class);
    }

    // Guard \\

    void raise(EntityInstance entity, CombatDirection direction) {

        short movesetID = combatManager.resolveGuardMoveset(entity);
        MovesetHandle movesetHandle = movesetManager.getMovesetHandleFromMovesetID(movesetID);

        entity.getEntityActionHandle().holdMove(
                EntityAction.BLOCK,
                direction,
                movesetID,
                movesetHandle.getGuardClip(direction),
                movesetHandle.getGuardRaise());
    }

    void control(EntityInstance entity, EntityInputHandle input) {

        EntityActionHandle actionHandle = entity.getEntityActionHandle();
        CombatDirection direction = input.getGuardDirection();

        if (!input.isBlockAction()) {
            actionHandle.release();
            return;
        }

        if (direction == actionHandle.getDirection())
            return;

        MovesetHandle movesetHandle = movesetManager.getMovesetHandleFromMovesetID(actionHandle.getMovesetID());

        actionHandle.shift(direction, movesetHandle.getGuardClip(direction), movesetHandle.getGuardShift());
    }

    // Catch \\

    // A guard meets a blow from the attacker's side when it is held toward that side's mirror
    boolean catches(EntityInstance target, CombatDirection side) {
        return target.getEntityActionHandle().getDirection() == side.mirror();
    }

    // The share of a blow the raised guard's moveset catches on its own
    float resolveCatch(EntityInstance target) {
        return movesetManager.getMovesetHandleFromMovesetID(target.getEntityActionHandle().getMovesetID())
                .getGuardCatch();
    }
}
