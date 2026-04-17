package engine.assets.camera;

import engine.root.DataPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;

public class OrthographicCameraData extends DataPackage {

    private final Matrix4 projectionMat;
    private final Vector2 screenSize;

    public OrthographicCameraData(float width, float height) {
        this.projectionMat = new Matrix4();
        this.screenSize = new Vector2();
        updateViewport(width, height);
    }

    public void updateViewport(float width, float height) {
        float safeWidth = Math.max(1f, width);
        float safeHeight = Math.max(1f, height);

        screenSize.set(safeWidth, safeHeight);
        projectionMat.set(
                2f / safeWidth, 0, 0, -1,
                0, 2f / safeHeight, 0, -1,
                0, 0, -1, 0,
                0, 0, 0, 1);
    }

    public Matrix4 getProjection() {
        return projectionMat;
    }

    public Vector2 getScreenSize() {
        return screenSize;
    }
}