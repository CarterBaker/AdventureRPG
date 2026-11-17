package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import application.bootstrap.geometrypipeline.mesh.MeshZone;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.GltfExportUtility;
import editor.bootstrap.charactereditorpipeline.util.GltfImportStruct;
import editor.bootstrap.charactereditorpipeline.util.GltfImportUtility;
import editor.bootstrap.charactereditorpipeline.util.GltfReadUtility;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterModelBranch extends BranchPackage {

    /*
     * Import and Export for the mesh being edited (see
     * .claude/docs/GLTF.md). Export writes the open mesh, its rig and its
     * morphs as a .glb in the workspace's Models folder beside the saves.
     * Import reads a .glb from that folder in steps the File menu's dialogs
     * walk: the file is inspected, bones the rig lacks are added only when
     * the user says so, a model with no skin is given the bone it hangs
     * from, and every material that names no texture and zone of its own is
     * given them by the user; then the model replaces the open mesh as one
     * edit, undone like any other and written by Save. The game never reads
     * a .glb, and nothing imported is an AI asset.
     */

    // Internal
    private MeshManager meshManager;
    private TextureManager textureManager;
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;

    // Directory
    private File modelRoot;

    // Import
    private GltfImportStruct model;
    private String modelName;
    private String[] textures;
    private MeshZone[] zones;
    private String hangBone;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.modelRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.MODEL_USER_DIRECTORY);
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.textureManager = get(TextureManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
    }

    // Export \\

    public void exportModel() {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (document == null) {
            characterEditorManager.setStatusMessage(EditorSetting.MODEL_MESSAGE_NO_MESH);
            return;
        }

        RiggedMeshStruct mesh = document.getMesh();
        int[] normals = new int[mesh.getFaceCount() * EngineSetting.QUAD_VERTEX_COUNT];
        String name = new File(document.getMeshName()).getName();
        File file = resolveModelFile(name);

        meshManager.packRiggedNormals(mesh, normals);

        try {
            modelRoot.mkdirs();
            Files.write(file.toPath(), GltfExportUtility.export(mesh, mesh.getRigHandle(), name, normals));
            characterEditorManager.setStatusMessage(EditorSetting.MODEL_MESSAGE_EXPORTED + file.getAbsolutePath());
        } catch (IOException e) {
            characterEditorManager.setStatusMessage(
                    EditorSetting.MODEL_MESSAGE_EXPORT_FAILED + file.getAbsolutePath());
        }
    }

    // Files \\

    public String getModelDirectory() {
        return modelRoot.getAbsolutePath();
    }

    public boolean isModelFile(String name) {
        return !name.isBlank() && resolveModelFile(name).isFile();
    }

    private File resolveModelFile(String name) {

        String extension = "." + EditorSetting.GLTF_FILE_EXTENSION;
        return new File(modelRoot, name.toLowerCase().endsWith(extension) ? name : name + extension);
    }

    // Import \\

    // Opens the file and reads what it holds; a refusal is left in the status line and nothing is imported
    public void beginImport(String name) {

        this.model = null;
        this.hangBone = null;
        this.modelName = name;

        if (characterEditorManager.getActiveMeshDocument() == null) {
            characterEditorManager.setStatusMessage(EditorSetting.MODEL_MESSAGE_NO_MESH);
            return;
        }

        try {
            this.model = GltfImportUtility.inspect(
                    GltfReadUtility.read(Files.readAllBytes(resolveModelFile(name).toPath())));
        } catch (IOException | InternalException e) {
            refuse(e);
            return;
        }

        this.textures = new String[model.getMaterialCount()];
        this.zones = new MeshZone[model.getMaterialCount()];

        for (int m = 0; m < textures.length; m++) {
            textures[m] = isTexture(model.getMaterialTexture(m)) ? model.getMaterialTexture(m) : null;
            zones[m] = findZone(model.getMaterialZone(m));
        }
    }

    public boolean isImporting() {
        return model != null;
    }

    private RigHandle getRigHandle() {
        return characterEditorManager.getActiveMeshDocument().getMesh().getRigHandle();
    }

    private void refuse(Exception failure) {

        this.model = null;
        characterEditorManager.setStatusMessage(EditorSetting.MODEL_MESSAGE_REFUSED + failure.getMessage());
    }

    // Bones \\

    public boolean hasMissingBones() {
        return !GltfImportUtility.findMissingBones(model, getRigHandle()).isEmpty();
    }

    public String describeMissingBones() {
        return String.join(", ", GltfImportUtility.findMissingBones(model, getRigHandle()));
    }

    // Adds the bones the model weighs onto and the rig lacks, each under the joint it hangs from in the file; the
    // import ends here and is run again against the rig as it now stands
    public void addMissingBones() {

        ObjectArrayList<String> names = GltfImportUtility.findMissingBones(model, getRigHandle());
        ObjectArrayList<String> parents = new ObjectArrayList<>(names.size());
        float[] points = new float[names.size() * 3];

        for (int i = 0; i < names.size(); i++) {
            parents.add(model.getJointParent(names.get(i)));
            System.arraycopy(model.getJointPoints(), model.getJointPointOffset(names.get(i)), points, i * 3, 3);
        }

        this.model = null;
        characterEditorManager.addRigBones(names, parents, points);
        characterEditorManager.setStatusMessage(names.size() + EditorSetting.MODEL_MESSAGE_BONES_ADDED);
    }

    public boolean needsHangBone() {
        return !model.isSkinned() && hangBone == null;
    }

    public boolean isBone(String boneName) {
        return getRigHandle().hasBone(boneName);
    }

    public void setHangBone(String boneName) {
        this.hangBone = boneName;
    }

    // Materials \\

    public boolean needsTexture() {
        return findOpenMaterial() != EngineSetting.INDEX_NOT_FOUND && textures[findOpenMaterial()] == null;
    }

    public boolean needsZone() {
        return findOpenMaterial() != EngineSetting.INDEX_NOT_FOUND;
    }

    // The name of the material the next texture or zone is asked for
    public String getOpenMaterialName() {

        String name = model.getMaterialName(findOpenMaterial());
        return name.isEmpty() ? EditorSetting.MODEL_UNNAMED_MATERIAL : name;
    }

    public boolean isTexture(String textureName) {
        return textureName != null && !textureName.isBlank() && textureManager.hasTexture(textureName);
    }

    public void setTexture(String textureName) {
        textures[findOpenMaterial()] = textureName;
    }

    public boolean isZone(String zoneName) {
        return findZone(zoneName) != null;
    }

    public void setZone(String zoneName) {
        zones[findOpenMaterial()] = findZone(zoneName);
    }

    // The first material a face uses that still lacks its texture or its zone
    private int findOpenMaterial() {

        for (int m = 0; m < textures.length; m++)
            if (model.isMaterialUsed(m) && (textures[m] == null || zones[m] == null))
                return m;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private MeshZone findZone(String zoneName) {

        for (MeshZone zone : MeshZone.VALUES)
            if (zone.name().equalsIgnoreCase(zoneName))
                return zone;

        return null;
    }

    // Finish \\

    // The model takes the open mesh's place as one edit, refused like any edit that would pass the mesh's limits
    public void finishImport() {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();
        RiggedMeshStruct mesh;

        try {
            mesh = GltfImportUtility.build(model, getRigHandle(), textures, zones, hangBone);
        } catch (InternalException e) {
            refuse(e);
            return;
        }

        this.model = null;
        characterHistoryBranch.begin(document);
        document.getMesh().set(mesh);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(EditorSetting.MODEL_MESSAGE_IMPORTED + mesh.getFaceCount()
                    + EditorSetting.MODEL_MESSAGE_IMPORTED_QUADS + modelName);
    }
}
