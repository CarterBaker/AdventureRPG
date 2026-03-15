package com.internal.bootstrap.renderpipeline.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.internal.core.engine.DataPackage;
import com.internal.core.util.mathematics.matrices.Matrix4;
import com.internal.core.util.mathematics.vectors.Vector2;

public class OrthographicCameraData extends DataPackage {

    /*
     * Raw orthographic camera state. Owns the LibGDX OrthographicCamera and
     * all derived caches updated in place on every mutation.
     * Created with new — owned by OrthographicCameraInstance.
     */

    // Internal
    private final OrthographicCamera orthographicCamera;

    // Cached
    private final Matrix4 projectionMat;
    private final Vector2 screenSize;

    // Constructor \\

    public OrthographicCameraData(float width, float height) {

        // Cached
        this.projectionMat = new Matrix4();
        this.screenSize = new Vector2();

        // Internal
        this.orthographicCamera = new OrthographicCamera();
        this.orthographicCamera.setToOrtho(false, width, height);
        this.orthographicCamera.update();

        syncCaches(width, height);
    }

    // Mutation \\

    public void updateViewport(float width, float height) {
        orthographicCamera.setToOrtho(false, width, height);
        orthographicCamera.update();
        syncCaches(width, height);
    }

    private void syncCaches(float width, float height) {
        projectionMat.fromGDX(orthographicCamera.combined);
        screenSize.set(width, height);
    }

    // Accessible \\

    public Matrix4 getProjection() {
        return projectionMat;
    }

    public Vector2 getScreenSize() {
        return screenSize;
    }
}