package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorShape;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorWeightTool;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterMeshPropertyBranch extends BranchPackage {

    /*
     * The one place every value of the Mesh and Weights panels is read,
     * stepped, typed, cycled or acted on, by the key its row carries, for
     * CharacterPropertyBranch to hand those keys to. Selection numbers move,
     * size or turn the selection itself; tool numbers and choices live on
     * the manager; actions run the selection, sculpt, weight and history
     * branches on the active part's mesh. Nothing here acts while a
     * transform is held in the viewport.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterWeightBranch characterWeightBranch;

    // Scratch
    private Vector3 centerScratch;
    private Vector3 minScratch;
    private Vector3 maxScratch;
    private Vector3 normalScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.centerScratch = new Vector3();
        this.minScratch = new Vector3();
        this.maxScratch = new Vector3();
        this.normalScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
        this.characterSculptBranch = get(CharacterSculptBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return isNumberKey(keyName) || isChoiceKey(keyName) || isActionKey(keyName)
                || keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_MESH_PART);
    }

    private boolean isNumberKey(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_ROTATE,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_RADIUS,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_SNAP,
                    EditorSetting.CHARACTER_EDITOR_KEY_PART_SIZE,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TARGET,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_STRENGTH,
                    EditorSetting.CHARACTER_EDITOR_KEY_BRUSH_RADIUS -> true;
            default -> false;
        };
    }

    private boolean isChoiceKey(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_MIRROR,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_XRAY,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_PROPORTIONAL,
                    EditorSetting.CHARACTER_EDITOR_KEY_PART_SHAPE,
                    EditorSetting.CHARACTER_EDITOR_KEY_PART_TEXTURE,
                    EditorSetting.CHARACTER_EDITOR_KEY_FACE_TEXTURE,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TOOL -> true;
            default -> false;
        };
    }

    private boolean isActionKey(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_ALL,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_NONE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_INVERT,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LINKED,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MIRROR,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_BONE,
                    EditorSetting.CHARACTER_EDITOR_KEY_GRAB,
                    EditorSetting.CHARACTER_EDITOR_KEY_ROTATE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SCALE,
                    EditorSetting.CHARACTER_EDITOR_KEY_EXTRUDE,
                    EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE,
                    EditorSetting.CHARACTER_EDITOR_KEY_DELETE_VERTICES,
                    EditorSetting.CHARACTER_EDITOR_KEY_DELETE_FACES,
                    EditorSetting.CHARACTER_EDITOR_KEY_MAKE_FACE,
                    EditorSetting.CHARACTER_EDITOR_KEY_MERGE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SUBDIVIDE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_POSITIVE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_NEGATIVE,
                    EditorSetting.CHARACTER_EDITOR_KEY_ADD_PART,
                    EditorSetting.CHARACTER_EDITOR_KEY_SAVE_MESH,
                    EditorSetting.CHARACTER_EDITOR_KEY_REVERT_MESH,
                    EditorSetting.CHARACTER_EDITOR_KEY_UNDO,
                    EditorSetting.CHARACTER_EDITOR_KEY_REDO,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ADD,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_REMOVE,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE,
                    EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_NEGATIVE -> true;
            default -> false;
        };
    }

    // Selection \\

    void select(String key) {

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_EDITOR_KEY_MESH_PART))
            characterEditorManager.selectPart(CharacterViewUtility.getKeyArgument(key));
    }

    // Values \\

    String getValueText(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        if (isNumberKey(keyName))
            return CharacterViewUtility.formatNumber(getNumber(key));

        return getChoiceText(keyName);
    }

    boolean isValueValid(String key, String text) {
        return isNumberKey(CharacterViewUtility.getKeyName(key))
                && hasTarget(key)
                && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {

        if (isValueValid(key, text))
            writeNumber(key, CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        String keyName = CharacterViewUtility.getKeyName(key);

        if (!isNumberKey(keyName) || !hasTarget(key))
            return;

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_MESH_ROTATE)) {
            writeNumber(key, direction * EditorSetting.CHARACTER_EDITOR_MESH_ROTATE_STEP);
            return;
        }

        writeNumber(key, getNumber(key) + direction * getStep(keyName));
    }

    // Numbers \\

    // Selection numbers need a selection held still on the active part; tool numbers always have their target
    private boolean hasTarget(String key) {

        if (characterSculptBranch.isTransforming())
            return false;

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE,
                    EditorSetting.CHARACTER_EDITOR_KEY_MESH_ROTATE -> resolveDocument() != null
                            && characterSelectionBranch.getSelectedCount(resolveDocument()) > 0;
            default -> true;
        };
    }

    private float getNumber(String key) {

        int axis = CharacterViewUtility.getKeyIndex(key);
        CharacterMeshDocumentInstance document = resolveDocument();

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER -> document != null
                    && characterSelectionBranch.resolveCenter(document, centerScratch)
                            ? getAxis(centerScratch, axis)
                            : 0f;
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE -> document != null
                    && characterSelectionBranch.resolveBounds(document, minScratch, maxScratch)
                            ? getAxis(maxScratch, axis) - getAxis(minScratch, axis)
                            : 0f;
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_RADIUS -> characterEditorManager.getProportionalRadius();
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_SNAP -> characterEditorManager.getSnapStep();
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_SIZE -> characterEditorManager.getPartSize();
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TARGET -> characterEditorManager.getWeightTarget();
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_STRENGTH -> characterEditorManager.getWeightStrength();
            case EditorSetting.CHARACTER_EDITOR_KEY_BRUSH_RADIUS -> characterEditorManager.getBrushRadius();
            default -> 0f;
        };
    }

    private void writeNumber(String key, float value) {

        int axis = CharacterViewUtility.getKeyIndex(key);
        CharacterMeshDocumentInstance document = resolveDocument();

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER -> characterSculptBranch.moveSelection(
                    document, axis, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE -> characterSculptBranch.resizeSelection(
                    document, axis, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_ROTATE -> characterSculptBranch.rotateSelection(
                    document, axis, value);
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_RADIUS -> characterEditorManager.setProportionalRadius(
                    CharacterViewUtility.round(CharacterViewUtility.clamp(
                            value,
                            EditorSetting.CHARACTER_EDITOR_MESH_RADIUS_MIN,
                            EditorSetting.CHARACTER_EDITOR_MESH_RADIUS_MAX)));
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_SNAP -> characterEditorManager.setSnapStep(
                    CharacterViewUtility.round(CharacterViewUtility.clamp(
                            value,
                            0f,
                            EditorSetting.CHARACTER_EDITOR_MESH_SNAP_MAX)));
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_SIZE -> characterEditorManager.setPartSize(
                    CharacterViewUtility.round(CharacterViewUtility.clamp(
                            value,
                            EditorSetting.CHARACTER_EDITOR_PART_SIZE_MIN,
                            EditorSetting.CHARACTER_EDITOR_PART_SIZE_MAX)));
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TARGET -> characterEditorManager.setWeightTarget(
                    CharacterViewUtility.round(CharacterViewUtility.clamp(value, 0f, 1f)));
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_STRENGTH -> characterEditorManager.setWeightStrength(
                    CharacterViewUtility.round(CharacterViewUtility.clamp(value, 0f, 1f)));
            default -> characterEditorManager.setBrushRadius(
                    CharacterViewUtility.round(CharacterViewUtility.clamp(
                            value,
                            EditorSetting.CHARACTER_EDITOR_BRUSH_RADIUS_MIN,
                            EditorSetting.CHARACTER_EDITOR_BRUSH_RADIUS_MAX)));
        }
    }

    private float getStep(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_CENTER -> EditorSetting.CHARACTER_EDITOR_POSITION_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_SIZE -> EditorSetting.CHARACTER_EDITOR_SIZE_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_RADIUS -> EditorSetting.CHARACTER_EDITOR_MESH_RADIUS_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_SNAP -> EditorSetting.CHARACTER_EDITOR_MESH_SNAP_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_SIZE -> EditorSetting.CHARACTER_EDITOR_SIZE_STEP;
            case EditorSetting.CHARACTER_EDITOR_KEY_BRUSH_RADIUS -> EditorSetting.CHARACTER_EDITOR_BRUSH_RADIUS_STEP;
            default -> EditorSetting.CHARACTER_EDITOR_WEIGHT_STEP_FINE;
        };
    }

    private float getAxis(Vector3 vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    // Choices \\

    void cycle(String key, int direction) {

        if (characterSculptBranch.isTransforming())
            return;

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_MIRROR -> characterEditorManager.setMirror(
                    !characterEditorManager.isMirror());
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_XRAY -> characterEditorManager.setXray(
                    !characterEditorManager.isXray());
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_PROPORTIONAL -> characterEditorManager.setProportional(
                    !characterEditorManager.isProportional());
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_SHAPE -> characterEditorManager.setPartShape(
                    CharacterEditorShape.VALUES[Math.floorMod(
                            characterEditorManager.getPartShape().ordinal() + direction,
                            CharacterEditorShape.VALUES.length)]);
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TOOL -> characterEditorManager.setWeightTool(
                    CharacterEditorWeightTool.VALUES[Math.floorMod(
                            characterEditorManager.getWeightTool().ordinal() + direction,
                            CharacterEditorWeightTool.VALUES.length)]);
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_TEXTURE -> cyclePartTexture(direction);
            case EditorSetting.CHARACTER_EDITOR_KEY_FACE_TEXTURE -> cycleFaceTexture(direction);
            default -> {
            }
        }
    }

    private String getChoiceText(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_MIRROR -> toYesNo(characterEditorManager.isMirror());
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_XRAY -> toYesNo(characterEditorManager.isXray());
            case EditorSetting.CHARACTER_EDITOR_KEY_MESH_PROPORTIONAL -> toYesNo(
                    characterEditorManager.isProportional());
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_SHAPE -> characterEditorManager.getPartShape().getLabel();
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_TOOL -> characterEditorManager.getWeightTool().getLabel();
            case EditorSetting.CHARACTER_EDITOR_KEY_PART_TEXTURE -> toTextureText(resolvePartTexture());
            case EditorSetting.CHARACTER_EDITOR_KEY_FACE_TEXTURE -> toTextureText(resolveDocument() != null
                    ? characterSculptBranch.findSelectedFaceTexture(resolveDocument())
                    : null);
            default -> "";
        };
    }

    private String toYesNo(boolean value) {
        return value ? EditorSetting.CHARACTER_VIEW_YES : EditorSetting.CHARACTER_VIEW_NO;
    }

    private String toTextureText(String textureName) {
        return textureName != null
                ? textureName.substring(textureName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1)
                : EditorSetting.CHARACTER_VIEW_NONE;
    }

    // Textures \\

    // The texture a new part is made in: the one chosen, or the active mesh's first
    String resolvePartTexture() {

        if (characterEditorManager.getPartTextureName() != null)
            return characterEditorManager.getPartTextureName();

        CharacterMeshDocumentInstance document = resolveDocument();

        return document != null && document.getMesh().getTextureCount() > 0
                ? document.getMesh().getTextureName(0)
                : null;
    }

    private void cyclePartTexture(int direction) {

        String next = resolveNextTexture(resolvePartTexture(), direction);

        if (next != null)
            characterEditorManager.setPartTextureName(next);
    }

    private void cycleFaceTexture(int direction) {

        CharacterMeshDocumentInstance document = resolveDocument();

        if (document == null)
            return;

        String next = resolveNextTexture(characterSculptBranch.findSelectedFaceTexture(document), direction);

        if (next != null)
            characterSculptBranch.setFaceTexture(document, next);
    }

    private String resolveNextTexture(String current, int direction) {

        CharacterMeshDocumentInstance document = resolveDocument();

        if (document == null)
            return null;

        ObjectArrayList<String> textureNames = characterMeshBranch.getTextureNames(document);

        if (textureNames.isEmpty())
            return null;

        int index = Math.max(0, textureNames.indexOf(current));
        return textureNames.get(Math.floorMod(index + direction, textureNames.size()));
    }

    // Actions \\

    void act(String key) {

        CharacterMeshDocumentInstance document = resolveDocument();
        String keyName = CharacterViewUtility.getKeyName(key);

        if (document == null || !isActionKey(keyName) || characterSculptBranch.isTransforming())
            return;

        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_UNDO -> characterEditorManager.undo();
            case EditorSetting.CHARACTER_EDITOR_KEY_REDO -> characterEditorManager.redo();
            case EditorSetting.CHARACTER_EDITOR_KEY_SAVE_MESH -> characterEditorManager.saveMesh(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_REVERT_MESH -> characterEditorManager.revertMesh(document);
            default -> {
                if (isSelectionAction(keyName))
                    actSelection(document, keyName);
                else if (isWeightAction(keyName))
                    actWeight(document, keyName);
                else
                    actGeometry(document, keyName);
            }
        }
    }

    private boolean isSelectionAction(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_ALL,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_NONE,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_INVERT,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LINKED,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MIRROR,
                    EditorSetting.CHARACTER_EDITOR_KEY_SELECT_BONE -> true;
            default -> false;
        };
    }

    private boolean isWeightAction(String keyName) {
        return keyName.startsWith(EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_PREFIX);
    }

    private void actSelection(CharacterMeshDocumentInstance document, String keyName) {
        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_ALL -> characterSelectionBranch.selectAll(document, true);
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_NONE -> characterSelectionBranch.selectAll(document, false);
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_INVERT -> characterSelectionBranch.invert(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LINKED -> characterSelectionBranch.selectLinked(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MIRROR -> characterSelectionBranch.selectMirror(document);
            default -> characterSelectionBranch.selectBone(document, characterEditorManager.getSelectedBoneIndex());
        }
    }

    private void actWeight(CharacterMeshDocumentInstance document, String keyName) {

        int boneIndex = characterEditorManager.getSelectedBoneIndex();

        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN -> characterWeightBranch.assign(
                    document, boneIndex, characterEditorManager.getWeightTarget());
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ADD -> characterWeightBranch.add(
                    document, boneIndex, EditorSetting.CHARACTER_EDITOR_WEIGHT_STEP_FINE);
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT -> characterWeightBranch.add(
                    document, boneIndex, -EditorSetting.CHARACTER_EDITOR_WEIGHT_STEP_FINE);
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_REMOVE -> characterWeightBranch.assign(
                    document, boneIndex, 0f);
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE -> characterWeightBranch.normalize(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH -> characterWeightBranch.smooth(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE -> characterWeightBranch.mirror(
                    document, true);
            default -> characterWeightBranch.mirror(document, false);
        }
    }

    private void actGeometry(CharacterMeshDocumentInstance document, String keyName) {
        switch (keyName) {
            case EditorSetting.CHARACTER_EDITOR_KEY_GRAB -> characterEditorManager.requestTransform(
                    CharacterEditorTransform.GRAB, null);
            case EditorSetting.CHARACTER_EDITOR_KEY_ROTATE -> characterEditorManager.requestTransform(
                    CharacterEditorTransform.ROTATE, null);
            case EditorSetting.CHARACTER_EDITOR_KEY_SCALE -> characterEditorManager.requestTransform(
                    CharacterEditorTransform.SCALE, null);
            case EditorSetting.CHARACTER_EDITOR_KEY_EXTRUDE -> extrude(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE -> {
                if (characterSculptBranch.duplicate(document))
                    characterEditorManager.requestTransform(CharacterEditorTransform.GRAB, null);
            }
            case EditorSetting.CHARACTER_EDITOR_KEY_DELETE_VERTICES -> characterSculptBranch.deleteVertices(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_DELETE_FACES -> characterSculptBranch.deleteFaces(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_MAKE_FACE -> characterSculptBranch.makeFace(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_MERGE -> characterSculptBranch.merge(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_SUBDIVIDE -> characterSculptBranch.subdivide(document);
            case EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_POSITIVE -> characterSculptBranch.symmetrize(
                    document, true);
            case EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_NEGATIVE -> characterSculptBranch.symmetrize(
                    document, false);
            case EditorSetting.CHARACTER_EDITOR_KEY_ADD_PART -> addPart(document);
            default -> {
            }
        }
    }

    // An extruded region is pulled out along its facing, extruded edges freely
    private void extrude(CharacterMeshDocumentInstance document) {

        if (!characterSculptBranch.extrude(document, normalScratch))
            return;

        characterEditorManager.requestTransform(
                CharacterEditorTransform.GRAB,
                normalScratch.lengthSquared() > 0f ? normalScratch : null);
    }

    private void addPart(CharacterMeshDocumentInstance document) {

        String textureName = resolvePartTexture();

        if (textureName == null) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_TEXTURE);
            return;
        }

        characterSculptBranch.addShape(
                document,
                characterEditorManager.getPartShape(),
                characterEditorManager.getPartSize(),
                characterEditorManager.getSelectedBoneIndex(),
                textureName);
    }

    // Utility \\

    private CharacterMeshDocumentInstance resolveDocument() {
        return characterEditorManager.getActiveMeshDocument();
    }

    // Accessible \\

    // The panel's line counting the active mesh's vertices, faces and selection
    String getStatsText(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        return mesh.getVertexCount() + EditorSetting.CHARACTER_EDITOR_LABEL_VERTICES
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + mesh.getFaceCount() + EditorSetting.CHARACTER_EDITOR_LABEL_FACES
                + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + characterSelectionBranch.getSelectedCount(document) + EditorSetting.CHARACTER_EDITOR_LABEL_SELECTED;
    }

    // The bones and weights of the active vertex in the order the mesh holds them, one line each
    void collectActiveInfluences(CharacterMeshDocumentInstance document, ObjectArrayList<String> lines) {

        int activeVertex = document.getActiveVertex();

        if (activeVertex == EngineSetting.INDEX_NOT_FOUND)
            return;

        RiggedMeshStruct mesh = document.getMesh();

        for (int i = 0; i < mesh.getInfluenceCount(activeVertex); i++)
            lines.add(mesh.getRigHandle().getBone(mesh.getInfluenceBone(activeVertex, i)).getName()
                    + EditorSetting.CHARACTER_VIEW_PAIR_SEPARATOR
                    + CharacterViewUtility.formatNumber(mesh.getInfluenceWeight(activeVertex, i)));
    }
}
