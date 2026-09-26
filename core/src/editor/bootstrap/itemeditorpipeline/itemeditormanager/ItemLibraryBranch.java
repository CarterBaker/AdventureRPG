package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;

class ItemLibraryBranch extends BranchPackage {

    /*
     * Reads and writes item meshes on disk. A mesh opens as sub-voxels,
     * converting an authored quad mesh on the way, and saves back to the same
     * mesh file. The item definitions that name those meshes belong to
     * InfoManager, never to this branch.
     */

    // Internal
    private SubVoxelManager subVoxelManager;

    // Directory
    private File meshRoot;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.meshRoot = new File(EngineSetting.MESH_JSON_PATH);
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Load \\

    SubVoxelModelStruct loadModel(ItemEntryStruct entry, String fallbackTextureName) {

        SubVoxelModelStruct model = subVoxelManager.resolveModel(loadMeshJson(entry), fallbackTextureName);

        if (model == null)
            return throwException("Item '" + entry.getItemName() + "' uses mesh '" + entry.getMeshName()
                    + "', which holds neither sub-voxels nor quads.");

        if (model.getPartCount() == 0)
            throwException("Item '" + entry.getItemName() + "' declares no parts in mesh '"
                    + entry.getMeshName() + "'.");

        return model;
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
        JsonUtility.writeJsonObject(
                getMeshFile(document.getEntry().getMeshName()),
                subVoxelManager.toMeshJson(document.getModel()),
                internal.gson);
    }

    // Delete \\

    void deleteMesh(String meshName) {

        File meshFile = getMeshFile(meshName);

        if (meshFile.isFile() && !meshFile.delete())
            throwException("Failed to delete mesh file: " + meshFile.getAbsolutePath());
    }

    // Utility \\

    private File getMeshFile(String meshName) {
        return new File(meshRoot, meshName + "." + EditorSetting.ITEM_EDITOR_FILE_EXTENSION);
    }

    // Accessible \\

    boolean hasMesh(String meshName) {
        return getMeshFile(meshName).isFile();
    }
}
