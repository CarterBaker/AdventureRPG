package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors and palettes, Build the build curve and the preview's
     * body, Equipment the anchors worn and held gear hangs from, Mesh the
     * vertices and faces of a worn part, Sculpt that part's surface under the
     * brush, Weights the bones each of its vertices follows, Paint the
     * texture maps it is coloured by, Rig the bones themselves, and Face the
     * controls that shape the face through its bones. Each mode that takes
     * viewport input is its own tool canvas; the mesh modes edit the part's
     * mesh file, Rig holds the bind pose so the character stands exactly as
     * its mesh and rig are authored, and those that show the vertices draw
     * them over the body. Each carries the hint the status bar shows while it
     * is open, and every other mode either stands in the one pose it names,
     * as Appearance stands idle the way the game's creator shows the player
     * and Face stands at rest, or lets its outliner pick the pose the preview
     * strikes — the mesh modes in the bind pose by default, their vertices
     * moved there alone. Each is a layer of the Character Editor: the modes
     * that take tools under every region that takes them, the rest under the
     * whole character.
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
            false,
            true,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_MESH_HINT),
    SCULPT(
            EditorSetting.CHARACTER_EDITOR_MODE_SCULPT,
            ToolCanvas.CHARACTER_SCULPT,
            true,
            false,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_SCULPT_HINT),
    WEIGHTS(EditorSetting.CHARACTER_EDITOR_MODE_WEIGHTS, ToolCanvas.CHARACTER_WEIGHTS, true, false, true, null, null),
    PAINT(
            EditorSetting.CHARACTER_EDITOR_MODE_PAINT,
            ToolCanvas.CHARACTER_PAINT,
            true,
            false,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_PAINT_HINT),
    RIG(
            EditorSetting.CHARACTER_EDITOR_MODE_RIG,
            ToolCanvas.CHARACTER_RIG,
            false,
            true,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_RIG_HINT),
    FACE(
            EditorSetting.CHARACTER_EDITOR_MODE_FACE,
            ToolCanvas.CHARACTER_FACE,
            false,
            false,
            false,
            CharacterEditorPose.REST,
            null);

    // Values
    public static final CharacterEditorMode[] VALUES = values();

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

    // True for the mode that paints the part's texture maps rather than its mesh, so no mesh stroke runs in it
    public boolean paintsMaps() {
        return toolCanvas == ToolCanvas.CHARACTER_PAINT;
    }

    // True for the mode that shapes the face's controls, worked through the face editor rather than the mesh or rig
    public boolean shapesFace() {
        return toolCanvas == ToolCanvas.CHARACTER_FACE;
    }

    // True for the mode that edits the rig, which draws the colliders its bones carry
    public boolean showsColliders() {
        return toolCanvas == ToolCanvas.CHARACTER_RIG;
    }

    // The tool canvas the viewport is while this mode is open, null for a mode that takes no tools
    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }

    // True for a mode that takes tools, listed under every region that takes it rather than the whole character's look
    public boolean isModeling() {
        return toolCanvas != null;
    }

    // Whether a region's layers list this mode: a mode that takes tools, the rig and the face only where it takes them
    public boolean isHeldBy(CharacterEditorRegion region) {
        return isModeling() && (toolCanvas != ToolCanvas.CHARACTER_RIG || region.takesRig())
                && (!shapesFace() || region.takesFace());
    }

    // What the status bar tells the pointer to do in this mode, null for a mode with nothing to say
    public String getHint() {
        return hint;
    }
}
