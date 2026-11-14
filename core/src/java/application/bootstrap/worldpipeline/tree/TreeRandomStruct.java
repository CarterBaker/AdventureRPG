package application.bootstrap.worldpipeline.tree;

import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class TreeRandomStruct extends StructPackage {

    /*
     * The one stream of random numbers a tree grows from. Every draw is a pure
     * hash of its seed and how many draws came before, so the same seed always
     * grows the same tree on any thread, in any order the world asks for it.
     */

    // State
    private long state;

    // Constructor \\

    public TreeRandomStruct(long seed) {
        this.state = seed;
    }

    // Draws \\

    // A value from 0 up to 1
    public float next() {

        state += EngineSetting.TERRAIN_OCTAVE_HASH_SALT;

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
}
