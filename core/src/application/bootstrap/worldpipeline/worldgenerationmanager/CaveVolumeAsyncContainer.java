package application.bootstrap.worldpipeline.worldgenerationmanager;

import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import engine.root.AsyncContainerPackage;
import engine.root.EngineSetting;

public class CaveVolumeAsyncContainer extends AsyncContainerPackage {

    /*
     * Thread-local scratch holding one subchunk's caves, resolved a margin
     * above and below it so stalactites and stalagmites can find the rock
     * they hang from: every cell's state, the octants still solid in a cell
     * the cave cuts through, and its density, how far it lies inside the
     * hollow or the rock. The chunk frame reads cave density over the
     * subchunk; the column frame reads it down one column at a time, for the
     * giant formations standing in the tall chambers it finds, which are
     * kept here too.
     */

    static final int MARGIN = EngineSetting.SPELEOTHEM_MAX_LENGTH_BLOCKS + 1;
    static final int HEIGHT = EngineSetting.CHUNK_SIZE + 2 * MARGIN;
    static final int CELL_COUNT = EngineSetting.CHUNK_SIZE * EngineSetting.CHUNK_SIZE * HEIGHT;

    static final int LATTICE_ROWS = (HEIGHT + TerrainCarveUtility.LATTICE_STEP - 1) / TerrainCarveUtility.LATTICE_STEP
            + 2;

    // The furthest a giant formation's scan reaches beyond the subchunk
    static final int SCAN_MARGIN = EngineSetting.SPELEOTHEM_GIANT_MAX_SPAN_BLOCKS
            + EngineSetting.SPELEOTHEM_GIANT_FLARE_BLOCKS + 1;
    static final int SCAN_HEIGHT = EngineSetting.CHUNK_SIZE + 2 * SCAN_MARGIN;
    static final int SCAN_LATTICE_ROWS = (SCAN_HEIGHT + TerrainCarveUtility.LATTICE_STEP - 1)
            / TerrainCarveUtility.LATTICE_STEP + 2;

    static final int FORMATION_MAX = EngineSetting.SPELEOTHEM_GIANT_SITES_PER_CHUNK
            * (SCAN_HEIGHT / (EngineSetting.SPELEOTHEM_GIANT_MIN_SPAN_BLOCKS + 1) + 1);

    static final byte STATE_SOLID = 0;
    static final byte STATE_PARTIAL = 1;
    static final byte STATE_HOLLOW = 2;
    static final byte STATE_LAKE = 3;
    static final byte STATE_SEA = 4;
    static final byte STATE_OPEN = 5;

    // Volume
    int originY;
    byte[] states;
    byte[] solidMasks;
    float[] densities;

    // Frames
    CaveFrameStruct chunkFrame;
    CaveFrameStruct columnFrame;
    float[] columnLattice;
    boolean[] scanHollow;

    // Formations
    CaveFormationStruct[] formations;
    int formationCount;

    // Result — the block the last speleothem read was made of
    short speleothemBlockID;

    // Result — the coverage the last rock read laid over its cell
    short cellCoverage;

    @Override
    protected void create() {

        this.states = new byte[CELL_COUNT];
        this.solidMasks = new byte[CELL_COUNT];
        this.densities = new float[CELL_COUNT];

        this.chunkFrame = new CaveFrameStruct();
        this.columnFrame = new CaveFrameStruct();
        this.columnLattice = new float[TerrainCarveUtility.COLUMN_LATTICE_SIDE
                * TerrainCarveUtility.COLUMN_LATTICE_SIDE * SCAN_LATTICE_ROWS * TerrainCarveUtility.CAVE_CHANNELS];
        this.scanHollow = new boolean[SCAN_HEIGHT];

        this.formations = new CaveFormationStruct[FORMATION_MAX];

        for (int i = 0; i < FORMATION_MAX; i++)
            this.formations[i] = new CaveFormationStruct();
    }

    // Cells \\

    int index(int localX, int worldY, int localZ) {
        return ((worldY - originY) * EngineSetting.CHUNK_SIZE + localZ) * EngineSetting.CHUNK_SIZE + localX;
    }

    boolean contains(int worldY) {
        return worldY >= originY && worldY < originY + HEIGHT;
    }

    byte getState(int localX, int worldY, int localZ) {
        return states[index(localX, worldY, localZ)];
    }

    // The octants a cell keeps solid
    int getSolidMask(int localX, int worldY, int localZ) {

        int cell = index(localX, worldY, localZ);

        return switch (states[cell]) {
            case STATE_SOLID -> EngineSetting.SUB_BLOCK_MASK_FULL;
            case STATE_PARTIAL -> solidMasks[cell] & EngineSetting.SUB_BLOCK_MASK_FULL;
            default -> EngineSetting.SUB_BLOCK_MASK_EMPTY;
        };
    }

    float getDensity(int localX, int worldY, int localZ) {
        return densities[index(localX, worldY, localZ)];
    }

    // Open cave air, dry
    boolean isHollow(int localX, int worldY, int localZ) {
        return contains(worldY) && getState(localX, worldY, localZ) == STATE_HOLLOW;
    }

    // Cave air or the water standing in it
    boolean isCavity(int localX, int worldY, int localZ) {

        if (!contains(worldY))
            return false;

        byte state = getState(localX, worldY, localZ);

        return state == STATE_HOLLOW || state == STATE_LAKE || state == STATE_SEA;
    }

    boolean isSolid(int localX, int worldY, int localZ) {
        return contains(worldY) && getState(localX, worldY, localZ) == STATE_SOLID;
    }

    // Formations \\

    CaveFormationStruct addFormation() {

        if (formationCount >= formations.length)
            return null;

        return formations[formationCount++];
    }
}
