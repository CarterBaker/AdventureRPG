package application.bootstrap.progressionpipeline.util;

import engine.root.StructPackage;

public class ValueMapStruct extends StructPackage {

    /*
     * An immutable list of registry IDs, each paired with one value — the
     * attributes a stat is fed by, the trades a skill feeds, the stats a trade
     * lends. Data files write it as an object of names to numbers, resolved to
     * IDs once at load, so it is read by index with no lookups or boxing.
     */

    // Values
    public static final ValueMapStruct EMPTY = new ValueMapStruct(new short[0], new float[0]);

    // Entries
    private final short[] ids;
    private final float[] values;

    // Constructor \\

    public ValueMapStruct(short[] ids, float[] values) {

        // Entries
        this.ids = ids;
        this.values = values;
    }

    // Accessible \\

    public int size() {
        return ids.length;
    }

    public boolean isEmpty() {
        return ids.length == 0;
    }

    public short getID(int index) {
        return ids[index];
    }

    public float getValue(int index) {
        return values[index];
    }

    // The value paired with an ID, the fallback when the ID is not listed
    public float find(short id, float fallback) {

        for (int i = 0; i < ids.length; i++)
            if (ids[i] == id)
                return values[i];

        return fallback;
    }

    public float getTotal() {

        float total = 0f;

        for (int i = 0; i < values.length; i++)
            total += values[i];

        return total;
    }
}
