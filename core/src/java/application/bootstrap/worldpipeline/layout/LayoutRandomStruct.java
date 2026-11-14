package application.bootstrap.worldpipeline.layout;

import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class LayoutRandomStruct extends StructPackage {

    /*
     * The one stream of random numbers a layout is planned from. Every draw is
     * a pure hash of its seed and how many draws came before, so the same seed
     * always plans the same layout on any thread, in any order the world asks
     * for it.
     */

    // State
    private long state;

    // Constructor \\

    public LayoutRandomStruct(long seed) {
        this.state = seed;
    }

    // Draws \\

    // A value from 0 up to 1
    public float next() {

        state += EngineSetting.LAYOUT_RANDOM_STEP_SALT;

        return BiomeFieldUtility.hash01(state);
    }

    public float nextRange(float min, float max) {
        return min + (max - min) * next();
    }

    // A whole number from min to max, both included
    public int nextInt(int min, int max) {
        return Math.min(max, min + (int) (next() * (max - min + 1)));
    }

    // A value from -1 up to 1
    public float nextSigned() {
        return next() * 2f - 1f;
    }

    // A fresh seed for a sub-plan, such as one road's bends, drawn from the stream
    public long nextSeed() {

        state += EngineSetting.LAYOUT_RANDOM_STEP_SALT;

        return BiomeFieldUtility.hashCell(state, 0, 0);
    }
}
