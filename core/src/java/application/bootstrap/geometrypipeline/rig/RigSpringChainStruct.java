package application.bootstrap.geometrypipeline.rig;

import engine.root.StructPackage;

public class RigSpringChainStruct extends StructPackage {

    /*
     * One spring chain a rig carries: bones parent to child, the first hung
     * from a bone the body moves, each joint after the first a point the
     * runtime simulates rather than any clip keying it — a lock of long hair,
     * a tail, a cloak's corner. Stiffness pulls each joint back toward where
     * the pose holds it, damping settles its swing against the body, gravity
     * draws it down and drag holds it back against the air, so it trails,
     * bounces and settles as the head and body move. Its bones are as thick
     * as its radius, and they rest against the colliders it names, by their
     * place in the rig; a chain naming none passes through everything.
     */

    // Identity
    private final String name;

    // Bones — root to tip, and per bone of the chain every bone below it, itself first
    private final int[] bones;
    private final int[][] carried;

    // Settings
    private final float stiffness;
    private final float damping;
    private final float gravity;
    private final float drag;

    // Collision — how thick the chain's bones are, and the rig's colliders they are kept out of
    private final float radius;
    private final int[] colliders;

    // Constructor \\

    public RigSpringChainStruct(
            String name,
            int[] bones,
            int[][] carried,
            float stiffness,
            float damping,
            float gravity,
            float drag,
            float radius,
            int[] colliders) {

        // Identity
        this.name = name;

        // Bones
        this.bones = bones;
        this.carried = carried;

        // Settings
        this.stiffness = stiffness;
        this.damping = damping;
        this.gravity = gravity;
        this.drag = drag;

        // Collision
        this.radius = radius;
        this.colliders = colliders;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getBoneCount() {
        return bones.length;
    }

    public int getBone(int joint) {
        return bones[joint];
    }

    public int[] getCarried(int joint) {
        return carried[joint];
    }

    public boolean holds(int boneIndex) {

        for (int bone : bones)
            if (bone == boneIndex)
                return true;

        return false;
    }

    public float getStiffness() {
        return stiffness;
    }

    public float getDamping() {
        return damping;
    }

    public float getGravity() {
        return gravity;
    }

    public float getDrag() {
        return drag;
    }

    public float getRadius() {
        return radius;
    }

    public int getColliderCount() {
        return colliders.length;
    }

    // The rig collider the chain rests against, by its place in the rig
    public int getCollider(int index) {
        return colliders[index];
    }
}
