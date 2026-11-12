package application.bootstrap.geometrypipeline.rig;

public enum RigColliderShape {

    /*
     * The shapes a rig's colliders take: a sphere about one point on its
     * bone, or a capsule, a sphere swept between two points on it — a head,
     * a neck, a shoulder, a chest — as physics assets and spring-bone
     * colliders build bodies for hair and cloth to rest against.
     */

    SPHERE,
    CAPSULE;

    public static final RigColliderShape[] VALUES = values();
}
