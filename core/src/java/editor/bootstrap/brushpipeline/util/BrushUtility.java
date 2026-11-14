package editor.bootstrap.brushpipeline.util;

import editor.bootstrap.brushpipeline.brush.BrushHandle;
import engine.root.EngineUtility;

public class BrushUtility extends EngineUtility {

    /*
     * Stateless brush arithmetic every stroking tool, outline and thumbnail
     * shares: how far an offset from a tip's centre lies in the tip's own
     * metric, turned by its angle and squashed by its ratio, so one radius
     * out is the tip's edge whatever its shape; how much of a tip covers an
     * offset, all of it across the hard core easing smoothly to none at that
     * edge, for the tool to scale by its own strength; and the gap between
     * two stamps of a stroke for a tip of a given radius. Offsets are taken
     * with y pointing up the screen, so an angle turns every tip the same way
     * in every window.
     */

    // Shape \\

    public static float measure(BrushHandle brush, float dx, float dy) {

        float cos = brush.getAngleCos();
        float sin = brush.getAngleSin();

        return brush.getShape().measure(
                dx * cos + dy * sin,
                (dy * cos - dx * sin) * brush.getInverseRatio());
    }

    // Coverage \\

    // Zero outside the tip; a radius of zero covers only the centre
    public static float resolveCoverage(BrushHandle brush, float dx, float dy, float radius) {

        float distance = measure(brush, dx, dy);

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
