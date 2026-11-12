package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterMeshRowBranch extends BranchPackage {

    /*
     * Lists the rows of the Mesh, Sculpt and Weights panels. The left panel
     * names every part the character wears a mesh for, the active one
     * selected; the right panel holds the active part's options, the
     * selection's numbers, the selected faces' texture, new parts and file in
     * Mesh mode, its shape keys, mirror, x-ray and file in Sculpt mode, and
     * the selected bone's weight target, options, the active vertex's weights
     * and file in Weights mode. Every tool and action — transform, geometry,
     * weight and selection — is the Tools panel's, listed by the mode's tool
     * canvas. Every value shown is read through CharacterMeshPropertyBranch,
     * the same path that edits it.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterMeshPropertyBranch characterMeshPropertyBranch;

    // Scratch
    private ObjectArrayList<String> partKeys;
    private ObjectArrayList<String> influenceLines;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.partKeys = new ObjectArrayList<>();
        this.influenceLines = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
        this.characterSculptBranch = get(CharacterSculptBranch.class);
        this.characterMeshPropertyBranch = get(CharacterMeshPropertyBranch.class);
    }

    // List \\

    void addPartRows(CharacterDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_PARTS));
        partKeys.clear();
        characterMeshBranch.collectPartKeys(document, partKeys);

        for (int i = 0; i < partKeys.size(); i++) {

            String partKey = partKeys.get(i);

            if (!characterEditorManager.holdsPartKey(characterEditorManager.getRegion(), partKey))
                continue;

            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_MESH_PART, partKey),
                    toPartLabel(document, partKey),
                    partKey.equals(characterEditorManager.getActivePartKey())));
        }
    }

    private String toPartLabel(CharacterDocumentInstance document, String partKey) {

        MeshHandle meshHandle = characterMeshBranch.resolvePartMesh(document, partKey);
        CharacterMeshDocumentInstance meshDocument = characterMeshBranch.open(meshHandle);
        String label = partKey + EditorSetting.CHARACTER_VIEW_PAIR_SEPARATOR + toLocalName(meshHandle.getMeshName());

        return meshDocument != null && meshDocument.isDirty()
                ? label + EditorSetting.CHARACTER_VIEW_DIRTY_MARKER
                : label;
    }

    private String toLocalName(String meshName) {
        return meshName.substring(meshName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1);
    }

    // Mesh \\

    void buildMeshRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (!addDocumentHeader(document, rows))
            return;

        boolean selected = characterSelectionBranch.getSelectedCount(document) > 0;

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_OPTIONS));
        addViewOptions(rows);
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_PROPORTIONAL,
                EditorSetting.CHARACTER_EDITOR_LABEL_PROPORTIONAL);

        if (characterEditorManager.isProportional())
            addNumber(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_RADIUS,
                    EditorSetting.CHARACTER_EDITOR_LABEL_RADIUS);

        addNumber(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_SNAP, EditorSetting.CHARACTER_EDITOR_LABEL_SNAP);

        if (selected) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_SELECTION));
            addAxes(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER, EditorSetting.CHARACTER_EDITOR_LABEL_CENTER);
            addAxes(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE, EditorSetting.CHARACTER_EDITOR_LABEL_SIZE);
            addAxes(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_ROTATE, EditorSetting.CHARACTER_EDITOR_LABEL_TURN);
        }

        if (characterSculptBranch.findSelectedFaceTexture(document) != null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_FACES));
            addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_FACE_TEXTURE,
                    EditorSetting.CHARACTER_EDITOR_LABEL_TEXTURE);
        }

        addPartToolRows(rows);
        addFileRows(rows);
    }

    private void addPartToolRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_NEW_PART));
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_PART_SHAPE, EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE);
        addNumber(rows, EditorSetting.CHARACTER_EDITOR_KEY_PART_SIZE, EditorSetting.CHARACTER_EDITOR_LABEL_SIZE);
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_PART_TEXTURE, EditorSetting.CHARACTER_EDITOR_LABEL_TEXTURE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_ADD_PART,
                EditorSetting.CHARACTER_EDITOR_LABEL_ADD_PART + characterEditorManager.getSelectedBoneName());
    }

    // Sculpt \\

    void buildSculptRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (!addDocumentHeader(characterEditorManager.getActiveMeshDocument(), rows))
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_SHAPE_KEYS));
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_EDIT_SHAPE, EditorSetting.CHARACTER_EDITOR_LABEL_EDIT_SHAPE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_ADD_SHAPE, EditorSetting.CHARACTER_EDITOR_LABEL_ADD_SHAPE);

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_OPTIONS));
        addViewOptions(rows);
        addFileRows(rows);
    }

    // Weights \\

    void buildWeightRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (!addDocumentHeader(document, rows))
            return;

        rows.add(CharacterViewRowStruct.header(characterEditorManager.getSelectedBoneName()));
        addNumber(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TARGET, EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT);

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_OPTIONS));
        addViewOptions(rows);
        addInfluenceRows(document, rows);
        addFileRows(rows);
    }

    private void addInfluenceRows(
            CharacterMeshDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows) {

        influenceLines.clear();
        characterMeshPropertyBranch.collectActiveInfluences(document, influenceLines);

        if (influenceLines.isEmpty())
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_ACTIVE_VERTEX));

        for (int i = 0; i < influenceLines.size(); i++)
            rows.add(CharacterViewRowStruct.header(influenceLines.get(i)));
    }

    // Shared \\

    // The active part's mesh name and counts; false, after saying why, when the part has no mesh file to edit
    private boolean addDocumentHeader(
            CharacterMeshDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows) {

        if (document == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_NO_MESH));
            return false;
        }

        String meshName = toLocalName(document.getMeshName());

        rows.add(CharacterViewRowStruct.header(
                document.isDirty() ? meshName + EditorSetting.CHARACTER_VIEW_DIRTY_MARKER : meshName));
        rows.add(CharacterViewRowStruct.header(characterMeshPropertyBranch.getStatsText(document)));

        return true;
    }

    private void addViewOptions(ObjectArrayList<CharacterViewRowStruct> rows) {
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_MIRROR, EditorSetting.CHARACTER_EDITOR_LABEL_MIRROR);
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_XRAY, EditorSetting.CHARACTER_EDITOR_LABEL_XRAY);
    }

    private void addFileRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_MESH_FILE));
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_UNDO, EditorSetting.CHARACTER_EDITOR_LABEL_UNDO);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_REDO, EditorSetting.CHARACTER_EDITOR_LABEL_REDO);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SAVE_MESH, EditorSetting.CHARACTER_EDITOR_LABEL_SAVE_MESH);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_REVERT_MESH,
                EditorSetting.CHARACTER_EDITOR_LABEL_REVERT_MESH);
    }

    // Utility \\

    private void addAxes(ObjectArrayList<CharacterViewRowStruct> rows, String keyName, String label) {

        for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_LABELS.length; axis++)
            addNumber(rows, CharacterViewUtility.toKey(keyName, axis),
                    label + EditorSetting.CHARACTER_VIEW_LABEL_SPACE + EditorSetting.CHARACTER_VIEW_AXIS_LABELS[axis]);
    }

    private void addNumber(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.number(key, label, characterMeshPropertyBranch.getValueText(key)));
    }

    private void addChoice(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.choice(key, label, characterMeshPropertyBranch.getValueText(key)));
    }

    private void addAction(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.action(key, label));
    }
}
