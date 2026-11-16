package editor.bootstrap.imageeditorpipeline.imageeditormanager;

import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.bootstrap.imagepipeline.util.MapRowUtility;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;

class ImageEditorMapBranch extends BranchPackage {

    /*
     * The maps of an image open in the Image Editor, each a special layer the
     * Layers tab never lists: a map picked in the Maps tab is the one shown
     * and painted, and is not there until Generate works it out of the
     * image's layers as they stand by the map's own settings, through the one
     * generator every editor shares; generating again paints over it as one
     * undo step, and Clear erases it, so it is gone until generated again.
     * The albedo is the layers themselves.
     */

    // Internal
    private ImageEditorManager imageEditorManager;
    private ImageEditorLayerBranch imageEditorLayerBranch;
    private ImageManager imageManager;

    // Base \\

    @Override
    protected void get() {
        this.imageEditorManager = get(ImageEditorManager.class);
        this.imageEditorLayerBranch = get(ImageEditorLayerBranch.class);
        this.imageManager = get(ImageManager.class);
    }

    // Open \\

    ImageDocumentInstance openMap(ImageProjectInstance project, ImageMap map, Pixmap pixmap) {

        ImageDocumentInstance document = imageManager.openImage(
                imageEditorLayerBranch.toDocumentName(project, map.name()),
                project.getFile(),
                pixmap,
                false);

        project.setMap(map, document);
        return document;
    }

    // Maps \\

    void showMap(ImageProjectInstance project, ImageMap map) {
        project.setShownMap(map);
        imageEditorManager.notifyChanged();
    }

    void setValue(ImageProjectInstance project, MapGenerateOption option, float value) {
        project.getMapSettings(project.getShownMap())[option.ordinal()] = option.clamp(value);
        project.markMapsChanged(true);
        imageEditorManager.notifyChanged();
    }

    // The map shown worked out of the layers by its settings, made the first time and painted over as one undo step
    void generate(ImageProjectInstance project) {

        ImageMap map = project.getShownMap();

        if (!map.isGenerated()) {
            imageEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_ALBEDO);
            return;
        }

        int width = project.getWidth();
        int[] pixels = new int[width * project.getHeight()];
        ImageDocumentInstance document = project.getMap(map);

        imageManager.generateMap(map, project.getStack().getComposite(), project.getMapSettings(map), pixels);

        if (document == null) {
            document = openMap(project, map, new Pixmap(width, project.getHeight(), Pixmap.Format.RGBA8888));
            project.markMapsChanged(true);
        }

        int revision = document.getRevision();

        imageManager.beginPaint(document);

        for (int y = 0; y < project.getHeight(); y++)
            for (int x = 0; x < width; x++)
                imageManager.paintPixel(document, x, y, pixels[y * width + x]);

        imageManager.flushPaint(document);
        imageManager.endStroke(document);
        imageEditorManager.recordStep(project, document, revision);
        imageEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_GENERATED + map.getLabel());
    }

    // The map shown erased, gone until generated again
    void clear(ImageProjectInstance project) {

        ImageMap map = project.getShownMap();
        ImageDocumentInstance document = project.getMap(map);

        if (document == null) {
            imageEditorManager.setStatusMessage(map.isGenerated()
                    ? EditorSetting.MAPS_MESSAGE_NOT_GENERATED
                    : EditorSetting.MAPS_MESSAGE_ALBEDO);
            return;
        }

        imageEditorLayerBranch.closeDocument(project, document);
        project.setMap(map, null);
        project.markMapsChanged(true);
        imageEditorManager.setStatusMessage(EditorSetting.MAPS_MESSAGE_CLEARED + map.getLabel());
    }

    // Every map of an image let go, as it closes
    void closeMaps(ImageProjectInstance project) {

        for (ImageMap map : ImageMap.VALUES) {

            ImageDocumentInstance document = project.getMap(map);

            if (document != null)
                imageManager.closeImage(document.getImageName());
        }
    }

    // Accessible \\

    // A bit per map generated, by MapRowUtility's mask
    long getGeneratedMask(ImageProjectInstance project) {

        long mask = 0L;

        for (ImageMap map : ImageMap.VALUES)
            if (project.getMap(map) != null)
                mask |= MapRowUtility.toBit(map);

        return mask;
    }
}
