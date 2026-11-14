package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.entity.EntityActionHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.player.PlayerCameraStruct;
import engine.assets.camera.CameraInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.NoiseUtility;

class CameraShakeBranch extends BranchPackage {

    /*
     * Shakes a window's view with the jolts its player takes. Each frame the
     * player is compared with how it was the frame before: a hard landing
     * builds trauma with the speed it fell at, a hit with the health it cost,
     * and a stagger or a guard knocked aside with a fixed jolt. Trauma drains
     * steadily, and the view turns by smooth noise on each of its axes, scaled
     * by the square of the trauma, so small knocks barely stir it and heavy
     * ones throw it hard before settling. The shake turns the view alone and
     * never the aim. Settings switch it off or scale it, and calm() stills it
     * while the player is not in play.
     */

    // Update \\

    void update(EntityInstance player, CameraInstance camera, PlayerCameraStruct view) {

        float deltaTime = internal.getDeltaTime();

        watchImpacts(player, view);
        view.advance(deltaTime, EngineSetting.CAMERA_SHAKE_DECAY, EngineSetting.CAMERA_SHAKE_TIME_PERIOD);

        if (!settings.cameraShake) {
            camera.setShake(0f, 0f, 0f);
            return;
        }

        float trauma = view.getTrauma();
        float shake = trauma * trauma * settings.cameraShakeIntensity;
        double time = view.getShakeTime() * EngineSetting.CAMERA_SHAKE_FREQUENCY;

        camera.setShake(
                EngineSetting.CAMERA_SHAKE_MAX_YAW_DEGREES * shake
                        * NoiseUtility.noise2(EngineSetting.CAMERA_SHAKE_SEED_YAW, time, 0.0),
                EngineSetting.CAMERA_SHAKE_MAX_PITCH_DEGREES * shake
                        * NoiseUtility.noise2(EngineSetting.CAMERA_SHAKE_SEED_PITCH, time, 0.0),
                EngineSetting.CAMERA_SHAKE_MAX_ROLL_DEGREES * shake
                        * NoiseUtility.noise2(EngineSetting.CAMERA_SHAKE_SEED_ROLL, time, 0.0));
    }

    void calm(CameraInstance camera, PlayerCameraStruct view) {
        view.reset();
        camera.setShake(0f, 0f, 0f);
    }

    // Impacts \\

    private void watchImpacts(EntityInstance player, PlayerCameraStruct view) {

        EntityStateHandle state = player.getEntityStateHandle();
        EntityActionHandle action = player.getEntityActionHandle();
        boolean airborne = !state.isGrounded();
        float health = player.getStatisticsHandle().getHealth();

        if (view.isWatched()) {
            watchLanding(view, airborne);
            watchHit(view, health);
            watchAction(view, action);
        }

        view.watch(
                airborne,
                airborne ? Math.max(0f, -state.getVerticalSpeed()) : 0f,
                health,
                action.getSequence());
    }

    // The ground meets the player at the speed it was falling the frame before
    private void watchLanding(PlayerCameraStruct view, boolean airborne) {

        if (!view.isAirborne() || airborne)
            return;

        float speed = view.getFallSpeed();
        float minimum = EngineSetting.CAMERA_SHAKE_LAND_SPEED_MIN;

        if (speed <= minimum)
            return;

        float share = Math.min(1f, (speed - minimum) / (EngineSetting.CAMERA_SHAKE_LAND_SPEED_MAX - minimum));

        view.addTrauma(share * EngineSetting.CAMERA_SHAKE_LAND_TRAUMA);
    }

    // A sudden fall in health is a hit; the slow drain of bleeding never reaches the threshold in one frame
    private void watchHit(PlayerCameraStruct view, float health) {

        float lost = view.getHealth() - health;

        if (lost < EngineSetting.CAMERA_SHAKE_HIT_HEALTH_MIN)
            return;

        view.addTrauma(Math.max(
                EngineSetting.CAMERA_SHAKE_HIT_TRAUMA_MIN,
                lost * EngineSetting.CAMERA_SHAKE_HIT_TRAUMA_PER_HEALTH));
    }

    private void watchAction(PlayerCameraStruct view, EntityActionHandle action) {

        if (action.getSequence() == view.getActionSequence())
            return;

        switch (action.getAction()) {
            case STAGGER -> view.addTrauma(EngineSetting.CAMERA_SHAKE_STAGGER_TRAUMA);
            case RECOIL -> view.addTrauma(EngineSetting.CAMERA_SHAKE_RECOIL_TRAUMA);
            default -> {
            }
        }
    }
}
