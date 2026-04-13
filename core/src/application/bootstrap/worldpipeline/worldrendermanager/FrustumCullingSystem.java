package application.bootstrap.worldpipeline.worldrendermanager;

import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.gridslot.GridSlotHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.SystemPackage;
import engine.settings.EngineSetting;
import engine.util.camera.CameraInstance;

class FrustumCullingSystem extends SystemPackage {

    /*
     * Per-frame frustum culling for world chunks and mega chunks. refresh() takes
     * a GridInstance and reads the camera from its window — each grid culls
     * against its own view independently. This enables the editor to run multiple
     * grids with different windows simultaneously without interference.
     */

    // Cached per awake
    private float megaAngularBleedBase;
    private float fullCenterRadiusSq;

    // Cached per frame — refreshed once per grid per renderWorld call
    private float cameraAngle;
    private float effectiveAngle;
    private float maxDistanceSq;

    // Internal \\

    @Override
    protected void awake() {

        float megaSize = EngineSetting.MEGA_CHUNK_SIZE;
        this.megaAngularBleedBase = (float) Math.sqrt(megaSize * megaSize + megaSize * megaSize) / 2f;

        float halfMega = megaSize / 2f;
        float centerRadius = (settings.maxRenderDistance / 2f) + halfMega;
        this.fullCenterRadiusSq = centerRadius * centerRadius;
    }

    // Refresh \\

    void refresh(GridInstance grid) {

        WindowInstance window = grid.getWindowInstance();

        if (window == null)
            return;

        CameraInstance camera = window.getActiveCamera();

        if (camera == null)
            return;

        this.cameraAngle = getCameraAngle(camera);

        float halfFov = getDiagonalHalfFov(camera);
        float absPitch = getAbsPitch(camera);
        float tAngle = getPitchT(absPitch, EngineSetting.FRUSTUM_PITCH_POWER_ANGLE);
        float tDistance = getPitchT(absPitch, EngineSetting.FRUSTUM_PITCH_POWER_DISTANCE);

        this.effectiveAngle = getEffectiveHalfAngle(halfFov, tAngle);
        this.maxDistanceSq = getPitchMaxDistanceSq(tDistance);
    }

    // Culling \\

    boolean isChunkVisible(GridSlotHandle slot) {

        float distanceSq = slot.getChunkDistanceFromCenter();

        if (distanceSq <= EngineSetting.FRUSTUM_ALWAYS_VISIBLE_DIST_SQ)
            return true;

        if (distanceSq > maxDistanceSq)
            return false;

        float distance = (float) Math.sqrt(distanceSq);
        float bleed = Math.max(
                EngineSetting.FRUSTUM_MIN_BLEED,
                EngineSetting.FRUSTUM_CHUNK_BLEED_SCALE / Math.max(distance, 0.001f));

        return isWithinAngle(slot.getChunkAngleFromCenter(), effectiveAngle + bleed);
    }

    boolean isMegaVisible(GridSlotHandle slot) {

        float distanceSq = slot.getMegaDistanceFromCenter();

        if (distanceSq > maxDistanceSq)
            return false;

        float distance = (float) Math.sqrt(distanceSq);
        float megaBleed = Math.max(
                EngineSetting.FRUSTUM_MIN_BLEED,
                (float) Math.atan(megaAngularBleedBase / Math.max(distance, 0.001f)));

        return isWithinAngle(slot.getMegaAngleFromCenter(), effectiveAngle + megaBleed);
    }

    private boolean isWithinAngle(float slotAngle, float tolerance) {

        float diff = slotAngle - cameraAngle;

        if (diff > EngineSetting.FRUSTUM_PI)
            diff -= EngineSetting.FRUSTUM_TWO_PI;

        if (diff < -EngineSetting.FRUSTUM_PI)
            diff += EngineSetting.FRUSTUM_TWO_PI;

        return Math.abs(diff) <= tolerance;
    }

    // Camera Math \\

    private float getCameraAngle(CameraInstance camera) {
        return (float) Math.atan2(camera.getDirection().z, camera.getDirection().x);
    }

    private float getDiagonalHalfFov(CameraInstance camera) {

        float verticalHalfFov = (float) Math.toRadians(camera.getFOV() / 2f);
        float aspectRatio = camera.getViewport().x / camera.getViewport().y;
        float horizontalHalfFov = (float) Math.atan(Math.tan(verticalHalfFov) * aspectRatio);

        return (float) Math.atan(Math.sqrt(
                Math.pow(Math.tan(verticalHalfFov), 2) +
                        Math.pow(Math.tan(horizontalHalfFov), 2)));
    }

    private float getAbsPitch(CameraInstance camera) {
        float sinPitch = Math.max(-1f, Math.min(1f, -camera.getDirection().y));
        return Math.abs((float) Math.asin(sinPitch));
    }

    private float getPitchT(float absPitch, float power) {
        return (float) Math.pow(absPitch / EngineSetting.FRUSTUM_HALF_PI, power);
    }

    private float getEffectiveHalfAngle(float halfFov, float t) {
        return halfFov + t * (EngineSetting.FRUSTUM_PI - halfFov);
    }

    private float getPitchMaxDistanceSq(float t) {
        return fullCenterRadiusSq + t * (EngineSetting.FRUSTUM_PITCH_MIN_DIST_SQ - fullCenterRadiusSq);
    }
}