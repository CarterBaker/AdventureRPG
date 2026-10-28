package editor.bootstrap.brushpipeline.util;

import editor.bootstrap.brushpipeline.brush.BrushHandle;
import engine.root.EngineUtility;

public class BrushUtility extends EngineUtility {

    /*
     * Stateless brush arithmetic every stroking tool shares: the strength a
     * preset stamps at an offset from its centre, full opacity across its
     * hard core easing smoothly to nothing at the edge of its shape, and the
     * gap between two stamps of a stroke for a tip of a given radius.
     */

    // Strength \\

    // Zero outside the tip; a radius of zero covers only the centre
    public static float resolveStrength(BrushHandle brush, float dx, float dy, float radius) {

        float distance = brush.getShape().measure(dx, dy);

        if (radius <= 0f)
            return distance <= 0f ? brush.getValue(BrushField.OPACITY) : 0f;

        float reach = distance / radius;

        if (reach > 1f)
            return 0f;

        float hardness = brush.getValue(BrushField.HARDNESS);
        float opacity = brush.getValue(BrushField.OPACITY);

        if (reach <= hardness)
            return opacity;

        float t = (1f - reach) / (1f - hardness);

        return opacity * t * t * (3f - 2f * t);
    }

    // Spacing \\

    // How far a stroke travels between stamps, never less than one unit
    public static float resolveSpacing(BrushHandle brush, float radius) {
        return Math.max(1f, brush.getValue(BrushField.SPACING) * (radius * 2f + 1f));
    }
}
