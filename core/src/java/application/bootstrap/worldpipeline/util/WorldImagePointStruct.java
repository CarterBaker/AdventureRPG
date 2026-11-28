package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3Double;

public class WorldImagePointStruct extends StructPackage {

    /*
     * The one way between a world's blocks and its images, both ways. On a
     * torus an image pixel spans CHUNKS_PER_PIXEL chunks along each axis and
     * positions wrap with the world. On a globe a block's direction falls on
     * the cube map image where the standard cube map rule puts it, its face
     * packed into the image's x, and an image position's direction lands on
     * the block cube's face it truly points through, wherever the cube is
     * turned, so a design stood on the globe image stands where it is shown.
     * Image positions keep their fractions; blocks are the columns they lie
     * in. One struct per owner, its last answer read through its getters.
     */

    // Image
    private double imageX;
    private double imageY;

    // Block
    private long blockX;
    private long blockZ;

    // Scratch
    private final Vector3Double direction = new Vector3Double();
    private final GlobeTexelStruct texel = new GlobeTexelStruct();

    // Locate \\

    // The image position a block position falls on
    public void locateImage(WorldHandle worldHandle, double worldX, double worldZ) {

        if (!worldHandle.isGlobe()) {
            this.imageX = WorldWrapUtility.wrapBlockX(worldHandle, worldX) / resolvePixelBlocks();
            this.imageY = WorldWrapUtility.wrapBlockZ(worldHandle, worldZ) / resolvePixelBlocks();
            return;
        }

        int faceTexels = worldHandle.getWorld().getHeight();

        GlobeUtility.resolveDirection(worldHandle, worldX, worldZ, direction);
        GlobeUtility.locateTexel(direction.x, direction.y, direction.z, faceTexels, texel);

        this.imageX = texel.getFace() * (double) faceTexels + texel.getX();
        this.imageY = texel.getY();
    }

    // The block column an image position falls on
    public void locateBlock(WorldHandle worldHandle, double imageX, double imageY) {

        if (!worldHandle.isGlobe()) {
            this.blockX = (long) Math.floor(imageX * resolvePixelBlocks());
            this.blockZ = (long) Math.floor(imageY * resolvePixelBlocks());
            return;
        }

        GlobeUtility.resolveImageDirection(worldHandle.getWorld().getHeight(), imageX, imageY, direction);
        direction.normalize();
        GlobeUtility.locateBlock(worldHandle, direction.x, direction.y, direction.z, texel);

        int faceBlocks = GlobeUtility.getFaceBlocks(worldHandle);

        this.blockX = GlobeUtility.faceOriginX(texel.getFace(), faceBlocks) + (long) Math.floor(texel.getX());
        this.blockZ = (long) Math.floor(texel.getY());
    }

    // Image Delta \\

    // How far one image position lies from another along each axis: across the wrap on a torus, plainly on a globe
    public double imageDeltaX(WorldHandle worldHandle, double a, double b) {
        return WorldWrapUtility.wrappedDelta(a, b, worldHandle.isGlobe() ? 0.0 : worldHandle.getWorld().getWidth());
    }

    public double imageDeltaY(WorldHandle worldHandle, double a, double b) {
        return WorldWrapUtility.wrappedDelta(a, b, worldHandle.isGlobe() ? 0.0 : worldHandle.getWorld().getHeight());
    }

    // Utility \\

    // The blocks one image pixel spans, along a torus's axes and across a globe face's middle alike
    public static double resolvePixelBlocks() {
        return EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
    }

    // Accessible \\

    public double getImageX() {
        return imageX;
    }

    public double getImageY() {
        return imageY;
    }

    public long getBlockX() {
        return blockX;
    }

    public long getBlockZ() {
        return blockZ;
    }
}
