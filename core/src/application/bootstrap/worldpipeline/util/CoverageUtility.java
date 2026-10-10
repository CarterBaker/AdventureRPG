package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class CoverageUtility extends EngineUtility {

    /*
     * Packing for coverage, the covering a cell's faces carry and how far it
     * has grown over them. A cell's coverage is one short in its subchunk's
     * coverage palette, the covering's ID above the level's bits, and NONE —
     * zero — is bare, since covering ID zero is the registry's reserved
     * sentinel. A level of zero is bare too, so every bare cell packs to the
     * same NONE. The vertex word adds whether the face shows the covering's
     * side tiles, and is exact in a float. Every system that reads or writes
     * coverage resolves it through here.
     */

    // Settings
    public static final short NONE = EngineSetting.COVERAGE_NONE;
    public static final int LEVEL_MAX = EngineSetting.COVERAGE_LEVEL_MAX;

    // Pack \\

    public static short pack(int coveringID, int level) {

        int clampedLevel = Math.min(Math.max(level, 0), LEVEL_MAX);

        if (coveringID == EngineSetting.REGISTRY_RESERVED_ID || clampedLevel == 0)
            return NONE;

        return (short) ((coveringID << EngineSetting.COVERAGE_LEVEL_BITS) | clampedLevel);
    }

    // The same covering grown or receded by a number of levels, bare once it falls to zero
    public static short addLevels(short coverage, int levels) {
        return pack(getCoveringID(coverage), getLevel(coverage) + levels);
    }

    // Unpack \\

    public static int getCoveringID(short coverage) {
        return (coverage & 0xFFFF) >>> EngineSetting.COVERAGE_LEVEL_BITS;
    }

    public static int getLevel(short coverage) {
        return coverage & LEVEL_MAX;
    }

    public static boolean isCovered(short coverage) {
        return coverage != NONE;
    }

    public static boolean isFull(short coverage) {
        return getLevel(coverage) == LEVEL_MAX;
    }

    // Share of the full level the coverage has reached, 0 bare to 1 fully grown
    public static float getShare(short coverage) {
        return getLevel(coverage) / (float) LEVEL_MAX;
    }

    // Vertex \\

    public static float toVertexWord(short coverage, boolean side) {

        if (!isCovered(coverage))
            return NONE;

        return (getCoveringID(coverage) << EngineSetting.COVERAGE_VERTEX_ID_SHIFT)
                | (side ? EngineSetting.COVERAGE_VERTEX_SIDE_BIT : 0)
                | getLevel(coverage);
    }
}
