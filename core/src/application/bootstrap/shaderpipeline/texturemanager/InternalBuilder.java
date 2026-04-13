package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureAtlasStruct;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import engine.root.BuilderPackage;
import engine.util.AtlasUtility;
import engine.util.FileUtility;
import engine.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Constructs TextureArrayStructs from raw image files. Handles tile creation,
 * atlas packing via AtlasUtility, and layer compositing per alias. All produced
 * objects are StructPackage types and GC with the loader after bootstrap.
 *
 * Only alias IDs actually encountered in source files are registered on the
 * resulting TextureArrayStruct — seedUBO uses this to write only the uniforms
 * this atlas provides.
 */
class InternalBuilder extends BuilderPackage {

    // Internal
    private AliasLibrarySystem aliasLibrarySystem;

    // Base \\

    @Override
    protected void get() {
        this.aliasLibrarySystem = get(AliasLibrarySystem.class);
    }

    // Build \\

    TextureArrayStruct build(List<File> imageFiles, File sourceDirectory, String arrayName) {

        LinkedHashMap<String, TextureTileStruct> tileMap = createTextureTiles(
                imageFiles, sourceDirectory, arrayName);

        if (tileMap.isEmpty())
            return null;

        ObjectArrayList<TextureTileStruct> tiles = new ObjectArrayList<>(tileMap.values());
        int atlasPixelSize = AtlasUtility.pack(tiles);
        TextureAtlasStruct[] atlasLayers = compositeAtlasLayers(tiles, atlasPixelSize);

        return createTextureArray(tileMap, arrayName, atlasPixelSize, atlasLayers);
    }

    // Texture Tiles \\

    private LinkedHashMap<String, TextureTileStruct> createTextureTiles(
            List<File> imageFiles,
            File sourceDirectory,
            String arrayName) {

        LinkedHashMap<String, TextureTileStruct> tileMap = new LinkedHashMap<>();
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

            TextureTileStruct tile = tileMap.get(fullName);

            if (tile == null) {
                tile = new TextureTileStruct(
                        RegistryUtility.toIntID(fullName),
                        fullName,
                        atlasName,
                        aliasCount);
                tileMap.put(fullName, tile);
            }

            tile.setImage(img, aliasId);
        }

        return organizeTextureTiles(tileMap);
    }

    private LinkedHashMap<String, TextureTileStruct> organizeTextureTiles(
            LinkedHashMap<String, TextureTileStruct> tileMap) {

        LinkedHashMap<String, TextureTileStruct> sorted = new LinkedHashMap<>();
        tileMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((a, b) -> Integer.compare(a.getID(), b.getID())))
                .forEachOrdered(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // Atlas Compositing \\

    private TextureAtlasStruct[] compositeAtlasLayers(
            ObjectArrayList<TextureTileStruct> tiles,
            int atlasPixelSize) {

        int aliasCount = aliasLibrarySystem.getAliasCount();
        TextureAtlasStruct[] atlasLayers = new TextureAtlasStruct[aliasCount];

        for (int alias = 0; alias < aliasCount; alias++) {

            BufferedImage canvas = new BufferedImage(
                    atlasPixelSize, atlasPixelSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();

            for (int i = 0; i < tiles.size(); i++) {

                TextureTileStruct tile = tiles.get(i);
                BufferedImage layer = tile.getImage(alias);
                int x = tile.getAtlasX();
                int y = tile.getAtlasY();
                int w = tile.getTileWidth();
                int h = tile.getTileHeight();

                if (layer != null) {
                    g.drawImage(layer,
                            x, y + h,
                            x + w, y,
                            0, 0, layer.getWidth(), layer.getHeight(), null);
                } else {
                    Color fill = aliasLibrarySystem.getDefaultColor(alias);
                    g.setColor(new Color(
                            fill.getRed(), fill.getGreen(),
                            fill.getBlue(), fill.getAlpha()));
                    g.fillRect(x, y, w, h);
                }
            }

            g.dispose();
            atlasLayers[alias] = new TextureAtlasStruct(atlasPixelSize, canvas);
        }

        return atlasLayers;
    }

    // Texture Array \\

    private TextureArrayStruct createTextureArray(
            LinkedHashMap<String, TextureTileStruct> tileMap,
            String arrayName,
            int atlasPixelSize,
            TextureAtlasStruct[] atlasLayers) {

        TextureArrayStruct arrayStruct = new TextureArrayStruct(
                RegistryUtility.toIntID(arrayName),
                arrayName,
                atlasPixelSize,
                atlasLayers);

        int aliasCount = aliasLibrarySystem.getAliasCount();

        for (TextureTileStruct tile : tileMap.values()) {
            arrayStruct.registerTile(tile);
            for (int alias = 0; alias < aliasCount; alias++)
                if (tile.getImage(alias) != null)
                    arrayStruct.registerFoundAlias(alias);
        }

        return arrayStruct;
    }
}