package editor.bootstrap.faceeditorpipeline.faceeditormanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class FacePropertyBranch extends BranchPackage {

    /*
     * The one place every value in the face editor's panels is read,
     * stepped, typed, cycled or acted on, by the key its row carries. A
     * control's name, group, morph, range, gender starts and bone targets are
     * read from the race's live ARPG and written through FaceEditBranch;
     * its preview value is the Character Editor's own slider, and the gender
     * row is the Character Editor's, so both tools preview one face. A range
     * always keeps its min below its max and its default between them, and a
     * control is never left driving nothing: its morph is cleared only while
     * it drives a bone, and its last bone is let go only while it has a
     * morph. A new control drives the face bone picked.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private FaceEditBranch faceEditBranch;
    private CharacterEditorManager characterEditorManager;
    private GenderManager genderManager;

    // Scratch
    private ObjectArrayList<String> morphOptions;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.morphOptions = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
        this.faceEditBranch = get(FaceEditBranch.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.genderManager = get(GenderManager.class);
    }

    // Selection \\

    void select(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_CONTROL))
            faceEditorManager.selectShape(index);
        else if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_BONE))
            faceEditorManager.selectBone(index);
    }

    // Values \\

    String getValueText(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);
        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_GENDER))
            return characterEditorManager.getValueText(key);

        if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_MIRROR))
            return faceEditorManager.isMirror() ? EditorSetting.CHARACTER_VIEW_YES : EditorSetting.CHARACTER_VIEW_NO;

        if (!faceEditBranch.hasShape(document, shapeIndex))
            return "";

        ArpgObjectStruct shape = faceEditBranch.getShapeArpg(document, shapeIndex);

        return switch (keyName) {
            case EditorSetting.FACE_EDITOR_KEY_NAME -> faceEditBranch.getShapeName(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_GROUP -> ArpgUtility.getString(
                    shape, EditorSetting.FACE_EDITOR_FIELD_GROUP, EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT);
            case EditorSetting.FACE_EDITOR_KEY_MORPH -> ArpgUtility.getString(
                    shape, EditorSetting.FACE_EDITOR_FIELD_MORPH, EditorSetting.CHARACTER_VIEW_NONE);
            default -> isNumberKey(keyName) && hasTarget(keyName, index)
                    ? CharacterViewUtility.formatNumber(getNumber(keyName, index))
                    : "";
        };
    }

    boolean isValueValid(String key, String text) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (!faceEditBranch.hasShape(faceEditorManager.getActiveDocument(), faceEditorManager.getSelectedShapeIndex()))
            return false;

        if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_NAME))
            return isNameAvailable(text.trim());

        return isNumberKey(keyName) && hasTarget(keyName, index) && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_NAME)) {
            faceEditBranch.renameShape(document, shapeIndex, text.trim());
            return;
        }

        writeNumber(keyName, CharacterViewUtility.getKeyIndex(key), CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (isNumberKey(keyName) && hasTarget(keyName, index))
            writeNumber(keyName, index, getNumber(keyName, index) + direction * getStep(keyName, index));
    }

    // A name no other control of the race has, and one a file could carry
    private boolean isNameAvailable(String shapeName) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int found = faceEditBranch.findShape(document, shapeName);

        return FileUtility.isValidFileName(shapeName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && (found == EngineSetting.INDEX_NOT_FOUND || found == faceEditorManager.getSelectedShapeIndex());
    }

    // Numbers \\

    private boolean isNumberKey(String keyName) {
        return keyName.equals(EditorSetting.FACE_EDITOR_KEY_MIN)
                || keyName.equals(EditorSetting.FACE_EDITOR_KEY_MAX)
                || keyName.equals(EditorSetting.FACE_EDITOR_KEY_DEFAULT)
                || keyName.equals(EditorSetting.FACE_EDITOR_KEY_PREVIEW)
                || keyName.equals(EditorSetting.FACE_EDITOR_KEY_START)
                || keyName.equals(EditorSetting.FACE_EDITOR_KEY_TARGET);
    }

    // A key names a value of the picked control that has to exist: a gender of the preview, or one of its targets
    private boolean hasTarget(String keyName, int index) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        if (!faceEditBranch.hasShape(document, shapeIndex))
            return false;

        return switch (keyName) {
            case EditorSetting.FACE_EDITOR_KEY_START -> index >= 0
                    && index < faceEditorManager.getPreviewData().getGenderCount();
            case EditorSetting.FACE_EDITOR_KEY_TARGET -> index >= 0
                    && index / EditorSetting.FACE_EDITOR_TARGET_STRIDE
                    < faceEditBranch.getTargetCount(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_PREVIEW -> shapeIndex < faceEditorManager.getAppearanceData()
                    .getShapeCount();
            default -> true;
        };
    }

    private float getNumber(String keyName, int index) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        return switch (keyName) {
            case EditorSetting.FACE_EDITOR_KEY_MIN -> faceEditBranch.getMin(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_MAX -> faceEditBranch.getMax(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_DEFAULT -> faceEditBranch.getDefault(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_PREVIEW -> faceEditorManager.getPreviewValue();
            case EditorSetting.FACE_EDITOR_KEY_START -> getStart(index);
            default -> faceEditBranch.getTargetComponent(
                    document,
                    shapeIndex,
                    index / EditorSetting.FACE_EDITOR_TARGET_STRIDE,
                    toChannel(index),
                    index % EditorSetting.FACE_EDITOR_AXES);
        };
    }

    private void writeNumber(String keyName, int index, float value) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();
        float min = faceEditBranch.getMin(document, shapeIndex);
        float max = faceEditBranch.getMax(document, shapeIndex);

        switch (keyName) {
            case EditorSetting.FACE_EDITOR_KEY_MIN -> setRangeEnd(document, shapeIndex, false, value);
            case EditorSetting.FACE_EDITOR_KEY_MAX -> setRangeEnd(document, shapeIndex, true, value);
            case EditorSetting.FACE_EDITOR_KEY_DEFAULT -> setDefault(document, shapeIndex, value);
            case EditorSetting.FACE_EDITOR_KEY_PREVIEW -> characterEditorManager.setShapeValue(
                    shapeIndex, CharacterViewUtility.round(CharacterViewUtility.clamp(value, min, max)));
            case EditorSetting.FACE_EDITOR_KEY_START -> faceEditBranch.setStart(
                    document,
                    getGenderName(index),
                    faceEditBranch.getShapeName(document, shapeIndex),
                    CharacterViewUtility.clamp(value, min, max));
            default -> faceEditBranch.setTargetComponent(
                    document,
                    shapeIndex,
                    index / EditorSetting.FACE_EDITOR_TARGET_STRIDE,
                    toChannel(index),
                    index % EditorSetting.FACE_EDITOR_AXES,
                    value);
        }
    }

    // One end of a control's range moved, kept a step from the other end, its default carried along inside it
    void setRangeEnd(CharacterDocumentInstance document, int shapeIndex, boolean upper, float value) {

        float min = faceEditBranch.getMin(document, shapeIndex);
        float max = faceEditBranch.getMax(document, shapeIndex);
        float defaultValue = faceEditBranch.getDefault(document, shapeIndex);
        float gap = EditorSetting.FACE_EDITOR_RANGE_STEP;

        if (upper) {
            float written = Math.max(value, min + gap);
            faceEditBranch.setRange(document, shapeIndex, min, written, Math.min(defaultValue, written));
            return;
        }

        float written = Math.min(value, max - gap);
        faceEditBranch.setRange(document, shapeIndex, written, max, Math.max(defaultValue, written));
    }

    // A control's default moved, held inside its range
    void setDefault(CharacterDocumentInstance document, int shapeIndex, float value) {

        float min = faceEditBranch.getMin(document, shapeIndex);
        float max = faceEditBranch.getMax(document, shapeIndex);

        faceEditBranch.setRange(document, shapeIndex, min, max, CharacterViewUtility.clamp(value, min, max));
    }

    private float getStep(String keyName, int index) {

        if (!keyName.equals(EditorSetting.FACE_EDITOR_KEY_TARGET))
            return EditorSetting.FACE_EDITOR_RANGE_STEP;

        return switch (toChannel(index)) {
            case EditorSetting.FACE_EDITOR_CHANNEL_MOVE -> EditorSetting.FACE_EDITOR_MOVE_STEP;
            case EditorSetting.FACE_EDITOR_CHANNEL_TURN -> EditorSetting.FACE_EDITOR_TURN_STEP;
            default -> EditorSetting.FACE_EDITOR_SCALE_STEP;
        };
    }

    private int toChannel(int index) {
        return index % EditorSetting.FACE_EDITOR_TARGET_STRIDE / EditorSetting.FACE_EDITOR_AXES;
    }

    // Where a gender starts the picked control: the value it writes, else the control's default
    private float getStart(int genderIndex) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();
        float start = faceEditBranch.getStart(getGenderName(genderIndex), faceEditBranch.getShapeName(document,
                shapeIndex));

        return Float.isNaN(start) ? faceEditBranch.getDefault(document, shapeIndex) : start;
    }

    // Genders \\

    String getGenderName(int genderIndex) {
        return genderManager.getGenderHandleFromGenderID(faceEditorManager.getPreviewData().getGenderID(genderIndex))
                .getGenderName();
    }

    String getGenderLabel(int genderIndex) {
        return genderManager.getGenderHandleFromGenderID(faceEditorManager.getPreviewData().getGenderID(genderIndex))
                .getDisplayName();
    }

    // Choices \\

    void cycle(String key, int direction) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        if (keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_GENDER)) {
            characterEditorManager.cycle(key, direction);
            return;
        }

        if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_MIRROR)) {
            faceEditorManager.setMirror(!faceEditorManager.isMirror());
            return;
        }

        if (!faceEditBranch.hasShape(document, shapeIndex))
            return;

        if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_GROUP))
            cycleGroup(document, shapeIndex, direction);
        else if (keyName.equals(EditorSetting.FACE_EDITOR_KEY_MORPH))
            cycleMorph(document, shapeIndex, direction);
    }

    // The group steps through the creator's groups; one not among them starts the list
    private void cycleGroup(CharacterDocumentInstance document, int shapeIndex, int direction) {

        String group = getValueText(EditorSetting.FACE_EDITOR_KEY_GROUP);
        String[] groups = EditorSetting.FACE_EDITOR_GROUPS;
        int current = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < groups.length; i++)
            if (groups[i].equals(group))
                current = i;

        int next = current == EngineSetting.INDEX_NOT_FOUND ? 0 : Math.floorMod(current + direction, groups.length);

        faceEditBranch.setField(document, shapeIndex, EditorSetting.FACE_EDITOR_FIELD_GROUP, groups[next]);
    }

    // The morph steps through the body's morphs, with none among them while the control drives a bone
    private void cycleMorph(CharacterDocumentInstance document, int shapeIndex, int direction) {

        collectMorphOptions(document, shapeIndex);

        if (morphOptions.isEmpty())
            return;

        String morphName = faceEditBranch.hasMorph(document, shapeIndex)
                ? faceEditBranch.getShapeArpg(document, shapeIndex)
                        .get(EditorSetting.FACE_EDITOR_FIELD_MORPH).getAsString()
                : null;
        int next = Math.floorMod(morphOptions.indexOf(morphName) + direction, morphOptions.size());

        faceEditBranch.setField(document, shapeIndex, EditorSetting.FACE_EDITOR_FIELD_MORPH, morphOptions.get(next));
    }

    // Every morph of the body the preview's gender wears, by name, after none when the control may drive no morph
    private void collectMorphOptions(CharacterDocumentInstance document, int shapeIndex) {

        morphOptions.clear();

        MeshHandle bodyMesh = faceEditorManager.getPreviewData()
                .getGender(characterEditorManager.getPreviewGenderID())
                .getMeshHandle();

        for (int m = 0; m < bodyMesh.getMorphCount(); m++)
            morphOptions.add(bodyMesh.getMorphName(m));

        morphOptions.sort(String.CASE_INSENSITIVE_ORDER);

        if (faceEditBranch.getTargetCount(document, shapeIndex) > 0)
            morphOptions.add(0, null);
    }

    // Actions \\

    void act(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        switch (keyName) {
            case EditorSetting.FACE_EDITOR_KEY_ADD_CONTROL -> addControl(document);
            case EditorSetting.FACE_EDITOR_KEY_RESET_PREVIEW -> characterEditorManager.resetShapeValues();
            default -> {

                if (!faceEditBranch.hasShape(document, shapeIndex)) {
                    faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_NO_CONTROL);
                    return;
                }

                actOnControl(document, shapeIndex, keyName, CharacterViewUtility.getKeyIndex(key));
            }
        }
    }

    private void actOnControl(CharacterDocumentInstance document, int shapeIndex, String keyName, int index) {
        switch (keyName) {
            case EditorSetting.FACE_EDITOR_KEY_DUPLICATE_CONTROL -> duplicateControl(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_REMOVE_CONTROL -> removeControl(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_ADD_TARGET -> addTarget(document, shapeIndex);
            case EditorSetting.FACE_EDITOR_KEY_REMOVE_TARGET -> removeTarget(document, shapeIndex, index);
            case EditorSetting.FACE_EDITOR_KEY_MIRROR_TARGETS -> mirrorTargets(document, shapeIndex);
            default -> {
            }
        }
    }

    // A new control driving the face bone picked, named with the first free number, in the bone's group if picked
    private void addControl(CharacterDocumentInstance document) {

        int boneIndex = faceEditorManager.getSelectedBoneIndex();

        if (!faceEditorManager.isFaceBone(boneIndex)) {
            faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_NOT_FACE_BONE);
            return;
        }

        String shapeName = resolveFreeName(EditorSetting.FACE_EDITOR_NEW_CONTROL_NAME);
        ArpgObjectStruct shape = new ArpgObjectStruct();

        shape.addProperty(EditorSetting.FACE_EDITOR_FIELD_NAME, shapeName);
        shape.addProperty(EditorSetting.FACE_EDITOR_FIELD_GROUP, EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT);

        int added = faceEditBranch.addShape(document, shape);

        faceEditBranch.addTarget(document, added, faceEditorManager.getBoneName(boneIndex));
        faceEditorManager.selectShape(added);
        faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_ADDED + shapeName);
    }

    private void duplicateControl(CharacterDocumentInstance document, int shapeIndex) {

        String shapeName = resolveFreeName(faceEditBranch.getShapeName(document, shapeIndex)
                + EditorSetting.FACE_EDITOR_COPY_SUFFIX);
        ArpgObjectStruct shape = faceEditBranch.getShapeArpg(document, shapeIndex).deepCopy();

        shape.remove(EditorSetting.FACE_EDITOR_FIELD_MORPH);
        shape.addProperty(EditorSetting.FACE_EDITOR_FIELD_NAME, shapeName);

        if (!ArpgUtility.hasArray(shape, EditorSetting.FACE_EDITOR_FIELD_BONES)) {
            faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_NOTHING_TO_COPY);
            return;
        }

        faceEditorManager.selectShape(faceEditBranch.addShape(document, shape));
        faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_ADDED + shapeName);
    }

    private void removeControl(CharacterDocumentInstance document, int shapeIndex) {

        String shapeName = faceEditBranch.getShapeName(document, shapeIndex);

        faceEditBranch.removeShape(document, shapeIndex);
        faceEditorManager.selectShape(Math.min(shapeIndex, faceEditBranch.getShapeCount(document) - 1));
        faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_REMOVED + shapeName);
    }

    private void addTarget(CharacterDocumentInstance document, int shapeIndex) {

        int boneIndex = faceEditorManager.getSelectedBoneIndex();

        if (!faceEditorManager.isFaceBone(boneIndex)) {
            faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_NOT_FACE_BONE);
            return;
        }

        String boneName = faceEditorManager.getBoneName(boneIndex);

        faceEditBranch.addTarget(document, shapeIndex, boneName);
        faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_DRIVES + boneName);
    }

    private void removeTarget(CharacterDocumentInstance document, int shapeIndex, int targetIndex) {

        if (canRemoveTarget(document, shapeIndex, targetIndex))
            faceEditBranch.removeTarget(document, shapeIndex, targetIndex);
    }

    // A control lets a bone go while it keeps another bone or a morph to drive
    boolean canRemoveTarget(CharacterDocumentInstance document, int shapeIndex, int targetIndex) {

        int count = faceEditBranch.getTargetCount(document, shapeIndex);

        return targetIndex >= 0 && targetIndex < count
                && (count > 1 || faceEditBranch.hasMorph(document, shapeIndex));
    }

    // The side of the picked bone written across onto the other; the left side when the picked bone sits on neither
    private void mirrorTargets(CharacterDocumentInstance document, int shapeIndex) {

        int boneIndex = faceEditorManager.getSelectedBoneIndex();
        boolean fromRight = faceEditorManager.isFaceBone(boneIndex)
                && faceEditorManager.getBoneName(boneIndex).endsWith(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT);

        String left = EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT;
        String right = EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT;

        faceEditBranch.mirrorTargets(document, shapeIndex, fromRight ? right : left, fromRight ? left : right);
        faceEditorManager.setStatusMessage(EditorSetting.FACE_EDITOR_MESSAGE_MIRRORED
                + faceEditBranch.getShapeName(document, shapeIndex));
    }

    // A name no control has, the base itself when free, else the base and the first free number
    private String resolveFreeName(String base) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();

        if (faceEditBranch.findShape(document, base) == EngineSetting.INDEX_NOT_FOUND
                && !base.equals(EditorSetting.FACE_EDITOR_NEW_CONTROL_NAME))
            return base;

        for (int number = 1;; number++)
            if (faceEditBranch.findShape(document, base + number) == EngineSetting.INDEX_NOT_FOUND)
                return base + number;
    }
}
