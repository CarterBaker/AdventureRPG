package application.runtime.inventory;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.itemdefinition.ContainerSpaceStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.runtime.RuntimeSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Int;

public class InventoryViewUtility extends EngineUtility {

    /*
     * The inventory's single set of transforms, shared by picking and
     * rendering. In its panel every container stands on its own floor, seen
     * through the panel's own camera looking down into it and framed to fill
     * the panel; a window point is unprojected through the inverse of both
     * into a container-space ray. A container opened where it lies is also
     * placed in the world, where only a space inside its own model is ever
     * drawn. Equipment icons still use a flat window projection.
     */

    // Projection \\

    public static void composeProjection(float width, float height, Matrix4 out) {
        out.set(
                2f / Math.max(1f, width), 0, 0, -1,
                0, 2f / Math.max(1f, height), 0, -1,
                0, 0, -1f / RuntimeSetting.INVENTORY_DEPTH_RANGE, 0,
                0, 0, 0, 1);
    }

    // Container Space \\

    // Ry(yaw) * S(block per sub-voxel) * T(-half footprint) — the space's floor centred on the origin
    public static void composeContainerMatrix(float yawDegrees, ContainerInstance containerInstance, Matrix4 out) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        double yaw = Math.toRadians(yawDegrees);
        float cosYaw = (float) Math.cos(yaw) * scale;
        float sinYaw = (float) Math.sin(yaw) * scale;

