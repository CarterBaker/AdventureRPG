package editor.bootstrap.brushpipeline.util;

import editor.runtime.EditorSetting;

public enum BrushMode {

    /*
     * What a brush does to what lies under it. Paint lays the tool's value
     * down, fading it in where the content can blend and dithering it in
     * where it cannot. Smooth softens toward the neighbourhood, averaging
     * where the content blends and taking the commonest neighbour where it
     * does not. Swirl twists what lies under the tip about its centre and
     * Scatter shuffles it, both moving only what is already there, so they
     * never make a value the content could not hold.
     */

    PAINT(EditorSetting.BRUSH_MODE_PAINT),
    SMOOTH(EditorSetting.BRUSH_MODE_SMOOTH),
    SWIRL(EditorSetting.BRUSH_MODE_SWIRL),
    SCATTER(EditorSetting.BRUSH_MODE_SCATTER);

    // Values
    public static final BrushMode[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    BrushMode(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
