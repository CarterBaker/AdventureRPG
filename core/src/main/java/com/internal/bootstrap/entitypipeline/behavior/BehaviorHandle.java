package com.internal.bootstrap.entitypipeline.behavior;

import com.internal.core.engine.HandlePackage;

public class BehaviorHandle extends HandlePackage {

    // Identity
    private String behaviorName;
    private short behaviorID;

    // Rules
    private float jumpDuration; // seconds — -1 = uncapped (flyers)

    // Constructor \\

    public void constructor(
            String behaviorName,
            short behaviorID,
            float jumpDuration) {

        this.behaviorName = behaviorName;
        this.behaviorID = behaviorID;
        this.jumpDuration = jumpDuration;
    }

    // Getters \\

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