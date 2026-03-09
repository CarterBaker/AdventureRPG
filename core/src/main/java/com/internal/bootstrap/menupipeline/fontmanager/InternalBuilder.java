package com.internal.bootstrap.menupipeline.fontmanager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import com.internal.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.menupipeline.fonts.FontHandle;
import com.internal.bootstrap.menupipeline.fonts.FontTileData;
import com.internal.bootstrap.menupipeline.fonts.GlyphMetricStruct;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.AtlasUtility;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Builds FontHandles directly from TTF/OTF files. No JSON config — all
 * parameters fall back to EngineSetting defaults. Pipeline:
 *   1. Rasterize TTF glyphs → FontTileData list
 *   2. Pack glyph tiles into atlas → atlasPixelSize
 *   3. Composite and upload atlas image to GPU
 *   4. Build GlyphMetricStruct table from packed positions
 *   5. Resolve label VAOHandle once for this font
 *   6. Create each glyph DynamicModelHandle, pass to DynamicGeometryManager to fill
 *   7. Construct and return FontHandle
 */
class InternalBuilder extends BuilderPackage {

    private DynamicGeometryManager dynamicGeometryManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;

    // Internal \\

    @Override
    protected void get() {
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.materialManager = get(MaterialManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    FontHandle build(String name, File fontFile) {

        int materialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.FONT_DEFAULT_MATERIAL);
        int size = EngineSetting.FONT_DEFAULT_SIZE;
        String charset = EngineSetting.FONT_DEFAULT_CHARSET;

        // 1. Rasterize
        ObjectArrayList<FontTileData> tiles = FontRasterizerUtility.rasterize(fontFile, size, charset, this);

        if (tiles.isEmpty())
            throwException("[FontManager] No glyphs rasterized for font: " + name);

        // 2. Pack
        int atlasPixelSize = AtlasUtility.pack(tiles);

        // 3. Composite and upload
        BufferedImage atlasImage = compositeAtlas(tiles, atlasPixelSize);
        int gpuHandle = GLSLUtility.pushTexture2D(atlasImage);

        // 4. Glyph metric table
        Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs = buildGlyphTable(tiles, atlasPixelSize);

        // 5. Resolve VAO once — all glyph models for this font share the same VAO
        VAOHandle vaoHandle = vaoManager.getVAOHandleFromName(EngineSetting.FONT_DEFAULT_VAO);

        // 6. Per-glyph DynamicModelHandles
        Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels = buildGlyphModels(glyphs, materialID, vaoHandle,
                atlasPixelSize);

        // 7. Clear heap images
        for (int i = 0; i < tiles.size(); i++)
            tiles.get(i).clearImage();

        FontHandle handle = create(FontHandle.class);
        handle.constructor(name, gpuHandle, materialID, atlasPixelSize, glyphs, glyphModels);
        return handle;
    }

    // Compositing \\

    private BufferedImage compositeAtlas(ObjectArrayList<FontTileData> tiles, int atlasPixelSize) {

        BufferedImage canvas = new BufferedImage(
                atlasPixelSize, atlasPixelSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        for (int i = 0; i < tiles.size(); i++) {
            FontTileData tile = tiles.get(i);
            g.drawImage(tile.getImage(),
                    tile.getAtlasX(), tile.getAtlasY(),
                    tile.getAtlasX() + tile.getTileWidth(),
                    tile.getAtlasY() + tile.getTileHeight(),
                    0, 0, tile.getTileWidth(), tile.getTileHeight(), null);
        }

        g.dispose();
        return canvas;
    }

    // Glyph Table \\

    private Int2ObjectOpenHashMap<GlyphMetricStruct> buildGlyphTable(
            ObjectArrayList<FontTileData> tiles, int atlasPixelSize) {

        Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs = new Int2ObjectOpenHashMap<>(tiles.size());

        for (int i = 0; i < tiles.size(); i++) {
            FontTileData tile = tiles.get(i);
            glyphs.put(tile.getCodepoint(), new GlyphMetricStruct(
                    tile.getAtlasX(), tile.getAtlasY(),
                    tile.getTileWidth(), tile.getTileHeight(),
                    tile.getBearingX(), tile.getBearingY(),
                    tile.getAdvance()));
        }

        return glyphs;
    }

    // Glyph Models \\

    private Int2ObjectOpenHashMap<DynamicModelHandle> buildGlyphModels(
            Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs,
            int materialID,
            VAOHandle vaoHandle,
            int atlasPixelSize) {

        Int2ObjectOpenHashMap<DynamicModelHandle> models = new Int2ObjectOpenHashMap<>(glyphs.size());

        for (var entry : glyphs.int2ObjectEntrySet()) {
            GlyphMetricStruct g = entry.getValue();
            if (g.width <= 0 || g.height <= 0)
                continue;

            // Caller creates the model with correct materialID and VAOHandle
            DynamicModelHandle model = create(DynamicModelHandle.class);
            model.constructor(materialID, vaoHandle);

            // Branch fills the verts
            dynamicGeometryManager.buildGlyphModel(model, g, atlasPixelSize);

            models.put(entry.getIntKey(), model);
        }

        return models;
    }

    // Helpers \\

    FontTileData createFontTile() {
        return create(FontTileData.class);
    }
}