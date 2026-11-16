package editor.bootstrap.stamppipeline.stamp;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class StampSocketStruct extends StructPackage {

    /*
     * One open edge loop of a stamp, in the stamp's own space: its plug, the
     * loop it joins a host by, or a socket another stamp may join it at. It
     * names itself, the stamp bone it rides, the loop's centre, the way it
     * faces out of the part and its mean radius, so a part placed on it is
     * set where, which way and how large the loop asks.
     */

    // Identity
    private final String socketName;
    private final int boneIndex;

    // Placement
    private final Vector3 position;
    private final Vector3 normal;
    private final float radius;

    // Constructor \\

    public StampSocketStruct(String socketName, int boneIndex, Vector3 position, Vector3 normal, float radius) {

        // Identity
        this.socketName = socketName;
        this.boneIndex = boneIndex;

        // Placement
        this.position = position;
        this.normal = normal;
        this.radius = radius;
    }

    // Accessible \\

    public String getSocketName() {
        return socketName;
    }

    // The bone of the stamp's own rig it rides, the host placeholder for a plug
    public int getBoneIndex() {
        return boneIndex;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public float getRadius() {
        return radius;
    }
}
