package editor.bootstrap.materialspipeline.materialsmanager;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasLibraryStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageColorUtility;
import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.assets.image.PixmapUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MaterialSampleBranch extends BranchPackage {

    /*
     * Reads a material's maps texel by texel, the way a texturing tool lays
     * a material's channels: each map is the image file the material names
     * for it, else the image its texture holds for it, or one texel of the
     * map's default where there is neither, read between texels and
     * repeating beyond its edges, then shaped by the material's parameters —
     * the albedo coloured by the base colour, the normal's slope scaled by
     * its strength, roughness and metallic shifted by their offsets. The
     * images are read on first use and let go as soon as the material laid
     * no longer takes them. The Material Viewer reads the same images, each
     * map's handed on as an editor texture made again only when the image
     * behind it changes, so the viewer and the brush show one material.
     * Export writes every map so read, once across the base texture's size,
     * as a PNG apiece.
     */

    // Internal
    private TextureManager textureManager;
    private ImageManager imageManager;
    private MaterialDocumentBranch materialDocumentBranch;

    // Directory
    private File textureRoot;

    // Maps — per texture one image per map, and per image file its image, each read on first use
    private Object2ObjectOpenHashMap<String, Pixmap[]> textureName2Maps;
    private Object2ObjectOpenHashMap<String, Pixmap> imagePath2Image;

    // Preview — per map the editor texture the viewer reads and the image it was made from
    private ImageDocumentInstance[] previewDocuments;
    private Pixmap[] previewSources;

    // Scratch
    private ObjectArrayList<String> nameScratch;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.textureRoot = new File(EngineSetting.BLOCK_TEXTURE_PATH);

        // Maps
        this.textureName2Maps = new Object2ObjectOpenHashMap<>();
        this.imagePath2Image = new Object2ObjectOpenHashMap<>();

        // Preview
        this.previewDocuments = new ImageDocumentInstance[EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length];
        this.previewSources = new Pixmap[EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length];

        // Scratch
        this.nameScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.imageManager = get(ImageManager.class);
        this.materialDocumentBranch = get(MaterialDocumentBranch.class);
    }

    // Maps \\

    // The image a map of the material laid is read from: the document's own image, else its texture's map; the
    // texture chosen's map where there is no document
    private Pixmap resolveSource(MaterialDocumentStruct document, String textureName, int map) {

        if (document != null && document.hasMapImage(map))
            return resolveImage(document.getMapImage(map), map);

        return resolveMap(document != null ? document.resolveMapTexture(map) : textureName, map);
    }

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
    Pixmap readMap(String textureName, int map) {

        AliasLibraryStruct library = textureManager.getAliasLibrary();
        AliasStruct alias = library.findAlias(EditorSetting.CHARACTER_PAINT_MAP_ALIASES[map]);
        TextureSourceStruct source = TextureBakeUtility.collectTileSource(textureRoot, textureName, library);

        for (int i = 0; i < source.getSourceCount(); i++)
            if (source.getAlias(i) == alias)
                return new Pixmap(source.getFile(i));

        return createDefault(map);
    }

    // An image file named for a map, read the first time it is asked for; the map's default while it cannot be read
    private Pixmap resolveImage(String imagePath, int map) {

        Pixmap image = imagePath2Image.get(imagePath);

        if (image == null) {
            image = readImage(materialDocumentBranch.resolveImageFile(imagePath));
            imagePath2Image.put(imagePath, image != null ? image : createDefault(map));
        }

        return imagePath2Image.get(imagePath);
    }

    // The pixels of an image file; null when it is not there or is not an image
    Pixmap readImage(File file) {

        if (!file.isFile())
            return null;

        try {
            return imageManager.decodeImage(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            return null;
        }
    }

    private Pixmap createDefault(int map) {

        AliasStruct alias = textureManager.getAliasLibrary().findAlias(EditorSetting.CHARACTER_PAINT_MAP_ALIASES[map]);
        Pixmap blank = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        blank.drawPixel(0, 0, toRGBA(alias.getAliasColor()));
        return blank;
    }

    // Every image read for a texture or a file the material laid no longer takes let go
    void retain(MaterialDocumentStruct document, String textureName) {

        nameScratch.clear();

        for (String readName : textureName2Maps.keySet())
            if (!isTextureTaken(document, textureName, readName))
                nameScratch.add(readName);

        for (int i = 0; i < nameScratch.size(); i++)
            release(textureName2Maps.remove(nameScratch.get(i)));

        nameScratch.clear();

        for (String readPath : imagePath2Image.keySet())
            if (!isImageTaken(document, readPath))
                nameScratch.add(readPath);

        for (int i = 0; i < nameScratch.size(); i++)
            imagePath2Image.remove(nameScratch.get(i)).dispose();
    }

    // Whether the material laid reads any map from the texture: the document's maps, or the texture chosen alone
    private boolean isTextureTaken(MaterialDocumentStruct document, String textureName, String readName) {

        if (document == null)
            return readName.equals(textureName);

        for (int m = 0; m < EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length; m++)
            if (!document.hasMapImage(m) && document.resolveMapTexture(m).equals(readName))
                return true;

        return false;
    }

    private boolean isImageTaken(MaterialDocumentStruct document, String readPath) {

        for (int m = 0; document != null && m < EditorSetting.CHARACTER_PAINT_MAP_ALIASES.length; m++)
            if (document.getMapImage(m).equals(readPath))
                return true;

        return false;
    }

    // An image file read again the next time it is asked for, as when it is chosen anew
    void forgetImage(String imagePath) {

        Pixmap image = imagePath2Image.remove(imagePath);

        if (image != null)
            image.dispose();
    }

    private void release(Pixmap[] maps) {

        for (int m = 0; m < maps.length; m++)
            if (maps[m] != null)
                maps[m].dispose();
    }

    // Sample \\

    // A map's texel where the UV falls, read between texels and repeating, v running up, then shaped by the
    // parameters; the texture chosen's own where there is no document
    int sample(MaterialDocumentStruct document, String textureName, int map, float u, float v) {

        Pixmap image = resolveSource(document, textureName, map);
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

    // Preview \\

    // The editor texture holding a map's image of the material laid, made again only when that image changes; zero
    // until its first upload
    int usePreviewTexture(MaterialDocumentStruct document, String textureName, int map) {

        Pixmap source = resolveSource(document, textureName, map);

        if (source != previewSources[map])
            showPreview(map, source);

        return imageManager.useTexture(previewDocuments[map]);
    }

    // The map's editor texture made again from a copy of its image, the old one let go
    private void showPreview(int map, Pixmap source) {

        String imageName = EditorSetting.MATERIALS_PREVIEW_IMAGE_PREFIX
                + EditorSetting.CHARACTER_PAINT_MAP_ALIASES[map];
        Pixmap copy = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);

        if (previewDocuments[map] != null) {
            Pixmap shown = previewDocuments[map].getPixmap();

            imageManager.closeImage(imageName);
            shown.dispose();
        }

        PixmapUtility.copyPixels(source, copy);
        previewDocuments[map] = imageManager.openImage(imageName, null, copy, true);
        previewSources[map] = source;
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

            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    image.drawPixel(x, y, sample(
                            document,
                            document.getBaseTexture(),
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
