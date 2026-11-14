package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterHairLockStruct extends StructPackage {

    /*
     * One lock of long hair found in a hair mesh, the cards hanging together
     * from one place on the scalp: the joints a spring chain is laid on in the
     * bind pose from its root to its tip along the line they share, and how
     * thick the lock is.
     */

    // Chain
    private final ObjectArrayList<Vector3> joints;
    private final float thickness;

    // Constructor \\

    public CharacterHairLockStruct(ObjectArrayList<Vector3> joints, float thickness) {

        // Chain
        this.joints = joints;
        this.thickness = thickness;
    }

    // Accessible \\

    public int getJointCount() {
        return joints.size();
    }

    public Vector3 getJoint(int joint) {
        return joints.get(joint);
    }

    public Vector3 getRoot() {
        return joints.get(0);
    }

    public float getThickness() {
        return thickness;
    }
}
