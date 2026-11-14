package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterDigitStruct extends StructPackage {

    /*
     * One digit found in a region of a body mesh: a finger, the thumb, or the
     * one toe of a foot whose toes are not modelled apart. It carries the name
     * its bones take, the joints its chain is laid on in the bind pose, root
     * to tip, the tip itself, how thick it is, and the vertices it alone
     * carries; a toe found at the ball of a foot carries those past the ball.
     */

    // Identity
    private final String name;

    // Chain
    private final ObjectArrayList<Vector3> joints;
    private final Vector3 tip;
    private final float thickness;

    // Vertices
    private final IntArrayList vertices;

    // Constructor \\

    public CharacterDigitStruct(
            String name,
            ObjectArrayList<Vector3> joints,
            Vector3 tip,
            float thickness,
            IntArrayList vertices) {

        // Identity
        this.name = name;

        // Chain
        this.joints = joints;
        this.tip = tip;
        this.thickness = thickness;

        // Vertices
        this.vertices = vertices;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public ObjectArrayList<Vector3> getJoints() {
        return joints;
    }

    public int getJointCount() {
        return joints.size();
    }

    public Vector3 getJoint(int joint) {
        return joints.get(joint);
    }

    // Where a joint's segment ends: the next joint, or the tip for the last
    public Vector3 getSegmentEnd(int joint) {
        return joint + 1 < joints.size() ? joints.get(joint + 1) : tip;
    }

    public Vector3 getTip() {
        return tip;
    }

    public float getThickness() {
        return thickness;
    }

    public IntArrayList getVertices() {
        return vertices;
    }

    public boolean carries(int vertexIndex) {
        return vertices.contains(vertexIndex);
    }
}
