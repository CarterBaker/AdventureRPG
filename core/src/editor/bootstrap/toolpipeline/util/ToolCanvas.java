package editor.bootstrap.toolpipeline.util;

import editor.bootstrap.toolpipeline.tool.ToolActionGroupStruct;
import editor.bootstrap.toolpipeline.tool.ToolActionStruct;
import editor.bootstrap.toolpipeline.tool.ToolOptionStruct;
import editor.runtime.EditorSetting;

public enum ToolCanvas {

    /*
     * What a window's tools work on right now, each with the tools it
     * takes, the first being the one it starts with, the actions it lists
     * under them in titled groups — one-off operations such as a mesh's
     * selection and geometry edits, run by its window — whether what it
     * holds can blend between values, and every tool option it honours with
     * its range there: a brush is sized in image pixels on the world's
     * layers and in window pixels on a character. A tool shows only the
     * options both it and the canvas have, and a canvas without Size takes
     * no brush; on one with it, every tool that carries Size strokes with
     * the brush, so a mesh's Select sweeps the brush over its vertices.
     * Biome colors name biomes, so they never blend; heights, bone weights
     * and a sculpted surface do. The world's heights are their own canvas,
     * raised, lowered, smoothed, flattened and erased back to level with
     * the brush, never painted from a palette. A character's rig, its face
     * and an animation's pose take the tools that pick and drag its joints;
     * the face drags a bone into the face control selected.
     */

