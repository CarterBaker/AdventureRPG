package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.menupipeline.font.GlyphMetricStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class FontGeometryBranch extends BranchPackage {

    /*
     * Fills a pre-constructed DynamicModelHandle with one origin-space quad per
     * glyph. Caller is responsible for creating the model with the correct
     * materialID and VAOHandle — same pattern as all other geometry branches.
     *
     * VAO layout [2, 2] — 4 floats per vertex:
     * [0] x [1] y [2] u [3] v
     */

    public static final int OFFSET_INDEX_X = EngineSetting.FONT_DEFAULT_OFFSET_INDEX_X;
    public static final int OFFSET_INDEX_Y = EngineSetting.FONT_DEFAULT_OFFSET_INDEX_Y;

    // Build \\

    void buildGlyphModel(
            DynamicModelHandle model,
            GlyphMetricStruct glyph,
            int atlasPixelSize) {

        float invAtlas = 1.0f / atlasPixelSize;
        float u0 = glyph.atlasX * invAtlas;
        float v0 = glyph.atlasY * invAtlas;
        float u1 = (glyph.atlasX + glyph.width) * invAtlas;
        float v1 = (glyph.atlasY + glyph.height) * invAtlas;
        float x1 = glyph.width;
        float y1 = glyph.height;

        FloatArrayList verts = new FloatArrayList(16);

        // Vert 0 — bottom left
        verts.add(0f);
        verts.add(0f);
        verts.add(u0);
        verts.add(v1);

        // Vert 1 — bottom right
        verts.add(x1);
        verts.add(0f);
        verts.add(u1);
        verts.add(v1);

        // Vert 2 — top right
        verts.add(x1);
        verts.add(y1);
        verts.add(u1);
        verts.add(v0);

        // Vert 3 — top left
        verts.add(0f);
        verts.add(y1);
        verts.add(u0);
        verts.add(v0);

        model.addQuadVertices(verts);
    }
}