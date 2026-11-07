package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors and palettes, Build the build curve and the preview's
     * body, Equipment the anchors worn and held gear hangs from, Mesh the
     * vertices and faces of a worn part, Sculpt that part's surface under the
     * brush, Weights the bones each of its vertices follows, and Rig the
     * bones themselves. Each mode that takes viewport input is its own tool
     * canvas; the mesh modes edit the part's mesh file, the ones that hold
     * the bind pose show the character exactly as its mesh and rig are
     * authored, and those that show the vertices draw them over the body.
     * Each carries the hint the status bar shows while it is open, and a
     * posed mode either stands in the one pose it names, as Appearance stands
     * idle the way the game's creator shows the player, or lets its outliner
     * pick the pose the preview strikes. The modes
     * that take tools are worked in the Character Mesh Editor, the rest in
     * the Character Editor, each window keeping to its own.
     */

    APPEARANCE(
            EditorSetting.CHARACTER_EDITOR_MODE_APPEARANCE,
            null,
            false,
            false,
            false,
            CharacterEditorPose.IDLE,
            null),
    BUILD(EditorSetting.CHARACTER_EDITOR_MODE_BUILD, null, false, false, false, null, null),
    EQUIPMENT(EditorSetting.CHARACTER_EDITOR_MODE_EQUIPMENT, null, false, false, false, null, null),
    MESH(
            EditorSetting.CHARACTER_EDITOR_MODE_MESH,
            ToolCanvas.CHARACTER_MESH,
            true,
            true,
            true,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_MESH_HINT),
    SCULPT(
            EditorSetting.CHARACTER_EDITOR_MODE_SCULPT,
            ToolCanvas.CHARACTER_SCULPT,
            true,
            true,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_SCULPT_HINT),
    WEIGHTS(EditorSetting.CHARACTER_EDITOR_MODE_WEIGHTS, ToolCanvas.CHARACTER_WEIGHTS, true, false, true, null, null),
    RIG(
            EditorSetting.CHARACTER_EDITOR_MODE_RIG,
            ToolCanvas.CHARACTER_RIG,
            false,
            true,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_RIG_HINT);

    // Internal
    private final String label;
    private final ToolCanvas toolCanvas;
    private final boolean meshMode;
    private final boolean bindPose;
    private final boolean vertexOverlay;
    private final CharacterEditorPose standingPose;
    private final String hint;

    // Constructor \\

    CharacterEditorMode(
            String label,
            ToolCanvas toolCanvas,
            boolean meshMode,
            boolean bindPose,
            boolean vertexOverlay,
            CharacterEditorPose standingPose,
            String hint) {
        this.label = label;
        this.toolCanvas = toolCanvas;
        this.meshMode = meshMode;
        this.bindPose = bindPose;
        this.vertexOverlay = vertexOverlay;
        this.standingPose = standingPose;
        this.hint = hint;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isMeshMode() {
        return meshMode;
    }

    // True for a mode that shows the character in the exact bind pose, so it lists no pose to play
    public boolean holdsBindPose() {
        return bindPose;
    }

    // The one pose a posed mode's preview stands in, null for one whose outliner picks the pose
    public CharacterEditorPose getStandingPose() {
        return standingPose;
    }

    // True for a posed mode whose outliner lists the pose the preview strikes
    public boolean picksPose() {
        return !bindPose && standingPose == null;
    }

    public boolean showsVertices() {
        return vertexOverlay;
    }

    // The tool canvas the viewport is while this mode is open, null for a mode that takes no tools
    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }

    // True for a mode worked in the Character Mesh Editor, the modes that take tools
    public boolean isModeling() {
        return toolCanvas != null;
    }

    // What the status bar tells the pointer to do in this mode, null for a mode with nothing to say
    public String getHint() {
        return hint;
    }
}
