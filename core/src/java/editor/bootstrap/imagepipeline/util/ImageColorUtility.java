package editor.bootstrap.imagepipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ImageColorUtility extends EngineUtility {

    /*
     * How RGBA8888 pixels of a layered image combine, red in the top byte:
     * a mix moves every colour channel a share of the way, the alpha only
     * where it is asked to; laying one over another is the painting tools'
     * normal blend, straight alpha, the top pixel's alpha scaled by its
     * share covering the one below and the alphas joining, so a stroke on a
     * clear layer comes out its own colour however faint it is.
     */

    // Mix \\

    // Two colours mixed a share of the way, alpha only where it is asked for
    public static int mix(int from, int to, float share, boolean alpha) {

        int mixed = 0;
        float t = Math.clamp(share, 0f, 1f);

        for (int channel = 0; channel < EngineSetting.COLOR_CHANNEL_COUNT; channel++) {

            int a = toChannel(from, channel);
            int b = channel == EngineSetting.COLOR_CHANNEL_COUNT - 1 && !alpha ? a : toChannel(to, channel);

            mixed |= Math.round(a + (b - a) * t) << toShift(channel);
        }

        return mixed;
    }

    // Over \\

    // The top colour laid over the one below by its alpha times the share, straight alpha
    public static int over(int below, int top, float share) {

        float topAlpha = toChannel(top, EngineSetting.COLOR_CHANNEL_COUNT - 1)
                / EngineSetting.COLOR_CHANNEL_BYTE_MAX * Math.clamp(share, 0f, 1f);

        if (topAlpha <= 0f)
            return below;

        float belowAlpha = toChannel(below, EngineSetting.COLOR_CHANNEL_COUNT - 1)
                / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        float belowShare = belowAlpha * (1f - topAlpha);
        float alpha = topAlpha + belowShare;
        int mixed = Math.round(alpha * EngineSetting.COLOR_CHANNEL_BYTE_MAX);

        for (int channel = 0; channel < EngineSetting.COLOR_CHANNEL_COUNT - 1; channel++) {

            float value = (toChannel(top, channel) * topAlpha + toChannel(below, channel) * belowShare) / alpha;

            mixed |= Math.round(value) << toShift(channel);
        }

        return mixed;
    }

    // Alpha \\

    // The colour kept, its alpha worn a share of the way to clear
    public static int wear(int color, float share) {

        int alpha = toChannel(color, EngineSetting.COLOR_CHANNEL_COUNT - 1);
        int worn = Math.round(alpha * (1f - Math.clamp(share, 0f, 1f)));

        return (color & ~EngineSetting.PACKED_COLOR_CHANNEL_MASK) | worn;
    }

    // Utility \\

    public static int toChannel(int rgba, int channel) {
        return (rgba >>> toShift(channel)) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    private static int toShift(int channel) {
        return (EngineSetting.COLOR_CHANNEL_COUNT - 1 - channel) * Byte.SIZE;
    }
}
