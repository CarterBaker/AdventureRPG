package application.bootstrap.worldpipeline.river;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.StructPackage;

public class RiverChangeStruct extends StructPackage {

    /*
     * The span of drainage cells a rebuilt network changed against the one it
     * replaced, widened by how far a river reaches beyond its cells, so the
     * terrain and maps it can alter are streamed again and nothing else is.
     * Inclusive cell bounds, unwrapped.
     */

    // World
    private final WorldHandle worldHandle;

    // Bounds — drainage cells
    private final int minCellX;
    private final int minCellZ;
    private final int maxCellX;
    private final int maxCellZ;

    // Constructor \\

    public RiverChangeStruct(WorldHandle worldHandle, int minCellX, int minCellZ, int maxCellX, int maxCellZ) {

        // World
        this.worldHandle = worldHandle;

        // Bounds
        this.minCellX = minCellX;
        this.minCellZ = minCellZ;
        this.maxCellX = maxCellX;
        this.maxCellZ = maxCellZ;
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public int getMinCellX() {
        return minCellX;
    }

    public int getMinCellZ() {
        return minCellZ;
    }

    public int getMaxCellX() {
        return maxCellX;
    }

    public int getMaxCellZ() {
        return maxCellZ;
    }
}
