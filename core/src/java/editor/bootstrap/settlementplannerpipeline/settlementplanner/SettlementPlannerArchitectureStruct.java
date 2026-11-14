package editor.bootstrap.settlementplannerpipeline.settlementplanner;

import engine.root.StructPackage;

public class SettlementPlannerArchitectureStruct extends StructPackage {

    /*
     * One architecture the designed settlement can be built in: its registry
     * name, empty for whichever one the biome under it picks, and the name
     * shown for it.
     */

    // Identity
    private final String architectureName;
    private final String displayName;

    // Constructor \\

    public SettlementPlannerArchitectureStruct(String architectureName, String displayName) {

        // Identity
        this.architectureName = architectureName;
        this.displayName = displayName;
    }

    // Accessible \\

    public String getArchitectureName() {
        return architectureName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
