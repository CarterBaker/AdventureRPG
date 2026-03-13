package com.internal.bootstrap.entitypipeline.behavior;

import com.internal.core.engine.HandlePackage;

public class BehaviorHandle extends HandlePackage {

    // Internal
    private BehaviorData behaviorData;

    // Constructor \\
    public void constructor(BehaviorData behaviorData) {

        // Internal
        this.behaviorData = behaviorData;
    }

    // Accessible \\

    public BehaviorData getBehaviorData() {
        return behaviorData;
    }

    public String getBehaviorName() {
        return behaviorData.behaviorName;
    }

    public short getBehaviorID() {
        return behaviorData.behaviorID;
    }

    public float getJumpDuration() {
        return behaviorData.jumpDuration;
    }
}