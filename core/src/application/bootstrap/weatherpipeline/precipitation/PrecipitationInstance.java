package application.bootstrap.weatherpipeline.precipitation;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector4;
import engine.util.mathematics.vectors.Vector4Int;

public class PrecipitationInstance extends InstancePackage {

    /*
     * One grid's precipitation: intensity, snow share, drift wind, and a ring
     * buffer of column tops around the focal entity so rain and snow stop at
     * the first block. Heights pack exactly as the PrecipitationData UBO reads
     * them, and unknown columns read as sheltered.
     */

    // Map
    private int[] columnKeyX;
    private int[] columnKeyZ;
    private Vector4Int[] packedColumns;
    private int refreshCursor;

    // Window
    private Vector4Int window;

    // State
    private Vector4 state;

    // Constructor \\

    public void constructor() {

        int mapSize = EngineSetting.PRECIPITATION_MAP_SIZE;
        int columnCount = mapSize * mapSize;
        int vectorCount = columnCount
                / (EngineSetting.PRECIPITATION_HEIGHTS_PER_INT * EngineSetting.PRECIPITATION_INTS_PER_VECTOR);

        // Map
        this.columnKeyX = new int[columnCount];
        this.columnKeyZ = new int[columnCount];
        this.packedColumns = new Vector4Int[vectorCount];

        for (int i = 0; i < vectorCount; i++)
            packedColumns[i] = new Vector4Int();

        for (int slot = 0; slot < columnCount; slot++)
            clearColumn(slot);

        this.refreshCursor = 0;

        // Window
        this.window = new Vector4Int();

        // State
        this.state = new Vector4();
    }

    // Columns \\

    public boolean isColumnCurrent(int slot, int columnX, int columnZ) {
        return columnKeyX[slot] == columnX && columnKeyZ[slot] == columnZ;
    }

    public void setColumn(int slot, int columnX, int columnZ, int columnTop) {

        columnKeyX[slot] = columnX;
        columnKeyZ[slot] = columnZ;
        writeHeight(slot, Math.min(columnTop, EngineSetting.PRECIPITATION_UNKNOWN_HEIGHT));
    }

    public void clearColumn(int slot) {

        columnKeyX[slot] = EngineSetting.PRECIPITATION_UNASSIGNED_COLUMN;
        columnKeyZ[slot] = EngineSetting.PRECIPITATION_UNASSIGNED_COLUMN;
        writeHeight(slot, EngineSetting.PRECIPITATION_UNKNOWN_HEIGHT);
    }

    public int advanceRefreshCursor() {

        int slot = refreshCursor;
        refreshCursor = (refreshCursor + 1) % columnKeyX.length;

        return slot;
    }

    // Packing \\

    private void writeHeight(int slot, int height) {

        int intIndex = slot / EngineSetting.PRECIPITATION_HEIGHTS_PER_INT;
        int shift = (slot % EngineSetting.PRECIPITATION_HEIGHTS_PER_INT) * EngineSetting.PRECIPITATION_HEIGHT_BITS;
        Vector4Int vector = packedColumns[intIndex / EngineSetting.PRECIPITATION_INTS_PER_VECTOR];
        int component = intIndex % EngineSetting.PRECIPITATION_INTS_PER_VECTOR;

        int packed = readComponent(vector, component);
        packed &= ~(EngineSetting.PRECIPITATION_HEIGHT_MASK << shift);
        packed |= (height & EngineSetting.PRECIPITATION_HEIGHT_MASK) << shift;

        writeComponent(vector, component, packed);
    }

    private int readComponent(Vector4Int vector, int component) {
        return switch (component) {
            case 0 -> vector.x;
            case 1 -> vector.y;
            case 2 -> vector.z;
            default -> vector.w;
        };
    }

    private void writeComponent(Vector4Int vector, int component, int value) {
        switch (component) {
            case 0 -> vector.x = value;
            case 1 -> vector.y = value;
            case 2 -> vector.z = value;
            default -> vector.w = value;
        }
    }

    // Window \\

    public void setWindow(int windowX, int windowZ, int frameX, int frameZ) {
        window.set(windowX, windowZ, frameX, frameZ);
    }

    // State \\

    public void setState(float intensity, float snow, float windX, float windZ) {
        state.set(intensity, snow, windX, windZ);
    }

    public boolean isFalling() {
        return state.x > 0f;
    }

    // Accessible \\

    public Vector4Int[] getPackedColumns() {
        return packedColumns;
    }

    public Vector4Int getWindow() {
        return window;
    }

    public Vector4 getState() {
        return state;
    }
}
