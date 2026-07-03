package application.bootstrap.geometrypipeline.rig;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class RigBoneStruct extends StructPackage {

    /*
     * Immutable bind-pose descriptor for a single bone within a RigData.
     * position is the bone's joint offset from its parent's joint, in the
     * parent's local bind-pose space. rotation is the bind-pose local
     * rotation (Euler degrees, XYZ order) — normally zero for a rest pose.
     * size is the reference cuboid dimensions of the body part rigidly
     * associated with this bone, used only for mesh authoring and debug
     * visualization — never read during pose evaluation.
     */

    // Identity
    private final String name;
    private final int parentIndex;

    // Bind Pose
    private final Vector3 position;
    private final Vector3 rotation;
    private final Vector3 size;

    // Constructor \\

    public RigBoneStruct(
            String name,
            int parentIndex,
            Vector3 position,
            Vector3 rotation,
            Vector3 size) {

        // Identity
        this.name = name;
        this.parentIndex = parentIndex;

        // Bind Pose
        this.position = position;
        this.rotation = rotation;
        this.size = size;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getParentIndex() {
        return parentIndex;
    }

    public boolean isRoot() {
        return parentIndex == EngineSetting.INDEX_NOT_FOUND;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public Vector3 getSize() {
        return size;
    }
}