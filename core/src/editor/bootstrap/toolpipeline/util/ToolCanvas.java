package editor.bootstrap.toolpipeline.util;

import editor.bootstrap.toolpipeline.tool.ToolOptionStruct;
import editor.runtime.EditorSetting;

public enum ToolCanvas {

    /*
     * What a window's tools work on right now, each with the tools it takes,
     * the first being the one it starts with, whether what it holds can blend
     * between values, and every tool option it honours with its range there:
     * a brush is sized in image pixels on the world's layers and in window
     * pixels on a character. A tool shows only the options both it and the
     * canvas have, and a canvas without Size takes no brush. Biome colors
     * name biomes, so they never blend; heights and bone weights do.
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
                    EditorTool.BRUSH, EditorTool.ERASE, EditorTool.SMOOTH, EditorTool.SWIRL, EditorTool.SCATTER,
                    EditorTool.FILL, EditorTool.PICK },
            true,
            imageSize(),
            strength(EditorSetting.TOOL_STRENGTH_DEFAULT),
            twist()),
    SETTLEMENTS(
            EditorSetting.TOOL_CANVAS_SETTLEMENTS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.PLACE, EditorTool.ERASE },
            false),
    CHARACTER_MESH(
            EditorSetting.TOOL_CANVAS_CHARACTER_MESH,
            new EditorTool[] { EditorTool.SELECT, EditorTool.MOVE, EditorTool.ROTATE, EditorTool.SCALE },
            false),
    CHARACTER_WEIGHTS(
            EditorSetting.TOOL_CANVAS_CHARACTER_WEIGHTS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.BRUSH, EditorTool.ERASE, EditorTool.SMOOTH },
            true,
            new ToolOptionStruct(
                    ToolOption.SIZE,
                    EditorSetting.TOOL_SCREEN_SIZE_MIN,
                    EditorSetting.TOOL_SCREEN_SIZE_MAX,
                    EditorSetting.TOOL_SCREEN_SIZE_STEP,
                    EditorSetting.TOOL_SCREEN_SIZE_DEFAULT),
            strength(EditorSetting.TOOL_WEIGHT_STRENGTH_DEFAULT)),
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
    private final boolean blending;
    private final ToolOptionStruct[] options;

    // Constructor \\

    ToolCanvas(String label, EditorTool[] tools, boolean blending, ToolOptionStruct... options) {
        this.label = label;
        this.tools = tools;
        this.blending = blending;
        this.options = options;
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
}
