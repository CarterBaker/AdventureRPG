package editor.bootstrap.imageeditorpipeline.imageproject;

import java.io.File;

import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImageProjectInstance extends InstancePackage {

    /*
     * One image open in the Image Editor, as a painting tool's document is:
     * its name and the .arpg file it saves to, the layered image itself, and
     * beside it every map it may carry — each an image document of its own
     * once generated, absent until then, a special layer the Layers tab
     * never lists — with each map's own settings and the map shown, the
     * albedo being the layers. It keeps the documents its undo and redo
     * steps painted, oldest first across its layers and maps, and whether
     * its maps changed since it was saved.
     */

    // Identity
    private String imageName;
    private File file;

    // Image — the layers, and the document their flattened result is shown through
    private ImageLayerStackStruct stack;
    private ImageDocumentInstance composite;

    // Maps — by ImageMap ordinal, null where not generated
    private ImageDocumentInstance[] maps;
    private float[][] mapSettings;
    private ImageMap shownMap;
    private boolean mapsChanged;

    // History
    private ObjectArrayList<ImageDocumentInstance> undoDocuments;
    private ObjectArrayList<ImageDocumentInstance> redoDocuments;

    // Internal \\

    @Override
    protected void create() {

        // Maps
        this.maps = new ImageDocumentInstance[ImageMap.VALUES.length];
        this.mapSettings = new float[ImageMap.VALUES.length][];

        for (int i = 0; i < mapSettings.length; i++)
            mapSettings[i] = MapGenerateOption.createDefaults();

        this.shownMap = ImageMap.ALBEDO;

        // History
        this.undoDocuments = new ObjectArrayList<>();
        this.redoDocuments = new ObjectArrayList<>();
    }

    // Constructor \\

    public void constructor(String imageName, File file, int width, int height) {

        // Identity
        this.imageName = imageName;
        this.file = file;

        // Image
        this.stack = new ImageLayerStackStruct(width, height, false);
    }

    // Image \\

    public void setComposite(ImageDocumentInstance composite) {
        this.composite = composite;
    }

    // Maps \\

    public void setMap(ImageMap map, ImageDocumentInstance document) {
        maps[map.ordinal()] = document;
    }

    public void setShownMap(ImageMap shownMap) {
        this.shownMap = shownMap;
    }

    public void markMapsChanged(boolean mapsChanged) {
        this.mapsChanged = mapsChanged;
    }

    // State \\

    // Whether anything painted, layered, generated or set has not yet been saved
    public boolean isDirty() {

        if (mapsChanged || stack.isDirty())
            return true;

        for (ImageDocumentInstance map : maps)
            if (map != null && map.isDirty())
                return true;

        return false;
    }

    // Accessible \\

    public String getImageName() {
        return imageName;
    }

    public File getFile() {
        return file;
    }

    public ImageLayerStackStruct getStack() {
        return stack;
    }

    public ImageDocumentInstance getComposite() {
        return composite;
    }

    public int getWidth() {
        return stack.getWidth();
    }

    public int getHeight() {
        return stack.getHeight();
    }

    public ImageDocumentInstance getMap(ImageMap map) {
        return maps[map.ordinal()];
    }

    public float[] getMapSettings(ImageMap map) {
        return mapSettings[map.ordinal()];
    }

    public ImageMap getShownMap() {
        return shownMap;
    }

    public boolean isMapsChanged() {
        return mapsChanged;
    }

    public ObjectArrayList<ImageDocumentInstance> getUndoDocuments() {
        return undoDocuments;
    }

    public ObjectArrayList<ImageDocumentInstance> getRedoDocuments() {
        return redoDocuments;
    }
}
