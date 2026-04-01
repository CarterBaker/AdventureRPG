package program.bootstrap.menupipeline.fontmanager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager;
import program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.menupipeline.fonts.FontHandle;
import program.bootstrap.menupipeline.fonts.FontTileData;
import program.bootstrap.menupipeline.fonts.GlyphMetricStruct;
import program.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import program.core.engine.BuilderPackage;
import program.core.settings.EngineSetting;
import program.core.util.AtlasUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Builds FontHandles directly from TTF/OTF files. No JSON config — all
     * parameters fall back to EngineSetting defaults. Pipeline:
     * 1. Rasterize TTF glyphs into FontTileData list
     * 2. Pack glyph tiles into atlas, derive atlasPixelSize
     * 3. Composite and upload atlas image to GPU
     * 4. Build GlyphMetricStruct table from packed positions
     * 5. Resolve label VAOHandle once for this font
     * 6. Create each glyph DynamicModelHandle, pass to DynamicGeometryManager to
     * fill
     * 7. Construct and return FontHandle
     */

    // Internal
    private DynamicGeometryManager dynamicGeometryManager;
    private MaterialManager materialManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
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

        ObjectArrayList<FontTileData> tiles = FontRasterizerUtility.rasterize(
                fontFile, size, charset, this);

        if (tiles.isEmpty())
            throwException("[FontManager] No glyphs rasterized for font: " + name);

        int atlasPixelSize = AtlasUtility.pack(tiles);
        BufferedImage atlasImage = compositeAtlas(tiles, atlasPixelSize);
        int gpuHandle = GLSLUtility.pushTexture2D(atlasImage);
        Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs = buildGlyphTable(tiles, atlasPixelSize);
        VAOHandle vaoHandle = vaoManager.getVAOHandleFromVAOName(EngineSetting.FONT_DEFAULT_VAO);
        Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels = buildGlyphModels(
                glyphs, materialID, vaoHandle, atlasPixelSize);

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
            g.drawImage(
                    tile.getImage(),
                    tile.getAtlasX(), tile.getAtlasY(),
                    tile.getAtlasX() + tile.getTileWidth(),
                    tile.getAtlasY() + tile.getTileHeight(),
                    0, 0, tile.getTileWidth(), tile.getTileHeight(),
                    null);
        }

        g.dispose();

        return canvas;
    }

    // Glyph Table \\

    private Int2ObjectOpenHashMap<GlyphMetricStruct> buildGlyphTable(
            ObjectArrayList<FontTileData> tiles,
            int atlasPixelSize) {

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

            DynamicModelHandle model = create(DynamicModelHandle.class);
            model.constructor(materialID, vaoHandle);
            dynamicGeometryManager.buildGlyphModel(model, g, atlasPixelSize);
            models.put(entry.getIntKey(), model);
        }

        return models;
    }

    // Helpers \\

    FontTileData createFontTile() {
        return new FontTileData();
    }
}