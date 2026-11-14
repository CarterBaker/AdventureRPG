package application.bootstrap.geometrypipeline.quadpacket;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public final class QuadRecordUtility extends EngineUtility {

    /*
     * Packing for the quad record layouts terrain and water draw from (see
     * the Quad Records group in EngineSetting). Stateless, so every builder
     * thread packs through it at once. A record's position word is the one
     * place merges touch, so its packing and a merge's shift share one
     * definition.
     */

    // Settings
    private static final int Z_SHIFT = EngineSetting.QUAD_RECORD_POSITION_Z_SHIFT;
    private static final int Y_SHIFT = EngineSetting.QUAD_RECORD_POSITION_Y_SHIFT;
    private static final int WORD_BITS = EngineSetting.QUAD_RECORD_STREAM_WORD_BITS;
    private static final int SECOND_LOW_SHIFT = Integer.SIZE - WORD_BITS;
    private static final int SECOND_HIGH_SHIFT = WORD_BITS * 2 - Integer.SIZE;
    private static final int THIRD_LOW_SHIFT = Integer.SIZE * 2 - WORD_BITS * 2;
    private static final int THIRD_HIGH_SHIFT = WORD_BITS * 3 - Integer.SIZE * 2;

    // Position \\

    // Sub-block coordinates from a draw's origin, which a merge offset packs the same way
    public static int packPosition(int subX, int subY, int subZ) {
        return subX | (subZ << Z_SHIFT) | (subY << Y_SHIFT);
    }

    // Stream \\

    // Four 24-bit words written as three ints
    public static void addWords(IntArrayList records, int first, int second, int third, int fourth) {
        records.add(first | (second << WORD_BITS));
        records.add((second >>> SECOND_LOW_SHIFT) | (third << SECOND_HIGH_SHIFT));
        records.add((third >>> THIRD_LOW_SHIFT) | (fourth << THIRD_HIGH_SHIFT));
    }

    // Liquid \\

    public static int packLiquid(int face, int sizeA, int sizeB, int level, boolean tidalSurface) {
        return face
                | ((sizeA - 1) << EngineSetting.QUAD_RECORD_LIQUID_SIZE_A_SHIFT)
                | ((sizeB - 1) << EngineSetting.QUAD_RECORD_LIQUID_SIZE_B_SHIFT)
                | (level << EngineSetting.QUAD_RECORD_LIQUID_LEVEL_SHIFT)
                | ((tidalSurface ? 1 : 0) << EngineSetting.QUAD_RECORD_LIQUID_TIDAL_SHIFT);
    }
}
