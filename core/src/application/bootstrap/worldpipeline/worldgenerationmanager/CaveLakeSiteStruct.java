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
     * centre is kept in the coordinates of whatever frame collected it.
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

    // Chamber \\

    // Blocks from the chamber's wall, positive inside it, its outline wobbled by a slow field
    float computeChamber(float pointX, float pointY, float pointZ, float wobble) {

        float above = pointY - getSurfaceY();
        float axisY = above >= 0f ? headroom : depth;
        float ex = (pointX - centerX) / radiusX;
        float ey = above / axisY;
        float ez = (pointZ - centerZ) / radiusZ;
        float extent = (float) Math.sqrt(ex * ex + ey * ey + ez * ez) + wobble * EngineSetting.CAVE_LAKE_SHORE_WOBBLE;

        return (1f - extent) * Math.min(Math.min(radiusX, radiusZ), axisY);
    }

    // Whether a point lies below the waterline, where the chamber holds water
    boolean isBelowWaterline(float pointY) {
        return pointY < getSurfaceY();
    }

    // Guard \\

    // How far every other cave is pushed closed at a point: wholly within the guard shell, easing out through the taper
    float computeGuardPenalty(float pointX, float pointY, float pointZ) {

        float below = getSurfaceY() - pointY;

        if (below <= 0f)
            return 0f;

        float guard = EngineSetting.CAVE_LAKE_GUARD_BLOCKS;
        float reach = guard + EngineSetting.CAVE_LAKE_TAPER_BLOCKS;
        float dx = pointX - centerX;
        float dz = pointZ - centerZ;

        float guardExtent = computeExtent(dx, below, dz, guard);

        if (guardExtent < 1f)
            return -EngineSetting.CAVE_DENSITY_SOLID;

        float taperExtent = computeExtent(dx, below, dz, reach);

        if (taperExtent >= 1f)
            return 0f;

        float share = (1f - taperExtent) / Math.max(1f - taperExtent / guardExtent, EngineSetting.DIVISION_EPSILON);

        return EngineSetting.CAVE_LAKE_TAPER_PENALTY_BLOCKS * Math.min(share, 1f);
    }

    private float computeExtent(float dx, float below, float dz, float grow) {

        float ex = dx / (radiusX + grow);
        float ey = below / (depth + grow);
        float ez = dz / (radiusZ + grow);

        return (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
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
