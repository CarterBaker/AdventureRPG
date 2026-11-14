package application.bootstrap.worldpipeline.util;

import java.io.File;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.io.FileUtility;

public final class WorldElevationUtility extends EngineUtility {

    /*
     * Reads a world's height layer, the grey image beside its world image that
     * raises and lowers the ground the biome field shapes. A pixel's level is
     * its red channel: neutral is no elevation, the darkest level -1 and the
     * brightest 1. Positions read the layer through a cubic B-spline over the
     * four by four pixels around them, wrapping both ways as the world does,
     * so painted levels become broad, smooth slopes rather than steps. The
     * layer's file is the world image's name with the height suffix, in the
     * same folder and format.
     */

    private static final int SPLINE_TAPS = 4;

    // File \\

    public static File resolveHeightLayerFile(File worldFile) {
        return new File(
                worldFile.getParentFile(),
                FileUtility.getFileName(worldFile) + EngineSetting.WORLD_HEIGHT_LAYER_SUFFIX
                        + "." + FileUtility.getExtension(worldFile));
    }

    public static boolean isHeightLayerFile(File file) {
        return FileUtility.getFileName(file).endsWith(EngineSetting.WORLD_HEIGHT_LAYER_SUFFIX);
    }

    // A layer the size of the world image holding neutral everywhere, for a world not given one yet
    public static Pixmap createNeutralLayer(int width, int height) {

        Pixmap layer = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        int neutral = toLayerColor(EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL);

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                layer.drawPixel(x, y, neutral);

        return layer;
    }

    // Level \\

    // The opaque grey RGBA8888 pixel that paints a level
    public static int toLayerColor(int level) {
        return (level << EngineSetting.PACKED_COLOR_ALPHA_SHIFT)
                | (level << (Byte.SIZE * 2))
                | (level << Byte.SIZE)
                | EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    public static int toLevel(int layerColor) {
        return layerColor >>> EngineSetting.PACKED_COLOR_ALPHA_SHIFT;
    }

    public static float toElevation(int level) {

        int offset = level - EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL;

        return offset >= 0
                ? offset / (float) (EngineSetting.WORLD_HEIGHT_LAYER_MAX_LEVEL
                        - EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL)
                : offset / (float) EngineSetting.WORLD_HEIGHT_LAYER_NEUTRAL_LEVEL;
    }

    // Sample \\

    // Any thread — the layer's elevation at a world position, read live from the pixels the editor paints
    public static float sampleElevation(WorldHandle worldHandle, double worldX, double worldZ) {

        Pixmap layer = worldHandle.getHeightLayer();
        int width = layer.getWidth();
        int height = layer.getHeight();

        double blocksPerPixel = EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
        double latticeX = worldX / blocksPerPixel - 0.5;
        double latticeZ = worldZ / blocksPerPixel - 0.5;

        int baseX = (int) Math.floor(latticeX) - 1;
        int baseZ = (int) Math.floor(latticeZ) - 1;
        float tx = (float) (latticeX - Math.floor(latticeX));
        float tz = (float) (latticeZ - Math.floor(latticeZ));

        float elevation = 0f;

        for (int j = 0; j < SPLINE_TAPS; j++) {

            int pixelZ = Math.floorMod(baseZ + j, height);
            float row = 0f;

            for (int i = 0; i < SPLINE_TAPS; i++)
                row += splineWeight(i, tx)
                        * toElevation(toLevel(layer.getPixel(Math.floorMod(baseX + i, width), pixelZ)));

            elevation += row * splineWeight(j, tz);
        }

        return elevation;
    }

    // Math \\

    // Uniform cubic B-spline basis: the four weights always sum to one and never overshoot
    private static float splineWeight(int tap, float t) {

        float s = 1f - t;

        return switch (tap) {
            case 0 -> s * s * s / 6f;
            case 1 -> (3f * t * t * t - 6f * t * t + 4f) / 6f;
            case 2 -> (3f * s * s * s - 6f * s * s + 4f) / 6f;
            default -> t * t * t / 6f;
        };
    }
}
