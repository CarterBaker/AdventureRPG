package editor.bootstrap.imageeditorpipeline.imageeditormanager;

import java.io.File;

import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ImageProjectBranch extends BranchPackage {

    /*
     * Reads and writes the Image Editor's images, one .arpg file each in the
     * workspace's Images folder beside the player's saves, as a painting tool keeps its
     * layered project file: the image's size, its layers bottom first (name,
     * opacity, whether shown, and the pixels as the bytes of an image file),
     * the layer painted, and per map its own settings and, once generated,
     * its pixels. A file that cannot be read is reported, never fatal.
     */

    // Internal
    private ImageManager imageManager;
    private ImageEditorLayerBranch imageEditorLayerBranch;
    private ImageEditorMapBranch imageEditorMapBranch;

    // Directory
    private File imageRoot;

    // Base \\

    @Override
    protected void create() {
        this.imageRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.IMAGE_EDITOR_USER_DIRECTORY);
    }

    @Override
    protected void get() {
        this.imageManager = get(ImageManager.class);
        this.imageEditorLayerBranch = get(ImageEditorLayerBranch.class);
        this.imageEditorMapBranch = get(ImageEditorMapBranch.class);
    }

    // Files \\

    File getFolder() {
        return imageRoot;
    }

    File resolveFile(String imageName) {
        return ArpgUtility.resolveFile(imageRoot, imageName);
    }

    boolean hasFile(String imageName) {
        return resolveFile(imageName).isFile();
    }

    // Every image saved in the folder, by name, in name order
    void collectImageNames(ObjectArrayList<String> out) {

        File[] files = imageRoot.listFiles();

        if (files == null)
            return;

        for (File file : files)
            if (file.isFile() && ArpgUtility.isArpgFile(file))
                out.add(FileUtility.getFileName(file));

        out.sort(null);
    }

    // Read \\

    // The image's file as written, null with the reason in the log when it cannot be read
    ArpgObjectStruct readFile(String imageName) {
        return ArpgUtility.tryLoadObject(resolveFile(imageName));
    }

    // The size a file records, zero where it records none
    int readWidth(ArpgObjectStruct arpg) {
        return ArpgUtility.getInt(arpg, EditorSetting.IMAGE_FILE_KEY_WIDTH, 0);
    }

    int readHeight(ArpgObjectStruct arpg) {
        return ArpgUtility.getInt(arpg, EditorSetting.IMAGE_FILE_KEY_HEIGHT, 0);
    }

    // The layers and maps of a file laid into a project made at the file's size; false when an image will not decode
    boolean load(ImageProjectInstance project, ArpgObjectStruct arpg) {

        ImageLayerStackStruct stack = project.getStack();
        ArpgArrayStruct layerArray = ArpgUtility.validateArray(arpg, EditorSetting.IMAGE_FILE_KEY_LAYERS);

        for (int i = 0; i < layerArray.size(); i++) {

            ArpgObjectStruct layerArpg = layerArray.get(i).getAsObject();
            Pixmap pixmap = decode(project, ArpgUtility.validateBytes(layerArpg, EditorSetting.IMAGE_FILE_KEY_PIXELS));

            if (pixmap == null)
                return false;

            imageEditorLayerBranch.openLayer(
                    project,
                    pixmap,
                    ArpgUtility.validateString(layerArpg, EditorSetting.IMAGE_FILE_KEY_NAME),
                    ArpgUtility.validateFloat(layerArpg, EditorSetting.IMAGE_FILE_KEY_OPACITY),
                    ArpgUtility.validateBoolean(layerArpg, EditorSetting.IMAGE_FILE_KEY_VISIBLE));
        }

        stack.setActiveLayer(Math.clamp(
                ArpgUtility.getInt(arpg, EditorSetting.IMAGE_FILE_KEY_ACTIVE_LAYER, 0), 0, stack.getLayerCount() - 1));
        stack.setNextLayerNumber(Math.max(
                stack.getNextLayerNumber(),
                ArpgUtility.getInt(arpg, EditorSetting.IMAGE_FILE_KEY_NEXT_LAYER, stack.getNextLayerNumber())));

        return !ArpgUtility.hasObject(arpg, EditorSetting.IMAGE_FILE_KEY_MAPS)
                || loadMaps(project, arpg.getAsObject(EditorSetting.IMAGE_FILE_KEY_MAPS));
    }

    private boolean loadMaps(ImageProjectInstance project, ArpgObjectStruct mapsArpg) {

        for (ImageMap map : ImageMap.VALUES) {

            String mapKey = ArpgUtility.toEnumName(map);

            if (!map.isGenerated() || !ArpgUtility.hasObject(mapsArpg, mapKey))
                continue;

            ArpgObjectStruct mapArpg = mapsArpg.getAsObject(mapKey);
            float[] settings = project.getMapSettings(map);

            if (ArpgUtility.hasObject(mapArpg, EditorSetting.IMAGE_FILE_KEY_SETTINGS)) {

                ArpgObjectStruct settingsArpg = mapArpg.getAsObject(EditorSetting.IMAGE_FILE_KEY_SETTINGS);

                for (MapGenerateOption option : map.getOptions())
                    settings[option.ordinal()] = option.clamp(ArpgUtility.getFloat(
                            settingsArpg, ArpgUtility.toEnumName(option), option.get(settings)));
            }

            if (!ArpgUtility.hasBytes(mapArpg, EditorSetting.IMAGE_FILE_KEY_PIXELS))
                continue;

            Pixmap pixmap = decode(project, ArpgUtility.validateBytes(mapArpg, EditorSetting.IMAGE_FILE_KEY_PIXELS));

            if (pixmap == null)
                return false;

            imageEditorMapBranch.openMap(project, map, pixmap);
        }

        return true;
    }

    // An image file's bytes as pixels, refused where they are not an image of the project's size
    private Pixmap decode(ImageProjectInstance project, byte[] bytes) {

        Pixmap pixmap = imageManager.decodeImage(bytes);

        if (pixmap == null)
            return null;

        if (pixmap.getWidth() == project.getWidth() && pixmap.getHeight() == project.getHeight())
            return pixmap;

        pixmap.dispose();
        errorLog("An image layer does not match its image's size: " + project.getFile().getAbsolutePath());
        return null;
    }

    // Write \\

    // The image written to a file, its own or a copy's
    boolean write(ImageProjectInstance project, File file) {

        ImageLayerStackStruct stack = project.getStack();
        ArpgObjectStruct arpg = new ArpgObjectStruct();
        ArpgArrayStruct layerArray = new ArpgArrayStruct();
        ArpgObjectStruct mapsArpg = new ArpgObjectStruct();

        arpg.addProperty(EditorSetting.IMAGE_FILE_KEY_WIDTH, project.getWidth());
        arpg.addProperty(EditorSetting.IMAGE_FILE_KEY_HEIGHT, project.getHeight());
        arpg.addProperty(EditorSetting.IMAGE_FILE_KEY_ACTIVE_LAYER, stack.getActiveLayer());
        arpg.addProperty(EditorSetting.IMAGE_FILE_KEY_NEXT_LAYER, stack.getNextLayerNumber());

        for (int i = 0; i < stack.getLayerCount(); i++) {

            ArpgObjectStruct layerArpg = new ArpgObjectStruct();
            byte[] pixels = imageManager.encodeImage(stack.getLayer(i).getPixmap());

            if (pixels == null)
                return false;

            layerArpg.addProperty(EditorSetting.IMAGE_FILE_KEY_NAME, stack.getLayerName(i));
            layerArpg.addProperty(EditorSetting.IMAGE_FILE_KEY_OPACITY, stack.getOpacity(i));
            layerArpg.addProperty(EditorSetting.IMAGE_FILE_KEY_VISIBLE, stack.isLayerVisible(i));
            layerArpg.addProperty(EditorSetting.IMAGE_FILE_KEY_PIXELS, pixels);
            layerArray.add(layerArpg);
        }

        for (ImageMap map : ImageMap.VALUES) {

            if (!map.isGenerated())
                continue;

            ArpgElementStruct mapArpg = writeMap(project, map);

            if (mapArpg == null)
                return false;

            mapsArpg.add(ArpgUtility.toEnumName(map), mapArpg);
        }

        arpg.add(EditorSetting.IMAGE_FILE_KEY_LAYERS, layerArray);
        arpg.add(EditorSetting.IMAGE_FILE_KEY_MAPS, mapsArpg);
        imageRoot.mkdirs();

        return ArpgUtility.tryWriteObject(file, arpg);
    }

    // A map's settings and, once generated, its pixels; null when they cannot be encoded
    private ArpgObjectStruct writeMap(ImageProjectInstance project, ImageMap map) {

        ArpgObjectStruct mapArpg = new ArpgObjectStruct();
        ArpgObjectStruct settingsArpg = new ArpgObjectStruct();
        float[] settings = project.getMapSettings(map);
        ImageDocumentInstance document = project.getMap(map);

        for (MapGenerateOption option : map.getOptions())
            settingsArpg.addProperty(ArpgUtility.toEnumName(option), option.get(settings));

        mapArpg.add(EditorSetting.IMAGE_FILE_KEY_SETTINGS, settingsArpg);

        if (document == null)
            return mapArpg;

        byte[] pixels = imageManager.encodeImage(document.getPixmap());

        if (pixels == null)
            return null;

        mapArpg.addProperty(EditorSetting.IMAGE_FILE_KEY_PIXELS, pixels);
        return mapArpg;
    }

    // Delete \\

    boolean delete(ImageProjectInstance project) {

        File file = project.getFile();

        return !file.isFile() || file.delete();
    }
}
