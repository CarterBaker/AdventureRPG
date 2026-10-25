package editor.bootstrap.infopipeline.infomanager;

import java.io.File;

import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.util.InfoResourceNaming;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgValueStruct;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class InfoReferenceBranch extends BranchPackage {

    /*
     * Answers what the game would find by a name. Every schema's entries are
     * indexed under the names the game registers them by — the file for one
     * entry per file, the file and entry name for an array layout, the entry
     * name alone for a schema referenced by name — counted, so a name held
     * twice shows, and rebuilt only once names have moved anywhere. Resource
     * directories are scanned once and named as the game names their files,
     * until a tool writes or removes one. Unique values are counted per
     * schema and recounted once one of them moves.
     */

    // Internal
    private InfoManager infoManager;

    // Names
    private Object2ObjectOpenHashMap<String, Object2IntOpenHashMap<String>> schemaName2NameCounts;
    private Object2IntOpenHashMap<String> schemaName2NameRevision;

    // Values
    private Object2ObjectOpenHashMap<String, Object2IntOpenHashMap<String>> uniqueKey2ValueCounts;
    private Object2IntOpenHashMap<String> uniqueKey2UniqueRevision;

    // Resources
    private Object2ObjectOpenHashMap<String, ObjectOpenHashSet<String>> resourceKey2ResourceNames;

    // Base \\

    @Override
    protected void create() {

        // Names
        this.schemaName2NameCounts = new Object2ObjectOpenHashMap<>();
        this.schemaName2NameRevision = new Object2IntOpenHashMap<>();
        this.schemaName2NameRevision.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        // Values
        this.uniqueKey2ValueCounts = new Object2ObjectOpenHashMap<>();
        this.uniqueKey2UniqueRevision = new Object2IntOpenHashMap<>();
        this.uniqueKey2UniqueRevision.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        // Resources
        this.resourceKey2ResourceNames = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Names \\

    String toReferenceName(InfoSchemaHandle schema, String definitionName, String entryName) {

        if (!schema.isArrayLayout())
            return definitionName;

        return schema.isReferencedByName()
                ? entryName
                : definitionName + EditorSetting.INFO_REFERENCE_SEPARATOR + entryName;
    }

    // Every name a document holds as it stands in the editor
    void addLiveNames(InfoDocumentInstance document, ObjectOpenHashSet<String> names) {

        InfoSchemaHandle schema = document.getSchema();

        if (!schema.isArrayLayout()) {
            names.add(document.getDefinitionName());
            return;
        }

        ObjectArrayList<String> entryNames = document.getEntryNames();

        for (int i = 0; i < entryNames.size(); i++)
            names.add(toReferenceName(schema, document.getDefinitionName(), entryNames.get(i)));
    }

    // Every name a document holds in its file on disk, none when it has no file yet
    void addSavedNames(InfoDocumentInstance document, ObjectOpenHashSet<String> names) {

        if (!document.isOnDisk())
            return;

        InfoSchemaHandle schema = document.getSchema();

        if (!schema.isArrayLayout()) {
            names.add(document.getDefinitionName());
            return;
        }

        for (String entryName : document.getSavedEntryNames())
            names.add(toReferenceName(schema, document.getDefinitionName(), entryName));
    }

    String getTabName(String schemaName) {
        return infoManager.getSchema(schemaName).getTabName();
    }

    boolean hasName(String schemaName, String name) {
        return countName(schemaName, name) > 0;
    }

    int countName(String schemaName, String name) {
        return resolveNameCounts(schemaName).getInt(name);
    }

    private Object2IntOpenHashMap<String> resolveNameCounts(String schemaName) {

        Object2IntOpenHashMap<String> nameCounts = schemaName2NameCounts.get(schemaName);

        if (nameCounts != null && schemaName2NameRevision.getInt(schemaName) == infoManager.getNameRevision())
            return nameCounts;

        nameCounts = new Object2IntOpenHashMap<>();
        ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(infoManager.getSchema(schemaName));
        ObjectOpenHashSet<String> names = new ObjectOpenHashSet<>();

        for (int i = 0; i < documents.size(); i++)
            countNames(documents.get(i), names, nameCounts);

        schemaName2NameCounts.put(schemaName, nameCounts);
        schemaName2NameRevision.put(schemaName, infoManager.getNameRevision());
        return nameCounts;
    }

    private void countNames(
            InfoDocumentInstance document,
            ObjectOpenHashSet<String> names,
            Object2IntOpenHashMap<String> nameCounts) {

        InfoSchemaHandle schema = document.getSchema();

        if (!schema.isArrayLayout()) {
            nameCounts.addTo(document.getDefinitionName(), 1);
            return;
        }

        ObjectArrayList<String> entryNames = document.getEntryNames();

        names.clear();

        for (int i = 0; i < entryNames.size(); i++) {

            String name = toReferenceName(schema, document.getDefinitionName(), entryNames.get(i));

            if (names.add(name) || schema.isReferencedByName())
                nameCounts.addTo(name, 1);
        }
    }

    // Values \\

    int countUniqueValue(InfoSchemaHandle schema, String key, ArpgValueStruct value) {
        return resolveValueCounts(schema, key).getInt(toValueKey(value));
    }

    private Object2IntOpenHashMap<String> resolveValueCounts(InfoSchemaHandle schema, String key) {

        String uniqueKey = schema.getSchemaName() + EditorSetting.HIERARCHY_KEY_SEPARATOR + key;
        Object2IntOpenHashMap<String> valueCounts = uniqueKey2ValueCounts.get(uniqueKey);
        int uniqueRevision = infoManager.getUniqueRevision(schema);

        if (valueCounts != null && uniqueKey2UniqueRevision.getInt(uniqueKey) == uniqueRevision)
            return valueCounts;

        valueCounts = new Object2IntOpenHashMap<>();
        ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(schema);

        for (int i = 0; i < documents.size(); i++)
            countValues(documents.get(i), key, valueCounts);

        uniqueKey2ValueCounts.put(uniqueKey, valueCounts);
        uniqueKey2UniqueRevision.put(uniqueKey, uniqueRevision);
        return valueCounts;
    }

    private void countValues(InfoDocumentInstance document, String key, Object2IntOpenHashMap<String> valueCounts) {

        if (!document.getSchema().isArrayLayout()) {
            countValue(document.getRoot(), key, valueCounts);
            return;
        }

        for (ArpgElementStruct entry : document.getEntryArray())
            if (entry.isObject())
                countValue(entry.getAsObject(), key, valueCounts);
    }

    private void countValue(ArpgObjectStruct entryArpg, String key, Object2IntOpenHashMap<String> valueCounts) {

        ArpgElementStruct value = entryArpg.get(key);

        if (value != null && value.isValue())
            valueCounts.addTo(toValueKey(value.getAsValue()), 1);
    }

    // Strings match ignoring case, as the copy dialog compares them
    private String toValueKey(ArpgValueStruct value) {
        return value.isString()
                ? EditorSetting.INFO_VALUE_KEY_STRING + value.getAsString().toLowerCase()
                : EditorSetting.INFO_VALUE_KEY_OTHER + value.getAsString();
    }

    // Resources \\

    boolean hasResource(String directory, InfoResourceNaming naming, String name) {
        return resolveResourceNames(directory, naming).contains(name);
    }

    void forgetResources() {
        resourceKey2ResourceNames.clear();
    }

    private ObjectOpenHashSet<String> resolveResourceNames(String directory, InfoResourceNaming naming) {

        String resourceKey = directory + EditorSetting.HIERARCHY_KEY_SEPARATOR + naming.getSchemaName();
        ObjectOpenHashSet<String> resourceNames = resourceKey2ResourceNames.get(resourceKey);

        if (resourceNames != null)
            return resourceNames;

        resourceNames = scanResources(new File(directory), naming);
        resourceKey2ResourceNames.put(resourceKey, resourceNames);
        return resourceNames;
    }

    private ObjectOpenHashSet<String> scanResources(File root, InfoResourceNaming naming) {

        ObjectOpenHashSet<String> resourceNames = new ObjectOpenHashSet<>();

        if (!root.isDirectory())
            return resourceNames;

        ObjectArrayList<File> files = naming == InfoResourceNaming.TILE
                ? FileUtility.collectFiles(root, EngineSetting.TEXTURE_FILE_EXTENSIONS)
                : FileUtility.collectFiles(root);

        for (int i = 0; i < files.size(); i++) {

            String resourceName = toResourceName(root, files.get(i), naming);

            if (resourceName != null)
                resourceNames.add(resourceName);
        }

        return resourceNames;
    }

    // The name the game reads a file under, null for a file it does not read as one
    private String toResourceName(File root, File file, InfoResourceNaming naming) {

        return switch (naming) {
            case STEM -> FileUtility.getPathWithFileNameWithoutExtension(root, file);
            case FILE -> FileUtility.getPathWithFileNameWithExtension(root, file);
            case TILE -> toTileName(FileUtility.getPathWithFileNameWithoutExtension(root, file));
        };
    }

    private String toTileName(String path) {

        int folderEnd = path.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);
        int aliasStart = path.indexOf(EditorSetting.INFO_TILE_ALIAS_SEPARATOR, folderEnd + 1);

        if (folderEnd == EngineSetting.INDEX_NOT_FOUND || aliasStart == EngineSetting.INDEX_NOT_FOUND)
            return null;

        if (aliasStart == folderEnd + 1 || path.indexOf(EditorSetting.INFO_TILE_ALIAS_SEPARATOR, aliasStart + 1)
                != EngineSetting.INDEX_NOT_FOUND)
            return null;

        return path.substring(0, aliasStart);
    }
}
