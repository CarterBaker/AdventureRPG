package com.internal.bootstrap.renderpipeline.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.matrices.Matrix4;
import com.internal.core.util.mathematics.vectors.Vector2;

public class OrthographicCameraInstance extends InstancePackage {

    private OrthographicCamera orthographicCamera;

    // Cached values
    private Matrix4 projectionMat = new Matrix4();
    private Vector2 screenSize = new Vector2();

    public void constructor(float width, float height) {
        orthographicCamera = new OrthographicCamera();
        orthographicCamera.setToOrtho(false, width, height);
        orthographicCamera.update();
        updateCachedValues(width, height);
    }

    public void updateViewport(float width, float height) {
        orthographicCamera.setToOrtho(false, width, height);
        orthographicCamera.update();
        updateCachedValues(width, height);
    }

    private void updateCachedValues(float width, float height) {
        projectionMat.fromGDX(orthographicCamera.combined);
        screenSize.set(width, height);
    }

    public Matrix4 getProjection() {
        return projectionMat;
    }

    public Vector2 getScreenSize() {
        return screenSize;
    }
}