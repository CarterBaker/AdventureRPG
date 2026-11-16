package editor.bootstrap.imagepipeline.util;

import editor.bootstrap.imagepipeline.imagemap.MapGenerateScratchStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class MapGenerateUtility extends EngineUtility {

    /*
     * Turns an image into its material maps the way a map generator does,
     * on float images held in a scratch, rows top first: the height is the
     * image's brightness with the forms larger than the detail size taken
     * away (a high pass), softened, stretched by its contrast and turned over
     * when dark is high; displacement is the height about its mid level and
     * bump the finest detail alone; the normal is the height's slope (Sobel)
     * bent by the strength, green up for OpenGL and down for DirectX;
     * occlusion and cavity darken where the height sits below its blurred
     * surroundings, widely and closely; curvature is the height's Laplacian,
     * ridges bright and creases dark; thickness is bright where the
     * surroundings sit low; roughness rises from its base where the image is
     * dark and glossiness is its inverse; metallic and tint are one value;
     * the specular colour runs from the dielectric level to the image's
     * colour as metallic rises and diffuse loses what metal takes; emissive
     * keeps the image's colour where it is brighter than the threshold; and
     * opacity is the image's alpha or brightness, cut when asked. Blurs are
     * two passes of a running box, so their cost holds whatever the radius,
     * and edges clamp. Nothing allocates.
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

    // Generators \\

    public static void generateNormal(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeNormal(scratch, settings, out, false);
    }

    public static void generateNormalDirectX(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeNormal(scratch, settings, out, true);
    }

    private static void writeNormal(
            MapGenerateScratchStruct scratch,
            float[] settings,
            int[] out,
            boolean greenDown) {

        int width = scratch.getWidth();
        int rows = scratch.getRows();
        float[] normal = scratch.getNormal();
        float strength = MapGenerateOption.NORMAL_STRENGTH.get(settings);

        resolveHeight(scratch, settings);

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < width; x++) {
                resolveNormal(scratch.getHeight(), width, rows, x, y, strength, normal);
                out[y * width + x] = toColor(normal[0], greenDown ? 1f - normal[1] : normal[1], normal[2]);
            }
    }

    public static void generateHeight(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        resolveHeight(scratch, settings);
        writeGreys(scratch.getHeight(), out, scratch.getCount());
    }

    public static void generateDisplacement(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        float[] height = scratch.getHeight();
        float offset = MapGenerateOption.MID_LEVEL.get(settings) - 0.5f;

        resolveHeight(scratch, settings);

        for (int i = 0; i < scratch.getCount(); i++)
            out[i] = toGrey(height[i] + offset);
    }

    public static void generateBump(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        float[] luminance = scratch.getLuminance();
        float[] work = scratch.getWork();
        float contrast = MapGenerateOption.HEIGHT_CONTRAST.get(settings);
        boolean invert = MapGenerateOption.HEIGHT_INVERT.isOn(settings);

        blur(luminance, work, scratch.getScratch(), scratch.getWidth(), scratch.getRows(),
                Math.round(MapGenerateOption.BUMP_DETAIL.get(settings)));

        for (int i = 0; i < scratch.getCount(); i++) {
            float value = (luminance[i] - work[i]) * contrast + 0.5f;
            out[i] = toGrey(invert ? 1f - value : value);
        }
    }

    public static void generateOcclusion(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeOcclusion(scratch, settings, out, MapGenerateOption.OCCLUSION_RADIUS.get(settings));
    }

    public static void generateCavity(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeOcclusion(scratch, settings, out, MapGenerateOption.CAVITY_RADIUS.get(settings));
    }

    private static void writeOcclusion(
            MapGenerateScratchStruct scratch,
            float[] settings,
            int[] out,
            float radius) {

        resolveHeight(scratch, settings);
        resolveOcclusion(
                scratch.getHeight(), scratch.getWork(), scratch.getScratch(), scratch.getWidth(), scratch.getRows(),
                Math.max(1, Math.round(radius)), MapGenerateOption.OCCLUSION_STRENGTH.get(settings));
        writeGreys(scratch.getWork(), out, scratch.getCount());
    }

    public static void generateCurvature(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        int width = scratch.getWidth();
        int rows = scratch.getRows();
        float[] height = scratch.getHeight();
        float strength = MapGenerateOption.CURVATURE_STRENGTH.get(settings);

        resolveHeight(scratch, settings);

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < width; x++) {

                float around = sample(height, width, rows, x - 1, y) + sample(height, width, rows, x + 1, y)
                        + sample(height, width, rows, x, y - 1) + sample(height, width, rows, x, y + 1);

                out[y * width + x] = toGrey(0.5f + (height[y * width + x] * 4f - around) * strength);
            }
    }

    public static void generateThickness(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        float[] work = scratch.getWork();

        resolveHeight(scratch, settings);
        blur(scratch.getHeight(), work, scratch.getScratch(), scratch.getWidth(), scratch.getRows(),
                Math.round(MapGenerateOption.THICKNESS_RADIUS.get(settings)));

        for (int i = 0; i < scratch.getCount(); i++)
            out[i] = toGrey(1f - work[i]);
    }

    public static void generateRoughness(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeRoughness(scratch, settings, out, false);
    }

    public static void generateGlossiness(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeRoughness(scratch, settings, out, true);
    }

    private static void writeRoughness(
            MapGenerateScratchStruct scratch,
            float[] settings,
            int[] out,
            boolean glossy) {

        float[] luminance = scratch.getLuminance();
        float base = MapGenerateOption.ROUGHNESS_BASE.get(settings);
        float variation = MapGenerateOption.ROUGHNESS_VARIATION.get(settings);

        for (int i = 0; i < scratch.getCount(); i++) {
            float roughness = resolveRoughness(luminance[i], base, variation);
            out[i] = toGrey(glossy ? 1f - roughness : roughness);
        }
    }

    public static void generateMetallic(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeLevel(MapGenerateOption.METALLIC.get(settings), out, scratch.getCount());
    }

    public static void generateTint(MapGenerateScratchStruct scratch, float[] settings, int[] out) {
        writeLevel(MapGenerateOption.TINT_AMOUNT.get(settings), out, scratch.getCount());
    }

    // The dielectric level, at most the strongest non-metal reflects, carried to the image's colour by metallic
    public static void generateSpecular(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        int[] pixels = scratch.getPixels();
        float dielectric = MapGenerateOption.SPECULAR_LEVEL.get(settings) * EditorSetting.MAP_GENERATE_MAX_DIELECTRIC_SPECULAR;
        float metallic = MapGenerateOption.METALLIC.get(settings);

        for (int i = 0; i < scratch.getCount(); i++)
            out[i] = toColor(
                    dielectric + (toChannel(pixels[i], 0) - dielectric) * metallic,
                    dielectric + (toChannel(pixels[i], 1) - dielectric) * metallic,
                    dielectric + (toChannel(pixels[i], 2) - dielectric) * metallic);
    }

    public static void generateDiffuse(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        int[] pixels = scratch.getPixels();
        float kept = 1f - MapGenerateOption.METALLIC.get(settings);

        for (int i = 0; i < scratch.getCount(); i++)
            out[i] = toColor(
                    toChannel(pixels[i], 0) * kept, toChannel(pixels[i], 1) * kept, toChannel(pixels[i], 2) * kept);
    }

    public static void generateEmissive(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        int[] pixels = scratch.getPixels();
        float[] luminance = scratch.getLuminance();
        float threshold = MapGenerateOption.GLOW_THRESHOLD.get(settings);
        float softness = Math.max(MapGenerateOption.GLOW_SOFTNESS.get(settings), EditorSetting.MAP_GENERATE_MIN_GLOW_SOFTNESS);
        float strength = MapGenerateOption.GLOW_STRENGTH.get(settings);

        for (int i = 0; i < scratch.getCount(); i++) {

            float t = Math.clamp((luminance[i] - threshold + softness) / (softness * 2f), 0f, 1f);
            float glow = t * t * (3f - 2f * t) * strength;

            out[i] = toColor(
                    toChannel(pixels[i], 0) * glow, toChannel(pixels[i], 1) * glow, toChannel(pixels[i], 2) * glow);
        }
    }

    public static void generateOpacity(MapGenerateScratchStruct scratch, float[] settings, int[] out) {

        int[] pixels = scratch.getPixels();
        float[] luminance = scratch.getLuminance();
        boolean fromBrightness = MapGenerateOption.OPACITY_FROM_BRIGHTNESS.isOn(settings);
        float cutoff = MapGenerateOption.OPACITY_CUTOFF.get(settings);

        for (int i = 0; i < scratch.getCount(); i++) {

            float opacity = fromBrightness ? luminance[i] : toChannel(pixels[i], EngineSetting.COLOR_CHANNEL_COUNT - 1);

            out[i] = toGrey(cutoff > 0f ? (opacity >= cutoff ? 1f : 0f) : opacity);
        }
    }

    // The height of the scratch's image by a map's settings
    private static void resolveHeight(MapGenerateScratchStruct scratch, float[] settings) {
        resolveHeight(
                scratch.getLuminance(), scratch.getHeight(), scratch.getBlurred(), scratch.getScratch(),
                scratch.getWidth(), scratch.getRows(),
                Math.max(1, Math.round(MapGenerateOption.HEIGHT_DETAIL.get(settings))),
                Math.round(MapGenerateOption.HEIGHT_SOFTNESS.get(settings)),
                MapGenerateOption.HEIGHT_CONTRAST.get(settings),
                MapGenerateOption.HEIGHT_INVERT.isOn(settings));
    }

    private static void writeGreys(float[] values, int[] out, int count) {
        for (int i = 0; i < count; i++)
            out[i] = toGrey(values[i]);
    }

    private static void writeLevel(float value, int[] out, int count) {

        int grey = toGrey(value);

        for (int i = 0; i < count; i++)
            out[i] = grey;
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
