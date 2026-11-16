package editor.bootstrap.charactereditorpipeline.util;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterLoopUtility extends EngineUtility {

    /*
     * Stateless work on a character mesh's open edge loops, the rims of its
     * holes and of the parts joined to it. A loop is walked along the edges
     * a face uses in its own winding that no face uses the other way, so
     * every loop runs the way the faces beside it wind; its centre, its
     * Newell normal (pointing into a hole the surrounding faces open, out of
     * a rim a part's faces end at) and its mean radius are measured from the
     * vertices' positions. Callers hand in the scratch the walk needs.
     */

    // Loops \\

    // Every open loop of the faces flagged (every face for null), each written into the next list of loops, the
    // lists kept between calls; returns how many were found
    public static int collectLoops(
            RiggedMeshStruct mesh,
            BooleanArrayList faces,
            LongOpenHashSet directedEdges,
            IntArrayList next,
            ObjectArrayList<IntArrayList> loops) {

        directedEdges.clear();
        next.clear();
        next.size(mesh.getVertexCount());

        for (int v = 0; v < next.size(); v++)
            next.set(v, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (faces == null || faces.getBoolean(f))
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                    int a = mesh.getFaceVertex(f, c);
                    int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                    if (a != b)
                        directedEdges.add(toDirectedKey(a, b));
                }

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (faces == null || faces.getBoolean(f))
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                    int a = mesh.getFaceVertex(f, c);
                    int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                    if (a != b && !directedEdges.contains(toDirectedKey(b, a)))
                        next.set(a, b);
                }

        int count = 0;

        for (int start = 0; start < next.size(); start++) {

            if (next.getInt(start) == EngineSetting.INDEX_NOT_FOUND)
                continue;

            if (loops.size() <= count)
                loops.add(new IntArrayList());

            if (walkLoop(next, start, loops.get(count)))
                count++;
        }

        return count;
    }

    // One loop from a vertex along the open edges, each edge taken once; false when it does not close
    private static boolean walkLoop(IntArrayList next, int start, IntArrayList loop) {

        loop.clear();

        int vertex = start;

        while (vertex != EngineSetting.INDEX_NOT_FOUND && next.getInt(vertex) != EngineSetting.INDEX_NOT_FOUND) {

            int following = next.getInt(vertex);

            loop.add(vertex);
            next.set(vertex, EngineSetting.INDEX_NOT_FOUND);
            vertex = following;

            if (vertex == start)
                return loop.size() > 2;
        }

        return false;
    }

    // The loop found among the first count whose centre lies nearest a point, INDEX_NOT_FOUND for none
    public static int findNearestLoop(
            RiggedMeshStruct mesh,
            ObjectArrayList<IntArrayList> loops,
            int count,
            Vector3 point,
            Vector3 centreScratch) {

        int nearest = EngineSetting.INDEX_NOT_FOUND;
        float best = Float.POSITIVE_INFINITY;

        for (int i = 0; i < count; i++) {

            resolveCentre(mesh, loops.get(i), centreScratch);

            float distance = CharacterMeshUtility.distanceSquared(centreScratch, point);

            if (distance < best) {
                best = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    // Measure \\

    public static void resolveCentre(RiggedMeshStruct mesh, IntArrayList loop, Vector3 out) {

        out.set(0f, 0f, 0f);

        for (int i = 0; i < loop.size(); i++)
            out.add(mesh.getX(loop.getInt(i)), mesh.getY(loop.getInt(i)), mesh.getZ(loop.getInt(i)));

        out.divide(Math.max(1, loop.size()));
    }

    // The loop's Newell normal made unit, or zero for a loop with no area
    public static void resolveNormal(RiggedMeshStruct mesh, IntArrayList loop, Vector3 out) {

        out.set(0f, 0f, 0f);

        for (int i = 0; i < loop.size(); i++) {

            int a = loop.getInt(i);
            int b = loop.getInt((i + 1) % loop.size());

            out.add(
                    (mesh.getY(a) - mesh.getY(b)) * (mesh.getZ(a) + mesh.getZ(b)),
                    (mesh.getZ(a) - mesh.getZ(b)) * (mesh.getX(a) + mesh.getX(b)),
                    (mesh.getX(a) - mesh.getX(b)) * (mesh.getY(a) + mesh.getY(b)));
        }

        float length = out.length();

        if (length > 0f)
            out.divide(length);
    }

    // The mean distance of the loop's vertices from a centre
    public static float resolveRadius(RiggedMeshStruct mesh, IntArrayList loop, Vector3 centre) {

        float total = 0f;

        for (int i = 0; i < loop.size(); i++) {

            int v = loop.getInt(i);
            float dx = mesh.getX(v) - centre.x;
            float dy = mesh.getY(v) - centre.y;
            float dz = mesh.getZ(v) - centre.z;

            total += (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        return total / Math.max(1, loop.size());
    }

    // The bone weighing most over the loop's vertices, INDEX_NOT_FOUND for a loop weighed onto none
    public static int resolveHeaviestBone(RiggedMeshStruct mesh, IntArrayList loop, float[] boneTotals) {

        Arrays.fill(boneTotals, 0f);

        int heaviest = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < loop.size(); i++) {

            int v = loop.getInt(i);

            for (int n = 0; n < mesh.getInfluenceCount(v); n++) {

                int bone = mesh.getInfluenceBone(v, n);

                boneTotals[bone] += mesh.getInfluenceWeight(v, n);

                if (heaviest == EngineSetting.INDEX_NOT_FOUND || boneTotals[bone] > boneTotals[heaviest])
                    heaviest = bone;
            }
        }

        return heaviest;
    }

    // Utility \\

    // An edge's key with its direction kept, the first vertex in the high half
    public static long toDirectedKey(int from, int to) {
        return (long) from << Integer.SIZE | to;
    }
}
