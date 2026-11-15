package application.bootstrap.entitypipeline.bonecontrol;

import engine.root.StructPackage;

public class BoneControlStruct extends StructPackage {

    /*
     * One control of a rig: a character slider that drives several bone
     * sliders at once, each along its own curve, the way a set driven key
     * drives a face rig. Known by its name, the name races, saves and
     * presets know the slider by, and listed under its group.
     */

    // Identity
    private final String controlName;
    private final String groupName;

    // Drives
    private final BoneControlDriveStruct[] drives;

    // Constructor \\

    public BoneControlStruct(String controlName, String groupName, BoneControlDriveStruct[] drives) {

        // Identity
        this.controlName = controlName;
        this.groupName = groupName;

        // Drives
        this.drives = drives;
    }

    // Accessible \\

    public String getControlName() {
        return controlName;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getDriveCount() {
        return drives.length;
    }

    public BoneControlDriveStruct getDrive(int driveIndex) {
        return drives[driveIndex];
    }
}
