package application.bootstrap.worldpipeline.structurelist;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import engine.root.DataPackage;

public class StructureListData extends DataPackage {

    /*
     * Immutable structure list built from ARPG by StructureListBuilder: the
     * structures that may fill one role of a layout, resolved to their
     * handles, and the running weight totals one roll picks among them with.
     */

    // Identity
    private final String structureListName;
    private final short structureListID;

    // List
    private final StructureHandle[] structures;
    private final float[] cumulativeWeights;

    // Constructor \\

    public StructureListData(
            String structureListName,
            short structureListID,
            StructureHandle[] structures,
            float[] cumulativeWeights) {

        // Identity
        this.structureListName = structureListName;
        this.structureListID = structureListID;

        // List
        this.structures = structures;
        this.cumulativeWeights = cumulativeWeights;
    }

    // Accessible \\

    public String getStructureListName() {
        return structureListName;
    }

    public short getStructureListID() {
        return structureListID;
    }

    public StructureHandle[] getStructures() {
        return structures;
    }

    public float[] getCumulativeWeights() {
        return cumulativeWeights;
    }
}
