package editor.runtime.characterview.util;

import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CharacterBonePickUtility extends EngineUtility {

    /*
     * The bone a click in a character viewport picks, as Blender picks one: a
     * bone is taken anywhere along the segment drawn from its joint to each
     * child's, not only at its joint, so a bone is as easy to hit as it is to
     * see. Each viewport projects its own joints and says which bones it lets
     * be picked; the nearest joint within the joint radius wins, else the
     * nearest segment within the segment radius, a joint always ahead of a
     * segment equally near so crowded joints stay reachable.
     */

    // Pick \\

    // screen holds every bone's joint as x then y in pixels, x NaN where it does not show; a bone neither pickable
    // nor showing is never taken, and a segment leading to it still picks its parent
    public static int pick(
            FloatArrayList screen,
            BooleanArrayList pickable,
            IntArrayList parents,
            float mouseX,
            float mouseY) {

        float jointRadius = EditorSetting.CHARACTER_VIEW_PICK_RADIUS_PIXELS;
        float segmentRadius = EditorSetting.CHARACTER_VIEW_PICK_SEGMENT_PIXELS;
        float best = jointRadius * jointRadius;
        int picked = EngineSetting.INDEX_NOT_FOUND;
        int count = parents.size();

        for (int i = 0; i < count; i++) {

            if (!pickable.getBoolean(i) || Float.isNaN(screen.getFloat(i * 2)))
                continue;

            float dx = screen.getFloat(i * 2) - mouseX;
            float dy = screen.getFloat(i * 2 + 1) - mouseY;
            float distance = dx * dx + dy * dy;

            if (distance <= best) {
                best = distance;
                picked = i;
            }
        }

        if (picked != EngineSetting.INDEX_NOT_FOUND)
            return picked;

        best = segmentRadius * segmentRadius;

        for (int i = 0; i < count; i++) {

            int parent = parents.getInt(i);

            if (parent == EngineSetting.INDEX_NOT_FOUND || !pickable.getBoolean(parent)
                    || Float.isNaN(screen.getFloat(i * 2)) || Float.isNaN(screen.getFloat(parent * 2)))
                continue;

            float distance = resolveSegmentDistance(
                    screen.getFloat(parent * 2), screen.getFloat(parent * 2 + 1),
                    screen.getFloat(i * 2), screen.getFloat(i * 2 + 1),
                    mouseX, mouseY);

            if (distance <= best) {
                best = distance;
                picked = parent;
            }
        }

        return picked;
    }

    // The squared distance from a point to the segment between two others
    private static float resolveSegmentDistance(float ax, float ay, float bx, float by, float px, float py) {

        float abx = bx - ax;
        float aby = by - ay;
        float lengthSquared = abx * abx + aby * aby;
        float along = lengthSquared > 0f
                ? Math.clamp(((px - ax) * abx + (py - ay) * aby) / lengthSquared, 0f, 1f)
                : 0f;
        float dx = ax + abx * along - px;
        float dy = ay + aby * along - py;

        return dx * dx + dy * dy;
    }

    // Lists sized for a rig of this many bones, every joint hidden and no bone pickable until the viewport says
    public static void reset(FloatArrayList screen, BooleanArrayList pickable, IntArrayList parents, int boneCount) {

        screen.size(boneCount * 2);
        pickable.size(boneCount);
        parents.size(boneCount);

        for (int i = 0; i < boneCount; i++) {
            screen.set(i * 2, Float.NaN);
            pickable.set(i, false);
            parents.set(i, EngineSetting.INDEX_NOT_FOUND);
        }
    }
}
