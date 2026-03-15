package com.internal.bootstrap.renderpipeline.camera;

import com.internal.core.engine.InstancePackage;
import com.internal.core.util.mathematics.matrices.Matrix4;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3;

public class CameraInstance extends InstancePackage {

    /*
     * Runtime perspective camera. Wraps CameraData and delegates all
     * mutation and access through it. Owned and registered by CameraManager.
     */

    // Internal
    private CameraData data;

    // Constructor \\

    public void constructor(CameraData data) {
        this.data = data;
    }

    // Accessible \\

    public CameraData getCameraData() {
        return data;
    }

    public void setRotation(Vector2 input) {
        data.setRotation(input);
    }

    public void setPosition(Vector3 input) {
        data.setPosition(input);
    }

    public void updateViewport(float width, float height) {
        data.updateViewport(width, height);
    }

    public Matrix4 getProjection() {
        return data.getProjection();
    }

    public Matrix4 getView() {
        return data.getView();
    }

    public Matrix4 getViewProjection() {
        return data.getViewProjection();
    }

    public Matrix4 getInverseProjection() {
        return data.getInverseProjection();
    }

    public Matrix4 getInverseView() {
        return data.getInverseView();
    }

    public Vector3 getPosition() {
        return data.getPosition();
    }

    public Vector3 getDirection() {
        return data.getDirection();
    }

    public Vector3 getUp() {
        return data.getUp();
    }

    public Vector2 getViewport() {
        return data.getViewport();
    }

    public float getFOV() {
        return data.getFOV();
    }

    public float getNearPlane() {
        return data.getNearPlane();
    }

    public float getFarPlane() {
        return data.getFarPlane();
    }
}