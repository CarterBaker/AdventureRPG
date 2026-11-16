package editor.bootstrap.imageeditorpipeline.imageeditormanager;

import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ImageEditorLayerBranch extends BranchPackage {

    /*
     * The layers of an image open in the Image Editor, as a painting tool
     * keeps them: each an image document laid flat into the image's
     * composite, a rectangle at a time, whenever anything changes it, the
     * composite shown through a document of its own; a new layer is clear
     * and goes on top, any layer may be removed while one remains, hidden,
     * renamed or faded, and the one picked is painted. Every change to the
     * stack lays the whole image flat again and marks it unsaved. Closing a
     * document takes its undo steps with it.
     */

    // Internal
    private ImageEditorManager imageEditorManager;
    private ImageManager imageManager;
    private ImageEditorMapBranch imageEditorMapBranch;

    // Scratch
    private ImageRegionStruct regionScratch;

    // Base \\

    @Override
    protected void create() {
        this.regionScratch = new ImageRegionStruct();
    }

    @Override
    protected void get() {
        this.imageEditorManager = get(ImageEditorManager.class);
        this.imageManager = get(ImageManager.class);
        this.imageEditorMapBranch = get(ImageEditorMapBranch.class);
    }

    // Open \\

    // The document the image's flattened layers are shown through
    void openComposite(ImageProjectInstance project) {
        project.setComposite(imageManager.openImage(
                toDocumentName(project, EditorSetting.IMAGE_EDITOR_DOCUMENT_COMPOSITE),
                project.getFile(),
                project.getStack().getComposite(),
                false));
    }

    // A layer on top of the stack as an image document, laid flat into the composite whenever anything changes it
    ImageDocumentInstance openLayer(
            ImageProjectInstance project,
            Pixmap pixmap,
            String layerName,
            float opacity,
            boolean visible) {

        ImageLayerStackStruct stack = project.getStack();
        String imageName = toDocumentName(
                project,
                EditorSetting.IMAGE_EDITOR_DOCUMENT_LAYER + stack.getNextLayerNumber());
        ImageDocumentInstance layer = imageManager.openImage(imageName, project.getFile(), pixmap, false);

        imageManager.addEditListener(imageName, region -> recomposite(
                project, region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY()));
        stack.addLayer(layer, layerName, opacity, visible);

        return layer;
    }

    // Composite \\

    void recomposite(ImageProjectInstance project, int minX, int minY, int maxX, int maxY) {

        imageManager.compositeLayers(project.getStack(), minX, minY, maxX, maxY);
        regionScratch.clear();
        regionScratch.include(minX, minY);
        regionScratch.include(maxX, maxY);
        project.getComposite().queueUpload(regionScratch);
    }

    void recompositeAll(ImageProjectInstance project) {
        recomposite(project, 0, 0, project.getWidth() - 1, project.getHeight() - 1);
    }

    // Layers \\

    void addLayer(ImageProjectInstance project) {

        ImageLayerStackStruct stack = project.getStack();

        if (stack.getLayerCount() >= EditorSetting.IMAGE_EDITOR_MAX_LAYERS) {
            imageEditorManager.setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_LAYER_FULL);
            return;
        }

        String layerName = EditorSetting.IMAGE_EDITOR_LAYER_NAME + stack.getNextLayerNumber();

        openLayer(project, new Pixmap(project.getWidth(), project.getHeight(), Pixmap.Format.RGBA8888),
                layerName, 1f, true);
        stack.setActiveLayer(stack.getLayerCount() - 1);
        changed(project, EditorSetting.IMAGE_EDITOR_MESSAGE_LAYER_ADDED + layerName);
    }

    // The layer painted removed with its history, the one below it painted next; the last layer stays
    void removeLayer(ImageProjectInstance project) {

        ImageLayerStackStruct stack = project.getStack();
        int layerIndex = stack.getActiveLayer();

        if (stack.getLayerCount() <= 1) {
            imageEditorManager.setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_LAYER_LAST);
            return;
        }

        String layerName = stack.getLayerName(layerIndex);

        closeDocument(project, stack.removeLayer(layerIndex));
        stack.setActiveLayer(Math.max(0, layerIndex - 1));
        changed(project, EditorSetting.IMAGE_EDITOR_MESSAGE_LAYER_REMOVED + layerName);
    }

    void selectLayer(ImageProjectInstance project, int layerIndex) {

        if (layerIndex < 0 || layerIndex >= project.getStack().getLayerCount())
            return;

        project.getStack().setActiveLayer(layerIndex);
        imageEditorManager.notifyChanged();
    }

    void toggleLayerVisible(ImageProjectInstance project, int layerIndex) {

        ImageLayerStackStruct stack = project.getStack();

        if (layerIndex < 0 || layerIndex >= stack.getLayerCount())
            return;

        stack.setLayerVisible(layerIndex, !stack.isLayerVisible(layerIndex));
        changed(project, null);
    }

    void setOpacity(ImageProjectInstance project, float opacity) {

        ImageLayerStackStruct stack = project.getStack();

        stack.setOpacity(stack.getActiveLayer(), Math.clamp(opacity, 0f, 1f));
        changed(project, null);
    }

    void renameLayer(ImageProjectInstance project, String layerName) {

        ImageLayerStackStruct stack = project.getStack();

        stack.setLayerName(stack.getActiveLayer(), layerName.strip());
        stack.markLayersChanged(true);
        imageEditorManager.notifyChanged();
    }

    // The layer whose document carries the key the Layers tab hands back, or none
    int findLayer(ImageProjectInstance project, String layerKey) {

        ImageLayerStackStruct stack = project.getStack();

        for (int i = 0; i < stack.getLayerCount(); i++)
            if (stack.getLayer(i).getImageName().equals(layerKey))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // A change to the stack: laid flat again, marked unsaved, and said in the status when there is something to say
    private void changed(ImageProjectInstance project, String message) {

        project.getStack().markLayersChanged(true);
        recompositeAll(project);

        if (message != null)
            imageEditorManager.setStatusMessage(message);
        else
            imageEditorManager.notifyChanged();
    }

    // Close \\

    // A layer or map no longer held, let go with its undo steps
    void closeDocument(ImageProjectInstance project, ImageDocumentInstance document) {
        project.getUndoDocuments().removeIf(step -> step == document);
        project.getRedoDocuments().removeIf(step -> step == document);
        imageManager.closeImage(document.getImageName());
    }

    // Every document of an image let go, as it closes
    void closeProject(ImageProjectInstance project) {

        ImageLayerStackStruct stack = project.getStack();

        for (int i = 0; i < stack.getLayerCount(); i++)
            imageManager.closeImage(stack.getLayer(i).getImageName());

        imageEditorMapBranch.closeMaps(project);

        if (project.getComposite() != null)
            imageManager.closeImage(project.getComposite().getImageName());
    }

    // Utility \\

    // A document's name in ImageManager, unique to its image
    String toDocumentName(ImageProjectInstance project, String part) {
        return EditorSetting.IMAGE_EDITOR_DOCUMENT_PREFIX + project.getImageName()
                + EditorSetting.IMAGE_EDITOR_DOCUMENT_SEPARATOR + part;
    }
}
