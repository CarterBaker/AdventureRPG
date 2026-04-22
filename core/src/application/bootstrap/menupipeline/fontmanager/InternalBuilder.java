package application.bootstrap.menupipeline.fontmanager;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import application.bootstrap.menupipeline.font.FontHandle;
import application.bootstrap.menupipeline.font.FontTileData;
import application.bootstrap.menupipeline.font.GlyphMetricStruct;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureAtlasStruct;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import engine.assets.atlas.AtlasUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Builds FontHandles directly from TTF/OTF files. No JSON config — all
     * parameters fall back to EngineSetting defaults. Pipeline:
     * 1. Rasterize TTF glyphs into FontTileData list
     * 2. Wrap each glyph as a TextureTileStruct (aliasCount=1, layer 0 = albedo)
     * 3. Pack tiles into atlas via AtlasUtility
     * 4. Composite single-layer atlas image
     * 5. Wrap in TextureArrayStruct, push to GPU via pushTextureArray
     * 6. Register with TextureManager — glyphs appear as fontName/glyph tiles
     * 7. Retrieve TextureHandles per glyph, build metric table, return FontHandle
     */

    // Internal
    private MaterialManager materialManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {

        this.materialManager = get(MaterialManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    FontHandle build(String name, File fontFile) {

        int size = EngineSetting.FONT_RASTER_SIZE;
        String charset = EngineSetting.FONT_DEFAULT_CHARSET;

        ObjectArrayList<FontTileData> tiles = FontRasterizerUtility.rasterize(
                fontFile, size, charset, this);

        if (tiles.isEmpty())
            throwException("[FontManager] No glyphs rasterized for font: " + name);

        ObjectArrayList<TextureTileStruct> tileStructs = buildTileStructs(tiles, name);

        int atlasPixelSize = AtlasUtility.pack(tileStructs);
        TextureArrayStruct arrayStruct = buildArrayStruct(name, atlasPixelSize, tileStructs, tiles);

        int gpuHandle = GLSLUtility.pushTextureArray(arrayStruct.getRawImageArray());
        textureManager.register(arrayStruct, gpuHandle);

        arrayStruct.clearAtlases();
        for (int i = 0; i < tileStructs.size(); i++)
            tileStructs.get(i).clearImages();

        // Material requested AFTER atlas is registered so the resolver finds it
        int materialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.FONT_DEFAULT_MATERIAL);

        Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs = new Int2ObjectOpenHashMap<>(tiles.size());
        Int2ObjectOpenHashMap<TextureHandle> glyphHandles = new Int2ObjectOpenHashMap<>(tiles.size());

        for (int i = 0; i < tiles.size(); i++) {

            FontTileData tile = tiles.get(i);
            String tileName = name + "/" + new String(Character.toChars(tile.getCodepoint()));
            TextureHandle glyphHandle = textureManager.getTextureHandleFromTextureName(tileName);

            glyphs.put(tile.getCodepoint(), new GlyphMetricStruct(
                    tile.getTileWidth(), tile.getTileHeight(),
                    tile.getBearingX(), tile.getBearingY(),
                    tile.getAdvance()));

            glyphHandles.put(tile.getCodepoint(), glyphHandle);

            tile.clearImage();
        }

        TextureHandle atlasHandle = textureManager.getTextureHandleFromArrayName(name);

        FontHandle handle = create(FontHandle.class);
        handle.constructor(name, atlasHandle, materialID, atlasPixelSize, size, glyphs, glyphHandles);

        return handle;
    }

    // Tile Structs \\

    private ObjectArrayList<TextureTileStruct> buildTileStructs(
            ObjectArrayList<FontTileData> tiles,
            String fontName) {

        ObjectArrayList<TextureTileStruct> structs = new ObjectArrayList<>(tiles.size());

        for (int i = 0; i < tiles.size(); i++) {

            FontTileData tile = tiles.get(i);
            String tileName = fontName + "/" + new String(Character.toChars(tile.getCodepoint()));

            TextureTileStruct struct = new TextureTileStruct(
                    RegistryUtility.toIntID(tileName),
                    tileName,
                    fontName,
                    1);

            struct.setImage(tile.getImage(), 0);
            structs.add(struct);
        }

        return structs;
    }

    // Array Struct \\

    private TextureArrayStruct buildArrayStruct(
            String name,
            int atlasPixelSize,
            ObjectArrayList<TextureTileStruct> tileStructs,
            ObjectArrayList<FontTileData> tiles) {

        BufferedImage canvas = new BufferedImage(
                atlasPixelSize, atlasPixelSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();

        for (int i = 0; i < tileStructs.size(); i++) {

            TextureTileStruct struct = tileStructs.get(i);
            BufferedImage img = struct.getImage(0);

            g.drawImage(
                    img,
                    struct.getAtlasX(), struct.getAtlasY(),
                    struct.getAtlasX() + struct.getTileWidth(),
                    struct.getAtlasY() + struct.getTileHeight(),
                    0, 0, img.getWidth(), img.getHeight(),
                    null);
        }

        g.dispose();

        TextureAtlasStruct atlasLayer = new TextureAtlasStruct(atlasPixelSize, canvas);
        TextureArrayStruct arrayStruct = new TextureArrayStruct(
                RegistryUtility.toIntID(name),
                name,
                atlasPixelSize,
                new TextureAtlasStruct[] { atlasLayer });

        for (int i = 0; i < tileStructs.size(); i++)
            arrayStruct.registerTile(tileStructs.get(i));

        return arrayStruct;
    }

    // Helpers \\

    FontTileData createFontTile() {
        return new FontTileData();
    }
}