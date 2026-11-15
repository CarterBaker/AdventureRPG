package application.bootstrap.entitypipeline.bonecontrol;

import engine.root.DataPackage;

public class BoneControlData extends DataPackage {

    /*
     * Immutable controls of one rig, in the order its file lists them,
     * shared by every race and stamp built on the rig.
     */

    // Identity
    private final String rigName;
    private final short boneControlID;

    // Controls
    private final BoneControlStruct[] controls;

    // Constructor \\

    public BoneControlData(String rigName, short boneControlID, BoneControlStruct[] controls) {

        // Identity
        this.rigName = rigName;
        this.boneControlID = boneControlID;

        // Controls
        this.controls = controls;
    }

    // Accessible \\

    public String getRigName() {
        return rigName;
    }

    public short getBoneControlID() {
        return boneControlID;
    }

    public int getControlCount() {
        return controls.length;
    }

    public BoneControlStruct getControl(int controlIndex) {
        return controls[controlIndex];
    }
}