        out.set(
                cosYaw, 0, sinYaw, 0,
                0, scale, 0, 0,
                -sinYaw, 0, cosYaw, 0,
                0, 0, 0, 1)
                .multiply(
                        1, 0, 0, -containerInstance.getSizeX() * 0.5f,
                        0, 1, 0, 0,
                        0, 0, 1, -containerInstance.getSizeZ() * 0.5f,
                        0, 0, 0, 1);
    }

    // Shift(area centre) * Perspective * View — a camera pitched down at the space's centre, backed off until
    // the space's bounding sphere fills the area, then closer by the zoom
    public static void composePanelViewProjection(
            ElementInstance area,
            ContainerInstance containerInstance,
            float pitchDegrees,
            float zoom,
            float width,
            float height,
            Matrix4 out) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float sizeX = containerInstance.getSizeX() / resolution;
        float sizeY = containerInstance.getSizeY() / resolution;
        float sizeZ = containerInstance.getSizeZ() / resolution;
        float radius = (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) * 0.5f;
        float centerY = sizeY * 0.5f;

        float windowWidth = Math.max(1f, width);
        float windowHeight = Math.max(1f, height);
        float aspect = windowWidth / windowHeight;
        float tanHalfFov = (float) Math.tan(Math.toRadians(RuntimeSetting.INVENTORY_VIEW_FOV_DEGREES) * 0.5);
        float areaExtent = Math.min(area.getComputedH() / windowHeight, area.getComputedW() / windowWidth * aspect);
        float distance = radius / (tanHalfFov * Math.max(areaExtent, Float.MIN_NORMAL)
                * RuntimeSetting.INVENTORY_VIEW_FILL * zoom);

        float near = Math.max(RuntimeSetting.INVENTORY_VIEW_NEAR_MIN,
                distance - radius * RuntimeSetting.INVENTORY_VIEW_DEPTH_MARGIN);
        float far = distance + radius * RuntimeSetting.INVENTORY_VIEW_DEPTH_MARGIN;
        float focal = 1f / tanHalfFov;

        double pitch = Math.toRadians(pitchDegrees);
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);

        float shiftX = (area.getComputedLeft() + area.getComputedW() * 0.5f) / windowWidth * 2f - 1f;
        float shiftY = (area.getComputedTop() + area.getComputedH() * 0.5f) / windowHeight * 2f - 1f;

        out.set(
                1, 0, 0, shiftX,
                0, 1, 0, shiftY,
                0, 0, 1, 0,
                0, 0, 0, 1)
                .multiply(
                        focal / aspect, 0, 0, 0,
                        0, focal, 0, 0,
                        0, 0, (far + near) / (near - far), 2f * far * near / (near - far),
                        0, 0, -1, 0)
                .multiply(
                        1, 0, 0, 0,
                        0, cosPitch, -sinPitch, -cosPitch * centerY,
                        0, sinPitch, cosPitch, -sinPitch * centerY - distance,
                        0, 0, 0, 1);
    }

    // item * S(block per sub-voxel) * T(space offset) — a space inside the item's own model, where it stands
    public static void composeWorldContainerMatrix(Matrix4 itemMatrix, ItemDefinitionHandle item, Matrix4 out) {

        float scale = 1f / EngineSetting.SUB_VOXEL_RESOLUTION;
        Vector3Int offset = item.getContainerSpace().getOffset();

        out.set(itemMatrix).multiply(
                scale, 0, 0, offset.x * scale,
                0, scale, 0, offset.y * scale,
                0, 0, scale, offset.z * scale,
                0, 0, 0, 1);
    }

    // The shell around a space in its panel: a pocket's box, built in blocks from the space's corner, or the
    // container's own model, whose block holds the space at its offset — scaled back up to sub-voxels
    public static void composeShellMatrix(Matrix4 containerMatrix, ItemDefinitionHandle item, Matrix4 out) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        ContainerSpaceStruct space = item.getContainerSpace();

        out.set(containerMatrix);

        if (!space.isPocket())
            out.multiply(
                    1, 0, 0, -space.getOffset().x,
                    0, 1, 0, -space.getOffset().y,
                    0, 0, 1, -space.getOffset().z,
                    0, 0, 0, 1);

        out.multiply(
                resolution, 0, 0, 0,
                0, resolution, 0, 0,
                0, 0, resolution, 0,
                0, 0, 0, 1);
    }

    // container * T(place) * rotation * T(-shape offset) * S(sub-voxels per block)
    public static void composeItemMatrix(
            Matrix4 containerMatrix,
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

        out.set(containerMatrix)
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

        composeIconMatrix(
                centerX,
                centerY,
                size,
                shape.getOffsetX(),
                shape.getOffsetY(),
                shape.getOffsetZ(),
                shape.getSizeX(),
                shape.getSizeY(),
                shape.getSizeZ(),
                out);
    }

    // A box of model sub-voxels turned to the same three-quarter view and fitted inside a square of the given size
    public static void composeIconMatrix(
            float centerX,
            float centerY,
            float size,
            int minX,
            int minY,
            int minZ,
            int sizeX,
            int sizeY,
            int sizeZ,
            Matrix4 out) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float extent = (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) / resolution;

        composeTurn(
                out,
                RuntimeSetting.INVENTORY_ICON_PITCH_DEGREES,
                RuntimeSetting.INVENTORY_ICON_YAW_DEGREES,
                size * RuntimeSetting.INVENTORY_ICON_FILL / extent,
                centerX,
                centerY,
                (minX + sizeX * 0.5f) / resolution,
                (minY + sizeY * 0.5f) / resolution,
                (minZ + sizeZ * 0.5f) / resolution);
    }

    // Picking \\

    // The ray under a window point, unprojected through the inverse of view projection * container matrix
    public static void castRay(
            Matrix4 inversePickMatrix,
            float width,
            float height,
            float x,
            float y,
            Vector3 origin,
            Vector3 direction) {

        float ndcX = x / Math.max(1f, width) * 2f - 1f;
        float ndcY = y / Math.max(1f, height) * 2f - 1f;

        unproject(inversePickMatrix, ndcX, ndcY, -1f, origin);
        unproject(inversePickMatrix, ndcX, ndcY, 1f, direction);

        direction.subtract(origin).normalize();
    }

    private static void unproject(Matrix4 inverse, float ndcX, float ndcY, float ndcZ, Vector3 out) {

        float[] m = inverse.val;
        float w = m[3] * ndcX + m[7] * ndcY + m[11] * ndcZ + m[15];

        out.set(
                (m[0] * ndcX + m[4] * ndcY + m[8] * ndcZ + m[12]) / w,
                (m[1] * ndcX + m[5] * ndcY + m[9] * ndcZ + m[13]) / w,
                (m[2] * ndcX + m[6] * ndcY + m[10] * ndcZ + m[14]) / w);
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
