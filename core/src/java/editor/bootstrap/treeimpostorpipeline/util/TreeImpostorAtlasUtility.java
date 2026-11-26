package editor.bootstrap.treeimpostorpipeline.util;

import java.nio.ByteBuffer;

import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionScratchStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;

public final class TreeImpostorAtlasUtility extends EngineUtility {

    /*
     * The geometry and texels of an impostor bake. Every frame across and
     * down of the hemi-octahedral atlas stands for the direction its centre
     * decodes to, the inverse of TreeImpostor.glsl's encoding, and is seen
     * along it by an orthographic camera framed on the shared tree's sphere,
     * its right the world's up crossed with that direction and its up that
     * direction crossed with its right, as the game's quad is laid. A frame
     * read back (BGRA, rows bottom first) is copied into the atlas as RGBA.
     * Before compressing, every texel the tree leaves empty takes the colour
     * of a covered neighbour, a few rings out, so filtering never draws a
     * dark fringe, and each smaller level averages the covered texels of the
     * four beneath it, its coverage their mean. Every level is then BC7, from
     * the whole atlas down to one texel.
     */

    // Directions \\

    // The direction a frame stands for, in out as x, y and z
    public static void resolveDirection(int frameX, int frameY, float[] out) {

        int frames = EngineSetting.TREE_IMPOSTOR_FRAMES;
        float u = (frameX + 0.5f) / frames * 2f - 1f;
        float v = (frameY + 0.5f) / frames * 2f - 1f;
        float x = (u + v) * 0.5f;
        float z = (u - v) * 0.5f;
        float y = 1f - Math.abs(x) - Math.abs(z);
        float length = (float) Math.sqrt(x * x + y * y + z * z);

        out[EngineSetting.AXIS_X] = x / length;
        out[EngineSetting.AXIS_Y] = y / length;
        out[EngineSetting.AXIS_Z] = z / length;
    }

    // The orthographic view and projection of a frame, framed on the sphere of a radius about a height on the trunk
    public static void writeViewProjection(float[] direction, float centerY, float radius, Matrix4 out) {

        float dx = direction[EngineSetting.AXIS_X];
        float dy = direction[EngineSetting.AXIS_Y];
        float dz = direction[EngineSetting.AXIS_Z];
        float rightX = dz;
        float rightZ = -dx;
        float across = (float) Math.sqrt(rightX * rightX + rightZ * rightZ);

        if (across < EditorSetting.TREE_IMPOSTOR_MIN_ACROSS) {
            rightX = 1f;
            rightZ = 0f;
        } else {
            rightX /= across;
            rightZ /= across;
        }

        float upX = dy * rightZ;
        float upY = dz * rightX - dx * rightZ;
        float upZ = -dy * rightX;
        float scale = 1f / radius;

        out.set(
                rightX * scale, 0f, rightZ * scale, 0f,
                upX * scale, upY * scale, upZ * scale, -upY * centerY * scale,
                -dx * scale, -dy * scale, -dz * scale, dy * centerY * scale,
                0f, 0f, 0f, 1f);
    }

    // Frames \\

    // A frame read back as BGRA, rows bottom first, copied into its place in an RGBA atlas
    public static void copyFrame(ByteBuffer bgra, byte[] atlas, int frameX, int frameY) {

        int texels = EngineSetting.TREE_IMPOSTOR_FRAME_TEXELS;
        int atlasTexels = EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS;
        int channels = EngineSetting.COLOR_CHANNEL_COUNT;

        for (int row = 0; row < texels; row++)
            for (int column = 0; column < texels; column++) {

                int from = (row * texels + column) * channels;
                int to = ((frameY * texels + row) * atlasTexels + frameX * texels + column) * channels;

                atlas[to] = bgra.get(from + 2);
                atlas[to + 1] = bgra.get(from + 1);
                atlas[to + 2] = bgra.get(from);
                atlas[to + 3] = bgra.get(from + 3);
            }
    }

    // Compression \\

