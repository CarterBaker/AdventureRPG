package com.internal.bootstrap.renderpipeline.camera;

import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.matrices.Matrix4;
import com.internal.core.util.mathematics.vectors.Vector2;

public class OrthographicCameraInstance extends InstancePackage {

    /*
     * Runtime orthographic camera. Wraps OrthographicCameraData and delegates
     * all mutation and access through it. Owned by CameraManager.
     */

    // Internal
    private OrthographicCameraData data;

    // Constructor \\

    public void constructor(OrthographicCameraData data) {
        this.data = data;
    }

    // Accessible \\

    public OrthographicCameraData getOrthographicCameraData() {
        return data;
    }

    public void updateViewport(float width, float height) {
        data.updateViewport(width, height);
    }

    public Matrix4 getProjection() {
        return data.getProjection();
    }

    public Vector2 getScreenSize() {
        return data.getScreenSize();
    }
}