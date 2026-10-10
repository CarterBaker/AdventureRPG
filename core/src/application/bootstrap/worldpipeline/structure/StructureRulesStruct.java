package application.bootstrap.worldpipeline.structure;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

public class StructureRulesStruct extends StructPackage {

    /*
     * Where a structure is appropriate, evaluated at its anchor column:
     * allowed biomes (empty allows all), dry or flooded ground or a cave
     * floor, the height range the ground or cave floor must lie in, and the
     * most it may vary across the footprint.
     */

    private final ShortOpenHashSet biomeIDs;
    private final StructureSurfaceType surfaceType;
    private final int minGroundHeightBlocks;
    private final int maxGroundHeightBlocks;
    private final int maxSlopeBlocks;
    private final boolean slopeLimited;

    public StructureRulesStruct(
            ShortOpenHashSet biomeIDs,
            StructureSurfaceType surfaceType,
            int minGroundHeightBlocks,
            int maxGroundHeightBlocks,
            int maxSlopeBlocks,
            boolean slopeLimited) {

        this.biomeIDs = biomeIDs;
        this.surfaceType = surfaceType;
        this.minGroundHeightBlocks = minGroundHeightBlocks;
        this.maxGroundHeightBlocks = maxGroundHeightBlocks;
        this.maxSlopeBlocks = maxSlopeBlocks;
        this.slopeLimited = slopeLimited;
    }

    public ShortOpenHashSet getBiomeIDs() {
        return biomeIDs;
    }

    public boolean hasBiomeRule() {
        return !biomeIDs.isEmpty();
    }

    public StructureSurfaceType getSurfaceType() {
        return surfaceType;
    }

    public int getMinGroundHeightBlocks() {
        return minGroundHeightBlocks;
    }

    public int getMaxGroundHeightBlocks() {
        return maxGroundHeightBlocks;
    }

    public int getMaxSlopeBlocks() {
        return maxSlopeBlocks;
    }

    public boolean isSlopeLimited() {
        return slopeLimited;
    }
}
