package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.util.ItemEditorTool;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ItemEditorManager extends ManagerPackage {

    /*
     * Owns the item editor's shared state — open items, the active item and
     * tool, and the status line. Items stay open with their edits until the
     * editor closes. Disk access, model edits, and the hierarchy tab each live
     * in their own branch.
     */

    // Internal
    private TextureManager textureManager;
    private ItemLibraryBranch itemLibraryBranch;
    private ItemEditBranch itemEditBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, ItemDocumentInstance> itemName2ItemDocument;
    private ObjectArrayList<String> textureNames;

    // Active
    private ItemDocumentInstance activeDocument;
    private ItemEditorTool activeTool;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.itemLibraryBranch = create(ItemLibraryBranch.class);
        this.itemEditBranch = create(ItemEditBranch.class);
        create(ItemHierarchyBranch.class);

        // Palette
        this.itemName2ItemDocument = new Object2ObjectOpenHashMap<>();

        // Active
        this.activeTool = ItemEditorTool.PLACE;
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
    }

    // Items \\

    public void openItem(String itemName) {

        ItemDocumentInstance document = itemName2ItemDocument.get(itemName);

        if (document == null) {
            document = createDocument(itemName, itemLibraryBranch.loadModel(itemName), true);
            itemName2ItemDocument.put(itemName, document);
        }

        activate(document);
    }

    public void createItem(String itemName) {

        if (!isItemNameAvailable(itemName))
            throwException("Cannot create an item named '" + itemName + "'.");

        int center = EngineSetting.SUB_VOXEL_RESOLUTION / 2;
        SubVoxelModelStruct model = new SubVoxelModelStruct();
        int partIndex = model.addPart(
                new SubVoxelPartStruct(EditorSetting.ITEM_EDITOR_DEFAULT_PART_NAME, getTextureNames().get(0)));
        model.setCell(center, 0, center, partIndex);

        ItemDocumentInstance document = createDocument(itemName, model, false);
        itemName2ItemDocument.put(itemName, document);

        activate(document);
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CREATED + itemName);
    }

    public void saveActiveItem() {

        if (activeDocument == null)
            return;

        if (activeDocument.getModel().isEmpty()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_EMPTY);
            return;
        }

        itemLibraryBranch.save(activeDocument);
        activeDocument.markSaved();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_SAVED + activeDocument.getItemName());
    }

    public void reloadActiveItem() {

        if (activeDocument == null)
            return;

        if (!activeDocument.isSaved()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_SAVED);
            return;
        }

        activeDocument.replaceModel(itemLibraryBranch.loadModel(activeDocument.getItemName()));
        activeDocument.markSaved();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_RELOADED + activeDocument.getItemName());
    }

    public boolean isItemNameAvailable(String itemName) {
        return FileUtility.isValidFileName(itemName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !itemName2ItemDocument.containsKey(itemName)
                && !itemLibraryBranch.hasMesh(itemName);
    }

    private ItemDocumentInstance createDocument(String itemName, SubVoxelModelStruct model, boolean saved) {

        ItemDocumentInstance document = create(ItemDocumentInstance.class);
        document.constructor(itemName, model, saved);
        return document;
    }

    private void activate(ItemDocumentInstance document) {

        this.activeDocument = document;
        this.statusMessage = null;
        notifyChanged();
    }

    // Parts \\

    public void selectPart(int partIndex) {

        if (activeDocument == null)
            return;

        activeDocument.selectPart(partIndex);
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
            itemEditBranch.applyTool(activeDocument, activeTool, hit);
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

    private ObjectArrayList<String> getTextureNames() {

        if (textureNames == null)
            this.textureNames = textureManager.getTextureNamesInArray(EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY);

        if (textureNames.isEmpty())
            throwException("Item editor texture array '" + EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY
                    + "' holds no textures.");

        return textureNames;
    }

    // Accessible \\

    public ObjectArrayList<String> getItemNames() {

        ObjectOpenHashSet<String> itemNames = new ObjectOpenHashSet<>(itemLibraryBranch.getItemNames());
        itemNames.addAll(itemName2ItemDocument.keySet());

        ObjectArrayList<String> sortedNames = new ObjectArrayList<>(itemNames);
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
        return sortedNames;
    }

    public ItemDocumentInstance getDocument(String itemName) {
        return itemName2ItemDocument.get(itemName);
    }

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

        if (activeDocument == null)
            return EditorSetting.ITEM_EDITOR_STATUS_NO_ITEM;

        SubVoxelPartStruct part = activeDocument.getModel().getPart(activeDocument.getSelectedPartIndex());
        StringBuilder status = new StringBuilder(activeDocument.getItemName());

        if (activeDocument.isDirty())
            status.append(EditorSetting.ITEM_EDITOR_DIRTY_MARKER);

        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(part.getPartName());
        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(part.getTextureName());
        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(activeTool.getLabel());

        if (statusMessage != null)
            status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }
}
