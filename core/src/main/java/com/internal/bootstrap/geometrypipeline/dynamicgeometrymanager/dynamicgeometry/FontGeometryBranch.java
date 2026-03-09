package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.menupipeline.fonts.GlyphMetricStruct;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/*
 * Fills a pre-constructed DynamicModelHandle with one origin-space quad per
 * glyph. Caller is responsible for creating the model with the correct
 * materialID and VAOHandle before passing it in — same pattern as all other
 * geometry branches which receive a packet/model and fill it.
 *
 * VAO layout [2, 2, 1] — 5 floats per vertex:
 *   [0] x    [1] y    [2] u    [3] v
 */
public class FontGeometryBranch extends BranchPackage {

    public static final int OFFSET_INDEX_X = EngineSetting.FONT_DEFAULT_OFFSET_INDEX_X;
    public static final int OFFSET_INDEX_Y = EngineSetting.FONT_DEFAULT_OFFSET_INDEX_Y;

    // Build \\

    public void buildGlyphModel(
            DynamicModelHandle model,
            GlyphMetricStruct glyph,
            int atlasPixelSize) {

        float invAtlas = 1.0f / atlasPixelSize;
        float u0 = glyph.atlasX * invAtlas;
        float v0 = glyph.atlasY * invAtlas;
        float u1 = (glyph.atlasX + glyph.width) * invAtlas;
        float v1 = (glyph.atlasY + glyph.height) * invAtlas;

        float x0 = 0f;
        float y0 = 0f;
        float x1 = glyph.width;
        float y1 = glyph.height;

        FloatArrayList verts = new FloatArrayList(20);

        // Vert 0 — top left
        verts.add(x0);
        verts.add(y0);
        verts.add(u0);
        verts.add(v0);
        // Vert 1 — top right
        verts.add(x1);
        verts.add(y0);
        verts.add(u1);
        verts.add(v0);
        // Vert 2 — bottom right
        verts.add(x1);
        verts.add(y1);
        verts.add(u1);
        verts.add(v1);
        // Vert 3 — bottom left
        verts.add(x0);
        verts.add(y1);
        verts.add(u0);
        verts.add(v1);

        model.addQuadVertices(verts);
    }
}