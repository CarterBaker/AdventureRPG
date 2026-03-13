package com.internal.bootstrap.entitypipeline.behavior;

import com.internal.core.engine.DataPackage;

public class BehaviorData extends DataPackage {

    // Identity
    public final String behaviorName;
    public final short behaviorID;

    // Rules
    public final float jumpDuration; // seconds — -1 = uncapped (flyers)

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
}