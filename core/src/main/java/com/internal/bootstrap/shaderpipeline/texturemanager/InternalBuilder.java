package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.internal.bootstrap.shaderpipeline.Texture.TextureArrayData;
import com.internal.bootstrap.shaderpipeline.Texture.TextureAtlasData;
import com.internal.bootstrap.shaderpipeline.Texture.TextureTileData;
import com.internal.bootstrap.shaderpipeline.texturemanager.aliassystem.AliasLibrarySystem;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.AtlasUtility;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Constructs TextureArrayData from raw image files. Handles tile creation,
 * atlas packing via AtlasUtility, and layer compositing per alias. All
 * produced objects are DataPackage types and must not be held after bootstrap
 * completes.
 *
 * Only the alias IDs actually encountered in the source files are registered
 * on the resulting TextureArrayData — seedUBO uses this to write only the
 * uniforms this atlas provides.
 */
class InternalBuilder extends BuilderPackage {

    // Internal
    private AliasLibrarySystem aliasLibrarySystem;
    private int textureCount;
    private int arrayCount;

    // Base \\

    @Override
    protected void create() {
        this.textureCount = 0;
        this.arrayCount = 0;
    }

    @Override
    protected void get() {
        this.aliasLibrarySystem = get(AliasLibrarySystem.class);
    }

    // Build \\

    TextureArrayData build(List<File> imageFiles, File sourceDirectory, String arrayName) {

        LinkedHashMap<String, TextureTileData> tileMap = createTextureTiles(imageFiles, sourceDirectory, arrayName);

        if (tileMap.isEmpty())
            return null;

        ObjectArrayList<TextureTileData> tiles = new ObjectArrayList<>(tileMap.values());

        // Pack — writes pixel-space atlasX/Y back onto each tile, returns canvas size
        int atlasPixelSize = AtlasUtility.pack(tiles);

        TextureAtlasData[] atlasLayers = compositeAtlasLayers(tiles, atlasPixelSize);

        return createTextureArray(tileMap, arrayName, atlasPixelSize, atlasLayers);
    }

    // Texture Tiles \\

    private LinkedHashMap<String, TextureTileData> createTextureTiles(
            List<File> imageFiles,
            File sourceDirectory,
            String arrayName) {

        LinkedHashMap<String, TextureTileData> tileMap = new LinkedHashMap<>();
        String atlasName = sourceDirectory.getName();
        int aliasCount = aliasLibrarySystem.getAliasCount();

        for (File file : imageFiles) {

            BufferedImage img;
            try {
                img = ImageIO.read(file);
            } catch (Exception e) {
                return throwException("File: " + file + " could not be read as an image");
            }

            String fileName = FileUtility.getFileName(file);
            String[] parts = FileUtility.splitFileNameByUnderscore(fileName);
            String instanceName = parts[0];
            String aliasType = parts[1];
            String fullName = arrayName + "/" + instanceName;
            int aliasId = aliasLibrarySystem.getOrDefault(aliasType);

            if (aliasId == -1)
                throwException("Alias: " + aliasType + " could not be found in the system");

            TextureTileData tile = tileMap.get(fullName);
            if (tile == null) {
                tile = create(TextureTileData.class);
                tile.constructor(textureCount++, fullName, atlasName, aliasCount);
                tileMap.put(fullName, tile);
            }

            tile.setImage(img, aliasId);
        }

        return organizeTextureTiles(tileMap);
    }

    private LinkedHashMap<String, TextureTileData> organizeTextureTiles(
            LinkedHashMap<String, TextureTileData> tileMap) {

        LinkedHashMap<String, TextureTileData> sorted = new LinkedHashMap<>();
        tileMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.getID(), b.getID())))
                .forEachOrdered(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // Atlas Compositing \\

    /*
     * Composites one BufferedImage per alias layer. Each tile is drawn at its
     * packed pixel position. Missing alias layers are filled with the alias
     * default colour at that tile's own pixel dimensions. Tiles are flipped
     * vertically to match GL bottom-left origin.
     */
    private TextureAtlasData[] compositeAtlasLayers(
            ObjectArrayList<TextureTileData> tiles, int atlasPixelSize) {

        int aliasCount = aliasLibrarySystem.getAliasCount();
        TextureAtlasData[] atlasLayers = new TextureAtlasData[aliasCount];

        for (int alias = 0; alias < aliasCount; alias++) {

            BufferedImage canvas = new BufferedImage(
                    atlasPixelSize, atlasPixelSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();

            for (int i = 0; i < tiles.size(); i++) {
                TextureTileData tile = tiles.get(i);
                BufferedImage layer = tile.getImage(alias);
                int x = tile.getAtlasX();
                int y = tile.getAtlasY();
                int w = tile.getTileWidth();
                int h = tile.getTileHeight();

                if (layer != null) {
                    // Flip vertically for GL bottom-left origin
                    g.drawImage(layer,
                            x, y + h,
                            x + w, y,
                            0, 0, layer.getWidth(), layer.getHeight(), null);
                } else {
                    Color fill = aliasLibrarySystem.getDefaultColor(alias);
                    g.setColor(new Color(fill.getRed(), fill.getGreen(),
                            fill.getBlue(), fill.getAlpha()));
                    g.fillRect(x, y, w, h);
                }
            }

            g.dispose();

            TextureAtlasData atlasData = create(TextureAtlasData.class);
            atlasData.constructor(atlasPixelSize, canvas);
            atlasLayers[alias] = atlasData;
        }

        return atlasLayers;
    }

    // Texture Array \\

    private TextureArrayData createTextureArray(
            LinkedHashMap<String, TextureTileData> tileMap,
            String arrayName,
            int atlasPixelSize,
            TextureAtlasData[] atlasLayers) {

        TextureArrayData arrayData = create(TextureArrayData.class);
        arrayData.constructor(arrayCount++, arrayName, atlasPixelSize, atlasLayers);

        int aliasCount = aliasLibrarySystem.getAliasCount();

        for (TextureTileData tile : tileMap.values()) {
            arrayData.registerTile(tile);
            for (int alias = 0; alias < aliasCount; alias++)
                if (tile.getImage(alias) != null)
                    arrayData.registerFoundAlias(alias);
        }

        return arrayData;
    }
}