package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import editor.bootstrap.itemeditorpipeline.util.ItemEditorTool;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemEditorManager extends ManagerPackage {

    /*
     * Owns the item editor's shared state — open items, the active item and
     * tool, and the status line. Items stay open with their edits until saved,
     * deleted, or the editor closes. New items join the active item's
     * definition file, or the editor's own when nothing is open. Disk access,
     * model edits, and the hierarchy tab each live in their own branch.
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

        if (document != null) {
            activate(document);
            return;
        }

        ItemEntryStruct entry = itemLibraryBranch.getEntry(itemName);

        if (entry == null)
            throwException("Cannot open unknown item '" + itemName + "'.");

        boolean conversion = itemLibraryBranch.requiresConversion(entry);
        SubVoxelModelStruct model = itemLibraryBranch.loadModel(entry, getTextureNames().get(0));

        document = createDocument(entry, model, conversion);
        activate(document);

        if (conversion)
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CONVERTED);
    }

    public void createItem(String localName) {

        if (!isItemNameAvailable(localName))
            throwException("Cannot create an item named '" + localName + "'.");

        int center = EngineSetting.SUB_VOXEL_RESOLUTION / 2;
        SubVoxelModelStruct model = new SubVoxelModelStruct();
        int partIndex = model.addPart(
                new SubVoxelPartStruct(EditorSetting.ITEM_EDITOR_DEFAULT_PART_NAME, getTextureNames().get(0)));
        model.setCell(center, 0, center, partIndex);

        activate(createDocument(createEntry(localName), model, true));
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CREATED + localName);
    }

    public void saveActiveItem() {

        if (activeDocument == null)
            return;

        if (activeDocument.getModel().isEmpty()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_EMPTY);
            return;
        }

        itemLibraryBranch.save(activeDocument);
        activeDocument.markClean();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_SAVED + activeDocument.getItemName());
    }

    public void reloadActiveItem() {

        if (activeDocument == null)
            return;

        ItemEntryStruct entry = activeDocument.getEntry();

        if (!itemLibraryBranch.hasItem(entry.getItemName())) {
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

        if (itemLibraryBranch.hasItem(entry.getItemName()))
            itemLibraryBranch.delete(entry);

        itemName2ItemDocument.remove(entry.getItemName());
        activate(null);
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_DELETED + entry.getItemName());
    }

    public boolean isItemNameAvailable(String localName) {

        if (!FileUtility.isValidFileName(localName, EditorSetting.NAME_INPUT_MAX_LENGTH))
            return false;

        ItemEntryStruct entry = createEntry(localName);

        return !itemLibraryBranch.hasItem(entry.getItemName())
                && !itemName2ItemDocument.containsKey(entry.getItemName())
                && !itemLibraryBranch.hasMesh(entry.getMeshName())
                && !isMeshOpen(entry.getMeshName());
    }

    public boolean isActiveItemName(String localName) {
        return activeDocument != null && activeDocument.getEntry().getLocalName().equals(localName);
    }

    private ItemEntryStruct createEntry(String localName) {

        String definitionName = activeDocument != null
                ? activeDocument.getEntry().getDefinitionName()
                : EditorSetting.ITEM_EDITOR_DEFINITION_FILE;

        return new ItemEntryStruct(
                definitionName,
                localName,
                EditorSetting.ITEM_EDITOR_MESH_DIRECTORY + "/" + localName);
    }

    private ItemDocumentInstance createDocument(ItemEntryStruct entry, SubVoxelModelStruct model, boolean dirty) {

        ItemDocumentInstance document = create(ItemDocumentInstance.class);
        document.constructor(entry, model, dirty);
        itemName2ItemDocument.put(entry.getItemName(), document);
        return document;
    }

    private void activate(ItemDocumentInstance document) {

        this.activeDocument = document;
        this.statusMessage = null;
        notifyChanged();
    }

    private boolean isMeshOpen(String meshName) {

        for (ItemDocumentInstance document : itemName2ItemDocument.values())
            if (document.getEntry().getMeshName().equals(meshName))
                return true;

        return false;
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

    public ObjectArrayList<ItemEntryStruct> getItemEntries() {

        ObjectArrayList<ItemEntryStruct> entries = itemLibraryBranch.getEntries();

        for (ItemDocumentInstance document : itemName2ItemDocument.values())
            if (!itemLibraryBranch.hasItem(document.getItemName()))
                entries.add(document.getEntry());

        entries.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getItemName(), b.getItemName()));
        return entries;
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

        StringBuilder status = new StringBuilder();

        if (activeDocument == null)
            status.append(EditorSetting.ITEM_EDITOR_STATUS_NO_ITEM);
        else
            appendDocumentStatus(status);

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
