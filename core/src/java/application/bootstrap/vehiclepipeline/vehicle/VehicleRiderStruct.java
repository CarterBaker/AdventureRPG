package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class VehicleRiderStruct extends StructPackage {

    /*
     * An entity riding a vehicle: the vehicle, where the centre of the
     * entity's feet stood in its model blocks after the entity last moved,
     * the vehicle's heading at that moment, and how far the vehicle has turned
     * under the entity since its owner last took that turn, so a rider's view
     * can turn with the deck.
     */

    // Internal
    private VehicleInstance vehicleInstance;

    // Position
    private final Vector3 feet;
    private float heading;

    // Turn
    private float pendingTurn;

    // Constructor \\

    public VehicleRiderStruct() {

        // Position
        this.feet = new Vector3();
    }

    // Management \\

    public void board(VehicleInstance vehicleInstance) {

        if (this.vehicleInstance != vehicleInstance)
            this.pendingTurn = 0f;

        this.vehicleInstance = vehicleInstance;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }

    public void addTurn(float turn) {
        pendingTurn += turn;
    }

    // The turn gathered since it was last taken, cleared as it is read
    public float takeTurn() {

        float turn = pendingTurn;
        pendingTurn = 0f;

        return turn;
    }

    // Accessible \\

    public VehicleInstance getVehicleInstance() {
        return vehicleInstance;
    }

    public Vector3 getFeet() {
        return feet;
    }

    public float getHeading() {
        return heading;
    }
}
