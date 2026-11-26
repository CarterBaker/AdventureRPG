package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterPosePropertyBranch extends BranchPackage {

    /*
     * The one place the Pose tab's values are listed, read, stepped and
     * typed, by the key its row carries, for CharacterPropertyBranch to hand
     * those keys to: the bone picked, then its turn in the edit pose about
     * each of its axes in degrees, as a modeller's pose mode lists the
     * rotation of the bone selected. Every change goes through
     * CharacterEditorManager.setEditPoseTurn, the path the Pose tool's drag
     * takes, so the twin follows while mirroring and every modelling tab
     * shows the pose at once.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;

    // Scratch
    private Vector3 turnScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.turnScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    // Keys \\

    boolean handles(String keyName) {
        return keyName.equals(EditorSetting.CHARACTER_EDITOR_KEY_POSE_TURN);
    }

    // Rows \\

    // The bone picked, then its turn about each axis
    void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (!hasBone())
            return;

        rows.add(CharacterViewRowStruct.header(characterEditorManager.getSelectedBoneName()));

        for (int axis = 0; axis < EditorSetting.CHARACTER_EDITOR_POSE_TURN_LABELS.length; axis++) {

            String key = CharacterViewUtility.toKey(EditorSetting.CHARACTER_EDITOR_KEY_POSE_TURN, axis);

            rows.add(CharacterViewRowStruct.number(
                    key,
                    EditorSetting.CHARACTER_EDITOR_POSE_TURN_LABELS[axis],
                    getValueText(key)));
        }
    }

    // Values \\

    String getValueText(String key) {
        return hasBone() ? CharacterViewUtility.formatNumber(getTurn(key)) : "";
    }

    boolean isValueValid(String key, String text) {
        return hasBone() && CharacterViewUtility.isNumber(text);
    }

    void setValue(String key, String text) {

        if (isValueValid(key, text))
            writeTurn(key, CharacterViewUtility.parseNumber(text));
    }

    void step(String key, int direction) {

        if (hasBone())
            writeTurn(key, getTurn(key) + direction * EditorSetting.CHARACTER_EDITOR_POSE_TURN_STEP);
    }

    // Turn \\

    private boolean hasBone() {
        return !characterEditorManager.getSelectedBoneName().isEmpty();
    }

    private float getTurn(String key) {

        Vector3 turn = characterEditorManager.getEditPoseTurn(characterEditorManager.getSelectedBoneIndex());

        return switch (CharacterViewUtility.getKeyIndex(key)) {
            case EngineSetting.AXIS_X -> turn.x;
            case EngineSetting.AXIS_Y -> turn.y;
            default -> turn.z;
        };
    }

    // The bone picked turned to the value on one axis, its other axes kept
    private void writeTurn(String key, float value) {

        int boneIndex = characterEditorManager.getSelectedBoneIndex();

        turnScratch.set(characterEditorManager.getEditPoseTurn(boneIndex));

        switch (CharacterViewUtility.getKeyIndex(key)) {
            case EngineSetting.AXIS_X -> turnScratch.x = value;
            case EngineSetting.AXIS_Y -> turnScratch.y = value;
            default -> turnScratch.z = value;
        }

        characterEditorManager.setEditPoseTurn(boneIndex, turnScratch);
    }
}
