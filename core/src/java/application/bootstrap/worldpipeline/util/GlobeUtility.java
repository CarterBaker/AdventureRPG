package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.GlobeChartStruct;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;

public final class GlobeUtility extends EngineUtility {

    /*
     * The geometry of a globe world. Its block grid is a cube of six square
     * faces in the standard cube map order (+X, -X, +Y, -Y, +Z, -Z), each a
     * flat grid whose x runs along the face's s axis, whose z runs along its
     * t axis and whose up is its outward normal, so every face is right handed
     * as the engine's own axes are. A chunk or block coordinate packs its face
     * into x, the faces laid a stride of spans apart with a span of margin
     * around each, so a place measured a little past a face's edge still names
     * the face it was measured from and folds onto the face it truly lies on. A place on a face is bent onto the ball by the equal-angle
     * mapping, which keeps cells close to the same size across the face, and
     * turned by the world's cube rotation into the globe's own frame, whose +Y
     * is the north pole and whose longitude runs from +Z toward +X; its
     * latitude, longitude and the heading of true north against the grid all
     * read from that direction. A grid works in a flat chart of the cube laid
     * out from an anchor cell: a step from the anchor is laid on the anchor's
     * face and folded over each edge it crosses onto the face beyond, the
     * step's axes turning by whole quarter turns as they fold. Beyond a face's
     * corner, where only three faces meet, the fold crosses first the edge on
     * the near side of the ray from the anchor through that corner, so the one
     * cut through the chart runs straight away from the anchor. The world's
     * images are a cube map of the globe in its own frame, laid on the same
     * face basis unturned, read where a direction falls by the standard cube
     * map rule; a texel past an image face's edge is the one its centre's
     * direction falls on, so filtering runs across image faces seamlessly.
     */

    private static final double TWO_PI = Math.PI * 2.0;
    private static final double HALF_PI = Math.PI * 0.5;
    private static final double QUARTER_PI = Math.PI * 0.25;

