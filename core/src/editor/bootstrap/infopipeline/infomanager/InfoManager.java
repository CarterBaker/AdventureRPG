package editor.bootstrap.infopipeline.infomanager;

import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchymanager.HierarchyManager;
import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.infotab.InfoTabInstance;
import editor.bootstrap.infopipeline.infotarget.InfoTargetStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.editor.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class InfoManager extends ManagerPackage {

    /*
     * Owns the editor's JSON content — every schema, every file opened under
     * it, and the one shared selection the hierarchy and every info panel
     * work on. Each schema becomes a hierarchy tab, the first one active until
     * another is picked. Files load the first time their tab is shown and keep
     * their edits until saved or reverted. A selection is a folder, a file, or
     * an entry; creating, deleting, saving, and every field edit act on it,
     * and listeners registered for a schema hear each entry selected or edited
     * in it. Disk access, JSON edits, panel rows, and hierarchy nodes each
     * live in their own branch.
     */

    // Internal
    private HierarchyManager hierarchyManager;
    private InfoLibraryBranch infoLibraryBranch;
    private InfoEditBranch infoEditBranch;
    private InfoRowBranch infoRowBranch;
    private InfoHierarchyBranch infoHierarchyBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, InfoSchemaHandle> schemaName2InfoSchemaHandle;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<InfoDocumentInstance>> schemaName2Documents;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<InfoEntryStruct>>> schemaName2Listeners;

    // Selection
    private InfoSchemaHandle activeSchema;
    private InfoDocumentInstance selectedDocument;
    private String selectedEntryName;
    private String selectedFolderName;

    // Expansion
    private ObjectOpenHashSet<String> toggledPaths;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.infoLibraryBranch = create(InfoLibraryBranch.class);
        this.infoEditBranch = create(InfoEditBranch.class);
        this.infoRowBranch = create(InfoRowBranch.class);
        this.infoHierarchyBranch = create(InfoHierarchyBranch.class);

        // Palette
        this.schemaName2InfoSchemaHandle = new Object2ObjectOpenHashMap<>();
        this.schemaName2Documents = new Object2ObjectOpenHashMap<>();
        this.schemaName2Listeners = new Object2ObjectOpenHashMap<>();

        // Expansion
        this.toggledPaths = new ObjectOpenHashSet<>();

        create(InfoSchemaLoader.class);
    }

    @Override
    protected void get() {
        this.hierarchyManager = get(HierarchyManager.class);
    }

    @Override
    protected void awake() {

        ((InfoSchemaLoader) internalLoader).requestAll();
        registerTabs();
    }

    private void registerTabs() {

        ObjectArrayList<InfoSchemaHandle> schemas = new ObjectArrayList<>(schemaName2InfoSchemaHandle.values());

        schemas.sort((a, b) -> a.getOrder() != b.getOrder()
                ? Integer.compare(a.getOrder(), b.getOrder())
                : String.CASE_INSENSITIVE_ORDER.compare(a.getTabName(), b.getTabName()));

        for (int i = 0; i < schemas.size(); i++) {

            InfoTabInstance tab = create(InfoTabInstance.class);
            tab.constructor(schemas.get(i), this);
            hierarchyManager.registerTabProvider(tab);
        }

        if (!schemas.isEmpty())
            this.activeSchema = schemas.get(0);
    }

    // Management \\

    void addSchema(InfoSchemaHandle schemaHandle) {
        schemaName2InfoSchemaHandle.put(schemaHandle.getSchemaName(), schemaHandle);
    }

    public void addSelectionListener(String schemaName, Consumer<InfoEntryStruct> listener) {
        schemaName2Listeners.computeIfAbsent(schemaName, name -> new ObjectArrayList<>()).add(listener);
    }

    // Documents \\

    ObjectArrayList<InfoDocumentInstance> getDocuments(InfoSchemaHandle schema) {

        ObjectArrayList<InfoDocumentInstance> documents = schemaName2Documents.get(schema.getSchemaName());

        if (documents == null) {
            documents = loadDocuments(schema);
            schemaName2Documents.put(schema.getSchemaName(), documents);
        }

        return documents;
    }

    private ObjectArrayList<InfoDocumentInstance> loadDocuments(InfoSchemaHandle schema) {

        ObjectArrayList<String> definitionNames = infoLibraryBranch.scanDefinitionNames(schema);
        ObjectArrayList<InfoDocumentInstance> documents = new ObjectArrayList<>(definitionNames.size());

        for (int i = 0; i < definitionNames.size(); i++) {

            JsonObject root = infoLibraryBranch.load(schema, definitionNames.get(i));

            if (root != null)
                documents.add(createDocument(schema, definitionNames.get(i), root, true));
        }

        return documents;
    }

    private InfoDocumentInstance createDocument(
            InfoSchemaHandle schema,
            String definitionName,
            JsonObject root,
            boolean onDisk) {

        InfoDocumentInstance document = create(InfoDocumentInstance.class);
        document.constructor(schema, definitionName, root, onDisk);
        return document;
    }

    private InfoDocumentInstance openNewDocument(InfoSchemaHandle schema, String definitionName, JsonObject root) {

        ObjectArrayList<InfoDocumentInstance> documents = getDocuments(schema);
        InfoDocumentInstance document = createDocument(schema, definitionName, root, false);

        documents.add(document);
        documents.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getDefinitionName(), b.getDefinitionName()));
        return document;
    }

    private InfoDocumentInstance findDocument(InfoSchemaHandle schema, String definitionName) {

        ObjectArrayList<InfoDocumentInstance> documents = getDocuments(schema);

        for (int i = 0; i < documents.size(); i++)
            if (documents.get(i).getDefinitionName().equals(definitionName))
                return documents.get(i);

        return null;
    }

    private boolean isDefinitionNameTaken(InfoSchemaHandle schema, String definitionName) {
        return findDocument(schema, definitionName) != null || infoLibraryBranch.exists(schema, definitionName);
    }

    // Selection \\

    public void selectTab(InfoSchemaHandle schema) {

        if (schema == activeSchema)
            return;

        select(schema, null, null, null);
    }

    public void selectNode(InfoSchemaHandle schema, String nodeKey) {

        if (nodeKey.startsWith(EditorSetting.HIERARCHY_FOLDER_KEY_PREFIX)) {
            select(schema, null, null, nodeKey.substring(EditorSetting.HIERARCHY_FOLDER_KEY_PREFIX.length()));
            return;
        }

        if (nodeKey.startsWith(EditorSetting.HIERARCHY_FILE_KEY_PREFIX)) {
            String definitionName = nodeKey.substring(EditorSetting.HIERARCHY_FILE_KEY_PREFIX.length());
            select(schema, findDocument(schema, definitionName), null, null);
            return;
        }

        if (!nodeKey.startsWith(EditorSetting.HIERARCHY_ENTRY_KEY_PREFIX))
            return;

        String entryKey = nodeKey.substring(EditorSetting.HIERARCHY_ENTRY_KEY_PREFIX.length());
        int separator = entryKey.indexOf(EditorSetting.HIERARCHY_KEY_SEPARATOR);
        InfoDocumentInstance document = findDocument(schema, entryKey.substring(0, separator));

        select(schema, document, entryKey.substring(separator + EditorSetting.HIERARCHY_KEY_SEPARATOR.length()), null);
    }

    private void select(
            InfoSchemaHandle schema,
            InfoDocumentInstance document,
            String entryName,
            String folderName) {

        this.activeSchema = schema;
        this.selectedDocument = document;
        this.selectedEntryName = entryName;
        this.selectedFolderName = folderName;
        this.statusMessage = null;
        notifyChanged();
        notifyListeners();
    }

    private void notifyListeners() {

        InfoEntryStruct entry = getSelectedEntry();
        ObjectArrayList<Consumer<InfoEntryStruct>> listeners = entry != null
                ? schemaName2Listeners.get(activeSchema.getSchemaName())
                : null;

        if (listeners != null)
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).accept(entry);
    }

    boolean isFolderSelected(InfoSchemaHandle schema, String folderName) {
        return schema == activeSchema && folderName.equals(selectedFolderName);
    }

    boolean isFileSelected(InfoDocumentInstance document) {
        return document == selectedDocument && (!document.getSchema().isArrayLayout() || selectedEntryName == null);
    }

    boolean isEntrySelected(InfoDocumentInstance document, String entryName) {
        return document == selectedDocument && entryName.equals(selectedEntryName);
    }

    private InfoEntryStruct getSelectedEntry() {

        JsonObject entryJson = getSelectedEntryJson();

        if (entryJson == null)
            return null;

        String entryName = activeSchema.isArrayLayout() ? selectedEntryName : selectedDocument.getDefinitionName();

        return new InfoEntryStruct(
                activeSchema.getSchemaName(),
                selectedDocument.getDefinitionName(),
                entryName,
                entryJson);
    }

    private JsonObject getSelectedEntryJson() {

        if (selectedDocument == null)
            return null;

        if (!activeSchema.isArrayLayout())
            return selectedDocument.getRoot();

        return selectedEntryName != null ? selectedDocument.findEntry(selectedEntryName) : null;
    }

    // Create \\

    public boolean hasActiveSchema() {
        return activeSchema != null;
    }

    public boolean isNewEntryNameValid(String entryName) {

        if (activeSchema == null || !FileUtility.isValidFileName(entryName, EditorSetting.NAME_INPUT_MAX_LENGTH))
            return false;

        if (!activeSchema.isArrayLayout())
            return !isDefinitionNameTaken(activeSchema, resolveFolderPrefix() + entryName);

        InfoDocumentInstance document = findDocument(activeSchema, resolveTargetDefinitionName());
        return document == null || document.findEntryIndex(entryName) == EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean isNewFileNameValid(String fileName) {
        return activeSchema != null
                && FileUtility.isValidFileName(fileName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !isDefinitionNameTaken(activeSchema, resolveFolderPrefix() + fileName);
    }

    public void createEntry(String entryName) {

        if (!isNewEntryNameValid(entryName))
            return;

        if (activeSchema.isArrayLayout())
            createEntry(activeSchema.getSchemaName(), resolveTargetDefinitionName(), entryName, null);
        else
            createFile(entryName);
    }

    public void createEntry(
            String schemaName,
            String definitionName,
            String entryName,
            Consumer<JsonObject> customizer) {

        InfoSchemaHandle schema = getSchema(schemaName);

        if (!schema.isArrayLayout())
            throwException("Info schema '" + schemaName + "' keeps one entry per file; create a file instead.");

        InfoDocumentInstance document = findDocument(schema, definitionName);

        if (document == null)
            document = openNewDocument(schema, definitionName, new JsonObject());

        if (document.findEntryIndex(entryName) != EngineSetting.INDEX_NOT_FOUND)
            throwException("Info file '" + definitionName + "' already holds an entry named '" + entryName + "'.");

        JsonObject entryJson = infoEditBranch.createDefault(schema.getRootField()).getAsJsonObject();
        entryJson.addProperty(schema.getNameField(), entryName);

        if (customizer != null)
            customizer.accept(entryJson);

        document.requireEntryArray().add(entryJson);
        document.markEdited();

        select(schema, document, entryName, null);
        setStatusMessage(EditorSetting.INFO_MESSAGE_CREATED + entryName);
    }

    public void createFile(String fileName) {

        if (!isNewFileNameValid(fileName))
            return;

        String definitionName = resolveFolderPrefix() + fileName;
        JsonObject root = activeSchema.isArrayLayout()
                ? new JsonObject()
                : infoEditBranch.createDefault(activeSchema.getRootField()).getAsJsonObject();
        InfoDocumentInstance document = openNewDocument(activeSchema, definitionName, root);

        if (activeSchema.isArrayLayout())
            document.requireEntryArray();

        select(activeSchema, document, null, null);
        setStatusMessage(EditorSetting.INFO_MESSAGE_CREATED + definitionName);
    }

    private String resolveTargetDefinitionName() {
        return selectedDocument != null ? selectedDocument.getDefinitionName() : activeSchema.getDefaultFile();
    }

    private String resolveFolderPrefix() {

        String folderName = selectedFolderName != null
                ? selectedFolderName
                : selectedDocument != null ? selectedDocument.getFolderName() : "";

        return folderName.isEmpty() ? "" : folderName + EditorSetting.INFO_FOLDER_SEPARATOR;
    }

    // Delete \\

    public String getSelectionName() {

        if (selectedDocument == null)
            return null;

        return activeSchema.isArrayLayout() && selectedEntryName != null
                ? selectedEntryName
                : selectedDocument.getFileName();
    }

    public boolean isSelectionName(String name) {
        return name.equals(getSelectionName());
    }

    public void deleteSelection() {

        if (selectedDocument == null)
            return;

        if (activeSchema.isArrayLayout() && selectedEntryName != null) {
            deleteEntry(activeSchema.getSchemaName(), selectedDocument.getDefinitionName(), selectedEntryName);
            return;
        }

        InfoDocumentInstance document = selectedDocument;

        if (document.isOnDisk())
            infoLibraryBranch.delete(document);

        getDocuments(activeSchema).remove(document);
        select(activeSchema, null, null, emptyToNull(document.getFolderName()));
        setStatusMessage(EditorSetting.INFO_MESSAGE_DELETED + document.getDefinitionName());
    }

    public void deleteEntry(String schemaName, String definitionName, String entryName) {

        InfoSchemaHandle schema = getSchema(schemaName);
        InfoDocumentInstance document = findDocument(schema, definitionName);
        int entryIndex = document != null ? document.findEntryIndex(entryName) : EngineSetting.INDEX_NOT_FOUND;

        if (entryIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        document.requireEntryArray().remove(entryIndex);
        document.markEdited();

        if (document == selectedDocument && entryName.equals(selectedEntryName))
            select(schema, document, null, null);

        setStatusMessage(EditorSetting.INFO_MESSAGE_DELETED + entryName);
    }

    // Save \\

    public void saveSelection() {

        if (selectedDocument != null)
            save(selectedDocument);
    }

    public void saveDocument(String schemaName, String definitionName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);

        if (document != null)
            save(document);
    }

    public void saveAll() {

        boolean saved = false;

        for (ObjectArrayList<InfoDocumentInstance> documents : schemaName2Documents.values())
            for (int i = 0; i < documents.size(); i++)
                if (documents.get(i).isDirty()) {
                    write(documents.get(i));
                    saved = true;
                }

        setStatusMessage(saved ? EditorSetting.INFO_MESSAGE_SAVED_ALL : EditorSetting.INFO_MESSAGE_NOTHING_TO_SAVE);
    }

    private void save(InfoDocumentInstance document) {

        write(document);
        setStatusMessage(EditorSetting.INFO_MESSAGE_SAVED + document.getDefinitionName());
    }

    private void write(InfoDocumentInstance document) {

        infoLibraryBranch.save(document);
        document.markSaved();
    }

    public void revertSelection() {

        if (selectedDocument == null)
            return;

        InfoDocumentInstance document = selectedDocument;

        if (!document.isOnDisk()) {
            getDocuments(activeSchema).remove(document);
            select(activeSchema, null, null, emptyToNull(document.getFolderName()));
            setStatusMessage(EditorSetting.INFO_MESSAGE_REVERTED + document.getDefinitionName());
            return;
        }

        JsonObject root = infoLibraryBranch.load(activeSchema, document.getDefinitionName());

        if (root == null)
            return;

        document.replaceRoot(root);

        String entryName = selectedEntryName != null
                && document.findEntryIndex(selectedEntryName) != EngineSetting.INDEX_NOT_FOUND
                        ? selectedEntryName
                        : null;

        select(activeSchema, document, entryName, null);
        setStatusMessage(EditorSetting.INFO_MESSAGE_REVERTED + document.getDefinitionName());
    }

    // Fields \\

    public InfoFieldType getValueType(String path) {

        InfoTargetStruct target = resolveSelected(path);
        return target != null && target.hasValue() ? target.resolveType() : null;
    }

    public String getValueText(String path) {

        InfoTargetStruct target = resolveSelected(path);

        return target != null && target.hasValue()
                ? infoEditBranch.formatValue(target.getValue(), target.resolveType())
                : "";
    }

    public boolean isValueValid(String path, String text) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue())
            return false;

        JsonElement value = infoEditBranch.parse(target, text);
        return value != null && isNameValueValid(path, value);
    }

    public void editValue(String path, String text) {

        if (!isValueValid(path, text))
            return;

        InfoTargetStruct target = resolveSelected(path);
        JsonElement value = infoEditBranch.parse(target, text);

        target.write(value);

        if (isNamePath(path))
            this.selectedEntryName = value.getAsString();

        markSelectionEdited();
    }

    public void toggleValue(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue() || target.resolveType() != InfoFieldType.BOOLEAN)
            return;

        infoEditBranch.toggle(target);
        markSelectionEdited();
    }

    public void cycleValue(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue() || target.resolveType() != InfoFieldType.ENUM)
            return;

        infoEditBranch.cycle(target);
        markSelectionEdited();
    }

    public void addField(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || target.hasValue() || target.getField() == null)
            return;

        infoEditBranch.addField(target);
        markSelectionEdited();
    }

    public void removeValue(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue())
            return;

        if (!infoEditBranch.isRemovable(target)) {
            setStatusMessage(target.isInArray()
                    ? EditorSetting.INFO_MESSAGE_FIXED_LENGTH
                    : EditorSetting.INFO_MESSAGE_REQUIRED);
            return;
        }

        target.remove();
        markSelectionEdited();
    }

    public boolean isMap(String path) {
        return getValueType(path) == InfoFieldType.MAP;
    }

    public void addElement(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue() || target.resolveType() != InfoFieldType.ARRAY)
            return;

        if (!infoEditBranch.isAddable(target)) {
            setStatusMessage(EditorSetting.INFO_MESSAGE_FIXED_LENGTH);
            return;
        }

        infoEditBranch.addElement(target);
        markSelectionEdited();
    }

    public boolean isMapKeyValid(String path, String key) {

        InfoTargetStruct target = resolveSelected(path);

        return target != null
                && target.hasValue()
                && target.resolveType() == InfoFieldType.MAP
                && infoEditBranch.isMapKeyAvailable(target, key);
    }

    public void addMapEntry(String path, String key) {

        if (!isMapKeyValid(path, key))
            return;

        infoEditBranch.addMapEntry(resolveSelected(path), key);
        markSelectionEdited();
    }

    private InfoTargetStruct resolveSelected(String path) {

        JsonObject entryJson = getSelectedEntryJson();
        return entryJson != null ? infoEditBranch.resolve(entryJson, activeSchema.getRootField(), path) : null;
    }

    private boolean isNamePath(String path) {
        return activeSchema.isArrayLayout() && path.equals(activeSchema.getNameField());
    }

    private boolean isNameValueValid(String path, JsonElement value) {

        if (!isNamePath(path))
            return true;

        if (!value.isJsonPrimitive())
            return false;

        String entryName = value.getAsString();

        if (entryName.equals(selectedEntryName))
            return true;

        return !entryName.isEmpty()
                && !entryName.contains(EditorSetting.HIERARCHY_KEY_SEPARATOR)
                && selectedDocument.findEntryIndex(entryName) == EngineSetting.INDEX_NOT_FOUND;
    }

    private void markSelectionEdited() {

        selectedDocument.markEdited();
        this.statusMessage = null;
        notifyChanged();
        notifyListeners();
    }

    // Expansion \\

    public void toggleExpanded(String path) {

        if (getSelectedEntryJson() == null)
            return;

        String expansionKey = toExpansionKey(path);

        if (!toggledPaths.remove(expansionKey))
            toggledPaths.add(expansionKey);

        notifyChanged();
    }

    boolean isExpanded(String path, int depth) {
        return (depth == 0) != toggledPaths.contains(toExpansionKey(path));
    }

    private String toExpansionKey(String path) {
        return activeSchema.getSchemaName() + EditorSetting.HIERARCHY_KEY_SEPARATOR
                + selectedDocument.getDefinitionName() + EditorSetting.HIERARCHY_KEY_SEPARATOR
                + selectedEntryName + EditorSetting.HIERARCHY_KEY_SEPARATOR
                + path;
    }

    // Build \\

    public void buildNodes(InfoSchemaHandle schema, ObjectArrayList<HierarchyNodeStruct> roots) {
        infoHierarchyBranch.buildNodes(schema, roots);
    }

    public void buildRows(ObjectArrayList<InfoRowStruct> rows) {

        JsonObject entryJson = getSelectedEntryJson();

        if (entryJson != null)
            infoRowBranch.buildRows(entryJson, activeSchema.getRootField(), rows);
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    public String getTitleText() {

        if (activeSchema == null)
            return "";

        StringBuilder title = new StringBuilder(activeSchema.getTabName());

        if (selectedFolderName != null)
            title.append(EditorSetting.INFO_TITLE_SEPARATOR).append(selectedFolderName);

        if (selectedDocument == null)
            return title.toString();

        title.append(EditorSetting.INFO_TITLE_SEPARATOR).append(selectedDocument.getDefinitionName());

        if (selectedDocument.isDirty())
            title.append(EditorSetting.INFO_DIRTY_MARKER);

        if (activeSchema.isArrayLayout() && selectedEntryName != null)
            title.append(EditorSetting.INFO_TITLE_SEPARATOR).append(selectedEntryName);

        return title.toString();
    }

    public String getStatusText() {

        if (statusMessage != null)
            return statusMessage;

        if (selectedFolderName != null)
            return EditorSetting.INFO_STATUS_FOLDER_SELECTED;

        if (selectedDocument == null)
            return EditorSetting.INFO_STATUS_NO_SELECTION;

        if (getSelectedEntryJson() == null)
            return EditorSetting.INFO_STATUS_FILE_SELECTED;

        return activeSchema.getDirectory() + EditorSetting.INFO_FOLDER_SEPARATOR
                + selectedDocument.getDefinitionName() + "." + EditorSetting.INFO_FILE_EXTENSION;
    }

    // Utility \\

    private String emptyToNull(String text) {
        return text.isEmpty() ? null : text;
    }

    // Accessible \\

    private InfoSchemaHandle getSchema(String schemaName) {

        InfoSchemaHandle schema = schemaName2InfoSchemaHandle.get(schemaName);

        if (schema == null)
            throwException("Unknown info schema '" + schemaName + "'.");

        return schema;
    }

    public boolean hasEntry(String schemaName, String definitionName, String entryName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);
        return document != null && document.findEntryIndex(entryName) != EngineSetting.INDEX_NOT_FOUND;
    }

    public ObjectArrayList<InfoEntryStruct> getEntries(String schemaName) {

        InfoSchemaHandle schema = getSchema(schemaName);
        ObjectArrayList<InfoDocumentInstance> documents = getDocuments(schema);
        ObjectArrayList<InfoEntryStruct> entries = new ObjectArrayList<>();

        for (int i = 0; i < documents.size(); i++)
            addEntries(schema, documents.get(i), entries);

        return entries;
    }

    private void addEntries(
            InfoSchemaHandle schema,
            InfoDocumentInstance document,
            ObjectArrayList<InfoEntryStruct> entries) {

        if (!schema.isArrayLayout()) {
            entries.add(new InfoEntryStruct(
                    schema.getSchemaName(), document.getDefinitionName(), document.getDefinitionName(),
                    document.getRoot()));
            return;
        }

        ObjectArrayList<String> entryNames = document.getEntryNames();

        for (int i = 0; i < entryNames.size(); i++)
            entries.add(new InfoEntryStruct(
                    schema.getSchemaName(), document.getDefinitionName(), entryNames.get(i),
                    document.findEntry(entryNames.get(i))));
    }

    public int getRevision() {
        return revision;
    }
}
