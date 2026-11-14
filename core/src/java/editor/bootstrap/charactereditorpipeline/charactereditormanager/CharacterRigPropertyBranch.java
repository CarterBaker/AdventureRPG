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
     * bone's, the stiffness, damping, gravity, drag and radius those of the
     * spring chain it belongs to with the colliders it rests against, and
     * the shape, radius, ends and chains those of the collider selected
     * among the bone's, through CharacterColliderBranch; the actions add,
     * remove and mirror bones, add and remove colliders,
     * symmetrize the rig, rig digits and hair locks, add and remove IK
     * handles and spring chains, switch the spring preview and step, save
     * and revert its history and file, all through CharacterRigBranch.
     * Nothing here acts while a joint is held in the viewport.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;
    private CharacterColliderBranch characterColliderBranch;

    // Base \\

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
        this.characterColliderBranch = get(CharacterColliderBranch.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return keyName.equals(EditorSetting.CHARACTER_RIG_KEY_BONE)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_NAME)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_PARENT)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING)
                || isColliderKey(keyName)
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

    private boolean isColliderKey(String keyName) {
        return keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_VALUE)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_SHAPE)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_CHAINS)
                || keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING_COLLIDER);
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
                    EditorSetting.CHARACTER_RIG_KEY_IK_REMOVE,
                    EditorSetting.CHARACTER_RIG_KEY_HAIR,
                    EditorSetting.CHARACTER_RIG_KEY_SPRING_ADD,
                    EditorSetting.CHARACTER_RIG_KEY_SPRING_REMOVE,
                    EditorSetting.CHARACTER_RIG_KEY_SPRING_PREVIEW,
                    EditorSetting.CHARACTER_RIG_KEY_COLLIDER_ADD,
                    EditorSetting.CHARACTER_RIG_KEY_COLLIDER_REMOVE -> true;
            default -> false;
        };
    }

    // Selection \\

    void select(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (!isIdle())
            return;

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_BONE))
            characterRigBranch.selectBone(index);
        else if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER) && isCollider(index))
            characterColliderBranch.selectCollider(index);
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

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING))
            return getSpringChain() != EngineSetting.INDEX_NOT_FOUND
                    ? CharacterViewUtility.formatNumber(characterRigBranch.getSpringValue(
                            getSpringChain(), CharacterViewUtility.getKeyIndex(key)))
                    : "";

        if (isColliderKey(keyName))
            return getColliderText(keyName, CharacterViewUtility.getKeyIndex(key));

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

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING))
            return getSpringChain() != EngineSetting.INDEX_NOT_FOUND && CharacterViewUtility.isNumber(text);

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_VALUE))
            return isCollider(toColliderIndex(CharacterViewUtility.getKeyIndex(key)))
                    && CharacterViewUtility.isNumber(text);

        return toField(keyName) != null && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {

        if (!isValueValid(key, text))
            return;

        int boneIndex = characterRigBranch.getSelectedBone();
        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_NAME))
            characterRigBranch.renameBone(boneIndex, text);
        else if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING))
            characterRigBranch.setSpringValue(
                    getSpringChain(), CharacterViewUtility.getKeyIndex(key), CharacterViewUtility.parseNumber(text));
        else if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_VALUE))
            characterColliderBranch.setValue(
                    toColliderIndex(CharacterViewUtility.getKeyIndex(key)),
                    toColliderField(CharacterViewUtility.getKeyIndex(key)),
                    CharacterViewUtility.parseNumber(text));
        else
            characterRigBranch.setComponent(
                    boneIndex, toField(keyName), CharacterViewUtility.getKeyIndex(key),
                    CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        int boneIndex = characterRigBranch.getSelectedBone();
        String field = toField(CharacterViewUtility.getKeyName(key));

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_RIG_KEY_SPRING)) {
            stepSpring(CharacterViewUtility.getKeyIndex(key), direction);
            return;
        }

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_VALUE)) {
            stepCollider(CharacterViewUtility.getKeyIndex(key), direction);
            return;
        }

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

    // A spring setting of the selected bone's chain moved by its own step
    private void stepSpring(int springField, int direction) {

        int chainIndex = getSpringChain();

        if (chainIndex == EngineSetting.INDEX_NOT_FOUND || !isIdle())
            return;

        characterRigBranch.setSpringValue(
                chainIndex,
                springField,
                characterRigBranch.getSpringValue(chainIndex, springField)
                        + direction * EditorSetting.CHARACTER_RIG_SPRING_STEPS[springField]);
    }

    // The spring chain the selected bone belongs to, INDEX_NOT_FOUND when none
    int getSpringChain() {
        return characterRigBranch.findSpringChain(characterRigBranch.getSelectedBone());
    }

    // Colliders \\

    // A value of the selected collider moved by its own step: the radius by the radius step, an end by the point step
    private void stepCollider(int index, int direction) {

        int colliderIndex = toColliderIndex(index);
        int field = toColliderField(index);

        if (!isCollider(colliderIndex) || !isIdle())
            return;

        characterColliderBranch.setValue(
                colliderIndex,
                field,
                characterColliderBranch.getValue(colliderIndex, field) + direction
                        * (field == EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_RADIUS
                                ? EditorSetting.CHARACTER_RIG_COLLIDER_RADIUS_STEP
                                : EditorSetting.CHARACTER_RIG_COLLIDER_POINT_STEP));
    }

    private String getColliderText(String keyName, int index) {

        int chainIndex = getSpringChain();

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING_COLLIDER))
            return isCollider(index) && chainIndex != EngineSetting.INDEX_NOT_FOUND
                    ? toYesNo(characterColliderBranch.isSpringCollider(chainIndex, index))
                    : "";

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_VALUE))
            return isCollider(toColliderIndex(index))
                    ? CharacterViewUtility.formatNumber(
                            characterColliderBranch.getValue(toColliderIndex(index), toColliderField(index)))
                    : "";

        if (!isCollider(index))
            return "";

        return switch (keyName) {
            case EditorSetting.CHARACTER_RIG_KEY_COLLIDER_SHAPE -> EditorSetting.CHARACTER_RIG_COLLIDER_SHAPE_LABELS[
                    characterColliderBranch.isCapsule(index) ? 1 : 0];
            case EditorSetting.CHARACTER_RIG_KEY_COLLIDER_CHAINS -> toChainsText(index);
            default -> characterColliderBranch.getColliderName(index);
        };
    }

    // How many spring chains rest against a collider: all, none, or so many of all
    private String toChainsText(int colliderIndex) {

        int count = characterColliderBranch.countChains(colliderIndex);
        int all = characterColliderBranch.countAllChains();

        if (count == 0)
            return EditorSetting.CHARACTER_VIEW_NONE;

        return count == all
                ? EditorSetting.CHARACTER_RIG_LABEL_CHAINS_ALL
                : count + EditorSetting.CHARACTER_RIG_LABEL_CHAINS_SOME + all;
    }

    private boolean isCollider(int colliderIndex) {
        return colliderIndex >= 0 && colliderIndex < characterColliderBranch.getColliderCount();
    }

    private int toColliderIndex(int index) {
        return index < 0 ? EngineSetting.INDEX_NOT_FOUND : index / EditorSetting.CHARACTER_RIG_COLLIDER_STRIDE;
    }

    private int toColliderField(int index) {
        return index % EditorSetting.CHARACTER_RIG_COLLIDER_STRIDE;
    }

    private String toYesNo(boolean value) {
        return value ? EditorSetting.CHARACTER_VIEW_YES : EditorSetting.CHARACTER_VIEW_NO;
    }

    // Choices \\

    void cycle(String key, int direction) {

        int boneIndex = characterRigBranch.getSelectedBone();
        String keyName = CharacterViewUtility.getKeyName(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        if (boneIndex == EngineSetting.INDEX_NOT_FOUND || !isIdle())
            return;

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_PARENT))
            characterRigBranch.cycleParent(boneIndex, direction);
        else if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_SHAPE) && isCollider(index))
            characterColliderBranch.cycleShape(index);
        else if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_COLLIDER_CHAINS) && isCollider(index))
            characterColliderBranch.setChains(index, direction > 0);
        else if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING_COLLIDER) && isCollider(index)
                && getSpringChain() != EngineSetting.INDEX_NOT_FOUND)
            characterColliderBranch.toggleSpringCollider(getSpringChain(), index);
    }

    // Actions \\

    void act(String key) {

        int boneIndex = characterRigBranch.getSelectedBone();
        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.CHARACTER_RIG_KEY_SPRING_PREVIEW)) {
            characterEditorManager.toggleSpringPreview();
            return;
        }

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
            case EditorSetting.CHARACTER_RIG_KEY_HAIR -> characterEditorManager.rigHairLocks();
            case EditorSetting.CHARACTER_RIG_KEY_SPRING_ADD -> characterRigBranch.addSpringChain(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_SPRING_REMOVE -> characterRigBranch.removeSpringChain(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_COLLIDER_ADD -> characterColliderBranch.addCollider(boneIndex);
            case EditorSetting.CHARACTER_RIG_KEY_COLLIDER_REMOVE -> characterColliderBranch.removeCollider();
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
