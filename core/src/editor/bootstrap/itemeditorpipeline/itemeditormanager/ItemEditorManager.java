package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import editor.bootstrap.itemeditorpipeline.util.ItemEditorTool;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemEditorManager extends ManagerPackage {

    /*
     * Owns the item editor's shared state — open item meshes, the active item,
     * tool, and brush texture, and the status line. Items themselves are JSON
     * entries owned by InfoManager: selecting one there opens the mesh it
     * names here, starting a fresh model when that mesh has no file yet, and
     * new and deleted items go through it. Meshes stay open with their edits
     * until saved, deleted, or the editor closes. New items join the active
     * item's definition file, or the editor's own when nothing is open. Disk
     * access and model edits each live in their own branch.
     */

    // Internal
    private TextureManager textureManager;
    private InfoManager infoManager;
    private ItemLibraryBranch itemLibraryBranch;
    private ItemEditBranch itemEditBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, ItemDocumentInstance> meshName2ItemDocument;
    private ObjectArrayList<String> textureNames;

    // Active
    private ItemDocumentInstance activeDocument;
    private ItemEditorTool activeTool;
    private String brushTextureName;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.itemLibraryBranch = create(ItemLibraryBranch.class);
        this.itemEditBranch = create(ItemEditBranch.class);

        // Palette
        this.meshName2ItemDocument = new Object2ObjectOpenHashMap<>();

        // Active
        this.activeTool = ItemEditorTool.PLACE;
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ITEMS, this::openEntry);
    }

    // Items \\

    private void openEntry(InfoEntryStruct entry) {

        String meshName = toMeshName(entry);

        if (meshName.isEmpty()) {
            activate(null);
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NO_MESH);
            return;
        }

        openItem(new ItemEntryStruct(entry.getDefinitionName(), entry.getEntryName(), meshName));
    }

    private void openItem(ItemEntryStruct entry) {

        ItemDocumentInstance document = meshName2ItemDocument.get(entry.getMeshName());

        if (document != null) {
            document.setEntry(entry);
            activate(document);
            return;
        }

        if (!itemLibraryBranch.hasMesh(entry.getMeshName())) {
            activate(createDocument(entry, createStarterModel(), true));
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NEW_MESH);
            return;
        }

        boolean conversion = itemLibraryBranch.requiresConversion(entry);
        SubVoxelModelStruct model = itemLibraryBranch.loadModel(entry, getTextureNames().get(0));

        activate(createDocument(entry, model, conversion));

        if (conversion)
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CONVERTED);
    }

    public void createItem(String localName) {

        if (!isItemNameAvailable(localName))
            throwException("Cannot create an item named '" + localName + "'.");

        ItemEntryStruct entry = toItemEntry(localName);

        infoManager.createEntry(
                EditorSetting.INFO_SCHEMA_ITEMS,
                entry.getDefinitionName(),
                localName,
                itemJson -> itemJson.addProperty(EditorSetting.INFO_ITEM_MESH_FIELD, entry.getMeshName()));
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CREATED + localName);
    }

    public void saveActiveItem() {

        if (activeDocument == null)
            return;

        if (activeDocument.getModel().isEmpty()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_EMPTY);
            return;
        }

        ItemEntryStruct entry = activeDocument.getEntry();

        itemLibraryBranch.save(activeDocument);

        if (hasItemDefinition(entry))
            infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName());

        activeDocument.markClean();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_SAVED + activeDocument.getItemName());
    }

    public void reloadActiveItem() {

        if (activeDocument == null)
            return;

        ItemEntryStruct entry = activeDocument.getEntry();

        if (!itemLibraryBranch.hasMesh(entry.getMeshName())) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_SAVED);
            return;
        }

        boolean conversion = itemLibraryBranch.requiresConversion(entry);
        activeDocument.replaceModel(itemLibraryBranch.loadModel(entry, getTextureNames().get(0)));

        if (conversion) {
            activeDocument.markEdited();
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CONVERTED);
            return;
        }

        activeDocument.markClean();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_RELOADED + entry.getItemName());
    }

    public void deleteActiveItem() {

        if (activeDocument == null)
            return;

        ItemEntryStruct entry = activeDocument.getEntry();

        if (hasItemDefinition(entry)) {
            infoManager.deleteEntry(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName(), entry.getLocalName());
            infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName());
        }

        if (!isMeshReferenced(entry.getMeshName()))
            itemLibraryBranch.deleteMesh(entry.getMeshName());

        meshName2ItemDocument.remove(entry.getMeshName());
        activate(null);
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_DELETED + entry.getItemName());
    }

    public boolean isItemNameAvailable(String localName) {

        if (!FileUtility.isValidFileName(localName, EditorSetting.NAME_INPUT_MAX_LENGTH))
            return false;

        ItemEntryStruct entry = toItemEntry(localName);

        return !hasItemDefinition(entry)
                && !itemLibraryBranch.hasMesh(entry.getMeshName())
                && !meshName2ItemDocument.containsKey(entry.getMeshName())
                && !isMeshReferenced(entry.getMeshName());
    }

    public boolean isActiveItemName(String localName) {
        return activeDocument != null && activeDocument.getEntry().getLocalName().equals(localName);
    }

    private ItemEntryStruct toItemEntry(String localName) {

        String definitionName = activeDocument != null
                ? activeDocument.getEntry().getDefinitionName()
                : EditorSetting.ITEM_EDITOR_DEFINITION_FILE;

        return new ItemEntryStruct(
                definitionName,
                localName,
                EditorSetting.ITEM_EDITOR_MESH_DIRECTORY + "/" + localName);
    }

    private SubVoxelModelStruct createStarterModel() {

        int center = EngineSetting.SUB_VOXEL_RESOLUTION / 2;
        SubVoxelModelStruct model = new SubVoxelModelStruct();
        int partIndex = model.addPart(
                new SubVoxelPartStruct(EditorSetting.ITEM_EDITOR_DEFAULT_PART_NAME, getTextureNames().get(0)));

        model.setCell(center, 0, center, partIndex);
        return model;
    }

    private ItemDocumentInstance createDocument(ItemEntryStruct entry, SubVoxelModelStruct model, boolean dirty) {

        ItemDocumentInstance document = create(ItemDocumentInstance.class);
        document.constructor(entry, model, dirty);
        meshName2ItemDocument.put(entry.getMeshName(), document);
        return document;
    }

    private void activate(ItemDocumentInstance document) {

        this.activeDocument = document;
        this.statusMessage = null;
        notifyChanged();
    }

    private boolean hasItemDefinition(ItemEntryStruct entry) {
        return infoManager.hasEntry(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName(), entry.getLocalName());
    }

    private boolean isMeshReferenced(String meshName) {

        ObjectArrayList<InfoEntryStruct> itemEntries = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ITEMS);

        for (int i = 0; i < itemEntries.size(); i++)
            if (toMeshName(itemEntries.get(i)).equals(meshName))
                return true;

        return false;
    }

    private String toMeshName(InfoEntryStruct itemEntry) {

        return JsonUtility.hasString(itemEntry.getJson(), EditorSetting.INFO_ITEM_MESH_FIELD)
                ? itemEntry.getJson().get(EditorSetting.INFO_ITEM_MESH_FIELD).getAsString()
                : "";
    }

    // Parts \\

    public void selectPart(int partIndex) {

        if (activeDocument == null || !activeDocument.getModel().hasPart(partIndex))
            return;

        activeDocument.selectPart(partIndex);
        this.brushTextureName = null;
        notifyChanged();
    }

    public void addPart(String partName) {

        if (activeDocument != null)
            itemEditBranch.addPart(activeDocument, partName);
    }

    public void removeSelectedPart() {

        if (activeDocument != null)
            itemEditBranch.removeSelectedPart(activeDocument);
    }

    public void renameSelectedPart(String partName) {

        if (activeDocument != null)
            itemEditBranch.renameSelectedPart(activeDocument, partName);
    }

    public boolean isPartNameAvailable(String partName) {

        if (activeDocument == null || !FileUtility.isValidFileName(partName, EditorSetting.NAME_INPUT_MAX_LENGTH))
            return false;

        SubVoxelModelStruct model = activeDocument.getModel();

        for (int i = 0; i < model.getPartCount(); i++)
            if (model.getPart(i).getPartName().equals(partName))
                return false;

        return true;
    }

    public void cycleTexture(int direction) {

        if (activeDocument != null)
            itemEditBranch.cycleTexture(activeDocument, getTextureNames(), direction);
    }

    // Tools \\

    public void setTool(ItemEditorTool tool) {

        this.activeTool = tool;
        notifyChanged();
    }

    public void applyTool(SubVoxelHitStruct hit) {

        if (activeDocument != null)
            itemEditBranch.applyTool(activeDocument, activeTool, hit, brushTextureName);
    }

    // Brush \\

    public void selectBrushTexture(String textureName) {

        if (!getTextureNames().contains(textureName))
            throwException("'" + textureName + "' is not in the item texture array '"
                    + EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY + "'.");

        this.brushTextureName = textureName;
        notifyChanged();
    }

    public void clearBrush() {

        this.brushTextureName = null;
        notifyChanged();
    }

    public String getBrushTextureName() {
        return brushTextureName;
    }

    // Status \\

    void notifyChanged() {
        revision++;
    }

    void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    // Utility \\

    public ObjectArrayList<String> getTextureNames() {

        if (textureNames == null)
            this.textureNames = textureManager.getTextureNamesInArray(EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY);

        if (textureNames.isEmpty())
            throwException("Item editor texture array '" + EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY
                    + "' holds no textures.");

        return textureNames;
    }

    // Accessible \\

    public ItemDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    public ItemEditorTool getTool() {
        return activeTool;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {

        StringBuilder status = new StringBuilder();

        if (activeDocument == null)
            status.append(EditorSetting.ITEM_EDITOR_STATUS_NO_ITEM);
        else
            appendDocumentStatus(status);

        if (brushTextureName != null)
            status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR)
                    .append(EditorSetting.ITEM_EDITOR_STATUS_BRUSH)
                    .append(brushTextureName);

        if (statusMessage != null)
            status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    private void appendDocumentStatus(StringBuilder status) {

        SubVoxelPartStruct part = activeDocument.getModel().getPart(activeDocument.getSelectedPartIndex());
        status.append(activeDocument.getItemName());

        if (activeDocument.isDirty())
            status.append(EditorSetting.ITEM_EDITOR_DIRTY_MARKER);

        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(part.getPartName());
        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(part.getTextureName());
        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(activeTool.getLabel());
    }
}
