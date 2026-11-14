package editor.bootstrap.faceeditorpipeline.faceeditormanager;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.characterdocument.CharacterDocumentInstance;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class FaceRowBranch extends BranchPackage {

    /*
     * Lists the rows of the face editor's two panels. The Face Outliner holds
     * how the preview is made — its gender, when the race has more than
     * one, and whether edits mirror across the face — then every face
     * control under the group a creator lists it in, groups in the order the
     * race first names them, and every face bone to pick. Face
     * Properties holds the picked control: its name, group, morph and
     * preview value, its range, where each gender starts it, and every bone
     * it drives with its move, turn and scale per unit, each with the button
     * that lets it go. The control's own operations are tool actions, listed
     * in the Tools window under the face's tools. Every value shown is read
     * through FacePropertyBranch, the same path that edits it.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private FaceEditBranch faceEditBranch;
    private FacePropertyBranch facePropertyBranch;

    // Scratch
    private ObjectArrayList<String> groupNames;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.groupNames = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
        this.faceEditBranch = get(FaceEditBranch.class);
        this.facePropertyBranch = get(FacePropertyBranch.class);
    }

    // List \\

    void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_PREVIEW));

        if (faceEditorManager.getPreviewData().getGenderCount() > 1)
            addChoice(rows, EditorSetting.CHARACTER_EDITOR_KEY_GENDER, EditorSetting.CHARACTER_EDITOR_LABEL_GENDER);

        addChoice(rows, EditorSetting.FACE_EDITOR_KEY_MIRROR, EditorSetting.FACE_EDITOR_LABEL_MIRROR);

        if (faceEditBranch.getAppearanceArpg() == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_NO_APPEARANCE));
            return;
        }

        addControlRows(rows);
        addBoneRows(rows);
    }

    // Each group a header, its controls under it in the race's order
    private void addControlRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int count = faceEditBranch.getShapeCount(document);

        groupNames.clear();

        for (int i = 0; i < count; i++) {

            String groupName = getGroupName(document, i);

            if (!groupNames.contains(groupName))
                groupNames.add(groupName);
        }

        for (int g = 0; g < groupNames.size(); g++) {

            rows.add(CharacterViewRowStruct.header(groupNames.get(g)));

            for (int i = 0; i < count; i++)
                if (getGroupName(document, i).equals(groupNames.get(g)))
                    rows.add(CharacterViewRowStruct.item(
                            CharacterViewUtility.toKey(EditorSetting.FACE_EDITOR_KEY_CONTROL, i),
                            faceEditBranch.getShapeName(document, i),
                            i == faceEditorManager.getSelectedShapeIndex()));
        }
    }

    private void addBoneRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        RigHandle rigHandle = faceEditorManager.getRigHandle();

        rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_FACE_BONES));

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            if (faceEditorManager.isFaceBone(i))
                rows.add(CharacterViewRowStruct.item(
                        CharacterViewUtility.toKey(EditorSetting.FACE_EDITOR_KEY_BONE, i),
                        rigHandle.getBone(i).getName(),
                        i == faceEditorManager.getSelectedBoneIndex()));
    }

    private String getGroupName(CharacterDocumentInstance document, int shapeIndex) {
        return ArpgUtility.getString(
                faceEditBranch.getShapeArpg(document, shapeIndex),
                EditorSetting.FACE_EDITOR_FIELD_GROUP,
                EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT);
    }

    // Properties \\

    void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterDocumentInstance document = faceEditorManager.getActiveDocument();
        int shapeIndex = faceEditorManager.getSelectedShapeIndex();

        if (!faceEditBranch.hasShape(document, shapeIndex)) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_NO_CONTROL));
            return;
        }

        rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_CONTROL));
        rows.add(CharacterViewRowStruct.text(
                EditorSetting.FACE_EDITOR_KEY_NAME,
                EditorSetting.FACE_EDITOR_LABEL_NAME,
                facePropertyBranch.getValueText(EditorSetting.FACE_EDITOR_KEY_NAME)));
        addChoice(rows, EditorSetting.FACE_EDITOR_KEY_GROUP, EditorSetting.FACE_EDITOR_LABEL_GROUP);
        addChoice(rows, EditorSetting.FACE_EDITOR_KEY_MORPH, EditorSetting.FACE_EDITOR_LABEL_MORPH);
        addNumber(rows, EditorSetting.FACE_EDITOR_KEY_PREVIEW, EditorSetting.FACE_EDITOR_LABEL_PREVIEW);

        rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_RANGE));
        addNumber(rows, EditorSetting.FACE_EDITOR_KEY_MIN, EditorSetting.FACE_EDITOR_LABEL_MIN);
        addNumber(rows, EditorSetting.FACE_EDITOR_KEY_MAX, EditorSetting.FACE_EDITOR_LABEL_MAX);
        addNumber(rows, EditorSetting.FACE_EDITOR_KEY_DEFAULT, EditorSetting.FACE_EDITOR_LABEL_DEFAULT);

        addStartRows(rows);
        addTargetRows(document, shapeIndex, rows);
    }

    private void addStartRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        int genderCount = faceEditorManager.getPreviewData().getGenderCount();

        if (genderCount == 0)
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_STARTS));

        for (int i = 0; i < genderCount; i++)
            addNumber(rows, CharacterViewUtility.toKey(EditorSetting.FACE_EDITOR_KEY_START, i),
                    facePropertyBranch.getGenderLabel(i));
    }

    // Every bone driven, its name a header over its move, turn and scale on each axis
    private void addTargetRows(
            CharacterDocumentInstance document,
            int shapeIndex,
            ObjectArrayList<CharacterViewRowStruct> rows) {

        int count = faceEditBranch.getTargetCount(document, shapeIndex);

        rows.add(CharacterViewRowStruct.header(EditorSetting.FACE_EDITOR_HEADER_TARGETS));

        for (int t = 0; t < count; t++) {

            rows.add(CharacterViewRowStruct.header(faceEditBranch.getTargetBoneName(document, shapeIndex, t)));

            for (int channel = 0; channel < EditorSetting.FACE_EDITOR_CHANNEL_LABELS.length; channel++)
                for (int axis = 0; axis < EditorSetting.FACE_EDITOR_AXES; axis++)
                    addNumber(rows,
                            CharacterViewUtility.toKey(
                                    EditorSetting.FACE_EDITOR_KEY_TARGET,
                                    t * EditorSetting.FACE_EDITOR_TARGET_STRIDE
                                            + channel * EditorSetting.FACE_EDITOR_AXES + axis),
                            EditorSetting.FACE_EDITOR_CHANNEL_LABELS[channel]
                                    + EditorSetting.CHARACTER_VIEW_LABEL_SPACE
                                    + EditorSetting.CHARACTER_VIEW_AXIS_LABELS[axis]);

            if (facePropertyBranch.canRemoveTarget(document, shapeIndex, t))
                rows.add(CharacterViewRowStruct.action(
                        CharacterViewUtility.toKey(EditorSetting.FACE_EDITOR_KEY_REMOVE_TARGET, t),
                        EditorSetting.FACE_EDITOR_LABEL_REMOVE_TARGET));
        }
    }

    // Utility \\

    private void addNumber(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.number(key, label, facePropertyBranch.getValueText(key)));
    }

    private void addChoice(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.choice(key, label, facePropertyBranch.getValueText(key)));
    }
}
