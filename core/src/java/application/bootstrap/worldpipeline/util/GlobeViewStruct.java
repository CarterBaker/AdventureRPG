package application.bootstrap.worldpipeline.util;

import engine.assets.camera.CameraInstance;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;

public class GlobeViewStruct extends StructPackage {

    /*
     * A globe of unit radius at the origin as one window sees it through its
     * camera: the camera's place, its forward axis and its side axes scaled
     * to the window's edges, read off the camera's own pick rays, so a window
     * position's ray is forward plus its offset from the window's middle, in
     * normalised device units, along each side axis. The rays a globe's
     * shader casts and the rays the pointer picks with are therefore one and
     * the same. A window position strikes the ball where its ray first meets
     * it; a point falls in the window where it projects, a point of the ball
     * shown only on the side facing the camera.
     */

    // Camera
    private final Vector3 position = new Vector3();
    private final Vector3 forward = new Vector3();
    private final Vector3 right = new Vector3();
    private final Vector3 up = new Vector3();

    // Projected
    private float projectedX;
    private float projectedY;

    // Scratch
    private final Vector3 rayOrigin = new Vector3();
    private final Vector3 rayDirection = new Vector3();

    // Camera \\

    // The camera's place and axes, read off its pick rays through the window's middle and edges
    public void resolveCamera(CameraInstance camera) {

        camera.getPickRay(0f, 0f, rayOrigin, forward);
        camera.getPickRay(1f, 0f, rayOrigin, rayDirection);
        stretchToForward(rayDirection, right);
        camera.getPickRay(0f, 1f, rayOrigin, rayDirection);
        stretchToForward(rayDirection, up);
        position.set(rayOrigin);
    }

    public void copy(GlobeViewStruct other) {
        position.set(other.position);
        forward.set(other.forward);
        right.set(other.right);
        up.set(other.up);
    }

    // A unit ray stretched to stand one forward unit ahead, the forward axis taken off it
    private void stretchToForward(Vector3 ray, Vector3 out) {

        float along = ray.x * forward.x + ray.y * forward.y + ray.z * forward.z;

        out.set(ray.x / along - forward.x, ray.y / along - forward.y, ray.z / along - forward.z);
    }

    // Strike \\

    // The unit direction where a window position's ray first meets the ball, false when it misses
    public boolean strike(CameraInstance camera, float ndcX, float ndcY, Vector3Double out) {

        camera.getPickRay(ndcX, ndcY, rayOrigin, rayDirection);

        double along = rayOrigin.x * rayDirection.x + rayOrigin.y * rayDirection.y + rayOrigin.z * rayDirection.z;
        double beyond = rayOrigin.x * rayOrigin.x + rayOrigin.y * rayOrigin.y + rayOrigin.z * rayOrigin.z - 1.0;
        double reach = along * along - beyond;

        if (reach < 0.0)
            return false;

        double distance = -along - Math.sqrt(reach);

        if (distance < 0.0)
            return false;

        out.set(
                rayOrigin.x + rayDirection.x * distance,
                rayOrigin.y + rayDirection.y * distance,
                rayOrigin.z + rayDirection.z * distance).normalize();

        return true;
    }

    // Project \\

    // Where a point falls in a window of the size given, in pixels with y up; false behind the camera
    public boolean project(double x, double y, double z, float width, float height) {

        double offsetX = x - position.x;
        double offsetY = y - position.y;
        double offsetZ = z - position.z;
        double depth = offsetX * forward.x + offsetY * forward.y + offsetZ * forward.z;

        if (depth <= 0.0)
            return false;

        double across = (offsetX * right.x + offsetY * right.y + offsetZ * right.z)
                / (depth * (right.x * right.x + right.y * right.y + right.z * right.z));
        double along = (offsetX * up.x + offsetY * up.y + offsetZ * up.z)
                / (depth * (up.x * up.x + up.y * up.y + up.z * up.z));

        this.projectedX = (float) ((across + 1.0) * 0.5 * width);
        this.projectedY = (float) ((along + 1.0) * 0.5 * height);

        return true;
    }

    // Where a unit direction on the ball falls, false on the ball's far side or behind the camera
    public boolean projectSurface(double x, double y, double z, float width, float height) {

        double facing = x * (position.x - x) + y * (position.y - y) + z * (position.z - z);

        return facing > 0.0 && project(x, y, z, width, height);
    }

    // Measure \\

    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(square(x - position.x) + square(y - position.y) + square(z - position.z));
    }

    // The span one window pixel covers at a distance from the camera, in globe radii
    public double resolveSpanPerPixel(double distance, float height) {
        return 2.0 * distance * Math.sqrt(up.x * up.x + up.y * up.y + up.z * up.z) / height;
    }

    public double getCameraDistance() {
        return Math.sqrt(position.x * position.x + position.y * position.y + position.z * position.z);
    }

    private static double square(double value) {
        return value * value;
    }

    // Accessible \\

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getForward() {
        return forward;
    }

    public Vector3 getRight() {
        return right;
    }

    public Vector3 getUp() {
        return up;
    }

    public float getProjectedX() {
        return projectedX;
    }

    public float getProjectedY() {
        return projectedY;
    }
}
