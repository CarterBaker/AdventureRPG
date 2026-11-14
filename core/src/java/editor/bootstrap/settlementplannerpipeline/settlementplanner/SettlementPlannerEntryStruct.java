package editor.bootstrap.settlementplannerpipeline.settlementplanner;

import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import engine.root.StructPackage;

public class SettlementPlannerEntryStruct extends StructPackage {

    /*
     * One thing the settlement planner can design and the World Editor place:
     * whether it is a settlement or a single structure, its registry name,
     * and the name shown for it.
     */

    // Identity
    private final WorldPlacementKind kind;
    private final String name;
    private final String displayName;

    // Constructor \\

    public SettlementPlannerEntryStruct(WorldPlacementKind kind, String name, String displayName) {

        // Identity
        this.kind = kind;
        this.name = name;
        this.displayName = displayName;
    }

    // Accessible \\

    public WorldPlacementKind getKind() {
        return kind;
    }

    public boolean isSettlement() {
        return kind == WorldPlacementKind.SETTLEMENT;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
}
