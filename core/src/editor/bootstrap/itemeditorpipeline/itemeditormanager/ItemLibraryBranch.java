package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.io.File;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemLibraryBranch extends BranchPackage {

    /*
     * Reads and writes items on disk. An item saves as a sub-voxel mesh — the
     * file the game loads — and is registered in the editor's item definition
     * file, keeping any properties an existing entry already has.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Directory
    private File meshDirectory;
    private File definitionFile;

    // Library
    private ObjectArrayList<String> itemNames;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.meshDirectory = new File(EngineSetting.MESH_JSON_PATH, EditorSetting.ITEM_EDITOR_MESH_DIRECTORY);
        this.definitionFile = new File(EngineSetting.ITEM_JSON_PATH,
                EditorSetting.ITEM_EDITOR_DEFINITION_FILE + "." + EditorSetting.ITEM_EDITOR_FILE_EXTENSION);
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    @Override
    protected void awake() {
        this.itemNames = scanItemNames();
    }

    // Load \\

    private ObjectArrayList<String> scanItemNames() {

        ObjectArrayList<String> scannedNames = new ObjectArrayList<>();

        if (!meshDirectory.isDirectory())
            return scannedNames;

        List<File> meshFiles = FileUtility.collectFilesShallow(meshDirectory, EngineSetting.JSON_FILE_EXTENSIONS);

        for (int i = 0; i < meshFiles.size(); i++) {

            JsonObject meshJson = JsonUtility.tryLoadJsonObject(meshFiles.get(i));

            if (meshJson != null && subVoxelManager.hasSubVoxels(meshJson))
                scannedNames.add(FileUtility.getFileName(meshFiles.get(i)));
        }

        return scannedNames;
    }

    SubVoxelModelStruct loadModel(String itemName) {

        File meshFile = getMeshFile(itemName);

        if (!meshFile.isFile())
            throwException("Item '" + itemName + "' has no mesh file: " + meshFile.getAbsolutePath());

        SubVoxelModelStruct model = subVoxelManager.parseModel(JsonUtility.loadJsonObject(meshFile));

        if (model.getPartCount() == 0)
            throwException("Item '" + itemName + "' declares no parts: " + meshFile.getAbsolutePath());

        return model;
    }

    // Save \\

    void save(ItemDocumentInstance document) {

        String itemName = document.getItemName();

        JsonUtility.writeJsonObject(
                getMeshFile(itemName),
                subVoxelManager.toMeshJson(document.getModel()),
                internal.gson);
        registerDefinition(itemName);

        if (!itemNames.contains(itemName))
            itemNames.add(itemName);
    }

    private void registerDefinition(String itemName) {

        JsonObject rootJson = definitionFile.isFile()
                ? JsonUtility.loadJsonObject(definitionFile)
                : new JsonObject();

        if (!JsonUtility.hasArray(rootJson, "items"))
            rootJson.add("items", new JsonArray());

        JsonArray itemsJson = rootJson.getAsJsonArray("items");
        JsonObject itemJson = findDefinition(itemsJson, itemName);

        if (itemJson == null) {
            itemJson = new JsonObject();
            itemJson.addProperty("name", itemName);
            itemsJson.add(itemJson);
        }

        itemJson.addProperty("mesh", getMeshName(itemName));
        JsonUtility.writeJsonObject(definitionFile, rootJson, internal.gson);
    }

    private JsonObject findDefinition(JsonArray itemsJson, String itemName) {

        for (int i = 0; i < itemsJson.size(); i++) {

            JsonObject itemJson = itemsJson.get(i).getAsJsonObject();

            if (itemName.equals(JsonUtility.getString(itemJson, "name", null)))
                return itemJson;
        }

        return null;
    }

    // Utility \\

    private File getMeshFile(String itemName) {
        return new File(meshDirectory, itemName + "." + EditorSetting.ITEM_EDITOR_FILE_EXTENSION);
    }

    private String getMeshName(String itemName) {
        return EditorSetting.ITEM_EDITOR_MESH_DIRECTORY + "/" + itemName;
    }

    // Accessible \\

    boolean hasMesh(String itemName) {
        return getMeshFile(itemName).exists();
    }

    ObjectArrayList<String> getItemNames() {
        return itemNames;
    }
}
