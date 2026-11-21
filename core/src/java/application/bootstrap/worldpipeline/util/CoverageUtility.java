package application.bootstrap.worldpipeline.util;

import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
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
     * side tiles, and is exact in a float. A tile's reveal shares travel to
     * the surface shader a byte per level, four levels to an int, packed for
     * coverings and contents alike here. Every system that reads or writes
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

    // Vertex \\

    public static int toVertexWord(short coverage, boolean side) {

        if (!isCovered(coverage))
            return NONE;

        return (getCoveringID(coverage) << EngineSetting.COVERAGE_VERTEX_ID_SHIFT)
                | (side ? EngineSetting.COVERAGE_VERTEX_SIDE_BIT : 0)
                | getLevel(coverage);
    }

    // Reveal \\

    // One component of a shares row — four consecutive levels, a byte each, the lowest level in the low byte
    public static int packRevealShares(TextureRevealStruct reveal, int component) {

        int word = 0;
        int firstLevel = component * EngineSetting.COVERAGE_SHARES_PER_WORD;

        for (int i = 0; i < EngineSetting.COVERAGE_SHARES_PER_WORD; i++) {

            int level = firstLevel + i;

            if (level > LEVEL_MAX)
                break;

            int share = Math.round(reveal.getLevelShare(level) * EngineSetting.COVERAGE_SHARE_MAX);
            word |= share << (i * EngineSetting.COVERAGE_SHARE_BITS);
        }

        return word;
    }
}
