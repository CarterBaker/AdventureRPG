package editor.bootstrap.infopipeline.infomanager;

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchymanager.HierarchyManager;
import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infoissue.InfoIssueStruct;
import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.infotab.InfoTabInstance;
import editor.bootstrap.infopipeline.infotarget.InfoTargetStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import editor.bootstrap.infopipeline.util.InfoIssueSeverity;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class InfoManager extends ManagerPackage {

    /*
     * Owns the editor's ARPG content: every schema, the files opened under it,
     * and the single selection shared by the hierarchy and info panels. Files
     * load on first view and keep edits until saved or reverted; disk access,
     * edits, rows, hierarchy nodes, validation, names and integrity each live
     * in their own branch. Every change to a file's content, from a panel or
     * a tool, goes through commit(), and every write to disk through
     * writeDocuments(), so nothing reaches disk that the game could not load:
     * a file with a problem, one naming an entry not yet saved, or one taking
     * away a name a saved file still uses is refused with the reason, and
     * nothing anything names can be deleted. A file's problems are found once
     * per change that can reach them and shown in red wherever it appears;
     * keys the game never reads are shown muted, kept, and never stop a save.
     * Change listeners hear every entry whose content moves — edited, created
     * or reverted, saved or not — and delete listeners every entry removed, so
     * tools can preview edits live. A copy pins its source when it begins and
     * is only built once a new name and every unique value check out, so a
     * stale or clashing copy aborts without touching any document.
     */

    // Internal
    private HierarchyManager hierarchyManager;
    private InfoLibraryBranch infoLibraryBranch;
    private InfoEditBranch infoEditBranch;
    private InfoRowBranch infoRowBranch;
    private InfoHierarchyBranch infoHierarchyBranch;
    private InfoReferenceBranch infoReferenceBranch;
    private InfoValidationBranch infoValidationBranch;
    private InfoIntegrityBranch infoIntegrityBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, InfoSchemaHandle> schemaName2InfoSchemaHandle;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<InfoDocumentInstance>> schemaName2Documents;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<InfoEntryStruct>>> schemaName2Listeners;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<InfoEntryStruct>>> schemaName2ChangeListeners;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<InfoEntryStruct>>> schemaName2DeleteListeners;

    // Dependencies
    private Object2ObjectOpenHashMap<String, ObjectArrayList<InfoSchemaHandle>> schemaName2ReferrerSchemas;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<InfoSchemaHandle>> directory2ReferrerSchemas;

    // Selection
    private InfoSchemaHandle activeSchema;
    private InfoDocumentInstance selectedDocument;
    private String selectedEntryName;
    private String selectedFolderName;

    // Copy
    private InfoSchemaHandle copySchema;
    private InfoDocumentInstance copyDocument;
    private String copyEntryName;

    // Expansion
    private ObjectOpenHashSet<String> toggledPaths;

    // Revisions
    private Object2IntOpenHashMap<String> schemaName2UniqueRevision;
    private int nameRevision;
    private int dependencyRevision;

    // Status
    private String statusMessage;
    private boolean statusRefused;
    private String refusal;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.infoLibraryBranch = create(InfoLibraryBranch.class);
        this.infoEditBranch = create(InfoEditBranch.class);
        this.infoRowBranch = create(InfoRowBranch.class);
        this.infoHierarchyBranch = create(InfoHierarchyBranch.class);
        this.infoReferenceBranch = create(InfoReferenceBranch.class);
        this.infoValidationBranch = create(InfoValidationBranch.class);
        this.infoIntegrityBranch = create(InfoIntegrityBranch.class);

        // Palette
        this.schemaName2InfoSchemaHandle = new Object2ObjectOpenHashMap<>();
        this.schemaName2Documents = new Object2ObjectOpenHashMap<>();
        this.schemaName2Listeners = new Object2ObjectOpenHashMap<>();
        this.schemaName2ChangeListeners = new Object2ObjectOpenHashMap<>();
        this.schemaName2DeleteListeners = new Object2ObjectOpenHashMap<>();

        // Dependencies
        this.schemaName2ReferrerSchemas = new Object2ObjectOpenHashMap<>();
        this.directory2ReferrerSchemas = new Object2ObjectOpenHashMap<>();

        // Expansion
        this.toggledPaths = new ObjectOpenHashSet<>();

        // Revisions
        this.schemaName2UniqueRevision = new Object2IntOpenHashMap<>();

        // Status
        this.refusal = EditorSetting.INFO_REFUSAL_NONE;

        create(InfoSchemaLoader.class);
    }

    @Override
    protected void get() {
        this.hierarchyManager = get(HierarchyManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
        registerDependencies();
        registerTabs();
    }

    // Every schema a field names verified, and each schema and directory given the schemas whose entries name it
    private void registerDependencies() {

        for (InfoSchemaHandle schema : schemaName2InfoSchemaHandle.values())
            schemaName2ReferrerSchemas.put(schema.getSchemaName(), new ObjectArrayList<>());

        for (InfoSchemaHandle schema : schemaName2InfoSchemaHandle.values()) {

            ObjectArrayList<String> referencedSchemaNames = schema.getReferencedSchemaNames();
            ObjectArrayList<String> referencedDirectories = schema.getReferencedDirectories();

            for (int i = 0; i < referencedSchemaNames.size(); i++) {

                ObjectArrayList<InfoSchemaHandle> referrers = schemaName2ReferrerSchemas.get(
                        referencedSchemaNames.get(i));

                if (referrers == null)
                    throwException("Info schema '" + schema.getSchemaName() + "' references unknown schema '"
                            + referencedSchemaNames.get(i) + "'.");

                referrers.add(schema);
            }

            for (int i = 0; i < referencedDirectories.size(); i++)
                directory2ReferrerSchemas
                        .computeIfAbsent(referencedDirectories.get(i), directory -> new ObjectArrayList<>())
                        .add(schema);
        }
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

    public void addChangeListener(String schemaName, Consumer<InfoEntryStruct> listener) {
        schemaName2ChangeListeners.computeIfAbsent(schemaName, name -> new ObjectArrayList<>()).add(listener);
    }

    public void addDeleteListener(String schemaName, Consumer<InfoEntryStruct> listener) {
        schemaName2DeleteListeners.computeIfAbsent(schemaName, name -> new ObjectArrayList<>()).add(listener);
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

            ArpgObjectStruct root = infoLibraryBranch.load(schema, definitionNames.get(i));

            if (root != null)
                documents.add(createDocument(schema, definitionNames.get(i), root, true));
        }

        return documents;
    }

    private InfoDocumentInstance createDocument(
            InfoSchemaHandle schema,
            String definitionName,
            ArpgObjectStruct root,
            boolean onDisk) {

        InfoDocumentInstance document = create(InfoDocumentInstance.class);
        document.constructor(schema, definitionName, root, onDisk);
        return document;
    }

    private InfoDocumentInstance openNewDocument(
            InfoSchemaHandle schema,
            String definitionName,
            ArpgObjectStruct root) {

        ObjectArrayList<InfoDocumentInstance> documents = getDocuments(schema);
        InfoDocumentInstance document = createDocument(schema, definitionName, root, false);

        documents.add(document);
        documents.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getDefinitionName(), b.getDefinitionName()));
        notifyDocumentsChanged(schema);
        return document;
    }

    private void closeDocument(InfoDocumentInstance document) {

        getDocuments(document.getSchema()).remove(document);
        notifyDocumentsChanged(document.getSchema());
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

    // Commit \\

    // The one path every change to a file's content goes through, from a panel or a tool: it marks the file and moves
    // every revision the change can reach, so its problems, and those of every file naming it, are found again
    private void commit(InfoDocumentInstance document) {

        document.markEdited();
        refreshDependents(document);

        this.statusMessage = null;
        this.statusRefused = false;
        notifyChanged();
    }

    // A file's tree replaced whole by a tool's write, or put back when the write was refused
    private void replaceContent(InfoDocumentInstance document, ArpgObjectStruct root, boolean dirty) {

        document.restoreRoot(root, dirty);
        notifyContentReplaced(document);
    }

    // Moves every revision a commit moves, for a tree replaced whole rather than edited
    private void notifyContentReplaced(InfoDocumentInstance document) {

        refreshDependents(document);
        notifyChanged();
    }

    // Names moved anywhere make every file look again for what it names; unique values moved, every file of the schema
    private void refreshDependents(InfoDocumentInstance document) {

        if (document.refreshEntryNames())
            notifyNamesChanged();

        if (document.refreshUniqueSignature())
            schemaName2UniqueRevision.addTo(document.getSchema().getSchemaName(), 1);
    }

    // A file opened or closed, so its schema's names and unique values change with it
    private void notifyDocumentsChanged(InfoSchemaHandle schema) {

        schemaName2UniqueRevision.addTo(schema.getSchemaName(), 1);
        notifyNamesChanged();
    }

    private void notifyNamesChanged() {

        nameRevision++;
        dependencyRevision++;
    }

    // A tool wrote or removed a resource file, so every name of one is looked for again
    public void notifyResourcesChanged() {

        infoReferenceBranch.forgetResources();
        dependencyRevision++;
        notifyChanged();
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

    // Selects an entry for a tool, turning every hierarchy panel to its tab
    public void selectEntry(String schemaName, String definitionName, String entryName) {

        InfoSchemaHandle schema = getSchema(schemaName);
        InfoDocumentInstance document = findDocument(schema, definitionName);
        String selectedName = schema.isArrayLayout() ? entryName : null;

        if (document == null)
            return;

        if (schema != activeSchema || document != selectedDocument || !Objects.equals(selectedName, selectedEntryName))
            select(schema, document, selectedName, null);

        hierarchyManager.showTab(schema.getTabName());
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
        this.statusRefused = false;
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

        ArpgObjectStruct entryArpg = getSelectedEntryArpg();

        if (entryArpg == null)
            return null;

        return new InfoEntryStruct(
                activeSchema.getSchemaName(),
                selectedDocument.getDefinitionName(),
                getSelectedIssueEntryName(),
                entryArpg);
    }

    private ArpgObjectStruct getSelectedEntryArpg() {

        if (selectedDocument == null)
            return null;

        if (!activeSchema.isArrayLayout())
            return selectedDocument.getRoot();

        return selectedEntryName != null ? selectedDocument.findEntry(selectedEntryName) : null;
    }

    // The entry name the selected entry's issues are filed under: its file's name when the file is the entry
    private String getSelectedIssueEntryName() {
        return activeSchema.isArrayLayout() ? selectedEntryName : selectedDocument.getDefinitionName();
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
            Consumer<ArpgObjectStruct> customizer) {

        InfoSchemaHandle schema = getSchema(schemaName);

        if (!schema.isArrayLayout())
            throwException("Info schema '" + schemaName + "' keeps one entry per file; create a file instead.");

        InfoDocumentInstance document = findDocument(schema, definitionName);

        if (document == null)
            document = openNewDocument(schema, definitionName, new ArpgObjectStruct());

        if (document.findEntryIndex(entryName) != EngineSetting.INDEX_NOT_FOUND)
            throwException("Info file '" + definitionName + "' already holds an entry named '" + entryName + "'.");

        ArpgObjectStruct entryArpg = infoEditBranch.createDefault(schema.getRootField()).getAsObject();
        entryArpg.addProperty(schema.getNameField(), entryName);

        if (customizer != null)
            customizer.accept(entryArpg);

        document.requireEntryArray().add(entryArpg);
        commit(document);

        select(schema, document, entryName, null);
        notifyEntry(schemaName2ChangeListeners, getSelectedEntry());
        setStatusMessage(EditorSetting.INFO_MESSAGE_CREATED + entryName);
    }

    public void createFile(String fileName) {

        if (!isNewFileNameValid(fileName))
            return;

        String definitionName = resolveFolderPrefix() + fileName;
        ArpgObjectStruct root = activeSchema.isArrayLayout()
                ? new ArpgObjectStruct()
                : infoEditBranch.createDefault(activeSchema.getRootField()).getAsObject();

        if (activeSchema.isArrayLayout())
            root.add(activeSchema.getEntriesKey(), new ArpgArrayStruct());

        InfoDocumentInstance document = openNewDocument(activeSchema, definitionName, root);

        select(activeSchema, document, null, null);
        notifyEntries(schemaName2ChangeListeners, activeSchema, document);
        setStatusMessage(EditorSetting.INFO_MESSAGE_CREATED + definitionName);
    }

    // Creates a one-entry file for a tool and selects it
    public void createDocument(String schemaName, String definitionName, Consumer<ArpgObjectStruct> customizer) {

        InfoSchemaHandle schema = getSchema(schemaName);

        if (schema.isArrayLayout())
            throwException("Info schema '" + schemaName + "' keeps its entries in an array; create an entry instead.");

        if (isDefinitionNameTaken(schema, definitionName))
            throwException("Info file '" + definitionName + "' already exists under schema '" + schemaName + "'.");

        ArpgObjectStruct root = infoEditBranch.createDefault(schema.getRootField()).getAsObject();

        if (customizer != null)
            customizer.accept(root);

        InfoDocumentInstance document = openNewDocument(schema, definitionName, root);

        select(schema, document, null, null);
        notifyEntries(schemaName2ChangeListeners, schema, document);
        setStatusMessage(EditorSetting.INFO_MESSAGE_CREATED + definitionName);
    }

    private String resolveTargetDefinitionName() {
        return selectedDocument != null ? selectedDocument.getDefinitionName() : activeSchema.getDefaultFile();
    }

    private String resolveFolderPrefix() {

        String folderName = selectedFolderName != null
                ? selectedFolderName
                : selectedDocument != null ? selectedDocument.getFolderName() : "";

        return toFolderPrefix(folderName);
    }

    private String toFolderPrefix(String folderName) {
        return folderName.isEmpty() ? "" : folderName + EditorSetting.INFO_FOLDER_SEPARATOR;
    }

    // Copy \\

    public boolean beginCopy() {

        if (getSelectedEntryArpg() == null)
            return false;

        this.copySchema = activeSchema;
        this.copyDocument = selectedDocument;
        this.copyEntryName = activeSchema.isArrayLayout() ? selectedEntryName : null;
        return true;
    }

    public String getCopySourceName() {
        return copyEntryName != null ? copyEntryName : copyDocument.getFileName();
    }

    public ObjectArrayList<String> getCopyUniqueKeys() {

        ArpgObjectStruct sourceArpg = getCopySourceArpg();
        ObjectArrayList<String> uniqueKeys = copySchema.getUniqueKeys();
        ObjectArrayList<String> copyKeys = new ObjectArrayList<>(uniqueKeys.size());

        for (int i = 0; i < uniqueKeys.size(); i++)
            if (sourceArpg != null && sourceArpg.has(uniqueKeys.get(i)))
                copyKeys.add(uniqueKeys.get(i));

        return copyKeys;
    }

    public String getCopyValueText(String key) {

        InfoTargetStruct target = resolveCopySource(key);

        return target != null && target.hasValue()
                ? infoEditBranch.formatValue(target.getValue(), target.resolveType())
                : "";
    }

    public boolean isCopyNameValid(String copyName) {

        if (!isCopySourceValid() || !FileUtility.isValidFileName(copyName, EditorSetting.NAME_INPUT_MAX_LENGTH))
            return false;

        return copySchema.isArrayLayout()
                ? copyDocument.findEntryIndex(copyName) == EngineSetting.INDEX_NOT_FOUND
                : !isDefinitionNameTaken(copySchema, toFolderPrefix(copyDocument.getFolderName()) + copyName);
    }

    public boolean isCopyValueValid(String key, String text) {

        InfoTargetStruct target = resolveCopySource(key);

        if (target == null || !target.hasValue())
            return false;

        ArpgElementStruct value = infoEditBranch.parse(target, text);
        return value != null && value.isValue() && !isValueTaken(key, value.getAsValue());
    }

    public void copy(String copyName, ObjectArrayList<String> copyTexts) {

        ObjectArrayList<String> copyKeys = isCopySourceValid() ? getCopyUniqueKeys() : null;

        if (!isCopyReady(copyName, copyKeys, copyTexts)) {
            endCopy();
            setStatusMessage(EditorSetting.INFO_MESSAGE_COPY_ABORTED);
            return;
        }

        InfoSchemaHandle schema = copySchema;
        InfoDocumentInstance sourceDocument = copyDocument;
        ArpgObjectStruct copyArpg = getCopySourceArpg().deepCopy();

        endCopy();

        for (int i = 0; i < copyKeys.size(); i++) {

            InfoTargetStruct target = infoEditBranch.resolve(copyArpg, schema.getRootField(), copyKeys.get(i));
            target.write(infoEditBranch.parse(target, copyTexts.get(i)));
        }

        if (schema.isArrayLayout()) {

            copyArpg.addProperty(schema.getNameField(), copyName);
            sourceDocument.requireEntryArray().add(copyArpg);
            commit(sourceDocument);

            select(schema, sourceDocument, copyName, null);
            notifyEntry(schemaName2ChangeListeners, getSelectedEntry());
        } else {

            InfoDocumentInstance document = openNewDocument(
                    schema, toFolderPrefix(sourceDocument.getFolderName()) + copyName, copyArpg);

            select(schema, document, null, null);
            notifyEntries(schemaName2ChangeListeners, schema, document);
        }

        setStatusMessage(EditorSetting.INFO_MESSAGE_COPIED + copyName);
    }

    private boolean isCopyReady(String copyName, ObjectArrayList<String> copyKeys, ObjectArrayList<String> copyTexts) {

        if (copyKeys == null || copyKeys.size() != copyTexts.size() || !isCopyNameValid(copyName))
            return false;

        for (int i = 0; i < copyKeys.size(); i++)
            if (!isCopyValueValid(copyKeys.get(i), copyTexts.get(i)))
                return false;

        return true;
    }

    private boolean isCopySourceValid() {
        return copyDocument != null && getDocuments(copySchema).contains(copyDocument) && getCopySourceArpg() != null;
    }

    private ArpgObjectStruct getCopySourceArpg() {

        if (copyDocument == null)
            return null;

        return copyEntryName != null ? copyDocument.findEntry(copyEntryName) : copyDocument.getRoot();
    }

    private InfoTargetStruct resolveCopySource(String key) {
        return isCopySourceValid() ? infoEditBranch.resolve(getCopySourceArpg(), copySchema.getRootField(), key) : null;
    }

    private boolean isValueTaken(String key, ArpgValueStruct value) {
        return infoReferenceBranch.countUniqueValue(copySchema, key, value) > 0;
    }

    private void endCopy() {

        this.copySchema = null;
        this.copyDocument = null;
        this.copyEntryName = null;
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

        deleteFile(selectedDocument);
    }

    // True once the file is gone; refused, with the reason kept, while anything still names it
    public boolean deleteDocument(String schemaName, String definitionName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);
        return document != null && deleteFile(document);
    }

    private boolean deleteFile(InfoDocumentInstance document) {

        String deleteRefusal = infoIntegrityBranch.findDeleteRefusal(document, null);

        if (!deleteRefusal.isEmpty())
            return refuse(deleteRefusal);

        InfoSchemaHandle schema = document.getSchema();

        if (document.isOnDisk())
            infoLibraryBranch.delete(document);

        notifyEntries(schemaName2DeleteListeners, schema, document);
        closeDocument(document);

        if (document == selectedDocument)
            select(schema, null, null, emptyToNull(document.getFolderName()));

        setStatusMessage(EditorSetting.INFO_MESSAGE_DELETED + document.getDefinitionName());
        return true;
    }

    // True once the entry is gone from its file; refused, with the reason kept, while anything still names it
    public boolean deleteEntry(String schemaName, String definitionName, String entryName) {

        InfoSchemaHandle schema = getSchema(schemaName);
        InfoDocumentInstance document = findDocument(schema, definitionName);
        int entryIndex = document != null ? document.findEntryIndex(entryName) : EngineSetting.INDEX_NOT_FOUND;

        if (entryIndex == EngineSetting.INDEX_NOT_FOUND)
            return false;

        String deleteRefusal = infoIntegrityBranch.findDeleteRefusal(document, entryName);

        if (!deleteRefusal.isEmpty())
            return refuse(deleteRefusal);

        notifyEntry(schemaName2DeleteListeners, new InfoEntryStruct(
                schemaName, definitionName, entryName, document.findEntry(entryName)));
        document.requireEntryArray().remove(entryIndex);
        commit(document);

        if (document == selectedDocument && entryName.equals(selectedEntryName))
            select(schema, document, null, null);

        setStatusMessage(EditorSetting.INFO_MESSAGE_DELETED + entryName);
        return true;
    }

    // Save \\

    public void saveSelection() {

        if (selectedDocument != null)
            save(selectedDocument);
    }

    // True once the file is on disk; refused, with the reason kept, when the game could not load it
    public boolean saveDocument(String schemaName, String definitionName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);
        return document != null && save(document);
    }

    public void saveAll() {

        ObjectArrayList<InfoDocumentInstance> dirtyDocuments = new ObjectArrayList<>();

        for (ObjectArrayList<InfoDocumentInstance> documents : schemaName2Documents.values())
            for (int i = 0; i < documents.size(); i++)
                if (documents.get(i).isDirty())
                    dirtyDocuments.add(documents.get(i));

        if (dirtyDocuments.isEmpty()) {
            setStatusMessage(EditorSetting.INFO_MESSAGE_NOTHING_TO_SAVE);
            return;
        }

        int written = writeDocuments(dirtyDocuments);

        if (written == dirtyDocuments.size()) {
            setStatusMessage(EditorSetting.INFO_MESSAGE_SAVED_ALL);
            return;
        }

        refuse(EditorSetting.INFO_MESSAGE_SAVED + written + EditorSetting.INFO_MESSAGE_SAVED_OF
                + dirtyDocuments.size() + EditorSetting.INFO_MESSAGE_SAVED_SUFFIX + refusal);
    }

    private boolean save(InfoDocumentInstance document) {

        ObjectArrayList<InfoDocumentInstance> documents = new ObjectArrayList<>(1);
        documents.add(document);

        if (writeDocuments(documents) == 0)
            return refuse(refusal);

        setStatusMessage(EditorSetting.INFO_MESSAGE_SAVED + document.getDefinitionName());
        return true;
    }

    // The one path every content file reaches disk through. Resources are looked for afresh, then every document the
    // game could load once the rest are written is written; a document refused takes those depending on it along,
    // and the first refusal is kept. Returns how many were written.
    private int writeDocuments(ObjectArrayList<InfoDocumentInstance> candidates) {

        ObjectArrayList<InfoDocumentInstance> writable = new ObjectArrayList<>(candidates);
        ObjectArrayList<InfoDocumentInstance> refused = new ObjectArrayList<>();

        infoReferenceBranch.forgetResources();
        this.refusal = EditorSetting.INFO_REFUSAL_NONE;

        do {

            refused.clear();
            infoIntegrityBranch.beginSave(writable);

            for (int i = 0; i < writable.size(); i++) {

                String documentRefusal = infoIntegrityBranch.findSaveRefusal(writable.get(i));

                if (documentRefusal.isEmpty())
                    continue;

                refused.add(writable.get(i));

                if (refusal.isEmpty())
                    this.refusal = documentRefusal;
            }

            infoIntegrityBranch.endSave();
            writable.removeAll(refused);
        } while (!refused.isEmpty() && !writable.isEmpty());

        for (int i = 0; i < writable.size(); i++) {
            infoLibraryBranch.save(writable.get(i));
            writable.get(i).markSaved();
        }

        notifyChanged();
        return writable.size();
    }

    // Writes a tool's whole content for one file through the same checks as every save, leaving the editor's copy as
    // it was when the game could not load the content
    public boolean writeDocument(String schemaName, String definitionName, ArpgObjectStruct root) {

        InfoSchemaHandle schema = getSchema(schemaName);

        if (schema.isArrayLayout())
            throwException("Info schema '" + schemaName + "' keeps its entries in an array; write an entry instead.");

        InfoDocumentInstance document = findDocument(schema, definitionName);
        boolean created = document == null;
        ArpgObjectStruct previousRoot = created ? null : document.getRoot();
        boolean previousDirty = !created && document.isDirty();

        if (created)
            document = openNewDocument(schema, definitionName, root);
        else
            replaceContent(document, root, true);

        if (!save(document)) {

            if (created)
                closeDocument(document);
            else
                replaceContent(document, previousRoot, previousDirty);

            return refuse(refusal);
        }

        notifyEntries(schemaName2ChangeListeners, schema, document);
        return true;
    }

    public void revertSelection() {

        if (selectedDocument != null)
            revert(selectedDocument);
    }

    public void revertDocument(String schemaName, String definitionName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);

        if (document != null)
            revert(document);
    }

    private void revert(InfoDocumentInstance document) {

        InfoSchemaHandle schema = document.getSchema();
        boolean selected = document == selectedDocument;

        if (!document.isOnDisk()) {
            notifyEntries(schemaName2DeleteListeners, schema, document);
            closeDocument(document);

            if (selected)
                select(schema, null, null, emptyToNull(document.getFolderName()));

            setStatusMessage(EditorSetting.INFO_MESSAGE_REVERTED + document.getDefinitionName());
            return;
        }

        ArpgObjectStruct root = infoLibraryBranch.load(schema, document.getDefinitionName());

        if (root == null)
            return;

        document.replaceRoot(root);
        notifyContentReplaced(document);
        notifyEntries(schemaName2ChangeListeners, schema, document);

        if (selected)
            select(schema, document, resolveRevertedEntryName(document), null);

        setStatusMessage(EditorSetting.INFO_MESSAGE_REVERTED + document.getDefinitionName());
    }

    private String resolveRevertedEntryName(InfoDocumentInstance document) {
        return selectedEntryName != null && document.findEntryIndex(selectedEntryName) != EngineSetting.INDEX_NOT_FOUND
                ? selectedEntryName
                : null;
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

        ArpgElementStruct value = infoEditBranch.parse(target, text);
        return value != null && isNameValueValid(path, value);
    }

    public void editValue(String path, String text) {

        if (!isValueValid(path, text))
            return;

        InfoTargetStruct target = resolveSelected(path);
        ArpgElementStruct value = infoEditBranch.parse(target, text);

        target.write(value);

        if (isNamePath(path))
            this.selectedEntryName = value.getAsString();

        commitSelection();
    }

    public void toggleValue(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue() || target.resolveType() != InfoFieldType.BOOLEAN)
            return;

        infoEditBranch.toggle(target);
        commitSelection();
    }

    public void cycleValue(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || !target.hasValue() || target.resolveType() != InfoFieldType.ENUM)
            return;

        infoEditBranch.cycle(target);
        commitSelection();
    }

    public void addField(String path) {

        InfoTargetStruct target = resolveSelected(path);

        if (target == null || target.hasValue() || target.getField() == null)
            return;

        infoEditBranch.addField(target);
        commitSelection();
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
        commitSelection();
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
        commitSelection();
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
        commitSelection();
    }

    // Applies a tool's edit to one entry in place, marking its file changed and telling every change listener
    public void editEntry(
            String schemaName,
            String definitionName,
            String entryName,
            Consumer<ArpgObjectStruct> editor) {

        InfoSchemaHandle schema = getSchema(schemaName);
        InfoDocumentInstance document = findDocument(schema, definitionName);
        ArpgObjectStruct entryArpg = document != null ? resolveEntryArpg(schema, document, entryName) : null;

        if (entryArpg == null)
            throwException("Info file '" + definitionName + "' holds no entry named '" + entryName + "'.");

        editor.accept(entryArpg);
        commit(document);

        notifyEntry(
                schemaName2ChangeListeners,
                new InfoEntryStruct(schemaName, definitionName, entryName, entryArpg));
    }

    private ArpgObjectStruct resolveEntryArpg(
            InfoSchemaHandle schema,
            InfoDocumentInstance document,
            String entryName) {
        return schema.isArrayLayout() ? document.findEntry(entryName) : document.getRoot();
    }

    private InfoTargetStruct resolveSelected(String path) {

        ArpgObjectStruct entryArpg = getSelectedEntryArpg();
        return entryArpg != null ? infoEditBranch.resolve(entryArpg, activeSchema.getRootField(), path) : null;
    }

    private boolean isNamePath(String path) {
        return activeSchema.isArrayLayout() && path.equals(activeSchema.getNameField());
    }

    private boolean isNameValueValid(String path, ArpgElementStruct value) {

        if (!isNamePath(path))
            return true;

        if (!value.isValue())
            return false;

        String entryName = value.getAsString();

        if (entryName.equals(selectedEntryName))
            return true;

        return !entryName.isEmpty()
                && !entryName.contains(EditorSetting.HIERARCHY_KEY_SEPARATOR)
                && selectedDocument.findEntryIndex(entryName) == EngineSetting.INDEX_NOT_FOUND;
    }

    private void commitSelection() {

        commit(selectedDocument);
        notifyListeners();
        notifyEntry(schemaName2ChangeListeners, getSelectedEntry());
    }

    // Entry Listeners \\

    private void notifyEntries(
            Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<InfoEntryStruct>>> schemaName2EntryListeners,
            InfoSchemaHandle schema,
            InfoDocumentInstance document) {

        if (!schemaName2EntryListeners.containsKey(schema.getSchemaName()))
            return;

        ObjectArrayList<InfoEntryStruct> entries = new ObjectArrayList<>();
        addEntries(schema, document, entries);

        for (int i = 0; i < entries.size(); i++)
            notifyEntry(schemaName2EntryListeners, entries.get(i));
    }

    private void notifyEntry(
            Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<InfoEntryStruct>>> schemaName2EntryListeners,
            InfoEntryStruct entry) {

        ObjectArrayList<Consumer<InfoEntryStruct>> listeners = entry != null
                ? schemaName2EntryListeners.get(entry.getSchemaName())
                : null;

        if (listeners != null)
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).accept(entry);
    }

    // Issues \\

    // A document's problems, found again only once a change has reached it
    ObjectArrayList<InfoIssueStruct> getIssues(InfoDocumentInstance document) {

        if (!document.hasIssuesAt(document.getEditRevision(), toDependencyStamp(document)))
            refreshIssues(document);

        return document.getIssues();
    }

    // A document's problems found again now, whatever has moved, for a save that must judge it as it stands
    ObjectArrayList<InfoIssueStruct> refreshIssues(InfoDocumentInstance document) {

        document.setIssues(
                infoValidationBranch.validate(document), document.getEditRevision(), toDependencyStamp(document));
        return document.getIssues();
    }

    // What a document's problems hang on beyond its own edits: names and resources anywhere, and its schema's
    // unique values; both only ever count up, so their sum moves whenever either does
    private int toDependencyStamp(InfoDocumentInstance document) {
        return dependencyRevision + getUniqueRevision(document.getSchema());
    }

    // True when any file of a schema holds an error, so its tab shows it
    public boolean hasProblem(InfoSchemaHandle schema) {

        ObjectArrayList<InfoDocumentInstance> documents = getDocuments(schema);

        for (int i = 0; i < documents.size(); i++)
            if (hasErrors(documents.get(i)))
                return true;

        return false;
    }

    boolean hasErrors(InfoDocumentInstance document) {

        getIssues(document);
        return document.hasErrors();
    }

    boolean hasEntryErrors(InfoDocumentInstance document, String entryName) {

        ObjectArrayList<InfoIssueStruct> issues = getIssues(document);

        for (int i = 0; i < issues.size(); i++)
            if (issues.get(i).isError() && entryName.equals(issues.get(i).getEntryName()))
                return true;

        return false;
    }

    // Every row path of the selected entry at or above an issue of a severity, so a collapsed group shows what lies
    // inside it
    private ObjectOpenHashSet<String> collectSelectedIssuePaths(InfoIssueSeverity severity) {

        ObjectOpenHashSet<String> issuePaths = new ObjectOpenHashSet<>();
        ObjectArrayList<InfoIssueStruct> issues = getSelectedIssues();

        for (int i = 0; i < issues.size(); i++) {

            if (issues.get(i).getSeverity() != severity)
                continue;

            String path = issues.get(i).getPath();

            while (!path.isEmpty() && issuePaths.add(path)) {

                int separator = path.lastIndexOf(EditorSetting.INFO_PATH_SEPARATOR);
                path = separator != EngineSetting.INDEX_NOT_FOUND ? path.substring(0, separator) : "";
            }
        }

        return issuePaths;
    }

    // The issues of the selected entry, or of the selected file as a whole when no entry of it is selected, its
    // errors first
    private ObjectArrayList<InfoIssueStruct> getSelectedIssues() {

        ObjectArrayList<InfoIssueStruct> selectedIssues = new ObjectArrayList<>();

        if (selectedDocument == null)
            return selectedIssues;

        ObjectArrayList<InfoIssueStruct> issues = getIssues(selectedDocument);
        String entryName = getSelectedEntryArpg() != null ? getSelectedIssueEntryName() : null;

        for (int i = 0; i < issues.size(); i++)
            if (entryName == null || entryName.equals(issues.get(i).getEntryName()))
                selectedIssues.add(issues.get(i));

        selectedIssues.sort((a, b) -> Boolean.compare(b.isError(), a.isError()));
        return selectedIssues;
    }

    private int countErrors(ObjectArrayList<InfoIssueStruct> issues) {

        int errors = 0;

        for (int i = 0; i < issues.size(); i++)
            if (issues.get(i).isError())
                errors++;

        return errors;
    }

    String describeIssue(InfoIssueStruct issue) {

        String location = issue.getPath().isEmpty()
                ? issue.getEntryName() != null ? issue.getEntryName() : ""
                : toDisplayPath(issue.getPath());

        return location.isEmpty()
                ? issue.getMessage()
                : location + EditorSetting.INFO_ISSUE_SEPARATOR + issue.getMessage();
    }

    String toDisplayPath(String path) {
        return path.replace(EditorSetting.INFO_PATH_SEPARATOR, EditorSetting.INFO_ISSUE_PATH_SEPARATOR);
    }

    // True when any content, live or on disk, names a file of a resource directory, so a tool may not remove it
    public boolean isResourceNamed(String directory, String name) {
        return infoIntegrityBranch.isResourceNamed(directory, name);
    }

    // Expansion \\

    public void toggleExpanded(String path) {

        if (getSelectedEntryArpg() == null)
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

        ArpgObjectStruct entryArpg = getSelectedEntryArpg();

        if (entryArpg != null)
            infoRowBranch.buildRows(
                    entryArpg,
                    activeSchema.getRootField(),
                    collectSelectedIssuePaths(InfoIssueSeverity.ERROR),
                    collectSelectedIssuePaths(InfoIssueSeverity.IGNORED),
                    rows);
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        this.statusRefused = false;
        notifyChanged();
    }

    // Keeps why an action was refused, for the panel and for the tool that asked, and reports it was not done
    private boolean refuse(String reason) {

        this.refusal = reason;
        setStatusMessage(reason);
        this.statusRefused = true;
        return false;
    }

    // Why the last save, write or delete asked for was refused
    public String getRefusal() {
        return refusal;
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

    public File getSelectedFile() {
        return selectedDocument != null
                ? infoLibraryBranch.getFile(selectedDocument.getSchema(), selectedDocument.getDefinitionName())
                : null;
    }

    public String getStatusText() {

        if (statusMessage != null)
            return statusMessage;

        if (selectedFolderName != null)
            return EditorSetting.INFO_STATUS_FOLDER_SELECTED;

        if (selectedDocument == null)
            return EditorSetting.INFO_STATUS_NO_SELECTION;

        ObjectArrayList<InfoIssueStruct> issues = getSelectedIssues();
        int errors = countErrors(issues);

        if (errors > 0)
            return errors + (errors == 1
                    ? EditorSetting.INFO_STATUS_PROBLEM
                    : EditorSetting.INFO_STATUS_PROBLEMS) + describeIssue(issues.get(0));

        if (!issues.isEmpty())
            return issues.size() + (issues.size() == 1
                    ? EditorSetting.INFO_STATUS_IGNORED
                    : EditorSetting.INFO_STATUS_IGNORED_PLURAL) + describeIssue(issues.get(0));

        if (getSelectedEntryArpg() == null)
            return EditorSetting.INFO_STATUS_FILE_SELECTED;

        return activeSchema.getDirectory() + EditorSetting.INFO_FOLDER_SEPARATOR
                + ArpgUtility.toFileName(selectedDocument.getDefinitionName());
    }

    // True when the status line reports a refusal or an error of the selection, so the panel shows it in red
    public boolean hasStatusProblem() {
        return statusMessage != null ? statusRefused : countErrors(getSelectedIssues()) > 0;
    }

    // Utility \\

    private String emptyToNull(String text) {
        return text.isEmpty() ? null : text;
    }

    // Accessible \\

    InfoSchemaHandle getSchema(String schemaName) {

        InfoSchemaHandle schema = schemaName2InfoSchemaHandle.get(schemaName);

        if (schema == null)
            throwException("Unknown info schema '" + schemaName + "'.");

        return schema;
    }

    ObjectArrayList<InfoSchemaHandle> getReferrerSchemas(String schemaName) {
        return schemaName2ReferrerSchemas.get(schemaName);
    }

    ObjectArrayList<InfoSchemaHandle> getResourceReferrerSchemas(String directory) {

        ObjectArrayList<InfoSchemaHandle> referrers = directory2ReferrerSchemas.get(directory);
        return referrers != null ? referrers : new ObjectArrayList<>();
    }

    int getUniqueRevision(InfoSchemaHandle schema) {
        return schemaName2UniqueRevision.getInt(schema.getSchemaName());
    }

    int getNameRevision() {
        return nameRevision;
    }

    public boolean hasEntry(String schemaName, String definitionName, String entryName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);
        return document != null && document.findEntryIndex(entryName) != EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean hasDocument(String schemaName, String definitionName) {
        return isDefinitionNameTaken(getSchema(schemaName), definitionName);
    }

    public boolean isDocumentDirty(String schemaName, String definitionName) {

        InfoDocumentInstance document = findDocument(getSchema(schemaName), definitionName);
        return document != null && document.isDirty();
    }

    public InfoEntryStruct findEntry(String schemaName, String definitionName, String entryName) {

        InfoSchemaHandle schema = getSchema(schemaName);
        InfoDocumentInstance document = findDocument(schema, definitionName);
        ArpgObjectStruct entryArpg = document != null ? resolveEntryArpg(schema, document, entryName) : null;

        return entryArpg != null ? new InfoEntryStruct(schemaName, definitionName, entryName, entryArpg) : null;
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
