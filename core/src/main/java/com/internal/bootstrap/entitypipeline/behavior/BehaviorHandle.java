package com.internal.bootstrap.entitypipeline.behavior;

import com.internal.core.engine.HandlePackage;

public class BehaviorHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded behavior definition. Registered and
     * owned by BehaviorManager. Delegates all accessors through BehaviorData.
     */

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
        return behaviorData.getBehaviorName();
    }

    public short getBehaviorID() {
        return behaviorData.getBehaviorID();
    }

    public float getJumpDuration() {
        return behaviorData.getJumpDuration();
    }
}