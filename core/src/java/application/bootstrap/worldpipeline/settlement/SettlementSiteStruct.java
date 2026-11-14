package application.bootstrap.worldpipeline.settlement;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import engine.root.StructPackage;

public class SettlementSiteStruct extends StructPackage {

    /*
     * Where one settlement stands and what it is, settled before it is
     * planned: its type and the architecture building it, the settlement
     * cell it belongs to, its centre column in wrapped world blocks, the
     * seed its layout is planned from, and whether it was placed by hand.
     */

    // Identity
    private final SettlementHandle settlementHandle;
    private final ArchitectureHandle architectureHandle;

    // Cell
    private final int cellX;
    private final int cellZ;

    // Centre — wrapped world blocks
    private final long centerX;
    private final long centerZ;

    // Plan
    private final long seed;
    private final boolean handPlaced;

    // Constructor \\

    public SettlementSiteStruct(
            SettlementHandle settlementHandle,
            ArchitectureHandle architectureHandle,
            int cellX,
            int cellZ,
            long centerX,
            long centerZ,
            long seed,
            boolean handPlaced) {

        // Identity
        this.settlementHandle = settlementHandle;
        this.architectureHandle = architectureHandle;

        // Cell
        this.cellX = cellX;
        this.cellZ = cellZ;

        // Centre
        this.centerX = centerX;
        this.centerZ = centerZ;

        // Plan
        this.seed = seed;
        this.handPlaced = handPlaced;
    }

    // Accessible \\

    public SettlementHandle getSettlementHandle() {
        return settlementHandle;
    }

    public ArchitectureHandle getArchitectureHandle() {
        return architectureHandle;
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellZ() {
        return cellZ;
    }

    public long getCenterX() {
        return centerX;
    }

    public long getCenterZ() {
        return centerZ;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isHandPlaced() {
        return handPlaced;
    }
}
