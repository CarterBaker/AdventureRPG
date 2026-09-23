package application.bootstrap.worldpipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TideUtility extends EngineUtility {

    /*
     * Block-space arithmetic for the tidal ocean surface. A surface is carried
     * as an integer count of liquid levels measured from world Y 0 — block Y
     * times LIQUID_LEVEL_MAX plus the fill of the cell the surface sits in —
     * quantized to OCEAN_TIDE_LEVEL_STEP, so world generation and the tide
     * pass resolve the same cell to the same level for the same tide. The
     * tide band is every cell whose fill can differ between the lowest and
     * the highest tide: cells beneath it are always full, cells above it are
     * never reached.
     */

    public static final int BASE_SURFACE_LEVELS = toSurfaceLevels(EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1.0);

    public static final int MIN_SURFACE_LEVELS = toSurfaceLevels(
            EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1.0 - EngineSetting.OCEAN_TIDE_AMPLITUDE_BLOCKS);

    public static final int MAX_SURFACE_LEVELS = toSurfaceLevels(
            EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS + 1.0 + EngineSetting.OCEAN_TIDE_AMPLITUDE_BLOCKS);

    public static final int BAND_MIN_Y = MIN_SURFACE_LEVELS / EngineSetting.LIQUID_LEVEL_MAX;
    public static final int BAND_MAX_Y = getTopWaterY(MAX_SURFACE_LEVELS);

    private TideUtility() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Surface \\

    public static int toSurfaceLevels(double surfaceHeightBlocks) {

        int step = EngineSetting.OCEAN_TIDE_LEVEL_STEP;

        return (int) Math.round(surfaceHeightBlocks * EngineSetting.LIQUID_LEVEL_MAX / step) * step;
    }

    public static int getTopWaterY(int surfaceLevels) {
        return (surfaceLevels - 1) / EngineSetting.LIQUID_LEVEL_MAX;
    }

    // Cell \\

    public static short getFillLevel(int surfaceLevels, int worldY) {

        int fill = surfaceLevels - worldY * EngineSetting.LIQUID_LEVEL_MAX;

        if (fill <= EngineSetting.LIQUID_LEVEL_EMPTY)
            return EngineSetting.LIQUID_LEVEL_EMPTY;

        if (fill >= EngineSetting.LIQUID_LEVEL_MAX)
            return EngineSetting.LIQUID_LEVEL_MAX;

        return (short) fill;
    }
}
