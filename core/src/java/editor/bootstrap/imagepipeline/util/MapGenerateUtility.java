package editor.bootstrap.imagepipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class MapGenerateUtility extends EngineUtility {

    /*
     * Turns an albedo into its material maps the way a map generator does,
     * on float images held by the caller, rows top first: the height is the
     * image's brightness with the forms larger than the detail size taken
     * away (a high pass), softened, stretched by its contrast and turned over
     * when dark is high; the normal is the height's slope (Sobel) bent by the
     * strength, written with green up as tangent-space maps are; occlusion
     * darkens where the height sits below its blurred surroundings, its
     * hollows; roughness rises from its base where the image is dark;
     * metallic is one value. Blurs are two passes of a running box, so their
     * cost holds whatever the radius, and edges clamp. Nothing allocates.
     */

    // Height \\

    // The brightness of every pixel of an RGBA8888 image, 0 to 1, by the eye's weighting of its channels
    public static void toLuminance(int[] rgba, float[] out, int count) {

        for (int i = 0; i < count; i++)
            out[i] = toChannel(rgba[i], 0) * EngineSetting.SKY_LUMINANCE_R
                    + toChannel(rgba[i], 1) * EngineSetting.SKY_LUMINANCE_G
                    + toChannel(rgba[i], 2) * EngineSetting.SKY_LUMINANCE_B;
    }

    // Height from brightness: the large forms taken away, softened, stretched about its middle and turned over if asked
    public static void resolveHeight(
            float[] luminance,
            float[] height,
            float[] blurred,
            float[] scratch,
            int width,
            int rows,
            int detailRadius,
            int softRadius,
            float contrast,
            boolean invert) {

        int count = width * rows;

        blur(luminance, blurred, scratch, width, rows, detailRadius);

        for (int i = 0; i < count; i++) {

            float value = (luminance[i] - blurred[i]) * contrast + 0.5f;
            height[i] = invert ? 1f - value : value;
        }

        if (softRadius > 0) {
            blur(height, blurred, scratch, width, rows, softRadius);
            System.arraycopy(blurred, 0, height, 0, count);
        }

        for (int i = 0; i < count; i++)
            height[i] = Math.clamp(height[i], 0f, 1f);
    }

    // Normal \\

    // The tangent-space normal at a pixel from the height's slope, x right and y up, packed 0 to 1 per axis
    public static void resolveNormal(float[] height, int width, int rows, int x, int y, float strength, float[] out) {

        float left = sample(height, width, rows, x - 1, y);
        float right = sample(height, width, rows, x + 1, y);
        float upLeft = sample(height, width, rows, x - 1, y - 1);
        float up = sample(height, width, rows, x, y - 1);
        float upRight = sample(height, width, rows, x + 1, y - 1);
        float downLeft = sample(height, width, rows, x - 1, y + 1);
        float down = sample(height, width, rows, x, y + 1);
        float downRight = sample(height, width, rows, x + 1, y + 1);

        float slopeX = (upRight + 2f * right + downRight) - (upLeft + 2f * left + downLeft);
        float slopeDown = (downLeft + 2f * down + downRight) - (upLeft + 2f * up + upRight);
        float nx = -slopeX * strength;
        float ny = slopeDown * strength;
        float length = (float) Math.sqrt(nx * nx + ny * ny + 1f);

        out[0] = nx / length * 0.5f + 0.5f;
        out[1] = ny / length * 0.5f + 0.5f;
        out[2] = 1f / length * 0.5f + 0.5f;
    }

    // Occlusion \\

    // Ambient occlusion from the height: as dark as a pixel sits below its blurred surroundings
    public static void resolveOcclusion(
            float[] height,
            float[] occlusion,
            float[] scratch,
            int width,
            int rows,
            int radius,
            float strength) {

        blur(height, occlusion, scratch, width, rows, radius);

        for (int i = 0; i < width * rows; i++)
            occlusion[i] = Math.clamp(1f - Math.max(0f, occlusion[i] - height[i]) * strength * 2f, 0f, 1f);
    }

    // Roughness \\

    // Roughness from its base, raised where the image is dark and lowered where it is bright
    public static float resolveRoughness(float luminance, float base, float variation) {
        return Math.clamp(base + (0.5f - luminance) * variation * 2f, 0f, 1f);
    }

    // Blur \\

    // Two passes of a running box blur across and down, near a Gaussian of the radius
    public static void blur(float[] in, float[] out, float[] scratch, int width, int rows, int radius) {

        if (radius <= 0) {
            System.arraycopy(in, 0, out, 0, width * rows);
            return;
        }

        boxRows(in, scratch, width, rows, radius);
        boxColumns(scratch, out, width, rows, radius);
        boxRows(out, scratch, width, rows, radius);
        boxColumns(scratch, out, width, rows, radius);
    }

    private static void boxRows(float[] in, float[] out, int width, int rows, int radius) {

        float share = 1f / (radius * 2 + 1);

        for (int y = 0; y < rows; y++) {

            int base = y * width;
            float sum = 0f;

            for (int k = -radius; k <= radius; k++)
                sum += in[base + Math.clamp(k, 0, width - 1)];

            for (int x = 0; x < width; x++) {
                out[base + x] = sum * share;
                sum += in[base + Math.min(x + radius + 1, width - 1)] - in[base + Math.max(x - radius, 0)];
            }
        }
    }

    private static void boxColumns(float[] in, float[] out, int width, int rows, int radius) {

        float share = 1f / (radius * 2 + 1);

        for (int x = 0; x < width; x++) {

            float sum = 0f;

            for (int k = -radius; k <= radius; k++)
                sum += in[Math.clamp(k, 0, rows - 1) * width + x];

            for (int y = 0; y < rows; y++) {
                out[y * width + x] = sum * share;
                sum += in[Math.min(y + radius + 1, rows - 1) * width + x] - in[Math.max(y - radius, 0) * width + x];
            }
        }
    }

    // Utility \\

    private static float sample(float[] values, int width, int rows, int x, int y) {
        return values[Math.clamp(y, 0, rows - 1) * width + Math.clamp(x, 0, width - 1)];
    }

    // A channel of an RGBA8888 pixel, red first, 0 to 1
    public static float toChannel(int rgba, int channel) {
        return (rgba >>> toShift(channel) & EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
    }

    // A value 0 to 1 as an opaque grey, RGBA8888
    public static int toGrey(float value) {
        return toColor(value, value, value);
    }

    // Three values 0 to 1 as an opaque colour, RGBA8888
    public static int toColor(float r, float g, float b) {
        return toByte(r) << toShift(0) | toByte(g) << toShift(1) | toByte(b) << toShift(2)
                | EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    private static int toByte(float value) {
        return Math.round(Math.clamp(value, 0f, 1f) * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
    }

    private static int toShift(int channel) {
        return (EngineSetting.COLOR_CHANNEL_COUNT - 1 - channel) * Byte.SIZE;
    }
}
