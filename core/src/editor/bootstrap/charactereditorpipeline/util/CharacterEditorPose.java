package editor.bootstrap.charactereditorpipeline.util;

import application.bootstrap.entitypipeline.entity.EntityState;
import editor.runtime.EditorSetting;

public enum CharacterEditorPose {

    /*
     * How the character editor's preview stands: Rest holds the bind pose,
     * and the others play the template's own animation tree in a movement
     * state at a speed, so a look can be judged in motion.
     */

    REST(EditorSetting.CHARACTER_EDITOR_POSE_REST, null, 0f),
    IDLE(EditorSetting.CHARACTER_EDITOR_POSE_IDLE, EntityState.IDLE, 0f),
    WALK(EditorSetting.CHARACTER_EDITOR_POSE_WALK, EntityState.WALKING, EditorSetting.CHARACTER_EDITOR_WALK_SPEED),
    RUN(EditorSetting.CHARACTER_EDITOR_POSE_RUN, EntityState.RUNNING, EditorSetting.CHARACTER_EDITOR_RUN_SPEED);

    // Values
    public static final CharacterEditorPose[] VALUES = values();

    // Internal
    private final String label;
    private final EntityState movementState;
    private final float speed;

    // Constructor \\

    CharacterEditorPose(String label, EntityState movementState, float speed) {
        this.label = label;
        this.movementState = movementState;
        this.speed = speed;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isPlayed() {
        return movementState != null;
    }

    public EntityState getMovementState() {
        return movementState;
    }

    public float getSpeed() {
        return speed;
    }
}
