package editor.bootstrap.imagepipeline.imagemanager;

import java.io.File;
import java.util.function.Consumer;

import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import engine.assets.image.Pixmap;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImageManager extends ManagerPackage {

    /*
     * Owns every image open for editing, shared by every editor tool and
     * window that edits one — the framework image tools build on. A document
     * edits its owner's own pixels in place, so whatever samples them sees a
     * change the moment it is made. Every change goes through notifyEdited(),
     * which marks the document, queues its region for the GPU texture views
     * draw, and hands the region to the image's edit listeners. Strokes and
     * fills each make one undo step. Edits, history, disk access and the
     * texture each live in their own branch.
     */

    // Internal
    private ImageEditBranch imageEditBranch;
    private ImageHistoryBranch imageHistoryBranch;
    private ImageLibraryBranch imageLibraryBranch;
    private ImageTextureBranch imageTextureBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, ImageDocumentInstance> imageName2ImageDocument;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<ImageRegionStruct>>> imageName2EditListeners;

    // Scratch
    private ImageRegionStruct editRegion;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.imageEditBranch = create(ImageEditBranch.class);
        this.imageHistoryBranch = create(ImageHistoryBranch.class);
        this.imageLibraryBranch = create(ImageLibraryBranch.class);
        this.imageTextureBranch = create(ImageTextureBranch.class);

        // Palette
        this.imageName2ImageDocument = new Object2ObjectOpenHashMap<>();
        this.imageName2EditListeners = new Object2ObjectOpenHashMap<>();

        // Scratch
        this.editRegion = new ImageRegionStruct();
    }

    @Override
    protected void dispose() {
        for (ImageDocumentInstance document : imageName2ImageDocument.values())
            imageTextureBranch.delete(document);
    }

    // Update \\

    @Override
    protected void update() {
        for (ImageDocumentInstance document : imageName2ImageDocument.values())
            imageTextureBranch.upload(document);
    }

    // Management \\

    public ImageDocumentInstance openImage(String imageName, File file, Pixmap pixmap, boolean wrapping) {

        ImageDocumentInstance document = imageName2ImageDocument.get(imageName);

        if (document != null)
            return document;

        document = create(ImageDocumentInstance.class);
        document.constructor(imageName, file, pixmap, wrapping);
        imageName2ImageDocument.put(imageName, document);

        return document;
    }

    public void addEditListener(String imageName, Consumer<ImageRegionStruct> listener) {
        imageName2EditListeners.computeIfAbsent(imageName, name -> new ObjectArrayList<>()).add(listener);
    }

    // Strokes \\

    public void beginStroke(ImageDocumentInstance document, int x, int y, ImageBrushStruct brush) {

        endStroke(document);
        imageHistoryBranch.beginStep(document);
        document.beginStroke(x, y);

        editRegion.clear();
        imageEditBranch.stamp(document, x, y, brush, editRegion);
        notifyEdited(document, editRegion);
    }

    public void continueStroke(ImageDocumentInstance document, int x, int y, ImageBrushStruct brush) {

        if (!document.isStroking() || (x == document.getStrokeX() && y == document.getStrokeY()))
            return;

        editRegion.clear();
        imageEditBranch.stroke(document, document.getStrokeX(), document.getStrokeY(), x, y, brush, editRegion);
        document.moveStroke(x, y);
        notifyEdited(document, editRegion);
    }

    public void endStroke(ImageDocumentInstance document) {

        if (!document.isStroking())
            return;

        document.endStroke();
        imageHistoryBranch.commitStep(document);
    }

    public void fill(ImageDocumentInstance document, int x, int y, int color) {

        endStroke(document);
        imageHistoryBranch.beginStep(document);

        editRegion.clear();
        imageEditBranch.fill(document, x, y, color, editRegion);
        imageHistoryBranch.commitStep(document);
        notifyEdited(document, editRegion);
    }

    public int pickColor(ImageDocumentInstance document, int x, int y) {
        return imageEditBranch.readPixel(document, x, y);
    }

    // History \\

    public boolean undo(ImageDocumentInstance document) {

        endStroke(document);

        ImageRegionStruct region = imageHistoryBranch.undo(document);

        if (region == null)
            return false;

        notifyEdited(document, region);
        return true;
    }

    public boolean redo(ImageDocumentInstance document) {

        endStroke(document);

        ImageRegionStruct region = imageHistoryBranch.redo(document);

        if (region == null)
            return false;

        notifyEdited(document, region);
        return true;
    }

    // Disk \\

    public boolean saveImage(ImageDocumentInstance document) {

        endStroke(document);

        if (!imageLibraryBranch.save(document))
            return false;

        document.markSaved();
        return true;
    }

    public boolean reloadImage(ImageDocumentInstance document) {

        endStroke(document);

        if (!imageLibraryBranch.reload(document))
            return false;

        imageHistoryBranch.clear(document);

        editRegion.clear();
        editRegion.include(0, 0);
        editRegion.include(document.getWidth() - 1, document.getHeight() - 1);
        notifyEdited(document, editRegion);

        document.markSaved();
        return true;
    }

    // Texture \\

    // The texture is made and kept current only once a view asks for it; zero until its first upload
    public int useTexture(ImageDocumentInstance document) {

        document.requestTexture();
        return document.getTexture();
    }

    // Utility \\

    private void notifyEdited(ImageDocumentInstance document, ImageRegionStruct region) {

        if (region.isEmpty())
            return;

        document.markEdited();
        document.queueUpload(region);

        ObjectArrayList<Consumer<ImageRegionStruct>> listeners = imageName2EditListeners.get(
                document.getImageName());

        if (listeners != null)
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).accept(region);
    }

    // Accessible \\

    public boolean hasImage(String imageName) {
        return imageName2ImageDocument.containsKey(imageName);
    }

    public ImageDocumentInstance getImage(String imageName) {

        ImageDocumentInstance document = imageName2ImageDocument.get(imageName);

        if (document == null)
            throwException("No image named '" + imageName + "' is open.");

        return document;
    }
}
