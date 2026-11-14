package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.SubCellGridStruct;
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
     * gather() resolves the subchunk being built and the one-cell shell
     * around it through its neighbors once, into the build's SubCellGridStruct,
     * treating unloaded space as null and out-of-world space as air; every
     * sample after that is one grid read. Classifies the columns beside a
     * quad edge for the surface shader, and decides when a block face can
     * mesh at block resolution.
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

    // The octant a whole front cell is sampled at, its lowest corner
    private static final int FRONT_OCTANT = 0;

    // Internal
    private DynamicGeometryManager dynamicGeometryManager;
    private BlockManager blockManager;
    private BiomeManager biomeManager;
    private BlockHandle airBlockHandle;
    private DynamicGeometryType airGeometry;

    // Settings
    private int chunkSize;
    private int worldHeight;
    private int divisions;
    private int divisionShift;
    private int divisionMask;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
        this.divisions = SubBlockUtility.DIVISIONS;
        this.divisionShift = Integer.numberOfTrailingZeros(divisions);
        this.divisionMask = divisions - 1;

        if (Integer.bitCount(divisions) != 1)
            throwException("Sub-block divisions must be a power of two to sample sub-cells by shift: " + divisions);
    }

    @Override
    protected void get() {

        // Internal
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
    }

    @Override
    protected void start() {

        // Sources
        this.blockManager = dynamicGeometryManager.getBlockManager();
        this.biomeManager = dynamicGeometryManager.getBiomeManager();
        this.airBlockHandle = dynamicGeometryManager.getAirBlockHandle();
        this.airGeometry = airBlockHandle.getGeometry();
    }

    // Gather \\

    // The subchunk and its one-cell shell, each neighbor resolved once for the region of cells it holds
    void gather(SubCellGridStruct subCellGrid, ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {
        for (int regionY = -1; regionY <= 1; regionY++)
            for (int regionZ = -1; regionZ <= 1; regionZ++)
                for (int regionX = -1; regionX <= 1; regionX++)
                    gatherRegion(subCellGrid, chunkInstance, subChunkInstance, regionX, regionY, regionZ);
    }

    private void gatherRegion(
            SubCellGridStruct subCellGrid,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int regionX, int regionY, int regionZ) {

        int lowX = toRegionLow(regionX);
        int lowY = toRegionLow(regionY);
        int lowZ = toRegionLow(regionZ);
        int highX = toRegionHigh(regionX);
        int highY = toRegionHigh(regionY);
        int highZ = toRegionHigh(regionZ);

        if (isOutsideWorld(subChunkInstance, lowY)) {
            fillRegion(subCellGrid, lowX, lowY, lowZ, highX, highY, highZ, airBlockHandle);
            return;
        }

        SubChunkInstance cellSubChunkInstance = resolveSubChunk(chunkInstance, subChunkInstance, lowX, lowY, lowZ);

        if (cellSubChunkInstance == null) {
            fillRegion(subCellGrid, lowX, lowY, lowZ, highX, highY, highZ, null);
            return;
        }

        BlockPaletteHandle biomePaletteHandle = cellSubChunkInstance.getBiomePaletteHandle();
        int wrapX = regionX * chunkSize;
        int wrapY = regionY * chunkSize;
        int wrapZ = regionZ * chunkSize;

        for (int cellY = lowY; cellY <= highY; cellY++)
            for (int cellZ = lowZ; cellZ <= highZ; cellZ++)
                for (int cellX = lowX; cellX <= highX; cellX++) {

                    int cellXYZ = Coordinate3Int.pack(cellX - wrapX, cellY - wrapY, cellZ - wrapZ);

                    int index = subCellGrid.toIndex(cellX, cellY, cellZ);

                    subCellGrid.setCell(
                            index,
                            blockManager.getBlockHandleFromBlockID(cellSubChunkInstance.getBlock(cellXYZ)),
                            cellSubChunkInstance.getSubBlockMask(cellXYZ));
                    BiomeHandle biomeHandle = biomeManager.getBiomeHandleFromBiomeID(biomePaletteHandle.getBlock(cellXYZ));

                    subCellGrid.setBiomeColor(index, biomeHandle.getBiomeColor());
                }
    }

    // Cells with no subchunk behind them: air outside the world, nothing in an unloaded neighbor, both whole
    private void fillRegion(
            SubCellGridStruct subCellGrid,
            int lowX, int lowY, int lowZ,
            int highX, int highY, int highZ,
            BlockHandle blockHandle) {

        for (int cellY = lowY; cellY <= highY; cellY++)
            for (int cellZ = lowZ; cellZ <= highZ; cellZ++)
                for (int cellX = lowX; cellX <= highX; cellX++) {

                    int index = subCellGrid.toIndex(cellX, cellY, cellZ);

                    subCellGrid.setCell(index, blockHandle, SubBlockUtility.MASK_FULL);
                    subCellGrid.clearBiomeColor(index);
                }
    }

    // The first and last cell of a region: the shell cell below, the subchunk's own span, or the shell cell above
    private int toRegionLow(int region) {
        return region < 0 ? -1 : region * chunkSize;
    }

    private int toRegionHigh(int region) {
        return region > 0 ? chunkSize : (region + 1) * chunkSize - 1;
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
            cellChunkInstance = chunkInstance.getChunkNeighbors().getBuildNeighborChunk(direction2Vector.index);

            if (cellChunkInstance == null)
                return null;
        }

        if (chunkOffsetY == 0 && cellChunkInstance == chunkInstance)
            return subChunkInstance;

        return cellChunkInstance.getSubChunk((int) subChunkInstance.getCoordinate() + chunkOffsetY);
    }

    // Lookup \\

    // The block filling a sub-cell: null in an unloaded chunk, air where its octant is empty
    private BlockHandle sampleSubCell(SubCellGridStruct subCellGrid, int subX, int subY, int subZ) {

        int index = subCellGrid.toIndex(subX >> divisionShift, subY >> divisionShift, subZ >> divisionShift);
        BlockHandle blockHandle = subCellGrid.getBlockHandle(index);

        if (blockHandle == null)
            return null;

        int octant = SubBlockUtility.getOctant(subX & divisionMask, subY & divisionMask, subZ & divisionMask);

        if (!SubBlockUtility.hasOctant(subCellGrid.getMask(index), octant))
            return airBlockHandle;

        return blockHandle;
    }

    // The geometry filling one octant of a grid cell: null in an unloaded chunk, air's where the octant is empty
    private DynamicGeometryType sampleGeometry(SubCellGridStruct subCellGrid, int index, int octant) {

        DynamicGeometryType geometry = subCellGrid.getGeometry(index);

        if (geometry == null)
            return null;

        if (!SubBlockUtility.hasOctant(subCellGrid.getMask(index), octant))
            return airGeometry;

        return geometry;
    }

    // Whether a grid cell is a whole block rather than a subdivided one
    private boolean isWholeCell(SubCellGridStruct subCellGrid, int index) {
        return !SubBlockUtility.isSubdivided(subCellGrid.getMask(index));
    }

    // Face Presence \\

    boolean hasSubFace(
            SubCellGridStruct subCellGrid,
            int subX, int subY, int subZ,
            BlockHandle blockHandle,
            Direction3Vector faceDirection) {

        if (blockHandle.getGeometry() == DynamicGeometryType.NONE)
            return false;

        int frontX = subX + faceDirection.x;
        int frontY = subY + faceDirection.y;
        int frontZ = subZ + faceDirection.z;
        DynamicGeometryType comparativeGeometry = sampleGeometry(
                subCellGrid,
                subCellGrid.toIndex(frontX >> divisionShift, frontY >> divisionShift, frontZ >> divisionShift),
                SubBlockUtility.getOctant(frontX & divisionMask, frontY & divisionMask, frontZ & divisionMask));

        if (comparativeGeometry == null)
            return false;

        return comparativeGeometry != blockHandle.getGeometry();
    }

    boolean isBlockSimple(
            SubCellGridStruct subCellGrid,
            int cellXYZ,
            Direction3Vector faceDirection,
            BlockHandle blockHandle) {

        int cellIndex = subCellGrid.toIndex(
                Coordinate3Int.unpackX(cellXYZ),
                Coordinate3Int.unpackY(cellXYZ),
                Coordinate3Int.unpackZ(cellXYZ));
        int faceOffset = subCellGrid.toOffset(faceDirection.x, faceDirection.y, faceDirection.z);
        int frontIndex = cellIndex + faceOffset;

        if (!isWholeCell(subCellGrid, frontIndex))
            return false;

        DynamicGeometryType frontGeometry = sampleGeometry(subCellGrid, frontIndex, FRONT_OCTANT);

        if (frontGeometry == null || frontGeometry == blockHandle.getGeometry())
            return true;

        for (Direction3Vector tangent : Direction3Vector.getTangents(faceDirection)) {

            int tangentOffset = subCellGrid.toOffset(tangent.x, tangent.y, tangent.z);

            for (int sign = -1; sign <= 1; sign += 2) {

                int sideIndex = cellIndex + tangentOffset * sign;

                if (!isWholeCell(subCellGrid, sideIndex))
                    return false;

                if (!isWholeCell(subCellGrid, sideIndex + faceOffset))
                    return false;
            }
        }

        return true;
    }

    // Column Classification \\

    int classifyColumn(
            SubCellGridStruct subCellGrid,
            BlockPaletteHandle rotationPaletteHandle,
            int subX, int subY, int subZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle,
            short orientation) {

        int behindX = subX + sideDirection.x;
        int behindY = subY + sideDirection.y;
        int behindZ = subZ + sideDirection.z;

        BlockHandle behindBlockHandle = sampleSubCell(subCellGrid, behindX, behindY, behindZ);
        BlockHandle frontBlockHandle = sampleSubCell(
                subCellGrid,
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
        return Coordinate3Int.pack(subX >> divisionShift, subY >> divisionShift, subZ >> divisionShift);
    }
}