    // Face Basis — the outward normal, s axis and t axis of each face, in cube map order
    private static final int[][] FACE_NORMAL = {
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
    private static final int[][] FACE_S = {
            { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 0 } };
    private static final int[][] FACE_T = {
            { 0, -1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 }, { 0, -1, 0 }, { 0, -1, 0 } };

    // Span \\

    // Blocks along one side of a face: the image is as tall as one face
    public static int getFaceBlocks(WorldHandle worldHandle) {
        return worldHandle.getWorldScale().y;
    }

    public static int getFaceChunks(WorldHandle worldHandle) {
        return worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE;
    }

    // Layout \\

    // The face a canonical x lies on or measured from, for coordinates span to a face side
    public static int faceOf(long canonicalX, int span) {
        return (int) Math.floorDiv(
                canonicalX + (long) span * EngineSetting.GLOBE_FACE_MARGIN_SPANS,
                (long) span * EngineSetting.GLOBE_FACE_STRIDE_SPANS);
    }

    public static int faceOf(double canonicalX, int span) {
        return (int) Math.floor((canonicalX + (double) span * EngineSetting.GLOBE_FACE_MARGIN_SPANS)
                / ((double) span * EngineSetting.GLOBE_FACE_STRIDE_SPANS));
    }

    // Where a face's coordinates begin along canonical x
    public static long faceOriginX(int face, int span) {
        return (long) face * span * EngineSetting.GLOBE_FACE_STRIDE_SPANS;
    }

    // Chart \\

    // The frame of one chart cell — its canonical face cell and the turns from the chart's axes into that face's —
    // for a chart of cells span to a face side
    public static void resolveChartCell(int span, GlobeChartStruct chart, long chartCell, GlobeFrameStruct out) {

        long anchorCanonical = chart.getAnchorCanonical();
        int anchorTurns = chart.getAnchorTurns();
        int deltaX = Coordinate2Long.unpackX(chartCell) - Coordinate2Long.unpackX(chart.getAnchorChunk());
        int deltaZ = Coordinate2Long.unpackY(chartCell) - Coordinate2Long.unpackY(chart.getAnchorChunk());

        int face = faceOf(Coordinate2Long.unpackX(anchorCanonical), span);
        int anchorU = (int) (Coordinate2Long.unpackX(anchorCanonical) - faceOriginX(face, span));
        int anchorV = Coordinate2Long.unpackY(anchorCanonical);
        int stepU = turnX(deltaX, deltaZ, anchorTurns);
        int stepV = turnZ(deltaX, deltaZ, anchorTurns);

        foldPoint(
                span,
                face,
                2.0 * (anchorU + stepU) + 1.0 - span,
                2.0 * (anchorV + stepV) + 1.0 - span,
                2.0 * anchorU + 1.0 - span,
                2.0 * anchorV + 1.0 - span,
                anchorTurns,
                out);

        int u = (int) Math.round((out.getFoldA() - 1.0 + span) * 0.5);
        int v = (int) Math.round((out.getFoldB() - 1.0 + span) * 0.5);

        out.set(Coordinate2Long.pack((int) (faceOriginX(out.getFace(), span) + u), v), out.getTurns());
    }

    // Folds a point laid on a face's plane, given doubled about the face centre so the face spans -span to span on
    // both axes, over every edge it lies beyond onto the face there, carrying the quarter turns from the frame the
    // step was measured in into each face's axes. Beyond a corner it folds first over the edge on the near side of
    // the ray from the reference through that corner. The result is the folded point's face, its doubled place on
    // it and the turns into that face's axes
    public static void foldPoint(
            int span,
            int face,
            double a,
            double b,
            double referenceA,
            double referenceB,
            int turns,
            GlobeFrameStruct out) {

        int axisU = turnX(1, 0, turns);
        int axisV = turnZ(1, 0, turns);
        int[] normal = FACE_NORMAL[face];
        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];
        int carriedX = axisU * axisS[0] + axisV * axisT[0];
        int carriedY = axisU * axisS[1] + axisV * axisT[1];
        int carriedZ = axisU * axisS[2] + axisV * axisT[2];
        boolean first = true;

        while (Math.abs(a) > span || Math.abs(b) > span) {

            boolean foldS = Math.abs(a) > span
                    && (Math.abs(b) <= span || !first || prefersFoldS(span, referenceA, referenceB, a, b));
            int sign = foldS ? (a > 0.0 ? 1 : -1) : (b > 0.0 ? 1 : -1);
            double remainder = 2.0 * span - Math.abs(foldS ? a : b);
            int[] crossed = foldS ? axisS : axisT;
            int[] kept = foldS ? axisT : axisS;
            double keptOffset = foldS ? b : a;
            int nextFace = findFace(sign * crossed[0], sign * crossed[1], sign * crossed[2]);

            double pointX = remainder * normal[0] + keptOffset * kept[0];
            double pointY = remainder * normal[1] + keptOffset * kept[1];
            double pointZ = remainder * normal[2] + keptOffset * kept[2];

            int carriedKept = carriedX * kept[0] + carriedY * kept[1] + carriedZ * kept[2];
            int carriedCrossed = sign * (carriedX * crossed[0] + carriedY * crossed[1] + carriedZ * crossed[2]);
            int nextCarriedX = carriedKept * kept[0] - carriedCrossed * normal[0];
            int nextCarriedY = carriedKept * kept[1] - carriedCrossed * normal[1];
            int nextCarriedZ = carriedKept * kept[2] - carriedCrossed * normal[2];

            face = nextFace;
            normal = FACE_NORMAL[face];
            axisS = FACE_S[face];
            axisT = FACE_T[face];
            a = pointX * axisS[0] + pointY * axisS[1] + pointZ * axisS[2];
            b = pointX * axisT[0] + pointY * axisT[1] + pointZ * axisT[2];
            carriedX = nextCarriedX;
            carriedY = nextCarriedY;
            carriedZ = nextCarriedZ;
            first = false;
        }

        out.setFold(
                face,
                a,
                b,
                toTurns(
                        carriedX * axisS[0] + carriedY * axisS[1] + carriedZ * axisS[2],
                        carriedX * axisT[0] + carriedY * axisT[1] + carriedZ * axisT[2]));
    }

    // Places \\

