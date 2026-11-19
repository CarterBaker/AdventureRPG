package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.io.File;
import java.util.function.Predicate;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.materialspipeline.materialsmanager.MaterialsManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemEditorManager extends ManagerPackage {

    /*
     * Owns the item editor's shared state — open items, the active item and
     * the status line. Items are worked as a modelling tool works its
     * projects: New, Open, Save, Revert and Delete keep workspace items in
     * the workspace's Items folder, and Import brings the one open into the
     * game. An item of the game opens whenever it is picked in the
     * Hierarchy, in any tab: the item itself, the item an entry of another
     * schema names (a furnishing's, a loot table's), or the item that names
     * the entry (an instrument's); its mesh then saves in place, before the
     * definition naming it, an item the game could not load refused there
     * and a mesh only removed once no content names it. Every part of an
     * item is one material, listed as a layer in the Layers tab, the layer
     * picked being the part the tools lay; a material dropped from the
     * Materials tab colours the part under the pointer. The tools are the
     * Tools tab's for the item, stamping with the Brushes tab's brush. Items
     * stay open with their edits until saved, deleted, or the editor
     * closes. Disk access, the workspace, stamps and model edits each live in
     * their own branch.
     */

    // Internal
    private TextureManager textureManager;
    private InfoManager infoManager;
    private ToolManager toolManager;
    private MaterialsManager materialsManager;
    private ItemLibraryBranch itemLibraryBranch;
    private ItemWorkspaceBranch itemWorkspaceBranch;
    private ItemEditBranch itemEditBranch;
    private ItemBrushBranch itemBrushBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, ItemDocumentInstance> meshName2ItemDocument;
    private Object2ObjectOpenHashMap<String, ItemDocumentInstance> workspaceName2ItemDocument;
    private String fallbackMaterial;

    // Active
    private ItemDocumentInstance activeDocument;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.itemLibraryBranch = create(ItemLibraryBranch.class);
        this.itemWorkspaceBranch = create(ItemWorkspaceBranch.class);
        this.itemEditBranch = create(ItemEditBranch.class);
        this.itemBrushBranch = create(ItemBrushBranch.class);

        // Palette
        this.meshName2ItemDocument = new Object2ObjectOpenHashMap<>();
        this.workspaceName2ItemDocument = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.infoManager = get(InfoManager.class);
        this.toolManager = get(ToolManager.class);
        this.materialsManager = get(MaterialsManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(this::openEntry);
    }

    // Game Items \\

    // The item an entry picked in any Hierarchy tab stands for opened; an entry standing for none changes nothing
    private void openEntry(InfoEntryStruct entry) {

        InfoEntryStruct itemEntry = resolveItemEntry(entry);

        if (itemEntry == null)
            return;

        String meshName = toMeshName(itemEntry);

        if (meshName.isEmpty()) {
            activate(null);
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NO_MESH);
            return;
        }

        openItem(new ItemEntryStruct(itemEntry.getDefinitionName(), itemEntry.getEntryName(), meshName));
    }

    // An item itself, the first item a schema naming items names in the entry, or the first item naming the entry
    private InfoEntryStruct resolveItemEntry(InfoEntryStruct entry) {

        String schemaName = entry.getSchemaName();

        if (schemaName.equals(EditorSetting.INFO_SCHEMA_ITEMS))
            return entry;

        if (infoManager.referencesSchema(schemaName, EditorSetting.INFO_SCHEMA_ITEMS)) {

            InfoEntryStruct named = findNamedItem(entry.getArpg());

            if (named != null)
                return named;
        }

        return infoManager.referencesSchema(EditorSetting.INFO_SCHEMA_ITEMS, schemaName)
                ? findNamingItem(entry.getEntryName())
                : null;
    }

    // The first item a string anywhere in the tree names by its full name
    private InfoEntryStruct findNamedItem(ArpgElementStruct element) {

        String itemName = findString(element, this::isItemName);

        return itemName != null ? findItem(itemName) : null;
    }

    // The first item holding the name anywhere in its fields
    private InfoEntryStruct findNamingItem(String entryName) {

        ObjectArrayList<InfoEntryStruct> items = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ITEMS);

        for (int i = 0; i < items.size(); i++)
            if (findString(items.get(i).getArpg(), entryName::equals) != null)
                return items.get(i);

        return null;
    }

    private boolean isItemName(String text) {
        return findItem(text) != null;
    }

    private InfoEntryStruct findItem(String itemName) {
        return infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_ITEMS,
                ItemRegistryUtility.toDefinitionName(itemName),
                itemName.substring(itemName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1));
    }

    // The first string value anywhere in the tree the test takes, depth first in key order; null for none
    private static String findString(ArpgElementStruct element, Predicate<String> test) {

        if (element.isBuffer())
            return null;

        if (element.isObject()) {

            ArpgObjectStruct object = element.getAsObject();

            for (String key : object.keySet()) {

                String found = findString(object.get(key), test);

                if (found != null)
                    return found;
            }

            return null;
        }

        if (element.isArray()) {

            ArpgArrayStruct array = element.getAsArray();

            for (int i = 0; i < array.size(); i++) {

                String found = findString(array.get(i), test);

                if (found != null)
                    return found;
            }

            return null;
        }

        return element.isValue() && element.getAsValue().isString() && test.test(element.getAsString())
                ? element.getAsString()
                : null;
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
        SubVoxelModelStruct model = itemLibraryBranch.loadModel(entry, getFallbackMaterial());

        activate(createDocument(entry, model, conversion));

        if (conversion)
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CONVERTED);
    }

    private ItemDocumentInstance createDocument(ItemEntryStruct entry, SubVoxelModelStruct model, boolean dirty) {

        ItemDocumentInstance document = create(ItemDocumentInstance.class);
        document.constructor(entry, model, dirty);
        meshName2ItemDocument.put(entry.getMeshName(), document);
        return document;
    }

    private boolean hasItemDefinition(ItemEntryStruct entry) {
        return infoManager.hasEntry(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName(), entry.getLocalName());
    }

    // True while any content, an item or anything else, live or on disk, names the mesh
    private boolean isMeshReferenced(String meshName) {
        return infoManager.isResourceNamed(EngineSetting.MESH_PATH, meshName);
    }

    private String toMeshName(InfoEntryStruct itemEntry) {

        return ArpgUtility.hasString(itemEntry.getArpg(), EditorSetting.INFO_ITEM_MESH_FIELD)
                ? itemEntry.getArpg().get(EditorSetting.INFO_ITEM_MESH_FIELD).getAsString()
                : "";
    }

    // Workspace Items \\

    // A new workspace item, one cube of one part in the material chosen, written at once and opened
    public void createItem(String itemName) {

        if (!isItemNameAvailable(itemName))
            throwException("Cannot create an item named '" + itemName + "'.");

        SubVoxelModelStruct model = createStarterModel();

        if (!itemWorkspaceBranch.write(itemName, model)) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_WRITTEN + itemName);
            return;
        }

        activate(createWorkspaceDocument(itemName, model));
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CREATED + itemName);
    }

    public boolean isItemNameAvailable(String itemName) {
        return itemWorkspaceBranch.isNameAvailable(itemName) && !workspaceName2ItemDocument.containsKey(itemName);
    }

    // A workspace item picked in the system's open dialog; a file outside the Items folder is refused
    public void openItemFile(File file) {

        String itemName = itemWorkspaceBranch.toItemName(file);

        if (itemName == null) {
            setStatusMessage(EditorSetting.WORKSPACE_MESSAGE_OUTSIDE + itemWorkspaceBranch.getFolder().getPath());
            return;
        }

        ItemDocumentInstance document = workspaceName2ItemDocument.get(itemName);

        if (document != null) {
            activate(document);
            return;
        }

        SubVoxelModelStruct model = itemWorkspaceBranch.read(itemName, getFallbackMaterial());

        if (model == null) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_READ + itemName);
            return;
        }

        activate(createWorkspaceDocument(itemName, model));
    }

    private ItemDocumentInstance createWorkspaceDocument(String itemName, SubVoxelModelStruct model) {

        ItemDocumentInstance document = create(ItemDocumentInstance.class);
        document.constructWorkspace(itemName, model);
        workspaceName2ItemDocument.put(itemName, document);
        return document;
    }

    // Whether the open workspace item may go into the game, saying why not when it may not
    public boolean canImport() {

        if (activeDocument == null)
            return false;

        if (!activeDocument.isWorkspace()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_IMPORT_WORKSPACE);
            return false;
        }

        if (activeDocument.getModel().isEmpty()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_EMPTY);
            return false;
        }

        return true;
    }

    public boolean isImportDefinition(String definitionName) {
        return activeDocument != null && activeDocument.isWorkspace()
                && itemWorkspaceBranch.isImportDefinition(activeDocument.getWorkspaceName(), definitionName);
    }

    // The open workspace item saved, then brought into the game; the item made for it opens once it is
    public void importActiveItem(String definitionName) {

        if (!canImport())
            return;

        ItemDocumentInstance document = activeDocument;
        String itemName = document.getWorkspaceName();

        saveActiveItem();
        meshName2ItemDocument.remove(itemWorkspaceBranch.toMeshName(itemName));

        String refusal = itemWorkspaceBranch.importItem(itemName, document.getModel(), definitionName);

        setStatusMessage(refusal != null ? refusal : EditorSetting.ITEM_EDITOR_MESSAGE_IMPORTED + itemName);
    }

    public File getItemFolder() {
        return itemWorkspaceBranch.getFolder();
    }

    // File \\

    public void saveActiveItem() {

        if (activeDocument == null)
            return;

        if (activeDocument.getModel().isEmpty()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_EMPTY);
            return;
        }

        if (activeDocument.isWorkspace()) {
            saveWorkspaceItem();
            return;
        }

        ItemEntryStruct entry = activeDocument.getEntry();

        itemLibraryBranch.save(activeDocument);
        infoManager.notifyResourcesChanged();
        activeDocument.markClean();

        if (hasItemDefinition(entry)
                && !infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName())) {
            setStatusMessage(infoManager.getRefusal());
            return;
        }

        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_SAVED + activeDocument.getItemName());
    }

    private void saveWorkspaceItem() {

        if (!itemWorkspaceBranch.write(activeDocument.getWorkspaceName(), activeDocument.getModel())) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_WRITTEN + activeDocument.getItemName());
            return;
        }

        activeDocument.markClean();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_SAVED + activeDocument.getItemName());
    }

    public void reloadActiveItem() {

        if (activeDocument == null)
            return;

        if (activeDocument.isWorkspace()) {
            reloadWorkspaceItem();
            return;
        }

        ItemEntryStruct entry = activeDocument.getEntry();

        if (!itemLibraryBranch.hasMesh(entry.getMeshName())) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_SAVED);
            return;
        }

        boolean conversion = itemLibraryBranch.requiresConversion(entry);
        activeDocument.replaceModel(itemLibraryBranch.loadModel(entry, getFallbackMaterial()));

        if (conversion) {
            activeDocument.markEdited();
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_CONVERTED);
            return;
        }

        activeDocument.markClean();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_RELOADED + entry.getItemName());
    }

    private void reloadWorkspaceItem() {

        SubVoxelModelStruct model = itemWorkspaceBranch.read(activeDocument.getWorkspaceName(), getFallbackMaterial());

        if (model == null) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_READ + activeDocument.getItemName());
            return;
        }

        activeDocument.replaceModel(model);
        activeDocument.markClean();
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_RELOADED + activeDocument.getItemName());
    }

    public void deleteActiveItem() {

        if (activeDocument == null)
            return;

        if (activeDocument.isWorkspace()) {
            deleteWorkspaceItem();
            return;
        }

        ItemEntryStruct entry = activeDocument.getEntry();
        boolean definitionSaved = true;

        if (hasItemDefinition(entry)) {

            if (!infoManager.deleteEntry(
                    EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName(), entry.getLocalName())) {
                setStatusMessage(infoManager.getRefusal());
                return;
            }

            definitionSaved = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, entry.getDefinitionName());
        }

        if (definitionSaved && !isMeshReferenced(entry.getMeshName())) {
            itemLibraryBranch.deleteMesh(entry.getMeshName());
            infoManager.notifyResourcesChanged();
        }

        meshName2ItemDocument.remove(entry.getMeshName());
        activate(null);
        setStatusMessage(definitionSaved
                ? EditorSetting.ITEM_EDITOR_MESSAGE_DELETED + entry.getItemName()
                : infoManager.getRefusal());
    }

    private void deleteWorkspaceItem() {

        String itemName = activeDocument.getWorkspaceName();

        if (!itemWorkspaceBranch.delete(itemName)) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_NOT_DELETED + itemName);
            return;
        }

        workspaceName2ItemDocument.remove(itemName);
        activate(null);
        setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_DELETED + itemName);
    }

    public boolean isActiveItemName(String localName) {
        return activeDocument != null && activeDocument.getLocalName().equals(localName);
    }

    private SubVoxelModelStruct createStarterModel() {

        int center = EngineSetting.SUB_VOXEL_RESOLUTION / 2;
        SubVoxelModelStruct model = new SubVoxelModelStruct();
        int partIndex = model.addPart(new SubVoxelPartStruct(
                EditorSetting.ITEM_EDITOR_DEFAULT_PART_NAME, materialsManager.getSelectedMaterial()));

        model.setCell(center, 0, center, partIndex);
        return model;
    }

    private void activate(ItemDocumentInstance document) {

        itemBrushBranch.endStroke();
        this.activeDocument = document;
        this.statusMessage = null;
        notifyChanged();
    }

    // Parts \\

    public void selectPart(int partIndex) {

        if (activeDocument == null || !activeDocument.getModel().hasPart(partIndex))
            return;

        activeDocument.selectPart(partIndex);
        notifyChanged();
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
        return activeDocument != null
                && FileUtility.isValidFileName(partName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !ItemEditBranch.hasPartNamed(activeDocument.getModel(), partName);
    }

    // Layers \\

    // Every part of the item a layer, by its name, with the material it is drawn in; the selected part active
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        if (activeDocument == null)
            return;

        SubVoxelModelStruct model = activeDocument.getModel();

        for (int i = 0; i < model.getPartCount(); i++) {

            SubVoxelPartStruct part = model.getPart(i);

            roots.add(new LayerNodeStruct(
                    part.getPartName(),
                    part.getPartName() + EditorSetting.ITEM_EDITOR_TEXTURE_SEPARATOR
                            + CharacterViewUtility.toResourceLabel(part.getTextureName()),
                    i == activeDocument.getSelectedPartIndex(),
                    false));
        }
    }

    public void selectLayer(String layerKey) {

        if (activeDocument == null)
            return;

        SubVoxelModelStruct model = activeDocument.getModel();

        for (int i = 0; i < model.getPartCount(); i++)
            if (model.getPart(i).getPartName().equals(layerKey)) {
                selectPart(i);
                return;
            }
    }

    // The tab's actions, run exactly as the Layers tab's icons and the Tools tab run them
    public void act(String actionKey) {

        if (activeDocument == null)
            return;

        if (actionKey.equals(EditorSetting.ITEM_EDITOR_KEY_PART_ADD))
            itemEditBranch.addPart(activeDocument, materialsManager.getSelectedMaterial());
        else if (actionKey.equals(EditorSetting.ITEM_EDITOR_KEY_PART_REMOVE))
            itemEditBranch.removeSelectedPart(activeDocument);
    }

    // Materials \\

    // The material dropped on the part the pointer struck, that part then selected; false when it struck nothing
    public boolean dropMaterial(SubVoxelHitStruct hit, String materialName) {

        if (activeDocument == null || !hit.hasTarget()) {
            setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_DROP_MISSED);
            return false;
        }

        SubVoxelModelStruct model = activeDocument.getModel();
        int partIndex = hit.isTargetWall()
                ? model.getWallPart(hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ())
                : model.getCellPart(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());

        activeDocument.selectPart(partIndex);
        itemEditBranch.setPartMaterial(activeDocument, partIndex, materialName);
        return true;
    }

    // Tools \\

    // A press of the tool chosen for the item: Brush and Erase begin a stroke, the rest act once
    public void pressTool(SubVoxelHitStruct hit) {

        if (activeDocument == null)
            return;

        switch (getTool()) {
            case BRUSH -> itemBrushBranch.beginStroke(activeDocument, true, hit);
            case ERASE -> eraseAt(hit);
            case FILL -> itemBrushBranch.fill(activeDocument, hit);
            case WALL -> itemEditBranch.placeWall(activeDocument, hit);
            case PICK -> itemEditBranch.pick(activeDocument, hit);
            default -> {
            }
        }
    }

    // A struck wall is taken away alone; cubes are dug by a stroke
    private void eraseAt(SubVoxelHitStruct hit) {

        if (hit.hasTarget() && hit.isTargetWall())
            itemEditBranch.eraseWall(activeDocument, hit);
        else
            itemBrushBranch.beginStroke(activeDocument, false, hit);
    }

    public void dragTool(Vector3 rayOrigin, Vector3 rayDirection) {

        if (activeDocument != null)
            itemBrushBranch.strokeTo(activeDocument, rayOrigin, rayDirection);
    }

    public void releaseTool() {
        itemBrushBranch.endStroke();
    }

    // Whether the tool chosen stamps with the brush, built or dug, rather than marking one cube or wall
    public boolean isStampTool() {
        return getTool() == EditorTool.BRUSH || getTool() == EditorTool.ERASE;
    }

    // The cubes the tool's stamp would reach where the pointer is, laid into the preview; false when none
    public boolean buildStampPreview(SubVoxelHitStruct hit, SubVoxelModelStruct preview) {
        return activeDocument != null && isStampTool()
                && itemBrushBranch.buildPreview(activeDocument, getTool() == EditorTool.BRUSH, hit, preview);
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

    // The material a quad mesh converted to cubes is drawn in, the item pack's first
    private String getFallbackMaterial() {

        if (fallbackMaterial != null)
            return fallbackMaterial;

        ObjectArrayList<String> textureNames = textureManager.getTextureNamesInArray(
                EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY);

        if (textureNames.isEmpty())
            throwException("Item editor texture array '" + EditorSetting.ITEM_EDITOR_TEXTURE_ARRAY
                    + "' holds no textures.");

        this.fallbackMaterial = textureNames.get(0);
        return fallbackMaterial;
    }

    // Accessible \\

    public ItemDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    public EditorTool getTool() {
        return toolManager.getTool(ToolCanvas.ITEM);
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
        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR)
                .append(CharacterViewUtility.toResourceLabel(part.getTextureName()));
        status.append(EditorSetting.ITEM_EDITOR_STATUS_SEPARATOR).append(getTool().getLabel());
    }
}
