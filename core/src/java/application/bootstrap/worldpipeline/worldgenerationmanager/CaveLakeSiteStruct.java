package application.bootstrap.worldpipeline.worldgenerationmanager;

import engine.root.EngineSetting;
import engine.root.StructPackage;

class CaveLakeSiteStruct extends StructPackage {

    /*
     * One underground lake: a basin chamber whose bowl holds still water up
     * to its own level and whose dome rises open above it. The chamber is an
     * ellipsoid squashed below the waterline to the bowl's depth and
     * stretched above it to the dome's headroom. Below the waterline a guard
     * shell of solid rock surrounds the bowl, closing every other cave that
     * would breach it, and a taper beyond the guard narrows them smoothly
     * before they reach it, so the water is only ever held by rock. The
     * centre is kept in the coordinates of whatever frame collected it, and
     * the CaveDensity kernel reads the chamber and its guard from a copy of
     * these fields.
     */

    float centerX;
    float centerZ;
    int waterY;
    float radiusX;
    float radiusZ;
    float depth;
    float headroom;

    void set(
            float centerX,
            float centerZ,
            int waterY,
            float radiusX,
            float radiusZ,
            float depth,
            float headroom) {

        this.centerX = centerX;
        this.centerZ = centerZ;
        this.waterY = waterY;
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
        this.depth = depth;
        this.headroom = headroom;
    }

    // Bounds \\

    float getReachBlocks() {
        return Math.max(radiusX, radiusZ) + EngineSetting.CAVE_LAKE_GUARD_BLOCKS + EngineSetting.CAVE_LAKE_TAPER_BLOCKS;
    }

    float getLowestY() {
        return getSurfaceY() - depth - EngineSetting.CAVE_LAKE_GUARD_BLOCKS - EngineSetting.CAVE_LAKE_TAPER_BLOCKS;
    }

    float getHighestY() {
        return getSurfaceY() + headroom;
    }

    // The top face of the highest water block
    float getSurfaceY() {
        return waterY + 1f;
    }
}
