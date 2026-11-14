package application.bootstrap.geometrypipeline.mesh;

public enum MeshZone {

    /*
     * What a rigged mesh face is made of, so the character shader shades it as
     * its material: skin with its painted masks, the procedural eye, brows,
     * lashes, hair, cloth, the inside of the mouth and the teeth. A face names
     * its zone by name in the mesh file and the shader reads its ordinal, so
     * the order here is the order CharacterZone.glsl declares.
     */

    SKIN,
    EYE,
    BROW,
    LASH,
    HAIR,
    CLOTH,
    MOUTH,
    TEETH;

    // Values
    public static final MeshZone[] VALUES = values();
}
