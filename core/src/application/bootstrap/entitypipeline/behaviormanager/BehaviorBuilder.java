package application.bootstrap.entitypipeline.behaviormanager;

import java.io.File;

import application.bootstrap.entitypipeline.behavior.BehaviorData;
import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.registry.RegistryUtility;

class BehaviorBuilder extends BuilderPackage {

    /*
     * Parses behavior ARPG into a BehaviorData and wraps it in a BehaviorHandle.
     * Derives the short behavior ID from the resource name via RegistryUtility.
     * Bootstrap-only.
     */

    // Build \\

    BehaviorHandle build(File file, String behaviorName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short behaviorID = RegistryUtility.toShortID(behaviorName);
        float jumpDuration = arpg.has("jump_duration")
                ? arpg.get("jump_duration").getAsFloat()
                : EngineSetting.DEFAULT_JUMP_DURATION;
        float turnResponsiveness = ArpgUtility.getFloat(
                arpg,
                "turn_responsiveness",
                EngineSetting.DEFAULT_TURN_RESPONSIVENESS);

        if (turnResponsiveness <= 0f)
            throwException("Behavior \"" + behaviorName + "\" must have a positive \"turn_responsiveness\". File: "
                    + file.getName());

        BehaviorData behaviorData = new BehaviorData(behaviorName, behaviorID, jumpDuration, turnResponsiveness);

        BehaviorHandle handle = create(BehaviorHandle.class);
        handle.constructor(behaviorData);

        return handle;
    }
}