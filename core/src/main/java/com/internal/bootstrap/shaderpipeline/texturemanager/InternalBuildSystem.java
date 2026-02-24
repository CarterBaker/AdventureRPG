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

class InternalBuildSystem extends SystemPackage {

    // Internal
    private AliasLibrarySystem aliasLibrarySystem;
    private int textureCount;
    private int arrayCount;

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

    TextureArrayInstance buildTextureArray(List<File> imageFiles, File sourceDirectory, String arrayName) {
        LinkedHashMap<String, TextureTileInstance> textureTiles = createTextureTiles(imageFiles, sourceDirectory,
                arrayName);
        TextureAtlasInstance[] textureAtlases = createTextureAtlases(textureTiles);
        return createTextureArray(textureTiles, arrayName, textureAtlases);
    }

    // Texture Tiles \\

    private LinkedHashMap<String, TextureTileInstance> createTextureTiles(
            List<File> imageFiles,
            File sourceDirectory,
            String arrayName) {

        LinkedHashMap<String, TextureTileInstance> textureTiles = new LinkedHashMap<>();
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

            TextureTileInstance textureTile = textureTiles.get(fullName);

            if (textureTile == null) {
                textureTile = create(TextureTileInstance.class);
                textureTile.constructor(
                        textureCount++,
                        fullName,
                        atlasName,
                        aliasCount);
                textureTiles.put(fullName, textureTile);
            }

            textureTile.setImage(img, aliasId);
        }

        return organizeTextureTiles(textureTiles);
    }

    private LinkedHashMap<String, TextureTileInstance> organizeTextureTiles(
            LinkedHashMap<String, TextureTileInstance> textureTiles) {

        LinkedHashMap<String, TextureTileInstance> sortedTiles = new LinkedHashMap<>();
        textureTiles.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((t1, t2) -> Integer.compare(t1.getID(), t2.getID())))
                .forEachOrdered(entry -> sortedTiles.put(entry.getKey(), entry.getValue()));

        return sortedTiles;
    }

    // Texture Atlas \\

    private TextureAtlasInstance[] createTextureAtlases(
            LinkedHashMap<String, TextureTileInstance> textureTiles) {

        TextureAtlasInstance[] textureAtlases = new TextureAtlasInstance[aliasLibrarySystem.getAliasCount()];
        int atlasSize = calculateAtlasSize(textureTiles.size());

        assignTilePositions(textureTiles, atlasSize);

        for (int alias = 0; alias < aliasLibrarySystem.getAliasCount(); alias++)
            textureAtlases[alias] = createTextureAtlas(alias, atlasSize, textureTiles);

        return textureAtlases;
    }

    private void assignTilePositions(
            LinkedHashMap<String, TextureTileInstance> textureTiles,
            int atlasSize) {

        int currentIndex = 0;

        for (TextureTileInstance tile : textureTiles.values()) {
            int x = currentIndex % atlasSize;
            int y = currentIndex / atlasSize;
            tile.setAtlasPosition(x, y);
            currentIndex++;
        }
    }

    private TextureAtlasInstance createTextureAtlas(
            int alias,
            int atlasSize,
            LinkedHashMap<String, TextureTileInstance> textureTiles) {

        int atlasPixelWidth = atlasSize * EngineSetting.BLOCK_TEXTURE_SIZE;
        int atlasPixelHeight = atlasSize * EngineSetting.BLOCK_TEXTURE_SIZE;

        BufferedImage atlasImage = new BufferedImage(
                atlasPixelWidth,
                atlasPixelHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphic = atlasImage.createGraphics();

        Color defaultColor = aliasLibrarySystem.getDefaultColor(alias);

        for (TextureTileInstance tile : textureTiles.values()) {

            int x = tile.getAtlasX() * EngineSetting.BLOCK_TEXTURE_SIZE;
            int y = tile.getAtlasY() * EngineSetting.BLOCK_TEXTURE_SIZE;

            BufferedImage tileImage = tile.getImage(alias);

            if (tileImage != null) {
                // Flip vertically so OpenGL's bottom-left origin matches Java's top-left
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

        TextureAtlasInstance textureAtlasInstance = create(TextureAtlasInstance.class);
        textureAtlasInstance.constructor(atlasSize, atlasImage);

        return textureAtlasInstance;
    }

    private int calculateAtlasSize(int tileCount) {
        int dimension = (int) Math.ceil(Math.sqrt(tileCount));
        return (int) Math.pow(2, Math.ceil(Math.log(dimension) / Math.log(2)));
    }

    // Texture Array \\

    private TextureArrayInstance createTextureArray(
            LinkedHashMap<String, TextureTileInstance> textureTiles,
            String arrayName,
            TextureAtlasInstance[] textureAtlases) {

        TextureArrayInstance textureArray = create(TextureArrayInstance.class);
        textureArray.constructor(
                arrayCount++,
                arrayName,
                textureAtlases[0].getAtlasSize(),
                textureAtlases);

        for (TextureTileInstance tile : textureTiles.values())
            textureArray.createTile(tile.getAtlasX(), tile.getAtlasY(), tile);

        return textureArray;
    }
}