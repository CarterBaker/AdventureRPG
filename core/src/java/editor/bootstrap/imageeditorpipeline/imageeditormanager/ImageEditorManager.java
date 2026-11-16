package editor.bootstrap.imageeditorpipeline.imageeditormanager;

import java.io.File;

import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.bootstrap.imagepipeline.imagebrush.ImageBrushStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagelayer.ImageLayerStackStruct;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageColorUtility;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.bootstrap.imagepipeline.util.ImageStroke;
import editor.bootstrap.imagepipeline.util.MapGenerateUtility;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImageEditorManager extends ManagerPackage {

    /*
     * Owns every image open in the Image Editor and the one being worked, as
     * a painting tool owns its documents; every Image Editor tab, the Layers
     * tab, the Maps tab and the Properties tab read it here. An image is made
     * through New at a size and background, opened by stepping through the
     * Images folder, and saved, copied, reverted and deleted through the File
     * menu. Its strokes paint the layer picked, or the map shown, with the
     * Tools tab's tool, the Brushes tab's brush and the Color Wheel's colour,
     * the one colour every painting window shares: Brush lays it over the
     * layer, Erase wears the layer clear and a map back to its neutral,
     * Smooth, Swirl and Scatter work what is there, Fill floods a run of one
     * colour and Pick takes the colour shown. Each stroke, fill and generated
     * map is one undo step of the document it painted, kept in order across
     * the image. Layers, maps, the image's files and its rows each live in
     * their own branch; the revision moves whenever what any tab shows may.
     */

    // Internal
    private ImageManager imageManager;
    private BrushManager brushManager;
    private ToolManager toolManager;
    private CharacterEditorManager characterEditorManager;
    private ImageProjectBranch imageProjectBranch;
    private ImageEditorLayerBranch imageEditorLayerBranch;
    private ImageEditorMapBranch imageEditorMapBranch;
    private ImageEditorRowBranch imageEditorRowBranch;

    // Images — open by name, and the one worked
    private Object2ObjectOpenHashMap<String, ImageProjectInstance> imageName2Project;
    private ImageProjectInstance activeProject;

    // Stroke — the brush, what each tool strokes, and the document a stroke paints with its revision at the start
    private ImageBrushStruct brush;
    private ImageStroke[] tool2Stroke;
    private ImageDocumentInstance strokeDocument;
    private int strokeRevision;

    // State
    private String statusMessage;
    private int revision;

    // Scratch
    private Color colorScratch;
    private ObjectArrayList<String> nameScratch;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.imageProjectBranch = create(ImageProjectBranch.class);
        this.imageEditorLayerBranch = create(ImageEditorLayerBranch.class);
        this.imageEditorMapBranch = create(ImageEditorMapBranch.class);
        this.imageEditorRowBranch = create(ImageEditorRowBranch.class);

        // Images
        this.imageName2Project = new Object2ObjectOpenHashMap<>();

        // Stroke
        this.brush = new ImageBrushStruct();
        this.tool2Stroke = new ImageStroke[EditorTool.values().length];
        this.tool2Stroke[EditorTool.BRUSH.ordinal()] = ImageStroke.PAINT;
        this.tool2Stroke[EditorTool.ERASE.ordinal()] = ImageStroke.ERASE;
        this.tool2Stroke[EditorTool.SMOOTH.ordinal()] = ImageStroke.SMOOTH;
        this.tool2Stroke[EditorTool.SWIRL.ordinal()] = ImageStroke.SWIRL;
        this.tool2Stroke[EditorTool.SCATTER.ordinal()] = ImageStroke.SCATTER;

        // State
        this.statusMessage = EditorSetting.IMAGE_EDITOR_MESSAGE_NONE;

        // Scratch
        this.colorScratch = new Color();
        this.nameScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.imageManager = get(ImageManager.class);
        this.brushManager = get(BrushManager.class);
        this.toolManager = get(ToolManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    // Images \\

    // A new image of the size asked, its one layer filled with the background, opened and worked
    public void newImage(String imageName, int width, int height, int background) {

        if (!isImageNameAvailable(imageName))
            return;

        ImageProjectInstance project = createProject(imageName, width, height);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                pixmap.drawPixel(x, y, background);

        imageEditorLayerBranch.openLayer(project, pixmap, EditorSetting.IMAGE_EDITOR_LAYER_BACKGROUND, 1f, true);
        imageEditorLayerBranch.recompositeAll(project);
        project.getStack().markLayersChanged(true);
        activate(project);
        setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_CREATED + imageName);
    }

    // An image by name, the open one or its file read; the status says why when it cannot be
    public void openImage(String imageName) {

        ImageProjectInstance project = imageName2Project.get(imageName);

        if (project == null)
            project = readProject(imageName);

        if (project == null) {
            setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_NOT_READ + imageName);
            return;
        }

        activate(project);
    }

    // The next or previous image among those saved and those open, in name order; the first or last with none worked
    public void cycleImage(int direction) {

        collectImageNames(nameScratch);

        if (nameScratch.isEmpty())
            return;

        int index = activeProject != null
                ? nameScratch.indexOf(activeProject.getImageName())
                : direction > 0 ? EngineSetting.INDEX_NOT_FOUND : nameScratch.size();

        openImage(nameScratch.get(Math.floorMod(index + direction, nameScratch.size())));
    }

    public void saveImage() {
        if (activeProject != null)
            save(activeProject);
    }

    public void saveAllImages() {
        for (ImageProjectInstance project : imageName2Project.values())
            if (project.isDirty())
                save(project);
    }

    // The image put back as its file holds it, or closed when it was never saved
    public void revertImage() {

        if (activeProject == null)
            return;

        endStroke();

        String imageName = activeProject.getImageName();

        closeProject(activeProject);

        if (imageProjectBranch.hasFile(imageName))
            openImage(imageName);

        setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_REVERTED + imageName);
    }

    // The image's file removed and the image closed
    public void deleteImage() {

        if (activeProject == null)
            return;

        String imageName = activeProject.getImageName();

        if (!imageProjectBranch.delete(activeProject)) {
            setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_NOT_DELETED + imageName);
            return;
        }

        closeProject(activeProject);
        setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_DELETED + imageName);
    }

    // The image as it stands saved under a new name, then that copy opened
    public void copyImage(String imageName) {

        if (activeProject == null || !isImageNameAvailable(imageName))
            return;

        endStroke();

        ImageProjectInstance project = activeProject;
        File file = imageProjectBranch.resolveFile(imageName);

        if (!imageProjectBranch.write(project, file)) {
            setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_NOT_SAVED + imageName);
            return;
        }

        openImage(imageName);
    }

    private void save(ImageProjectInstance project) {

        endStroke();

        if (!imageProjectBranch.write(project, project.getFile())) {
            setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_NOT_SAVED + project.getImageName());
            return;
        }

        markSaved(project);
        setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_SAVED + project.getImageName());
    }

    private void markSaved(ImageProjectInstance project) {

        ImageLayerStackStruct stack = project.getStack();

        for (int i = 0; i < stack.getLayerCount(); i++)
            stack.getLayer(i).markSaved();

        for (ImageMap map : ImageMap.VALUES)
            if (project.getMap(map) != null)
                project.getMap(map).markSaved();

        stack.markLayersChanged(false);
        project.markMapsChanged(false);
    }

    // The single call sites that open and close an image
    private ImageProjectInstance createProject(String imageName, int width, int height) {

        ImageProjectInstance project = create(ImageProjectInstance.class);

        project.constructor(imageName, imageProjectBranch.resolveFile(imageName), width, height);
        imageEditorLayerBranch.openComposite(project);
        imageName2Project.put(imageName, project);

        return project;
    }

    private ImageProjectInstance readProject(String imageName) {

        ArpgObjectStruct arpg = imageProjectBranch.readFile(imageName);

        if (arpg == null)
            return null;

        int width = imageProjectBranch.readWidth(arpg);
        int height = imageProjectBranch.readHeight(arpg);

        if (!isSizeValid(width) || !isSizeValid(height))
            return null;

        ImageProjectInstance project = createProject(imageName, width, height);

        if (!imageProjectBranch.load(project, arpg) || project.getStack().getLayerCount() == 0) {
            closeProject(project);
            return null;
        }

        imageEditorLayerBranch.recompositeAll(project);
        markSaved(project);

        return project;
    }

    private void closeProject(ImageProjectInstance project) {

        imageEditorLayerBranch.closeProject(project);
        imageName2Project.remove(project.getImageName());

        if (activeProject == project)
            this.activeProject = null;

        notifyChanged();
    }

    private void activate(ImageProjectInstance project) {
        endStroke();
        this.activeProject = project;
        notifyChanged();
    }

    // Every image saved in the folder and every one open, once each, in name order
    private void collectImageNames(ObjectArrayList<String> out) {

        out.clear();
        imageProjectBranch.collectImageNames(out);

        for (String imageName : imageName2Project.keySet())
            if (!out.contains(imageName))
                out.add(imageName);

        out.sort(null);
    }

    // Strokes \\

    // The tool's work begun at an image pixel, rows top first: a stroke, a fill or a pick
    public void beginStroke(int pixelX, int pixelY) {

        ImageDocumentInstance target = resolveTarget();

        if (target == null)
            return;

        EditorTool tool = toolManager.getTool(ToolCanvas.IMAGE_EDITOR);

        switch (tool) {

            case FILL -> {
                int before = target.getRevision();
                imageManager.fill(target, pixelX, pixelY, resolvePaintColor());
                recordStep(activeProject, target, before);
            }

            case PICK -> pickColor(pixelX, pixelY);

            default -> beginBrush(target, tool, pixelX, pixelY);
        }
    }

    public void continueStroke(int pixelX, int pixelY) {
        if (strokeDocument != null)
            imageManager.continueStroke(strokeDocument, pixelX, pixelY, brush);
    }

    public void endStroke() {

        if (strokeDocument == null)
            return;

        imageManager.endStroke(strokeDocument);
        recordStep(activeProject, strokeDocument, strokeRevision);
        this.strokeDocument = null;
    }

    private void beginBrush(ImageDocumentInstance target, EditorTool tool, int pixelX, int pixelY) {

        ImageStroke stroke = tool2Stroke[tool.ordinal()];

        if (stroke == null)
            return;

        ToolCanvas canvas = ToolCanvas.IMAGE_EDITOR;
        boolean layer = activeProject.getShownMap() == ImageMap.ALBEDO;

        brush.set(
                brushManager.getActiveBrush(),
                stroke,
                Math.round(toolManager.getOption(canvas, ToolOption.SIZE)),
                toolManager.getOption(canvas, ToolOption.STRENGTH),
                toolManager.getOption(canvas, ToolOption.TWIST),
                canvas.isBlending());
        brush.setTransparent(layer);
        brush.setColor(stroke == ImageStroke.ERASE && !layer
                ? activeProject.getShownMap().getNeutralColor()
                : resolvePaintColor());

        this.strokeDocument = target;
        this.strokeRevision = target.getRevision();
        imageManager.beginStroke(target, pixelX, pixelY, brush);
    }

    // The colour shown at a pixel, laid flat or the map's, made the paint colour every painting window shares
    private void pickColor(int pixelX, int pixelY) {

        Pixmap shown = resolveShownPixmap();
        int picked = shown.getPixel(
                Math.clamp(pixelX, 0, shown.getWidth() - 1),
                Math.clamp(pixelY, 0, shown.getHeight() - 1));

        colorScratch.set(
                ImageColorUtility.toChannel(picked, 0) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                ImageColorUtility.toChannel(picked, 1) / EngineSetting.COLOR_CHANNEL_BYTE_MAX,
                ImageColorUtility.toChannel(picked, 2) / EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        characterEditorManager.setPaintHSB(
                colorScratch.getSaturation() > 0f ? colorScratch.getHue() : characterEditorManager.getPaintHSB(0),
                colorScratch.getSaturation(),
                colorScratch.getBrightness());
        notifyChanged();
    }

    // The document the tools paint: the layer picked while the albedo is shown, otherwise the map shown once made
    private ImageDocumentInstance resolveTarget() {

        if (activeProject == null) {
            setStatusMessage(EditorSetting.IMAGE_EDITOR_MESSAGE_NO_IMAGE);
            return null;
        }

        ImageMap shownMap = activeProject.getShownMap();

        if (shownMap == ImageMap.ALBEDO) {
            ImageLayerStackStruct stack = activeProject.getStack();
            return stack.getLayer(stack.getActiveLayer());
        }

        ImageDocumentInstance map = activeProject.getMap(shownMap);

        if (map == null)
            setStatusMessage(EditorSetting.MAPS_MESSAGE_NOT_GENERATED);

        return map;
    }

    private int resolvePaintColor() {
        return MapGenerateUtility.toColor(
                characterEditorManager.getPaintColor(0),
                characterEditorManager.getPaintColor(1),
                characterEditorManager.getPaintColor(2));
    }

    // History \\

    // A document painted since its revision stood at the one given becomes the image's newest undo step
    void recordStep(ImageProjectInstance project, ImageDocumentInstance document, int revisionBefore) {

        if (document.getRevision() == revisionBefore)
            return;

        project.getUndoDocuments().add(document);
        project.getRedoDocuments().clear();

        while (project.getUndoDocuments().size() > EditorSetting.IMAGE_HISTORY_MAX_STEPS)
            project.getUndoDocuments().remove(0);

        notifyChanged();
    }

    public void undo() {
        if (activeProject != null)
            stepHistory(activeProject.getUndoDocuments(), activeProject.getRedoDocuments(), true);
    }

    public void redo() {
        if (activeProject != null)
            stepHistory(activeProject.getRedoDocuments(), activeProject.getUndoDocuments(), false);
    }

    private void stepHistory(
            ObjectArrayList<ImageDocumentInstance> from,
            ObjectArrayList<ImageDocumentInstance> to,
            boolean back) {

        endStroke();

        while (!from.isEmpty()) {

            ImageDocumentInstance document = from.pop();

            if (back ? imageManager.undo(document) : imageManager.redo(document)) {
                to.push(document);
                notifyChanged();
                return;
            }
        }
    }

    // Layers \\

    // The image's layers top first, the one painted active and each with its eye; nothing with no image open
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        if (activeProject == null)
            return;

        ImageLayerStackStruct stack = activeProject.getStack();

        for (int i = stack.getLayerCount() - 1; i >= 0; i--)
            roots.add(new LayerNodeStruct(
                    stack.getLayer(i).getImageName(),
                    stack.getLayerName(i),
                    i == stack.getActiveLayer() && activeProject.getShownMap() == ImageMap.ALBEDO,
                    true,
                    stack.isLayerVisible(i),
                    false));
    }

    // A layer picked is painted, and the albedo shown so it is
    public void selectLayer(String layerKey) {

        if (activeProject == null)
            return;

        endStroke();
        activeProject.setShownMap(ImageMap.ALBEDO);
        imageEditorLayerBranch.selectLayer(activeProject, imageEditorLayerBranch.findLayer(activeProject, layerKey));
    }

    public void toggleLayerVisible(String layerKey) {
        if (activeProject != null)
            imageEditorLayerBranch.toggleLayerVisible(
                    activeProject,
                    imageEditorLayerBranch.findLayer(activeProject, layerKey));
    }

    // Rows \\

    public void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        imageEditorRowBranch.buildPropertyRows(activeProject, rows);
    }

    public void buildMapRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        imageEditorRowBranch.buildMapRows(activeProject, rows);
    }

    public void select(String key) {
        endStroke();
        imageEditorRowBranch.select(activeProject, key);
    }

    public void step(String key, int steps) {
        imageEditorRowBranch.step(activeProject, key, steps);
    }

    public void cycle(String key, int direction) {
        imageEditorRowBranch.cycle(activeProject, key, direction);
    }

    public void act(String key) {
        endStroke();
        imageEditorRowBranch.act(activeProject, key);
    }

    public String getValueText(String key) {
        return imageEditorRowBranch.getValueText(activeProject, key);
    }

    public boolean isValueValid(String key, String text) {
        return imageEditorRowBranch.isValueValid(activeProject, key, text);
    }

    public void setValue(String key, String text) {
        imageEditorRowBranch.setValue(activeProject, key, text);
    }

    // State \\

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        notifyChanged();
    }

    void notifyChanged() {
        revision++;
    }

    // Utility \\

    // A name a new image can take: a plain file name no image saved or open has
    public boolean isImageNameAvailable(String imageName) {
        return imageName != null && imageName.matches(EditorSetting.IMAGE_EDITOR_NAME_PATTERN)
                && !imageName2Project.containsKey(imageName) && !imageProjectBranch.hasFile(imageName);
    }

    public boolean isActiveImageName(String imageName) {
        return activeProject != null && activeProject.getImageName().equals(imageName);
    }

    public boolean isSizeValid(int size) {
        return size >= EditorSetting.IMAGE_EDITOR_SIZE_MIN && size <= EditorSetting.IMAGE_EDITOR_SIZE_MAX;
    }

    // The colour a new image's background takes, by its place among the background choices
    public int resolveBackground(int background) {
        return background == EditorSetting.IMAGE_EDITOR_BACKGROUND_PAINT
                ? resolvePaintColor()
                : EditorSetting.IMAGE_EDITOR_BACKGROUND_COLORS[background];
    }

    // Accessible \\

    public boolean hasActiveImage() {
        return activeProject != null;
    }

    public ImageProjectInstance getActiveProject() {
        return activeProject;
    }

    // The pixels shown: the layers laid flat while the albedo is, otherwise the map's once made
    public Pixmap resolveShownPixmap() {

        ImageDocumentInstance map = activeProject.getMap(activeProject.getShownMap());

        return map != null ? map.getPixmap() : activeProject.getStack().getComposite();
    }

    // The texture of what is shown, made and kept current once asked for; zero for a map not made, and until the
    // first upload
    public int useShownTexture() {

        ImageMap shownMap = activeProject.getShownMap();

        if (shownMap == ImageMap.ALBEDO)
            return imageManager.useTexture(activeProject.getComposite());

        ImageDocumentInstance map = activeProject.getMap(shownMap);

        return map != null ? imageManager.useTexture(map) : 0;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getRevision() {
        return revision;
    }
}
