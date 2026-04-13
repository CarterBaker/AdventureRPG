package application.bootstrap.entitypipeline.behavior;

import engine.root.DataPackage;

public class BehaviorData extends DataPackage {

    /*
     * Immutable behavior definition loaded from JSON. Holds the identity and
     * movement rules for one named behavior type. Owned by BehaviorHandle
     * for the engine lifetime.
     */

    // Identity
    private final String behaviorName;
    private final short behaviorID;

    // Rules
    private final float jumpDuration;

    // Constructor \\

    public BehaviorData(
            String behaviorName,
            short behaviorID,
            float jumpDuration) {

        // Identity
        this.behaviorName = behaviorName;
        this.behaviorID = behaviorID;

        // Rules
        this.jumpDuration = jumpDuration;
    }

    // Accessible \\

    public String getBehaviorName() {
        return behaviorName;
    }

    public short getBehaviorID() {
        return behaviorID;
    }

    public float getJumpDuration() {
        return jumpDuration;
    }
}