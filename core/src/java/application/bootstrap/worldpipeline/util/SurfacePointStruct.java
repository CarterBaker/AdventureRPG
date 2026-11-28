package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3Double;

public class SurfacePointStruct extends StructPackage {

    /*
     * One place on a world's ground as everything that samples the ground
     * reads it: noise, the world's images and anything hashed by position.
     * Positions are given in the coordinates of a home chunk — the chart a
     * grid holds that chunk in — and resolve to the place they stand. A torus
     * place is its own coordinate, read by the circle its X rides and the
     * periods it wraps after. A globe place is folded from the home chunk's
     * own frame onto the face it lies on: its face, its place there in blocks,
     * the turns from the home's axes into that face's, its true direction in
     * the globe's frame and its point on the ball, at the radius where a block
     * spans a block of arc, which three-dimensional noise reads with no seam,
     * and where that direction falls on the world's images.
     * One struct per thread, homed once per chunk and resolved per sample.
     */

    private static final double TWO_PI = Math.PI * 2.0;

    // Home
    private WorldHandle worldHandle;
    private boolean globe;
    private int faceBlocks;
    private double radiusBlocks;
    private double homeChartX;
    private double homeChartZ;
    private int homeFace;
    private double homeU;
    private double homeV;
    private int homeTurns;

    // Point
    private double x;
    private double z;
    private int face;
    private double u;
    private double v;
    private int turns;
    private final Vector3Double direction = new Vector3Double();
    private final GlobeTexelStruct texel = new GlobeTexelStruct();
    private final GlobeTexelStruct texelScratch = new GlobeTexelStruct();

    // Torus
    private double cosAngle;
    private double sinAngle;

    // Fold
    private final GlobeFrameStruct fold = new GlobeFrameStruct();

    // Home \\

    // Homes the point on a chunk a grid holds at chartChunk, standing on canonicalChunk turned by turns
    public void setHome(WorldHandle worldHandle, long chartChunk, long canonicalChunk, int turns) {

        this.worldHandle = worldHandle;
        this.globe = worldHandle.isGlobe();
        this.homeChartX = (Coordinate2Long.unpackX(chartChunk) + 0.5) * EngineSetting.CHUNK_SIZE;
        this.homeChartZ = (Coordinate2Long.unpackY(chartChunk) + 0.5) * EngineSetting.CHUNK_SIZE;
        this.homeFace = 0;
        this.homeTurns = 0;

        if (!globe)
            return;

        int faceChunks = GlobeUtility.getFaceChunks(worldHandle);
        int canonicalX = Coordinate2Long.unpackX(canonicalChunk);

        this.faceBlocks = GlobeUtility.getFaceBlocks(worldHandle);
        this.radiusBlocks = GlobeUtility.getRadiusBlocks(worldHandle);
        this.homeFace = GlobeUtility.faceOf(canonicalX, faceChunks);
        this.homeU = (canonicalX - GlobeUtility.faceOriginX(homeFace, faceChunks) + 0.5) * EngineSetting.CHUNK_SIZE;
        this.homeV = (Coordinate2Long.unpackY(canonicalChunk) + 0.5) * EngineSetting.CHUNK_SIZE;
        this.homeTurns = turns;
    }

    // Homes the point on the canonical chunk a canonical block position lies in, unturned, so positions given are
    // the world's own coordinates
    public void setCanonicalHome(WorldHandle worldHandle, double blockX, double blockZ) {

        long chunk = Coordinate2Long.pack(
                Math.floorDiv((int) Math.floor(blockX), EngineSetting.CHUNK_SIZE),
                Math.floorDiv((int) Math.floor(blockZ), EngineSetting.CHUNK_SIZE));

        setHome(worldHandle, chunk, chunk, 0);
    }

    public void copyHome(SurfacePointStruct other) {

        this.worldHandle = other.worldHandle;
        this.globe = other.globe;
        this.faceBlocks = other.faceBlocks;
        this.radiusBlocks = other.radiusBlocks;
        this.homeChartX = other.homeChartX;
        this.homeChartZ = other.homeChartZ;
        this.homeFace = other.homeFace;
        this.homeU = other.homeU;
        this.homeV = other.homeV;
        this.homeTurns = other.homeTurns;
    }

