package application.bootstrap.worldpipeline.tree;

import engine.root.StructPackage;

public class TreeImpostorStruct extends StructPackage {

    /*
     * One species' baked impostor, as its file holds it: the checksum of the
     * species and bake it was made from, the shared tree's height and the
     * sphere about its trunk every frame was framed on, in blocks from the
     * centre of its root, and the albedo and normal atlases as BC7 chains from
     * the whole atlas down to one texel. Immutable once read, so any thread
     * may hold it.
     */

    // Source
    private final long source;

    // Frame — blocks
    private final float height;
    private final float centerY;
    private final float radius;

    // Maps
    private final byte[] albedo;
    private final byte[] normal;

    // Constructor \\

    public TreeImpostorStruct(long source, float height, float centerY, float radius, byte[] albedo, byte[] normal) {

        // Source
        this.source = source;

        // Frame
        this.height = height;
        this.centerY = centerY;
        this.radius = radius;

        // Maps
        this.albedo = albedo;
        this.normal = normal;
    }

    // Accessible \\

    public long getSource() {
        return source;
    }

    public float getHeight() {
        return height;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }

    public byte[] getAlbedo() {
        return albedo;
    }

    public byte[] getNormal() {
        return normal;
    }
}
