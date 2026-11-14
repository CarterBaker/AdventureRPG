package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterHairCardStruct extends StructPackage {

    /*
     * One hair card found in a hair mesh: the line it hangs along in the bind
     * pose, one point per row of its strands from root to tip with the raw v
     * each row stands at and how far along the line it lies, and every vertex
     * it is made of with the v it stands at, so a point anywhere along the
     * card, and how far down the card a vertex grows, can be read back.
     */

    // Line — root to tip
    private final ObjectArrayList<Vector3> points;
    private final FloatArrayList rowVs;
    private final FloatArrayList reaches;

    // Vertices
    private final IntArrayList vertices;
    private final FloatArrayList vertexVs;

    // Constructor \\

    public CharacterHairCardStruct(
            ObjectArrayList<Vector3> points,
            FloatArrayList rowVs,
            IntArrayList vertices,
            FloatArrayList vertexVs) {

        // Line
        this.points = points;
        this.rowVs = rowVs;
        this.reaches = new FloatArrayList(points.size());

        float reach = 0f;

        for (int i = 0; i < points.size(); i++) {

            if (i > 0)
                reach += distance(points.get(i - 1), points.get(i));

            reaches.add(reach);
        }

        // Vertices
        this.vertices = vertices;
        this.vertexVs = vertexVs;
    }

    // Line \\

    // The point the given distance along the line from its root, held to the line's ends
    public Vector3 sampleAt(float reach, Vector3 out) {

        int last = points.size() - 1;

        if (reach <= 0f || last == 0)
            return out.set(points.get(0));

        for (int i = 1; i <= last; i++)
            if (reach <= reaches.getFloat(i) || i == last) {

                float span = reaches.getFloat(i) - reaches.getFloat(i - 1);
                float t = span > 0f ? Math.min(1f, (reach - reaches.getFloat(i - 1)) / span) : 1f;
                Vector3 a = points.get(i - 1);
                Vector3 b = points.get(i);

                return out.set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
            }

        return out.set(points.get(last));
    }

    // How far along the line the row at a raw v lies, read between the rows about it
    public float reachAt(float v) {

        int last = rowVs.size() - 1;

        if (v <= rowVs.getFloat(0) || last == 0)
            return 0f;

        for (int i = 1; i <= last; i++)
            if (v <= rowVs.getFloat(i)) {

                float span = rowVs.getFloat(i) - rowVs.getFloat(i - 1);
                float t = span > 0f ? (v - rowVs.getFloat(i - 1)) / span : 1f;

                return reaches.getFloat(i - 1) + (reaches.getFloat(i) - reaches.getFloat(i - 1)) * t;
            }

        return reaches.getFloat(last);
    }

    private static float distance(Vector3 a, Vector3 b) {

        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float dz = b.z - a.z;

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Accessible \\

    public Vector3 getRoot() {
        return points.get(0);
    }

    public float getLength() {
        return reaches.getFloat(reaches.size() - 1);
    }

    public IntArrayList getVertices() {
        return vertices;
    }

    public int getVertex(int index) {
        return vertices.getInt(index);
    }

    public float getVertexV(int index) {
        return vertexVs.getFloat(index);
    }
}