    // Folds a block position in canonical coordinates, measured on or past the edges of the face its x names, onto
    // the face it truly lies on: the result's canonical position and the turns from the measured face's axes into
    // that face's, beyond a corner folded first over the edge nearer the face's centre line
    public static void foldBlock(WorldHandle worldHandle, double blockX, double blockZ, GlobeFrameStruct out) {

        int span = getFaceBlocks(worldHandle);
        int face = faceOf(blockX, span);

        foldPoint(span, face, 2.0 * (blockX - faceOriginX(face, span)) - span, 2.0 * blockZ - span, 0.0, 0.0, 0, out);
    }

    // The canonical x and z of the place a fold ended on
    public static double toFoldedX(WorldHandle worldHandle, GlobeFrameStruct fold) {

        int span = getFaceBlocks(worldHandle);

        return faceOriginX(fold.getFace(), span) + (fold.getFoldA() + span) * 0.5;
    }

    public static double toFoldedZ(WorldHandle worldHandle, GlobeFrameStruct fold) {
        return (fold.getFoldB() + getFaceBlocks(worldHandle)) * 0.5;
    }

    // A canonical block position laid out from a reference face: unchanged on the face itself, a face beside it
    // unfolded across the edge they share into the reference face's margin, as the reference face's own plane runs
    // on. The result is left in out's fold, its face the reference; a place on the far face is left where it lies
    public static void unfoldBlock(
            WorldHandle worldHandle,
            int referenceFace,
            double blockX,
            double blockZ,
            GlobeFrameStruct out) {

        int span = getFaceBlocks(worldHandle);
        int face = faceOf(blockX, span);
        double a = 2.0 * (blockX - faceOriginX(face, span)) - span;
        double b = 2.0 * blockZ - span;

        int[] normal = FACE_NORMAL[face];
        int[] reference = FACE_NORMAL[referenceFace];
        int facing = normal[0] * reference[0] + normal[1] * reference[1] + normal[2] * reference[2];

        if (face == referenceFace || facing != 0) {
            out.setFold(face, a, b, 0);
            return;
        }

        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];
        double pointX = a * axisS[0] + b * axisT[0];
        double pointY = a * axisS[1] + b * axisT[1];
        double pointZ = a * axisS[2] + b * axisT[2];
        double along = pointX * reference[0] + pointY * reference[1] + pointZ * reference[2];
        double outward = 2.0 * span - along;
        double unfoldedX = pointX - along * reference[0] + outward * normal[0];
        double unfoldedY = pointY - along * reference[1] + outward * normal[1];
        double unfoldedZ = pointZ - along * reference[2] + outward * normal[2];
        int[] referenceS = FACE_S[referenceFace];
        int[] referenceT = FACE_T[referenceFace];

