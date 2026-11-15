package application.bootstrap.entitypipeline.bonecontrolmanager;

import application.bootstrap.entitypipeline.bonecontrol.BoneControlData;
import application.bootstrap.entitypipeline.bonecontrol.BoneControlDriveStruct;
import application.bootstrap.entitypipeline.bonecontrol.BoneControlHandle;
import application.bootstrap.entitypipeline.bonecontrol.BoneControlStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class BoneControlBuilder extends BuilderPackage {

    /*
     * Parses a rig's controls file into a BoneControlData and wraps it in a
     * BoneControlHandle. "controls" lists each control by its "name", unique
     * in the rig, with the "group" a creator lists it under and the "drives"
     * it moves: each a bone slider by name ("slider"), driven once per
     * control, whether it rises with the control ("increase", true when left
     * out), and at most two "keys", each a place along the control and the
     * share of the slider reached there, both from 0 to 1, in ascending
     * order. Whether each slider exists is checked by the race that wears
     * the rig, once its sliders are known.
     */

    // Internal
    private BoneControlManager boneControlManager;

    // Base \\

    @Override
    protected void get() {
        this.boneControlManager = get(BoneControlManager.class);
    }

    // Build \\

    BoneControlHandle build(ArpgObjectStruct arpg, String rigName) {

        BoneControlHandle handle = create(BoneControlHandle.class);
        handle.constructor(parse(arpg, rigName, boneControlManager.registerRigName(rigName)));

        return handle;
    }

    BoneControlData parse(ArpgObjectStruct arpg, String rigName, short boneControlID) {

        ArpgArrayStruct controlsArpg = ArpgUtility.validateArray(arpg, "controls");
        BoneControlStruct[] controls = new BoneControlStruct[controlsArpg.size()];

        for (int i = 0; i < controls.length; i++) {

            controls[i] = parseControl(controlsArpg.get(i).getAsObject(), rigName);

            for (int j = 0; j < i; j++)
                if (controls[j].getControlName().equals(controls[i].getControlName()))
                    throwException("Bone control \"" + controls[i].getControlName() + "\" is listed twice for rig \""
                            + rigName + "\"");
        }

        return new BoneControlData(rigName, boneControlID, controls);
    }

    // Parse \\

    private BoneControlStruct parseControl(ArpgObjectStruct controlArpg, String rigName) {

        String controlName = ArpgUtility.validateString(controlArpg, "name");
        ArpgArrayStruct drivesArpg = ArpgUtility.validateArray(controlArpg, "drives");
        BoneControlDriveStruct[] drives = new BoneControlDriveStruct[drivesArpg.size()];

        if (drives.length == 0)
            throwException("Bone control \"" + controlName + "\" of rig \"" + rigName + "\" drives nothing");

        for (int i = 0; i < drives.length; i++) {

            drives[i] = parseDrive(drivesArpg.get(i).getAsObject(), controlName, rigName);

            for (int j = 0; j < i; j++)
                if (drives[j].getSliderName().equals(drives[i].getSliderName()))
                    throwException("Bone control \"" + controlName + "\" of rig \"" + rigName + "\" drives slider \""
                            + drives[i].getSliderName() + "\" twice");
        }

        return new BoneControlStruct(
                controlName,
                ArpgUtility.getString(controlArpg, "group", EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT),
                drives);
    }

    private BoneControlDriveStruct parseDrive(ArpgObjectStruct driveArpg, String controlName, String rigName) {

        String sliderName = ArpgUtility.validateString(driveArpg, "slider");
        float[] keys = parseKeys(driveArpg, controlName, sliderName, rigName);

        return new BoneControlDriveStruct(sliderName, ArpgUtility.getBoolean(driveArpg, "increase", true), keys);
    }

    // Each key a place and a share, both within 0 to 1, the places rising
    private float[] parseKeys(ArpgObjectStruct driveArpg, String controlName, String sliderName, String rigName) {

        if (!ArpgUtility.hasArray(driveArpg, "keys"))
            return new float[0];

        ArpgArrayStruct keysArpg = driveArpg.getAsArray("keys");

        if (keysArpg.size() % 2 != 0 || keysArpg.size() > EngineSetting.BONE_CONTROL_MAX_KEYS * 2)
            throwException("Bone control \"" + controlName + "\" of rig \"" + rigName + "\" gives slider \""
                    + sliderName + "\" keys that are not up to " + EngineSetting.BONE_CONTROL_MAX_KEYS
                    + " place and share pairs");

        float[] keys = new float[keysArpg.size()];

        for (int i = 0; i < keys.length; i++) {

            keys[i] = keysArpg.get(i).getAsFloat();

            boolean outside = keys[i] < 0f || keys[i] > 1f;
            boolean falling = i % 2 == 0 && i >= 2 && keys[i] <= keys[i - 2];

            if (outside || falling)
                throwException("Bone control \"" + controlName + "\" of rig \"" + rigName + "\" gives slider \""
                        + sliderName + "\" a key outside 0 to 1, or keys whose places do not rise");
        }

        return keys;
    }
}
