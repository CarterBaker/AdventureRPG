package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterRigRowBranch extends BranchPackage {

    /*
     * Lists the rows of the Rig panels. The left panel holds every bone of
     * the rig as it is being edited, set in under its parent and marked when
     * it was added since the editor started, the selected one highlighted;
     * the right panel holds the selected bone's name while it can still take
     * a new one, its parent, its bind position, rotation and size, the tools
     * that add, remove and mirror bones and symmetrize the rig, and the rig's
     * history and file. Every value shown is read through
     * CharacterRigPropertyBranch, the same path that edits it.
     */

    // Internal
    private CharacterRigBranch characterRigBranch;
    private CharacterRigPropertyBranch characterRigPropertyBranch;

    // Scratch
    private StringBuilder labelScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.labelScratch = new StringBuilder();
    }

    @Override
    protected void get() {
        this.characterRigBranch = get(CharacterRigBranch.class);
        this.characterRigPropertyBranch = get(CharacterRigPropertyBranch.class);
    }

    // List \\

    void addBoneRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_EDITOR_HEADER_BONES));

        for (int i = 0; i < characterRigBranch.getBoneCount(); i++)
            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.CHARACTER_RIG_KEY_BONE, i),
                    toBoneLabel(i),
                    i == characterRigBranch.getSelectedBone()));
    }

    // A bone's name set in one step per parent above it, marked when nothing loaded is laid out by it yet
    private String toBoneLabel(int boneIndex) {

        labelScratch.setLength(0);

        for (int parent = characterRigBranch.getParent(boneIndex); parent != EngineSetting.INDEX_NOT_FOUND;
                parent = characterRigBranch.getParent(parent))
            labelScratch.append(EditorSetting.CHARACTER_RIG_INDENT);

        labelScratch.append(characterRigBranch.getBoneName(boneIndex));

        if (!characterRigBranch.isLoaded(boneIndex))
            labelScratch.append(EditorSetting.CHARACTER_RIG_ADDED_MARKER);

        return labelScratch.toString();
    }

    // Properties \\

    void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        int boneIndex = characterRigBranch.getSelectedBone();

        if (!characterRigBranch.hasRig() || boneIndex == EngineSetting.INDEX_NOT_FOUND) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_RIG_HEADER_NO_RIG));
            return;
        }

        rows.add(CharacterViewRowStruct.header(characterRigBranch.getBoneName(boneIndex)));

        if (characterRigBranch.canRename(boneIndex))
            rows.add(CharacterViewRowStruct.text(
                    EditorSetting.CHARACTER_RIG_KEY_NAME,
                    EditorSetting.CHARACTER_RIG_LABEL_NAME,
                    characterRigPropertyBranch.getValueText(EditorSetting.CHARACTER_RIG_KEY_NAME)));

        if (boneIndex > 0)
            rows.add(CharacterViewRowStruct.choice(
                    EditorSetting.CHARACTER_RIG_KEY_PARENT,
                    EditorSetting.CHARACTER_RIG_LABEL_PARENT,
                    characterRigPropertyBranch.getValueText(EditorSetting.CHARACTER_RIG_KEY_PARENT)));

        addAxes(rows, EditorSetting.CHARACTER_RIG_KEY_POSITION, EditorSetting.CHARACTER_EDITOR_LABEL_POSITION);
        addAxes(rows, EditorSetting.CHARACTER_RIG_KEY_ROTATION, EditorSetting.CHARACTER_EDITOR_LABEL_ROTATION);
        addAxes(rows, EditorSetting.CHARACTER_RIG_KEY_SIZE, EditorSetting.CHARACTER_EDITOR_LABEL_SIZE);
        addStructureRows(rows, boneIndex);
        addFileRows(rows);
    }

    private void addStructureRows(ObjectArrayList<CharacterViewRowStruct> rows, int boneIndex) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_RIG_HEADER_STRUCTURE));
        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_ADD_BONE, EditorSetting.CHARACTER_RIG_LABEL_ADD_BONE);

        if (characterRigBranch.canRename(boneIndex))
            addAction(rows, EditorSetting.CHARACTER_RIG_KEY_REMOVE_BONE, EditorSetting.CHARACTER_RIG_LABEL_REMOVE_BONE);

        if (characterRigPropertyBranch.isSided(boneIndex))
            addAction(rows, EditorSetting.CHARACTER_RIG_KEY_MIRROR_BONE, EditorSetting.CHARACTER_RIG_LABEL_MIRROR_BONE);

        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_SYMMETRIZE_LEFT,
                EditorSetting.CHARACTER_RIG_LABEL_SYMMETRIZE_LEFT);
        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_SYMMETRIZE_RIGHT,
                EditorSetting.CHARACTER_RIG_LABEL_SYMMETRIZE_RIGHT);
    }

    private void addFileRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.CHARACTER_RIG_HEADER_FILE));
        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_UNDO, EditorSetting.CHARACTER_EDITOR_LABEL_UNDO);
        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_REDO, EditorSetting.CHARACTER_EDITOR_LABEL_REDO);
        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_SAVE, EditorSetting.CHARACTER_RIG_LABEL_SAVE);
        addAction(rows, EditorSetting.CHARACTER_RIG_KEY_REVERT, EditorSetting.CHARACTER_RIG_LABEL_REVERT);
    }

    // Utility \\

    private void addAxes(ObjectArrayList<CharacterViewRowStruct> rows, String keyName, String label) {

        for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_LABELS.length; axis++) {

            String key = CharacterViewUtility.toKey(keyName, axis);

            rows.add(CharacterViewRowStruct.number(
                    key,
                    label + EditorSetting.CHARACTER_VIEW_LABEL_SPACE + EditorSetting.CHARACTER_VIEW_AXIS_LABELS[axis],
                    characterRigPropertyBranch.getValueText(key)));
        }
    }

    private void addAction(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.action(key, label));
    }
}
