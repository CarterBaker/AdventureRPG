package application.bootstrap.geometrypipeline.meshmanager;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;

class MeshQuadUtility extends EngineUtility {

    /*
     * The texturing rules every quad entry shares, rigged or not: the corner
     * UVs a quad may author, and how a corner's UV snaps to its tile's pixels
     * with a sliver of inset on the tile's border, so nearest sampling along a
     * seam never reads the neighbouring tile of the atlas.
     */

    // UVs \\

    // The eight corner UV floats a quad authors in "uvs", or null when it authors none
    static float[] parseCornerUVs(ArpgObjectStruct quadArpg, String sourceName) {

        if (!quadArpg.has("uvs") || quadArpg.get("uvs").isNull())
            return null;

        ArpgArrayStruct uvsArray = quadArpg.getAsArray("uvs");

        if (uvsArray.size() != EngineSetting.QUAD_VERTEX_COUNT)
            throwException("Quad 'uvs' must have exactly 4 entries in file: " + sourceName);

        float[] cornerUVs = new float[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.RIGGED_MESH_UV_FLOATS];

        for (int i = 0; i < EngineSetting.QUAD_VERTEX_COUNT; i++) {

            ArpgArrayStruct pair = uvsArray.get(i).getAsArray();

            if (pair.size() != EngineSetting.RIGGED_MESH_UV_FLOATS)
                throwException("Quad 'uvs' entry " + i + " must have 2 values in file: " + sourceName);

            cornerUVs[i * EngineSetting.RIGGED_MESH_UV_FLOATS] = pair.get(0).getAsFloat();
            cornerUVs[i * EngineSetting.RIGGED_MESH_UV_FLOATS + 1] = pair.get(1).getAsFloat();
        }

        return cornerUVs;
    }

    static float snapUV(float local, float tileMin, float tileMax, int tilePixelSize) {

        int pixel = Math.round(local * tilePixelSize);
        float inset = EngineSetting.MESH_UV_EDGE_INSET_TEXELS / tilePixelSize;
        float snapped = Math.clamp(pixel / (float) tilePixelSize, inset, 1f - inset);

        return tileMin + snapped * (tileMax - tileMin);
    }
}
