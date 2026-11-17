package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors, palettes, every slider the creator offers and the
     * build, Equipment the anchors worn and held gear hangs from, Mesh the
     * vertices and faces of a worn part, Sculpt that part's surface under the
     * brush, Weights the bones each of its vertices follows, Paint the
     * texture maps it is coloured by, Rig the bones themselves and the
     * slider channels they offer, and UV the seams that cut the part's
     * surface and the faces unwrapped flat, their UVs laid out in the UV
     * Editor beside it. Sculpt's Brush also places the body part chosen in
     * the Stamps tab, its bones grafted onto the rig and its mesh merged into
     * the body. Each mode that takes viewport input is its own tool
     * canvas; the mesh modes edit the part's mesh file, Rig holds the bind
     * pose so the character stands exactly as its mesh and rig are authored,
     * and those that show the vertices draw them over the body. Each carries
     * the hint the status bar shows while it is open, and every other mode
     * either stands in the one pose it names, as Appearance stands idle the
     * way the game's creator shows the player, or lets its outliner pick the
     * pose the preview strikes — the mesh modes in the bind pose by default,
     * their vertices moved there alone. Each is its own character tool tab;
     * Mesh, Sculpt, Weights, Rig and UV list the regions as their layers,
     * Paint the paint layers of the map painted, and the rest work on the
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
    UV(
            EditorSetting.CHARACTER_EDITOR_MODE_UV,
            ToolCanvas.CHARACTER_UV,
            true,
            false,
            true,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_UV_HINT);

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

    // True for the mode whose Brush joins the body part chosen in the Stamps tab in place of a stroke
    public boolean placesParts() {
        return toolCanvas == ToolCanvas.CHARACTER_SCULPT;
    }

    // True for a mesh mode whose tools select and shape the part's vertices: every one but Paint
    public boolean editsVertices() {
        return meshMode && !paintsMaps();
    }

    // True for the mode that edits the rig, which draws the colliders its bones carry
    public boolean showsColliders() {
        return toolCanvas == ToolCanvas.CHARACTER_RIG;
    }

    // The tool canvas the viewport is while this mode is open, null for a mode that takes no tools
    public ToolCanvas getToolCanvas() {
        return toolCanvas;
    }

    // True for a mode that takes tools, rather than one shaping the whole character's look
    public boolean isModeling() {
        return toolCanvas != null;
    }

    // True for a mode whose tab lists the regions as its layers, every edit held to the region picked
    public boolean takesRegions() {
        return isModeling() && !paintsMaps();
    }

    // True for a mode whose tab lists layers in the Layers tab: the regions, or the paint layers
    public boolean listsLayers() {
        return takesRegions() || paintsMaps();
    }

    // What the status bar tells the pointer to do in this mode, null for a mode with nothing to say
    public String getHint() {
        return hint;
    }
}
