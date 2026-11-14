package application.bootstrap.worldpipeline.structurelistmanager;

import java.io.File;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structurelist.StructureListData;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.WeightedTableUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class StructureListBuilder extends BuilderPackage {

    /*
     * Parses one structure list ARPG file into a StructureListData and wraps
     * it in a StructureListHandle. Its "structures" each name a structure and
     * an optional "weight", one by default, and every structure is resolved
     * here, so a list naming an unknown structure fails at boot.
     */

    // Internal
    private StructureListManager structureListManager;
    private StructureManager structureManager;

    // Base \\

    @Override
    protected void get() {
        this.structureListManager = get(StructureListManager.class);
        this.structureManager = get(StructureManager.class);
    }

    // Build \\

    StructureListHandle build(File file, String structureListName) {

        short structureListID = structureListManager.registerStructureListName(structureListName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct structuresArpg = ArpgUtility.validateArray(arpg, "structures");

        StructureHandle[] structures = new StructureHandle[structuresArpg.size()];
        FloatArrayList weights = new FloatArrayList(structuresArpg.size());

        for (int i = 0; i < structuresArpg.size(); i++) {

            ArpgObjectStruct entry = structuresArpg.get(i).getAsObject();

            structures[i] = structureManager.getStructureHandleFromStructureName(
                    ArpgUtility.validateString(entry, "structure"));
            weights.add(ArpgUtility.getFloat(entry, "weight", 1f));
        }

        StructureListData structureListData = new StructureListData(
                structureListName,
                structureListID,
                structures,
                WeightedTableUtility.buildCumulative(weights, structureListName));

        StructureListHandle structureListHandle = create(StructureListHandle.class);
        structureListHandle.constructor(structureListData);

        return structureListHandle;
    }
}
