package application.bootstrap.geometrypipeline.rig;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class RigColliderStruct extends StructPackage {

    /*
     * One collision shape a rig's bone carries, laid out in that bone's bind
     * frame so it turns and moves with the bone: a sphere about one point,
     * or a capsule between two, of a radius in blocks. A sphere's two ends
     * are the same point. Spring chains naming it are pushed out of it as
     * they are simulated, so hair rests on the head and shoulders instead of
     * passing through them.
     */

    // Identity
    private final String name;
    private final int boneIndex;
    private final RigColliderShape shape;

    // Shape — both ends in the bone's frame, and the radius about the line between them
    private final Vector3 start;
    private final Vector3 end;
    private final float radius;

    // Constructor \\

    public RigColliderStruct(
            String name,
            int boneIndex,
            RigColliderShape shape,
            Vector3 start,
            Vector3 end,
            float radius) {

        // Identity
        this.name = name;
        this.boneIndex = boneIndex;
        this.shape = shape;

        // Shape
        this.start = start;
        this.end = end;
        this.radius = radius;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getBoneIndex() {
        return boneIndex;
    }

    public RigColliderShape getShape() {
        return shape;
    }

    public Vector3 getStart() {
        return start;
    }

    public Vector3 getEnd() {
        return end;
    }

    public float getRadius() {
        return radius;
    }
}
