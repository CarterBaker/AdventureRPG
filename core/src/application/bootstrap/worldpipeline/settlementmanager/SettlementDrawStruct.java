package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import engine.root.StructPackage;

class SettlementDrawStruct extends StructPackage {

    /*
     * The settlement types one architecture grows on its own, in name order,
     * with their running weight totals, kept together so a draw never pairs
     * types from one table with weights from another while the tables are
     * rebuilt after a live edit.
     */

    // Draw
    private final SettlementHandle[] settlements;
    private final float[] cumulativeWeights;

    // Constructor \\

    SettlementDrawStruct(SettlementHandle[] settlements, float[] cumulativeWeights) {

        // Draw
        this.settlements = settlements;
        this.cumulativeWeights = cumulativeWeights;
    }

    // Accessible \\

    SettlementHandle[] getSettlements() {
        return settlements;
    }

    float[] getCumulativeWeights() {
        return cumulativeWeights;
    }
}
