package editor.bootstrap.stamppipeline.stamp;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.stamppipeline.util.StampCategory;
import engine.root.HandlePackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.mathematics.vectors.Vector3;

public class StampHandle extends HandlePackage {

    /*
     * Persistent reference to one stamp. Registered and owned by
     * StampManager. Delegates all accessors through StampData, which a live
     * edit of the stamp replaces in place, so the Stamps tab and a placement
     * under way take the edit at once.
     */

    // Internal
    private StampData stampData;

    // Constructor \\

    public void constructor(StampData stampData) {

        // Internal
        this.stampData = stampData;
    }

    // Accessible \\

    public String getStampName() {
        return stampData.getStampName();
    }

    public int getStampID() {
        return stampData.getStampID();
    }

    public String getDisplayName() {
        return stampData.getDisplayName();
    }

    public StampCategory getCategory() {
        return stampData.getCategory();
    }

    public RigHandle getRigHandle() {
        return stampData.getRigHandle();
    }

    public ArpgArrayStruct getBonesArpg() {
        return stampData.getBonesArpg();
    }

    public ArpgArrayStruct getPartsArpg() {
        return stampData.getPartsArpg();
    }

    public RiggedMeshStruct getMesh() {
        return stampData.getMesh();
    }

    public Vector3 getBoundsMin() {
        return stampData.getBoundsMin();
    }

    public Vector3 getBoundsMax() {
        return stampData.getBoundsMax();
    }

    public boolean hasPlug() {
        return stampData.hasPlug();
    }

    public StampSocketStruct getPlug() {
        return stampData.getPlug();
    }

    public int getSocketCount() {
        return stampData.getSocketCount();
    }

    public StampSocketStruct getSocket(int index) {
        return stampData.getSocket(index);
    }

    // Whether the stamp is fur, its card laid over the body rather than joined to it
    public boolean hasFur() {
        return stampData.hasFur();
    }

    public StampFurStruct getFur() {
        return stampData.getFur();
    }
}
