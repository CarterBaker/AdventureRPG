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
     * Lists the rows of the Mesh and Weights panels. The left panel names
     * every part the character wears a mesh for, the active one selected;
     * the right panel holds the active part's options, selection, geometry
     * tools, new parts and file in Mesh mode, and the selected bone's weight
     * tools and the active vertex's weights in Weights mode. Every value
     * shown is read through CharacterMeshPropertyBranch, the same path that
     * edits it.
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
        addSelectionRows(rows);

        if (selected) {
            addAxes(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER, EditorSetting.CHARACTER_EDITOR_LABEL_CENTER);
            addAxes(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE, EditorSetting.CHARACTER_EDITOR_LABEL_SIZE);
            addAxes(rows, EditorSetting.CHARACTER_EDITOR_KEY_MESH_ROTATE, EditorSetting.CHARACTER_EDITOR_LABEL_TURN);
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_TRANSFORM));
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_GRAB, EditorSetting.CHARACTER_EDITOR_LABEL_GRAB);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_ROTATE, EditorSetting.CHARACTER_EDITOR_LABEL_ROTATE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SCALE, EditorSetting.CHARACTER_EDITOR_LABEL_SCALE);

        addGeometryRows(rows);

        if (characterSculptBranch.findSelectedFaceTexture(document) != null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_FACES));
            addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_FACE_TEXTURE,
                    EditorSetting.CHARACTER_EDITOR_LABEL_TEXTURE);
        }

        addPartToolRows(rows);
        addFileRows(rows);
    }

    private void addGeometryRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_GEOMETRY));
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_EXTRUDE, EditorSetting.CHARACTER_EDITOR_LABEL_EXTRUDE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE, EditorSetting.CHARACTER_EDITOR_LABEL_DUPLICATE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_MAKE_FACE, EditorSetting.CHARACTER_EDITOR_LABEL_MAKE_FACE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_MERGE, EditorSetting.CHARACTER_EDITOR_LABEL_MERGE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SUBDIVIDE, EditorSetting.CHARACTER_EDITOR_LABEL_SUBDIVIDE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_DELETE_VERTICES,
                EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_VERTICES);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_DELETE_FACES,
                EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_FACES);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_POSITIVE,
                EditorSetting.CHARACTER_EDITOR_LABEL_SYMMETRIZE_POSITIVE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_NEGATIVE,
                EditorSetting.CHARACTER_EDITOR_LABEL_SYMMETRIZE_NEGATIVE);
    }

    private void addPartToolRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_NEW_PART));
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_PART_SHAPE, EditorSetting.CHARACTER_EDITOR_LABEL_SHAPE);
        addNumber(rows, EditorSetting.CHARACTER_EDITOR_KEY_PART_SIZE, EditorSetting.CHARACTER_EDITOR_LABEL_SIZE);
        addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_PART_TEXTURE, EditorSetting.CHARACTER_EDITOR_LABEL_TEXTURE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_ADD_PART,
                EditorSetting.CHARACTER_EDITOR_LABEL_ADD_PART + characterEditorManager.getSelectedBoneName());
    }

    // Weights \\

    void buildWeightRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (!addDocumentHeader(document, rows))
            return;

        rows.add(CharacterViewRowStruct.header(characterEditorManager.getSelectedBoneName()));
        addNumber(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TARGET, EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_ASSIGN);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ADD, EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_ADD);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_SUBTRACT);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_REMOVE,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_REMOVE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_NORMALIZE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_SMOOTH);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_POSITIVE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_NEGATIVE,
                EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_NEGATIVE);

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_OPTIONS));
        addViewOptions(rows);
        addSelectionRows(rows);
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

    private void addSelectionRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_SELECTION));
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_ALL, EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_ALL);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_NONE,
                EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_NONE);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_INVERT,
                EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_INVERT);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LINKED,
                EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_LINKED);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MIRROR,
                EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_MIRROR);
        addAction(rows, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_BONE,
                EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_BONE + characterEditorManager.getSelectedBoneName());
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
