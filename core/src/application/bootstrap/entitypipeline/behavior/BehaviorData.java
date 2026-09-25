package application.bootstrap.entitypipeline.behavior;

import engine.root.DataPackage;

public class BehaviorData extends DataPackage {

    /*
     * Immutable behavior definition loaded from JSON. Holds the identity and
     * movement rules for one named behavior type. Owned by BehaviorHandle
     * for the engine lifetime. turnResponsiveness is how quickly the body
     * swings round to face where the entity is heading, per second.
     */

    // Identity
    private final String behaviorName;
    private final short behaviorID;

    // Rules
    private final float jumpDuration;
    private final float turnResponsiveness;

    // Constructor \\

    public BehaviorData(
            String behaviorName,
            short behaviorID,
            float jumpDuration,
            float turnResponsiveness) {

        // Identity
        this.behaviorName = behaviorName;
        this.behaviorID = behaviorID;

        // Rules
        this.jumpDuration = jumpDuration;
        this.turnResponsiveness = turnResponsiveness;
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

    public float getTurnResponsiveness() {
        return turnResponsiveness;
    }
}