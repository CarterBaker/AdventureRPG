package editor.bootstrap.materialspipeline.materialsmanager;

import java.io.File;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.bootstrap.materialspipeline.materials.MaterialDocumentStruct;
import editor.bootstrap.materialspipeline.materials.MaterialDropProvider;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.colorwheel.ColorWheelContext;
import editor.runtime.materials.MaterialsContext;
import engine.assets.image.Pixmap;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialsManager extends ManagerPackage {

    /*
     * Owns the editor's materials: every texture of every baked pack, pack by
     * pack in name order, the one chosen, which every window that lays a
     * material reads, as the Brushes tab holds the one brush every canvas
     * strokes with, and the material of the workspace open, made from a
     * texture with maps and parameters of its own, as a texturing tool makes
     * one. Choosing the open material makes it the one laid: the Material
     * Viewer shows it and the Paint tab lays its maps, while
     * the hair and items, which take only the game's textures, take its base
     * until it is imported. Every edit of the open material is a step of its
     * undo history. A material pressed in the Materials tab is chosen
     * and picked up; held, it is carried over the windows, and once let go it
     * is dropped on the window under the pointer when that window takes
     * materials. A release is seen on the first frame no drag is reported,
     * whichever runs first in a frame, the tab's drag or this update. The
     * revision moves whenever the list or what is chosen changes, the value
     * revision whenever only a value of the open material does. Painting
     * lays the material chosen instead of the colour while the Materials tab
     * was worked in after the Color Wheel, as a texturing tool's fill swaps
     * between a colour and a material; working in the Color Wheel again
     * paints the colour.
     */

    // Internal
    private TextureManager textureManager;
    private WindowManager windowManager;
    private ImageEditorManager imageEditorManager;
    private ActiveWindowManager activeWindowManager;
    private MaterialDocumentBranch materialDocumentBranch;
    private MaterialSampleBranch materialSampleBranch;
    private MaterialThumbnailBranch materialThumbnailBranch;
    private MaterialRowBranch materialRowBranch;
    private MaterialHistoryBranch materialHistoryBranch;

    // Palette
    private ObjectArrayList<String> packNames;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<String>> packName2MaterialNames;
    private ObjectArrayList<String> materialNames;

    // Selection — the texture chosen, the workspace material open, and whether it is the one chosen
    private String selectedMaterial;
    private MaterialDocumentStruct document;
    private boolean documentChosen;

    // Drag — the material carried, and whether its drag was reported since the last update
    private String draggedMaterial;
    private boolean dragReported;

    // Directory
    private File exportRoot;

    // Status
    private int revision;
    private int valueRevision;
    private String statusMessage;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.materialDocumentBranch = create(MaterialDocumentBranch.class);
        this.materialSampleBranch = create(MaterialSampleBranch.class);
        this.materialThumbnailBranch = create(MaterialThumbnailBranch.class);
        this.materialRowBranch = create(MaterialRowBranch.class);
        this.materialHistoryBranch = create(MaterialHistoryBranch.class);

        // Palette
        this.packName2MaterialNames = new Object2ObjectOpenHashMap<>();
        this.materialNames = new ObjectArrayList<>();

        // Selection
        this.selectedMaterial = EditorSetting.MATERIALS_DEFAULT_MATERIAL;

        // Directory
        this.exportRoot = new File(
                WorkspaceUtility.resolveFolder(internal.path, EditorSetting.IMAGE_EXPORT_DIRECTORY),
                EditorSetting.MATERIALS_EXPORT_DIRECTORY);

        // Status
        this.statusMessage = "";
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.windowManager = get(WindowManager.class);
        this.imageEditorManager = get(ImageEditorManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (draggedMaterial == null)
            return;

        if (dragReported) {
            dragReported = false;
            return;
        }

        String material = draggedMaterial;
        draggedMaterial = null;
        drop(material);
    }

    // Palette \\

    // The packs and their materials listed once, on first use, after every pack has loaded at bootstrap
    private void resolvePalette() {

        if (packNames != null)
            return;

        this.packNames = textureManager.getPackNames();

        for (int i = 0; i < packNames.size(); i++) {

            ObjectArrayList<String> names = textureManager.getTextureNamesInArray(packNames.get(i));

            packName2MaterialNames.put(packNames.get(i), names);
            materialNames.addAll(names);
        }
    }

    // The material after or before one, pack by pack, the first or last for one not in the list
    public String stepMaterialName(String materialName, int direction) {

        resolvePalette();

        if (materialNames.isEmpty())
            return materialName;

        int index = materialNames.indexOf(materialName);

        if (index == EngineSetting.INDEX_NOT_FOUND)
            return materialNames.get(direction > 0 ? 0 : materialNames.size() - 1);

        return materialNames.get(Math.floorMod(index + direction, materialNames.size()));
    }

    // Selection \\

    // A texture of the game chosen, the open material, if any, no longer the one laid
    public void selectMaterial(String materialName) {

        if (!hasMaterial(materialName) || materialName.equals(selectedMaterial) && !documentChosen)
            return;

        this.selectedMaterial = materialName;
        this.documentChosen = false;
        changeSource();
    }

    // The material chosen stepped through every material, pack by pack
    public void cycleMaterial(int direction) {
        selectMaterial(stepMaterialName(selectedMaterial, direction));
    }

    // The open material of the workspace chosen again, its base the texture the hair and items take
    public void chooseDocument() {

        if (document == null || documentChosen)
            return;

        this.documentChosen = true;
        this.selectedMaterial = document.getBaseTexture();
        changeSource();
    }

    // What is laid changed: the list marks it, and the images the old one read are let go
    private void changeSource() {

        this.statusMessage = "";
        materialSampleBranch.retain(getChosenDocument(), selectedMaterial);
        revision++;
    }

    // Drag \\

    // Reported every frame the material's row is held, from the press until it is let go
    public void dragMaterial(String materialName) {

        selectMaterial(materialName);
        this.draggedMaterial = materialName;
        this.dragReported = true;
    }

    private void drop(String materialName) {

        WindowInstance window = windowManager.getHoveredWindow();
        ContextPackage context = window != null && window.hasContext() ? window.getContext() : null;

        if (context instanceof MaterialDropProvider provider)
            provider.dropMaterial(materialName);
    }

    // Edit \\

    void setBaseTexture(String textureName) {

        if (document == null || !hasMaterial(textureName))
            return;

        materialHistoryBranch.record(document, null);
        document.setBaseTexture(textureName);

        if (documentChosen)
            this.selectedMaterial = textureName;

        changeSource();
    }

    void setMapTexture(int map, String textureName) {

        if (document == null || !textureName.isEmpty() && !hasMaterial(textureName))
            return;

        materialHistoryBranch.record(document, null);
        document.setMapTexture(map, textureName);
        changeSource();
    }

    // A map taken from an image file, read again so a file changed since it was last read shows as it is now
    void setMapImage(int map, File imageFile) {

        if (document == null)
            return;

        Pixmap image = materialSampleBranch.readImage(imageFile);

        if (image == null) {
            setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NOT_IMAGE + imageFile.getPath());
            return;
        }

        String imagePath = materialDocumentBranch.toImagePath(imageFile);

        image.dispose();
        materialHistoryBranch.record(document, null);
        materialSampleBranch.forgetImage(imagePath);
        document.setMapImage(map, imagePath);
        changeSource();
        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_IMAGE + imagePath);
    }

    // A parameter set, the steps of one row in a row recorded as one edit
    void setParameter(int parameter, float value, String rowKey) {

        if (document == null)
            return;

        boolean wasDirty = document.isDirty();

        materialHistoryBranch.record(document, rowKey);
        document.setParameter(parameter, value);
        valueRevision++;

        if (!wasDirty)
            revision++;
    }

    // Sample \\

    // A map's texel of the material laid where a UV falls, repeating by its tiling and shaped by its parameters, as
    // a texturing tool's brush lays a material's channels
    public int sampleMap(int map, float u, float v) {

        float tiling = getParameter(EditorSetting.MATERIALS_PARAMETER_TILING);

        return materialSampleBranch.sample(getChosenDocument(), selectedMaterial, map, u * tiling, v * tiling);
    }

    // The editor texture holding a map's image of the material laid, before its parameters, as the Material Viewer
    // reads it; zero until its first upload
    public boolean hasThumbnail(String materialName) {
        return materialThumbnailBranch.hasThumbnail(materialName);
    }

    // The material's thumbnail in the Materials tab, made from its albedo the first time it is asked for
    public int resolveThumbnail(String materialName) {
        return materialThumbnailBranch.resolveThumbnail(materialName);
    }

    public int usePreviewTexture(int map) {
        return materialSampleBranch.usePreviewTexture(getChosenDocument(), selectedMaterial, map);
    }

    // Rows \\

    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        materialRowBranch.buildRows(rows);
    }

    public void stepRow(String key, int steps) {
        materialRowBranch.step(key, steps);
    }

    public void cycleRow(String key, int direction) {
        materialRowBranch.cycle(key, direction);
    }

    public String getRowValueText(String key) {
        return materialRowBranch.getValueText(key);
    }

    public boolean isRowValueValid(String key, String text) {
        return materialRowBranch.isValueValid(key, text);
    }

    public void setRowValue(String key, String text) {
        materialRowBranch.setValue(key, text);
    }

    // The map an image row names taken from the image file chosen
    public void setRowImage(String key, File imageFile) {
        materialRowBranch.setImage(key, imageFile);
    }

    // The folder the image dialog starts in
    public File getImageFolder() {
        return materialDocumentBranch.getImageFolder();
    }

    // History \\

    public void undoMaterial() {

        if (requireDocument() && materialHistoryBranch.undo(document))
            restore();
    }

    public void redoMaterial() {

        if (requireDocument() && materialHistoryBranch.redo(document))
            restore();
    }

    // A step put back shown everywhere: the base the hair and items take, the images read, every row and value
    private void restore() {

        if (documentChosen)
            this.selectedMaterial = document.getBaseTexture();

        changeSource();
        valueRevision++;
    }

    // Files \\

    public boolean isMaterialNameAvailable(String materialName) {
        return materialDocumentBranch.isNameAvailable(materialName);
    }

    // A new material of the workspace made from the texture chosen, written at once, then opened and chosen
    public void newMaterial(String materialName) {

        if (!isMaterialNameAvailable(materialName))
            return;

        MaterialDocumentStruct created = new MaterialDocumentStruct(materialName, selectedMaterial);

        if (!materialDocumentBranch.write(created)) {
            setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NOT_WRITTEN + materialName);
            return;
        }

        created.markSaved();
        open(created);
        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_CREATED + materialName);
    }

    // The workspace folder every material is kept in, where the open dialog starts
    public File getMaterialFolder() {
        return materialDocumentBranch.getFolder();
    }

    // A material file picked in the open dialog opened and chosen; the status says why when it cannot be
    public void openMaterialFile(File file) {

        String materialName = materialDocumentBranch.toMaterialName(file);

        if (materialName == null) {
            setStatusMessage(EditorSetting.WORKSPACE_MESSAGE_OUTSIDE + getMaterialFolder().getPath());
            return;
        }

        MaterialDocumentStruct read = materialDocumentBranch.read(materialName);

        if (read == null || !hasMaterial(read.getBaseTexture())) {
            setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NOT_READ + materialName);
            return;
        }

        open(read);
    }

    private void open(MaterialDocumentStruct opened) {

        materialHistoryBranch.clear();
        this.document = opened;
        this.documentChosen = false;
        chooseDocument();
    }

    public void saveMaterial() {

        if (!requireDocument())
            return;

        if (!materialDocumentBranch.write(document)) {
            setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NOT_WRITTEN + document.getMaterialName());
            return;
        }

        document.markSaved();
        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_SAVED + document.getMaterialName());
    }

    // The open material read again as its file holds it
    public void revertMaterial() {

        if (!requireDocument())
            return;

        MaterialDocumentStruct read = materialDocumentBranch.read(document.getMaterialName());

        if (read == null || !hasMaterial(read.getBaseTexture())) {
            setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NOT_READ + document.getMaterialName());
            return;
        }

        boolean chosen = documentChosen;

        materialHistoryBranch.clear();
        this.document = read;

        if (chosen)
            this.selectedMaterial = read.getBaseTexture();

        changeSource();
        valueRevision++;
        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_REVERTED + read.getMaterialName());
    }

    // Whether a name is the open material's, the confirmation Delete asks for
    public boolean isOpenMaterialName(String materialName) {
        return document != null && document.getMaterialName().equals(materialName);
    }

    // The open material's file removed and the material closed, its base staying the texture chosen
    public void deleteMaterial() {

        if (!requireDocument())
            return;

        String materialName = document.getMaterialName();

        if (!materialDocumentBranch.delete(materialName)) {
            setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NOT_DELETED + materialName);
            return;
        }

        materialHistoryBranch.clear();
        this.document = null;
        this.documentChosen = false;
        changeSource();
        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_DELETED + materialName);
    }

    // Exchange \\

    // Every map of the open material written as a PNG in the workspace's Exports folder, as a texturing tool exports
    // a material's channels
    public void exportMaterial() {

        if (!requireDocument())
            return;

        File folder = resolveExportFolder();
        int written = materialSampleBranch.export(document, folder, document.getMaterialName());

        setStatusMessage(written == EngineSetting.INDEX_NOT_FOUND
                ? EditorSetting.IMAGE_EXPORT_MESSAGE_NOT_WRITTEN + document.getMaterialName()
                : EditorSetting.IMAGE_EXPORT_MESSAGE_WRITTEN + written + EditorSetting.IMAGE_EXPORT_MESSAGE_TO
                        + folder.getPath());
    }

    // Whether the open material has exported maps to import; the status says so when it has none
    public boolean canImport() {

        if (!requireDocument())
            return false;

        if (imageEditorManager.hasMaps(resolveExportFolder()))
            return true;

        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_EXPORT_FIRST);
        return false;
    }

    // The open material's exported maps imported into the game as a tile of the pack, baked, and cleared from the
    // workspace, as an image's are
    public void importMaterial(String packName, String tileName) {

        if (!requireDocument())
            return;

        setStatusMessage(imageEditorManager.importMaps(resolveExportFolder(), packName, tileName));
    }

    private File resolveExportFolder() {
        return new File(exportRoot, document.getMaterialName());
    }

    // Whether a material is open; the status says how to open one when none is
    private boolean requireDocument() {

        if (document != null)
            return true;

        setStatusMessage(EditorSetting.MATERIALS_MESSAGE_NONE_OPEN);
        return false;
    }

    // Status \\

    private void setStatusMessage(String message) {
        this.statusMessage = message;
        revision++;
    }

    // Accessible \\

    public ObjectArrayList<String> getPackNames() {

        resolvePalette();
        return packNames;
    }

    public ObjectArrayList<String> getMaterialNames(String packName) {

        resolvePalette();
        return packName2MaterialNames.get(packName);
    }

    // Whether painting lays the material chosen rather than the colour: the Materials tab worked in after the Color
    // Wheel
    public boolean isPaintingMaterial() {

        WindowInstance recent = activeWindowManager.findRecent(MaterialsContext.class, ColorWheelContext.class);

        return recent != null && recent.getContext() instanceof MaterialsContext;
    }

    public boolean hasMaterial(String materialName) {
        return textureManager.hasTexture(materialName);
    }

    // The texture of the game chosen: the open material's base while it is the one chosen
    public String getSelectedMaterial() {
        return selectedMaterial;
    }

    // The workspace material open, null while none is
    public MaterialDocumentStruct getDocument() {
        return document;
    }

    // The workspace material laid, null while a texture of the game is
    public MaterialDocumentStruct getChosenDocument() {
        return documentChosen ? document : null;
    }

    // A parameter of the material laid, its default for a texture of the game
    public float getParameter(int parameter) {
        return documentChosen
                ? document.getParameter(parameter)
                : EditorSetting.MATERIALS_PARAMETER_DEFAULT[parameter];
    }

    // The name of the material laid: the open material's, or the texture's
    public String getChosenName() {
        return documentChosen ? document.getMaterialName() : selectedMaterial;
    }

    // What the last file action reported, empty since the choice last changed
    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isDragging() {
        return draggedMaterial != null;
    }

    public int getRevision() {
        return revision;
    }

    public int getValueRevision() {
        return valueRevision;
    }
}
