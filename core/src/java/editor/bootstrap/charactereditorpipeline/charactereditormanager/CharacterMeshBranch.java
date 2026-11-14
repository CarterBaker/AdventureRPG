package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.io.File;

import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterMeshBranch extends BranchPackage {

    /*
     * Opens, saves and reverts the rigged mesh files a character wears — its
     * body and the mesh of each default feature — and rebuilds the registered
     * mesh in place after every change, so every view drawing it, this
     * editor's, the animation editors' and a running game's, shows the edit
     * at once. A mesh opens once and stays open with its edits and history
     * until saved or reverted. An edit is finished here: refused when it
     * would leave the mesh empty or past the vertex limit, committed to the
     * history otherwise.
     */

    // Internal
    private MeshManager meshManager;
    private TextureManager textureManager;
    private CharacterEditorManager characterEditorManager;
    private CharacterHistoryBranch characterHistoryBranch;

    // Directory
    private File meshRoot;

    // Palette
    private Object2ObjectOpenHashMap<String, CharacterMeshDocumentInstance> meshName2MeshDocument;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<String>> arrayName2TextureNames;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.meshRoot = new File(EngineSetting.MESH_PATH);

        // Palette
        this.meshName2MeshDocument = new Object2ObjectOpenHashMap<>();
        this.arrayName2TextureNames = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.textureManager = get(TextureManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
    }

    // Parts \\

    // The keys of every part the preview's gender wears a mesh for: the body, then each slot's starting feature
    void collectPartKeys(CharacterDocumentInstance document, ObjectArrayList<String> partKeys) {

        if (!document.hasPreview())
            return;

        RaceData raceData = document.getPreviewData();
        partKeys.add(EditorSetting.CHARACTER_EDITOR_PART_BODY);

        if (!raceData.hasAppearance())
            return;

        short genderID = characterEditorManager.getPreviewGenderID();

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (raceData.getAppearanceData().getStartingFeature(genderID, featureSlot) != null)
                partKeys.add(ArpgUtility.toEnumName(featureSlot));
    }

    MeshHandle resolvePartMesh(CharacterDocumentInstance document, String partKey) {

        if (!document.hasPreview())
            return null;

        RaceData raceData = document.getPreviewData();
        short genderID = characterEditorManager.getPreviewGenderID();

        if (partKey.equals(EditorSetting.CHARACTER_EDITOR_PART_BODY))
            return raceData.getGender(genderID).getMeshHandle();

        FeatureSlot featureSlot = CharacterViewUtility.toEnumOrDefault(partKey, FeatureSlot.class, null);

        if (featureSlot == null || !raceData.hasAppearance())
            return null;

        FeatureHandle featureHandle = raceData.getAppearanceData().getStartingFeature(genderID, featureSlot);
        return featureHandle != null ? featureHandle.getMeshHandle() : null;
    }

    // Documents \\

    // The open document of a mesh, opening its file the first time; null for a mesh with no rigged file of its own
    CharacterMeshDocumentInstance open(MeshHandle meshHandle) {

        if (meshHandle == null || !meshHandle.hasRig())
            return null;

        CharacterMeshDocumentInstance document = meshName2MeshDocument.get(meshHandle.getMeshName());

        if (document != null)
            return document;

        File meshFile = getMeshFile(meshHandle.getMeshName());
        ArpgObjectStruct meshArpg = meshFile.isFile() ? ArpgUtility.tryLoadObject(meshFile) : null;

        if (meshArpg == null || !meshManager.isRiggedMesh(meshArpg))
            return null;

        document = create(CharacterMeshDocumentInstance.class);
        document.constructor(meshHandle, meshArpg, meshManager.parseRiggedMesh(meshArpg, meshFile.getName()));
        meshName2MeshDocument.put(meshHandle.getMeshName(), document);

        return document;
    }

    // Writes the mesh, a shape key being sculpted written back into its morph first
    boolean save(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (document.isEditingShape()) {
            mesh = new RiggedMeshStruct(mesh);
            CharacterMeshUtility.bakeShapeKey(mesh, document.getEditShape(), document.getShapeBase());
        }

        ArpgObjectStruct meshArpg = meshManager.toRiggedMeshArpg(mesh, document.getSourceArpg());

        if (!ArpgUtility.tryWriteObject(getMeshFile(document.getMeshName()), meshArpg))
            return false;

        document.markSaved(meshArpg);
        return true;
    }

    // Reads the file back, dropping every unsaved change and the history with them
    boolean revert(CharacterMeshDocumentInstance document) {

        File meshFile = getMeshFile(document.getMeshName());
        ArpgObjectStruct meshArpg = meshFile.isFile() ? ArpgUtility.tryLoadObject(meshFile) : null;

        if (meshArpg == null)
            return false;

        RiggedMeshStruct mesh = meshManager.parseRiggedMesh(meshArpg, meshFile.getName());

        document.replace(meshArpg, mesh);
        characterHistoryBranch.clear(document);
        upload(document);

        return true;
    }

    // Shape Keys \\

    // Sculpts a morph instead of the base: the morph is lifted into the positions every tool edits and baked back into
    // its deltas when sculpting returns to the base, so each tool shapes it unchanged. The history is cleared on either
    // side, since its steps hold the surface as it was being sculpted.
    void editShape(CharacterMeshDocumentInstance document, int morphIndex) {

        if (document.getEditShape() == morphIndex)
            return;

        RiggedMeshStruct mesh = document.getMesh();

        if (document.isEditingShape())
            CharacterMeshUtility.bakeShapeKey(mesh, document.getEditShape(), document.getShapeBase());

        if (morphIndex != EngineSetting.INDEX_NOT_FOUND)
            CharacterMeshUtility.liftShapeKey(mesh, morphIndex, document.getShapeBase());

        document.setEditShape(morphIndex);
        characterHistoryBranch.clear(document);
        upload(document);
        characterEditorManager.notifyChanged();
    }

    // A new, empty shape key under the first free numbered name, sculpted at once
    void addShape(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        int number = mesh.getMorphCount() + 1;

        while (mesh.getMorphIndex(EditorSetting.CHARACTER_EDITOR_SHAPE_NAME_PREFIX + number)
                != EngineSetting.INDEX_NOT_FOUND)
            number++;

        int morphIndex = mesh.resolveMorph(EditorSetting.CHARACTER_EDITOR_SHAPE_NAME_PREFIX + number);

        document.markEdited();
        editShape(document, morphIndex);
    }

    // Every open mesh back to sculpting its base
    void endShapeEdits() {

        for (CharacterMeshDocumentInstance document : meshName2MeshDocument.values())
            editShape(document, EngineSetting.INDEX_NOT_FOUND);
    }

    // Edits \\

    // Accepts an edit made since the history step opened, or puts the mesh back and says why it was refused
    boolean finishEdit(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (mesh.isEmpty())
            return refuseEdit(document, EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_EMPTY);

        if (!meshManager.fitsRiggedMeshLimit(mesh))
            return refuseEdit(document, EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_LIMIT);

        document.syncSelection();
        document.markEdited();
        characterHistoryBranch.commit(document);
        upload(document);
        characterEditorManager.notifyChanged();

        return true;
    }

    private boolean refuseEdit(CharacterMeshDocumentInstance document, String message) {

        characterHistoryBranch.cancel(document);
        upload(document);
        characterEditorManager.setStatusMessage(message);

        return false;
    }

    // A change still in progress — a held transform or a brush stroke — shown live without touching the history
    void preview(CharacterMeshDocumentInstance document) {
        document.markEdited();
        upload(document);
    }

    void upload(CharacterMeshDocumentInstance document) {

        if (!document.getMesh().isEmpty())
            meshManager.updateRiggedMesh(document.getMeshHandle(), document.getMesh());
    }

    // Textures \\

    // Every texture sharing the array a mesh's first texture lives in, by name
    ObjectArrayList<String> getTextureNames(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (mesh.getTextureCount() == 0)
            return new ObjectArrayList<>();

        String arrayName = textureManager.getTextureHandleFromTextureName(mesh.getTextureName(0)).getArrayName();
        ObjectArrayList<String> textureNames = arrayName2TextureNames.get(arrayName);

        if (textureNames == null) {
            textureNames = textureManager.getTextureNamesInArray(arrayName);
            arrayName2TextureNames.put(arrayName, textureNames);
        }

        return textureNames;
    }

    // Utility \\

    private File getMeshFile(String meshName) {
        return ArpgUtility.resolveFile(meshRoot, meshName);
    }

    // Accessible \\

    void collectDirtyDocuments(ObjectArrayList<CharacterMeshDocumentInstance> documents) {

        for (CharacterMeshDocumentInstance document : meshName2MeshDocument.values())
            if (document.isDirty())
                documents.add(document);
    }
}
