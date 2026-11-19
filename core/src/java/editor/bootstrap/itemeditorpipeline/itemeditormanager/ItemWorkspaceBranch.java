package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.io.File;

import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

class ItemWorkspaceBranch extends BranchPackage {

    /*
     * The items of the workspace, as a modelling tool keeps its project
     * files apart from the engine that imports them: each a sub-voxel mesh
     * file in the workspace's Items folder, written exactly as the game's
     * item meshes are. Import brings one into the game: its mesh written
     * among the game's item meshes under its own name, and an item naming it
     * made in the definition file asked for, unless content already names
     * that mesh, which then simply takes the new shape. The workspace file
     * stays to be worked and imported again.
     */

    // Internal
    private SubVoxelManager subVoxelManager;
    private MeshManager meshManager;
    private InfoManager infoManager;

    // Directory
    private File itemRoot;
    private File meshRoot;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.itemRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.ITEM_WORKSPACE_DIRECTORY);
        this.meshRoot = new File(EngineSetting.MESH_PATH);
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
        this.meshManager = get(MeshManager.class);
        this.infoManager = get(InfoManager.class);
    }

    // Files \\

    File getFolder() {
        return itemRoot;
    }

    // The item a file in the Items folder holds, by name; null for a file outside it
    String toItemName(File file) {
        return WorkspaceUtility.toName(itemRoot, file);
    }

    boolean isNameAvailable(String itemName) {
        return FileUtility.isValidFileName(itemName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !resolveFile(itemName).exists();
    }

    private File resolveFile(String itemName) {
        return ArpgUtility.resolveFile(itemRoot, itemName);
    }

    // Load \\

    // The workspace item's model; null when its file cannot be read or holds no part
    SubVoxelModelStruct read(String itemName, String fallbackTextureName) {

        ArpgObjectStruct meshArpg = ArpgUtility.tryLoadObject(resolveFile(itemName));

        if (meshArpg == null)
            return null;

        SubVoxelModelStruct model = subVoxelManager.resolveModel(meshArpg, fallbackTextureName);

        return model != null && model.getPartCount() > 0 ? model : null;
    }

    // Save \\

    boolean write(String itemName, SubVoxelModelStruct model) {

        File file = resolveFile(itemName);

        file.getParentFile().mkdirs();
        return ArpgUtility.tryWriteObject(file, subVoxelManager.toMeshArpg(model));
    }

    // Delete \\

    boolean delete(String itemName) {

        File file = resolveFile(itemName);
        return !file.isFile() || file.delete();
    }

    // Import \\

    // The mesh the workspace item is written to among the game's item meshes
    String toMeshName(String itemName) {
        return EditorSetting.ITEM_EDITOR_MESH_DIRECTORY + EditorSetting.INFO_FOLDER_SEPARATOR
                + FileUtility.getFileName(itemName);
    }

    // Whether the item may be made in the definition file: one the game has, or a new file's name, holding no item of
    // the workspace item's name yet; any file will do when content already names the mesh
    boolean isImportDefinition(String itemName, String definitionName) {

        if (infoManager.isResourceNamed(EngineSetting.MESH_PATH, toMeshName(itemName)))
            return true;

        return (infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ITEMS, definitionName)
                || FileUtility.isValidFileName(definitionName, EditorSetting.NAME_INPUT_MAX_LENGTH))
                && !infoManager.hasEntry(
                        EditorSetting.INFO_SCHEMA_ITEMS, definitionName, FileUtility.getFileName(itemName));
    }

    // The model brought into the game: its mesh written among the game's item meshes and an item naming it made in the
    // definition file, unless content already names the mesh; null when it was, else why not
    String importItem(String itemName, SubVoxelModelStruct model, String definitionName) {

        String meshName = toMeshName(itemName);
        File meshFile = ArpgUtility.resolveFile(meshRoot, meshName);
        boolean named = infoManager.isResourceNamed(EngineSetting.MESH_PATH, meshName);

        meshFile.getParentFile().mkdirs();

        if (!ArpgUtility.tryWriteObject(meshFile, subVoxelManager.toMeshArpg(model)))
            return EditorSetting.ITEM_EDITOR_MESSAGE_NOT_WRITTEN + meshName;

        meshManager.addMeshFile(meshName, meshFile);
        infoManager.notifyResourcesChanged();

        if (named)
            return null;

        infoManager.createEntry(
                EditorSetting.INFO_SCHEMA_ITEMS,
                definitionName,
                FileUtility.getFileName(itemName),
                itemArpg -> itemArpg.addProperty(EditorSetting.INFO_ITEM_MESH_FIELD, meshName));

        return infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ITEMS, definitionName)
                ? null
                : infoManager.getRefusal();
    }
}
