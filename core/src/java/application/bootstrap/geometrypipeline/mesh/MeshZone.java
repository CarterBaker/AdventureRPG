package application.bootstrap.geometrypipeline.mesh;

public enum MeshZone {

    /*
     * What a rigged mesh face is made of, so the character shader shades it as
     * its material: skin with its painted masks, the procedural eye, brows,
     * lashes, hair, cloth dyed the character's cloth color, the inside of
     * the mouth, the teeth, and gear shown in its own texture untinted, as
     * metal and leather armor is. A face names its zone by name in the mesh
     * file and the shader reads its ordinal, so the order here is the order
     * CharacterZone.glsl declares.
     */

    SKIN,
    EYE,
    BROW,
    LASH,
    HAIR,
    CLOTH,
    MOUTH,
    TEETH,
    GEAR;

    // Values
    public static final MeshZone[] VALUES = values();
}
