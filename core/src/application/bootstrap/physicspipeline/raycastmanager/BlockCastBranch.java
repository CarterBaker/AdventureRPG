package application.bootstrap.physicspipeline.raycastmanager;

import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector3;

class BlockCastBranch extends BranchPackage {

    /*
     * Performs DDA raycasting from a world position along a direction at
     * sub-block resolution, so a ray passes through the empty octants of a
     * subdivided block and stops on the exact sub-block it meets. Writes the
     * hit block's cell, the octant hit within it and the face entered into
     * a caller-supplied BlockCastStruct — no allocation per cast. Crosses
     * chunk boundaries transparently during traversal.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;

    // Settings
    private int chunkSize;
    private int subVoxelResolution;
    private int divisions;
    private float subBlockSize;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
        this.subVoxelResolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.divisions = SubBlockUtility.DIVISIONS;
        this.subBlockSize = SubBlockUtility.SIZE;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Cast \\

    void cast(
            long chunkCoordinate,
            Vector3 rayOrigin,
            Vector3 direction,
            float maxDistance,
            BlockCastStruct out) {

        out.setHit(false);

        int subX = SubBlockSampleUtility.toSub(rayOrigin.x);
        int subY = SubBlockSampleUtility.toSub(rayOrigin.y);
        int subZ = SubBlockSampleUtility.toSub(rayOrigin.z);

        int stepX = direction.x > 0 ? 1 : (direction.x < 0 ? -1 : 0);
        int stepY = direction.y > 0 ? 1 : (direction.y < 0 ? -1 : 0);
        int stepZ = direction.z > 0 ? 1 : (direction.z < 0 ? -1 : 0);

        float tDeltaX = stepX != 0 ? Math.abs(subBlockSize / direction.x) : Float.MAX_VALUE;
        float tDeltaY = stepY != 0 ? Math.abs(subBlockSize / direction.y) : Float.MAX_VALUE;
        float tDeltaZ = stepZ != 0 ? Math.abs(subBlockSize / direction.z) : Float.MAX_VALUE;

        float tMaxX = resolveFirstCrossing(rayOrigin.x, direction.x, subX, stepX);
        float tMaxY = resolveFirstCrossing(rayOrigin.y, direction.y, subY, stepY);
        float tMaxZ = resolveFirstCrossing(rayOrigin.z, direction.z, subZ, stepZ);

        Direction3Vector lastFace = null;
        float t = 0f;

        while (t < maxDistance) {

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX;
                if (t >= maxDistance)
                    break;
                subX += stepX;
                tMaxX += tDeltaX;
                lastFace = Direction3Vector.getDirectionX(-stepX);
            } else if (tMaxY < tMaxZ) {
                t = tMaxY;
                if (t >= maxDistance)
                    break;
                subY += stepY;
                tMaxY += tDeltaY;
                lastFace = Direction3Vector.getDirectionY(-stepY);
            } else {
                t = tMaxZ;
                if (t >= maxDistance)
                    break;
                subZ += stepZ;
                tMaxZ += tDeltaZ;
                lastFace = Direction3Vector.getDirectionZ(-stepZ);
            }

            BlockHandle block = SubBlockSampleUtility.getSubBlockAt(
                    worldStreamManager, blockManager, chunkCoordinate, subX, subY, subZ);

            if (block == null)
                continue;

            writeHit(out, chunkCoordinate, rayOrigin, direction, t, subX, subY, subZ, lastFace, block);
            return;
        }
    }

    private float resolveFirstCrossing(float origin, float direction, int sub, int step) {

        if (step > 0)
            return ((sub + 1) * subBlockSize - origin) / direction;

        if (step < 0)
            return (origin - sub * subBlockSize) / -direction;

        return Float.MAX_VALUE;
    }

    // Hit \\

    private void writeHit(
            BlockCastStruct out,
            long chunkCoordinate,
            Vector3 rayOrigin,
            Vector3 direction,
            float t,
            int subX, int subY, int subZ,
            Direction3Vector hitFace,
            BlockHandle block) {

        int cellX = Math.floorDiv(subX, divisions);
        int cellY = Math.floorDiv(subY, divisions);
        int cellZ = Math.floorDiv(subZ, divisions);

        float hitX = rayOrigin.x + direction.x * t;
        float hitY = rayOrigin.y + direction.y * t;
        float hitZ = rayOrigin.z + direction.z * t;

        out.setHitSubX(toSubVoxel(hitX - cellX));
        out.setHitSubY(toSubVoxel(hitY - cellY));
        out.setHitSubZ(toSubVoxel(hitZ - cellZ));

        out.setHit(true);
        out.setChunkCoordinate(Coordinate2Long.add(
                chunkCoordinate,
                Math.floorDiv(cellX, chunkSize),
                Math.floorDiv(cellZ, chunkSize)));
        out.setBlockX(Math.floorMod(cellX, chunkSize));
        out.setBlockY(cellY % chunkSize);
        out.setBlockZ(Math.floorMod(cellZ, chunkSize));
        out.setSubChunkY(cellY / chunkSize);
        out.setHitOctant(SubBlockUtility.getOctant(
                Math.floorMod(subX, divisions),
                Math.floorMod(subY, divisions),
                Math.floorMod(subZ, divisions)));
        out.setHitFace(hitFace);
        out.setDistance(t);
        out.setBlock(block);
    }

    private int toSubVoxel(float offsetInBlock) {
        return Math.max(0, Math.min(subVoxelResolution - 1, (int) (offsetInBlock * subVoxelResolution)));
    }
}
