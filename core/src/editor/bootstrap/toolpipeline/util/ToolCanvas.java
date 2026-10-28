package editor.bootstrap.toolpipeline.util;

import editor.bootstrap.brushpipeline.util.BrushMode;
import editor.runtime.EditorSetting;

public enum ToolCanvas {

    /*
     * What a window's tools work on right now, each with the tools it takes,
     * the first being the one it starts with, whether what it holds can blend
     * between values, the brush modes it can stroke, and the range and step
     * of its brush size in its own units: image pixels on the world's layers,
     * window pixels on a character. A canvas that takes no brush has no size.
     * Biome colors name biomes, so they never blend; heights and bone
     * weights do.
     */

    BIOMES(
            EditorSetting.TOOL_CANVAS_BIOMES,
            new EditorTool[] { EditorTool.BRUSH, EditorTool.FILL, EditorTool.PICK },
            false,
            BrushMode.VALUES,
            EditorSetting.TOOL_IMAGE_SIZE_MIN,
            EditorSetting.TOOL_IMAGE_SIZE_MAX,
            EditorSetting.TOOL_IMAGE_SIZE_STEP,
            EditorSetting.TOOL_IMAGE_SIZE_DEFAULT),
    HEIGHTS(
            EditorSetting.TOOL_CANVAS_HEIGHTS,
            new EditorTool[] { EditorTool.BRUSH, EditorTool.ERASE, EditorTool.FILL, EditorTool.PICK },
            true,
            BrushMode.VALUES,
            EditorSetting.TOOL_IMAGE_SIZE_MIN,
            EditorSetting.TOOL_IMAGE_SIZE_MAX,
            EditorSetting.TOOL_IMAGE_SIZE_STEP,
            EditorSetting.TOOL_IMAGE_SIZE_DEFAULT),
    SETTLEMENTS(
            EditorSetting.TOOL_CANVAS_SETTLEMENTS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.PLACE, EditorTool.ERASE },
            false,
            new BrushMode[0],
            0f,
            0f,
            0f,
            0f),
    CHARACTER_MESH(
            EditorSetting.TOOL_CANVAS_CHARACTER_MESH,
            new EditorTool[] { EditorTool.SELECT },
            false,
            new BrushMode[0],
            0f,
            0f,
            0f,
            0f),
    CHARACTER_WEIGHTS(
            EditorSetting.TOOL_CANVAS_CHARACTER_WEIGHTS,
            new EditorTool[] { EditorTool.SELECT, EditorTool.BRUSH, EditorTool.ERASE },
            true,
            new BrushMode[] { BrushMode.PAINT, BrushMode.SMOOTH },
            EditorSetting.TOOL_SCREEN_SIZE_MIN,
            EditorSetting.TOOL_SCREEN_SIZE_MAX,
            EditorSetting.TOOL_SCREEN_SIZE_STEP,
            EditorSetting.TOOL_SCREEN_SIZE_DEFAULT),
    ITEM(
            EditorSetting.TOOL_CANVAS_ITEM,
            new EditorTool[] { EditorTool.PLACE, EditorTool.WALL, EditorTool.ERASE, EditorTool.BRUSH },
            false,
            new BrushMode[0],
            0f,
            0f,
            0f,
            0f);

    // Values
    public static final ToolCanvas[] VALUES = values();

    // Internal
    private final String label;
    private final EditorTool[] tools;
    private final boolean blending;
    private final BrushMode[] modes;
    private final float sizeMin;
    private final float sizeMax;
    private final float sizeStep;
    private final float sizeDefault;

    // Constructor \\

    ToolCanvas(
            String label,
            EditorTool[] tools,
            boolean blending,
            BrushMode[] modes,
            float sizeMin,
            float sizeMax,
            float sizeStep,
            float sizeDefault) {

        this.label = label;
        this.tools = tools;
        this.blending = blending;
        this.modes = modes;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.sizeStep = sizeStep;
        this.sizeDefault = sizeDefault;
    }

    // Support \\

    public boolean supports(EditorTool tool) {

        for (int i = 0; i < tools.length; i++)
            if (tools[i] == tool)
                return true;

        return false;
    }

    public boolean supports(BrushMode mode) {

        for (int i = 0; i < modes.length; i++)
            if (modes[i] == mode)
                return true;

        return false;
    }

    // A size one step from another, held to the canvas's range
    public float stepSize(float size, int direction) {
        return Math.max(sizeMin, Math.min(sizeMax, size + direction * sizeStep));
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

    public boolean takesBrush() {
        return modes.length > 0;
    }

    public float getSizeDefault() {
        return sizeDefault;
    }
}
