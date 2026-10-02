package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Direction3Vector;

public class VehicleCastStruct extends StructPackage {

    /*
     * Output container for one ray cast against the vehicles. Written in place,
     * never allocated per cast. A hit names the vehicle, and either the part
     * struck or the cargo struck; a solid sub-voxel or a cargo cell keeps the
     * face the ray entered in the vehicle's model axes and the anchor, the
     * model sub-voxel just outside that face, where cargo set against it
     * starts from.
     */

    // Internal
    private boolean hit;
    private float distance;
    private VehicleInstance vehicleInstance;

    // Target
    private int partIndex;
    private VehicleCargoInstance cargoInstance;

    // Face
    private boolean faceHit;
    private Direction3Vector hitFace;
    private int anchorX;
    private int anchorY;
    private int anchorZ;

    // Management \\

    public void clear(float distance) {

        this.hit = false;
        this.distance = distance;
        this.vehicleInstance = null;
        this.partIndex = EngineSetting.INDEX_NOT_FOUND;
        this.cargoInstance = null;
        this.faceHit = false;
    }

    public void setPart(VehicleInstance vehicleInstance, float distance, int partIndex) {

        this.hit = true;
        this.distance = distance;
        this.vehicleInstance = vehicleInstance;
        this.partIndex = partIndex;
        this.cargoInstance = null;
        this.faceHit = false;
    }

    public void setCargo(VehicleInstance vehicleInstance, float distance, VehicleCargoInstance cargoInstance) {

        this.hit = true;
        this.distance = distance;
        this.vehicleInstance = vehicleInstance;
        this.partIndex = EngineSetting.INDEX_NOT_FOUND;
        this.cargoInstance = cargoInstance;
        this.faceHit = false;
    }

    public void setFace(Direction3Vector hitFace, int anchorX, int anchorY, int anchorZ) {

        this.faceHit = true;
        this.hitFace = hitFace;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    // Accessible \\

    public boolean isHit() {
        return hit;
    }

    public float getDistance() {
        return distance;
    }

    public VehicleInstance getVehicleInstance() {
        return vehicleInstance;
    }

    public boolean isPartHit() {
        return hit && partIndex != EngineSetting.INDEX_NOT_FOUND;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public boolean isCargoHit() {
        return hit && cargoInstance != null;
    }

    public VehicleCargoInstance getCargoInstance() {
        return cargoInstance;
    }

    public boolean isFaceHit() {
        return faceHit;
    }

    public Direction3Vector getHitFace() {
        return hitFace;
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
}
