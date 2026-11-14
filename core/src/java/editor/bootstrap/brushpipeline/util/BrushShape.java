package editor.bootstrap.brushpipeline.util;

import editor.runtime.EditorSetting;

public enum BrushShape {

    /*
     * The outline of a brush's tip: a disc, a square or a diamond. Each measures
     * how far an offset lies from the tip's centre in its own metric, so an
     * offset at distance one from the centre lies on the tip's edge whatever
     * its shape. The ordinal is the shape index the brush outline shader
     * draws, so the order is fixed.
     */

    ROUND(EditorSetting.BRUSH_SHAPE_ROUND) {
        @Override
        public float measure(float dx, float dy) {
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    },
    SQUARE(EditorSetting.BRUSH_SHAPE_SQUARE) {
        @Override
        public float measure(float dx, float dy) {
            return Math.max(Math.abs(dx), Math.abs(dy));
        }
    },
    DIAMOND(EditorSetting.BRUSH_SHAPE_DIAMOND) {
        @Override
        public float measure(float dx, float dy) {
            return Math.abs(dx) + Math.abs(dy);
        }
    };

    // Values
    public static final BrushShape[] VALUES = values();

    // Internal
    private final String label;

    // Constructor \\

    BrushShape(String label) {
        this.label = label;
    }

    // Shape \\

    public abstract float measure(float dx, float dy);

    // Accessible \\

    public String getLabel() {
        return label;
    }
}
