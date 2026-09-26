package application.runtime.inventory;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class InventoryViewUtility extends EngineUtility {

    /*
     * The inventory's single set of view transforms, shared by picking and
     * rendering. Places a container's sub-voxel box inside its menu element,
     * turned and tipped toward the viewer, and inverts that to turn a cursor
     * point into a ray.
     */

    // Shell faces — origin, u edge, v edge, and inward normal, each scaled by the container size
    private static final float[][] SHELL_FACES = {
            { 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0 },
            { 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1 },
            { 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, -1 },
            { 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0 },
            { 1, 0, 0, 0, 0, 1, 0, 1, 0, -1, 0, 0 }
    };

    // Projection \\

    public static void composeProjection(float width, float height, Matrix4 out) {
        out.set(
                2f / Math.max(1f, width), 0, 0, -1,
                0, 2f / Math.max(1f, height), 0, -1,
                0, 0, -1f / RuntimeSetting.INVENTORY_DEPTH_RANGE, 0,
                0, 0, 0, 1);
    }

    // Container View \\

    public static void composeViewMatrix(
            ElementInstance viewElement,
            ContainerInstance containerInstance,
            float yawDegrees,
            Matrix4 out) {

        float sizeX = containerInstance.getSizeX();
        float sizeY = containerInstance.getSizeY();
        float sizeZ = containerInstance.getSizeZ();
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(RuntimeSetting.INVENTORY_VIEW_PITCH_DEGREES);

        float cosYaw = (float) Math.abs(Math.cos(yaw));
        float sinYaw = (float) Math.abs(Math.sin(yaw));
        float projectedWidth = sizeX * cosYaw + sizeZ * sinYaw;
        float projectedDepth = sizeX * sinYaw + sizeZ * cosYaw;
        float projectedHeight = (float) (sizeY * Math.cos(pitch) + projectedDepth * Math.sin(pitch));

        float fill = RuntimeSetting.INVENTORY_VIEW_FILL;
        float scale = Math.min(
                viewElement.getComputedW() * fill / projectedWidth,
                viewElement.getComputedH() * fill / projectedHeight);

        composeTurn(
                out,
                RuntimeSetting.INVENTORY_VIEW_PITCH_DEGREES,
                yawDegrees,
                scale,
                viewElement.getComputedLeft() + viewElement.getComputedW() * 0.5f,
                viewElement.getComputedTop() + viewElement.getComputedH() * 0.5f,
                sizeX * 0.5f,
                sizeY * 0.5f,
                sizeZ * 0.5f);
    }

    // view * T(place) * rotation * T(-shape offset) * S(sub-voxels per block)
    public static void composeItemMatrix(
            Matrix4 viewMatrix,
            ItemShapeStruct shape,
            int x,
            int y,
            int z,
            int rotation,
            Matrix4 out,
            Matrix4 scratch) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        shape.composeRotation(scratch, rotation).multiply(
                resolution, 0, 0, -shape.getOffsetX(),
                0, resolution, 0, -shape.getOffsetY(),
                0, 0, resolution, -shape.getOffsetZ(),
                0, 0, 0, 1);

        out.set(viewMatrix)
                .multiply(
                        1, 0, 0, x,
                        0, 1, 0, y,
                        0, 0, 1, z,
                        0, 0, 0, 1)
                .multiply(scratch);
    }

    // Icon \\

    // An item turned to a three-quarter view and fitted inside a square of the given size
    public static void composeIconMatrix(
            float centerX,
            float centerY,
            float size,
            ItemShapeStruct shape,
            Matrix4 out) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float extent = (float) Math.sqrt(
                shape.getSizeX() * shape.getSizeX()
                        + shape.getSizeY() * shape.getSizeY()
                        + shape.getSizeZ() * shape.getSizeZ())
                / resolution;

        composeTurn(
                out,
                RuntimeSetting.INVENTORY_ICON_PITCH_DEGREES,
                RuntimeSetting.INVENTORY_ICON_YAW_DEGREES,
                size * RuntimeSetting.INVENTORY_ICON_FILL / extent,
                centerX,
                centerY,
                (shape.getOffsetX() + shape.getSizeX() * 0.5f) / resolution,
                (shape.getOffsetY() + shape.getSizeY() * 0.5f) / resolution,
                (shape.getOffsetZ() + shape.getSizeZ() * 0.5f) / resolution);
    }

    // Shell \\

    public static int getShellFaceCount() {
        return SHELL_FACES.length;
    }

    // A face is drawn only when its inward side turns toward the viewer, so no wall hides the contents
    public static boolean isShellFaceVisible(Matrix4 viewMatrix, int face) {

        float[] f = SHELL_FACES[face];

        return viewMatrix.val[2] * f[9] + viewMatrix.val[6] * f[10] + viewMatrix.val[10] * f[11] > 0f;
    }

    public static void composeShellMatrix(
            Matrix4 viewMatrix,
            ContainerInstance containerInstance,
            int face,
            Matrix4 out) {

        float[] f = SHELL_FACES[face];
        float sizeX = containerInstance.getSizeX();
        float sizeY = containerInstance.getSizeY();
        float sizeZ = containerInstance.getSizeZ();

        out.set(viewMatrix).multiply(
                f[3] * sizeX, f[9], f[6] * sizeX, f[0] * sizeX,
                f[4] * sizeY, f[10], f[7] * sizeY, f[1] * sizeY,
                f[5] * sizeZ, f[11], f[8] * sizeZ, f[2] * sizeZ,
                0, 0, 0, 1);
    }

    // How many grid cells the face shows along its u and v edges
    public static Vector2 resolveShellCells(ContainerInstance containerInstance, int face, Vector2 out) {

        float[] f = SHELL_FACES[face];
        float step = RuntimeSetting.INVENTORY_GRID_STEP;
        float lengthU = f[3] * containerInstance.getSizeX()
                + f[4] * containerInstance.getSizeY()
                + f[5] * containerInstance.getSizeZ();
        float lengthV = f[6] * containerInstance.getSizeX()
                + f[7] * containerInstance.getSizeY()
                + f[8] * containerInstance.getSizeZ();

        return out.set(lengthU / step, lengthV / step);
    }

    // Picking \\

    public static void castRay(
            Matrix4 inverseViewMatrix,
            float x,
            float y,
            Vector3 origin,
            Vector3 direction) {

        float[] m = inverseViewMatrix.val;
        float z = RuntimeSetting.INVENTORY_DEPTH_RANGE;

        origin.set(
                m[0] * x + m[4] * y + m[8] * z + m[12],
                m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14]);
        direction.set(-m[8], -m[9], -m[10]).normalize();
    }

    // Where a ray meets the container floor, false when it runs level or upward
    public static boolean intersectFloor(Vector3 origin, Vector3 direction, Vector3 out) {

        if (direction.y >= 0f)
            return false;

        float distance = -origin.y / direction.y;

        out.set(
                origin.x + direction.x * distance,
                0f,
                origin.z + direction.z * distance);

        return true;
    }

    public static boolean isInside(ElementInstance element, float x, float y) {
        return x >= element.getComputedLeft()
                && x <= element.getComputedLeft() + element.getComputedW()
                && y >= element.getComputedTop()
                && y <= element.getComputedTop() + element.getComputedH();
    }

    // Utility \\

    // T(center) * scale * Rx(pitch) * Ry(yaw) * T(-pivot)
    private static void composeTurn(
            Matrix4 out,
            float pitchDegrees,
            float yawDegrees,
            float scale,
            float centerX,
            float centerY,
            float pivotX,
            float pivotY,
            float pivotZ) {

        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);

        float m00 = scale * cosYaw;
        float m01 = 0f;
        float m02 = scale * sinYaw;
        float m10 = scale * sinPitch * sinYaw;
        float m11 = scale * cosPitch;
        float m12 = -scale * sinPitch * cosYaw;
        float m20 = -scale * cosPitch * sinYaw;
        float m21 = scale * sinPitch;
        float m22 = scale * cosPitch * cosYaw;

        out.set(
                m00, m01, m02, centerX - (m00 * pivotX + m01 * pivotY + m02 * pivotZ),
                m10, m11, m12, centerY - (m10 * pivotX + m11 * pivotY + m12 * pivotZ),
                m20, m21, m22, -(m20 * pivotX + m21 * pivotY + m22 * pivotZ),
                0, 0, 0, 1);
    }
}
