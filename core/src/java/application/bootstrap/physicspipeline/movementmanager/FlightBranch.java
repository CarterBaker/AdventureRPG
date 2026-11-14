package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class FlightBranch extends BranchPackage {

    /*
     * Computes free flight displacement each frame from the entity's
     * EntityInputHandle for MovementManager.fly(). No gravity, liquid,
     * collision, or acceleration ramp — forward and back follow the full
     * facing direction including pitch, strafing stays level, jump rises,
     * walk sinks, and sprint multiplies the flight speed.
     */

    // Settings
    private float flightSpeed;
    private float flightSprintMultiplier;

    // Cached Vectors
    private Vector3 right;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.flightSpeed = EngineSetting.FREE_CAMERA_FLIGHT_SPEED;
        this.flightSprintMultiplier = EngineSetting.FREE_CAMERA_SPRINT_MULTIPLIER;

        // Cached Vectors
        this.right = new Vector3();
    }

    // Movement \\

    void calculate(Vector3 movement, EntityInstance entity) {

        EntityInputHandle input = entity.getEntityInputHandle();
        Vector3 facing = input.getFacingDirection();

        // Right — perpendicular to facing, kept level
        right.x = -facing.z;
        right.y = 0f;
        right.z = facing.x;
        right.normalize();

        int inputX = input.getHorizontalX();
        int inputZ = input.getHorizontalZ();
        int inputY = (input.isJump() ? 1 : 0) - (input.isWalk() ? 1 : 0);

        movement.x = facing.x * inputZ + right.x * inputX;
        movement.y = facing.y * inputZ + inputY;
        movement.z = facing.z * inputZ + right.z * inputX;

        if (!movement.hasValues())
            return;

        float speed = input.isSprint() ? flightSpeed * flightSprintMultiplier : flightSpeed;

        movement.normalize();
        movement.multiply(speed * internal.getDeltaTime());
    }
}