    BIOMES(
            EditorSetting.TOOL_CANVAS_BIOMES,
            new EditorTool[] {
                    EditorTool.BRUSH, EditorTool.SMOOTH, EditorTool.SWIRL, EditorTool.SCATTER,
                    EditorTool.FILL, EditorTool.PICK },
            false,
            imageSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT),
            twist()),
    HEIGHTS(
            EditorSetting.TOOL_CANVAS_HEIGHTS,
            new EditorTool[] {
                    EditorTool.RAISE, EditorTool.LOWER, EditorTool.SMOOTH, EditorTool.FLATTEN, EditorTool.ERASE },
            true,
            imageSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT)),
    SETTLEMENTS(
            EditorSetting.TOOL_CANVAS_SETTLEMENTS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.PLACE, EditorTool.ERASE },
            false),
    CAMERAS(
            EditorSetting.TOOL_CANVAS_CAMERAS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.PLACE, EditorTool.ERASE },
            false),
    CHARACTER_MESH(
            EditorSetting.TOOL_CANVAS_CHARACTER_MESH,
            new EditorTool[] { EditorTool.SELECT, EditorTool.MOVE, EditorTool.ROTATE, EditorTool.SCALE },
            new ToolActionGroupStruct[] { meshTransform(), meshGeometry(), meshSelection() },
            false,
            screenSize()),
    CHARACTER_SCULPT(
            EditorSetting.TOOL_CANVAS_CHARACTER_SCULPT,
            new EditorTool[] {
                    EditorTool.BRUSH, EditorTool.SMOOTH, EditorTool.RELAX, EditorTool.INFLATE, EditorTool.PINCH,
                    EditorTool.CREASE, EditorTool.FLATTEN, EditorTool.NUDGE },
            true,
            screenSize(),
            strength(EditorSetting.TOOL_SCULPT_STRENGTH_DEFAULT)),
    CHARACTER_WEIGHTS(
            EditorSetting.TOOL_CANVAS_CHARACTER_WEIGHTS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.BRUSH, EditorTool.ERASE, EditorTool.SMOOTH },
            new ToolActionGroupStruct[] { meshWeights(), meshSelection() },
            true,
            screenSize(),
            strength(EditorSetting.TOOL_WEIGHT_STRENGTH_DEFAULT)),
    CHARACTER_RIG(
            EditorSetting.TOOL_CANVAS_CHARACTER_RIG,
            new EditorTool[] {
                    EditorTool.SELECT, EditorTool.MOVE, EditorTool.SCALE, EditorTool.PLACE, EditorTool.ERASE },
            new ToolActionGroupStruct[] { rigActions(), hairActions() },
            false),
    CHARACTER_FACE(
            EditorSetting.TOOL_CANVAS_CHARACTER_FACE,
            new EditorTool[] { EditorTool.SELECT, EditorTool.MOVE, EditorTool.ROTATE, EditorTool.SCALE },
            new ToolActionGroupStruct[] { faceControls() },
            false),
    ANIMATION_POSE(
            EditorSetting.TOOL_CANVAS_ANIMATION_POSE,
            new EditorTool[] { EditorTool.ROTATE, EditorTool.SELECT, EditorTool.MOVE, EditorTool.SCALE, EditorTool.IK },
            false),
    ITEM(
            EditorSetting.TOOL_CANVAS_ITEM,
            new EditorTool[] { EditorTool.PLACE, EditorTool.WALL, EditorTool.ERASE, EditorTool.BRUSH, EditorTool.PICK },
            false),
    RECORDER(
            EditorSetting.TOOL_CANVAS_RECORDER,
            new EditorTool[] { EditorTool.SELECT, EditorTool.CUT },
            false);

    // Values
    public static final ToolCanvas[] VALUES = values();

    // Internal
    private final String label;
    private final EditorTool[] tools;
    private final ToolActionGroupStruct[] actionGroups;
    private final boolean blending;
    private final ToolOptionStruct[] options;

    // Constructor \\

    ToolCanvas(String label, EditorTool[] tools, boolean blending, ToolOptionStruct... options) {
        this(label, tools, new ToolActionGroupStruct[0], blending, options);
    }

    ToolCanvas(
            String label,
            EditorTool[] tools,
            ToolActionGroupStruct[] actionGroups,
            boolean blending,
            ToolOptionStruct... options) {
        this.label = label;
        this.tools = tools;
        this.actionGroups = actionGroups;
        this.blending = blending;
        this.options = options;
    }

    // Actions \\

    private static ToolActionGroupStruct meshTransform() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_TRANSFORM,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_GRAB, EditorSetting.CHARACTER_EDITOR_KEY_GRAB),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_ROTATE, EditorSetting.CHARACTER_EDITOR_KEY_ROTATE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_SCALE, EditorSetting.CHARACTER_EDITOR_KEY_SCALE));
    }

    private static ToolActionGroupStruct meshGeometry() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_GEOMETRY,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_EXTRUDE, EditorSetting.CHARACTER_EDITOR_KEY_EXTRUDE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_DUPLICATE, EditorSetting.CHARACTER_EDITOR_KEY_DUPLICATE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_MAKE_FACE, EditorSetting.CHARACTER_EDITOR_KEY_MAKE_FACE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_MERGE, EditorSetting.CHARACTER_EDITOR_KEY_MERGE),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_SUBDIVIDE, EditorSetting.CHARACTER_EDITOR_KEY_SUBDIVIDE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_VERTICES,
                        EditorSetting.CHARACTER_EDITOR_KEY_DELETE_VERTICES),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_DELETE_FACES,
                        EditorSetting.CHARACTER_EDITOR_KEY_DELETE_FACES),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SYMMETRIZE_POSITIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_POSITIVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SYMMETRIZE_NEGATIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SYMMETRIZE_NEGATIVE));
    }

    private static ToolActionGroupStruct meshWeights() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_WEIGHTS,
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_ASSIGN,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ASSIGN),
                action(EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_ADD, EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_ADD),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_SUBTRACT,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SUBTRACT),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_REMOVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_REMOVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_NORMALIZE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_NORMALIZE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_SMOOTH,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_SMOOTH),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_POSITIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_POSITIVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_MIRROR_NEGATIVE,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_MIRROR_NEGATIVE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_DIGITS,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_DIGITS),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_WEIGHT_LOCKS,
                        EditorSetting.CHARACTER_EDITOR_KEY_WEIGHT_LOCKS),
                action(
                        EditorSetting.CHARACTER_RIG_LABEL_SPRING_PREVIEW,
                        EditorSetting.CHARACTER_RIG_KEY_SPRING_PREVIEW));
    }

    private static ToolActionGroupStruct meshSelection() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_EDITOR_HEADER_SELECTION,
                action(EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_ALL, EditorSetting.CHARACTER_EDITOR_KEY_SELECT_ALL),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_NONE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_NONE),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_INVERT,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_INVERT),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_LINKED,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_LINKED),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_MIRROR,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_MIRROR),
                action(
                        EditorSetting.CHARACTER_EDITOR_LABEL_SELECT_BONE,
                        EditorSetting.CHARACTER_EDITOR_KEY_SELECT_BONE));
    }

    private static ToolActionGroupStruct rigActions() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_RIG_HEADER_ACTIONS,
                action(EditorSetting.CHARACTER_RIG_LABEL_DIGITS, EditorSetting.CHARACTER_RIG_KEY_DIGITS),
                action(EditorSetting.CHARACTER_RIG_LABEL_IK_ADD, EditorSetting.CHARACTER_RIG_KEY_IK_ADD),
                action(EditorSetting.CHARACTER_RIG_LABEL_IK_REMOVE, EditorSetting.CHARACTER_RIG_KEY_IK_REMOVE));
    }

    private static ToolActionGroupStruct hairActions() {
        return new ToolActionGroupStruct(
                EditorSetting.CHARACTER_RIG_HEADER_HAIR,
                action(EditorSetting.CHARACTER_RIG_LABEL_HAIR, EditorSetting.CHARACTER_RIG_KEY_HAIR),
                action(EditorSetting.CHARACTER_RIG_LABEL_SPRING_ADD, EditorSetting.CHARACTER_RIG_KEY_SPRING_ADD),
                action(EditorSetting.CHARACTER_RIG_LABEL_SPRING_REMOVE, EditorSetting.CHARACTER_RIG_KEY_SPRING_REMOVE),
                action(
                        EditorSetting.CHARACTER_RIG_LABEL_SPRING_PREVIEW,
                        EditorSetting.CHARACTER_RIG_KEY_SPRING_PREVIEW));
    }

    private static ToolActionGroupStruct faceControls() {
        return new ToolActionGroupStruct(
                EditorSetting.FACE_EDITOR_HEADER_ACTIONS,
                action(EditorSetting.FACE_EDITOR_LABEL_ADD_CONTROL, EditorSetting.FACE_EDITOR_KEY_ADD_CONTROL),
                action(
                        EditorSetting.FACE_EDITOR_LABEL_DUPLICATE_CONTROL,
                        EditorSetting.FACE_EDITOR_KEY_DUPLICATE_CONTROL),
                action(EditorSetting.FACE_EDITOR_LABEL_REMOVE_CONTROL, EditorSetting.FACE_EDITOR_KEY_REMOVE_CONTROL),
                action(EditorSetting.FACE_EDITOR_LABEL_ADD_TARGET, EditorSetting.FACE_EDITOR_KEY_ADD_TARGET),
                action(EditorSetting.FACE_EDITOR_LABEL_MIRROR_TARGETS, EditorSetting.FACE_EDITOR_KEY_MIRROR_TARGETS),
                action(EditorSetting.FACE_EDITOR_LABEL_RESET_PREVIEW, EditorSetting.FACE_EDITOR_KEY_RESET_PREVIEW));
    }

    private static ToolActionStruct action(String label, String key) {
        return new ToolActionStruct(label, key);
    }

    // Options \\

    private static ToolOptionStruct imageSize() {
        return new ToolOptionStruct(
                ToolOption.SIZE,
                EditorSetting.TOOL_IMAGE_SIZE_MIN,
                EditorSetting.TOOL_IMAGE_SIZE_MAX,
                EditorSetting.TOOL_IMAGE_SIZE_STEP,
                EditorSetting.TOOL_IMAGE_SIZE_DEFAULT);
    }

    private static ToolOptionStruct screenSize() {
        return new ToolOptionStruct(
                ToolOption.SIZE,
                EditorSetting.TOOL_SCREEN_SIZE_MIN,
                EditorSetting.TOOL_SCREEN_SIZE_MAX,
                EditorSetting.TOOL_SCREEN_SIZE_STEP,
                EditorSetting.TOOL_SCREEN_SIZE_DEFAULT);
    }

    private static ToolOptionStruct strength(float defaultValue) {
        return new ToolOptionStruct(
                ToolOption.STRENGTH,
                EditorSetting.TOOL_STRENGTH_STEP,
                1f,
                EditorSetting.TOOL_STRENGTH_STEP,
                defaultValue);
    }

    private static ToolOptionStruct twist() {
        return new ToolOptionStruct(
                ToolOption.TWIST,
                -EditorSetting.TOOL_TWIST_MAX,
                EditorSetting.TOOL_TWIST_MAX,
                EditorSetting.TOOL_TWIST_STEP,
                EditorSetting.TOOL_TWIST_DEFAULT);
    }

    // Support \\

    public boolean supports(EditorTool tool) {

        for (int i = 0; i < tools.length; i++)
            if (tools[i] == tool)
                return true;

        return false;
    }

    // Whether the canvas lists an action under a key, the only actions the Tools panel runs on it
    public boolean hasAction(String actionKey) {

        for (int i = 0; i < actionGroups.length; i++) {

            ToolActionStruct[] actions = actionGroups[i].getActions();

            for (int j = 0; j < actions.length; j++)
                if (actions[j].getKey().equals(actionKey))
                    return true;
        }

        return false;
    }

    // The canvas's own range for an option, null for an option it does not honour
    public ToolOptionStruct findOption(ToolOption option) {

        for (int i = 0; i < options.length; i++)
            if (options[i].getOption() == option)
                return options[i];

        return null;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public EditorTool[] getTools() {
        return tools;
    }

    public ToolActionGroupStruct[] getActionGroups() {
        return actionGroups;
    }

    public EditorTool getDefaultTool() {
        return tools[0];
    }

    public boolean isBlending() {
        return blending;
    }

    // A canvas strokes with the brush when its tools have a size to stroke at
    public boolean takesBrush() {
        return findOption(ToolOption.SIZE) != null;
    }

    // Whether a tool strokes the brush here: every tool sized by the brush on a canvas that takes one
    public boolean strokes(EditorTool tool) {
        return takesBrush() && tool.hasOption(ToolOption.SIZE);
    }
}
