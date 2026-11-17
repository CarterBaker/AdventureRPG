package editor.bootstrap.stamppipeline.stampmanager;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StampImageBranch extends BranchPackage {

    /*
     * Main thread — the picture stamps the Image Editor presses, as a
     * painting tool keeps a library of stamp images: every PNG in the
     * workspace's Stamps folder, by its path there without the extension, in
     * name order. The folder is read again each time the library is shown,
     * so a picture dropped in appears and one taken away leaves, its pixels
     * and emblem with it. A picture's pixels are read the first time it is
     * pressed or shown, and its emblem is the picture fitted to a square,
     * drawn once; rows are bottom first, as every sprite's are.
     */

    // Internal
    private TextureManager textureManager;
    private ImageManager imageManager;

    // Directory
    private File stampRoot;

    // Library — the pictures listed, each one's pixels and emblem once made
    private ObjectArrayList<String> names;
    private Object2ObjectOpenHashMap<String, Pixmap> name2Pixmap;
    private Object2IntOpenHashMap<String> name2Texture;

    // Scratch
    private ByteBuffer texels;
    private ObjectArrayList<String> nameScratch;

    // Base \\

    @Override
    protected void create() {

        int texelCount = EditorSetting.STAMP_THUMBNAIL_SIZE * EditorSetting.STAMP_THUMBNAIL_SIZE;

        // Directory
        this.stampRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.IMAGE_STAMP_DIRECTORY);

        // Library
        this.names = new ObjectArrayList<>();
        this.name2Pixmap = new Object2ObjectOpenHashMap<>();
        this.name2Texture = new Object2IntOpenHashMap<>();

        // Scratch
        this.texels = BufferUtility.newByteBuffer(texelCount * EngineSetting.COLOR_CHANNEL_COUNT);
        this.nameScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.imageManager = get(ImageManager.class);
    }

    @Override
    protected void dispose() {

        for (String name : names)
            release(name);

        names.clear();
    }

    // Library \\

    // The folder read again; true when the pictures listed changed
    boolean scan() {

        nameScratch.clear();

        if (stampRoot.isDirectory()) {

            ObjectArrayList<File> files = FileUtility.collectFiles(stampRoot, EngineSetting.TEXTURE_FILE_EXTENSIONS);

            for (int i = 0; i < files.size(); i++)
                nameScratch.add(FileUtility.getPathWithFileNameWithoutExtension(stampRoot, files.get(i)));
        }

        nameScratch.sort(null);

        if (nameScratch.equals(names))
            return false;

        for (String name : names)
            if (!nameScratch.contains(name))
                release(name);

        names.clear();
        names.addAll(nameScratch);

        return true;
    }

    private void release(String name) {

        Pixmap pixmap = name2Pixmap.remove(name);

        if (pixmap != null)
            pixmap.dispose();

        if (name2Texture.containsKey(name))
            textureManager.deleteTexture2D(name2Texture.removeInt(name));
    }

    // Pixels \\

    // The picture's pixels, read the first time they are asked for; null when the file is gone or not an image
    Pixmap getPixmap(String name) {

        Pixmap pixmap = name2Pixmap.get(name);

        if (pixmap != null || !names.contains(name))
            return pixmap;

        File file = new File(stampRoot, name + EditorSetting.IMAGE_EXPORT_EXTENSION);

        try {
            pixmap = imageManager.decodeImage(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            errorLog(EditorSetting.IMAGE_STAMP_MESSAGE_NOT_READ + file.getPath());
            return null;
        }

        if (pixmap != null)
            name2Pixmap.put(name, pixmap);

        return pixmap;
    }

    // Emblem \\

    // The picture's emblem, drawn the first time a tile shows it; 0 when the picture cannot be read
    int getThumbnail(String name) {

        if (name2Texture.containsKey(name))
            return name2Texture.getInt(name);

        Pixmap pixmap = getPixmap(name);

        if (pixmap == null)
            return 0;

        int size = EditorSetting.STAMP_THUMBNAIL_SIZE;
        int texture = textureManager.createTexture2D(
                size,
                size,
                EngineSetting.GL_CLAMP_TO_EDGE,
                EngineSetting.GL_LINEAR);

        draw(pixmap, size);
        textureManager.updateTexture2D(texture, 0, 0, size, size, texels);
        name2Texture.put(name, texture);

        return texture;
    }

    // The picture fitted inside the square by its longer side, nearest pixel, the rest left clear
    private void draw(Pixmap pixmap, int size) {

        float scale = (float) Math.max(pixmap.getWidth(), pixmap.getHeight()) / size;
        float offsetX = (size - pixmap.getWidth() / scale) * 0.5f;
        float offsetY = (size - pixmap.getHeight() / scale) * 0.5f;

        texels.clear();

        for (int row = size - 1; row >= 0; row--)
            for (int column = 0; column < size; column++) {

                int x = (int) ((column - offsetX) * scale);
                int y = (int) ((row - offsetY) * scale);
                int color = x >= 0 && y >= 0 && x < pixmap.getWidth() && y < pixmap.getHeight()
                        ? pixmap.getPixel(x, y)
                        : 0;

                texels.put((byte) (color >>> EngineSetting.RGBA_RED_SHIFT))
                        .put((byte) (color >>> EngineSetting.RGBA_GREEN_SHIFT))
                        .put((byte) (color >>> EngineSetting.RGBA_BLUE_SHIFT))
                        .put((byte) color);
            }

        texels.flip();
    }

    // Accessible \\

    ObjectArrayList<String> getNames() {
        return names;
    }

    boolean hasName(String name) {
        return names.contains(name);
    }
}
