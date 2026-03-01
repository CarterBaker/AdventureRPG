package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

/*
 * Constructs TextureArrayData from raw image files during bootstrap. Handles
 * tile creation, atlas packing, and layer composition per alias. All produced
 * objects are DataPackage types and must not be held after bootstrap completes.
 */
class InternalBuildSystem extends SystemPackage {

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
        aliasLibrarySystem = get(AliasLibrarySystem.class);
    }

    // Main \\

    TextureArrayData buildTextureArray(List<File> imageFiles, File sourceDirectory, String arrayName) {
        LinkedHashMap<String, TextureTileData> textureTiles = createTextureTiles(imageFiles, sourceDirectory,
                arrayName);
        TextureAtlasData[] textureAtlases = createTextureAtlases(textureTiles);
        return createTextureArray(textureTiles, arrayName, textureAtlases);
    }

    // Texture Tiles \\

    private LinkedHashMap<String, TextureTileData> createTextureTiles(
            List<File> imageFiles,
            File sourceDirectory,
            String arrayName) {

        LinkedHashMap<String, TextureTileData> textureTiles = new LinkedHashMap<>();
        String atlasName = sourceDirectory.getName();
        int aliasCount = aliasLibrarySystem.getAliasCount();

        for (File file : imageFiles) {

            BufferedImage img;

            try {
                img = ImageIO.read(file);
            } catch (Exception e) {
                return throwException("File: " + file + ", could not be read as an image");
            }

            String fileName = FileUtility.getFileName(file);
            String[] parts = FileUtility.splitFileNameByUnderscore(fileName);

            String instanceName = parts[0];
            String aliasType = parts[1];
            String fullName = arrayName + "/" + instanceName;

            int aliasId = aliasLibrarySystem.getOrDefault(aliasType);

            if (aliasId == -1)
                throwException("Alias: " + aliasType + ", could not be found in the system");

            TextureTileData textureTile = textureTiles.get(fullName);

            if (textureTile == null) {
                textureTile = create(TextureTileData.class);
                textureTile.constructor(textureCount++, fullName, atlasName, aliasCount);
                textureTiles.put(fullName, textureTile);
            }

            textureTile.setImage(img, aliasId);
        }

        return organizeTextureTiles(textureTiles);
    }

    private LinkedHashMap<String, TextureTileData> organizeTextureTiles(
            LinkedHashMap<String, TextureTileData> textureTiles) {

        LinkedHashMap<String, TextureTileData> sortedTiles = new LinkedHashMap<>();
        textureTiles.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((t1, t2) -> Integer.compare(t1.getID(), t2.getID())))
                .forEachOrdered(entry -> sortedTiles.put(entry.getKey(), entry.getValue()));

        return sortedTiles;
    }

    // Texture Atlas \\

    private TextureAtlasData[] createTextureAtlases(LinkedHashMap<String, TextureTileData> textureTiles) {

        TextureAtlasData[] textureAtlases = new TextureAtlasData[aliasLibrarySystem.getAliasCount()];
        int atlasSize = calculateAtlasSize(textureTiles.size());

        assignTilePositions(textureTiles, atlasSize);

        for (int alias = 0; alias < aliasLibrarySystem.getAliasCount(); alias++)
            textureAtlases[alias] = createTextureAtlas(alias, atlasSize, textureTiles);

        return textureAtlases;
    }

    private void assignTilePositions(LinkedHashMap<String, TextureTileData> textureTiles, int atlasSize) {

        int currentIndex = 0;

        for (TextureTileData tile : textureTiles.values()) {
            int x = currentIndex % atlasSize;
            int y = currentIndex / atlasSize;
            tile.setAtlasPosition(x, y);
            currentIndex++;
        }
    }

    private TextureAtlasData createTextureAtlas(
            int alias,
            int atlasSize,
            LinkedHashMap<String, TextureTileData> textureTiles) {

        int atlasPixelWidth = atlasSize * EngineSetting.BLOCK_TEXTURE_SIZE;
        int atlasPixelHeight = atlasSize * EngineSetting.BLOCK_TEXTURE_SIZE;

        BufferedImage atlasImage = new BufferedImage(
                atlasPixelWidth,
                atlasPixelHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphic = atlasImage.createGraphics();
        Color defaultColor = aliasLibrarySystem.getDefaultColor(alias);

        for (TextureTileData tile : textureTiles.values()) {

            int x = tile.getAtlasX() * EngineSetting.BLOCK_TEXTURE_SIZE;
            int y = tile.getAtlasY() * EngineSetting.BLOCK_TEXTURE_SIZE;

            BufferedImage tileImage = tile.getImage(alias);

            if (tileImage != null) {
                graphic.drawImage(tileImage,
                        x, y + EngineSetting.BLOCK_TEXTURE_SIZE,
                        x + EngineSetting.BLOCK_TEXTURE_SIZE, y,
                        0, 0, tileImage.getWidth(), tileImage.getHeight(),
                        null);
            } else {
                graphic.setColor(new Color(
                        defaultColor.getRed(),
                        defaultColor.getGreen(),
                        defaultColor.getBlue(),
                        defaultColor.getAlpha()));
                graphic.fillRect(x, y, EngineSetting.BLOCK_TEXTURE_SIZE, EngineSetting.BLOCK_TEXTURE_SIZE);
            }
        }

        graphic.dispose();

        TextureAtlasData textureAtlasData = create(TextureAtlasData.class);
        textureAtlasData.constructor(atlasSize, atlasImage);

        return textureAtlasData;
    }

    private int calculateAtlasSize(int tileCount) {
        int dimension = (int) Math.ceil(Math.sqrt(tileCount));
        return (int) Math.pow(2, Math.ceil(Math.log(dimension) / Math.log(2)));
    }

    // Texture Array \\

    private TextureArrayData createTextureArray(
            LinkedHashMap<String, TextureTileData> textureTiles,
            String arrayName,
            TextureAtlasData[] textureAtlases) {

        TextureArrayData textureArrayData = create(TextureArrayData.class);
        textureArrayData.constructor(
                arrayCount++,
                arrayName,
                textureAtlases[0].getAtlasSize(),
                textureAtlases);

        for (TextureTileData tile : textureTiles.values())
            textureArrayData.createTile(tile.getAtlasX(), tile.getAtlasY(), tile);

        return textureArrayData;
    }
}