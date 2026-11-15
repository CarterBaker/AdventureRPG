package application.bootstrap.entitypipeline.bonecontrol;

import engine.root.HandlePackage;

public class BoneControlHandle extends HandlePackage {

    /*
     * Persistent reference to the controls of one rig. Registered and owned
     * by BoneControlManager; an editor's change to the rig's controls hands
     * it new data in place, so every race built afterwards reads the
     * controls as they now stand.
     */

    // Internal
    private BoneControlData boneControlData;

    // Constructor \\

    public void constructor(BoneControlData boneControlData) {

        // Internal
        this.boneControlData = boneControlData;
    }

    // Accessible \\

    public BoneControlData getBoneControlData() {
        return boneControlData;
    }

    public String getRigName() {
        return boneControlData.getRigName();
    }

    public short getBoneControlID() {
        return boneControlData.getBoneControlID();
    }

    public int getControlCount() {
        return boneControlData.getControlCount();
    }

    public BoneControlStruct getControl(int controlIndex) {
        return boneControlData.getControl(controlIndex);
    }
}
