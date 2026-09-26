package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.extras.Direction3Vector;

class SubCellSampleBranch extends BranchPackage {

    /*
     * Read-only sub-cell sampling shared by the solid geometry branches.
     * Resolves any sub-block coordinate around a subchunk through its
     * neighbors, treats unloaded space as null and out-of-world space as air,
     * classifies the columns beside a quad edge for the surface shader, and
     * decides when a block face can mesh at block resolution.
     */

    // Column Codes — must match surface/includes/Bevel.glsl
    static final int COLUMN_BEHIND_EMPTY = 0;
    static final int COLUMN_BEHIND_SAME = 1;
    static final int COLUMN_BEHIND_NATURAL = 2;
    static final int COLUMN_BEHIND_ARTIFICIAL = 3;
    static final int COLUMN_FRONT_EMPTY = 0;
    static final int COLUMN_FRONT_NATURAL = 1;
    static final int COLUMN_FRONT_ARTIFICIAL = 2;
    static final int COLUMN_FRONT_SHIFT = 2;

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;
    private BlockHandle airBlockHandle;

    // Settings
    private int chunkSize;
    private int worldHeight;
    private int divisions;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
        this.divisions = SubBlockUtility.DIVISIONS;
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void start() {

        // Internal
        this.airBlockHandle = blockManager.getBlockHandleFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Lookup \\

    BlockHandle sampleSubCell(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int subX, int subY, int subZ) {

        int cellX = Math.floorDiv(subX, divisions);
        int cellY = Math.floorDiv(subY, divisions);
        int cellZ = Math.floorDiv(subZ, divisions);

        if (isOutsideWorld(subChunkInstance, cellY))
            return airBlockHandle;

        SubChunkInstance cellSubChunkInstance = resolveSubChunk(chunkInstance, subChunkInstance, cellX, cellY, cellZ);

        if (cellSubChunkInstance == null)
            return null;

        int cellXYZ = wrapCell(cellX, cellY, cellZ);
        int octant = SubBlockUtility.getOctant(
                Math.floorMod(subX, divisions),
                Math.floorMod(subY, divisions),
                Math.floorMod(subZ, divisions));

        if (!SubBlockUtility.hasOctant(cellSubChunkInstance.getSubBlockMask(cellXYZ), octant))
            return airBlockHandle;

        return blockManager.getBlockHandleFromBlockID(cellSubChunkInstance.getBlock(cellXYZ));
    }

    // Whether the cell holding a sub-cell is a whole block rather than a subdivided one
    boolean isWholeCell(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellX, int cellY, int cellZ) {

        if (isOutsideWorld(subChunkInstance, cellY))
            return true;

        SubChunkInstance cellSubChunkInstance = resolveSubChunk(chunkInstance, subChunkInstance, cellX, cellY, cellZ);

        if (cellSubChunkInstance == null)
            return true;

        return !SubBlockUtility.isSubdivided(cellSubChunkInstance.getSubBlockMask(wrapCell(cellX, cellY, cellZ)));
    }

    // The biome of a cell, or the fallback where the cell lies outside the world or in an unloaded chunk
    BiomeHandle sampleBiome(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellX, int cellY, int cellZ,
            BiomeHandle fallbackBiomeHandle) {

        if (isOutsideWorld(subChunkInstance, cellY))
            return fallbackBiomeHandle;

        SubChunkInstance cellSubChunkInstance = resolveSubChunk(chunkInstance, subChunkInstance, cellX, cellY, cellZ);

        if (cellSubChunkInstance == null)
            return fallbackBiomeHandle;

        short biomeID = cellSubChunkInstance.getBiomePaletteHandle().getBlock(wrapCell(cellX, cellY, cellZ));

        return biomeManager.getBiomeHandleFromBiomeID(biomeID);
    }

    private boolean isOutsideWorld(SubChunkInstance subChunkInstance, int cellY) {
        int subChunkCoordinate = (int) subChunkInstance.getCoordinate() + Math.floorDiv(cellY, chunkSize);
        return subChunkCoordinate < 0 || subChunkCoordinate >= worldHeight;
    }

    private SubChunkInstance resolveSubChunk(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellX, int cellY, int cellZ) {

        int chunkOffsetX = Math.floorDiv(cellX, chunkSize);
        int chunkOffsetY = Math.floorDiv(cellY, chunkSize);
        int chunkOffsetZ = Math.floorDiv(cellZ, chunkSize);

        ChunkInstance cellChunkInstance = chunkInstance;

        if (chunkOffsetX != 0 || chunkOffsetZ != 0) {

            Direction2Vector direction2Vector = Direction2Vector.getDirection(chunkOffsetX, chunkOffsetZ);
            cellChunkInstance = chunkInstance.getChunkNeighbors().getNeighborChunk(direction2Vector.index);

            if (cellChunkInstance == null)
                return null;
        }

        if (chunkOffsetY == 0 && cellChunkInstance == chunkInstance)
            return subChunkInstance;

        return cellChunkInstance.getSubChunk((int) subChunkInstance.getCoordinate() + chunkOffsetY);
    }

    private int wrapCell(int cellX, int cellY, int cellZ) {
        return Coordinate3Int.pack(
                Math.floorMod(cellX, chunkSize),
                Math.floorMod(cellY, chunkSize),
                Math.floorMod(cellZ, chunkSize));
    }

    // Face Presence \\

    boolean hasSubFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int subX, int subY, int subZ,
            BlockHandle blockHandle,
            Direction3Vector faceDirection) {

        if (blockHandle.getGeometry() == DynamicGeometryType.NONE)
            return false;

        BlockHandle comparativeBlockHandle = sampleSubCell(
                chunkInstance,
                subChunkInstance,
                subX + faceDirection.x,
                subY + faceDirection.y,
                subZ + faceDirection.z);

        if (comparativeBlockHandle == null)
            return false;

        return comparativeBlockHandle.getGeometry() != blockHandle.getGeometry();
    }

