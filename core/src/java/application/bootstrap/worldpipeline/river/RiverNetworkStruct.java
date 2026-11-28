package application.bootstrap.worldpipeline.river;

import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class RiverNetworkStruct extends StructPackage {

    /*
     * One world's drainage, immutable once built: per drainage cell the
     * neighbour it drains into, the strength of the river it carries there as
     * steps of doubled flow past the river threshold, none where too little
     * water gathers to make one, and the level the river's surface keeps at
     * the cell's heart, never rising downstream. A near bit marks every cell
     * close enough to a river for one to reach it, so a query far from water
     * costs a single lookup. Each cell's heart is jittered by a hash so rivers
     * never run on the grid; a sink keeps its centre. Cells wrap both ways as
     * a torus does; a globe's cells are whole cells of every cube face, laid
     * face after face, a cell stepped to past a face's edge folded onto the
     * cell it truly is, each cell's directions read in its own face's axes.
     */

    public static final byte DIRECTION_NONE = -1;
    public static final int INDEX_NONE = EngineSetting.INDEX_NOT_FOUND;
    public static final int NEAR_RADIUS_CELLS = 2;

    private static final int[] DIRECTION_X = { 1, 1, 0, -1, -1, -1, 0, 1 };
    private static final int[] DIRECTION_Z = { 0, 1, 1, 1, 0, -1, -1, -1 };

    // World
    private final WorldHandle worldHandle;
    private final long seed;
    private final int biomeRevision;

    // Grid — a globe's cells per face side, none for a torus
    private final int cellCountX;
    private final int cellCountZ;
    private final int cellsPerFace;
    private final double cellSizeBlocks;

    // Cells
    private final byte[] downstream;
    private final byte[] strength;
    private final short[] levelBlocks;
    private final long[] nearWords;

    // Constructor \\

    public RiverNetworkStruct(
            WorldHandle worldHandle,
            int biomeRevision,
            int cellCountX,
            int cellCountZ,
            int cellsPerFace,
            double cellSizeBlocks,
            byte[] downstream,
            byte[] strength,
            short[] levelBlocks,
            long[] nearWords) {

        // World
        this.worldHandle = worldHandle;
        this.seed = worldHandle.getSeed();
        this.biomeRevision = biomeRevision;

        // Grid
        this.cellCountX = cellCountX;
        this.cellCountZ = cellCountZ;
        this.cellsPerFace = cellsPerFace;
        this.cellSizeBlocks = cellSizeBlocks;

        // Cells
        this.downstream = downstream;
        this.strength = strength;
        this.levelBlocks = levelBlocks;
        this.nearWords = nearWords;
    }

    // Grid \\

    public static int getDirectionX(int direction) {
        return DIRECTION_X[direction];
    }

    public static int getDirectionZ(int direction) {
        return DIRECTION_Z[direction];
    }

    public static int getDirectionCount() {
        return DIRECTION_X.length;
    }

    // The index of the cell a pair of cell coordinates names: wrapped on a torus, on a globe folded from the face
    // the x is counted from, the turns from that face's axes into the cell's left in scratch
    public static int toIndex(
            int cellCountX,
            int cellCountZ,
            int cellsPerFace,
            int cellX,
            int cellZ,
            GlobeFrameStruct scratch) {

        if (cellsPerFace == 0)
            return Math.floorMod(cellZ, cellCountZ) * cellCountX + Math.floorMod(cellX, cellCountX);

        int face = GlobeUtility.faceOf(cellX, cellsPerFace);

        GlobeUtility.foldPoint(
                cellsPerFace, face,
                2.0 * (cellX - GlobeUtility.faceOriginX(face, cellsPerFace)) + 1.0 - cellsPerFace,
                2.0 * cellZ + 1.0 - cellsPerFace,
                0.0, 0.0, 0, scratch);

        int u = (int) Math.round((scratch.getFoldA() - 1.0 + cellsPerFace) * 0.5);
        int v = (int) Math.round((scratch.getFoldB() - 1.0 + cellsPerFace) * 0.5);

        return (scratch.getFace() * cellsPerFace + v) * cellsPerFace + u;
    }

    // A cell's coordinates: a globe cell's x counted from its own face's origin
    public static int toCellX(int cellCountX, int cellsPerFace, int index) {

        if (cellsPerFace == 0)
            return index % cellCountX;

        int perFace = cellsPerFace * cellsPerFace;

        return (int) GlobeUtility.faceOriginX(index / perFace, cellsPerFace) + index % perFace % cellsPerFace;
    }

    public static int toCellZ(int cellCountX, int cellsPerFace, int index) {

        if (cellsPerFace == 0)
            return index / cellCountX;

        return index % (cellsPerFace * cellsPerFace) / cellsPerFace;
    }

    public int toIndex(int cellX, int cellZ, GlobeFrameStruct scratch) {
        return toIndex(cellCountX, cellCountZ, cellsPerFace, cellX, cellZ, scratch);
    }

    public int getCellX(int index) {
        return toCellX(cellCountX, cellsPerFace, index);
    }

    public int getCellZ(int index) {
        return toCellZ(cellCountX, cellsPerFace, index);
    }

    // The cell a world position lies in, unwrapped on a globe so its index folds it
    public int toCellX(double worldX) {

        int cellX = (int) Math.floor(worldX / cellSizeBlocks);

        return cellsPerFace == 0 ? Math.floorMod(cellX, cellCountX) : cellX;
    }

    public int toCellZ(double worldZ) {

        int cellZ = (int) Math.floor(worldZ / cellSizeBlocks);

        return cellsPerFace == 0 ? Math.floorMod(cellZ, cellCountZ) : cellZ;
    }

    // Cells \\

    public int getDownstreamIndex(int index, GlobeFrameStruct scratch) {

        int direction = downstream[index];

        if (direction == DIRECTION_NONE)
            return INDEX_NONE;

        return toIndex(getCellX(index) + DIRECTION_X[direction], getCellZ(index) + DIRECTION_Z[direction], scratch);
    }

    public boolean isSink(int index) {
        return downstream[index] == DIRECTION_NONE;
    }

    // A cell that carries a river on to the cell it drains into
    public boolean isRiver(int index) {
        return strength[index] != 0 && downstream[index] != DIRECTION_NONE;
    }

    public int getStrength(int index) {
        return strength[index] & 0xFF;
    }

    public int getLevelBlocks(int index) {
        return levelBlocks[index];
    }

    public boolean isNear(int index) {
        return (nearWords[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    // The upstream neighbour carrying the strongest river into a cell, INDEX_NONE where no river flows in
    public int findMainUpstream(int index, GlobeFrameStruct scratch) {

        int cellX = getCellX(index);
        int cellZ = getCellZ(index);
        int best = INDEX_NONE;
        int bestStrength = 0;

        for (int direction = 0; direction < DIRECTION_X.length; direction++) {

            int neighbour = toIndex(cellX + DIRECTION_X[direction], cellZ + DIRECTION_Z[direction], scratch);

            if (!isRiver(neighbour) || getDownstreamIndex(neighbour, scratch) != index)
                continue;

            int neighbourStrength = getStrength(neighbour);

            if (neighbourStrength > bestStrength) {
                best = neighbour;
                bestStrength = neighbourStrength;
            }
        }

        return best;
    }

    // Node — unwrapped blocks of the cell's own place on the grid \\

    public double getNodeX(int index) {
        return (getCellX(index) + 0.5 + resolveJitter(index, 0L)) * cellSizeBlocks;
    }

    public double getNodeZ(int index) {
        return (getCellZ(index) + 0.5 + resolveJitter(index, EngineSetting.HASH_FINALIZER_MULTIPLIER_1))
                * cellSizeBlocks;
    }

    private double resolveJitter(int index, long axisSalt) {

        if (isSink(index))
            return 0.0;

        long hash = BiomeFieldUtility.hashCell(seed ^ EngineSetting.RIVER_NODE_SALT ^ axisSalt,
                getCellX(index), getCellZ(index));

        return (BiomeFieldUtility.hash01(hash) * 2.0 - 1.0) * EngineSetting.RIVER_NODE_JITTER;
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public long getSeed() {
        return seed;
    }

    public int getBiomeRevision() {
        return biomeRevision;
    }

    public int getCellCountX() {
        return cellCountX;
    }

    public int getCellCountZ() {
        return cellCountZ;
    }

    public int getCellCount() {
        return cellCountX * cellCountZ;
    }

    public double getCellSizeBlocks() {
        return cellSizeBlocks;
    }
}