    // Every map's empty texels given a covered neighbour's colour, a few rings out, judged by the albedo's coverage
    public static void dilate(byte[] albedo, byte[] normal, boolean[] covered, boolean[] scratch) {

        int size = EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS;
        int channels = EngineSetting.COLOR_CHANNEL_COUNT;

        for (int texel = 0; texel < covered.length; texel++)
            covered[texel] = (albedo[texel * channels + 3] & 0xFF) > 0;

        for (int ring = 0; ring < EditorSetting.TREE_IMPOSTOR_DILATE_RINGS; ring++) {

            System.arraycopy(covered, 0, scratch, 0, covered.length);

            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++) {

                    int texel = y * size + x;

                    if (scratch[texel])
                        continue;

                    int source = findCoveredNeighbour(scratch, x, y, size);

                    if (source == EngineSetting.INDEX_NOT_FOUND)
                        continue;

                    for (int channel = 0; channel < 3; channel++) {
                        albedo[texel * channels + channel] = albedo[source * channels + channel];
                        normal[texel * channels + channel] = normal[source * channels + channel];
                    }

                    covered[texel] = true;
                }
        }
    }

    private static int findCoveredNeighbour(boolean[] covered, int x, int y, int size) {

        if (x > 0 && covered[y * size + x - 1])
            return y * size + x - 1;

        if (x + 1 < size && covered[y * size + x + 1])
            return y * size + x + 1;

        if (y > 0 && covered[(y - 1) * size + x])
            return (y - 1) * size + x;

        if (y + 1 < size && covered[(y + 1) * size + x])
            return (y + 1) * size + x;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // A map's whole BC7 chain, each smaller level averaging the four texels beneath it weighed by the albedo's
    // coverage there, the albedo's own coverage their mean
    public static byte[] compress(
            byte[] map,
            byte[] albedo,
            boolean coverageChannel,
            TextureCompressionScratchStruct scratch) {

        int size = EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS;
        byte[] out = new byte[TextureCompressionUtility.getChainBytes(size)];
        byte[] level = map.clone();
        byte[] levelCoverage = albedo.clone();
        int offset = 0;

        for (int edge = size; edge >= 1; edge >>= 1) {

            TextureCompressionUtility.encodeBC7(level, edge, edge, out, offset, scratch);
            offset += TextureCompressionUtility.getLevelBytes(edge, edge);

            if (edge == 1)
                break;

            byte[] smaller = new byte[(edge / 2) * (edge / 2) * EngineSetting.COLOR_CHANNEL_COUNT];
            byte[] smallerCoverage = new byte[smaller.length];

            downsample(level, levelCoverage, edge, smaller, smallerCoverage, coverageChannel);
            level = smaller;
            levelCoverage = smallerCoverage;
        }

        return out;
    }

    private static void downsample(
            byte[] level,
            byte[] coverage,
            int edge,
            byte[] smaller,
            byte[] smallerCoverage,
            boolean coverageChannel) {

        int half = edge / 2;
        int channels = EngineSetting.COLOR_CHANNEL_COUNT;

        for (int y = 0; y < half; y++)
            for (int x = 0; x < half; x++) {

                float weight = 0f;
                float alpha = 0f;
                float red = 0f;
                float green = 0f;
                float blue = 0f;

                for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++) {

                    int texel = ((y * 2 + (corner >> 1)) * edge + x * 2 + (corner & 1)) * channels;
                    float covered = (coverage[texel + 3] & 0xFF) / 255f;
                    float texelWeight = Math.max(covered, EditorSetting.TREE_IMPOSTOR_EMPTY_WEIGHT);

                    red += (level[texel] & 0xFF) * texelWeight;
                    green += (level[texel + 1] & 0xFF) * texelWeight;
                    blue += (level[texel + 2] & 0xFF) * texelWeight;

                    weight += texelWeight;
                    alpha += covered;
                }

                int to = (y * half + x) * channels;
                int meanAlpha = Math.round(alpha / EngineSetting.QUAD_VERTEX_COUNT * 255f);

                smaller[to] = (byte) Math.round(red / weight);
                smaller[to + 1] = (byte) Math.round(green / weight);
                smaller[to + 2] = (byte) Math.round(blue / weight);

                smaller[to + 3] = coverageChannel ? (byte) meanAlpha : (byte) 0xFF;
                smallerCoverage[to + 3] = (byte) meanAlpha;
            }
    }
}
