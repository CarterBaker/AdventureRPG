package engine.assets.camera;

import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;

public class OrthographicCameraData extends DataPackage {

    /*
     * Screen-space orthographic camera. Maps pixel coordinates with the origin
     * at the bottom-left corner to clip space for the current viewport size.
     */

    // Internal
    private final Matrix4 projectionMat;
    private final Vector2 screenSize;

    // Constructor \\

    public OrthographicCameraData(float width, float height) {

        this.projectionMat = new Matrix4();
        this.screenSize = new Vector2();

        applyViewport(width, height);
    }

    // Management \\

    public void updateViewport(float width, float height) {
        applyViewport(width, height);
    }

    private void applyViewport(float width, float height) {

        float safeWidth = Math.max(EngineSetting.CAMERA_MIN_VIEWPORT_DIMENSION, width);
        float safeHeight = Math.max(EngineSetting.CAMERA_MIN_VIEWPORT_DIMENSION, height);

        screenSize.set(safeWidth, safeHeight);
        projectionMat.set(
                2f / safeWidth, 0, 0, -1,
                0, 2f / safeHeight, 0, -1,
                0, 0, -1, 0,
                0, 0, 0, 1);
    }

    // Accessible \\

    public Matrix4 getProjection() {
        return projectionMat;
    }

    public Vector2 getScreenSize() {
        return screenSize;
    }
}
