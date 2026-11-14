package application.bootstrap.worldpipeline.structurelist;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import engine.root.HandlePackage;

public class StructureListHandle extends HandlePackage {

    /*
     * Persistent structure list. Wraps StructureListData and delegates all
     * access through it.
     */

    // Internal
    private StructureListData structureListData;

    // Constructor \\

    public void constructor(StructureListData structureListData) {
        this.structureListData = structureListData;
    }

    // Accessible \\

    public StructureListData getStructureListData() {
        return structureListData;
    }

    public String getStructureListName() {
        return structureListData.getStructureListName();
    }

    public short getStructureListID() {
        return structureListData.getStructureListID();
    }

    public StructureHandle[] getStructures() {
        return structureListData.getStructures();
    }

    public float[] getCumulativeWeights() {
        return structureListData.getCumulativeWeights();
    }
}
