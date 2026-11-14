package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;

class VehicleMotionBranch extends BranchPackage {

    /*
     * Moves a vehicle as one rigid body. Gravity pulls through its centre of
     * mass along the world's own gravity, and every force gathered over the
     * sub-step then moves it by semi-implicit Euler: its velocity first, then
     * its spin through its inertia turned into world axes, gyroscopic term
     * included, then its position and its orientation along what they have
     * become. Speed and spin are capped so no stiff contact can ever fling it.
     */

    // Scratch
    private Vector3 bodyScratch;
    private Quaternion spinScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.bodyScratch = new Vector3();
        this.spinScratch = new Quaternion();
    }

    // Gravity \\

    void applyGravity(VehicleInstance vehicle) {

        WorldHandle worldHandle = vehicle.getWorldHandle();
        Vector3 direction = worldHandle.getGravityDirection();
        float length = direction.length();

        if (length <= 0f)
            return;

        float weight = EngineSetting.GRAVITY_FORCE * worldHandle.getGravityMultiplier()
                * vehicle.getVehicleHandle().getHull().getMass() / length;

        vehicle.applyForce(direction.x * weight, direction.y * weight, direction.z * weight, 0f, 0f, 0f);
    }

    // Integrate \\

    void integrate(VehicleInstance vehicle, float subStep) {

        integrateVelocity(vehicle, subStep);
        integrateSpin(vehicle, subStep);
        integratePose(vehicle, subStep);
    }

    private void integrateVelocity(VehicleInstance vehicle, float subStep) {

        Vector3 velocity = vehicle.getVelocity();
        Vector3 force = vehicle.getForce();
        float inverseMass = 1f / vehicle.getVehicleHandle().getHull().getMass();

        velocity.add(force.x * inverseMass * subStep, force.y * inverseMass * subStep, force.z * inverseMass * subStep);
        clampLength(velocity, EngineSetting.VEHICLE_MAX_SPEED);
    }

    // dw/dt = I^-1 (torque - w x I w), worked in model axes where the inertia is diagonal
    private void integrateSpin(VehicleInstance vehicle, float subStep) {

        Vector3 inertia = vehicle.getVehicleHandle().getHull().getInertia();
        Vector3 spin = vehicle.getAngularVelocity();
        Vector3 torque = vehicle.getTorque();
        float[] m = vehicle.getRotation().val;

        float spinX = m[0] * spin.x + m[1] * spin.y + m[2] * spin.z;
        float spinY = m[4] * spin.x + m[5] * spin.y + m[6] * spin.z;
        float spinZ = m[8] * spin.x + m[9] * spin.y + m[10] * spin.z;

        float momentumX = inertia.x * spinX;
        float momentumY = inertia.y * spinY;
        float momentumZ = inertia.z * spinZ;

        float torqueX = m[0] * torque.x + m[1] * torque.y + m[2] * torque.z;
        float torqueY = m[4] * torque.x + m[5] * torque.y + m[6] * torque.z;
        float torqueZ = m[8] * torque.x + m[9] * torque.y + m[10] * torque.z;

        bodyScratch.set(
                (torqueX - (spinY * momentumZ - spinZ * momentumY)) / inertia.x,
                (torqueY - (spinZ * momentumX - spinX * momentumZ)) / inertia.y,
                (torqueZ - (spinX * momentumY - spinY * momentumX)) / inertia.z);

        spin.add(
                (m[0] * bodyScratch.x + m[4] * bodyScratch.y + m[8] * bodyScratch.z) * subStep,
                (m[1] * bodyScratch.x + m[5] * bodyScratch.y + m[9] * bodyScratch.z) * subStep,
                (m[2] * bodyScratch.x + m[6] * bodyScratch.y + m[10] * bodyScratch.z) * subStep);

        clampLength(spin, EngineSetting.VEHICLE_MAX_SPIN);
    }

    // dq/dt = 1/2 (0, w) q, with w in world axes
    private void integratePose(VehicleInstance vehicle, float subStep) {

        Vector3 velocity = vehicle.getVelocity();
        Vector3 spin = vehicle.getAngularVelocity();
        Quaternion orientation = vehicle.getOrientation();
        float half = subStep * 0.5f;

        vehicle.getWorldPositionStruct().getPosition().add(
                velocity.x * subStep,
                velocity.y * subStep,
                velocity.z * subStep);

        spinScratch.set(0f, spin.x, spin.y, spin.z).multiply(orientation);
        orientation.add(
                spinScratch.getW() * half,
                spinScratch.getX() * half,
                spinScratch.getY() * half,
                spinScratch.getZ() * half).normalize();

        vehicle.refreshRotation();
    }

    // Utility \\

    private void clampLength(Vector3 vector, float limit) {

        float length = vector.length();

        if (length > limit)
            vector.multiply(limit / length);
    }
}
