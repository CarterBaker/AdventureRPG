package application.bootstrap.geometrypipeline.rig;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class RigIkChainStruct extends StructPackage {

    /*
     * One IK handle a rig carries: three bones parent to child — the root a
     * limb turns from, the middle that bends and the end whose joint the
     * handle drags — and the pole, the direction in the character's space the
     * middle joint bends toward, so an elbow folds back, a knee forward and a
     * finger toward the palm however far the end is pulled.
     */

    // Identity
    private final String name;

    // Bones
    private final int rootBone;
    private final int middleBone;
    private final int endBone;

    // Bend
    private final Vector3 pole;

    // Constructor \\

    public RigIkChainStruct(String name, int rootBone, int middleBone, int endBone, Vector3 pole) {

        // Identity
        this.name = name;

        // Bones
        this.rootBone = rootBone;
        this.middleBone = middleBone;
        this.endBone = endBone;

        // Bend
        this.pole = pole;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getRootBone() {
        return rootBone;
    }

    public int getMiddleBone() {
        return middleBone;
    }

    public int getEndBone() {
        return endBone;
    }

    public Vector3 getPole() {
        return pole;
    }
}
