package application.bootstrap.menupipeline.fontmanager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import application.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.menupipeline.font.FontHandle;
import application.bootstrap.menupipeline.font.FontTileData;
import application.bootstrap.menupipeline.font.GlyphMetricStruct;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import engine.assets.atlas.AtlasUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
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
    private MaterialManager materialManager;
    private VAOManager vaoManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.materialManager = get(MaterialManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    // Build \\

    FontHandle build(String name, File fontFile) {

        int materialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.FONT_DEFAULT_MATERIAL);
        int size = EngineSetting.FONT_RASTER_SIZE;
        String charset = EngineSetting.FONT_DEFAULT_CHARSET;

        ObjectArrayList<FontTileData> tiles = FontRasterizerUtility.rasterize(
                fontFile, size, charset, this);

        if (tiles.isEmpty())
            throwException("[FontManager] No glyphs rasterized for font: " + name);

        int atlasPixelSize = AtlasUtility.pack(tiles);
        BufferedImage atlasImage = compositeAtlas(tiles, atlasPixelSize);
        int gpuHandle = GLSLUtility.pushTexture2D(atlasImage);
        Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs = buildGlyphTable(tiles, atlasPixelSize);

        for (int i = 0; i < tiles.size(); i++)
            tiles.get(i).clearImage();

        FontHandle handle = create(FontHandle.class);
        handle.constructor(name, gpuHandle, materialID, atlasPixelSize, size, glyphs);

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

    // Helpers \\

    FontTileData createFontTile() {
        return new FontTileData();
    }
}
