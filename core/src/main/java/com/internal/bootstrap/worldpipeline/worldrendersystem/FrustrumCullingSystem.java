package com.internal.bootstrap.worldpipeline.worldrendersystem;

import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridManager;
import com.internal.bootstrap.worldpipeline.gridmanager.GridSlotHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

class FrustumCullingSystem extends SystemPackage {

    // Internal
    private CameraManager cameraManager;
    private GridManager gridManager;

    // Culling constants
    private static final float TWO_PI = (float) (Math.PI * 2f);
    private static final float PI = (float) Math.PI;
    private static final float HALF_PI = (float) (Math.PI / 2f);

    private static final float MIN_BLEED = 0.05f;
    private static final float PITCH_MIN_DIST_SQ = 25f;
    private static final float PITCH_POWER_ANGLE = 1f;
    private static final float PITCH_POWER_DISTANCE = 6f;

    private static final float IMMEDIATE_3X3_DIST_SQ = 4.5f;

    private float fullCenterRadiusSq;
    private float megaAngularBleedBase;

    // Per-frame cached values — refreshed once per renderWorld call
    private float cameraAngle;
    private float effectiveAngle;
    private float maxDistanceSq;

    // Internal \\

    @Override
    protected void get() {
        this.cameraManager = get(CameraManager.class);
        this.gridManager = get(GridManager.class);
    }

    @Override
    protected void awake() {
        float megaSize = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaAngularBleedBase = (float) Math.sqrt(megaSize * megaSize + megaSize * megaSize) / 2f;

        float halfMega = megaSize / 2f;
        float centerRadius = (settings.maxRenderDistance / 2f) + halfMega;
        this.fullCenterRadiusSq = centerRadius * centerRadius;
    }

    // Called once per frame before any culling checks
    void refresh() {

        CameraInstance camera = cameraManager.getMainCamera();

        this.cameraAngle = getCameraAngle(camera);

        float halfFov = getHalfFov(camera);
        float absPitch = getAbsPitch(camera);

        float tAngle = getPitchT(absPitch, PITCH_POWER_ANGLE);
        float tDistance = getPitchT(absPitch, PITCH_POWER_DISTANCE);

        this.effectiveAngle = getEffectiveHalfAngle(halfFov, tAngle);
        this.maxDistanceSq = getPitchMaxDistanceSq(tDistance);
    }

    // Culling \\

    boolean isChunkVisible(GridSlotHandle slot) {

        float distanceSq = slot.getChunkDistanceFromCenter();

        if (distanceSq <= IMMEDIATE_3X3_DIST_SQ)
            return true;

        if (distanceSq > maxDistanceSq)
            return false;

        float distance = (float) Math.sqrt(distanceSq);
        float bleed = Math.max(MIN_BLEED, 0.75f / Math.max(distance, 0.001f));

        return isWithinAngle(slot.getChunkAngleFromCenter(), effectiveAngle + bleed);
    }

    boolean isMegaVisible(GridSlotHandle slot) {

        float distanceSq = slot.getMegaDistanceFromCenter();

        if (distanceSq > maxDistanceSq)
            return false;

        float distance = (float) Math.sqrt(distanceSq);
        float megaBleed = Math.max(MIN_BLEED, (float) Math.atan(megaAngularBleedBase / Math.max(distance, 0.001f)));

        return isWithinAngle(slot.getMegaAngleFromCenter(), effectiveAngle + megaBleed);
    }

    private boolean isWithinAngle(float slotAngle, float tolerance) {
        float diff = slotAngle - cameraAngle;
        if (diff > PI)
            diff -= TWO_PI;
        if (diff < -PI)
            diff += TWO_PI;
        return Math.abs(diff) <= tolerance;
    }

    // Camera Math \\

    private float getCameraAngle(CameraInstance camera) {
        return (float) Math.atan2(camera.getDirection().z, camera.getDirection().x);
    }

    private float getHalfFov(CameraInstance camera) {
        float verticalHalfFov = (float) Math.toRadians(camera.getFOV() / 2f);
        float aspectRatio = camera.getViewport().x / camera.getViewport().y;
        return (float) Math.atan(Math.tan(verticalHalfFov) * aspectRatio);
    }

    private float getAbsPitch(CameraInstance camera) {
        float sinPitch = Math.max(-1f, Math.min(1f, -camera.getDirection().y));
        return Math.abs((float) Math.asin(sinPitch));
    }

    private float getPitchT(float absPitch, float power) {
        return (float) Math.pow(absPitch / HALF_PI, power);
    }

    private float getEffectiveHalfAngle(float halfFov, float t) {
        return halfFov + t * (PI - halfFov);
    }

    private float getPitchMaxDistanceSq(float t) {
        return fullCenterRadiusSq + t * (PITCH_MIN_DIST_SQ - fullCenterRadiusSq);
    }
}