package application.bootstrap.shaderpipeline.texturemanager;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.CRC32C;

import javax.imageio.ImageIO;

import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureMapType;
import application.bootstrap.shaderpipeline.texture.TextureRevealStruct;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import engine.graphics.color.PackedColorUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureBakeUtility extends EngineUtility {

    /*
     * Turns a tile's source images into the files the game streams: each
     * image resampled to the tile's power-of-two size (sources below the base
     * tier upscaled nearest-neighbour, which keeps their exact texels), packed
     * channel by channel into the albedo, normal, material and extra maps its
     * alias names, filtered into a full mip chain, and block-compressed per
     * tier it fills, at the fallback size and for the extra set. The average
     * colour and reveal are measured on the sources exactly as the far tier
     * expects. The bake tool runs it over every tile whose sources changed;
     * the editor runs it on one tile it paints. Layers hold rows bottom first,
     * as the GPU reads them.
     */

    // Aliases \\

    public static AliasLibraryStruct loadAliasLibrary(File aliasRoot) {

        FileUtility.verifyDirectory(aliasRoot, "Alias root directory not found: " + aliasRoot.getAbsolutePath());

        AliasLibraryStruct library = new AliasLibraryStruct();
        ObjectArrayList<File> aliasFiles = FileUtility.collectFilesShallow(aliasRoot,
                EngineSetting.ARPG_FILE_EXTENSIONS);
        aliasFiles.sort(null);

        for (int i = 0; i < aliasFiles.size(); i++)
            loadAlias(aliasFiles.get(i), library);

        return library;
    }

    private static void loadAlias(File file, AliasLibraryStruct library) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct colorArray = ArpgUtility.validateArray(arpg, EngineSetting.TEXTURE_ALIAS_KEY_DEFAULT_COLOR,
                EngineSetting.TEXTURE_ALIAS_DEFAULT_COLOR_CHANNELS);
        ArpgArrayStruct namesArray = ArpgUtility.validateArray(arpg, EngineSetting.TEXTURE_ALIAS_KEY_ALIASES);
        TextureMapType mapType = ArpgUtility.toEnum(
                ArpgUtility.validateString(arpg, EngineSetting.TEXTURE_ALIAS_KEY_MAP), TextureMapType.class);
        int channel = ArpgUtility.validateInt(arpg, EngineSetting.TEXTURE_ALIAS_KEY_CHANNEL);

        if (channel < 0 || channel + mapType.getSourceChannels() > EngineSetting.COLOR_CHANNEL_COUNT)
            throwException("Alias '" + file.getName() + "' fills channels past its map: channel " + channel);

        Color defaultColor = new Color(
                colorArray.get(0).getAsFloat(),
                colorArray.get(1).getAsFloat(),
                colorArray.get(2).getAsFloat(),
                EngineSetting.SHADER_ALIAS_DEFAULT_ALPHA);
        ObjectArrayList<String> names = new ObjectArrayList<>(namesArray.size());

        for (int i = 0; i < namesArray.size(); i++)
            names.add(namesArray.get(i).getAsString());

        library.addAlias(new AliasStruct(FileUtility.getFileName(file), defaultColor, mapType, channel), names);
    }

    // Sources \\

    // Every tile under the texture root, pack by pack, each pack's tiles in name order
    public static ObjectArrayList<TextureSourceStruct> collectSources(File textureRoot, AliasLibraryStruct library) {

        FileUtility.verifyDirectory(textureRoot, "Texture root directory not found: " + textureRoot.getAbsolutePath());

        ObjectArrayList<TextureSourceStruct> sources = new ObjectArrayList<>();
        ObjectArrayList<File> directories = FileUtility.collectAllSubdirectories(textureRoot);
        directories.sort(null);

        for (int d = 0; d < directories.size(); d++) {

            File directory = directories.get(d);
            String packName = FileUtility.getPathWithFileNameWithoutExtension(textureRoot, directory);
            ObjectArrayList<File> imageFiles = FileUtility.collectFilesShallow(directory,
                    EngineSetting.TEXTURE_FILE_EXTENSIONS);
            Object2ObjectLinkedOpenHashMap<String, TextureSourceStruct> tileName2Source =
                    new Object2ObjectLinkedOpenHashMap<>();

            imageFiles.sort(null);

            for (int i = 0; i < imageFiles.size(); i++)
                addSourceImage(imageFiles.get(i), packName, library, tileName2Source);

            ObjectArrayList<String> tileNames = new ObjectArrayList<>(tileName2Source.keySet());
            tileNames.sort(null);

            for (int i = 0; i < tileNames.size(); i++)
                sources.add(tileName2Source.get(tileNames.get(i)));
        }

        return sources;
    }

    private static void addSourceImage(
            File file,
            String packName,
            AliasLibraryStruct library,
            Object2ObjectLinkedOpenHashMap<String, TextureSourceStruct> tileName2Source) {

        String[] parts = FileUtility.splitFileNameByUnderscore(FileUtility.getFileName(file));
        AliasStruct alias = library.findAlias(parts[1]);

        if (alias == null)
            throwException("Alias: " + parts[1] + " could not be found in the system (" + file + ")");

        String tileName = toTileName(packName, parts[0]);
        TextureSourceStruct source = tileName2Source.get(tileName);

        if (source == null) {
            source = new TextureSourceStruct(tileName);
            tileName2Source.put(tileName, source);
        }

        for (int i = 0; i < source.getSourceCount(); i++)
            if (source.getAlias(i) == alias)
                throwException("Tile '" + tileName + "' has two images for alias " + alias.getAliasType());

        source.addSource(file, alias);
    }

    // The source images of one tile, found in its pack's folder by the tile's name
    public static TextureSourceStruct collectTileSource(File textureRoot, String tileName, AliasLibraryStruct library) {

        int separator = tileName.lastIndexOf(EngineSetting.TEXTURE_TILE_NAME_SEPARATOR);
        String packName = tileName.substring(0, separator);
        String instanceName = tileName.substring(separator + 1);
        ObjectArrayList<File> imageFiles = FileUtility.collectFilesShallow(new File(textureRoot, packName),
                EngineSetting.TEXTURE_FILE_EXTENSIONS);
        Object2ObjectLinkedOpenHashMap<String, TextureSourceStruct> tileName2Source =
                new Object2ObjectLinkedOpenHashMap<>();

        imageFiles.sort(null);

        for (int i = 0; i < imageFiles.size(); i++)
            if (FileUtility.splitFileNameByUnderscore(FileUtility.getFileName(imageFiles.get(i)))[0]
                    .equals(instanceName))
                addSourceImage(imageFiles.get(i), packName, library, tileName2Source);

        TextureSourceStruct source = tileName2Source.get(tileName);
        return source != null ? source : new TextureSourceStruct(tileName);
    }

    public static String toTileName(String packName, String instanceName) {
        return packName + EngineSetting.TEXTURE_TILE_NAME_SEPARATOR + instanceName;
    }

    // The checksum of the bake format and every source file of a tile, names and bytes alike
    public static long computeChecksum(TextureSourceStruct source) {

        CRC32C checksum = new CRC32C();
        checksum.update(EngineSetting.TEXTURE_BAKE_VERSION);

        for (int i = 0; i < source.getSourceCount(); i++) {

            File file = source.getFile(i);
            checksum.update(file.getName().getBytes(StandardCharsets.UTF_8));

            try {
                checksum.update(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                throwException("Texture source could not be read: " + file, e);
            }
        }

        return checksum.getValue();
    }

    // Whether the tile's baked files exist and were baked from exactly these sources
    public static boolean isBaked(File bakeRoot, String tileName, long checksum) {

        ArpgObjectStruct arpg = ArpgUtility.tryLoadObject(resolveDefinitionFile(bakeRoot, tileName));

        if (arpg == null || !ArpgUtility.hasNumber(arpg, EngineSetting.TEXTURE_BAKE_KEY_SOURCE)
                || arpg.get(EngineSetting.TEXTURE_BAKE_KEY_SOURCE).getAsLong() != checksum)
            return false;

        int size = ArpgUtility.getInt(arpg, EngineSetting.TEXTURE_BAKE_KEY_SIZE, 0);

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            if (EngineSetting.TEXTURE_TIER_SIZES[tier] <= size && !resolveTierFile(bakeRoot, tileName, tier).isFile())
                return false;

        return true;
    }

    // Bake \\

    public static TextureBakeStruct bakeSource(
            TextureSourceStruct source,
            AliasLibraryStruct library,
            long checksum,
            TextureCompressionScratchStruct scratch) {

        BufferedImage[] images = new BufferedImage[source.getSourceCount()];
        AliasStruct[] aliases = new AliasStruct[source.getSourceCount()];

        for (int i = 0; i < images.length; i++) {
            images[i] = readImage(source.getFile(i));
            aliases[i] = source.getAlias(i);
        }

        return bakeImages(source.getTileName(), images, aliases, library, checksum, scratch);
    }

    public static TextureBakeStruct bakeImages(
            String tileName,
            BufferedImage[] images,
            AliasStruct[] aliases,
            AliasLibraryStruct library,
            long checksum,
            TextureCompressionScratchStruct scratch) {

        int size = resolveTileSize(images);
        byte[][] packed = packMaps(images, aliases, library, size);
        byte[][][] tierMaps = new byte[EngineSetting.TEXTURE_TIER_COUNT][][];
        byte[][] fallbackMaps = new byte[TextureMapType.TIERED_COUNT][];
        byte[] extraMap = null;

        for (int m = 0; m < TextureMapType.VALUES.length; m++) {

            TextureMapType mapType = TextureMapType.VALUES[m];

            if (packed[m] == null)
                continue;

            byte[][] chain = buildChain(packed[m], size, mapType);

            if (!mapType.isTiered()) {
                extraMap = compressChain(chain, size, EngineSetting.TEXTURE_EXTRA_SIZE, mapType, scratch);
                continue;
            }

            for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++) {

                if (EngineSetting.TEXTURE_TIER_SIZES[tier] > size)
                    continue;

                if (tierMaps[tier] == null)
                    tierMaps[tier] = new byte[TextureMapType.TIERED_COUNT][];

                tierMaps[tier][m] = compressChain(chain, size, EngineSetting.TEXTURE_TIER_SIZES[tier], mapType,
                        scratch);
            }

            fallbackMaps[m] = compressChain(chain, size, EngineSetting.TEXTURE_FALLBACK_SIZE, mapType, scratch);
        }

        BufferedImage albedo = findImage(images, aliases, library.findAlias(EngineSetting.SHADER_ALIAS_ALBEDO));
        BufferedImage growth = findImage(images, aliases, library.findAlias(EngineSetting.SHADER_ALIAS_GROWTH));

        return new TextureBakeStruct(
                tileName,
                checksum,
                size,
                resolveAverageColor(albedo, library),
                resolveReveal(albedo, growth, size, library),
                tierMaps,
                fallbackMaps,
                extraMap);
    }

    // The extra map every alias of the extra set fills with its default, for the sentinel layer a tile with no
    // extra maps reads
    public static byte[] bakeDefaultExtra(AliasLibraryStruct library, TextureCompressionScratchStruct scratch) {

        int size = EngineSetting.TEXTURE_EXTRA_SIZE;
        byte[] map = new byte[size * size * EngineSetting.COLOR_CHANNEL_COUNT];

        for (int a = 0; a < library.getAliasCount(); a++)
            if (library.getAlias(a).getMapType() == TextureMapType.EXTRA)
                fillDefault(map, library.getAlias(a));

        return compressChain(buildChain(map, size, TextureMapType.EXTRA), size, size, TextureMapType.EXTRA, scratch);
    }

    public static BufferedImage readImage(File file) {

        try {

            BufferedImage image = ImageIO.read(file);

            if (image == null)
                throwException("File: " + file + " could not be read as an image");

            return image;
        } catch (IOException e) {
            return throwException("File: " + file + " could not be read as an image", e);
        }
    }

    private static BufferedImage findImage(BufferedImage[] images, AliasStruct[] aliases, AliasStruct alias) {

        for (int i = 0; i < images.length; i++)
            if (aliases[i] == alias)
                return images[i];

        return null;
    }

    // The power of two nearest the largest image side, held within the tiers
    private static int resolveTileSize(BufferedImage[] images) {

        int size = EngineSetting.TEXTURE_TIER_BASE_SIZE;

        for (int i = 0; i < images.length; i++)
            size = Math.max(size, toNearestPowerOfTwo(Math.max(images[i].getWidth(), images[i].getHeight())));

        return Math.min(size, EngineSetting.TEXTURE_TIER_TOP_SIZE);
    }

    private static int toNearestPowerOfTwo(int value) {

        int lower = Integer.highestOneBit(Math.max(value, 1));
        return (long) value * value >= 2L * lower * lower ? lower << 1 : lower;
    }

    // Packing \\

    // Every map at the tile's size, each channel holding its alias's image or, where none, its default
    private static byte[][] packMaps(
            BufferedImage[] images,
            AliasStruct[] aliases,
            AliasLibraryStruct library,
            int size) {

        byte[][] packed = new byte[TextureMapType.VALUES.length][];
        int bytes = size * size * EngineSetting.COLOR_CHANNEL_COUNT;

        for (int i = 0; i < aliases.length; i++)
            if (!aliases[i].getMapType().isTiered())
                packed[aliases[i].getMapType().ordinal()] = new byte[bytes];

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            packed[m] = new byte[bytes];

        for (int a = 0; a < library.getAliasCount(); a++) {

            AliasStruct alias = library.getAlias(a);
            byte[] map = packed[alias.getMapType().ordinal()];

            if (map != null)
                fillDefault(map, alias);
        }

        for (int i = 0; i < images.length; i++)
            packImage(images[i], aliases[i], packed[aliases[i].getMapType().ordinal()], size);

        byte[] normal = packed[TextureMapType.NORMAL.ordinal()];

        for (int t = 0; t < size * size; t++)
            normalizeTexel(normal, t * EngineSetting.COLOR_CHANNEL_COUNT);

        return packed;
    }

    private static void fillDefault(byte[] map, AliasStruct alias) {

        Color color = alias.getAliasColor();
        int[] channels = { color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() };
        int first = alias.getChannel();
        int count = alias.getMapType().getSourceChannels();

        for (int t = 0; t < map.length; t += EngineSetting.COLOR_CHANNEL_COUNT)
            for (int c = 0; c < count; c++)
                map[t + first + c] = (byte) channels[c];
    }

    // One image resampled into its alias's channels, its top row written last so rows run bottom first
    private static void packImage(BufferedImage image, AliasStruct alias, byte[] map, int size) {

        int width = image.getWidth();
        int height = image.getHeight();
        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        float[] source = new float[width * height * channels];
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);

        for (int t = 0; t < argb.length; t++) {
            source[t * channels] = PackedColorUtility.red(argb[t]);
            source[t * channels + 1] = PackedColorUtility.green(argb[t]);
            source[t * channels + 2] = PackedColorUtility.blue(argb[t]);
            source[t * channels + 3] = (argb[t] >>> EngineSetting.PACKED_COLOR_ALPHA_SHIFT)
                    & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
        }

        float[] rows = new float[size * height * channels];

        for (int y = 0; y < height; y++)
            for (int x = 0; x < size; x++)
                accumulateAxis(source, y * width * channels, channels, width, x, size, rows,
                        (y * size + x) * channels);

        float[] resampled = new float[size * size * channels];

        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                accumulateAxis(rows, x * channels, size * channels, height, y, size, resampled,
                        (y * size + x) * channels);

        int first = alias.getChannel();
        int count = alias.getMapType().getSourceChannels();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                int from = (y * size + x) * channels;
                int to = ((size - 1 - y) * size + x) * channels;

                for (int c = 0; c < count; c++)
                    map[to + first + c] = (byte) Math.round(resampled[from + c]);
            }
        }
    }

    // The texels one destination texel covers along an axis: the nearest when enlarging, an area average otherwise
    private static void accumulateAxis(
            float[] source,
            int sourceBase,
            int sourceStep,
            int sourceLength,
            int targetIndex,
            int targetLength,
            float[] target,
            int targetBase) {

        int channels = EngineSetting.COLOR_CHANNEL_COUNT;

        if (targetLength >= sourceLength) {

            int nearest = Math.min(sourceLength - 1, (int) ((targetIndex + 0.5f) * sourceLength / targetLength));

            for (int c = 0; c < channels; c++)
                target[targetBase + c] = source[sourceBase + nearest * sourceStep + c];

            return;
        }

        float scale = sourceLength / (float) targetLength;
        float start = targetIndex * scale;
        float end = start + scale;
        int last = Math.min(sourceLength, (int) Math.ceil(end));

        for (int c = 0; c < channels; c++)
            target[targetBase + c] = 0f;

        for (int s = (int) start; s < last; s++) {

            float weight = (Math.min(end, s + 1) - Math.max(start, s)) / scale;

            for (int c = 0; c < channels; c++)
                target[targetBase + c] += source[sourceBase + s * sourceStep + c] * weight;
        }
    }

    // Mips \\

    // The map and every halving of it down to a single texel
    private static byte[][] buildChain(byte[] top, int size, TextureMapType mapType) {

        byte[][] chain = new byte[Integer.numberOfTrailingZeros(size) + 1][];
        chain[0] = top;

        float coverage = mapType.isCoveragePreserved() && hasPartialAlpha(top)
                ? measureCoverage(top, 1f)
                : EngineSetting.INDEX_NOT_FOUND;

        for (int level = 1, levelSize = size >> 1; level < chain.length; level++, levelSize >>= 1) {

            chain[level] = downsample(chain[level - 1], levelSize << 1, mapType.isNormalized());

            if (coverage >= 0f)
                preserveCoverage(chain[level], coverage);
        }

        return chain;
    }

    private static byte[] downsample(byte[] level, int size, boolean normalized) {

        int half = size >> 1;
        int channels = EngineSetting.COLOR_CHANNEL_COUNT;
        byte[] next = new byte[half * half * channels];

        for (int y = 0; y < half; y++) {
            for (int x = 0; x < half; x++) {

                int to = (y * half + x) * channels;

                for (int c = 0; c < channels; c++) {

                    int sum = (level[((y * 2) * size + x * 2) * channels + c] & EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                            + (level[((y * 2) * size + x * 2 + 1) * channels + c]
                                    & EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                            + (level[((y * 2 + 1) * size + x * 2) * channels + c]
                                    & EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                            + (level[((y * 2 + 1) * size + x * 2 + 1) * channels + c]
                                    & EngineSetting.PACKED_COLOR_CHANNEL_MASK);

                    next[to + c] = (byte) ((sum + 2) >> 2);
                }

                if (normalized)
                    normalizeTexel(next, to);
            }
        }

        return next;
    }

    // A tangent-space normal stored as unsigned bytes, rescaled to unit length
    private static void normalizeTexel(byte[] map, int offset) {

        float max = EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        float x = (map[offset] & EngineSetting.PACKED_COLOR_CHANNEL_MASK) / max * 2f - 1f;
        float y = (map[offset + 1] & EngineSetting.PACKED_COLOR_CHANNEL_MASK) / max * 2f - 1f;
        float z = (map[offset + 2] & EngineSetting.PACKED_COLOR_CHANNEL_MASK) / max * 2f - 1f;
        float length = (float) Math.sqrt(x * x + y * y + z * z);

        if (length <= 0f)
            return;

        map[offset] = (byte) Math.round((x / length * 0.5f + 0.5f) * max);
        map[offset + 1] = (byte) Math.round((y / length * 0.5f + 0.5f) * max);
        map[offset + 2] = (byte) Math.round((z / length * 0.5f + 0.5f) * max);
    }

    private static boolean hasPartialAlpha(byte[] map) {

        for (int t = EngineSetting.COLOR_CHANNEL_COUNT - 1; t < map.length; t += EngineSetting.COLOR_CHANNEL_COUNT)
            if ((map[t] & EngineSetting.PACKED_COLOR_CHANNEL_MASK) != EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                return true;

        return false;
    }

    // The share of texels whose scaled alpha clears the cutout
    private static float measureCoverage(byte[] map, float scale) {

        int texels = map.length / EngineSetting.COLOR_CHANNEL_COUNT;
        float cutoff = EngineSetting.COVERAGE_ALPHA_CUTOFF * EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        int covered = 0;

        for (int t = EngineSetting.COLOR_CHANNEL_COUNT - 1; t < map.length; t += EngineSetting.COLOR_CHANNEL_COUNT)
            if ((map[t] & EngineSetting.PACKED_COLOR_CHANNEL_MASK) * scale >= cutoff)
                covered++;

        return covered / (float) texels;
    }

    // Scales a mip's alpha so its cutout shows the same share of texels the full-size map does
    private static void preserveCoverage(byte[] map, float coverage) {

        float low = 0f;
        float high = EngineSetting.TEXTURE_COVERAGE_SCALE_MAX;

        for (int step = 0; step < EngineSetting.TEXTURE_COVERAGE_SEARCH_STEPS; step++) {

            float middle = (low + high) * 0.5f;

            if (measureCoverage(map, middle) < coverage)
                low = middle;
            else
                high = middle;
        }

        for (int t = EngineSetting.COLOR_CHANNEL_COUNT - 1; t < map.length; t += EngineSetting.COLOR_CHANNEL_COUNT)
            map[t] = (byte) Math.min(EngineSetting.PACKED_COLOR_CHANNEL_MASK,
                    Math.round((map[t] & EngineSetting.PACKED_COLOR_CHANNEL_MASK) * high));
    }

    // Compression \\

    // The chain from the level of the given size down, block-compressed back to back
    private static byte[] compressChain(
            byte[][] chain,
            int size,
            int levelSize,
            TextureMapType mapType,
            TextureCompressionScratchStruct scratch) {

        byte[] out = new byte[TextureCompressionUtility.getChainBytes(levelSize)];
        int offset = 0;
        int level = Integer.numberOfTrailingZeros(size) - Integer.numberOfTrailingZeros(levelSize);

        for (int edge = levelSize; edge >= 1; edge >>= 1, level++) {

            if (mapType.isTwoChannel())
                TextureCompressionUtility.encodeBC5(chain[level], edge, edge, out, offset, scratch);
            else
                TextureCompressionUtility.encodeBC7(chain[level], edge, edge, out, offset, scratch);

            offset += TextureCompressionUtility.getLevelBytes(edge, edge);
        }

        return out;
    }

    // Average Color \\

    // The albedo's average weighted by coverage, so a cutout's transparent texels never darken it
    private static int resolveAverageColor(BufferedImage albedo, AliasLibraryStruct library) {

        Color fallback = library.findAlias(EngineSetting.SHADER_ALIAS_ALBEDO).getAliasColor();
        int fallbackColor = PackedColorUtility.pack(fallback.getRed(), fallback.getGreen(), fallback.getBlue());

        if (albedo == null)
            return fallbackColor;

        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        double coverage = 0.0;

        for (int y = 0; y < albedo.getHeight(); y++) {
            for (int x = 0; x < albedo.getWidth(); x++) {

                int argb = albedo.getRGB(x, y);
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

    // Reveal \\

    // A covering's full draw texel for texel: shown once its growth is reached and its alpha clears the cutoff,
    // tinted as far as its chroma allows, measured over the albedo's texels with the growth map read at each
    private static TextureRevealStruct resolveReveal(
            BufferedImage albedo,
            BufferedImage growth,
            int size,
            AliasLibraryStruct library) {

        int albedoFallback = library.findAlias(EngineSetting.SHADER_ALIAS_ALBEDO).getAliasColor().getRGB();
        int growthFallback = library.findAlias(EngineSetting.SHADER_ALIAS_GROWTH).getAliasColor().getRGB();
        BufferedImage measured = albedo != null ? albedo : growth;
        int width = measured != null ? measured.getWidth() : size;
        int height = measured != null ? measured.getHeight() : size;

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

                int growthValue = PackedColorUtility.red(growth != null
                        ? growth.getRGB(x * growth.getWidth() / width, y * growth.getHeight() / height)
                        : growthFallback);
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
    private static float resolveTintWeight(int argb) {

        int max = Math.max(PackedColorUtility.red(argb),
                Math.max(PackedColorUtility.green(argb), PackedColorUtility.blue(argb)));
        int min = Math.min(PackedColorUtility.red(argb),
                Math.min(PackedColorUtility.green(argb), PackedColorUtility.blue(argb)));
        float chroma = (max - min) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;

        return 1f - SkyColorUtility.smoothstep(
                EngineSetting.COVERAGE_TINT_CHROMA_LOW, EngineSetting.COVERAGE_TINT_CHROMA_HIGH, chroma);
    }

    // Files \\

    public static File resolveDefinitionFile(File bakeRoot, String tileName) {
        return ArpgUtility.resolveFile(new File(bakeRoot, EngineSetting.BAKED_TEXTURE_TILE_DIRECTORY), tileName);
    }

    public static File resolveTierFile(File bakeRoot, String tileName, int tier) {
        return ArpgUtility.resolveFile(
                new File(bakeRoot, EngineSetting.BAKED_TEXTURE_TIER_PREFIX + EngineSetting.TEXTURE_TIER_SIZES[tier]),
                tileName);
    }

    // Writes the definition and every tier file of a bake, each read back and compared, and returns the files
    public static ObjectArrayList<File> writeBake(File bakeRoot, TextureBakeStruct bake) {

        ObjectArrayList<File> files = new ObjectArrayList<>();
        writeVerified(resolveDefinitionFile(bakeRoot, bake.getTileName()), toDefinitionArpg(bake), files);

        for (int tier = 0; tier < bake.getTierCount(); tier++)
            writeVerified(resolveTierFile(bakeRoot, bake.getTileName(), tier), toMapsArpg(bake, tier), files);

        return files;
    }

    private static void writeVerified(File file, ArpgObjectStruct arpg, ObjectArrayList<File> files) {

        ArpgUtility.writeObject(file, arpg);

        if (!ArpgUtility.readObject(file).equals(arpg))
            throwException("Baked texture file did not read back identically: " + file);

        files.add(file);
    }

    private static ArpgObjectStruct toDefinitionArpg(TextureBakeStruct bake) {

        ArpgObjectStruct arpg = new ArpgObjectStruct();
        arpg.addProperty(EngineSetting.TEXTURE_BAKE_KEY_SOURCE, bake.getSourceChecksum());
        arpg.addProperty(EngineSetting.TEXTURE_BAKE_KEY_SIZE, bake.getSize());
        arpg.addProperty(EngineSetting.TEXTURE_BAKE_KEY_AVERAGE, bake.getAverageColor());
        arpg.add(EngineSetting.TEXTURE_BAKE_KEY_REVEAL, toRevealArpg(bake.getReveal()));

        ArpgObjectStruct fallback = new ArpgObjectStruct();

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            fallback.addProperty(ArpgUtility.toEnumName(TextureMapType.VALUES[m]),
                    bake.getFallbackMap(TextureMapType.VALUES[m]));

        arpg.add(EngineSetting.TEXTURE_BAKE_KEY_FALLBACK, fallback);

        if (bake.getExtraMap() != null)
            arpg.addProperty(EngineSetting.TEXTURE_BAKE_KEY_EXTRA, bake.getExtraMap());

        return arpg;
    }

    private static ArpgObjectStruct toRevealArpg(TextureRevealStruct reveal) {

        ArpgObjectStruct arpg = new ArpgObjectStruct();
        ArpgArrayStruct shares = new ArpgArrayStruct();

        for (int level = 0; level <= EngineSetting.COVERAGE_LEVEL_MAX; level++)
            shares.add(reveal.getLevelShare(level));

        arpg.add(EngineSetting.TEXTURE_BAKE_KEY_SHARES, shares);
        arpg.addProperty(EngineSetting.TEXTURE_BAKE_KEY_COLOR, reveal.getRevealColor());
        arpg.addProperty(EngineSetting.TEXTURE_BAKE_KEY_TINTABLE, reveal.getTintableColor());

        return arpg;
    }

    private static ArpgObjectStruct toMapsArpg(TextureBakeStruct bake, int tier) {

        ArpgObjectStruct arpg = new ArpgObjectStruct();

        for (int m = 0; m < TextureMapType.TIERED_COUNT; m++)
            arpg.addProperty(ArpgUtility.toEnumName(TextureMapType.VALUES[m]),
                    bake.getTierMap(tier, TextureMapType.VALUES[m]));

        return arpg;
    }

    // Read \\

    public static TextureRevealStruct readReveal(ArpgObjectStruct revealArpg) {

        ArpgArrayStruct sharesArray = ArpgUtility.validateArray(revealArpg, EngineSetting.TEXTURE_BAKE_KEY_SHARES,
                EngineSetting.COVERAGE_LEVEL_MAX + 1);
        float[] shares = new float[sharesArray.size()];

        for (int level = 0; level < shares.length; level++)
            shares[level] = sharesArray.get(level).getAsFloat();

        return new TextureRevealStruct(
                shares,
                ArpgUtility.validateInt(revealArpg, EngineSetting.TEXTURE_BAKE_KEY_COLOR),
                ArpgUtility.validateInt(revealArpg, EngineSetting.TEXTURE_BAKE_KEY_TINTABLE));
    }

    // One map's chain from a definition's fallback or a tier file, by the map's name
    public static byte[] readMap(ArpgObjectStruct mapsArpg, TextureMapType mapType) {
        return ArpgUtility.validateBytes(mapsArpg, ArpgUtility.toEnumName(mapType));
    }
}
