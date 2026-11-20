package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;

public enum CharacterEditorMode {

    /*
     * What the character editor's panels work on: Appearance the default
     * features, colors, palettes, every slider the creator offers and the
     * build, Equipment the anchors worn and held gear hangs from, Mesh the
     * vertices and faces of a worn part, Topology the edges and faces those
     * vertices are joined by, Sculpt that part's surface under the brush, Weights the bones each of its vertices follows, Paint the
     * texture maps it is coloured by, Rig the bones themselves and the
     * slider channels they offer, and UV the seams that cut the part's
     * surface and the faces unwrapped flat, their UVs laid out in the UV
     * Editor beside it, and Hair the strands of the groom open, grown over
     * the part's surface and turned into cards when exported. Sculpt's Brush also places the body part chosen in
     * the Stamps tab, its bones grafted onto the rig and its mesh merged into
     * the body. Each mode that takes viewport input is its own tool
     * canvas; the mesh modes edit the part's mesh file, Rig holds the bind
     * pose so the character stands exactly as its mesh and rig are authored,
     * and those that show the vertices draw them over the body. Each carries
     * the hint the status bar shows while it is open, and every other mode
     * either stands in the one pose it names, as Appearance stands idle the
     * way the game's creator shows the player, or lets its rows pick the
     * pose the preview strikes — the mesh modes in the bind pose by default,
     * their vertices moved there alone. Each is its own character tool tab;
     * Mesh, Topology, Sculpt, Weights, Rig and UV list the regions as their layers,
     * Paint the paint layers of the map painted, and the rest work on the
     * whole character; Topology lists the regions too, Hair works on the
     * whole character. The modes that take tools frame the region picked and
     * share their view with one another's tabs stacked in the same dock.
     */

    APPEARANCE(
            EditorSetting.CHARACTER_EDITOR_MODE_APPEARANCE,
            null,
            false,
            false,
            false,
            false,
            CharacterEditorPose.IDLE,
            null),
    EQUIPMENT(EditorSetting.CHARACTER_EDITOR_MODE_EQUIPMENT, null, false, false, false, false, null, null),
    MESH(
            EditorSetting.CHARACTER_EDITOR_MODE_MESH,
            ToolCanvas.CHARACTER_MESH,
            true,
            false,
            true,
            true,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_MESH_HINT),
    TOPOLOGY(
            EditorSetting.CHARACTER_EDITOR_MODE_TOPOLOGY,
            ToolCanvas.CHARACTER_TOPOLOGY,
            true,
            false,
            true,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_TOPOLOGY_HINT),
    SCULPT(
            EditorSetting.CHARACTER_EDITOR_MODE_SCULPT,
            ToolCanvas.CHARACTER_SCULPT,
            true,
            false,
            false,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_SCULPT_HINT),
    WEIGHTS(
            EditorSetting.CHARACTER_EDITOR_MODE_WEIGHTS,
            ToolCanvas.CHARACTER_WEIGHTS,
            true,
            false,
            true,
            true,
            null,
            null),
    PAINT(
            EditorSetting.CHARACTER_EDITOR_MODE_PAINT,
            ToolCanvas.CHARACTER_PAINT,
            true,
            false,
            false,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_PAINT_HINT),
    HAIR(
            EditorSetting.CHARACTER_EDITOR_MODE_HAIR,
            ToolCanvas.CHARACTER_HAIR,
            true,
            true,
            false,
            false,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_HAIR_HINT),
    RIG(
            EditorSetting.CHARACTER_EDITOR_MODE_RIG,
            ToolCanvas.CHARACTER_RIG,
            false,
            true,
            false,
            true,
            null,
            EditorSetting.CHARACTER_EDITOR_STATUS_RIG_HINT),
    UV(
            EditorSetting.CHARACTER_EDITOR_MODE_UV,
            ToolCanvas.CHARACTER_UV,
            true,
            false,
            true,
            false,
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
    private final boolean drawsBones;
    private final CharacterEditorPose standingPose;
    private final String hint;

    // Constructor \\

    CharacterEditorMode(
            String label,
            ToolCanvas toolCanvas,
            boolean meshMode,
            boolean bindPose,
            boolean vertexOverlay,
            boolean drawsBones,
            CharacterEditorPose standingPose,
            String hint) {
        this.label = label;
        this.toolCanvas = toolCanvas;
        this.meshMode = meshMode;
        this.bindPose = bindPose;
        this.vertexOverlay = vertexOverlay;
        this.drawsBones = drawsBones;
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

    // The one pose a posed mode's preview stands in, null for one whose rows pick the pose
    public CharacterEditorPose getStandingPose() {
        return standingPose;
    }

    // True for a posed mode whose rows list the pose the preview strikes
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

    // True for the mode that grooms the strands of hair grown over the part, never its vertices
    public boolean groomsHair() {
        return toolCanvas == ToolCanvas.CHARACTER_HAIR;
    }

    // True for the mode whose edge tools pick the edge under the pointer
    public boolean editsEdges() {
        return toolCanvas == ToolCanvas.CHARACTER_TOPOLOGY;
    }

    // True for the mode whose Brush joins the body part chosen in the Stamps tab in place of a stroke
    public boolean placesParts() {
        return toolCanvas == ToolCanvas.CHARACTER_SCULPT;
    }

    // True for a mesh mode whose tools select and shape the part's vertices: every one but Paint and Hair
    public boolean editsVertices() {
        return meshMode && !paintsMaps() && !groomsHair();
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

    // True for a mode whose viewport draws the rig's bones, and lists how they are drawn in the Properties tab: Mesh
    // and Weights the posed character's, Rig the rig being edited
    public boolean drawsBones() {
        return drawsBones;
    }

    // True for a mode whose tab frames the region picked and shares its view with such tabs stacked with it
    public boolean sharesView() {
        return isModeling();
    }

    // True for a mode whose tab lists the regions as its layers, every edit held to the region picked
    public boolean takesRegions() {
        return isModeling() && !paintsMaps() && !groomsHair();
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
