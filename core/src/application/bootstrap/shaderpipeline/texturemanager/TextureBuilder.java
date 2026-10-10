package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import javax.imageio.ImageIO;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureAtlasStruct;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import engine.assets.atlas.AtlasUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TextureBuilder extends BuilderPackage {

    /*
     * Builds TextureArrayStructs from image files: creates tiles, records each
     * tile's average albedo and its reveal, packs the atlas and composites one
     * layer per alias. Only aliases found in the sources are registered, so
     * UBO seeding writes exactly those. The average is weighted by coverage,
     * so a cutout texture's transparent pixels never darken it. The reveal
     * follows the surface shader's full draw of a covering texel for texel —
     * shown once its growth is reached and its alpha clears the cutoff, tinted
     * as far as its chroma allows — so a distant face approximating the tile
     * shows exactly the share and color a full draw would.
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
        resolveReveals(tiles);

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

    // Reveals \\

    private void resolveReveals(ObjectArrayList<TextureTileStruct> tiles) {

        int albedoAlias = aliasLibrarySystem.get(EngineSetting.SHADER_ALIAS_ALBEDO);
        int growthAlias = aliasLibrarySystem.get(EngineSetting.SHADER_ALIAS_GROWTH);

        if (growthAlias == EngineSetting.INDEX_NOT_FOUND)
            throwException("Alias: " + EngineSetting.SHADER_ALIAS_GROWTH + " could not be found in the system");

        int albedoFallback = aliasLibrarySystem.getDefaultColor(albedoAlias).getRGB();
        int growthFallback = aliasLibrarySystem.getDefaultColor(growthAlias).getRGB();

        for (int i = 0; i < tiles.size(); i++) {

            TextureTileStruct tile = tiles.get(i);

            tile.setReveal(revealImage(
                    tile.getImage(albedoAlias),
                    tile.getImage(growthAlias),
                    tile.getTileWidth(),
                    tile.getTileHeight(),
                    albedoFallback,
                    growthFallback));
        }
    }

    private TextureRevealStruct revealImage(
            BufferedImage albedo,
            BufferedImage growth,
            int width,
            int height,
            int albedoFallback,
            int growthFallback) {

        int levelMax = EngineSetting.COVERAGE_LEVEL_MAX;
        int channelMax = EngineSetting.PACKED_COLOR_CHANNEL_MASK;
        int[] firstShownCounts = new int[levelMax + 1];

        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double tintableRed = 0.0;
        double tintableGreen = 0.0;
        double tintableBlue = 0.0;
        int shown = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int argb = albedo != null ? albedo.getRGB(x, y) : albedoFallback;
                int alpha = (argb >>> EngineSetting.PACKED_COLOR_ALPHA_SHIFT) & channelMax;

                if (alpha / EngineSetting.COLOR_CHANNEL_BYTE_MAX < EngineSetting.COVERAGE_ALPHA_CUTOFF)
                    continue;

                int growthValue = PackedColorUtility.red(growth != null ? growth.getRGB(x, y) : growthFallback);
                firstShownCounts[(growthValue * levelMax + channelMax - 1) / channelMax]++;

                float tintWeight = resolveTintWeight(argb);

                red += PackedColorUtility.red(argb);
                green += PackedColorUtility.green(argb);
                blue += PackedColorUtility.blue(argb);
                tintableRed += PackedColorUtility.red(argb) * tintWeight;
                tintableGreen += PackedColorUtility.green(argb) * tintWeight;
                tintableBlue += PackedColorUtility.blue(argb) * tintWeight;
                shown++;
            }
        }

        float[] levelShares = new float[levelMax + 1];
        float texelCount = Math.max(width * height, 1);
        int shownByLevel = 0;

        for (int level = 0; level <= levelMax; level++) {
            shownByLevel += firstShownCounts[level];
            levelShares[level] = shownByLevel / texelCount;
        }

        if (shown == 0) {
            int fallbackColor = PackedColorUtility.pack(
                    PackedColorUtility.red(albedoFallback),
                    PackedColorUtility.green(albedoFallback),
                    PackedColorUtility.blue(albedoFallback));
            return new TextureRevealStruct(levelShares, fallbackColor, fallbackColor);
        }

        return new TextureRevealStruct(
                levelShares,
                PackedColorUtility.pack((float) (red / shown), (float) (green / shown), (float) (blue / shown)),
                PackedColorUtility.pack(
                        (float) (tintableRed / shown),
                        (float) (tintableGreen / shown),
                        (float) (tintableBlue / shown)));
    }

    // How far a texel takes a biome's tint, full while unsaturated and none once its chroma clears the band
    private float resolveTintWeight(int argb) {

        int max = Math.max(PackedColorUtility.red(argb),
                Math.max(PackedColorUtility.green(argb), PackedColorUtility.blue(argb)));
        int min = Math.min(PackedColorUtility.red(argb),
                Math.min(PackedColorUtility.green(argb), PackedColorUtility.blue(argb)));
        float chroma = (max - min) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;

        return 1f - SkyColorUtility.smoothstep(
                EngineSetting.COVERAGE_TINT_CHROMA_LOW, EngineSetting.COVERAGE_TINT_CHROMA_HIGH, chroma);
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