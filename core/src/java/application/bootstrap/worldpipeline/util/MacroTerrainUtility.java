package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class MacroTerrainUtility extends EngineUtility {

    /*
     * Stateless geometry of distant macro terrain against the chunk grid. The
     * world is flat, so the ring reaches a fixed distance in every direction.
     * Coverage mirrors the chunk grid's footprint inset by the streaming
     * margin, the rim band where chunks are still streaming in or never mesh
     * for want of a neighbour, so a tile is left unbuilt only where chunks
     * settle for certain, and a tile's lattice resolution follows how wide one
     * cell looks from where the player stands.
     */

    // Coverage — chunk units, relative to the active chunk's origin corner \\

    public static int resolveSettledHalf(int renderDistance) {
        return Math.max(renderDistance / 2 - EngineSetting.MACRO_STREAMING_MARGIN_CHUNKS, 0);
    }

    public static float resolveSettledRadiusSq(int renderDistance) {

        float settledRadius = Math.max(renderDistance / 2f - EngineSetting.MACRO_STREAMING_MARGIN_CHUNKS, 0f);

        return settledRadius * settledRadius;
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
