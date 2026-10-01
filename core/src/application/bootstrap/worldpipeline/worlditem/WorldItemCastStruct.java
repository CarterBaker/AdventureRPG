package application.bootstrap.worldpipeline.worlditem;

import engine.root.StructPackage;
import engine.util.mathematics.extras.Direction3Vector;

public class WorldItemCastStruct extends StructPackage {

    /*
     * Output container for a single world item raycast. Passed into the cast
     * and written in place — never allocated per cast. The anchor is the
     * sub-voxel cell just outside the face the ray entered, in the hit item's
     * own chunk-local sub-voxels, which is where an item placed against that
     * face starts from.
     */

    // Internal
    private boolean hit;
    private float distance;
    private WorldItemInstance worldItemInstance;
    private Direction3Vector hitFace;

    // Anchor
    private long chunkCoordinate;
    private int anchorX;
    private int anchorY;
    private int anchorZ;

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

    public WorldItemInstance getWorldItemInstance() {
        return worldItemInstance;
    }

    public void setWorldItemInstance(WorldItemInstance worldItemInstance) {
        this.worldItemInstance = worldItemInstance;
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

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public int getAnchorZ() {
        return anchorZ;
    }

    public void setAnchor(int anchorX, int anchorY, int anchorZ) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }
}
