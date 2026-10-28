package editor.bootstrap.brushpipeline.util;

import editor.bootstrap.brushpipeline.brush.BrushHandle;
import engine.root.EngineUtility;

public class BrushUtility extends EngineUtility {

    /*
     * Stateless brush arithmetic every stroking tool shares: how much of a
     * tip covers an offset from its centre, all of it across the hard core
     * easing smoothly to none at the edge of its shape, for the tool to scale
     * by its own strength, and the gap between two stamps of a stroke for a
     * tip of a given radius.
     */

    // Coverage \\

    // Zero outside the tip; a radius of zero covers only the centre
    public static float resolveCoverage(BrushHandle brush, float dx, float dy, float radius) {

        float distance = brush.getShape().measure(dx, dy);

        if (radius <= 0f)
            return distance <= 0f ? 1f : 0f;

        float reach = distance / radius;

        if (reach > 1f)
            return 0f;

        float hardness = brush.getValue(BrushField.HARDNESS);

        if (reach <= hardness)
            return 1f;

        float t = (1f - reach) / (1f - hardness);

        return t * t * (3f - 2f * t);
    }

    // Spacing \\

    // How far a stroke travels between stamps, never less than one unit
    public static float resolveSpacing(BrushHandle brush, float radius) {
        return Math.max(1f, brush.getValue(BrushField.SPACING) * (radius * 2f + 1f));
    }
}
