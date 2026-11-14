package application.bootstrap.mappipeline.util;

import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class MapShadeUtility extends EngineUtility {

    /*
     * Stateless coloring of one map texel, shared by the generated tiles and
     * the real chunk tops drawn over them, so both read alike. Land turns from
     * its top color to its side color as it steepens, the same rule the macro
     * terrain shader follows, and is lit from the north west with relief
     * exaggerated as texels widen, so a continent still shows its ranges.
     * Water deepens from turquoise to blue with the floor showing through the
     * shallows. Colors are packed 0xRRGGBB, and alpha is always opaque.
     */

    // Land \\

    public static int shadeLand(int topColor, int sideColor, float slopeX, float slopeZ, float texelBlocks) {

        float steepness = slopeX * slopeX + slopeZ * slopeZ;
        float facingUp = 1f / (float) Math.sqrt(1f + steepness);
        float slope = SkyColorUtility.smoothstep(
                EngineSetting.MAP_SLOPE_START, EngineSetting.MAP_SLOPE_END, 1f - facingUp);
        int baseColor = PackedColorUtility.mix(topColor, sideColor, slope);

        float relief = EngineSetting.MAP_RELIEF_PER_BLOCK * (float) Math.sqrt(texelBlocks);
        float facingLight = -(slopeX * EngineSetting.MAP_LIGHT_X + slopeZ * EngineSetting.MAP_LIGHT_Z) * relief;
        float shade = 1f + Math.max(-1f, Math.min(1f, facingLight)) * EngineSetting.MAP_HILLSHADE_STRENGTH;

        return PackedColorUtility.scale(baseColor, shade);
    }

    // Water \\

    public static int shadeWater(int floorColor, float depthBlocks) {

        float deep = SkyColorUtility.smoothstep(0f, EngineSetting.MAP_WATER_DEEP_BLOCKS, depthBlocks);
        float clear = 1f - SkyColorUtility.smoothstep(0f, EngineSetting.MAP_WATER_CLEAR_BLOCKS, depthBlocks);
        int waterColor = PackedColorUtility.mix(
                EngineSetting.MAP_COLOR_WATER_SHALLOW, EngineSetting.MAP_COLOR_WATER_DEEP, deep);

        return PackedColorUtility.mix(waterColor, floorColor, clear * EngineSetting.MAP_WATER_FLOOR_SHARE);
    }

    // Texel \\

    public static void writeTexel(byte[] pixels, int texel, int color) {

        int offset = texel * EngineSetting.COLOR_CHANNEL_COUNT;

        pixels[offset] = (byte) PackedColorUtility.red(color);
        pixels[offset + 1] = (byte) PackedColorUtility.green(color);
        pixels[offset + 2] = (byte) PackedColorUtility.blue(color);
        pixels[offset + 3] = (byte) EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }
}
