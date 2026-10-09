package engine.graphics.color;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class PackedColorUtility extends EngineUtility {

    /*
     * Stateless helpers for opaque colors packed as one 0xRRGGBB int, the form
     * texture averages, block and biome surface colors and map pixels travel
     * in. Channels are whole bytes, and every result is clamped back into
     * range, so packing never bleeds one channel into the next.
     */

    // Pack \\

    public static int pack(int red, int green, int blue) {
        return (clampChannel(red) << EngineSetting.PACKED_COLOR_RED_SHIFT)
                | (clampChannel(green) << EngineSetting.PACKED_COLOR_GREEN_SHIFT)
                | clampChannel(blue);
    }

    public static int pack(float red, float green, float blue) {
        return pack(Math.round(red), Math.round(green), Math.round(blue));
    }

    public static int packUnit(float red, float green, float blue) {
        return pack(
                red * EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                green * EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                blue * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
    }

    private static int clampChannel(int channel) {
        return Math.max(0, Math.min(EngineSetting.PACKED_COLOR_CHANNEL_MASK, channel));
    }

    // Channels \\

    public static int red(int packed) {
        return (packed >> EngineSetting.PACKED_COLOR_RED_SHIFT) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    public static int green(int packed) {
        return (packed >> EngineSetting.PACKED_COLOR_GREEN_SHIFT) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    public static int blue(int packed) {
        return packed & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    // Blend \\

    public static int scale(int packed, float factor) {
        return pack(red(packed) * factor, green(packed) * factor, blue(packed) * factor);
    }

    // One color tinted by another, channel by channel, as a texture is tinted by its color
    public static int multiply(int color, int tint) {
        return pack(
                red(color) * red(tint) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                green(color) * green(tint) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                blue(color) * blue(tint) / EngineSetting.COLOR_CHANNEL_BYTE_MAX);
    }

    public static int mix(int from, int to, float t) {
        return pack(
                red(from) + (red(to) - red(from)) * t,
                green(from) + (green(to) - green(from)) * t,
                blue(from) + (blue(to) - blue(from)) * t);
    }
}
