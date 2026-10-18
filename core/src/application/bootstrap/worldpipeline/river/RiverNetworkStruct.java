package application.bootstrap.worldpipeline.river;

import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
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
     * the world does.
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

    // Grid
    private final int cellCountX;
    private final int cellCountZ;
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

    public int toIndex(int cellX, int cellZ) {
        return Math.floorMod(cellZ, cellCountZ) * cellCountX + Math.floorMod(cellX, cellCountX);
    }

    public int getCellX(int index) {
        return index % cellCountX;
    }

    public int getCellZ(int index) {
        return index / cellCountX;
    }

    public int toCellX(double worldX) {
        return Math.floorMod((int) Math.floor(worldX / cellSizeBlocks), cellCountX);
    }

    public int toCellZ(double worldZ) {
        return Math.floorMod((int) Math.floor(worldZ / cellSizeBlocks), cellCountZ);
    }

    // Cells \\

    public int getDownstreamIndex(int index) {

        int direction = downstream[index];

        if (direction == DIRECTION_NONE)
            return INDEX_NONE;

        return toIndex(getCellX(index) + DIRECTION_X[direction], getCellZ(index) + DIRECTION_Z[direction]);
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
    public int findMainUpstream(int index) {

        int cellX = getCellX(index);
        int cellZ = getCellZ(index);
        int best = INDEX_NONE;
        int bestStrength = 0;

        for (int direction = 0; direction < DIRECTION_X.length; direction++) {

            int neighbour = toIndex(cellX + DIRECTION_X[direction], cellZ + DIRECTION_Z[direction]);

            if (!isRiver(neighbour) || getDownstreamIndex(neighbour) != index)
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