    boolean isBlockSimple(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            BlockHandle blockHandle) {

        int cellX = Coordinate3Int.unpackX(cellXYZ);
        int cellY = Coordinate3Int.unpackY(cellXYZ);
        int cellZ = Coordinate3Int.unpackZ(cellXYZ);

        int frontX = cellX + faceDirection.x;
        int frontY = cellY + faceDirection.y;
        int frontZ = cellZ + faceDirection.z;

        if (!isWholeCell(chunkInstance, subChunkInstance, frontX, frontY, frontZ))
            return false;

        BlockHandle frontBlockHandle = sampleSubCell(
                chunkInstance,
                subChunkInstance,
                frontX * divisions,
                frontY * divisions,
                frontZ * divisions);

        if (frontBlockHandle == null || frontBlockHandle.getGeometry() == blockHandle.getGeometry())
            return true;

        for (Direction3Vector tangent : Direction3Vector.getTangents(faceDirection)) {
            for (int sign = -1; sign <= 1; sign += 2) {

                int sideX = cellX + tangent.x * sign;
                int sideY = cellY + tangent.y * sign;
                int sideZ = cellZ + tangent.z * sign;

                if (!isWholeCell(chunkInstance, subChunkInstance, sideX, sideY, sideZ))
                    return false;

                if (!isWholeCell(
                        chunkInstance,
                        subChunkInstance,
                        sideX + faceDirection.x,
                        sideY + faceDirection.y,
                        sideZ + faceDirection.z))
                    return false;
            }
        }

        return true;
    }

    // Column Classification \\

    int classifyColumn(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            int subX, int subY, int subZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle,
            short orientation) {

        int behindX = subX + sideDirection.x;
        int behindY = subY + sideDirection.y;
        int behindZ = subZ + sideDirection.z;

        BlockHandle behindBlockHandle = sampleSubCell(chunkInstance, subChunkInstance, behindX, behindY, behindZ);
        BlockHandle frontBlockHandle = sampleSubCell(
                chunkInstance,
                subChunkInstance,
                behindX + faceDirection.x,
                behindY + faceDirection.y,
                behindZ + faceDirection.z);

        if (behindBlockHandle == null || frontBlockHandle == null)
            return COLUMN_BEHIND_ARTIFICIAL | (COLUMN_FRONT_EMPTY << COLUMN_FRONT_SHIFT);

        int behindState = COLUMN_BEHIND_EMPTY;

        if (behindBlockHandle.getGeometry() == blockHandle.getGeometry()) {

            if (isSameSurface(rotationPaletteHandle, behindX, behindY, behindZ,
                    behindBlockHandle, blockHandle, orientation))
                behindState = COLUMN_BEHIND_SAME;
            else
                behindState = behindBlockHandle.isNatural() ? COLUMN_BEHIND_NATURAL : COLUMN_BEHIND_ARTIFICIAL;
        }

        int frontState = COLUMN_FRONT_EMPTY;

        if (frontBlockHandle.getGeometry() == blockHandle.getGeometry())
            frontState = frontBlockHandle.isNatural() ? COLUMN_FRONT_NATURAL : COLUMN_FRONT_ARTIFICIAL;

        return behindState | (frontState << COLUMN_FRONT_SHIFT);
    }

    private boolean isSameSurface(
            BlockPaletteHandle rotationPaletteHandle,
            int subX, int subY, int subZ,
            BlockHandle cellBlockHandle,
            BlockHandle blockHandle,
            short orientation) {

        if (cellBlockHandle != blockHandle)
            return false;

        if (!requiresOrientationMatch(blockHandle))
            return true;

        return isInsideSubChunk(subX, subY, subZ)
                && rotationPaletteHandle.getBlock(toCellXYZ(subX, subY, subZ)) == orientation;
    }

    // Utility \\

    boolean requiresOrientationMatch(BlockHandle blockHandle) {
        BlockRotationType rotationType = blockHandle.getRotationType();
        return rotationType != BlockRotationType.NONE && rotationType != BlockRotationType.NATURAL_FULL;
    }

    boolean isInsideSubChunk(int subX, int subY, int subZ) {

        int subChunkSize = chunkSize * divisions;

        return subX >= 0 && subX < subChunkSize &&
                subY >= 0 && subY < subChunkSize &&
                subZ >= 0 && subZ < subChunkSize;
    }

    // The packed cell holding a sub-cell that lies inside the subchunk being built
    int toCellXYZ(int subX, int subY, int subZ) {
        return Coordinate3Int.pack(
                Math.floorDiv(subX, divisions),
                Math.floorDiv(subY, divisions),
                Math.floorDiv(subZ, divisions));
    }
}
