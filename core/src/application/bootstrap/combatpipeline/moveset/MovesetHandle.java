package application.bootstrap.combatpipeline.moveset;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.entitypipeline.entity.CombatDirection;
import engine.root.HandlePackage;

public class MovesetHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded moveset. Registered and owned by
     * MovesetManager and shared by every item that names it. Delegates all
     * accessors through MovesetData.
     */

    // Internal
    private MovesetData movesetData;

    // Constructor \\

    public void constructor(MovesetData movesetData) {

        // Internal
        this.movesetData = movesetData;
    }

    // Accessible \\

    public MovesetData getMovesetData() {
        return movesetData;
    }

    public String getMovesetName() {
        return movesetData.getMovesetName();
    }

    public short getMovesetID() {
        return movesetData.getMovesetID();
    }

    public AttackStruct getAttack(CombatDirection direction) {
        return movesetData.getAttack(direction);
    }

    public AnimationClipHandle getGuardClip(CombatDirection direction) {
        return movesetData.getGuardClip(direction);
    }

    public float getGuardRaise() {
        return movesetData.getGuardRaise();
    }

    public float getGuardShift() {
        return movesetData.getGuardShift();
    }

    public float getGuardCatch() {
        return movesetData.getGuardCatch();
    }
}
