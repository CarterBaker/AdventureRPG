package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import javax.imageio.ImageIO;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureAtlasStruct;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import engine.assets.atlas.AtlasUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TextureBuilder extends BuilderPackage {

    /*
     * Builds TextureArrayStructs from image files: creates tiles, records each
     * tile's average albedo, packs the atlas and composites one layer per
     * alias. Only aliases found in the sources are registered, so UBO seeding
     * writes exactly those. The average is weighted by coverage, so a cutout
     * texture's transparent pixels never darken it.
     */

    // Internal
    private AliasLibrarySystem aliasLibrarySystem;

    // Base \\

    @Override
    protected void get() {
        this.aliasLibrarySystem = get(AliasLibrarySystem.class);
    }

    // Build \\

    TextureArrayStruct build(ObjectArrayList<File> imageFiles, File sourceDirectory, String arrayName) {

        Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> tileMap = createTextureTiles(
                imageFiles, sourceDirectory, arrayName);

        if (tileMap.isEmpty())
            return null;

        ObjectArrayList<TextureTileStruct> tiles = new ObjectArrayList<>(tileMap.values());
        resolveAverageColors(tiles);

        int atlasPixelSize = AtlasUtility.pack(tiles);
        TextureAtlasStruct[] atlasLayers = compositeAtlasLayers(tiles, atlasPixelSize);

        return createTextureArray(tileMap, arrayName, atlasPixelSize, atlasLayers);
    }

    // Texture Tiles \\

    private Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> createTextureTiles(
            ObjectArrayList<File> imageFiles,
            File sourceDirectory,
            String arrayName) {

        Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> tileMap = new Object2ObjectLinkedOpenHashMap<>();
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
                        fullName,
                        atlasName,
                        aliasCount);
                tileMap.put(fullName, tile);
            }

            tile.setImage(img, aliasId);
        }

        return organizeTextureTiles(tileMap);
    }

    private Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> organizeTextureTiles(
            Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> tileMap) {

        ObjectArrayList<String> tileNames = new ObjectArrayList<>(tileMap.keySet());
        tileNames.sort(String::compareTo);

        Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> sorted = new Object2ObjectLinkedOpenHashMap<>();

        for (int i = 0; i < tileNames.size(); i++)
            sorted.put(tileNames.get(i), tileMap.get(tileNames.get(i)));

        return sorted;
    }

    // Average Colors \\

    private void resolveAverageColors(ObjectArrayList<TextureTileStruct> tiles) {

        int albedoAlias = aliasLibrarySystem.get(EngineSetting.SHADER_ALIAS_ALBEDO);

        if (albedoAlias == EngineSetting.INDEX_NOT_FOUND)
            throwException("Alias: " + EngineSetting.SHADER_ALIAS_ALBEDO + " could not be found in the system");

        Color fallback = aliasLibrarySystem.getDefaultColor(albedoAlias);
        int fallbackColor = PackedColorUtility.pack(fallback.getRed(), fallback.getGreen(), fallback.getBlue());

        for (int i = 0; i < tiles.size(); i++) {

            TextureTileStruct tile = tiles.get(i);
            BufferedImage albedo = tile.getImage(albedoAlias);

            tile.setAverageColor(albedo != null ? averageImage(albedo, fallbackColor) : fallbackColor);
        }
    }

    private int averageImage(BufferedImage image, int fallbackColor) {

        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double coverage = 0.0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                int argb = image.getRGB(x, y);
                int alpha = (argb >>> EngineSetting.PACKED_COLOR_ALPHA_SHIFT) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;

                red += PackedColorUtility.red(argb) * alpha;
                green += PackedColorUtility.green(argb) * alpha;
                blue += PackedColorUtility.blue(argb) * alpha;
                coverage += alpha;
            }
        }

        if (coverage <= 0.0)
            return fallbackColor;

        return PackedColorUtility.pack((float) (red / coverage), (float) (green / coverage), (float) (blue / coverage));
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
            Object2ObjectLinkedOpenHashMap<String, TextureTileStruct> tileMap,
            String arrayName,
            int atlasPixelSize,
            TextureAtlasStruct[] atlasLayers) {

        TextureArrayStruct arrayStruct = new TextureArrayStruct(
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