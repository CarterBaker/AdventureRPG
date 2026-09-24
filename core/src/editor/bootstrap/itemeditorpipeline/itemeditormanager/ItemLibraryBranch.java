package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.io.File;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemLibraryBranch extends BranchPackage {

    /*
     * Reads and writes items on disk. The catalog is every item every
     * definition file in the item directory declares — the same set the game
     * loads — kept current as items are saved and deleted. An item's mesh
     * opens as sub-voxels, converting an authored quad mesh on the way, and
     * saves back to the same mesh file with its definition entry kept intact.
     * Deleting drops the definition entry, and the mesh unless another item
     * still draws with it.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Directory
    private File definitionRoot;
    private File meshRoot;

    // Catalog
    private Object2ObjectOpenHashMap<String, ItemEntryStruct> itemName2ItemEntry;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.definitionRoot = new File(EngineSetting.ITEM_JSON_PATH);
        this.meshRoot = new File(EngineSetting.MESH_JSON_PATH);

        // Catalog
        this.itemName2ItemEntry = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    @Override
    protected void awake() {
        scanCatalog();
    }

    // Load \\

    private void scanCatalog() {

        if (!definitionRoot.isDirectory())
            return;

        List<File> definitionFiles = FileUtility.collectFiles(definitionRoot, EngineSetting.JSON_FILE_EXTENSIONS);

        for (int i = 0; i < definitionFiles.size(); i++)
            scanDefinitionFile(definitionFiles.get(i));
    }

    private void scanDefinitionFile(File definitionFile) {

        JsonObject rootJson = JsonUtility.tryLoadJsonObject(definitionFile);

        if (rootJson == null || !JsonUtility.hasArray(rootJson, "items"))
            return;

        String definitionName = FileUtility.getPathWithFileNameWithoutExtension(definitionRoot, definitionFile);
        JsonArray itemsJson = rootJson.getAsJsonArray("items");

        for (int i = 0; i < itemsJson.size(); i++) {

            if (!itemsJson.get(i).isJsonObject())
                continue;

            JsonObject itemJson = itemsJson.get(i).getAsJsonObject();

            if (!JsonUtility.hasString(itemJson, "name") || !JsonUtility.hasString(itemJson, "mesh"))
                continue;

            addEntry(new ItemEntryStruct(
                    definitionName,
                    itemJson.get("name").getAsString(),
                    itemJson.get("mesh").getAsString()));
        }
    }

    SubVoxelModelStruct loadModel(ItemEntryStruct entry, String fallbackTextureName) {

        JsonObject meshJson = loadMeshJson(entry);

        if (subVoxelManager.hasSubVoxels(meshJson)) {

            SubVoxelModelStruct model = subVoxelManager.parseModel(meshJson);

            if (model.getPartCount() == 0)
                throwException("Item '" + entry.getItemName() + "' declares no parts in mesh '"
                        + entry.getMeshName() + "'.");

            return model;
        }

        if (subVoxelManager.hasQuads(meshJson))
            return subVoxelManager.importQuadMesh(meshJson, fallbackTextureName);

        return throwException("Item '" + entry.getItemName() + "' uses mesh '" + entry.getMeshName()
                + "', which holds neither sub-voxels nor quads.");
    }

    boolean requiresConversion(ItemEntryStruct entry) {
        return !subVoxelManager.hasSubVoxels(loadMeshJson(entry));
    }

    private JsonObject loadMeshJson(ItemEntryStruct entry) {

        File meshFile = getMeshFile(entry.getMeshName());

        if (!meshFile.isFile())
            throwException("Item '" + entry.getItemName() + "' has no mesh file: " + meshFile.getAbsolutePath());

        return JsonUtility.loadJsonObject(meshFile);
    }

    // Save \\

    void save(ItemDocumentInstance document) {

        ItemEntryStruct entry = document.getEntry();

        JsonUtility.writeJsonObject(
                getMeshFile(entry.getMeshName()),
                subVoxelManager.toMeshJson(document.getModel()),
                internal.gson);
        writeDefinition(entry);
        addEntry(entry);
    }

    private void writeDefinition(ItemEntryStruct entry) {

        File definitionFile = getDefinitionFile(entry.getDefinitionName());
        JsonObject rootJson = definitionFile.isFile()
                ? JsonUtility.loadJsonObject(definitionFile)
                : new JsonObject();

        if (!JsonUtility.hasArray(rootJson, "items"))
            rootJson.add("items", new JsonArray());

        JsonArray itemsJson = rootJson.getAsJsonArray("items");
        int definitionIndex = findDefinition(itemsJson, entry.getLocalName());
        JsonObject itemJson = definitionIndex != EngineSetting.INDEX_NOT_FOUND
                ? itemsJson.get(definitionIndex).getAsJsonObject()
                : createDefinition(itemsJson, entry.getLocalName());

        itemJson.addProperty("mesh", entry.getMeshName());
        JsonUtility.writeJsonObject(definitionFile, rootJson, internal.gson);
    }

    private JsonObject createDefinition(JsonArray itemsJson, String localName) {

        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("name", localName);
        itemsJson.add(itemJson);
        return itemJson;
    }

    // Delete \\

    void delete(ItemEntryStruct entry) {

        File definitionFile = getDefinitionFile(entry.getDefinitionName());

        if (definitionFile.isFile()) {

            JsonObject rootJson = JsonUtility.loadJsonObject(definitionFile);

            if (JsonUtility.hasArray(rootJson, "items")) {

                JsonArray itemsJson = rootJson.getAsJsonArray("items");
                int definitionIndex = findDefinition(itemsJson, entry.getLocalName());

                if (definitionIndex != EngineSetting.INDEX_NOT_FOUND) {
                    itemsJson.remove(definitionIndex);
                    JsonUtility.writeJsonObject(definitionFile, rootJson, internal.gson);
                }
            }
        }

        itemName2ItemEntry.remove(entry.getItemName());

        File meshFile = getMeshFile(entry.getMeshName());

        if (!isMeshReferenced(entry.getMeshName()) && meshFile.isFile() && !meshFile.delete())
            throwException("Failed to delete mesh file: " + meshFile.getAbsolutePath());
    }

    // Utility \\

    private void addEntry(ItemEntryStruct entry) {
        itemName2ItemEntry.put(entry.getItemName(), entry);
    }

    private int findDefinition(JsonArray itemsJson, String localName) {

        for (int i = 0; i < itemsJson.size(); i++) {

            if (!itemsJson.get(i).isJsonObject())
                continue;

            JsonObject itemJson = itemsJson.get(i).getAsJsonObject();

            if (localName.equals(JsonUtility.getString(itemJson, "name", null)))
                return i;
        }

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean isMeshReferenced(String meshName) {

        for (ItemEntryStruct entry : itemName2ItemEntry.values())
            if (entry.getMeshName().equals(meshName))
                return true;

        return false;
    }

    private File getDefinitionFile(String definitionName) {
        return new File(definitionRoot, definitionName + "." + EditorSetting.ITEM_EDITOR_FILE_EXTENSION);
    }

    private File getMeshFile(String meshName) {
        return new File(meshRoot, meshName + "." + EditorSetting.ITEM_EDITOR_FILE_EXTENSION);
    }

    // Accessible \\

    boolean hasItem(String itemName) {
        return itemName2ItemEntry.containsKey(itemName);
    }

    boolean hasMesh(String meshName) {
        return getMeshFile(meshName).exists() || isMeshReferenced(meshName);
    }

    ItemEntryStruct getEntry(String itemName) {
        return itemName2ItemEntry.get(itemName);
    }

    ObjectArrayList<ItemEntryStruct> getEntries() {
        return new ObjectArrayList<>(itemName2ItemEntry.values());
    }
}
