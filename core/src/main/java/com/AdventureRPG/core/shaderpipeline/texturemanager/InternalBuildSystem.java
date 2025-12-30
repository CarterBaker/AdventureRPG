package com.AdventureRPG.core.shaderpipeline.texturemanager;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.FileUtility;

class InternalBuildSystem extends SystemPackage {

    // Internal
    private AliasLibrarySystem aliasLibrarySystem;
    private int textureCount;
    private int arrayCount;

    @Override
    protected void create() {

        // Internal
        this.textureCount = 0;
        this.arrayCount = 0;
    }

    @Override
    protected void init() {
        aliasLibrarySystem = internal.get(AliasLibrarySystem.class);
    }

    // Main \\

    TextureArrayInstance buildTextureArray(List<File> imageFiles, File sourceDirectory) {

        LinkedHashMap<String, TextureTileInstance> textureTiles = createTextureTiles(imageFiles, sourceDirectory);

        TextureAtlasInstance[] textureAtlases = createTextureAtlases(textureTiles);

        return createTextureArray(
                textureTiles,
                sourceDirectory.getName(),
                textureAtlases);
    }

    // Texture Tiles \\

    private LinkedHashMap<String, TextureTileInstance> createTextureTiles(List<File> imageFiles, File sourceDirectory) {

        LinkedHashMap<String, TextureTileInstance> textureTiles = new LinkedHashMap<>();
        String atlasName = sourceDirectory.getName();
        int aliasCount = aliasLibrarySystem.getAliasCount();

        for (File file : imageFiles) {

            BufferedImage img;

            try {
                img = ImageIO.read(file);
            }

            catch (Exception e) {
                return throwException("File: " + file + ", could not be read as an image");
            }

            String fileName = FileUtility.getFileName(file);
            String[] parts = FileUtility.splitFileNameByUnderscore(fileName);

            String instanceName = parts[0];
            String aliasType = parts[1];

            int aliasId = aliasLibrarySystem.getOrDefault(aliasType);

            if (aliasId == -1)
                throwException(
                        "Alias: " + aliasType + ", could not be found in the system");

            TextureTileInstance textureTile = textureTiles.get(instanceName);

            if (textureTile == null) {
                textureTile = create(TextureTileInstance.class);
                textureTile.init(
                        textureCount++,
                        instanceName,
                        atlasName,
                        aliasCount);
                textureTiles.put(instanceName, textureTile);
            }

            textureTile.setImage(img, aliasId);
        }

        textureTiles = organizeTextureTiles(textureTiles);

        return textureTiles;
    }

    private LinkedHashMap<String, TextureTileInstance> organizeTextureTiles(
            LinkedHashMap<String, TextureTileInstance> textureTiles) {

        LinkedHashMap<String, TextureTileInstance> sortedTiles = new LinkedHashMap<>();
        textureTiles.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((tile1, tile2) -> Integer.compare(tile1.getID(), tile2.getID())))
                .forEachOrdered(entry -> sortedTiles.put(entry.getKey(), entry.getValue()));

        return sortedTiles;
    }

    // Texture Atlas \\

    private TextureAtlasInstance[] createTextureAtlases(
            LinkedHashMap<String, TextureTileInstance> textureTiles) {

        TextureAtlasInstance[] textureAtlases = new TextureAtlasInstance[aliasLibrarySystem.getAliasCount()];
        int atlasSize = calculateAtlasSize(textureTiles.size());

        assignTilePositions(
                textureTiles,
                atlasSize);

        for (int alias = 0; alias < aliasLibrarySystem.getAliasCount(); alias++)
            textureAtlases[alias] = createTextureAtlas(alias, atlasSize, textureTiles);

        return textureAtlases;
    }

    private void assignTilePositions(
            LinkedHashMap<String, TextureTileInstance> textureTiles,
            int atlasSize) {

        int tilesPerRow = atlasSize;
        int currentIndex = 0;

        for (TextureTileInstance tile : textureTiles.values()) {

            int x = currentIndex % tilesPerRow;
            int y = currentIndex / tilesPerRow;

            tile.setAtlasPosition(x, y);
            currentIndex++;
        }
    }

    private TextureAtlasInstance createTextureAtlas(
            int alias,
            int atlasSize,
            LinkedHashMap<String, TextureTileInstance> textureTiles) {

        // Create the atlas image
        int atlasPixelWidth = atlasSize * EngineSetting.BLOCK_TEXTURE_SIZE;
        int atlasPixelHeight = atlasSize * EngineSetting.BLOCK_TEXTURE_SIZE;

        BufferedImage atlasImage = new BufferedImage(
                atlasPixelWidth,
                atlasPixelHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphic = atlasImage.createGraphics();

        // Get default color for this alias
        Color defaultColor = aliasLibrarySystem.getDefaultColor(alias);

        // Fill each tile position
        for (TextureTileInstance tile : textureTiles.values()) {

            int x = tile.getAtlasX() * EngineSetting.BLOCK_TEXTURE_SIZE;
            int y = tile.getAtlasY() * EngineSetting.BLOCK_TEXTURE_SIZE;

            BufferedImage tileImage = tile.getImage(alias);

            // Draw the actual tile image
            if (tileImage != null)
                graphic.drawImage(tileImage, x, y, null);

            // Draw default color for missing layer
            else {
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
        textureAtlasInstance.init(
                atlasSize,
                atlasImage);

        return textureAtlasInstance;
    }

    private int calculateAtlasSize(int tileCount) {
        int dimension = (int) Math.ceil(Math.sqrt(tileCount));
        return (int) Math.pow(2, Math.ceil(Math.log(dimension) / Math.log(2)));
    }

    // Texture Array \\

    private TextureArrayInstance createTextureArray(
            LinkedHashMap<String, TextureTileInstance> textureTiles,
            String textureArrayName,
            TextureAtlasInstance[] textureAtlases) {

        TextureArrayInstance textureArray = create(TextureArrayInstance.class);
        textureArray.init(
                arrayCount++,
                textureArrayName,
                textureAtlases[0].getAtlasSize(),
                textureAtlases);

        for (TextureTileInstance tile : textureTiles.values())
            textureArray.registerTile(
                    tile.getAtlasX(),
                    tile.getAtlasY(),
                    tile);

        return textureArray;
    }
}