        out.setFold(
                referenceFace,
                unfoldedX * referenceS[0] + unfoldedY * referenceS[1] + unfoldedZ * referenceS[2],
                unfoldedX * referenceT[0] + unfoldedY * referenceT[1] + unfoldedZ * referenceT[2],
                0);
    }

    // True while a cell within reach cells of a face corner, where the chart's cut can move
    public static boolean isNearCorner(int span, long canonicalCell, int reach) {

        int face = faceOf(Coordinate2Long.unpackX(canonicalCell), span);
        int u = (int) (Coordinate2Long.unpackX(canonicalCell) - faceOriginX(face, span));
        int v = Coordinate2Long.unpackY(canonicalCell);

        return Math.min(u, span - 1 - u) <= reach && Math.min(v, span - 1 - v) <= reach;
    }

    // Beyond a corner, folds over the s edge first when the point lies on the s edge's side of the ray from the
    // reference through the corner
    private static boolean prefersFoldS(int span, double referenceA, double referenceB, double a, double b) {

        double signA = a > 0.0 ? 1.0 : -1.0;
        double signB = b > 0.0 ? 1.0 : -1.0;
        double towardA = signA * span - referenceA;
        double towardB = signB * span - referenceB;
        double side = towardA * (b - signB * span) - towardB * (a - signA * span);

        return side == 0.0 || Math.signum(side) == Math.signum(-towardB * signA);
    }

    private static int findFace(int normalX, int normalY, int normalZ) {

        for (int face = 0; face < EngineSetting.GLOBE_FACE_COUNT; face++) {

            int[] normal = FACE_NORMAL[face];

            if (normal[0] == normalX && normal[1] == normalY && normal[2] == normalZ)
                return face;
        }

        return throwException("No globe face has the normal (" + normalX + ", " + normalY + ", " + normalZ + ").");
    }

    // Turns \\

    // A vector's x after turning it a number of quarter turns, each taking +X to +Z
    public static int turnX(int x, int z, int turns) {
        return switch (turns & 3) {
            case 0 -> x;
            case 1 -> -z;
            case 2 -> -x;
            default -> z;
        };
    }

    public static int turnZ(int x, int z, int turns) {
        return switch (turns & 3) {
            case 0 -> z;
            case 1 -> x;
            case 2 -> -z;
            default -> -x;
        };
    }

    public static double turnX(double x, double z, int turns) {
        return switch (turns & 3) {
            case 0 -> x;
            case 1 -> -z;
            case 2 -> -x;
            default -> z;
        };
    }

    public static double turnZ(double x, double z, int turns) {
        return switch (turns & 3) {
            case 0 -> z;
            case 1 -> x;
            case 2 -> -z;
            default -> -x;
        };
    }

    private static int toTurns(int carriedS, int carriedT) {

        if (carriedS > 0)
            return 0;

        if (carriedT > 0)
            return 1;

        return carriedS < 0 ? 2 : 3;
    }

    // Cube Rotation \\

    // The matrix taking a direction on the block grid's cube into the globe's frame, from degrees about X, Y and Z,
    // applied roll about Z first, then pitch about X, then yaw about Y
    public static void resolveGlobeFromCube(Vector3 degrees, Matrix3Double out) {

        double cx = Math.cos(Math.toRadians(degrees.x));
        double sx = Math.sin(Math.toRadians(degrees.x));
        double cy = Math.cos(Math.toRadians(degrees.y));
        double sy = Math.sin(Math.toRadians(degrees.y));
        double cz = Math.cos(Math.toRadians(degrees.z));
        double sz = Math.sin(Math.toRadians(degrees.z));

        out.set(
                cy * cz + sy * sx * sz, -cy * sz + sy * sx * cz, sy * cx,
                cx * sz, cx * cz, -sx,
                -sy * cz + cy * sx * sz, sy * sz + cy * sx * cz, cy * cx);
    }

    // Direction \\

    // The globe-frame direction of a block position, x and z in the packed face layout
    public static void resolveDirection(WorldHandle worldHandle, double blockX, double blockZ, Vector3Double out) {

        int faceBlocks = getFaceBlocks(worldHandle);
        int face = faceOf(blockX, faceBlocks);

        resolveFaceDirection(worldHandle, face, blockX - faceOriginX(face, faceBlocks), blockZ, out);
    }

    // The globe-frame direction of a place on one face, u and v in blocks from the face's corner
    public static void resolveFaceDirection(WorldHandle worldHandle, int face, double u, double v, Vector3Double out) {

        int faceBlocks = getFaceBlocks(worldHandle);
        double s = Math.tan(QUARTER_PI * (2.0 * u / faceBlocks - 1.0));
        double t = Math.tan(QUARTER_PI * (2.0 * v / faceBlocks - 1.0));

        int[] normal = FACE_NORMAL[face];
        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];

        rotate(
                worldHandle.getGlobeFromCube(),
                normal[0] + s * axisS[0] + t * axisT[0],
                normal[1] + s * axisS[1] + t * axisT[1],
                normal[2] + s * axisS[2] + t * axisT[2],
                out);
        out.normalize();
    }

    // The radius in blocks at which one block of a face spans one block of arc at the face's centre lines
    public static double getRadiusBlocks(WorldHandle worldHandle) {
        return 2.0 * getFaceBlocks(worldHandle) / Math.PI;
    }

    public static void resolveChunkDirection(WorldHandle worldHandle, long chunkCoordinate, Vector3Double out) {
        resolveDirection(
                worldHandle,
                (Coordinate2Long.unpackX(chunkCoordinate) + 0.5) * EngineSetting.CHUNK_SIZE,
                (Coordinate2Long.unpackY(chunkCoordinate) + 0.5) * EngineSetting.CHUNK_SIZE,
                out);
    }

    // Image \\

    // The image face and continuous texel position a globe-frame direction falls on, for faces faceTexels square
    public static void locateTexel(double x, double y, double z, int faceTexels, GlobeTexelStruct out) {

        double absoluteX = Math.abs(x);
        double absoluteY = Math.abs(y);
        double absoluteZ = Math.abs(z);
        int face;

        if (absoluteX >= absoluteY && absoluteX >= absoluteZ)
            face = x > 0.0 ? 0 : 1;
        else if (absoluteY >= absoluteZ)
            face = y > 0.0 ? 2 : 3;
        else
            face = z > 0.0 ? 4 : 5;

        int[] normal = FACE_NORMAL[face];
        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];
        double major = normal[0] * x + normal[1] * y + normal[2] * z;
        double s = (axisS[0] * x + axisS[1] * y + axisS[2] * z) / major;
        double t = (axisT[0] * x + axisT[1] * y + axisT[2] * z) / major;

        out.set(face, (s + 1.0) * 0.5 * faceTexels, (t + 1.0) * 0.5 * faceTexels);
    }

    // The pixel of a globe image holding a texel of one image face, the texel allowed past the face's edge — the
    // image's faces lie side by side, so the pixel's x packs its face
    public static long resolveImagePixel(int faceTexels, int face, int texelX, int texelY, GlobeTexelStruct scratch) {

        if (texelX >= 0 && texelX < faceTexels && texelY >= 0 && texelY < faceTexels)
            return Coordinate2Long.pack(face * faceTexels + texelX, texelY);

        double s = 2.0 * (texelX + 0.5) / faceTexels - 1.0;
        double t = 2.0 * (texelY + 0.5) / faceTexels - 1.0;
        int[] normal = FACE_NORMAL[face];
        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];

        locateTexel(
                normal[0] + s * axisS[0] + t * axisT[0],
                normal[1] + s * axisS[1] + t * axisT[1],
                normal[2] + s * axisS[2] + t * axisT[2],
                faceTexels,
                scratch);

        int x = Math.max(0, Math.min(faceTexels - 1, (int) Math.floor(scratch.getX())));
        int y = Math.max(0, Math.min(faceTexels - 1, (int) Math.floor(scratch.getY())));

        return Coordinate2Long.pack(scratch.getFace() * faceTexels + x, y);
    }

    // The block face a globe-frame direction points through and its place there in blocks from the face's corner,
    // the inverse of resolveFaceDirection, left in out as its face and position
    public static void locateBlock(
            WorldHandle worldHandle,
            double x,
            double y,
            double z,
            GlobeTexelStruct out) {

        Matrix3Double globeFromCube = worldHandle.getGlobeFromCube();
        double cubeX = globeFromCube.getM00() * x + globeFromCube.getM10() * y + globeFromCube.getM20() * z;
        double cubeY = globeFromCube.getM01() * x + globeFromCube.getM11() * y + globeFromCube.getM21() * z;
        double cubeZ = globeFromCube.getM02() * x + globeFromCube.getM12() * y + globeFromCube.getM22() * z;
        int faceBlocks = getFaceBlocks(worldHandle);

        locateTexel(cubeX, cubeY, cubeZ, faceBlocks, out);

        double s = Math.atan(2.0 * out.getX() / faceBlocks - 1.0) / QUARTER_PI;
        double t = Math.atan(2.0 * out.getY() / faceBlocks - 1.0) / QUARTER_PI;

        out.set(out.getFace(), (s + 1.0) * 0.5 * faceBlocks, (t + 1.0) * 0.5 * faceBlocks);
    }

    // The globe-frame direction, unnormalised, through a continuous position on a globe image, its face packed into x
    public static void resolveImageDirection(int faceTexels, double imageX, double imageY, Vector3Double out) {

        int face = Math.max(0, Math.min(EngineSetting.GLOBE_FACE_COUNT - 1, (int) Math.floor(imageX / faceTexels)));
        double s = 2.0 * (imageX - face * faceTexels) / faceTexels - 1.0;
        double t = 2.0 * imageY / faceTexels - 1.0;
        int[] normal = FACE_NORMAL[face];
        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];

        out.set(
                normal[0] + s * axisS[0] + t * axisT[0],
                normal[1] + s * axisS[1] + t * axisT[1],
                normal[2] + s * axisS[2] + t * axisT[2]);
    }

    // A continuous position on a globe image laid on one image face's plane, run on past its edges, as that face's
    // own texel position; false for a position facing away from that face, which no plane of it reaches
    public static boolean unfoldImageTexel(
            int faceTexels,
            int face,
            double imageX,
            double imageY,
            Vector3Double scratch,
            GlobeTexelStruct out) {

        resolveImageDirection(faceTexels, imageX, imageY, scratch);

        int[] normal = FACE_NORMAL[face];
        int[] axisS = FACE_S[face];
        int[] axisT = FACE_T[face];
        double major = normal[0] * scratch.x + normal[1] * scratch.y + normal[2] * scratch.z;

        if (major <= 0.0)
            return false;

        double s = (axisS[0] * scratch.x + axisS[1] * scratch.y + axisS[2] * scratch.z) / major;
        double t = (axisT[0] * scratch.x + axisT[1] * scratch.y + axisT[2] * scratch.z) / major;

        out.set(face, (s + 1.0) * 0.5 * faceTexels, (t + 1.0) * 0.5 * faceTexels);

        return true;
    }

    // Location \\

    // Latitude as a share of a right angle, 1 at the north pole and -1 at the south
    public static double resolveLatitudeFactor(Vector3Double direction) {
        return Math.asin(Math.max(-1.0, Math.min(1.0, direction.y))) / HALF_PI;
    }

    // Longitude as a fraction of a turn east of +Z
    public static double resolveLongitudeFraction(Vector3Double direction) {

        double fraction = Math.atan2(direction.x, direction.z) / TWO_PI;

        return fraction < 0.0 ? fraction + 1.0 : fraction;
    }

    // The angle in radians from the grid's +Z toward its +X at which true north lies, zero at either pole
    public static double resolveNorthHeading(WorldHandle worldHandle, long chunkCoordinate, Vector3Double direction) {

        int face = faceOf(Coordinate2Long.unpackX(chunkCoordinate), getFaceChunks(worldHandle));
        int[] axisT = FACE_T[face];
        Matrix3Double globeFromCube = worldHandle.getGlobeFromCube();

        double tx = globeFromCube.getM00() * axisT[0] + globeFromCube.getM01() * axisT[1]
                + globeFromCube.getM02() * axisT[2];
        double ty = globeFromCube.getM10() * axisT[0] + globeFromCube.getM11() * axisT[1]
                + globeFromCube.getM12() * axisT[2];
        double tz = globeFromCube.getM20() * axisT[0] + globeFromCube.getM21() * axisT[1]
                + globeFromCube.getM22() * axisT[2];
        double along = tx * direction.x + ty * direction.y + tz * direction.z;

        tx -= along * direction.x;
        ty -= along * direction.y;
        tz -= along * direction.z;

        double xx = direction.y * tz - direction.z * ty;
        double xy = direction.z * tx - direction.x * tz;
        double xz = direction.x * ty - direction.y * tx;

        double northX = -direction.y * direction.x;
        double northY = 1.0 - direction.y * direction.y;
        double northZ = -direction.y * direction.z;

        return Math.atan2(
                northX * xx + northY * xy + northZ * xz,
                northX * tx + northY * ty + northZ * tz);
    }

    // Math \\

    private static void rotate(Matrix3Double matrix, double x, double y, double z, Vector3Double out) {
        out.set(
                matrix.getM00() * x + matrix.getM01() * y + matrix.getM02() * z,
                matrix.getM10() * x + matrix.getM11() * y + matrix.getM12() * z,
                matrix.getM20() * x + matrix.getM21() * y + matrix.getM22() * z);
    }
}
