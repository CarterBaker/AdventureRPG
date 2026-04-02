package program.bootstrap.physicspipeline.util;

import program.bootstrap.worldpipeline.block.BlockHandle;
import program.core.engine.StructPackage;
import program.core.util.mathematics.extras.Direction3Vector;

public class BlockCastStruct extends StructPackage {

    /*
     * Output container for a single block raycast result. Passed into cast
     * methods and written in place — never allocated per cast.
     */

    // Internal
    private boolean hit;
    private float distance;
    private Direction3Vector hitFace;
    private long chunkCoordinate;
    private int subChunkY;
    private BlockHandle block;
    private int blockX;
    private int blockY;
    private int blockZ;
    private int hitSubX;
    private int hitSubY;
    private int hitSubZ;

    // Accessible \\

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public Direction3Vector getHitFace() {
        return hitFace;
    }

    public void setHitFace(Direction3Vector hitFace) {
        this.hitFace = hitFace;
    }

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public void setChunkCoordinate(long chunkCoordinate) {
        this.chunkCoordinate = chunkCoordinate;
    }

    public int getSubChunkY() {
        return subChunkY;
    }

    public void setSubChunkY(int subChunkY) {
        this.subChunkY = subChunkY;
    }

    public BlockHandle getBlock() {
        return block;
    }

    public void setBlock(BlockHandle block) {
        this.block = block;
    }

    public int getBlockX() {
        return blockX;
    }

    public void setBlockX(int blockX) {
        this.blockX = blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public void setBlockY(int blockY) {
        this.blockY = blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    public void setBlockZ(int blockZ) {
        this.blockZ = blockZ;
    }

    public int getHitSubX() {
        return hitSubX;
    }

    public void setHitSubX(int hitSubX) {
        this.hitSubX = hitSubX;
    }

    public int getHitSubY() {
        return hitSubY;
    }

    public void setHitSubY(int hitSubY) {
        this.hitSubY = hitSubY;
    }

    public int getHitSubZ() {
        return hitSubZ;
    }

    public void setHitSubZ(int hitSubZ) {
        this.hitSubZ = hitSubZ;
    }
}