package editor.bootstrap.charactereditorpipeline.util;

public enum CharacterBoneDisplay {

    /*
     * How a Character Editor viewport draws the rig's bones, the Tools
     * panel's Bones option on the mesh, weights and rig canvases, in the
     * order EditorSetting.TOOL_BONE_DISPLAY_NAMES names them: a cube at each
     * joint, a stick from each joint to its parent's, both, or nothing, so
     * the bones never stand in the way of the vertices behind them.
     */

    JOINTS(true, false),
    STICKS(false, true),
    JOINTS_AND_STICKS(true, true),
    HIDDEN(false, false);

    // Values
    public static final CharacterBoneDisplay[] VALUES = values();

    // Internal
    private final boolean joints;
    private final boolean sticks;

    // Constructor \\

    CharacterBoneDisplay(boolean joints, boolean sticks) {
        this.joints = joints;
        this.sticks = sticks;
    }

    // Accessible \\

    // The display an option's value names, its whole step from the first
    public static CharacterBoneDisplay fromOption(float value) {
        return VALUES[Math.clamp(Math.round(value), 0, VALUES.length - 1)];
    }

    public boolean drawsJoints() {
        return joints;
    }

    public boolean drawsSticks() {
        return sticks;
    }
}
