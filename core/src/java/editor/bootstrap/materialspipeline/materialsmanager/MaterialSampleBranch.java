package editor.bootstrap.materialspipeline.materialsmanager;

import java.awt.Color;
import java.io.File;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasLibraryStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageColorUtility;
import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MaterialSampleBranch extends BranchPackage {

    /*
     * Reads a material's maps texel by texel, the way a texturing tool lays
     * a material's channels: each map is the image its texture holds for
     * it, or one texel of the map's default where the texture has none,
     * read between texels and repeating beyond its edges, then shaped by the
     * material's parameters — the albedo coloured by the base colour, the
     * normal's slope scaled by its strength, roughness and metallic shifted
     * by their offsets. The images are read on first use and let go as soon
     * as the material laid no longer takes them. Export writes every map so
     * read, once across the base texture's size, as a PNG apiece.
     */

    // Internal
    private TextureManager textureManager;
    private ImageManager imageManager;

    // Directory
    private File textureRoot;

    // Maps — per texture one image per map, read on first use
    private Object2ObjectOpenHashMap<String, Pixmap[]> textureName2Maps;

    // Scratch
    private ObjectArrayList<String> textureScratch;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.textureRoot = new File(EngineSetting.BLOCK_TEXTURE_PATH);

        // Maps
        this.textureName2Maps = new Object2ObjectOpenHashMap<>();

        // Scratch
        this.textureScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.imageManager = get(ImageManager.class);
    }

    // Maps \\

    // A map's image of a texture, read the first time it is asked for
    private Pixmap resolveMap(String textureName, int map) {

        Pixmap[] maps = textureName2Maps.get(textureName);

        if (maps == null) {
            maps = new Pixmap[EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length];
            textureName2Maps.put(textureName, maps);
        }

        if (maps[map] == null)
            maps[map] = readMap(textureName, map);

        return maps[map];
    }

    // The image the texture's pack holds for the map, or one texel of the map's default where it holds none
    private Pixmap readMap(String textureName, int map) {

        AliasLibraryStruct library = textureManager.getAliasLibrary();
        AliasStruct alias = library.findAlias(EditorSetting.CHARACTER_PAINT_MAP_ALIASES[map]);
        TextureSourceStruct source = TextureBakeUtility.collectTileSource(textureRoot, textureName, library);

        for (int i = 0; i < source.getSourceCount(); i++)
            if (source.getAlias(i) == alias)
                return new Pixmap(source.getFile(i));

        Pixmap blank = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        blank.drawPixel(0, 0, toRGBA(alias.getAliasColor()));
        return blank;
    }

    // Every image read for a texture the material no longer takes let go
    void retain(MaterialDocumentStruct document, String textureName) {

        textureScratch.clear();

        for (String readName : textureName2Maps.keySet())
            if (!isTaken(document, textureName, readName))
                textureScratch.add(readName);

        for (int i = 0; i < textureScratch.size(); i++)
            release(textureName2Maps.remove(textureScratch.get(i)));
    }

    // Whether the material laid reads any map from the texture: the document's maps, or the texture chosen alone
    private boolean isTaken(MaterialDocumentStruct document, String textureName, String readName) {

        if (document == null)
            return readName.equals(textureName);

        for (int m = 0; m < EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length; m++)
            if (document.resolveMapTexture(m).equals(readName))
                return true;

        return false;
    }

    private void release(Pixmap[] maps) {

        for (int m = 0; m < maps.length; m++)
            if (maps[m] != null)
                maps[m].dispose();
    }

    // Sample \\

    // A map's texel where the UV falls, read between texels and repeating, v running up, then shaped by the
    // parameters; the material's own defaults where there is no document
    int sample(MaterialDocumentStruct document, String textureName, int map, float u, float v) {

        Pixmap image = resolveMap(textureName, map);
        int width = image.getWidth();
        int height = image.getHeight();
        float x = (u - (float) Math.floor(u)) * width - 0.5f;
        float y = ((1f - v) - (float) Math.floor(1f - v)) * height - 0.5f;
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float fx = x - x0;
        float fy = y - y0;
        int top = ImageColorUtility.mix(
                image.getPixel(Math.floorMod(x0, width), Math.floorMod(y0, height)),
                image.getPixel(Math.floorMod(x0 + 1, width), Math.floorMod(y0, height)),
                fx,
                true);
        int bottom = ImageColorUtility.mix(
                image.getPixel(Math.floorMod(x0, width), Math.floorMod(y0 + 1, height)),
                image.getPixel(Math.floorMod(x0 + 1, width), Math.floorMod(y0 + 1, height)),
                fx,
                true);

        return shape(document, map, ImageColorUtility.mix(top, bottom, fy, true));
    }

    // Parameters \\

    // A texel shaped by the parameters the map takes
    private int shape(MaterialDocumentStruct document, int map, int texel) {

        if (document == null)
            return texel;

        return switch (map) {
            case EditorSetting.MATERIALS_MAP_ALBEDO -> colour(document, texel);
            case EditorSetting.MATERIALS_MAP_NORMAL -> steepen(texel, document.getParameter(
                    EditorSetting.MATERIALS_PARAMETER_NORMAL_STRENGTH));
            case EditorSetting.MATERIALS_MAP_ROUGHNESS -> shift(texel, document.getParameter(
                    EditorSetting.MATERIALS_PARAMETER_ROUGHNESS));
            case EditorSetting.MATERIALS_MAP_METALLIC -> shift(texel, document.getParameter(
                    EditorSetting.MATERIALS_PARAMETER_METALLIC));
            default -> texel;
        };
    }

    // The albedo multiplied by the base colour, its alpha kept
    private int colour(MaterialDocumentStruct document, int texel) {
        return toRGBA(
                ImageColorUtility.toChannel(texel, 0) * document.getParameter(EditorSetting.MATERIALS_PARAMETER_RED),
                ImageColorUtility.toChannel(texel, 1) * document.getParameter(EditorSetting.MATERIALS_PARAMETER_GREEN),
                ImageColorUtility.toChannel(texel, 2) * document.getParameter(EditorSetting.MATERIALS_PARAMETER_BLUE),
                ImageColorUtility.toChannel(texel, 3));
    }

    // A tangent-space normal's slope scaled by the strength, made whole again
    private int steepen(int texel, float strength) {

        float nx = decodeNormal(texel, 0) * strength;
        float ny = decodeNormal(texel, 1) * strength;
        float nz = decodeNormal(texel, 2);
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        float scale = length > EngineSetting.DIVISION_EPSILON ? 1f / length : 0f;

        return toRGBA(
                encodeNormal(nx * scale),
                encodeNormal(ny * scale),
                encodeNormal(nz * scale),
                ImageColorUtility.toChannel(texel, 3));
    }

    private float decodeNormal(int texel, int channel) {
        return ImageColorUtility.toChannel(texel, channel) / EngineSetting.COLOR_CHANNEL_BYTE_MAX
                / EditorSetting.MATERIALS_NORMAL_MIDDLE - 1f;
    }

    private float encodeNormal(float value) {
        return (value + 1f) * EditorSetting.MATERIALS_NORMAL_MIDDLE * EngineSetting.COLOR_CHANNEL_BYTE_MAX;
    }

    // Every colour channel moved by the offset, a share of the whole range
    private int shift(int texel, float offset) {

        float moved = offset * EngineSetting.COLOR_CHANNEL_BYTE_MAX;

        return toRGBA(
                ImageColorUtility.toChannel(texel, 0) + moved,
                ImageColorUtility.toChannel(texel, 1) + moved,
                ImageColorUtility.toChannel(texel, 2) + moved,
                ImageColorUtility.toChannel(texel, 3));
    }

    // Export \\

    // Every map of the material written once across the base texture's size, named for the material and the map's
    // suffix; how many were written, or -1 when one could not be
    int export(MaterialDocumentStruct document, File folder, String stem) {

        TextureHandle baseHandle = textureManager.getTextureHandleFromTextureName(document.getBaseTexture());
        int width = baseHandle.getTileWidth();
        int height = baseHandle.getTileHeight();
        Pixmap image = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        int written = 0;

        folder.mkdirs();

        for (int m = 0; m < EditorSetting.CHARACTER_PAINT_MAP_SUFFIXES.length; m++) {

            String textureName = document.resolveMapTexture(m);

            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    image.drawPixel(x, y, sample(
                            document,
                            textureName,
                            m,
                            (x + 0.5f) / width,
                            1f - (y + 0.5f) / height));

            if (!imageManager.writeImage(new File(folder, stem + EditorSetting.IMAGE_IMPORT_ALIAS_SEPARATOR
                    + EditorSetting.CHARACTER_PAINT_MAP_SUFFIXES[m] + EditorSetting.IMAGE_EXPORT_EXTENSION), image)) {
                image.dispose();
                return EngineSetting.INDEX_NOT_FOUND;
            }

            written++;
        }

        image.dispose();
        return written;
    }

    // Utility \\

    private static int toRGBA(Color color) {
        return toRGBA(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    // Four channels, each held to a byte, as RGBA8888
    private static int toRGBA(float red, float green, float blue, float alpha) {
        return toByte(red) << Byte.SIZE * 3 | toByte(green) << Byte.SIZE * 2 | toByte(blue) << Byte.SIZE
                | toByte(alpha);
    }

    private static int toByte(float value) {
        return Math.clamp(Math.round(value), 0, (int) EngineSetting.COLOR_CHANNEL_BYTE_MAX);
    }
}
