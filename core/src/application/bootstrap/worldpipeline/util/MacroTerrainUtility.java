package application.bootstrap.worldpipeline.util;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class MacroTerrainUtility extends EngineUtility {

    /*
     * Stateless geometry of distant macro terrain against the shared world
     * curvature. The curve bends height down by k times the squared distance
     * from the player, so ground h blocks above sea level stays in view out to
     * the square root of h over k past the sea-level horizon; the eye's reach
     * plus the terrain's reach bounds everything the curve lets the eye see.
     * Coverage mirrors the chunk grid's footprint exactly, so a tile wholly
     * inside it is never built, and a tile's lattice resolution follows how
     * wide one cell looks from where the player stands.
     */

    // Horizon \\

    public static float resolveHorizonReachBlocks(float heightBlocks) {

        float heightAboveSea = heightBlocks - EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;

        if (heightAboveSea <= 0f)
            return 0f;

        return (float) Math.sqrt(heightAboveSea / EngineSetting.WORLD_CURVATURE_STRENGTH);
    }

    public static float resolveEyeReachBlocks(GridInstance grid) {

        EntityInstance focalEntity = grid.getFocalEntity();
        float eyeHeightBlocks = focalEntity.getWorldPositionStruct().getPosition().y
                + focalEntity.getEyeHeight()
                + EngineSetting.MACRO_HORIZON_EYE_MARGIN_BLOCKS;

        return resolveHorizonReachBlocks(eyeHeightBlocks);
    }

    // Coverage — chunk units, relative to the active chunk's origin corner \\

    public static int resolveGridHalf(int renderDistance) {
        return renderDistance / 2;
    }

    public static float resolveGridRadiusSq(int renderDistance) {

        float gridRadius = renderDistance / 2f;

        return gridRadius * gridRadius;
    }

    public static boolean isCoveredByChunkGrid(
            int relativeX,
            int relativeZ,
            int spanChunks,
            int gridHalf,
            float gridRadiusSq) {

        int lastX = relativeX + spanChunks - 1;
        int lastZ = relativeZ + spanChunks - 1;

        if (relativeX < -gridHalf || relativeZ < -gridHalf || lastX >= gridHalf || lastZ >= gridHalf)
            return false;

        int farX = Math.max(Math.abs(relativeX), Math.abs(lastX));
        int farZ = Math.max(Math.abs(relativeZ), Math.abs(lastZ));

        return farX * farX + farZ * farZ <= gridRadiusSq;
    }

    public static float nearestDistanceChunks(int relativeX, int relativeZ, int spanChunks) {

        float anchorCenter = EngineSetting.MACRO_ANCHOR_CENTER_CHUNKS;
        float deltaX = Math.clamp(anchorCenter, relativeX, relativeX + spanChunks) - anchorCenter;
        float deltaZ = Math.clamp(anchorCenter, relativeZ, relativeZ + spanChunks) - anchorCenter;

        return (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    // Resolution \\

    public static int resolveCellsPerSide(float nearestDistanceBlocks, float tileSizeBlocks) {

        float cellTarget = nearestDistanceBlocks * EngineSetting.MACRO_CELL_ANGLE_RADIANS;
        int cellsPerSide = EngineSetting.MACRO_CELLS_PER_SIDE_MAX;

        while (cellsPerSide > EngineSetting.MACRO_CELLS_PER_SIDE_MIN && tileSizeBlocks / cellsPerSide < cellTarget)
            cellsPerSide >>= 1;

        return cellsPerSide;
    }
}
