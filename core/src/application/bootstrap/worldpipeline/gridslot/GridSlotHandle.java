package application.bootstrap.worldpipeline.gridslot;

import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import engine.root.HandlePackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GridSlotHandle extends HandlePackage {

    /*
     * Persistent slot record for a single grid coordinate. Owns a UBOInstance
     * pre-seeded with the GPU grid position. Holds culling metrics for both
     * the individual chunk footprint and the full mega-chunk footprint, plus
     * a list of all grid slots covered by this mega-chunk origin.
     */

    // Identity
    private long gridCoordinate;
    private UBOInstance slotUBO;
    private GridSlotDetailLevel detailLevel;
    private GridInstance gridInstance;

    // Chunk culling
    private float chunkDistanceFromCenter;
    private float chunkAngleFromCenter;

    // Mega culling
    private float megaDistanceFromCenter;
    private float megaAngleFromCenter;

    // Mega coverage
    private ObjectArrayList<GridSlotHandle> coveredSlots;

    // Constructor \\

    public void constructor(
            long gridCoordinate,
            UBOInstance slotUBO,
            float chunkDistanceFromCenter,
            float chunkAngleFromCenter,
            float megaDistanceFromCenter,
            float megaAngleFromCenter,
            GridSlotDetailLevel detailLevel,
            GridInstance gridInstance) {

        this.gridCoordinate = gridCoordinate;
        this.slotUBO = slotUBO;
        this.chunkDistanceFromCenter = chunkDistanceFromCenter;
        this.chunkAngleFromCenter = chunkAngleFromCenter;
        this.megaDistanceFromCenter = megaDistanceFromCenter;
        this.megaAngleFromCenter = megaAngleFromCenter;
        this.detailLevel = detailLevel;
        this.gridInstance = gridInstance;
        this.coveredSlots = new ObjectArrayList<>();
    }

    // Computed Coordinate Lookups \\

    public long getChunkCoordinate() {
        return Coordinate2Long.add(gridInstance.getActiveChunkCoordinate(), gridCoordinate);
    }

    public long getMegaCoordinate() {
        return Coordinate2Long.toMegaChunkCoordinate(getChunkCoordinate());
    }

    // Accessible \\

    public long getGridCoordinate() {
        return gridCoordinate;
    }

    public UBOInstance getSlotUBO() {
        return slotUBO;
    }

    public float getChunkDistanceFromCenter() {
        return chunkDistanceFromCenter;
    }

    public float getChunkAngleFromCenter() {
        return chunkAngleFromCenter;
    }

    public float getMegaDistanceFromCenter() {
        return megaDistanceFromCenter;
    }

    public float getMegaAngleFromCenter() {
        return megaAngleFromCenter;
    }

    public GridSlotDetailLevel getDetailLevel() {
        return detailLevel;
    }

    public ObjectArrayList<GridSlotHandle> getCoveredSlots() {
        return coveredSlots;
    }
}