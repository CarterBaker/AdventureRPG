package com.internal.bootstrap.entitypipeline.behaviormanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.RegistryUtility;

class InternalBuilder extends BuilderPackage {

    // Build \\

    BehaviorHandle build(File file, String behaviorName) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        short behaviorID = RegistryUtility.toShortID(behaviorName);
        float jumpDuration = json.has("jump_duration") ? json.get("jump_duration").getAsFloat() : 0.5f;

        BehaviorHandle handle = create(BehaviorHandle.class);
        handle.constructor(behaviorName, behaviorID, jumpDuration);

        return handle;
    }
}