    // Resolve \\

    // The place a position in the home's coordinates stands
    public SurfacePointStruct resolve(double x, double z) {

        this.x = x;
        this.z = z;

        if (!globe) {

            double spatialAngle = (x / worldHandle.getWorldScale().x) * TWO_PI;

            this.turns = 0;

            this.cosAngle = Math.cos(spatialAngle);
            this.sinAngle = Math.sin(spatialAngle);
            return this;
        }

        double offsetX = x - homeChartX;
        double offsetZ = z - homeChartZ;
        double referenceA = 2.0 * homeU - faceBlocks;
        double referenceB = 2.0 * homeV - faceBlocks;

        GlobeUtility.foldPoint(
                faceBlocks,
                homeFace,
                referenceA + 2.0 * GlobeUtility.turnX(offsetX, offsetZ, homeTurns),
                referenceB + 2.0 * GlobeUtility.turnZ(offsetX, offsetZ, homeTurns),
                referenceA,
                referenceB,
                homeTurns,
                fold);

        this.face = fold.getFace();
        this.u = (fold.getFoldA() + faceBlocks) * 0.5;
        this.v = (fold.getFoldB() + faceBlocks) * 0.5;
        this.turns = fold.getTurns();

        GlobeUtility.resolveFaceDirection(worldHandle, face, u, v, direction);
        GlobeUtility.locateTexel(
                direction.x, direction.y, direction.z, worldHandle.getWorld().getHeight(), texel);

        return this;
    }

    // Home Frame \\

    // A position in the home's coordinates laid on the home face's own plane, run on past its edges — a torus
    // home's plane is its own coordinates
    public double toFaceU(double x, double z) {
        return globe ? homeU + GlobeUtility.turnX(x - homeChartX, z - homeChartZ, homeTurns) : x;
    }

    public double toFaceV(double x, double z) {
        return globe ? homeV + GlobeUtility.turnZ(x - homeChartX, z - homeChartZ, homeTurns) : z;
    }

    // A place on the home face's plane in the home's coordinates
    public double toChartX(double faceU, double faceV) {
        return globe ? homeChartX + GlobeUtility.turnX(faceU - homeU, faceV - homeV, -homeTurns) : faceU;
    }

    public double toChartZ(double faceU, double faceV) {
        return globe ? homeChartZ + GlobeUtility.turnZ(faceU - homeU, faceV - homeV, -homeTurns) : faceV;
    }

    public int getHomeFace() {
        return homeFace;
    }

    public double getHomeU() {
        return homeU;
    }

    public double getHomeV() {
        return homeV;
    }

    public int getHomeTurns() {
        return homeTurns;
    }

    public int getFaceBlocks() {
        return faceBlocks;
    }

    // Blocks \\

    // The place a block of the home's coordinates truly stands, read at its centre
    public SurfacePointStruct resolveBlock(long x, long z) {
        return resolve(x + EngineSetting.BLOCK_CENTER_OFFSET, z + EngineSetting.BLOCK_CENTER_OFFSET);
    }

    // The canonical block the last place resolved lies in
    public long getCanonicalBlockX() {
        return (long) Math.floor(getCanonicalX());
    }

    public long getCanonicalBlockZ() {
        return (long) Math.floor(getCanonicalZ());
    }

    // Where a canonical block stands in the home's coordinates — on a globe laid out from the home face, a face
    // beside it unfolded across their edge and the far face a stride off, out of every reach; on a torus wrapped
    // into the world
    public long toChartBlockX(long canonicalX, long canonicalZ) {

        if (!globe)
            return WorldWrapUtility.wrapBlockX(worldHandle, canonicalX);

        if (!unfoldFromHome(canonicalX, canonicalZ))
            return (long) homeChartX - (long) faceBlocks * EngineSetting.GLOBE_FACE_STRIDE_SPANS;

        return (long) Math.floor(toChartX(unfoldedU(), unfoldedV()));
    }

