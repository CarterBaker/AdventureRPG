package program.bootstrap.entitypipeline.behaviormanager;

import java.io.File;
import com.google.gson.JsonObject;
import program.bootstrap.entitypipeline.behavior.BehaviorData;
import program.bootstrap.entitypipeline.behavior.BehaviorHandle;
import program.core.engine.BuilderPackage;
import program.core.settings.EngineSetting;
import program.core.util.JsonUtility;
import program.core.util.RegistryUtility;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses behavior JSON into a BehaviorData and wraps it in a BehaviorHandle.
     * Derives the short behavior ID from the resource name via RegistryUtility.
     * Bootstrap-only.
     */

    // Build \\

    BehaviorHandle build(File file, String behaviorName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        short behaviorID = RegistryUtility.toShortID(behaviorName);
        float jumpDuration = json.has("jump_duration")
                ? json.get("jump_duration").getAsFloat()
                : EngineSetting.DEFAULT_JUMP_DURATION;

        BehaviorData behaviorData = new BehaviorData(behaviorName, behaviorID, jumpDuration);

        BehaviorHandle handle = create(BehaviorHandle.class);
        handle.constructor(behaviorData);

        return handle;
    }
}