package editor.bootstrap.charactereditorpipeline.util;

import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterStrandStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class CharacterStrandUtility extends EngineUtility {

    /*
     * Stateless layout of a hair strand's card, shared by the export that
     * builds it and the overlay that shows it: at every point a row of
     * corners across the strand, one per card column, half the strand's width
     * out either side along a side vector carried down the strand from the
     * root's (the root tangent crossed with the surface normal, then kept
     * square to each tangent as it goes, a rotation-minimising frame), the
     * width narrowing from the root to the taper at the tip. Each row holds
     * three floats per column, followed by the v the row stands at, 1 at the
     * root to 0 at the tip.
     */

    // Card \\

    // Every point's row of card corners and its v, written over the rows given
    public static void resolveCardRows(CharacterStrandStruct strand, FloatArrayList rows) {

        int count = strand.getPointCount();
        int columns = EditorSetting.CHARACTER_HAIR_CARD_U.length;
        float length = Math.max(strand.getLength(), EditorSetting.CHARACTER_HAIR_EPSILON);
        float reach = 0f;
        float nx = strand.getNormal().x;
        float ny = strand.getNormal().y;
        float nz = strand.getNormal().z;
        float tx = resolveTangent(strand, 0, 0);
        float ty = resolveTangent(strand, 0, 1);
        float tz = resolveTangent(strand, 0, 2);
        float sx = ty * nz - tz * ny;
        float sy = tz * nx - tx * nz;
        float sz = tx * ny - ty * nx;

        rows.clear();
        rows.ensureCapacity(count * (columns * 3 + 1));

        if (sx * sx + sy * sy + sz * sz < EditorSetting.CHARACTER_HAIR_EPSILON) {
            sx = 1f;
            sy = 0f;
            sz = 0f;
        }

        for (int i = 0; i < count; i++) {

            if (i > 0)
                reach += strand.getSegmentLength(i - 1);

            tx = resolveTangent(strand, i, 0);
            ty = resolveTangent(strand, i, 1);
            tz = resolveTangent(strand, i, 2);

            float along = sx * tx + sy * ty + sz * tz;

            sx -= tx * along;
            sy -= ty * along;
            sz -= tz * along;

            float sideLength = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);

            if (sideLength > EditorSetting.CHARACTER_HAIR_EPSILON) {
                sx /= sideLength;
                sy /= sideLength;
                sz /= sideLength;
            }

            float share = reach / length;
            float half = strand.getWidth() * 0.5f * (1f + (strand.getTaper() - 1f) * share);

            for (int u = 0; u < columns; u++) {

                float across = (EditorSetting.CHARACTER_HAIR_CARD_U[u] * 2f - 1f) * half;

                rows.add(strand.getX(i) + sx * across);
                rows.add(strand.getY(i) + sy * across);
                rows.add(strand.getZ(i) + sz * across);
            }

            rows.add(1f - share);
        }
    }

    // How many floats one row of corners and its v takes
    public static int getRowFloats() {
        return EditorSetting.CHARACTER_HAIR_CARD_U.length * 3 + 1;
    }

    // One axis of the unit way a strand runs at a point, from the point before it to the one after; straight down
    // where the two stand together
    private static float resolveTangent(CharacterStrandStruct strand, int pointIndex, int axis) {

        int from = Math.max(0, pointIndex - 1);
        int to = Math.min(strand.getPointCount() - 1, pointIndex + 1);
        float dx = strand.getX(to) - strand.getX(from);
        float dy = strand.getY(to) - strand.getY(from);
        float dz = strand.getZ(to) - strand.getZ(from);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length < EditorSetting.CHARACTER_HAIR_EPSILON)
            return axis == 1 ? -1f : 0f;

        return (axis == 0 ? dx : axis == 1 ? dy : dz) / length;
    }
}
