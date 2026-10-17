package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.animationeditorpipeline.util.AnimationEditorChannel;
import editor.characterview.panel.CharacterViewRowStruct;
import editor.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationRowBranch extends BranchPackage {

    /*
     * Lists the rows of the animation editor's two panels. The left panel
     * holds the clip's character and looping and every bone of its rig, each
     * with how many keys it has; the right panel the selected bone at the
     * playhead — its key there or the way to make one, its rotation,
     * position and scale — and the pose tools that work on every bone at
     * once. Every value shown is read through AnimationPropertyBranch, the
     * same path that edits it.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
    private AnimationPropertyBranch animationPropertyBranch;

    // Base \\

    @Override
    protected void get() {
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.animationPropertyBranch = get(AnimationPropertyBranch.class);
    }

    // List \\

    void buildListRows(AnimationDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_EDITOR_HEADER_CLIP));
        addChoice(document, rows, EditorSetting.ANIMATION_EDITOR_KEY_CHARACTER,
                EditorSetting.ANIMATION_EDITOR_LABEL_CHARACTER);
        addChoice(document, rows, EditorSetting.ANIMATION_EDITOR_KEY_LOOP, EditorSetting.ANIMATION_EDITOR_LABEL_LOOP);

        RigHandle rigHandle = document.getRigHandle();

        if (rigHandle == null)
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_EDITOR_HEADER_BONES));

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            String boneName = rigHandle.getBone(i).getName();
            int keyCount = animationPropertyBranch.getKeyCount(boneName);
            String label = keyCount > 0
                    ? boneName + EditorSetting.ANIMATION_EDITOR_KEY_COUNT_OPEN + keyCount
                            + EditorSetting.ANIMATION_EDITOR_KEY_COUNT_CLOSE
                    : boneName;

            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.ANIMATION_EDITOR_KEY_BONE, i),
                    label,
                    i == animationEditorManager.getSelectedBoneIndex()));
        }
    }

    // Properties \\

    void buildPropertyRows(AnimationDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        RigHandle rigHandle = document.getRigHandle();

        if (rigHandle == null) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_EDITOR_STATUS_NO_RIG));
            return;
        }

        int boneIndex = animationEditorManager.getSelectedBoneIndex();

        rows.add(CharacterViewRowStruct.header(rigHandle.getBone(boneIndex).getName()));
        addKeyRows(document, rows, boneIndex);

        for (AnimationEditorChannel channel : AnimationEditorChannel.VALUES) {

            rows.add(CharacterViewRowStruct.header(channel.getLabel()));

            for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_LABELS.length; axis++) {

                String key = CharacterViewUtility.toKey(channel.getField(), axis);

                rows.add(CharacterViewRowStruct.number(
                        key,
                        EditorSetting.CHARACTER_VIEW_AXIS_LABELS[axis],
                        animationPropertyBranch.getValueText(document, key)));
            }
        }

        addPoseRows(rigHandle, rows);
    }

    private void addKeyRows(
            AnimationDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            int boneIndex) {

        int keyIndex = animationPropertyBranch.findKeyIndexAt(document, boneIndex);
        String time = CharacterViewUtility.formatNumber(animationEditorManager.getTime())
                + EditorSetting.ANIMATION_EDITOR_TIME_SUFFIX;

        if (keyIndex == EngineSetting.INDEX_NOT_FOUND) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_EDITOR_HEADER_NO_KEY + time));
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ANIMATION_EDITOR_KEY_ADD_KEY,
                    EditorSetting.ANIMATION_EDITOR_LABEL_ADD_KEY));
        } else {
            rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_EDITOR_HEADER_KEY + (keyIndex + 1)
                    + EditorSetting.CHARACTER_VIEW_COUNT_SEPARATOR
                    + animationPropertyBranch.getKeyCount(document.getRigHandle().getBone(boneIndex).getName())
                    + EditorSetting.ANIMATION_EDITOR_KEY_AT + time));
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ANIMATION_EDITOR_KEY_DELETE_KEY,
                    EditorSetting.ANIMATION_EDITOR_LABEL_DELETE_KEY));
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ANIMATION_EDITOR_KEY_KEY_EARLIER,
                    EditorSetting.ANIMATION_EDITOR_LABEL_KEY_EARLIER));
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ANIMATION_EDITOR_KEY_KEY_LATER,
                    EditorSetting.ANIMATION_EDITOR_LABEL_KEY_LATER));
        }

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ANIMATION_EDITOR_KEY_PREVIOUS_KEY,
                EditorSetting.ANIMATION_EDITOR_LABEL_PREVIOUS_KEY));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ANIMATION_EDITOR_KEY_NEXT_KEY,
                EditorSetting.ANIMATION_EDITOR_LABEL_NEXT_KEY));
    }

    private void addPoseRows(RigHandle rigHandle, ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_EDITOR_HEADER_POSE));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ANIMATION_EDITOR_KEY_KEY_POSE,
                EditorSetting.ANIMATION_EDITOR_LABEL_KEY_POSE));
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ANIMATION_EDITOR_KEY_COPY_POSE,
                EditorSetting.ANIMATION_EDITOR_LABEL_COPY_POSE));

        if (animationEditorManager.hasClipboard(rigHandle))
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ANIMATION_EDITOR_KEY_PASTE_POSE,
                    EditorSetting.ANIMATION_EDITOR_LABEL_PASTE_POSE));

        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ANIMATION_EDITOR_KEY_MIRROR_POSE,
                EditorSetting.ANIMATION_EDITOR_LABEL_MIRROR_POSE));
    }

    // Utility \\

    private void addChoice(
            AnimationDocumentInstance document,
            ObjectArrayList<CharacterViewRowStruct> rows,
            String key,
            String label) {

        rows.add(CharacterViewRowStruct.choice(key, label, animationPropertyBranch.getValueText(document, key)));
    }
}
