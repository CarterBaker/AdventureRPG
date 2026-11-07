package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class CharacterRigPropertyBranch extends BranchPackage {

    /*
     * The one place every value of the Rig panels is read, stepped, typed,
     * cycled or acted on, by the key its row carries, for
     * CharacterPropertyBranch to hand those keys to. The bone rows pick a
     * bone; the name, parent, position, rotation and size are the selected
     * bone's; the actions add, remove and mirror bones, symmetrize the rig
     * and step, save and revert its history and file, all through
     * CharacterRigBranch. Nothing here acts while a joint is held in the
     * viewport.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return keyName.equals(EditorSetting.CHARACTER_RIG_KEY_BONE)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_NAME)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_PARENT)
                || toField(keyName) != null
                || isActionKey(keyName);
    }

    // The bone field a number row edits, null for a key that edits none
    private String toField(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_RIG_KEY_POSITION -> EditorSetting.CHARACTER_EDITOR_FIELD_POSITION;
            case EditorSetting.CHARACTER_RIG_KEY_ROTATION -> EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION;
            case EditorSetting.CHARACTER_RIG_KEY_SIZE -> EditorSetting.CHARACTER_EDITOR_FIELD_SIZE;
            default -> null;
        };
    }

    private boolean isActionKey(String keyName) {
        return switch (keyName) {
            case EditorSetting.CHARACTER_RIG_KEY_ADD_BONE,
                    EditorSetting.CHARACTER_RIG_KEY_REMOVE_BONE,
                    EditorSetting.CHARACTER_RIG_KEY_MIRROR_BONE,
                    EditorSetting.CHARACTER_RIG_KEY_SYMMETRIZE_LEFT,
                    EditorSetting.CHARACTER_RIG_KEY_SYMMETRIZE_RIGHT,
                    EditorSetting.CHARACTER_RIG_KEY_UNDO,
                    EditorSetting.CHARACTER_RIG_KEY_REDO,
                    EditorSetting.CHARACTER_RIG_KEY_SAVE,
                    EditorSetting.CHARACTER_RIG_KEY_REVERT,
                    EditorSetting.CHARACTER_RIG_KEY_DIGITS,
                    EditorSetting.CHARACTER_RIG_KEY_IK_ADD,
                    EditorSetting.CHARACTER_RIG_KEY_IK_REMOVE -> true;
            default -> false;
        };
    }

    // Selection \\

    void select(String key) {
        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_RIG_KEY_BONE) && isIdle())
            characterRigBranch.selectBone(CharacterViewUtility.getKeyIndex(key));
    }

    // Values \\

    String getValueText(String key) {

        int boneIndex = characterRigBranch.getSelectedBone();

        if (boneIndex == EngineSetting.INDEX_NOT_FOUND)
            return "";

        String keyName = CharacterViewUtility.getKeyName(key);
        String field = toField(keyName);

        if (field != null)
            return CharacterViewUtility.formatNumber(getNumber(field, CharacterViewUtility.getKeyIndex(key)));

        return switch (keyName) {
            case EditorSetting.CHARACTER_RIG_KEY_NAME -> characterRigBranch.getBoneName(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_PARENT -> characterRigBranch.getParent(boneIndex)
                    != EngineSetting.INDEX_NOT_FOUND
                            ? characterRigBranch.getBoneName(characterRigBranch.getParent(boneIndex))
                            : EditorSetting.CHARACTER_VIEW_NONE;
            default -> "";
        };
    }

    boolean isValueValid(String key, String text) {

        int boneIndex = characterRigBranch.getSelectedBone();
        String keyName = CharacterViewUtility.getKeyName(key);

        if (boneIndex == EngineSetting.INDEX_NOT_FOUND || !isIdle())
            return false;

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_NAME))
            return characterRigBranch.isBoneNameValid(boneIndex, text);

        return toField(keyName) != null && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {

        if (!isValueValid(key, text))
            return;

        int boneIndex = characterRigBranch.getSelectedBone();
        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_NAME))
            characterRigBranch.renameBone(boneIndex, text);
        else
            characterRigBranch.setComponent(
                    boneIndex, toField(keyName), CharacterViewUtility.getKeyIndex(key),
                    CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        int boneIndex = characterRigBranch.getSelectedBone();
        String field = toField(CharacterViewUtility.getKeyName(key));

        if (field == null || boneIndex == EngineSetting.INDEX_NOT_FOUND || !isIdle())
            return;

        int axis = CharacterViewUtility.getKeyIndex(key);

        characterRigBranch.setComponent(boneIndex, field, axis, getNumber(field, axis) + direction * getStep(field));
    }

    private float getNumber(String field, int axis) {

        int boneIndex = characterRigBranch.getSelectedBone();
        Vector3 vector = switch (field) {
            case EditorSetting.CHARACTER_EDITOR_FIELD_POSITION -> characterRigBranch.getPosition(boneIndex);
            case EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION -> characterRigBranch.getRotation(boneIndex);
            default -> characterRigBranch.getSize(boneIndex);
        };

        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    private float getStep(String field) {
        return switch (field) {
            case EditorSetting.CHARACTER_EDITOR_FIELD_POSITION -> EditorSetting.CHARACTER_EDITOR_POSITION_STEP;
            case EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION -> EditorSetting.CHARACTER_EDITOR_ROTATION_STEP;
            default -> EditorSetting.CHARACTER_EDITOR_SIZE_STEP;
        };
    }

    // Choices \\

    void cycle(String key, int direction) {

        int boneIndex = characterRigBranch.getSelectedBone();

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_RIG_KEY_PARENT)
                && boneIndex != EngineSetting.INDEX_NOT_FOUND && isIdle())
            characterRigBranch.cycleParent(boneIndex, direction);
    }

    // Actions \\

    void act(String key) {

        int boneIndex = characterRigBranch.getSelectedBone();
        String keyName = CharacterViewUtility.getKeyName(key);

        if (!characterRigBranch.hasRig() || !isIdle())
            return;

        switch (keyName) {
            case EditorSetting.CHARACTER_RIG_KEY_ADD_BONE -> characterRigBranch.addBone(null);
            case EditorSetting.CHARACTER_RIG_KEY_REMOVE_BONE -> characterRigBranch.removeBone(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_MIRROR_BONE -> characterRigBranch.mirrorBone(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_SYMMETRIZE_LEFT -> characterRigBranch.symmetrize(
                    EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT);
            case EditorSetting.CHARACTER_RIG_KEY_SYMMETRIZE_RIGHT -> characterRigBranch.symmetrize(
                    EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT);
            case EditorSetting.CHARACTER_RIG_KEY_UNDO -> characterEditorManager.undo();
            case EditorSetting.CHARACTER_RIG_KEY_REDO -> characterEditorManager.redo();
            case EditorSetting.CHARACTER_RIG_KEY_SAVE -> characterEditorManager.saveRig();
            case EditorSetting.CHARACTER_RIG_KEY_REVERT -> characterEditorManager.revertRig();
            case EditorSetting.CHARACTER_RIG_KEY_DIGITS -> characterEditorManager.rigDigits();
            case EditorSetting.CHARACTER_RIG_KEY_IK_ADD -> characterRigBranch.addIkChain(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_IK_REMOVE -> characterRigBranch.removeIkChain(boneIndex);
            default -> {
            }
        }
    }

    // Utility \\

    // True when the selected bone carries a side, so it has a twin across the body to mirror onto
    boolean isSided(int boneIndex) {
        return boneIndex > 0 && !CharacterMeshUtility.toMirrorBoneName(characterRigBranch.getBoneName(boneIndex))
                .equals(characterRigBranch.getBoneName(boneIndex));
    }

    private boolean isIdle() {
        return !characterRigBranch.isGesturing();
    }
}