    public long toChartBlockZ(long canonicalX, long canonicalZ) {

        if (!globe)
            return WorldWrapUtility.wrapBlockZ(worldHandle, canonicalZ);

        if (!unfoldFromHome(canonicalX, canonicalZ))
            return (long) homeChartZ - (long) faceBlocks * EngineSetting.GLOBE_FACE_STRIDE_SPANS;

        return (long) Math.floor(toChartZ(unfoldedU(), unfoldedV()));
    }

    // False for a place on the face opposite the home's, which no home reaches: it is held a stride away
    private boolean unfoldFromHome(long canonicalX, long canonicalZ) {

        GlobeUtility.unfoldBlock(
                worldHandle,
                homeFace,
                canonicalX + EngineSetting.BLOCK_CENTER_OFFSET,
                canonicalZ + EngineSetting.BLOCK_CENTER_OFFSET,
                fold);

        return fold.getFace() == homeFace;
    }

    private double unfoldedU() {
        return (fold.getFoldA() + faceBlocks) * 0.5;
    }

    private double unfoldedV() {
        return (fold.getFoldB() + faceBlocks) * 0.5;
    }

    // Home Block \\

    // The canonical block a column of the home chunk stands on, from its place in the chunk — what a column is
    // hashed by, so it draws the same wherever a chart lays it
    public long toHomeBlockX(int localX, int localZ) {

        if (!globe)
            return (long) (homeChartX - EngineSetting.CHUNK_SIZE * 0.5) + localX;

        double offsetX = localX + 0.5 - EngineSetting.CHUNK_SIZE * 0.5;
        double offsetZ = localZ + 0.5 - EngineSetting.CHUNK_SIZE * 0.5;

        return GlobeUtility.faceOriginX(homeFace, faceBlocks)
                + Math.round(homeU + GlobeUtility.turnX(offsetX, offsetZ, homeTurns) - 0.5);
    }

    public long toHomeBlockZ(int localX, int localZ) {

        if (!globe)
            return (long) (homeChartZ - EngineSetting.CHUNK_SIZE * 0.5) + localZ;

        double offsetX = localX + 0.5 - EngineSetting.CHUNK_SIZE * 0.5;
        double offsetZ = localZ + 0.5 - EngineSetting.CHUNK_SIZE * 0.5;

        return Math.round(homeV + GlobeUtility.turnZ(offsetX, offsetZ, homeTurns) - 0.5);
    }

    // Accessible \\

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public boolean isGlobe() {
        return globe;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    // The canonical block position of a globe place, its face packed into x
    public double getCanonicalX() {
        return globe ? GlobeUtility.faceOriginX(face, faceBlocks) + u : x;
    }

    public double getCanonicalZ() {
        return globe ? v : z;
    }

    public int getFace() {
        return face;
    }

    public double getFaceU() {
        return u;
    }

    public double getFaceV() {
        return v;
    }

    public int getTurns() {
        return turns;
    }

    public Vector3Double getDirection() {
        return direction;
    }

    // Where a globe place falls on the world's images
    public GlobeTexelStruct getTexel() {
        return texel;
    }

    // Scratch for reading the images past an image face's edge around this place
    public GlobeTexelStruct getTexelScratch() {
        return texelScratch;
    }

    public double getNoiseX() {
        return direction.x * radiusBlocks;
    }

    public double getNoiseY() {
        return direction.y * radiusBlocks;
    }

    public double getNoiseZ() {
        return direction.z * radiusBlocks;
    }

    public double getCosAngle() {
        return cosAngle;
    }

    public double getSinAngle() {
        return sinAngle;
    }

    // The blocks a torus wraps after along each axis
    public double getWrapWidth() {
        return worldHandle.getWorldScale().x;
    }

    public double getWrapHeight() {
        return worldHandle.getWorldScale().y;
    }
}
