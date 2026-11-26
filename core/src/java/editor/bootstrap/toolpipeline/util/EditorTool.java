package editor.bootstrap.toolpipeline.util;

import editor.runtime.EditorSetting;

public enum EditorTool {

    /*
     * The editor's tools, the same in every window that takes them; what
     * each does is the window's own, and each canvas takes only those that
     * mean something there. Select picks things to work on, sweeping the
     * brush's tip over them wherever the canvas sizes a brush; Brush
     * strokes the brush over what it works on, laying the chosen value
     * down, and Erase strokes it away; Smooth softens what lies under the
     * brush, Swirl twists it about the brush's centre and Scatter shuffles
     * it; Inflate swells a surface along its own normals, Pinch draws it in
     * toward the brush's centre, Flatten levels it, a surface onto its mean
     * plane and an image to the value where the stroke began, and Nudge
     * drags it along with the pointer; Raise and Lower build what lies
     * under the brush up or wear it down a little more with every stamp;
     * Topology gives a surface more faces where the brush covers it, or
     * fewer with the stroke inverted, keeping it all quads; Flip Edge turns
     * the edge clicked one step round the two faces beside it, so its ends
     * move to the next corners along, Loop Cut cuts a new edge loop through
     * the ring of faces across the edge clicked, and Dissolve Loop takes the
     * edge loop clicked away, joining the faces either side of it; Draw
     * draws a new strand of hair from where it presses along the drag;
     * Fill floods a run of one value and Pick takes up the value under the
     * pointer; Place stands a new thing where it clicks and Wall a thin
     * wall on the face it clicks; Move, Rotate and Scale drag what is
     * selected, and on hair Place grows a strand where it clicks; Cut cuts a clip in two where it clicks; IK Handle drags the
     * end of a limb or digit the rig carries a handle for, and the joints
     * above it bend the way the handle's pole says; Pose turns a bone by its
     * joint so what it carries follows, a pose held only to work in. While a
     * body part is chosen in the Stamps tab, the Brush joins it where it
     * clicks, at its Scale and Turn, or lays fur combed the way it is set,
     * on the canvases that declare those options. Light turns the light
     * a viewer shows its subject by, dragged from the subject. Collider gives
     * the bone clicked a collider, or takes one off it while Ctrl is held. Hand
     * only moves the view, as a painting tool's hand does: on a canvas whose
     * tool it is, every press goes to the view, a left drag turning a 3D view
     * and sliding a flat one, so Move always moves what is selected. The
     * stroking tools work along a drag with a brush and carry the options
     * that shape their strokes; the rest act once per press. Each tool's
     * emblem is the sprite named after it in PascalCase under the tool icon
     * folder (Flip Edge is menus/tools/FlipEdge), the one every list of tools
     * shows beside its name.
     */

    SELECT(EditorSetting.TOOL_SELECT, false, ToolOption.SIZE),
    BRUSH(
            EditorSetting.TOOL_BRUSH,
            true,
            ToolOption.SIZE,
            ToolOption.STRENGTH,
            ToolOption.SCALE,
            ToolOption.TURN,
            ToolOption.COMB),
    ERASE(EditorSetting.TOOL_ERASE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    SMOOTH(EditorSetting.TOOL_SMOOTH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    SWIRL(EditorSetting.TOOL_SWIRL, true, ToolOption.SIZE, ToolOption.STRENGTH, ToolOption.TWIST),
    SCATTER(EditorSetting.TOOL_SCATTER, true, ToolOption.SIZE, ToolOption.STRENGTH),
    INFLATE(EditorSetting.TOOL_INFLATE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    PINCH(EditorSetting.TOOL_PINCH, true, ToolOption.SIZE, ToolOption.STRENGTH),
    CREASE(EditorSetting.TOOL_CREASE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    RELAX(EditorSetting.TOOL_RELAX, true, ToolOption.SIZE, ToolOption.STRENGTH),
    FLATTEN(EditorSetting.TOOL_FLATTEN, true, ToolOption.SIZE, ToolOption.STRENGTH),
    NUDGE(EditorSetting.TOOL_NUDGE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    RAISE(EditorSetting.TOOL_RAISE, true, ToolOption.SIZE, ToolOption.STRENGTH),
    LOWER(EditorSetting.TOOL_LOWER, true, ToolOption.SIZE, ToolOption.STRENGTH),
    TOPOLOGY(EditorSetting.TOOL_TOPOLOGY, true, ToolOption.SIZE, ToolOption.STRENGTH, ToolOption.DETAIL),
    FLIP_EDGE(EditorSetting.TOOL_FLIP_EDGE, false),
    LOOP_CUT(EditorSetting.TOOL_LOOP_CUT, false),
    DISSOLVE_LOOP(EditorSetting.TOOL_DISSOLVE_LOOP, false),
    DRAW(EditorSetting.TOOL_DRAW, false, ToolOption.WIDTH),
    FILL(EditorSetting.TOOL_FILL, false),
    PICK(EditorSetting.TOOL_PICK, false),
    PLACE(EditorSetting.TOOL_PLACE, false, ToolOption.LENGTH, ToolOption.WIDTH),
    WALL(EditorSetting.TOOL_WALL, false),
    MOVE(EditorSetting.TOOL_MOVE, false),
    ROTATE(EditorSetting.TOOL_ROTATE, false),
    SCALE(EditorSetting.TOOL_SCALE, false),
    CUT(EditorSetting.TOOL_CUT, false),
    IK(EditorSetting.TOOL_IK, false),
    POSE(EditorSetting.TOOL_POSE, false),
    LIGHT(EditorSetting.TOOL_LIGHT, false),
    COLLIDER(EditorSetting.TOOL_COLLIDER, false),
    HAND(EditorSetting.TOOL_HAND, false);

    // Values
    public static final EditorTool[] VALUES = values();

    // Internal
    private final String label;
    private final boolean stroke;
    private final ToolOption[] options;
    private final String icon;

    // Constructor \\

    EditorTool(String label, boolean stroke, ToolOption... options) {
        this.label = label;
        this.stroke = stroke;
        this.options = options;
        this.icon = EditorSetting.TOOL_ICON_FOLDER + toPascalCase(name());
    }

    // An enum name read as a resource name: each word between underscores opened in upper case, the rest lower
    private static String toPascalCase(String enumName) {

        StringBuilder pascal = new StringBuilder(enumName.length());
        boolean wordStart = true;

        for (int i = 0; i < enumName.length(); i++) {

            char character = enumName.charAt(i);

            if (character == '_') {
                wordStart = true;
                continue;
            }

            pascal.append(wordStart ? character : Character.toLowerCase(character));
            wordStart = false;
        }

        return pascal.toString();
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The sprite of the tool's emblem
    public String getIcon() {
        return icon;
    }

    public boolean isStroke() {
        return stroke;
    }

    public ToolOption[] getOptions() {
        return options;
    }

    public boolean hasOption(ToolOption option) {

        for (int i = 0; i < options.length; i++)
            if (options[i] == option)
                return true;

        return false;
    }
}
